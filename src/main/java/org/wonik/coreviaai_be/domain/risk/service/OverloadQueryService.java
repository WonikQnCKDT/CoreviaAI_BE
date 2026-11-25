package org.wonik.coreviaai_be.domain.risk.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.department.entity.Department;
import org.wonik.coreviaai_be.domain.department.repository.DepartmentRepository;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.policy.ProjectAccessPolicy;
import org.wonik.coreviaai_be.domain.project.policy.ProjectVisibilityPolicy;
import org.wonik.coreviaai_be.domain.project.repository.ProjectMemberRepository;
import org.wonik.coreviaai_be.domain.risk.config.RiskAnalysisProperties;
import org.wonik.coreviaai_be.domain.risk.entity.RiskAnalysisRun;
import org.wonik.coreviaai_be.domain.risk.entity.RiskDetection;
import org.wonik.coreviaai_be.domain.risk.entity.type.RiskDetectionType;
import org.wonik.coreviaai_be.domain.risk.entity.type.RiskRunStatus;
import org.wonik.coreviaai_be.domain.risk.repository.RiskAnalysisRunRepository;
import org.wonik.coreviaai_be.domain.risk.repository.RiskDetectionRepository;
import org.wonik.coreviaai_be.domain.risk.service.result.OverloadView;
import org.wonik.coreviaai_be.domain.risk.service.result.OverloadView.AssigneeLoad;
import org.wonik.coreviaai_be.domain.risk.service.result.OverloadView.DepartmentLoad;
import org.wonik.coreviaai_be.domain.risk.service.result.OverloadView.Scope;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;
import org.wonik.global.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 최근 성공한 일일 계산의 인력 편중을 권한에 맞게 돌려준다(FR-AIR-03, 권한 매트릭스 3.3).
 * PM은 프로젝트의 담당자 전원, 개발자·QA는 본인, 경영진은 소속 범위 부서의 인원 수만 본다.
 */
@Service
@RequiredArgsConstructor
public class OverloadQueryService {

	static final String UNAFFILIATED_DEPARTMENT_NAME = "소속 없음";

	private final ProjectAccessPolicy accessPolicy;
	private final ProjectVisibilityPolicy visibilityPolicy;
	private final ProjectMemberRepository memberRepository;
	private final RiskAnalysisRunRepository runRepository;
	private final RiskDetectionRepository detectionRepository;
	private final UserRepository userRepository;
	private final DepartmentRepository departmentRepository;
	private final RiskAnalysisProperties properties;
	private final JsonMapper jsonMapper;

	@Transactional(readOnly = true)
	public OverloadView find(final Long viewerId, final Long projectId) {
		accessPolicy.getVisibleProject(viewerId, projectId);
		final User viewer = accessPolicy.getViewer(viewerId);
		final Scope scope = resolveScope(viewer, projectId);
		final Optional<RiskAnalysisRun> run = runRepository.findFirstByStatusOrderByRunDateDesc(RiskRunStatus.SUCCEEDED);
		if (run.isEmpty()) {
			return OverloadView.empty(scope, properties.overloadThresholdPercent());
		}
		final List<RiskDetection> detections = detectionRepository
			.findAllByRunIdAndProjectIdAndDetectionTypeOrderByTargetIdAsc(
				run.get().getId(), projectId, RiskDetectionType.OVERLOAD);
		final int threshold = detections.stream()
			.map(this::thresholdOf)
			.flatMap(Optional::stream)
			.findFirst()
			.orElse(properties.overloadThresholdPercent());
		if (scope == Scope.DEPARTMENTS) {
			return new OverloadView(scope, threshold, run.get().getRunDate(), List.of(),
				departmentsOf(viewer, detections));
		}
		final List<RiskDetection> visible = accessPolicy.isPm(viewerId, projectId)
			? detections
			: detections.stream().filter(row -> viewerId.equals(row.getTargetId())).toList();
		return new OverloadView(scope, threshold, run.get().getRunDate(), assigneesOf(visible), List.of());
	}

	private Scope resolveScope(final User viewer, final Long projectId) {
		if (viewer.getSystemRole() == SystemRole.EXECUTIVE) {
			return Scope.DEPARTMENTS;
		}
		if (accessPolicy.isPm(viewerId(viewer), projectId)) {
			return Scope.ASSIGNEES;
		}
		return memberRepository.findByProjectIdAndUserId(projectId, viewer.getId())
			.map(member -> member.getActor())
			.filter(actor -> actor == ProjectActor.DEVELOPER || actor == ProjectActor.QA)
			.map(actor -> Scope.ASSIGNEES)
			.orElseThrow(() -> new CustomException(ErrorCode.OVERLOAD_VIEW_FORBIDDEN));
	}

	private static Long viewerId(final User viewer) {
		return viewer.getId();
	}

	private List<AssigneeLoad> assigneesOf(final List<RiskDetection> detections) {
		if (detections.isEmpty()) {
			return List.of();
		}
		final Map<Long, User> users = userRepository.findAllById(detections.stream().map(RiskDetection::getTargetId).toList())
			.stream()
			.collect(Collectors.toMap(User::getId, Function.identity()));
		final List<AssigneeLoad> loads = new ArrayList<>();
		for (final RiskDetection detection : detections) {
			final User user = users.get(detection.getTargetId());
			if (user == null) {
				continue;
			}
			final StoredDetails details = readDetails(detection.getDetails());
			loads.add(new AssigneeLoad(
				user.getId(),
				user.getName(),
				detection.getMetricValue(),
				details.allocatedEffort(),
				details.availableDays()));
		}
		loads.sort(Comparator.comparing(AssigneeLoad::userId));
		return loads;
	}

	private List<DepartmentLoad> departmentsOf(final User executive, final List<RiskDetection> detections) {
		if (detections.isEmpty()) {
			return List.of();
		}
		final Set<Long> scope = visibilityPolicy.executiveScope(executive.getDepartmentId());
		final Map<Long, User> users = userRepository.findAllById(detections.stream().map(RiskDetection::getTargetId).toList())
			.stream()
			.collect(Collectors.toMap(User::getId, Function.identity()));
		final Map<Long, Integer> counts = new HashMap<>();
		int unaffiliated = 0;
		for (final RiskDetection detection : detections) {
			final User user = users.get(detection.getTargetId());
			if (user == null) {
				continue;
			}
			final Long departmentId = user.getDepartmentId();
			if (departmentId == null) {
				unaffiliated++;
			} else if (scope.contains(departmentId)) {
				counts.merge(departmentId, 1, Integer::sum);
			}
		}
		final Map<Long, String> names = departmentRepository.findAllById(counts.keySet()).stream()
			.collect(Collectors.toMap(Department::getId, Department::getName));
		final List<DepartmentLoad> loads = new ArrayList<>();
		counts.entrySet().stream()
			.sorted(Map.Entry.comparingByKey())
			.forEach(entry -> loads.add(new DepartmentLoad(entry.getKey(), names.get(entry.getKey()), entry.getValue())));
		if (unaffiliated > 0) {
			loads.add(new DepartmentLoad(null, UNAFFILIATED_DEPARTMENT_NAME, unaffiliated));
		}
		return loads;
	}

	private Optional<Integer> thresholdOf(final RiskDetection detection) {
		final JsonNode node = readTree(detection.getDetails());
		if (node == null || !node.hasNonNull("thresholdPercent")) {
			return Optional.empty();
		}
		return Optional.of(node.get("thresholdPercent").asInt());
	}

	private StoredDetails readDetails(final String json) {
		final JsonNode node = readTree(json);
		if (node == null) {
			return new StoredDetails(null, 0);
		}
		final BigDecimal effort = node.hasNonNull("allocatedEffort")
			? new BigDecimal(node.get("allocatedEffort").asString())
			: null;
		final int days = node.hasNonNull("availableDays") ? node.get("availableDays").asInt() : 0;
		return new StoredDetails(effort, days);
	}

	private JsonNode readTree(final String json) {
		if (json == null || json.isBlank()) {
			return null;
		}
		return jsonMapper.readTree(json);
	}

	private record StoredDetails(BigDecimal allocatedEffort, int availableDays) {
	}
}

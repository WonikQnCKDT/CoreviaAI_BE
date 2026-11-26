package org.wonik.coreviaai_be.domain.project.service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.department.entity.Department;
import org.wonik.coreviaai_be.domain.department.repository.DepartmentRepository;
import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.entity.ProjectActorAssignment;
import org.wonik.coreviaai_be.domain.project.entity.ProjectDepartment;
import org.wonik.coreviaai_be.domain.project.entity.ProjectFeatureAssignment;
import org.wonik.coreviaai_be.domain.project.entity.ProjectMember;
import org.wonik.coreviaai_be.domain.project.entity.ProjectPhase;
import org.wonik.coreviaai_be.domain.project.entity.ProjectSettingHistory;
import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectFeature;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectSettingChangeType;
import org.wonik.coreviaai_be.domain.project.policy.ProjectAccessPolicy;
import org.wonik.coreviaai_be.domain.project.repository.ProjectActorAssignmentRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectDepartmentRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectFeatureAssignmentRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectMemberRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectPhaseRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectSettingHistoryRepository;
import org.wonik.coreviaai_be.domain.project.service.command.UpdateProjectSettingsCommand;
import org.wonik.coreviaai_be.domain.project.service.command.UpdateProjectSettingsCommand.BasicInfo;
import org.wonik.coreviaai_be.domain.project.service.result.ProjectSettingHistoryResult;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsTaskRepository;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.audit.AuditChanges;
import org.wonik.global.audit.AuditLogger;
import org.wonik.global.audit.AuditTargetType;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.pagination.CursorPage;
import org.wonik.global.common.pagination.CursorPageRequest;
import org.wonik.global.user.entity.User;
import org.wonik.global.user.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import lombok.RequiredArgsConstructor;

/**
 * 생성 후 프로젝트 설정 변경(FR-PRJ-14). PM만 바꿀 수 있고, 바뀐 구역마다 변경 전후 이력을 남긴다.
 * 기능 모듈을 꺼도 모듈 데이터는 지우지 않는다. 운영 방식은 업무가 없을 때만 바꾼다.
 */
@Service
@RequiredArgsConstructor
public class ProjectSettingsService {

	private final ProjectAccessPolicy accessPolicy;
	private final ProjectDepartmentRepository departmentRepository;
	private final DepartmentRepository orgDepartmentRepository;
	private final ProjectActorAssignmentRepository actorRepository;
	private final ProjectFeatureAssignmentRepository featureRepository;
	private final ProjectMemberRepository memberRepository;
	private final ProjectPhaseRepository phaseRepository;
	private final ProjectSettingHistoryRepository historyRepository;
	private final WbsTaskRepository taskRepository;
	private final UserRepository userRepository;
	private final AuditLogger auditLogger;
	private final EntityManager entityManager;
	private final JsonMapper jsonMapper;
	private final Clock clock;

	@Transactional
	public void update(final AuditActor actor, final Long projectId, final UpdateProjectSettingsCommand command) {
		if (command.basicInfo() == null && command.methodology() == null && command.actors() == null
			&& command.features() == null && !command.gateApproverPresent()) {
			throw new CustomException(ErrorCode.INVALID_PROJECT_SETTINGS);
		}
		final Project project = accessPolicy.getManagedProjectForUpdate(actor.userId(), projectId);
		if (project.getVersion() != command.version()) {
			throw new CustomException(ErrorCode.CONCURRENT_MODIFICATION);
		}
		final List<ProjectSettingChangeType> changed = new ArrayList<>();
		if (command.basicInfo() != null && changeBasicInfo(project, command.basicInfo(), actor)) {
			changed.add(ProjectSettingChangeType.BASIC_INFO);
		}
		if (command.methodology() != null && changeMethodology(project, command.methodology(), actor)) {
			changed.add(ProjectSettingChangeType.METHODOLOGY);
		}
		if (command.actors() != null && changeActors(project, command.actors(), actor)) {
			changed.add(ProjectSettingChangeType.ACTORS);
		}
		if (command.features() != null && changeFeatures(project, command.features(), actor)) {
			changed.add(ProjectSettingChangeType.FEATURES);
		}
		if (command.gateApproverPresent() && changeGateApprover(project, command.gateApproverUserId(), actor)) {
			changed.add(ProjectSettingChangeType.GATE_APPROVER);
		}
		if (changed.isEmpty()) {
			return;
		}
		entityManager.lock(project, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
		auditLogger.record(actor, AuditAction.PROJECT_SETTINGS_CHANGED, AuditTargetType.PROJECT, projectId,
			AuditChanges.create().value("changeTypes", changed));
	}

	@Transactional(readOnly = true)
	public CursorPage<ProjectSettingHistoryResult> findHistories(
		final Long viewerId,
		final Long projectId,
		final CursorPageRequest page
	) {
		accessPolicy.getVisibleProject(viewerId, projectId);
		final CursorPage<ProjectSettingHistory> histories = CursorPage.of(
			historyRepository.findPageByProjectId(projectId, page.cursorId(), page.fetchPageable()),
			page.size(),
			ProjectSettingHistory::getId
		);
		final Set<Long> userIds = histories.items().stream().map(ProjectSettingHistory::getChangedBy)
			.collect(Collectors.toSet());
		final Map<Long, String> names = userRepository.findAllById(userIds).stream()
			.collect(Collectors.toMap(User::getId, User::getName));
		return histories.map(history -> new ProjectSettingHistoryResult(
			history,
			names.get(history.getChangedBy()),
			readJson(history.getBeforeValue()),
			readJson(history.getAfterValue())
		));
	}

	private boolean changeBasicInfo(final Project project, final BasicInfo basicInfo, final AuditActor actor) {
		if (basicInfo.startDate() != null && basicInfo.endDate() != null
			&& basicInfo.startDate().isAfter(basicInfo.endDate())) {
			throw new CustomException(ErrorCode.INVALID_PROJECT_PERIOD);
		}
		final List<Long> departmentIds = distinctDepartments(basicInfo.departmentIds());
		requireAssignableDepartments(departmentIds);
		final List<Long> beforeIds = departmentRepository.findAllByProjectIdOrderById(project.getId()).stream()
			.map(ProjectDepartment::getDepartmentId)
			.toList();
		final boolean sameDepartments = new HashSet<>(beforeIds).equals(new HashSet<>(departmentIds));
		final boolean same = sameDepartments
			&& Objects.equals(project.getStartDate(), basicInfo.startDate())
			&& Objects.equals(project.getEndDate(), basicInfo.endDate())
			&& project.getVisibility() == basicInfo.visibility();
		if (same) {
			return false;
		}
		final Map<String, Object> before = basicInfoMap(project.getStartDate(), project.getEndDate(), beforeIds,
			project.getVisibility().name());
		project.changeBasicInfo(basicInfo.startDate(), basicInfo.endDate(), basicInfo.visibility());
		if (!sameDepartments) {
			replaceDepartments(project.getId(), beforeIds, departmentIds);
		}
		saveHistory(project.getId(), ProjectSettingChangeType.BASIC_INFO, before,
			basicInfoMap(basicInfo.startDate(), basicInfo.endDate(), departmentIds, basicInfo.visibility().name()),
			actor);
		return true;
	}

	private boolean changeMethodology(final Project project, final Methodology methodology, final AuditActor actor) {
		if (project.getMethodology() == methodology) {
			return false;
		}
		if (!methodology.isEnabled()) {
			throw new CustomException(ErrorCode.METHODOLOGY_NOT_AVAILABLE);
		}
		if (taskRepository.countByProjectId(project.getId()) > 0) {
			throw new CustomException(ErrorCode.METHODOLOGY_CHANGE_NOT_ALLOWED);
		}
		final String before = project.getMethodology().name();
		project.changeMethodology(methodology);
		saveHistory(project.getId(), ProjectSettingChangeType.METHODOLOGY,
			Map.of("methodology", before), Map.of("methodology", methodology.name()), actor);
		return true;
	}

	private boolean changeActors(final Project project, final Set<ProjectActor> actors, final AuditActor actor) {
		if (!actors.contains(ProjectActor.PM)) {
			throw new CustomException(ErrorCode.PM_ACTOR_REQUIRED);
		}
		for (final ProjectActor assigned : actors) {
			if (!assigned.selectableFor(project.getDepartmentType())) {
				throw new CustomException(ErrorCode.ACTOR_NOT_AVAILABLE);
			}
		}
		final Set<ProjectActor> current = actorRepository.findAllByProjectIdOrderById(project.getId()).stream()
			.map(ProjectActorAssignment::getActor)
			.collect(Collectors.toSet());
		if (current.equals(actors)) {
			return false;
		}
		final boolean memberUsesRemovedActor = memberRepository.findAllByProjectIdOrderById(project.getId()).stream()
			.anyMatch(member -> !actors.contains(member.getActor()));
		if (memberUsesRemovedActor) {
			throw new CustomException(ErrorCode.ACTOR_HAS_MEMBERS);
		}
		final List<String> before = names(current);
		final List<ProjectActorAssignment> removed = actorRepository.findAllByProjectIdOrderById(project.getId()).stream()
			.filter(assignment -> !actors.contains(assignment.getActor()))
			.toList();
		actorRepository.deleteAll(removed);
		actors.stream()
			.filter(actorType -> !current.contains(actorType))
			.map(actorType -> ProjectActorAssignment.of(project.getId(), actorType))
			.forEach(actorRepository::save);
		saveHistory(project.getId(), ProjectSettingChangeType.ACTORS,
			Map.of("actors", before), Map.of("actors", names(actors)), actor);
		return true;
	}

	private boolean changeFeatures(final Project project, final Set<ProjectFeature> features, final AuditActor actor) {
		final boolean hasPhases = phaseRepository.countByProjectId(project.getId()) > 0;
		for (final ProjectFeature feature : features) {
			if (!feature.selectableFor(hasPhases)) {
				throw new CustomException(ErrorCode.FEATURE_NOT_AVAILABLE);
			}
		}
		final Set<ProjectFeature> current = featureRepository.findAllByProjectIdOrderById(project.getId()).stream()
			.map(ProjectFeatureAssignment::getFeature)
			.collect(Collectors.toSet());
		if (current.equals(features)) {
			return false;
		}
		final List<String> before = names(current);
		final List<ProjectFeatureAssignment> removed = featureRepository.findAllByProjectIdOrderById(project.getId()).stream()
			.filter(assignment -> !features.contains(assignment.getFeature()))
			.toList();
		featureRepository.deleteAll(removed);
		features.stream()
			.filter(feature -> !current.contains(feature))
			.map(feature -> ProjectFeatureAssignment.of(project.getId(), feature))
			.forEach(featureRepository::save);
		saveHistory(project.getId(), ProjectSettingChangeType.FEATURES,
			Map.of("features", before), Map.of("features", names(features)), actor);
		return true;
	}

	private boolean changeGateApprover(final Project project, final Long userId, final AuditActor actor) {
		final List<ProjectPhase> phases = phaseRepository.findAllByProjectIdOrderByPhaseOrder(project.getId());
		if (phases.isEmpty()) {
			throw new CustomException(ErrorCode.GATE_APPROVER_NOT_AVAILABLE);
		}
		if (userId != null) {
			final ProjectMember member = memberRepository.findByProjectIdAndUserId(project.getId(), userId)
				.orElseThrow(() -> new CustomException(ErrorCode.GATE_APPROVER_NOT_MEMBER));
			if (member.getActor() == ProjectActor.PM) {
				throw new CustomException(ErrorCode.GATE_APPROVER_CANNOT_BE_PM);
			}
			final User user = userRepository.findById(userId)
				.orElseThrow(() -> new CustomException(ErrorCode.GATE_APPROVER_NOT_MEMBER));
			if (!user.isActive()) {
				throw new CustomException(ErrorCode.GATE_APPROVER_NOT_MEMBER);
			}
		}
		final boolean unchanged = phases.stream().allMatch(phase -> Objects.equals(phase.getGateApproverUserId(), userId));
		if (unchanged) {
			return false;
		}
		final Long before = phases.get(0).getGateApproverUserId();
		phases.forEach(phase -> phase.assignGateApprover(userId));
		saveHistory(project.getId(), ProjectSettingChangeType.GATE_APPROVER,
			approverMap(before), approverMap(userId), actor);
		return true;
	}

	private void replaceDepartments(final Long projectId, final List<Long> beforeIds, final List<Long> departmentIds) {
		final Set<Long> next = new HashSet<>(departmentIds);
		final List<ProjectDepartment> current = departmentRepository.findAllByProjectIdOrderById(projectId);
		departmentRepository.deleteAll(current.stream().filter(row -> !next.contains(row.getDepartmentId())).toList());
		final Set<Long> existing = new HashSet<>(beforeIds);
		departmentIds.stream()
			.filter(id -> !existing.contains(id))
			.map(id -> ProjectDepartment.of(projectId, id))
			.forEach(departmentRepository::save);
	}

	private static List<Long> distinctDepartments(final List<Long> departmentIds) {
		if (new HashSet<>(departmentIds).size() != departmentIds.size()) {
			throw new CustomException(ErrorCode.DUPLICATE_PROJECT_DEPARTMENT);
		}
		return List.copyOf(departmentIds);
	}

	private void requireAssignableDepartments(final List<Long> departmentIds) {
		if (departmentIds.isEmpty()) {
			return;
		}
		final List<Department> departments = orgDepartmentRepository.findAllById(departmentIds);
		final boolean assignable = departments.size() == departmentIds.size()
			&& departments.stream().allMatch(Department::isActive);
		if (!assignable) {
			throw new CustomException(ErrorCode.INVALID_PROJECT_DEPARTMENT);
		}
	}

	private void saveHistory(
		final Long projectId,
		final ProjectSettingChangeType changeType,
		final Map<String, Object> before,
		final Map<String, Object> after,
		final AuditActor actor
	) {
		historyRepository.save(ProjectSettingHistory.of(
			projectId,
			changeType,
			jsonMapper.writeValueAsString(before),
			jsonMapper.writeValueAsString(after),
			actor.userId(),
			clock.instant()
		));
	}

	private static Map<String, Object> basicInfoMap(
		final Object startDate,
		final Object endDate,
		final List<Long> departmentIds,
		final String visibility
	) {
		final Map<String, Object> values = new LinkedHashMap<>();
		values.put("startDate", startDate == null ? null : startDate.toString());
		values.put("endDate", endDate == null ? null : endDate.toString());
		values.put("departmentIds", departmentIds);
		values.put("visibility", visibility);
		return values;
	}

	private static Map<String, Object> approverMap(final Long userId) {
		final Map<String, Object> values = new LinkedHashMap<>();
		values.put("userId", userId);
		return values;
	}

	private static List<String> names(final Set<? extends Enum<?>> values) {
		return values.stream().map(Enum::name).sorted().toList();
	}

	private JsonNode readJson(final String value) {
		if (value == null) {
			return null;
		}
		try {
			final JsonNode node = jsonMapper.readTree(value);
			return node.isString() ? jsonMapper.readTree(node.stringValue()) : node;
		} catch (final JacksonException exception) {
			throw new CustomException(ErrorCode.JSON_PARSING_FAILURE, exception);
		}
	}
}

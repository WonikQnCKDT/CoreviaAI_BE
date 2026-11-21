package org.wonik.coreviaai_be.domain.project.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.department.entity.Department;
import org.wonik.coreviaai_be.domain.department.repository.DepartmentRepository;
import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.entity.ProjectActorAssignment;
import org.wonik.coreviaai_be.domain.project.entity.ProjectDepartment;
import org.wonik.coreviaai_be.domain.project.entity.ProjectFeatureAssignment;
import org.wonik.coreviaai_be.domain.project.entity.ProjectMember;
import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectFeature;
import org.wonik.coreviaai_be.domain.project.policy.ProjectCreationPolicy;
import org.wonik.coreviaai_be.domain.project.repository.ProjectActorAssignmentRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectDepartmentRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectFeatureAssignmentRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectMemberRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectRepository;
import org.wonik.coreviaai_be.domain.project.service.command.CreateProjectCommand;
import org.wonik.coreviaai_be.domain.project.service.command.InitialMember;
import org.wonik.coreviaai_be.domain.project.structure.ProjectStructureInitializer;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.audit.AuditChanges;
import org.wonik.global.audit.AuditLogger;
import org.wonik.global.audit.AuditTargetType;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;
import org.wonik.global.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 프로젝트 생성(FR-PRJ-01). 단계형 화면(①~⑥)의 입력을 마지막에 한 번에 받아 한 트랜잭션으로 만든다.
 * 그래서 템플릿·운영 방식·액터 지정이 끝나기 전의 반쯤 만든 프로젝트가 DB에 남지 않는다.
 * <p>
 * 생성 권한 제한은 없다(요구사항 명세서 3.1): 활성 일반 사용자는 누구나 만들 수 있고 생성자는 첫 PM으로 등록된다.
 * 경영진은 프로젝트에서 읽기 전용, 시스템 관리자는 프로젝트 데이터에 접근하지 않으므로 생성할 수 없다(3.3 PRJ-01).
 * PM 추가·해제는 ProjectMemberService, 관리자 PM 재지정은 ProjectPmService(FR-PRJ-04), 게이트 승인자 변경은 FR-PRJ-05 범위다.
 */
@Service
@RequiredArgsConstructor
public class ProjectCreationService {

	private final ProjectRepository projectRepository;
	private final ProjectActorAssignmentRepository actorAssignmentRepository;
	private final ProjectDepartmentRepository projectDepartmentRepository;
	private final ProjectFeatureAssignmentRepository featureAssignmentRepository;
	private final ProjectMemberRepository memberRepository;
	private final UserRepository userRepository;
	private final DepartmentRepository departmentRepository;
	private final ProjectCreationPolicy creationPolicy;
	private final ProjectMemberService memberService;
	private final List<ProjectStructureInitializer> structureInitializers;
	private final AuditLogger auditLogger;

	/**
	 * code가 자연 키다. 같은 요청이 재전송되면 두 번째는 DUPLICATE_PROJECT_CODE(409)로 끝나고 프로젝트는 하나만 생긴다.
	 *
	 * @return 생성한 프로젝트 ID
	 */
	@Transactional
	public Long create(final AuditActor actor, final CreateProjectCommand command) {
		final User creator = requireProjectCreator(actor.userId());
		creationPolicy.validate(command, creator.getId());
		requireAssignableDepartments(command.departmentIds());
		memberService.requireInvitableUsers(command.members().stream().map(InitialMember::userId).toList());
		if (projectRepository.existsByCode(command.code())) {
			throw new CustomException(ErrorCode.DUPLICATE_PROJECT_CODE);
		}

		final Project project = saveAndFlushNewProject(Project.create(
			command.code(),
			command.name().strip(),
			command.startDate(),
			command.endDate(),
			command.visibility(),
			command.template(),
			command.methodology(),
			command.departmentType(),
			creator.getId()
		));
		final List<ProjectActor> actors = command.actors().stream().sorted().toList();
		actorAssignmentRepository.saveAll(
			actors.stream().map(projectActor -> ProjectActorAssignment.of(project.getId(), projectActor)).toList()
		);
		final List<Long> departmentIds = command.departmentIds().stream().sorted().toList();
		projectDepartmentRepository.saveAll(
			departmentIds.stream().map(departmentId -> ProjectDepartment.of(project.getId(), departmentId)).toList()
		);
		final List<ProjectFeature> features = command.features().stream().sorted().toList();
		featureAssignmentRepository.saveAll(
			features.stream().map(feature -> ProjectFeatureAssignment.of(project.getId(), feature)).toList()
		);
		memberRepository.saveAll(membersOf(project, creator, command.members()));
		initializerFor(command.methodology()).initialize(project, command.gateApproverUserId());

		auditLogger.record(actor, AuditAction.PROJECT_CREATED, AuditTargetType.PROJECT, project.getId(), AuditChanges.create()
			.value("code", project.getCode())
			.value("name", project.getName())
			.value("template", project.getTemplate())
			.value("methodology", project.getMethodology())
			.value("departmentType", project.getDepartmentType())
			.value("actors", actors)
			.value("features", features)
			.value("departmentIds", departmentIds)
			.value("pmUserIds", List.of(creator.getId()))
			.value("memberUserIds", command.members().stream().map(InitialMember::userId).sorted().toList())
			.value("gateApproverUserId", command.gateApproverUserId()));
		return project.getId();
	}

	/** ① 단계에서 입력 중인 코드의 중복을 미리 확인한다. 최종 판정은 생성 시점의 UNIQUE 제약이 한다. */
	@Transactional(readOnly = true)
	public boolean isCodeAvailable(final Long userId, final String code) {
		requireProjectCreator(userId);
		return !projectRepository.existsByCode(code);
	}

	/** 별도 생성 권한 없이 활성 일반 사용자는 누구나 만들 수 있다(요구사항 명세서 3.1, FR-PRJ-01 인수 기준). */
	private User requireProjectCreator(final Long userId) {
		final User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_CREATION_FORBIDDEN));
		if (!user.isActive() || user.getSystemRole() != SystemRole.USER) {
			throw new CustomException(ErrorCode.PROJECT_CREATION_FORBIDDEN);
		}
		return user;
	}

	/** 주관 부서는 모두 존재하는 활성 부서여야 한다. 지정하지 않으면(빈 목록) 검사하지 않는다. */
	private void requireAssignableDepartments(final List<Long> departmentIds) {
		if (departmentIds.isEmpty()) {
			return;
		}
		final Set<Long> distinctIds = new HashSet<>(departmentIds);
		final List<Department> departments = departmentRepository.findAllById(distinctIds);
		final boolean assignable = departments.size() == distinctIds.size()
			&& departments.stream().allMatch(Department::isActive);
		if (!assignable) {
			throw new CustomException(ErrorCode.INVALID_PROJECT_DEPARTMENT);
		}
	}

	private List<ProjectMember> membersOf(final Project project, final User creator, final List<InitialMember> invited) {
		final List<ProjectMember> members = new ArrayList<>(invited.size() + 1);
		members.add(ProjectMember.of(project.getId(), creator.getId(), ProjectActor.PM));
		invited.stream()
			.sorted(Comparator.comparing(InitialMember::userId))
			.forEach(member -> members.add(ProjectMember.of(project.getId(), member.userId(), member.actor())));
		return members;
	}

	/** 정책에서 비활성 운영 방식을 이미 거부하므로, 여기서 구현체가 없으면 코드 구성 오류다. */
	private ProjectStructureInitializer initializerFor(final Methodology methodology) {
		return structureInitializers.stream()
			.filter(initializer -> initializer.supports(methodology))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("운영 방식의 기본 구조 생성기가 없습니다: " + methodology));
	}

	/** 사전 중복 확인과 저장 사이에 같은 코드가 먼저 저장되면 UNIQUE 제약 위반을 중복 오류로 바꾼다. */
	private Project saveAndFlushNewProject(final Project project) {
		try {
			return projectRepository.saveAndFlush(project);
		} catch (final DataIntegrityViolationException e) {
			throw new CustomException(ErrorCode.DUPLICATE_PROJECT_CODE, e);
		}
	}
}

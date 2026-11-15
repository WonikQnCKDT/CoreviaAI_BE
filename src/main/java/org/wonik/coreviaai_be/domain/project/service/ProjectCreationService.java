package org.wonik.coreviaai_be.domain.project.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.department.entity.Department;
import org.wonik.coreviaai_be.domain.department.repository.DepartmentRepository;
import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.entity.ProjectActorAssignment;
import org.wonik.coreviaai_be.domain.project.entity.ProjectMember;
import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.policy.ProjectCreationPolicy;
import org.wonik.coreviaai_be.domain.project.repository.ProjectActorAssignmentRepository;
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
 * 프로젝트 생성(FR-PRJ-01). 단계형 화면(①~⑤)의 입력을 마지막에 한 번에 받아 한 트랜잭션으로 만든다.
 * 그래서 운영 방식·액터 지정이 끝나기 전의 반쯤 만든 프로젝트가 DB에 남지 않는다.
 * <p>
 * 생성자는 PM으로 자동 등록된다(요구사항 명세서 3.1). PM 위임·재지정은 FR-PRJ-04,
 * 게이트 승인자 변경은 FR-PRJ-05 범위다.
 */
@Service
@RequiredArgsConstructor
public class ProjectCreationService {

	private final ProjectRepository projectRepository;
	private final ProjectActorAssignmentRepository actorAssignmentRepository;
	private final ProjectMemberRepository memberRepository;
	private final UserRepository userRepository;
	private final DepartmentRepository departmentRepository;
	private final ProjectCreationPolicy creationPolicy;
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
		requireAssignableDepartment(command.departmentId());
		requireInvitableUsers(command.members());
		if (projectRepository.existsByCode(command.code())) {
			throw new CustomException(ErrorCode.DUPLICATE_PROJECT_CODE);
		}

		final Project project = saveAndFlushNewProject(Project.create(
			command.code(),
			command.name().strip(),
			command.startDate(),
			command.endDate(),
			command.departmentId(),
			command.visibility(),
			command.methodology(),
			command.departmentType(),
			creator.getId()
		));
		final List<ProjectActor> actors = command.actors().stream().sorted().toList();
		actorAssignmentRepository.saveAll(
			actors.stream().map(projectActor -> ProjectActorAssignment.of(project.getId(), projectActor)).toList()
		);
		memberRepository.saveAll(membersOf(project, creator, command.members()));
		initializerFor(command.methodology()).initialize(project, command.gateApproverUserId());

		auditLogger.record(actor, AuditAction.PROJECT_CREATED, AuditTargetType.PROJECT, project.getId(), AuditChanges.create()
			.value("code", project.getCode())
			.value("name", project.getName())
			.value("methodology", project.getMethodology())
			.value("departmentType", project.getDepartmentType())
			.value("actors", actors)
			.value("pmUserId", project.getPmUserId())
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

	/** 프로젝트 생성 권한은 일반 사용자에게만 부여된다(FR-ADM-02). 역할이 바뀌면 권한도 함께 회수된다. */
	private User requireProjectCreator(final Long userId) {
		final User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_CREATION_FORBIDDEN));
		if (!user.isActive() || user.getSystemRole() != SystemRole.USER || !user.isCanCreateProject()) {
			throw new CustomException(ErrorCode.PROJECT_CREATION_FORBIDDEN);
		}
		return user;
	}

	private void requireAssignableDepartment(final Long departmentId) {
		final boolean assignable = departmentRepository.findById(departmentId)
			.map(Department::isActive)
			.orElse(false);
		if (!assignable) {
			throw new CustomException(ErrorCode.INVALID_PROJECT_DEPARTMENT);
		}
	}

	/**
	 * 초대 대상은 활성 일반 사용자만이다. 경영진은 프로젝트에서 읽기 전용이라(3.1) 역할을 줄 수 없고,
	 * 시스템 관리자는 프로젝트 데이터에 접근할 수 없다. 다른 부서 사용자는 초대할 수 있다(현업 담당자 등).
	 */
	private void requireInvitableUsers(final List<InitialMember> members) {
		if (members.isEmpty()) {
			return;
		}
		final Set<Long> userIds = members.stream().map(InitialMember::userId).collect(Collectors.toSet());
		final List<User> users = userRepository.findAllById(userIds);
		final boolean allInvitable = users.size() == userIds.size()
			&& users.stream().allMatch(user -> user.isActive() && user.getSystemRole() == SystemRole.USER);
		if (!allInvitable) {
			throw new CustomException(ErrorCode.INVALID_PROJECT_MEMBER);
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

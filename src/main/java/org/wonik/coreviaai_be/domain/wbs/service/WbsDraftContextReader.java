package org.wonik.coreviaai_be.domain.wbs.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.entity.ProjectMember;
import org.wonik.coreviaai_be.domain.project.entity.ProjectPhase;
import org.wonik.coreviaai_be.domain.project.repository.ProjectMemberRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectPhaseRepository;
import org.wonik.coreviaai_be.domain.wbs.policy.WbsDraftAccessPolicy;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsTaskRepository;
import org.wonik.coreviaai_be.domain.wbs.service.command.DraftExistingTask;
import org.wonik.coreviaai_be.domain.wbs.service.command.DraftTeamMember;
import org.wonik.coreviaai_be.domain.wbs.service.command.GenerateWbsDraftCommand;
import org.wonik.coreviaai_be.domain.wbs.service.command.WbsDraftContext;
import org.wonik.global.user.entity.User;
import org.wonik.global.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * AI 초안 생성 입력을 한 번의 읽기 트랜잭션으로 모은다. AI 호출(최대 수십 초)을 트랜잭션 밖에서 하기 위해
 * 생성 서비스에서 분리했다(be-principles: 긴 트랜잭션 안에서 외부 호출 금지).
 * <p>
 * 프로젝트 기간은 선택 입력이라(FR-PRJ-01) 비어 있을 수 있고, 그대로 aiserver에 넘긴다.
 */
@Component
@RequiredArgsConstructor
public class WbsDraftContextReader {

	private final WbsDraftAccessPolicy draftAccessPolicy;
	private final ProjectPhaseRepository phaseRepository;
	private final ProjectMemberRepository memberRepository;
	private final UserRepository userRepository;
	private final WbsTaskRepository taskRepository;

	@Transactional(readOnly = true)
	public WbsDraftContext read(final Long userId, final Long projectId, final GenerateWbsDraftCommand command) {
		final Project project = draftAccessPolicy.getDraftableProject(userId, projectId);

		final List<DraftExistingTask> existingTasks =
			taskRepository.findAllByProjectIdAndParentTaskIdIsNullOrderBySortOrderAscIdAsc(projectId).stream()
				.map(task -> new DraftExistingTask(task.getId(), task.getName(), task.getStatus()))
				.toList();

		final List<ProjectMember> members = memberRepository.findAllByProjectIdOrderById(projectId);
		final Map<Long, String> names = userRepository.findAllById(members.stream().map(ProjectMember::getUserId).toList())
			.stream()
			.collect(Collectors.toMap(User::getId, User::getName));
		final List<DraftTeamMember> teamMembers = members.stream()
			.map(member -> new DraftTeamMember(member.getUserId(), names.get(member.getUserId()), member.getActor()))
			.toList();

		return new WbsDraftContext(
			project.getId(),
			project.getName(),
			project.getStartDate(),
			project.getEndDate(),
			phaseRepository.findAllByProjectIdOrderByPhaseOrder(projectId).stream()
				.map(ProjectPhase::getPhaseType)
				.toList(),
			existingTasks,
			teamMembers,
			command
		);
	}
}

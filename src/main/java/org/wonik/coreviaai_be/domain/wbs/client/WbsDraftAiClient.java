package org.wonik.coreviaai_be.domain.wbs.client;

import java.util.List;
import java.util.function.Function;

import org.springframework.stereotype.Component;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftMilestone;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftResult;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftStage;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftSubtask;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftTask;
import org.wonik.aiserver.domain.wbs.api.AiWbsExistingTask;
import org.wonik.aiserver.domain.wbs.api.AiWbsGenerateRequest;
import org.wonik.aiserver.domain.wbs.api.AiWbsProjectContext;
import org.wonik.aiserver.domain.wbs.api.AiWbsStage;
import org.wonik.aiserver.domain.wbs.api.AiWbsTeamMember;
import org.wonik.aiserver.domain.wbs.api.WbsDraftGenerator;
import org.wonik.coreviaai_be.domain.wbs.service.command.DraftProjectPhase;
import org.wonik.coreviaai_be.domain.wbs.service.command.GenerateWbsDraftCommand;
import org.wonik.coreviaai_be.domain.wbs.service.command.WbsDraftContext;
import org.wonik.coreviaai_be.domain.wbs.service.result.DraftFeasibility;
import org.wonik.coreviaai_be.domain.wbs.service.result.ProposedMilestone;
import org.wonik.coreviaai_be.domain.wbs.service.result.ProposedStage;
import org.wonik.coreviaai_be.domain.wbs.service.result.ProposedSubtask;
import org.wonik.coreviaai_be.domain.wbs.service.result.ProposedTask;
import org.wonik.coreviaai_be.domain.wbs.service.result.WbsDraftProposal;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * aiserver의 WBS 초안 생성 계약({@link WbsDraftGenerator}, FR-PRJ-07) 호출. 도메인 입력을 aiserver 계약 타입으로,
 * 생성 결과를 도메인 결과로 바꾸는 경계라서 서비스는 aiserver 타입을 알지 않는다.
 * <p>
 * 결과의 단계 코드가 프로젝트 단계가 아니면 형식 오류(502)로 본다. 그 밖의 값 검증(기간·순환 등)은
 * aiserver가 하고, PM이 수정한 값은 반영 API에서 다시 검증한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WbsDraftAiClient {

	private final WbsDraftGenerator draftGenerator;

	public WbsDraftProposal generate(final WbsDraftContext context) {
		final AiWbsDraftResult result = draftGenerator.generate(toRequest(context));
		return toProposal(result, context.phases());
	}

	private static AiWbsGenerateRequest toRequest(final WbsDraftContext context) {
		final GenerateWbsDraftCommand command = context.command();
		final List<DraftProjectPhase> phases = context.phases();
		return new AiWbsGenerateRequest(
			context.projectId(),
			context.projectName(),
			command.projectPurpose(),
			command.requirementsText(),
			new AiWbsProjectContext(context.startDate(), context.endDate(), context.methodology().name(),
				command.constraints()),
			phases.stream()
				.map(phase -> new AiWbsStage(phase.phaseId(), phase.name(), phase.order()))
				.toList(),
			context.existingTasks().stream()
				.map(task -> new AiWbsExistingTask(task.taskId(), task.name(), null, task.status().name()))
				.toList(),
			context.members().stream()
				.map(member -> new AiWbsTeamMember(member.userId(), member.name(), member.actor().name()))
				.toList(),
			command.userInstruction()
		);
	}

	private static WbsDraftProposal toProposal(final AiWbsDraftResult result, final List<DraftProjectPhase> phases) {
		return new WbsDraftProposal(
			map(result.stages(), stage -> toStage(stage, phases)),
			map(result.milestones(), WbsDraftAiClient::toMilestone),
			result.feasibility() == null ? null
				: new DraftFeasibility(result.feasibility().status(), result.feasibility().reason()),
			orEmpty(result.warnings()),
			orEmpty(result.suggestions()),
			orEmpty(result.assumptions()),
			orEmpty(result.clarificationQuestions())
		);
	}

	private static ProposedStage toStage(final AiWbsDraftStage stage, final List<DraftProjectPhase> phases) {
		final DraftProjectPhase phase = phases.stream()
			.filter(candidate -> candidate.phaseId().equals(stage.phaseId()))
			.findFirst()
			.orElseThrow(() -> {
				log.warn("AI 초안에 프로젝트에 없는 단계가 있습니다: {}", stage.phaseId());
				return new CustomException(ErrorCode.AI_GENERATION_FAILED);
			});
		return new ProposedStage(phase.phaseId(), phase.name(), map(stage.tasks(), WbsDraftAiClient::toTask));
	}

	private static ProposedTask toTask(final AiWbsDraftTask task) {
		return new ProposedTask(
			task.tempId(),
			task.name(),
			task.description(),
			task.estimatedPersonDays(),
			task.suggestedAssigneeId(),
			task.suggestedRole(),
			task.startDate(),
			task.endDate(),
			orEmpty(task.dependencies()),
			map(task.subtasks(), WbsDraftAiClient::toSubtask),
			task.reason()
		);
	}

	private static ProposedSubtask toSubtask(final AiWbsDraftSubtask subtask) {
		return new ProposedSubtask(subtask.tempId(), subtask.name(), subtask.description(), subtask.estimatedPersonDays());
	}

	private static ProposedMilestone toMilestone(final AiWbsDraftMilestone milestone) {
		return new ProposedMilestone(
			milestone.tempId(),
			milestone.name(),
			milestone.targetDate(),
			orEmpty(milestone.relatedTaskIds()),
			milestone.reason()
		);
	}

	private static <S, T> List<T> map(final List<S> source, final Function<S, T> mapper) {
		return source == null ? List.of() : source.stream().map(mapper).toList();
	}

	private static List<String> orEmpty(final List<String> values) {
		return values == null ? List.of() : values;
	}
}

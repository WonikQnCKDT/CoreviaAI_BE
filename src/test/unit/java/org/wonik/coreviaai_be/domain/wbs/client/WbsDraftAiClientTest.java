package org.wonik.coreviaai_be.domain.wbs.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftMilestone;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftResult;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftStage;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftSubtask;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftTask;
import org.wonik.aiserver.domain.wbs.api.AiWbsFeasibility;
import org.wonik.aiserver.domain.wbs.api.AiWbsGenerateRequest;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;
import org.wonik.coreviaai_be.domain.wbs.service.command.DraftExistingTask;
import org.wonik.coreviaai_be.domain.wbs.service.command.DraftProjectPhase;
import org.wonik.coreviaai_be.domain.wbs.service.command.DraftTeamMember;
import org.wonik.coreviaai_be.domain.wbs.service.command.GenerateWbsDraftCommand;
import org.wonik.coreviaai_be.domain.wbs.service.command.WbsDraftContext;
import org.wonik.coreviaai_be.domain.wbs.service.result.WbsDraftProposal;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

/**
 * 업무 모듈 ↔ aiserver 계약 변환(FR-PRJ-07). 요청에 넣는 값과 결과 해석, 프로젝트에 없는 단계 거부를 확인한다.
 */
class WbsDraftAiClientTest {

	private static final List<DraftProjectPhase> PHASES = List.of(
		new DraftProjectPhase(101L, "분석", 1),
		new DraftProjectPhase(102L, "설계", 2)
	);

	@Test
	@DisplayName("요청: 프로젝트 문맥·단계 순서·기존 작업(단계 null)·멤버를 계약 필드로 옮긴다. 기간이 없으면 null")
	void toRequest() {
		final List<AiWbsGenerateRequest> captured = new ArrayList<>();
		final WbsDraftAiClient client = new WbsDraftAiClient(request -> {
			captured.add(request);
			return emptyResult();
		});

		client.generate(context(null, null));

		final AiWbsGenerateRequest request = captured.get(0);
		assertThat(request.projectId()).isEqualTo(1L);
		assertThat(request.requirementsText()).isEqualTo("발주 등록");
		assertThat(request.projectContext().startDate()).isNull();
		assertThat(request.projectContext().endDate()).isNull();
		assertThat(request.projectContext().methodology()).isEqualTo("WATERFALL");
		assertThat(request.existingStages()).extracting("phaseId", "name", "order")
			.startsWith(tuple(101L, "분석", 1));
		assertThat(request.existingTasks()).singleElement()
			.satisfies(task -> {
				assertThat(task.phaseId()).isNull();
				assertThat(task.status()).isEqualTo("TODO");
			});
		assertThat(request.teamMembers()).singleElement()
			.satisfies(member -> assertThat(member.role()).isEqualTo("DEVELOPER"));
	}

	@Test
	@DisplayName("결과: 단계·작업·하위작업·마일스톤·실현 가능성을 도메인 결과로 바꾸고, 빠진 목록은 빈 목록으로 채운다")
	void toProposal() {
		final AiWbsDraftTask task = new AiWbsDraftTask("T-001", "분석", null, BigDecimal.ONE, null, null,
			LocalDate.parse("2026-12-01"), LocalDate.parse("2026-12-05"), null,
			List.of(new AiWbsDraftSubtask("ST-001", "목록 정리", null, null)), "이유");
		final WbsDraftAiClient client = new WbsDraftAiClient(request -> new AiWbsDraftResult(1L,
			List.of(new AiWbsDraftStage(101L, List.of(task))),
			List.of(new AiWbsDraftMilestone("M-001", "분석 완료", LocalDate.parse("2026-12-05"), List.of("T-001"), null)),
			new AiWbsFeasibility("FEASIBLE", "충분"), null, null, null, null));

		final WbsDraftProposal proposal = client.generate(context(LocalDate.parse("2026-12-01"), null));

		assertThat(proposal.stages()).singleElement()
			.satisfies(stage -> {
				assertThat(stage.phaseId()).isEqualTo(101L);
				assertThat(stage.phaseName()).isEqualTo("분석");
				assertThat(stage.tasks()).singleElement().satisfies(proposed -> {
					assertThat(proposed.dependencies()).isEmpty();
					assertThat(proposed.subtasks()).hasSize(1);
				});
			});
		assertThat(proposal.milestones()).singleElement()
			.satisfies(milestone -> assertThat(milestone.relatedTaskIds()).containsExactly("T-001"));
		assertThat(proposal.feasibility().status()).isEqualTo("FEASIBLE");
		assertThat(proposal.warnings()).isEmpty();
	}

	@Test
	@DisplayName("결과에 프로젝트에 없는 단계가 있으면 502 AI_GENERATION_FAILED")
	void unknownStage() {
		final WbsDraftAiClient client = new WbsDraftAiClient(request -> new AiWbsDraftResult(1L,
			List.of(new AiWbsDraftStage(999L, List.of())), List.of(), null, null, null, null, null));

		assertThatThrownBy(() -> client.generate(context(null, null)))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode").isEqualTo(ErrorCode.AI_GENERATION_FAILED);
	}

	@Test
	@DisplayName("aiserver 오류는 그대로 전달한다")
	void generatorFailure() {
		final WbsDraftAiClient client = new WbsDraftAiClient(request -> {
			throw new CustomException(ErrorCode.AI_NOT_READY);
		});

		assertThatThrownBy(() -> client.generate(context(null, null)))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode").isEqualTo(ErrorCode.AI_NOT_READY);
	}

	private static WbsDraftContext context(final LocalDate startDate, final LocalDate endDate) {
		return new WbsDraftContext(1L, "구매 시스템", startDate, endDate, Methodology.WATERFALL, PHASES,
			List.of(new DraftExistingTask(10L, "기존 작업", WbsTaskStatus.TODO)),
			List.of(new DraftTeamMember(11L, "개발자", ProjectActor.DEVELOPER)),
			new GenerateWbsDraftCommand(null, "발주 등록", List.of(), null));
	}

	private static AiWbsDraftResult emptyResult() {
		return new AiWbsDraftResult(1L, List.of(), List.of(), null, List.of(), List.of(), List.of(), List.of());
	}
}

package org.wonik.aiserver.domain.wbs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftMilestone;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftResult;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftStage;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftSubtask;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftTask;
import org.wonik.aiserver.domain.wbs.api.AiWbsFeasibility;

class WbsDraftResultNormalizerTest {

	private final WbsDraftResultNormalizer normalizer = new WbsDraftResultNormalizer();

	@ParameterizedTest(name = "{0} -> {1}")
	@CsvSource({"3.25,3.3", "3.24,3.2", "0.05,0.1", "0.04,0.0", "5,5.0", "3.5,3.5", "0.1,0.1", "2.96,3.0"})
	@DisplayName("작업과 하위작업의 공수를 소수 첫째 자리로 반올림한다")
	void roundsToOneDecimal(final String input, final String expected) {
		AiWbsDraftSubtask subtask = new AiWbsDraftSubtask("ST-001", "하위", "설명", new BigDecimal(input));
		AiWbsDraftResult result = resultOf(task(new BigDecimal(input), List.of(subtask)));

		AiWbsDraftResult normalized = normalizer.normalize(result);

		AiWbsDraftTask task = normalized.stages().get(0).tasks().get(0);
		assertThat(task.estimatedPersonDays()).isEqualTo(new BigDecimal(expected));
		assertThat(task.subtasks().get(0).estimatedPersonDays()).isEqualTo(new BigDecimal(expected));
	}

	@Test
	@DisplayName("공수가 없으면(null) 그대로 둔다")
	void nullPersonDaysStayNull() {
		AiWbsDraftSubtask subtask = new AiWbsDraftSubtask("ST-001", "하위", "설명", null);
		AiWbsDraftResult normalized = normalizer.normalize(resultOf(task(null, List.of(subtask))));

		AiWbsDraftTask task = normalized.stages().get(0).tasks().get(0);
		assertThat(task.estimatedPersonDays()).isNull();
		assertThat(task.subtasks().get(0).estimatedPersonDays()).isNull();
	}

	@Test
	@DisplayName("공수 외의 값은 그대로이고 원본은 바뀌지 않는다")
	void keepsOtherFieldsAndDoesNotMutateOriginal() {
		AiWbsDraftResult original = resultOf(task(new BigDecimal("3.25"), List.of()));

		AiWbsDraftResult normalized = normalizer.normalize(original);

		assertThat(normalized.projectId()).isEqualTo(7L);
		assertThat(normalized.stages().get(0).phaseId()).isEqualTo(1L);
		AiWbsDraftTask task = normalized.stages().get(0).tasks().get(0);
		assertThat(task.tempId()).isEqualTo("T-001");
		assertThat(task.name()).isEqualTo("작업");
		assertThat(task.description()).isEqualTo("설명");
		assertThat(task.suggestedAssigneeId()).isEqualTo(11L);
		assertThat(task.suggestedRole()).isEqualTo("DEVELOPER");
		assertThat(task.startDate()).isEqualTo(LocalDate.of(2026, 12, 1));
		assertThat(task.endDate()).isEqualTo(LocalDate.of(2026, 12, 2));
		assertThat(task.dependencies()).containsExactly("T-000");
		assertThat(task.reason()).isEqualTo("이유");
		assertThat(normalized.milestones()).isEqualTo(original.milestones());
		assertThat(normalized.feasibility()).isEqualTo(original.feasibility());
		assertThat(normalized.warnings()).containsExactly("w");
		assertThat(normalized.suggestions()).containsExactly("s");
		assertThat(normalized.assumptions()).containsExactly("a");
		assertThat(normalized.clarificationQuestions()).containsExactly("q");

		// 원본은 그대로
		assertThat(original.stages().get(0).tasks().get(0).estimatedPersonDays()).isEqualTo(new BigDecimal("3.25"));
	}

	@Test
	@DisplayName("결과가 null이거나 안에 null이 섞여 있어도 오류 없이 처리한다")
	void toleratesNulls() {
		assertThat(normalizer.normalize(null)).isNull();

		AiWbsDraftResult withNulls = new AiWbsDraftResult(7L,
			Collections.singletonList(null), List.of(), null, List.of(), List.of(), List.of(), List.of());
		assertThatCode(() -> normalizer.normalize(withNulls)).doesNotThrowAnyException();

		AiWbsDraftResult nullTask = new AiWbsDraftResult(7L,
			List.of(new AiWbsDraftStage(1L, Collections.singletonList(null))), List.of(), null, List.of(), List.of(),
			List.of(), List.of());
		assertThatCode(() -> normalizer.normalize(nullTask)).doesNotThrowAnyException();

		AiWbsDraftResult nullStages = new AiWbsDraftResult(7L, null, List.of(), null, List.of(), List.of(),
			List.of(), List.of());
		assertThat(normalizer.normalize(nullStages).stages()).isNull();
	}

	private static AiWbsDraftTask task(final BigDecimal days, final List<AiWbsDraftSubtask> subtasks) {
		return new AiWbsDraftTask("T-001", "작업", "설명", days, 11L, "DEVELOPER", LocalDate.of(2026, 12, 1),
			LocalDate.of(2026, 12, 2), List.of("T-000"), subtasks, "이유");
	}

	private static AiWbsDraftResult resultOf(final AiWbsDraftTask task) {
		AiWbsDraftMilestone milestone = new AiWbsDraftMilestone("M-001", "마일스톤", LocalDate.of(2026, 12, 2),
			List.of("T-001"), "이유");
		return new AiWbsDraftResult(7L, List.of(new AiWbsDraftStage(1L, List.of(task))), List.of(milestone),
			new AiWbsFeasibility("FEASIBLE", "ok"), List.of("w"), List.of("s"), List.of("a"), List.of("q"));
	}
}

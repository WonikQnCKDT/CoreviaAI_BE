package org.wonik.aiserver.domain.wbs.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftMilestone;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftResult;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftStage;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftSubtask;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftTask;
import org.wonik.aiserver.domain.wbs.api.AiWbsFeasibility;
import org.wonik.aiserver.domain.wbs.api.AiWbsGenerateRequest;
import org.wonik.aiserver.domain.wbs.api.AiWbsProjectContext;
import org.wonik.aiserver.domain.wbs.api.AiWbsStage;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

class WbsDraftResultValidatorTest {

	private final WbsDraftResultValidator validator = new WbsDraftResultValidator();

	// ---------- 정상 ----------

	@Test
	@DisplayName("규칙에 맞는 정상 초안은 통과한다")
	void validDraftPasses() {
		assertAccepted(request(), validResult());
	}

	@Test
	@DisplayName("프로젝트 기간이 없어도(기간 null, 컨텍스트 null) 통과한다")
	void noProjectPeriodPasses() {
		assertAccepted(request(null, null), validResult());

		AiWbsGenerateRequest noContext = new AiWbsGenerateRequest(7L, "이름", "목적", null, null,
			List.of(new AiWbsStage(1L, "요구분석", 1)), List.of(), List.of(), null);
		assertAccepted(noContext, validResult());
	}

	@Test
	@DisplayName("마름모 모양 선행 관계(순환 아님)는 통과한다")
	void diamondDependenciesPass() {
		AiWbsDraftResult result = resultOfTasks(
			task("T-001", "2026-12-01", "2026-12-02"),
			task("T-002", "2026-12-03", "2026-12-04", "T-001"),
			task("T-003", "2026-12-03", "2026-12-04", "T-001"),
			task("T-004", "2026-12-05", "2026-12-06", "T-002", "T-003"));

		assertAccepted(request(), result);
	}

	// ---------- 빈 초안 ----------

	@Test
	@DisplayName("정보 부족이면 빈 초안도 통과하고, 아니면 거절한다")
	void emptyDraft() {
		AiWbsFeasibility insufficient = new AiWbsFeasibility("INSUFFICIENT_INFO", "정보 부족");
		assertAccepted(request(), result(List.of(), List.of(), insufficient));
		assertAccepted(request(), new AiWbsDraftResult(7L, List.of(), List.of(), insufficient, List.of(), List.of(),
			List.of(), List.of("질문")));

		assertRejected(result(List.of(), List.of(), new AiWbsFeasibility("FEASIBLE", "ok")));
		assertRejected(result(List.of(), List.of(), null));
	}

	@Test
	@DisplayName("요청이나 결과가 null이면 거절한다")
	void nullInputs() {
		assertRejected(null, validResult());
		assertRejected(request(), null);
		assertRejected(request(), new AiWbsDraftResult(7L, null, List.of(), null, List.of(), List.of(), List.of(),
			List.of()));
	}

	// ---------- 단계 ----------

	@Test
	@DisplayName("요청에 없는 단계, null 단계, phaseId가 없는 단계는 거절한다")
	void phaseRules() {
		assertRejected(new AiWbsDraftResult(7L,
			List.of(new AiWbsDraftStage(99L, List.of(task("T-001", "2026-12-01", "2026-12-02")))),
			List.of(), null, List.of(), List.of(), List.of(), List.of()));
		assertRejected(new AiWbsDraftResult(7L,
			List.of(new AiWbsDraftStage(null, List.of(task("T-001", "2026-12-01", "2026-12-02")))),
			List.of(), null, List.of(), List.of(), List.of(), List.of()));
		assertRejected(new AiWbsDraftResult(7L, Collections.singletonList(null), List.of(), null, List.of(),
			List.of(), List.of(), List.of()));
	}

	// ---------- tempId ----------

	@Test
	@DisplayName("tempId 중복(작업끼리, 작업과 마일스톤, 작업과 하위작업)과 잘못된 형식은 거절한다")
	void tempIdRules() {
		assertRejected(resultOfTasks(task("T-001", "2026-12-01", "2026-12-02"),
			task("T-001", "2026-12-03", "2026-12-04")));

		assertRejected(result(List.of(task("T-001", "2026-12-01", "2026-12-02")),
			List.of(milestone("T-001", "2026-12-02", "T-001")), null));

		assertRejected(resultOfTasks(taskOf("T-001", "작업", new BigDecimal("1.0"), date("2026-12-01"),
			date("2026-12-02"), List.of(), List.of(subtask("T-001")))));

		assertRejected(resultOfTasks(task(" ", "2026-12-01", "2026-12-02")));
		assertRejected(resultOfTasks(task(null, "2026-12-01", "2026-12-02")));
		assertRejected(resultOfTasks(task("T".repeat(51), "2026-12-01", "2026-12-02")));
		assertAccepted(request(), resultOfTasks(task("T".repeat(50), "2026-12-01", "2026-12-02")));
	}

	// ---------- 글자 수 ----------

	@Test
	@DisplayName("이름·설명 길이 경계(이름 200자, 설명 2000자)를 지킨다")
	void textLengthBoundaries() {
		assertAccepted(request(), resultOfTasks(taskNamed("가".repeat(200), "나".repeat(2000))));
		assertRejected(resultOfTasks(taskNamed("가".repeat(201), "설명")));
		assertRejected(resultOfTasks(taskNamed("작업", "나".repeat(2001))));
		assertRejected(resultOfTasks(taskNamed("  ", "설명")));
		assertRejected(resultOfTasks(taskNamed(null, "설명")));
	}

	// ---------- 공수 ----------

	@Test
	@DisplayName("공수는 없어도 되고, 있으면 0.1 이상이어야 한다")
	void personDays() {
		assertAccepted(request(), resultOfTasks(taskDays("0.1")));
		assertAccepted(request(), resultOfTasks(taskDays(null)));
		assertRejected(resultOfTasks(taskDays("0.09")));
		assertRejected(resultOfTasks(taskDays("0")));
		assertRejected(resultOfTasks(taskDays("-1")));

		AiWbsDraftSubtask badSubtask = new AiWbsDraftSubtask("ST-001", "하위", "설명", new BigDecimal("0.09"));
		assertRejected(resultOfTasks(taskOf("T-001", "작업", new BigDecimal("1.0"), date("2026-12-01"),
			date("2026-12-02"), List.of(), List.of(badSubtask))));
	}

	// ---------- 날짜 ----------

	@Test
	@DisplayName("시작일이 종료일보다 늦거나 날짜가 비면 거절하고, 같은 날은 통과한다")
	void dateRules() {
		assertAccepted(request(), resultOfTasks(task("T-001", "2026-12-05", "2026-12-05")));
		assertRejected(resultOfTasks(task("T-001", "2026-12-06", "2026-12-05")));
		assertRejected(resultOfTasks(taskOf("T-001", "작업", new BigDecimal("1.0"), null, date("2026-12-05"),
			List.of(), List.of())));
		assertRejected(resultOfTasks(taskOf("T-001", "작업", new BigDecimal("1.0"), date("2026-12-05"), null,
			List.of(), List.of())));
	}

	@Test
	@DisplayName("프로젝트 기간이 있으면 그 안이어야 하고, 시작·종료 중 있는 쪽만 확인한다")
	void periodRules() {
		// 기간: 2026-12-01 ~ 2026-12-31
		assertAccepted(request(), resultOfTasks(task("T-001", "2026-12-01", "2026-12-31")));
		assertRejected(resultOfTasks(task("T-001", "2026-11-30", "2026-12-05")));
		assertRejected(resultOfTasks(task("T-001", "2026-12-05", "2027-01-01")));

		// 시작만 있는 경우: 끝은 제한이 없다
		AiWbsGenerateRequest onlyStart = request(date("2026-12-01"), null);
		assertAccepted(onlyStart, resultOfTasks(task("T-001", "2030-01-01", "2030-12-31")));
		assertRejected(onlyStart, resultOfTasks(task("T-001", "2026-11-30", "2026-12-05")));

		// 끝만 있는 경우: 시작은 제한이 없다
		AiWbsGenerateRequest onlyEnd = request(null, date("2026-12-31"));
		assertAccepted(onlyEnd, resultOfTasks(task("T-001", "2020-01-01", "2026-12-31")));
		assertRejected(onlyEnd, resultOfTasks(task("T-001", "2026-12-05", "2027-01-01")));
	}

	// ---------- 선행 작업 ----------

	@Test
	@DisplayName("없는 선행 작업, 자기 자신, 하위작업을 선행으로 지정하면 거절한다")
	void dependencyReferences() {
		assertRejected(resultOfTasks(task("T-001", "2026-12-01", "2026-12-02", "T-999")));
		assertRejected(resultOfTasks(task("T-001", "2026-12-01", "2026-12-02", "T-001")));

		AiWbsDraftTask parent = taskOf("T-001", "작업", new BigDecimal("1.0"), date("2026-12-01"),
			date("2026-12-02"), List.of(), List.of(subtask("ST-001")));
		assertRejected(resultOfTasks(parent, task("T-002", "2026-12-03", "2026-12-04", "ST-001")));
	}

	@Test
	@DisplayName("선행 작업이 순환하면(2개, 3개) 거절한다")
	void dependencyCycles() {
		assertRejected(resultOfTasks(
			task("T-001", "2026-12-01", "2026-12-02", "T-002"),
			task("T-002", "2026-12-03", "2026-12-04", "T-001")));

		assertRejected(resultOfTasks(
			task("T-001", "2026-12-01", "2026-12-02", "T-003"),
			task("T-002", "2026-12-03", "2026-12-04", "T-001"),
			task("T-003", "2026-12-05", "2026-12-06", "T-002")));
	}

	// ---------- 마일스톤 ----------

	@Test
	@DisplayName("마일스톤은 실제 작업을 가리켜야 하고 날짜가 있고 기간 안이어야 한다")
	void milestoneRules() {
		List<AiWbsDraftTask> tasks = List.of(taskOf("T-001", "작업", new BigDecimal("1.0"), date("2026-12-01"),
			date("2026-12-02"), List.of(), List.of(subtask("ST-001"))));

		assertAccepted(request(), result(tasks, List.of(milestone("M-001", "2026-12-31", "T-001")), null));
		assertAccepted(request(), result(tasks, List.of(milestone("M-001", "2026-12-01")), null));

		assertRejected(result(tasks, List.of(milestone("M-001", "2026-12-02", "T-999")), null));
		assertRejected(result(tasks, List.of(milestone("M-001", "2026-12-02", "ST-001")), null));
		assertRejected(result(tasks, List.of(milestone("M-001", null, "T-001")), null));
		assertRejected(result(tasks, List.of(milestone("M-001", "2026-11-30", "T-001")), null));
		assertRejected(result(tasks, List.of(milestone("M-001", "2027-01-01", "T-001")), null));
	}

	// ---------- 개수 상한 ----------

	@Test
	@DisplayName("작업 200개, 하위작업 작업당 50개, 마일스톤 50개까지 통과하고 하나 더 많으면 거절한다")
	void countLimits() {
		assertAccepted(request(), resultOfTasks(tasksOf(200)));
		assertRejected(resultOfTasks(tasksOf(201)));

		assertAccepted(request(), resultOfTasks(taskWithSubtasks(50)));
		assertRejected(resultOfTasks(taskWithSubtasks(51)));

		List<AiWbsDraftTask> base = List.of(task("T-001", "2026-12-01", "2026-12-02"));
		assertAccepted(request(), result(base, milestonesOf(50), null));
		assertRejected(result(base, milestonesOf(51), null));
	}

	// ---------- 판단 ----------

	@Test
	@DisplayName("판단 상태는 4가지 중 하나여야 하고, 판단 자체는 없어도 된다")
	void feasibilityRules() {
		List<AiWbsDraftTask> tasks = List.of(task("T-001", "2026-12-01", "2026-12-02"));

		for (String status : List.of("FEASIBLE", "AT_RISK", "INFEASIBLE", "INSUFFICIENT_INFO")) {
			assertAccepted(request(), result(tasks, List.of(), new AiWbsFeasibility(status, "사유")));
		}
		assertAccepted(request(), result(tasks, List.of(), null));
		assertRejected(result(tasks, List.of(), new AiWbsFeasibility("MAYBE", "사유")));
		assertRejected(result(tasks, List.of(), new AiWbsFeasibility(null, "사유")));
	}

	// ---------- 원문 미노출 ----------

	@Test
	@DisplayName("오류 메시지와 원인에 AI가 쓴 내용이 남지 않는다")
	void errorDoesNotLeakContent() {
		String secret = "SECRET-TEMP-ID";
		AiWbsDraftResult result = resultOfTasks(task(secret, "2026-12-01", "2026-12-02"),
			task(secret, "2026-12-03", "2026-12-04"));

		assertThatThrownBy(() -> validator.validate(request(), result))
			.isInstanceOfSatisfying(CustomException.class, e -> {
				assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AI_GENERATION_FAILED);
				assertThat(e.getMessage()).doesNotContain(secret);
				assertThat(e.getCause()).isNull();
			});
	}

	// ---------- 도우미 ----------

	private void assertAccepted(final AiWbsGenerateRequest request, final AiWbsDraftResult result) {
		assertThatCode(() -> validator.validate(request, result)).doesNotThrowAnyException();
	}

	private void assertRejected(final AiWbsGenerateRequest request, final AiWbsDraftResult result) {
		assertThatThrownBy(() -> validator.validate(request, result))
			.isInstanceOfSatisfying(CustomException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AI_GENERATION_FAILED));
	}

	private void assertRejected(final AiWbsDraftResult result) {
		assertRejected(request(), result);
	}

	private static LocalDate date(final String value) {
		return LocalDate.parse(value);
	}

	/** 단계 1, 2가 있고 프로젝트 기간이 start~end인 요청. */
	private static AiWbsGenerateRequest request(final LocalDate start, final LocalDate end) {
		return new AiWbsGenerateRequest(7L, "이름", "목적", null,
			new AiWbsProjectContext(start, end, "WATERFALL", List.of()),
			List.of(new AiWbsStage(1L, "요구분석", 1), new AiWbsStage(2L, "설계", 2)),
			List.of(), List.of(), null);
	}

	/** 기간 2026-12-01 ~ 2026-12-31. */
	private static AiWbsGenerateRequest request() {
		return request(date("2026-12-01"), date("2026-12-31"));
	}

	private static AiWbsDraftTask task(final String id, final String start, final String end,
		final String... dependencies) {
		return taskOf(id, "작업", new BigDecimal("1.0"), date(start), date(end), List.of(dependencies), List.of());
	}

	private static AiWbsDraftTask taskNamed(final String name, final String description) {
		return new AiWbsDraftTask("T-001", name, description, new BigDecimal("1.0"), null, null,
			date("2026-12-01"), date("2026-12-02"), List.of(), List.of(), "이유");
	}

	private static AiWbsDraftTask taskDays(final String days) {
		return taskOf("T-001", "작업", days == null ? null : new BigDecimal(days), date("2026-12-01"),
			date("2026-12-02"), List.of(), List.of());
	}

	private static AiWbsDraftTask taskOf(final String id, final String name, final BigDecimal days,
		final LocalDate start, final LocalDate end, final List<String> dependencies,
		final List<AiWbsDraftSubtask> subtasks) {
		return new AiWbsDraftTask(id, name, "설명", days, null, null, start, end, dependencies, subtasks, "이유");
	}

	private static AiWbsDraftSubtask subtask(final String id) {
		return new AiWbsDraftSubtask(id, "하위작업", "설명", new BigDecimal("0.5"));
	}

	private static AiWbsDraftMilestone milestone(final String id, final String targetDate, final String... taskIds) {
		return new AiWbsDraftMilestone(id, "마일스톤", targetDate == null ? null : date(targetDate),
			List.of(taskIds), "이유");
	}

	private static List<AiWbsDraftTask> tasksOf(final int count) {
		return IntStream.rangeClosed(1, count)
			.mapToObj(i -> task("T-" + i, "2026-12-01", "2026-12-02"))
			.toList();
	}

	private static AiWbsDraftTask taskWithSubtasks(final int count) {
		List<AiWbsDraftSubtask> subtasks = IntStream.rangeClosed(1, count)
			.mapToObj(i -> subtask("ST-" + i))
			.toList();
		return taskOf("T-001", "작업", new BigDecimal("1.0"), date("2026-12-01"), date("2026-12-02"), List.of(),
			subtasks);
	}

	private static List<AiWbsDraftMilestone> milestonesOf(final int count) {
		return IntStream.rangeClosed(1, count)
			.mapToObj(i -> milestone("M-" + i, "2026-12-10", "T-001"))
			.toList();
	}

	/** 단계 1에 작업들을 넣은 결과. */
	private static AiWbsDraftResult result(final List<AiWbsDraftTask> tasks,
		final List<AiWbsDraftMilestone> milestones, final AiWbsFeasibility feasibility) {
		return new AiWbsDraftResult(7L, List.of(new AiWbsDraftStage(1L, tasks)), milestones, feasibility,
			List.of(), List.of(), List.of(), List.of());
	}

	private static AiWbsDraftResult resultOfTasks(final List<AiWbsDraftTask> tasks) {
		return result(tasks, List.of(), new AiWbsFeasibility("FEASIBLE", "ok"));
	}

	private static AiWbsDraftResult resultOfTasks(final AiWbsDraftTask... tasks) {
		return resultOfTasks(List.of(tasks));
	}

	private static AiWbsDraftResult validResult() {
		return result(
			List.of(task("T-001", "2026-12-01", "2026-12-05"), task("T-002", "2026-12-08", "2026-12-17", "T-001")),
			List.of(milestone("M-001", "2026-12-17", "T-002")),
			new AiWbsFeasibility("FEASIBLE", "ok"));
	}
}

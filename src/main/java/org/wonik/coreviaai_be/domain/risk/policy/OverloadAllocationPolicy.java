package org.wonik.coreviaai_be.domain.risk.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;

/**
 * 인력 편중 판정(FR-AIR-03). 규칙만 적용하고 LLM·AI 추정 공수는 쓰지 않는다.
 * <p>
 * 한 프로젝트 안에서 담당자별로 계산한다. 감지 행이 프로젝트에 속하므로(risk_detections.project_id)
 * 다른 프로젝트 공수는 합치지 않는다. 기간은 그 담당자의 대상 작업 시작일 최소~종료일 최대다.
 * 대상 작업은 미완료 리프(하위작업이 있는 작업은 제외해 상위·하위 공수를 두 번 더하지 않음)이고,
 * 0보다 큰 확정 예상공수만 더한다.
 * 가용 인일 = 그 기간의 월~금 일수 × 투입률. 투입률 컬럼(FR-PRJ-11)이 없어 미설정 100%를 쓴다.
 * 공휴일 달력은 스키마에 없으므로 주말만 뺀다. 비율이 임계값을 초과할 때만 편중이다(같으면 아님).
 */
public final class OverloadAllocationPolicy {

	/** FR-PRJ-11 투입률이 생기기 전까지의 미설정 값. */
	public static final int UNSET_ALLOCATION_RATE_PERCENT = 100;

	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
	private static final int PERCENT_SCALE = 2;

	private OverloadAllocationPolicy() {
	}

	public record TaskLoad(
		Long assigneeUserId,
		Long taskId,
		Long parentTaskId,
		WbsTaskStatus status,
		BigDecimal estimatedEffort,
		LocalDate startDate,
		LocalDate endDate
	) {
	}

	public record Hit(
		Long userId,
		BigDecimal allocationPercent,
		BigDecimal allocatedEffort,
		int availableDays,
		String detailsJson
	) {
	}

	public static List<Hit> evaluate(final List<TaskLoad> tasks, final int thresholdPercent) {
		if (tasks == null || tasks.isEmpty() || thresholdPercent < 0) {
			return List.of();
		}
		final Set<Long> parentIds = new HashSet<>();
		for (final TaskLoad task : tasks) {
			if (task.parentTaskId() != null) {
				parentIds.add(task.parentTaskId());
			}
		}
		final Map<Long, List<TaskLoad>> byAssignee = new HashMap<>();
		for (final TaskLoad task : tasks) {
			if (!counts(task, parentIds)) {
				continue;
			}
			byAssignee.computeIfAbsent(task.assigneeUserId(), key -> new ArrayList<>()).add(task);
		}
		final List<Hit> hits = new ArrayList<>();
		for (final Map.Entry<Long, List<TaskLoad>> entry : byAssignee.entrySet()) {
			evaluateAssignee(entry.getKey(), entry.getValue(), thresholdPercent).ifPresent(hits::add);
		}
		hits.sort(Comparator.comparing(Hit::userId));
		return hits;
	}

	/** 기간의 시작·종료를 포함한 월~금 일수. 종료가 시작보다 앞이면 0. */
	static int weekdaysInclusive(final LocalDate start, final LocalDate end) {
		if (start == null || end == null || end.isBefore(start)) {
			return 0;
		}
		int days = 0;
		for (LocalDate cursor = start; !cursor.isAfter(end); cursor = cursor.plusDays(1)) {
			final DayOfWeek day = cursor.getDayOfWeek();
			if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
				days++;
			}
		}
		return days;
	}

	private static boolean counts(final TaskLoad task, final Set<Long> parentIds) {
		return task.assigneeUserId() != null
			&& task.status() != WbsTaskStatus.DONE
			&& task.taskId() != null
			&& !parentIds.contains(task.taskId())
			&& task.estimatedEffort() != null
			&& task.estimatedEffort().signum() > 0
			&& task.startDate() != null
			&& task.endDate() != null
			&& !task.endDate().isBefore(task.startDate());
	}

	private static java.util.Optional<Hit> evaluateAssignee(
		final Long userId,
		final List<TaskLoad> loads,
		final int thresholdPercent
	) {
		BigDecimal allocated = BigDecimal.ZERO;
		LocalDate periodStart = null;
		LocalDate periodEnd = null;
		for (final TaskLoad load : loads) {
			allocated = allocated.add(load.estimatedEffort());
			if (periodStart == null || load.startDate().isBefore(periodStart)) {
				periodStart = load.startDate();
			}
			if (periodEnd == null || load.endDate().isAfter(periodEnd)) {
				periodEnd = load.endDate();
			}
		}
		final int weekdays = weekdaysInclusive(periodStart, periodEnd);
		final BigDecimal available = BigDecimal.valueOf(weekdays)
			.multiply(BigDecimal.valueOf(UNSET_ALLOCATION_RATE_PERCENT))
			.divide(HUNDRED, PERCENT_SCALE, RoundingMode.HALF_UP);
		final BigDecimal percent = available.signum() == 0
			? null
			: allocated.multiply(HUNDRED).divide(available, PERCENT_SCALE, RoundingMode.HALF_UP);
		final boolean overloaded = percent == null
			? allocated.signum() > 0
			: percent.compareTo(BigDecimal.valueOf(thresholdPercent)) > 0;
		if (!overloaded) {
			return java.util.Optional.empty();
		}
		return java.util.Optional.of(new Hit(
			userId,
			percent,
			allocated,
			weekdays,
			detailsJson(allocated, weekdays, thresholdPercent)));
	}

	private static String detailsJson(final BigDecimal allocated, final int availableDays, final int thresholdPercent) {
		return "{\"allocatedEffort\":" + allocated.toPlainString()
			+ ",\"availableDays\":" + availableDays
			+ ",\"allocationRatePercent\":" + UNSET_ALLOCATION_RATE_PERCENT
			+ ",\"thresholdPercent\":" + thresholdPercent + "}";
	}
}

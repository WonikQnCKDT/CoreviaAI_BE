package org.wonik.coreviaai_be.domain.risk.service.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 인력 편중 조회 결과. scope가 DEPARTMENTS면 사람 단위 목록은 비운다(경영진은 부서 단위만).
 *
 * @param thresholdPercent 이 결과를 만들 때 쓴 임계값(퍼센트). 성공한 실행이 없으면 현재 설정값
 * @param runDate          성공한 계산일. 한 번도 성공하지 않았으면 null
 */
public record OverloadView(
	Scope scope,
	int thresholdPercent,
	LocalDate runDate,
	List<AssigneeLoad> assignees,
	List<DepartmentLoad> departments
) {

	public enum Scope {
		ASSIGNEES,
		DEPARTMENTS
	}

	public record AssigneeLoad(
		Long userId,
		String name,
		BigDecimal allocationPercent,
		BigDecimal allocatedEffort,
		int availableDays
	) {
	}

	public record DepartmentLoad(Long departmentId, String name, int overloadedCount) {
	}

	public static OverloadView empty(final Scope scope, final int thresholdPercent) {
		return new OverloadView(scope, thresholdPercent, null, List.of(), List.of());
	}
}

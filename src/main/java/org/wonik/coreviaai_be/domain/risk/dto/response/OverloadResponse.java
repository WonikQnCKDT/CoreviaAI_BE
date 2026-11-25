package org.wonik.coreviaai_be.domain.risk.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.wonik.coreviaai_be.domain.risk.service.result.OverloadView;

/**
 * 인력 편중 조회 응답. scope가 DEPARTMENTS이면 assignees는 빈 배열이다.
 */
public record OverloadResponse(
	String scope,
	int thresholdPercent,
	LocalDate runDate,
	List<AssigneeResponse> assignees,
	List<DepartmentResponse> departments
) {

	public record AssigneeResponse(
		Long userId,
		String name,
		BigDecimal allocationPercent,
		BigDecimal allocatedEffort,
		int availableDays
	) {
	}

	public record DepartmentResponse(Long departmentId, String name, int overloadedCount) {
	}

	public static OverloadResponse from(final OverloadView view) {
		return new OverloadResponse(
			view.scope().name(),
			view.thresholdPercent(),
			view.runDate(),
			view.assignees().stream()
				.map(row -> new AssigneeResponse(
					row.userId(), row.name(), row.allocationPercent(), row.allocatedEffort(), row.availableDays()))
				.toList(),
			view.departments().stream()
				.map(row -> new DepartmentResponse(row.departmentId(), row.name(), row.overloadedCount()))
				.toList());
	}
}

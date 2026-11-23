package org.wonik.coreviaai_be.domain.project.dto.response;

import java.util.Map;

import org.wonik.coreviaai_be.domain.project.entity.ProjectPhase;
import org.wonik.coreviaai_be.domain.project.entity.type.GateStatus;
import org.wonik.coreviaai_be.domain.project.entity.type.PhaseStatus;

/**
 * 단계 하나와 게이트 상태. 게이트 승인자가 미지정이면 gateApproverUserId·gateApproverName이 null이다.
 */
public record ProjectPhaseResponse(
	Long id,
	String name,
	int phaseOrder,
	int progress,
	PhaseStatus status,
	GateStatus gateStatus,
	Long gateApproverUserId,
	String gateApproverName
) {

	public static ProjectPhaseResponse of(final ProjectPhase phase, final Map<Long, String> userNames,
		final int progress) {
		final Long approverId = phase.getGateApproverUserId();
		return new ProjectPhaseResponse(
			phase.getId(),
			phase.getName(),
			phase.getPhaseOrder(),
			progress,
			phase.getStatus(),
			phase.getGateStatus(),
			approverId,
			approverId == null ? null : userNames.get(approverId)
		);
	}

	public static ProjectPhaseResponse of(final ProjectPhase phase, final Map<Long, String> userNames) {
		return of(phase, userNames, 0);
	}
}

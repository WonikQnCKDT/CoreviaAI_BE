package org.wonik.coreviaai_be.domain.project.dto.request;

import org.wonik.coreviaai_be.domain.project.entity.type.PreviousPmAction;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.service.command.DelegatePmCommand;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * PM 위임(FR-PRJ-04). previousPmActor는 previousPmAction이 CHANGE_ROLE일 때만 보낸다(서비스에서 검증).
 */
public record DelegatePmRequest(
	@NotNull(message = "새 PM을 선택해 주세요")
	@Positive(message = "사용자 ID를 확인해 주세요")
	Long newPmUserId,

	@NotNull(message = "기존 PM의 처리 방식을 선택해 주세요")
	PreviousPmAction previousPmAction,

	ProjectActor previousPmActor
) {

	public DelegatePmCommand toCommand() {
		return new DelegatePmCommand(newPmUserId, previousPmAction, previousPmActor);
	}
}

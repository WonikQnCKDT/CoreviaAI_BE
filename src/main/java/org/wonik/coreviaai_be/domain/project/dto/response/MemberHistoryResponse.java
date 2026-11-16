package org.wonik.coreviaai_be.domain.project.dto.response;

import java.time.Instant;

import org.wonik.coreviaai_be.domain.project.entity.ProjectMemberHistory;
import org.wonik.coreviaai_be.domain.project.entity.type.MemberChangeType;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.service.result.MemberHistoryResult;

/**
 * 멤버 이력 한 항목. 초대는 beforeActor가 null, 제외는 afterActor가 null이다.
 *
 * @param changedBy 처리자(PM, 재지정이면 시스템 관리자)
 */
public record MemberHistoryResponse(
	Long id,
	Long userId,
	String userName,
	MemberChangeType changeType,
	ProjectActor beforeActor,
	ProjectActor afterActor,
	Long changedBy,
	String changedByName,
	Instant occurredAt
) {

	public static MemberHistoryResponse from(final MemberHistoryResult result) {
		final ProjectMemberHistory history = result.history();
		return new MemberHistoryResponse(
			history.getId(),
			history.getUserId(),
			result.userName(),
			history.getChangeType(),
			history.getBeforeActor(),
			history.getAfterActor(),
			history.getChangedBy(),
			result.changedByName(),
			history.getOccurredAt()
		);
	}
}

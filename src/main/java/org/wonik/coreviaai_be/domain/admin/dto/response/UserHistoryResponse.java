package org.wonik.coreviaai_be.domain.admin.dto.response;

import java.time.Instant;

import org.wonik.coreviaai_be.domain.admin.service.result.UserHistoryResult;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditLog;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 사용자 변경 이력 응답 항목.
 *
 * @param actorName 변경한 사용자 이름. 행위자가 없거나 찾을 수 없으면 null
 * @param details   변경 내용. 예: {"systemRole": {"before": "USER", "after": "EXECUTIVE"}}
 */
public record UserHistoryResponse(
	Long id,
	AuditAction action,
	Long actorUserId,
	String actorName,
	JsonNode details,
	String ipAddress,
	Instant occurredAt
) {

	/** 저장된 JSON 문자열을 FE가 다시 파싱하지 않도록 객체로 내려준다. */
	public static UserHistoryResponse from(final UserHistoryResult result, final JsonMapper jsonMapper) {
		final AuditLog auditLog = result.auditLog();
		return new UserHistoryResponse(
			auditLog.getId(),
			auditLog.getAction(),
			auditLog.getActorUserId(),
			result.actorName(),
			auditLog.getDetails() == null ? null : jsonMapper.readTree(auditLog.getDetails()),
			auditLog.getIpAddress(),
			auditLog.getOccurredAt()
		);
	}
}

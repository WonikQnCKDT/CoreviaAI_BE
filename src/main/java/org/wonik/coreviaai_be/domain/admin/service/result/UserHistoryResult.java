package org.wonik.coreviaai_be.domain.admin.service.result;

import org.wonik.global.audit.AuditLog;

/**
 * 사용자 변경 이력 한 건.
 *
 * @param actorName 변경한 사용자 이름. 행위자가 없거나 찾을 수 없으면 null
 */
public record UserHistoryResult(AuditLog auditLog, String actorName) {
}

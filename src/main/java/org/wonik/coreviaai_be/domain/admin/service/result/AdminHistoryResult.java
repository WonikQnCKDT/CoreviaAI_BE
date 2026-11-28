package org.wonik.coreviaai_be.domain.admin.service.result;

import org.wonik.global.audit.AuditLog;

/**
 * 관리자 대상(공통 코드·다국어 라벨 등) 변경 이력 한 건.
 *
 * @param actorName 변경한 사용자 이름. 행위자가 없거나 찾을 수 없으면 null
 */
public record AdminHistoryResult(AuditLog auditLog, String actorName) {
}

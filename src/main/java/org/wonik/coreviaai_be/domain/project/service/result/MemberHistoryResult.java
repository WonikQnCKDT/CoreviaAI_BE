package org.wonik.coreviaai_be.domain.project.service.result;

import org.wonik.coreviaai_be.domain.project.entity.ProjectMemberHistory;

/**
 * 멤버 이력 한 항목. 대상자·처리자 이름은 화면 표시용으로 서비스가 채운다.
 */
public record MemberHistoryResult(ProjectMemberHistory history, String userName, String changedByName) {
}

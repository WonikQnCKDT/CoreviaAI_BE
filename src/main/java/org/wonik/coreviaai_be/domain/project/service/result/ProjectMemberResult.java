package org.wonik.coreviaai_be.domain.project.service.result;

import org.wonik.coreviaai_be.domain.project.entity.ProjectMember;

/**
 * 프로젝트 멤버 목록 한 항목. 이름은 화면 표시용으로 서비스가 채운다(없는 사용자면 null).
 */
public record ProjectMemberResult(ProjectMember member, String name) {
}

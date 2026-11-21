package org.wonik.aiserver.domain.wbs.api;

/**
 * 담당자·역할 제안에 쓰는 멤버(teamMembers). role은 ProjectActor 값이다.
 */
public record AiWbsTeamMember(Long userId, String name, String role) {
}

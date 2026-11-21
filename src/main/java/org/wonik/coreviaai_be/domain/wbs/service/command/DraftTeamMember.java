package org.wonik.coreviaai_be.domain.wbs.service.command;

import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;

/**
 * AI 초안 생성 시 알려 주는 멤버(담당자·역할 제안용).
 */
public record DraftTeamMember(Long userId, String name, ProjectActor actor) {
}

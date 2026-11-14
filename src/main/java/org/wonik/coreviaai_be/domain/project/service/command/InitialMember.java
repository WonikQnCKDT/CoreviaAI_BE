package org.wonik.coreviaai_be.domain.project.service.command;

import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;

/**
 * 생성 시 초대하는 멤버 한 명(FR-PRJ-01 ④): 사용자와 이 프로젝트에서 맡을 역할.
 */
public record InitialMember(Long userId, ProjectActor actor) {
}

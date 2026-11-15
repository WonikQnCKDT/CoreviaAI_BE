package org.wonik.coreviaai_be.domain.project.service.command;

import org.wonik.coreviaai_be.domain.project.entity.type.PreviousPmAction;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;

/**
 * PM 위임(FR-PRJ-04). 기존 PM은 previousPmActor 역할로 바뀌거나(CHANGE_ROLE) 멤버에서 빠진다(LEAVE, previousPmActor는 null).
 */
public record DelegatePmCommand(Long newPmUserId, PreviousPmAction previousPmAction, ProjectActor previousPmActor) {
}

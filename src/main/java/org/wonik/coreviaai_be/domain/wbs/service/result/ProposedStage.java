package org.wonik.coreviaai_be.domain.wbs.service.result;

import java.util.List;

import org.wonik.coreviaai_be.domain.project.entity.type.WaterfallPhaseType;

/**
 * 단계별 제안 작업. 단계는 프로젝트에 이미 있는 단계만 온다.
 */
public record ProposedStage(WaterfallPhaseType stageCode, List<ProposedTask> tasks) {
}

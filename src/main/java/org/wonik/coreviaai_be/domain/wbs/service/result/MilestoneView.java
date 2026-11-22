package org.wonik.coreviaai_be.domain.wbs.service.result;

import java.util.List;

import org.wonik.coreviaai_be.domain.wbs.entity.Milestone;
import org.wonik.coreviaai_be.domain.wbs.entity.MilestoneDeadline;

/**
 * 마일스톤과 조회 시점의 D-day·지연 경고, 연결 작업(작업 ID 순).
 */
public record MilestoneView(Milestone milestone, MilestoneDeadline deadline, List<MilestoneLinkedTask> tasks) {
}

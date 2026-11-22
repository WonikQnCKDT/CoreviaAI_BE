package org.wonik.coreviaai_be.domain.wbs.service.command;

/**
 * 카드를 다른 컬럼으로 옮긴다(FR-TSK-02).
 *
 * @param columnId 옮길 컬럼(같은 프로젝트)
 * @param version  업무 version(낙관적 잠금, WBS·업무 API와 공유)
 */
public record MoveBoardCardCommand(Long columnId, long version) {
}

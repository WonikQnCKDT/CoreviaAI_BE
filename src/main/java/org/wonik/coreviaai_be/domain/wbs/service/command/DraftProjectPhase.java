package org.wonik.coreviaai_be.domain.wbs.service.command;

/** AI가 새 단계를 만들지 않고 프로젝트에 저장된 단계만 사용하도록 전달하는 참조. */
public record DraftProjectPhase(Long phaseId, String name, int order) {
}

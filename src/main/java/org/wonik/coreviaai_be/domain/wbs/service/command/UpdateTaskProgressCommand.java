package org.wonik.coreviaai_be.domain.wbs.service.command;

/** 업무 진척률과 낙관적 잠금 버전. */
public record UpdateTaskProgressCommand(int progress, long version) {
}

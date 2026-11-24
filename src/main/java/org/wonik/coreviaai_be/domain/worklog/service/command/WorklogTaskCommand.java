package org.wonik.coreviaai_be.domain.worklog.service.command;
public record WorklogTaskCommand(Long projectId, String content, int progress, int spentMinutes,
    boolean completed, long taskVersion, long version) {}

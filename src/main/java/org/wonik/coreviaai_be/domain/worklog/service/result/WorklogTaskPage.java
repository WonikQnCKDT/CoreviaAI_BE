package org.wonik.coreviaai_be.domain.worklog.service.result;
import java.util.List;
public record WorklogTaskPage(List<WorklogTaskResult> items, boolean hasNext) {}

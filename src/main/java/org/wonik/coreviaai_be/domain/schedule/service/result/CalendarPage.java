package org.wonik.coreviaai_be.domain.schedule.service.result;
import java.util.List;
public record CalendarPage(List<CalendarItem> items, boolean hasNext) {}

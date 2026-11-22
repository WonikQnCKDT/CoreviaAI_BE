package org.wonik.coreviaai_be.domain.todo.service.result;
import java.time.Instant;
import java.time.LocalDate;
public record TodoItem(Long id, String source, String title, LocalDate dueDate, boolean done, Instant doneAt,
	Long projectId, String taskStatus, int urgency, boolean overdue) { }

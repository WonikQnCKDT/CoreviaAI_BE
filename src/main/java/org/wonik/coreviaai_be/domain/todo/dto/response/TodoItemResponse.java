package org.wonik.coreviaai_be.domain.todo.dto.response;
import java.time.Instant;
import java.time.LocalDate;
import org.wonik.coreviaai_be.domain.todo.service.result.TodoItem;
public record TodoItemResponse(Long id, String source, String title, LocalDate dueDate, boolean done, Instant doneAt,
	Long projectId, String taskStatus, int urgency, boolean overdue) {
	public static TodoItemResponse from(final TodoItem item) {
		return new TodoItemResponse(item.id(),item.source(),item.title(),item.dueDate(),item.done(),item.doneAt(),
			item.projectId(),item.taskStatus(),item.urgency(),item.overdue());
	}
}

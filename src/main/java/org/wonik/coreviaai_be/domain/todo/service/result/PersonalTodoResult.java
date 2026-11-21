package org.wonik.coreviaai_be.domain.todo.service.result;
import java.time.Instant;
import java.time.LocalDate;
import org.wonik.coreviaai_be.domain.todo.entity.PersonalTodo;
public record PersonalTodoResult(Long id, String title, LocalDate dueDate, boolean done, Instant doneAt,
	Instant createdAt, Instant updatedAt) {
	public static PersonalTodoResult from(final PersonalTodo todo) {
		return new PersonalTodoResult(todo.getId(), todo.getTitle(), todo.getDueDate(), todo.isDone(), todo.getDoneAt(),
			todo.getCreatedAt(), todo.getUpdatedAt());
	}
}

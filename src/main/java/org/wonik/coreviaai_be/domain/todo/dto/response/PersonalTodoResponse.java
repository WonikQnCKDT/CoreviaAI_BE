package org.wonik.coreviaai_be.domain.todo.dto.response;
import java.time.Instant;
import java.time.LocalDate;
import org.wonik.coreviaai_be.domain.todo.service.result.PersonalTodoResult;
public record PersonalTodoResponse(Long id, String title, LocalDate dueDate, boolean done, Instant doneAt,
	Instant createdAt, Instant updatedAt) {
	public static PersonalTodoResponse from(final PersonalTodoResult result) {
		return new PersonalTodoResponse(result.id(), result.title(), result.dueDate(), result.done(), result.doneAt(),
			result.createdAt(), result.updatedAt());
	}
}

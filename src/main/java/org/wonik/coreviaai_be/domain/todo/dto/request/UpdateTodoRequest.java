package org.wonik.coreviaai_be.domain.todo.dto.request;
import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.wonik.coreviaai_be.domain.todo.entity.PersonalTodo;
import org.wonik.coreviaai_be.domain.todo.service.command.UpdateTodoCommand;
public record UpdateTodoRequest(@NotBlank @Size(max = PersonalTodo.TITLE_MAX_LENGTH)
	@Pattern(regexp = "[^\\p{Cntrl}]*") String title,
	LocalDate dueDate, @NotNull Boolean done) {
	public UpdateTodoCommand toCommand() { return new UpdateTodoCommand(title.strip(), dueDate, done); }
	@Override public String toString() { return "UpdateTodoRequest[redacted]"; }
}

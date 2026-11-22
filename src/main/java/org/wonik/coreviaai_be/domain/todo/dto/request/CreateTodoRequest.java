package org.wonik.coreviaai_be.domain.todo.dto.request;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.wonik.coreviaai_be.domain.todo.entity.PersonalTodo;
import org.wonik.coreviaai_be.domain.todo.service.command.CreateTodoCommand;
public record CreateTodoRequest(@NotNull UUID requestId,
	@NotBlank @Size(max = PersonalTodo.TITLE_MAX_LENGTH) @Pattern(regexp = "[^\\p{Cntrl}]*") String title,
	LocalDate dueDate) {
	public CreateTodoCommand toCommand() { return new CreateTodoCommand(requestId, title.strip(), dueDate); }
	@Override public String toString() { return "CreateTodoRequest[redacted]"; }
}

package org.wonik.coreviaai_be.domain.todo.service.command;
import java.time.LocalDate;
import java.util.UUID;
public record CreateTodoCommand(UUID requestId, String title, LocalDate dueDate) { }

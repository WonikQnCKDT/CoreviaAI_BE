package org.wonik.coreviaai_be.domain.todo.service.command;
import java.time.LocalDate;
public record UpdateTodoCommand(String title, LocalDate dueDate, boolean done) { }

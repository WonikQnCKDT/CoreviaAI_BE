package org.wonik.coreviaai_be.domain.todo.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.todo.entity.PersonalTodo;
import org.wonik.coreviaai_be.domain.todo.policy.TodoDatePolicy;
import org.wonik.coreviaai_be.domain.todo.repository.PersonalTodoRepository;
import org.wonik.coreviaai_be.domain.todo.service.command.CreateTodoCommand;
import org.wonik.coreviaai_be.domain.todo.service.command.UpdateTodoCommand;
import org.wonik.coreviaai_be.domain.todo.service.result.PersonalTodoResult;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonalTodoService {
	private final PersonalTodoRepository repository;
	private final EntityManager entityManager;
	private final Clock clock;

	@Transactional
	public PersonalTodoResult create(final Long userId, final CreateTodoCommand command) {
		TodoDatePolicy.validate(command.dueDate());
		// Lock ordering is users -> personal_todos, as in authentication. This serializes same-owner
		// creation before the unique-key check, so concurrent retries do not poison the transaction.
		if (entityManager.find(User.class, userId, LockModeType.PESSIMISTIC_WRITE) == null) {
			throw new CustomException(ErrorCode.USER_NOT_FOUND);
		}
		final String hash = requestHash(command);
		final PersonalTodo existing = repository.findByUserIdAndRequestId(userId, command.requestId().toString()).orElse(null);
		if (existing != null) {
			if (!existing.getRequestHash().equals(hash)) { throw new CustomException(ErrorCode.TODO_REQUEST_CONFLICT); }
			// Recreate the original creation response even if the resource has since been edited/completed.
			return new PersonalTodoResult(existing.getId(), command.title(), command.dueDate(), false, null,
				existing.getCreatedAt(), existing.getCreatedAt());
		}
		final PersonalTodo created = repository.saveAndFlush(PersonalTodo.create(userId, command.requestId(), hash,
			command.title(), command.dueDate()));
		// Read DB TIMESTAMP(6) precision before the first response so a later retry is byte-for-byte stable.
		entityManager.refresh(created);
		return PersonalTodoResult.from(created);
	}

	public PersonalTodoResult get(final Long userId, final Long todoId) {
		return PersonalTodoResult.from(repository.findByIdAndUserId(todoId, userId)
			.orElseThrow(() -> new CustomException(ErrorCode.TODO_NOT_FOUND)));
	}

	@Transactional
	public PersonalTodoResult update(final Long userId, final Long todoId, final UpdateTodoCommand command) {
		TodoDatePolicy.validate(command.dueDate());
		final PersonalTodo todo = repository.findOwnedForUpdate(todoId, userId)
			.orElseThrow(() -> new CustomException(ErrorCode.TODO_NOT_FOUND));
		if (!todo.matches(command.title(), command.dueDate(), command.done())) {
			todo.update(command.title(), command.dueDate(), command.done(), clock.instant());
			repository.flush();
			entityManager.refresh(todo);
		}
		return PersonalTodoResult.from(todo);
	}

	private static String requestHash(final CreateTodoCommand command) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
				(command.title().length() + ":" + command.title() + ":" + command.dueDate()).getBytes(StandardCharsets.UTF_8)));
		} catch (final NoSuchAlgorithmException impossible) {
			throw new IllegalStateException("Java runtime must support SHA-256", impossible);
		}
	}
}

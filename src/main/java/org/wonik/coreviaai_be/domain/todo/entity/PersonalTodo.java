package org.wonik.coreviaai_be.domain.todo.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import org.wonik.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "personal_todos")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonalTodo extends BaseTimeEntity {

	public static final int TITLE_MAX_LENGTH = 200;
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "user_id", nullable = false)
	private Long userId;
	@Column(nullable = false, length = TITLE_MAX_LENGTH)
	private String title;
	@Column(name = "due_date")
	private LocalDate dueDate;
	@Column(nullable = false)
	private boolean done;
	@Column(name = "done_at")
	private Instant doneAt;
	@Column(name = "deleted_at")
	private Instant deletedAt;
	@Column(name = "request_id", nullable = false, updatable = false, length = 36)
	private String requestId;
	@Column(name = "request_hash", nullable = false, updatable = false, length = 64)
	private String requestHash;

	public static PersonalTodo create(final Long userId, final UUID requestId, final String requestHash,
		final String title, final LocalDate dueDate) {
		final PersonalTodo todo = new PersonalTodo();
		todo.userId = userId;
		todo.requestId = requestId.toString();
		todo.requestHash = requestHash;
		todo.title = title;
		todo.dueDate = dueDate;
		return todo;
	}

	/** A retry of completion keeps its first completion instant; reopening clears it. */
	public void update(final String title, final LocalDate dueDate, final boolean done, final Instant now) {
		this.title = title;
		this.dueDate = dueDate;
		if (this.done != done) {
			this.doneAt = done ? now : null;
			this.done = done;
		}
	}

	/** Tombstones retain creation keys; retries must never resurrect or duplicate a deleted item. */
	public boolean softDelete(final Instant now) {
		if (deletedAt != null) { return false; }
		deletedAt = now;
		return true;
	}

	public boolean matches(final String title, final LocalDate dueDate, final boolean done) {
		return this.title.equals(title) && Objects.equals(this.dueDate, dueDate) && this.done == done;
	}
}

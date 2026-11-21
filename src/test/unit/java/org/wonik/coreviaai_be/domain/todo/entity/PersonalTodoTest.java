package org.wonik.coreviaai_be.domain.todo.entity;
import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
class PersonalTodoTest {
	private static final Instant NOW = Instant.parse("2026-11-21T00:00:00Z");
	@Test void newTodoIsOpenAndKeepsDateOnly() {
		final PersonalTodo todo = PersonalTodo.create(1L, UUID.randomUUID(), "hash", "Plan", LocalDate.of(2026,11,21));
		assertThat(todo.isDone()).isFalse(); assertThat(todo.getDoneAt()).isNull();
		assertThat(todo.getDueDate()).isEqualTo(LocalDate.of(2026,11,21));
	}
	@Test void completionRetryKeepsTimestampAndReopenClearsIt() {
		final PersonalTodo todo = PersonalTodo.create(1L, UUID.randomUUID(), "hash", "Plan", null);
		todo.update("Plan", null, true, NOW); todo.update("Plan", null, true, NOW.plusSeconds(1));
		assertThat(todo.getDoneAt()).isEqualTo(NOW);
		todo.update("Plan", null, false, NOW.plusSeconds(2));
		assertThat(todo.getDoneAt()).isNull(); assertThat(todo.isDone()).isFalse();
	}
	@Test void mutableFieldsDoNotChangeCreationIdentity() {
		final UUID requestId = UUID.randomUUID();
		final PersonalTodo todo = PersonalTodo.create(1L, requestId, "original-hash", "Plan", null);
		todo.update("Changed", LocalDate.of(2027,1,1), true, NOW);
		assertThat(todo.getRequestId()).isEqualTo(requestId.toString());
		assertThat(todo.getRequestHash()).isEqualTo("original-hash");
		assertThat(todo.matches("Changed", LocalDate.of(2027,1,1), true)).isTrue();
		assertThat(todo.matches("Plan", null, false)).isFalse();
	}
}

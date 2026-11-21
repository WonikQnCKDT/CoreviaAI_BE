package org.wonik.coreviaai_be.domain.todo.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.wonik.coreviaai_be.domain.todo.entity.PersonalTodo;
import jakarta.persistence.LockModeType;

public interface PersonalTodoRepository extends JpaRepository<PersonalTodo, Long> {
	Optional<PersonalTodo> findByUserIdAndRequestId(Long userId, String requestId);
	Optional<PersonalTodo> findByIdAndUserId(Long id, Long userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from PersonalTodo t where t.id = :id and t.userId = :userId")
	Optional<PersonalTodo> findOwnedForUpdate(@Param("id") Long id, @Param("userId") Long userId);
}

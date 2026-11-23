package org.wonik.coreviaai_be.domain.schedule.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.wonik.coreviaai_be.domain.schedule.entity.PersonalEvent;

public interface PersonalEventRepository extends JpaRepository<PersonalEvent, Long> {

	Optional<PersonalEvent> findByIdAndUserId(Long id, Long userId);

	/** [from, to) 기간과 겹치는 본인 일정. 기간이 시작 시각 순으로 보이도록 정렬한다. */
	@Query("select e from PersonalEvent e where e.userId = :userId and e.startAt < :to and e.endAt > :from "
		+ "order by e.startAt, e.id")
	List<PersonalEvent> findOverlapping(@Param("userId") Long userId, @Param("from") Instant from,
		@Param("to") Instant to);
}

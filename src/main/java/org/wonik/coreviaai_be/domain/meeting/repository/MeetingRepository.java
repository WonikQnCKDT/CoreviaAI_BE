package org.wonik.coreviaai_be.domain.meeting.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.wonik.coreviaai_be.domain.meeting.entity.Meeting;
import jakarta.persistence.LockModeType;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

	Optional<Meeting> findByIdAndProjectId(Long id, Long projectId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select m from Meeting m where m.id = :id and m.projectId = :projectId")
	Optional<Meeting> findForUpdate(@Param("id") Long id, @Param("projectId") Long projectId);

	@Query("""
		select m from Meeting m
		where m.projectId = :projectId
		  and (:cursorHeldAt is null
		       or m.heldAt < :cursorHeldAt
		       or (m.heldAt = :cursorHeldAt and m.id < :cursorId))
		  and (lower(m.title) like :keywordPattern escape '\\'
		       or lower(coalesce(m.content, '')) like :keywordPattern escape '\\'
		       or exists (select d.id from MeetingDecision d
		                  where d.meeting = m and lower(d.content) like :keywordPattern escape '\\')
		       or exists (select a.id from ActionItem a
		                  where a.meeting = m and lower(a.content) like :keywordPattern escape '\\'))
		order by m.heldAt desc, m.id desc
		""")
	List<Meeting> searchPage(
		@Param("projectId") Long projectId,
		@Param("cursorHeldAt") Instant cursorHeldAt,
		@Param("cursorId") Long cursorId,
		@Param("keywordPattern") String keywordPattern,
		Pageable pageable
	);
}

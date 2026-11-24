package org.wonik.coreviaai_be.domain.meeting.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wonik.coreviaai_be.domain.meeting.entity.MeetingCreateRequest;

public interface MeetingCreateRequestRepository extends JpaRepository<MeetingCreateRequest, Long> {
	Optional<MeetingCreateRequest> findByProjectIdAndAuthorUserIdAndRequestId(Long projectId, Long userId, String requestId);
	Optional<MeetingCreateRequest> findByMeetingId(Long meetingId);
}

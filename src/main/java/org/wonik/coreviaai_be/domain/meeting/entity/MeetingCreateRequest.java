package org.wonik.coreviaai_be.domain.meeting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 생성 요청 키는 회의록 삭제 이후에도 남겨 재생성을 막는다. */
@Getter
@Entity
@Table(name = "meeting_create_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingCreateRequest {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "project_id", nullable = false, updatable = false)
	private Long projectId;
	@Column(name = "author_user_id", nullable = false, updatable = false)
	private Long authorUserId;
	@Column(name = "request_id", nullable = false, updatable = false, length = 36)
	private String requestId;
	@Column(name = "request_hash", nullable = false, updatable = false, length = 64)
	private String requestHash;
	@Column(name = "meeting_id")
	private Long meetingId;

	public static MeetingCreateRequest create(final Long projectId, final Long authorUserId,
		final String requestId, final String requestHash, final Long meetingId) {
		final MeetingCreateRequest request = new MeetingCreateRequest();
		request.projectId = projectId;
		request.authorUserId = authorUserId;
		request.requestId = requestId;
		request.requestHash = requestHash;
		request.meetingId = meetingId;
		return request;
	}

	public void markDeleted() {
		meetingId = null;
	}
}

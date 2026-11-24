package org.wonik.coreviaai_be.domain.meeting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 사내 사용자(userId) 또는 외부 참석자 이름 중 하나만 저장한다. */
@Getter
@Entity
@Table(name = "meeting_attendees")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingAttendee {

	public static final int EXTERNAL_NAME_MAX_LENGTH = 50;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "meeting_id", nullable = false, updatable = false)
	private Meeting meeting;

	@Column(name = "user_id")
	private Long userId;

	@Column(name = "external_name", length = EXTERNAL_NAME_MAX_LENGTH)
	private String externalName;

	static MeetingAttendee of(final Meeting meeting, final Long userId, final String externalName) {
		final MeetingAttendee attendee = new MeetingAttendee();
		attendee.meeting = meeting;
		attendee.userId = userId;
		attendee.externalName = externalName;
		return attendee;
	}
}

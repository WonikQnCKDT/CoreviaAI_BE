package org.wonik.coreviaai_be.domain.meeting.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.wonik.global.common.entity.BaseTimeEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 회의록 aggregate root. 참석자·결정사항·액션아이템은 회의록과 같은 트랜잭션에서 교체한다. */
@Getter
@Entity
@Table(name = "meetings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meeting extends BaseTimeEntity {

	public static final int TITLE_MAX_LENGTH = 200;
	public static final int CONTENT_MAX_LENGTH = 50_000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "project_id", nullable = false, updatable = false)
	private Long projectId;

	@Column(nullable = false, length = TITLE_MAX_LENGTH)
	private String title;

	@Column(name = "held_at", nullable = false)
	private Instant heldAt;

	@Column(columnDefinition = "TEXT")
	private String content;

	@Column(name = "author_user_id", nullable = false, updatable = false)
	private Long authorUserId;

	@Version
	@Column(nullable = false)
	private long version;

	@OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id ASC")
	private List<MeetingAttendee> attendees = new ArrayList<>();

	@OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sortOrder ASC, id ASC")
	private List<MeetingDecision> decisions = new ArrayList<>();

	@OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id ASC")
	private List<ActionItem> actionItems = new ArrayList<>();

	public static Meeting create(
		final Long projectId,
		final String title,
		final Instant heldAt,
		final String content,
		final Long authorUserId
	) {
		final Meeting meeting = new Meeting();
		meeting.projectId = projectId;
		meeting.authorUserId = authorUserId;
		meeting.changeDetails(title, heldAt, content);
		return meeting;
	}

	public void changeDetails(final String title, final Instant heldAt, final String content) {
		this.title = title;
		this.heldAt = heldAt;
		this.content = content;
	}

	public void replaceAttendees(final List<AttendeeValue> values) {
		attendees.clear();
		values.forEach(value -> attendees.add(MeetingAttendee.of(this, value.userId(), value.externalName())));
	}

	public void replaceDecisions(final List<String> contents) {
		decisions.clear();
		for (int index = 0; index < contents.size(); index++) {
			decisions.add(MeetingDecision.manual(this, contents.get(index), index));
		}
	}

	public void replaceActionItems(final List<ActionItemValue> values) {
		actionItems.clear();
		values.forEach(value -> actionItems.add(
			ActionItem.manual(this, value.content(), value.assigneeUserId(), value.dueDate())));
	}

	public record AttendeeValue(Long userId, String externalName) {
	}

	public record ActionItemValue(String content, Long assigneeUserId, LocalDate dueDate) {
	}
}

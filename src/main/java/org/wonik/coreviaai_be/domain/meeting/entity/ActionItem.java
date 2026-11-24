package org.wonik.coreviaai_be.domain.meeting.entity;

import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.wonik.coreviaai_be.domain.meeting.entity.type.ActionItemStatus;
import org.wonik.coreviaai_be.domain.meeting.entity.type.MeetingItemSource;
import org.wonik.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Getter
@Entity
@Table(name = "action_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActionItem extends BaseTimeEntity {

	public static final int CONTENT_MAX_LENGTH = 1000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "meeting_id", nullable = false, updatable = false)
	private Meeting meeting;

	@Column(nullable = false, length = CONTENT_MAX_LENGTH)
	private String content;

	@Column(name = "assignee_user_id")
	private Long assigneeUserId;

	@Column(name = "due_date")
	private LocalDate dueDate;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private ActionItemStatus status;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private MeetingItemSource source;

	@Column(name = "task_id")
	private Long taskId;

	@Column(name = "approved_by")
	private Long approvedBy;

	@Column(name = "approved_at")
	private Instant approvedAt;

	static ActionItem manual(
		final Meeting meeting,
		final String content,
		final Long assigneeUserId,
		final LocalDate dueDate
	) {
		final ActionItem item = new ActionItem();
		item.meeting = meeting;
		item.content = content;
		item.assigneeUserId = assigneeUserId;
		item.dueDate = dueDate;
		item.status = ActionItemStatus.OPEN;
		item.source = MeetingItemSource.MANUAL;
		return item;
	}
}

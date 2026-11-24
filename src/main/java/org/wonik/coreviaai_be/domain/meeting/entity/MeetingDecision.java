package org.wonik.coreviaai_be.domain.meeting.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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
@Table(name = "meeting_decisions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingDecision extends BaseTimeEntity {

	public static final int CONTENT_MAX_LENGTH = 1000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "meeting_id", nullable = false, updatable = false)
	private Meeting meeting;

	@Column(nullable = false, length = CONTENT_MAX_LENGTH)
	private String content;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private MeetingItemSource source;

	static MeetingDecision manual(final Meeting meeting, final String content, final int sortOrder) {
		final MeetingDecision decision = new MeetingDecision();
		decision.meeting = meeting;
		decision.content = content;
		decision.sortOrder = sortOrder;
		decision.source = MeetingItemSource.MANUAL;
		return decision;
	}
}

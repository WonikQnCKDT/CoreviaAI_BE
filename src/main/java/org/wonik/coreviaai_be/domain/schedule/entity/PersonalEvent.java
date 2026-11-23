package org.wonik.coreviaai_be.domain.schedule.entity;

import java.time.Instant;

import org.wonik.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "personal_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonalEvent extends BaseTimeEntity {

	public static final int TITLE_MAX_LENGTH = 200;
	public static final int MEMO_MAX_LENGTH = 2000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "user_id", nullable = false, updatable = false)
	private Long userId;
	@Column(nullable = false, length = TITLE_MAX_LENGTH)
	private String title;
	@Column(name = "start_at", nullable = false)
	private Instant startAt;
	@Column(name = "end_at", nullable = false)
	private Instant endAt;
	@Column(name = "all_day", nullable = false)
	private boolean allDay;
	@Column(length = MEMO_MAX_LENGTH)
	private String memo;

	public static PersonalEvent create(final Long userId, final String title, final Instant startAt,
		final Instant endAt, final boolean allDay, final String memo) {
		final PersonalEvent event = new PersonalEvent();
		event.userId = userId;
		event.apply(title, startAt, endAt, allDay, memo);
		return event;
	}

	public void update(final String title, final Instant startAt, final Instant endAt, final boolean allDay,
		final String memo) {
		apply(title, startAt, endAt, allDay, memo);
	}

	private void apply(final String title, final Instant startAt, final Instant endAt, final boolean allDay,
		final String memo) {
		this.title = title;
		this.startAt = startAt;
		this.endAt = endAt;
		this.allDay = allDay;
		this.memo = memo;
	}
}

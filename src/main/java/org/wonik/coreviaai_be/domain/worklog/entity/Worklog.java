package org.wonik.coreviaai_be.domain.worklog.entity;

import java.time.LocalDate;

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

/** 날짜별 업무 노트. 사용자당 날짜마다 한 건이다(uk_worklogs_user_id_log_date). */
@Getter
@Entity
@Table(name = "worklogs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Worklog extends BaseTimeEntity {

	/** 임시 상한. ERD는 TEXT만 정하므로 운영 데이터를 본 뒤 조정한다. */
	public static final int CONTENT_MAX_LENGTH = 10_000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "user_id", nullable = false, updatable = false)
	private Long userId;
	@Column(name = "log_date", nullable = false, updatable = false)
	private LocalDate logDate;
	@Column
	private String content;

	public static Worklog create(final Long userId, final LocalDate logDate, final String content) {
		final Worklog log = new Worklog();
		log.userId = userId;
		log.logDate = logDate;
		log.content = content;
		return log;
	}

	/** 값이 같으면 JPA 더티 체킹이 UPDATE를 생략하므로 같은 내용의 재저장은 수정 시각을 바꾸지 않는다. */
	public void replaceContent(final String content) {
		this.content = content;
	}
}

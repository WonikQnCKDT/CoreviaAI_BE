package org.wonik.coreviaai_be.domain.wbs.entity;

import java.time.LocalDate;
import java.util.Objects;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsItemSource;
import org.wonik.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 마일스톤(FR-WBS-04). 연결 작업(MilestoneTask)이 끝나지 않은 채 목표일이 오면 지연 경고 대상이 된다.
 * 목표일은 달력 날짜라 LocalDate로 모델링한다.
 */
@Getter
@Entity
@Table(name = "milestones")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Milestone extends BaseTimeEntity {

	public static final int NAME_MAX_LENGTH = 200;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "project_id", nullable = false)
	private Long projectId;

	@Column(name = "name", nullable = false, length = NAME_MAX_LENGTH)
	private String name;

	@Column(name = "target_date", nullable = false)
	private LocalDate targetDate;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "source", nullable = false, length = 20)
	private WbsItemSource source;

	@Column(name = "created_by", nullable = false)
	private Long createdBy;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	private Milestone(
		final Long projectId,
		final String name,
		final LocalDate targetDate,
		final WbsItemSource source,
		final Long createdBy
	) {
		this.projectId = projectId;
		this.name = name;
		this.targetDate = targetDate;
		this.source = source;
		this.createdBy = createdBy;
	}

	public static Milestone of(
		final Long projectId,
		final String name,
		final LocalDate targetDate,
		final WbsItemSource source,
		final Long createdBy
	) {
		return new Milestone(projectId, name, targetDate, source, createdBy);
	}

	public boolean hasSameDetails(final String name, final LocalDate targetDate) {
		return Objects.equals(this.name, name) && Objects.equals(this.targetDate, targetDate);
	}

	public void changeDetails(final String name, final LocalDate targetDate) {
		this.name = name;
		this.targetDate = targetDate;
	}
}

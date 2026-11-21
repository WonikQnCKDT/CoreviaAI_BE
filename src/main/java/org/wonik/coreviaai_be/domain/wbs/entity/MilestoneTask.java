package org.wonik.coreviaai_be.domain.wbs.entity;

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

/**
 * 마일스톤에 연결한 작업(FR-WBS-04). 같은 연결은 한 번만 둔다(UNIQUE(milestone_id, task_id)).
 */
@Getter
@Entity
@Table(name = "milestone_tasks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MilestoneTask extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "milestone_id", nullable = false)
	private Long milestoneId;

	@Column(name = "task_id", nullable = false)
	private Long taskId;

	private MilestoneTask(final Long milestoneId, final Long taskId) {
		this.milestoneId = milestoneId;
		this.taskId = taskId;
	}

	public static MilestoneTask of(final Long milestoneId, final Long taskId) {
		return new MilestoneTask(milestoneId, taskId);
	}
}

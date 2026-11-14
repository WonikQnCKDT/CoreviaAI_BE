package org.wonik.coreviaai_be.domain.project.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.wonik.coreviaai_be.domain.project.entity.type.GateStatus;
import org.wonik.coreviaai_be.domain.project.entity.type.PhaseStatus;
import org.wonik.coreviaai_be.domain.project.entity.type.WaterfallPhaseType;
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
 * 폭포수 단계와 게이트(FR-PRJ-01 ②·⑤, FR-PRJ-05). 폭포수 생성 시 5단계가 자동으로 만들어진다.
 * 게이트 승인자는 PM이 아닌 멤버 중 지정하며 미지정(null)일 수 있다. 단계 전환·게이트 판정과
 * 계획·실제 시작/종료일 관리는 FR-PRJ-05에서 다루고, WBS 작업(FR-WBS-01)은 이 단계를 최상위로 참조한다.
 */
@Getter
@Entity
@Table(name = "project_phases")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectPhase extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "project_id", nullable = false)
	private Long projectId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "phase_type", nullable = false, length = 20)
	private WaterfallPhaseType phaseType;

	/** 단계 순서(1부터). */
	@Column(name = "phase_order", nullable = false)
	private int phaseOrder;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "status", nullable = false, length = 20)
	private PhaseStatus status;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "gate_status", nullable = false, length = 20)
	private GateStatus gateStatus;

	@Column(name = "gate_approver_user_id")
	private Long gateApproverUserId;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	private ProjectPhase(
		final Long projectId,
		final WaterfallPhaseType phaseType,
		final int phaseOrder,
		final Long gateApproverUserId
	) {
		this.projectId = projectId;
		this.phaseType = phaseType;
		this.phaseOrder = phaseOrder;
		this.status = PhaseStatus.NOT_STARTED;
		this.gateStatus = GateStatus.PENDING;
		this.gateApproverUserId = gateApproverUserId;
	}

	public static ProjectPhase of(
		final Long projectId,
		final WaterfallPhaseType phaseType,
		final int phaseOrder,
		final Long gateApproverUserId
	) {
		return new ProjectPhase(projectId, phaseType, phaseOrder, gateApproverUserId);
	}
}

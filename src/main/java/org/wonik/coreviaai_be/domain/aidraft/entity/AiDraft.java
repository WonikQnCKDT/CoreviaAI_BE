package org.wonik.coreviaai_be.domain.aidraft.entity;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.wonik.coreviaai_be.domain.aidraft.entity.type.AiDraftFeature;
import org.wonik.coreviaai_be.domain.aidraft.entity.type.AiDraftStatus;
import org.wonik.global.common.entity.BaseTimeEntity;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 초안·추정값 한 건(ai_drafts, 10. AI 리스크·AI 초안 ERD). AI 결과와 사람의 수락·거절을 남기는 기록이며,
 * 업무 데이터 반영은 각 기능이 사람 승인 뒤에 따로 한다(NFR-08).
 * <p>
 * 상태 전이: DRAFT → ACCEPTED·PARTIALLY_ACCEPTED·REJECTED(사람 결정, 한 번만). 생성 실패는 처음부터 FAILED.
 */
@Getter
@Entity
@Table(name = "ai_drafts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiDraft extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "feature", nullable = false, updatable = false, length = 30)
	private AiDraftFeature feature;

	@Column(name = "project_id", updatable = false)
	private Long projectId;

	@Column(name = "target_type", updatable = false, length = 30)
	private String targetType;

	@Column(name = "target_id", updatable = false)
	private Long targetId;

	@Column(name = "requested_by", updatable = false)
	private Long requestedBy;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "status", nullable = false, length = 20)
	private AiDraftStatus status;

	/** DB 타입은 TEXT(길이 제한 없음). PostgreSQL·H2 모두 TEXT를 VARCHAR 계열로 알려 주므로 String 기본 매핑으로 둔다. */
	@Column(name = "output", updatable = false)
	private String output;

	@Column(name = "model", updatable = false, length = 100)
	private String model;

	@Column(name = "latency_ms", updatable = false)
	private Integer latencyMs;

	@Column(name = "decided_by")
	private Long decidedBy;

	@Column(name = "decided_at")
	private Instant decidedAt;

	private AiDraft(
		final AiDraftFeature feature,
		final Long projectId,
		final Long requestedBy,
		final AiDraftStatus status,
		final String output,
		final Integer latencyMs
	) {
		this.feature = feature;
		this.projectId = projectId;
		this.requestedBy = requestedBy;
		this.status = status;
		this.output = output;
		this.latencyMs = latencyMs;
	}

	/** 생성 성공. output은 AI 결과 JSON이다(사용자 입력 원문은 넣지 않는다). */
	public static AiDraft draft(
		final AiDraftFeature feature,
		final Long projectId,
		final Long requestedBy,
		final String output,
		final int latencyMs
	) {
		return new AiDraft(feature, projectId, requestedBy, AiDraftStatus.DRAFT, output, latencyMs);
	}

	/** 생성 실패. 결과가 없어 output은 비운다. 실패 사유(ErrorCode)는 감사 로그에 남는다. */
	public static AiDraft failed(
		final AiDraftFeature feature,
		final Long projectId,
		final Long requestedBy,
		final int latencyMs
	) {
		return new AiDraft(feature, projectId, requestedBy, AiDraftStatus.FAILED, null, latencyMs);
	}

	/**
	 * 사람의 결정(수락·일부 수락·거절)을 남긴다. DRAFT에서만 한 번 바꿀 수 있다.
	 * 이미 결정된 초안이면 409 AI_DRAFT_ALREADY_APPLIED.
	 */
	public void decide(final AiDraftStatus decision, final Long decidedBy, final Instant decidedAt) {
		if (decision == AiDraftStatus.DRAFT || decision == AiDraftStatus.FAILED) {
			throw new IllegalArgumentException("사람의 결정은 ACCEPTED·PARTIALLY_ACCEPTED·REJECTED만 가능합니다: " + decision);
		}
		if (status != AiDraftStatus.DRAFT) {
			throw new CustomException(ErrorCode.AI_DRAFT_ALREADY_APPLIED);
		}
		this.status = decision;
		this.decidedBy = decidedBy;
		this.decidedAt = decidedAt;
	}
}

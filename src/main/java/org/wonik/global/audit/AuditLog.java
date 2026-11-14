package org.wonik.global.audit;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
 * 감사 로그(NFR-07). 권한·계정·조직 변경 등을 행위자·대상·변경 내용과 함께 남기는 append-only 로그다.
 * 조회 화면과 1년 보관 정리는 FR-ADM-04에서 구현한다. 로그인 시도는 login_history에 따로 남긴다.
 * <p>
 * authserver(비밀번호 초기화)와 업무 모듈(사용자·조직 관리)이 함께 기록하므로 global에 둔다.
 */
@Getter
@Entity
@Table(name = "audit_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

	static final int DETAILS_MAX_LENGTH = 4000;
	private static final int IP_MAX_LENGTH = 45;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "actor_user_id", updatable = false)
	private Long actorUserId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "action", nullable = false, updatable = false, length = 50)
	private AuditAction action;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "target_type", nullable = false, updatable = false, length = 30)
	private AuditTargetType targetType;

	@Column(name = "target_id", updatable = false)
	private Long targetId;

	/** 변경 전·후 값 JSON 문자열({@link AuditChanges}). */
	@Column(name = "details", updatable = false, length = DETAILS_MAX_LENGTH)
	private String details;

	@Column(name = "ip_address", updatable = false, length = IP_MAX_LENGTH)
	private String ipAddress;

	@Column(name = "occurred_at", nullable = false, updatable = false)
	private Instant occurredAt;

	private AuditLog(
		final AuditActor actor,
		final AuditAction action,
		final AuditTargetType targetType,
		final Long targetId,
		final String details,
		final Instant occurredAt
	) {
		this.actorUserId = actor.userId();
		this.action = action;
		this.targetType = targetType;
		this.targetId = targetId;
		this.details = details;
		this.ipAddress = truncate(actor.ipAddress());
		this.occurredAt = occurredAt;
	}

	public static AuditLog of(
		final AuditActor actor,
		final AuditAction action,
		final AuditTargetType targetType,
		final Long targetId,
		final String details,
		final Instant occurredAt
	) {
		return new AuditLog(actor, action, targetType, targetId, details, occurredAt);
	}

	private static String truncate(final String value) {
		if (value == null) {
			return null;
		}
		return value.length() <= IP_MAX_LENGTH ? value : value.substring(0, IP_MAX_LENGTH);
	}
}

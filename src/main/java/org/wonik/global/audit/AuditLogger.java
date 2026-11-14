package org.wonik.global.audit;

import java.time.Clock;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

/**
 * 감사 로그 기록. 변경 작업과 같은 트랜잭션에서만 호출할 수 있다(MANDATORY):
 * 변경이 롤백되면 로그도 함께 롤백되고, 로그 기록이 실패하면 변경도 커밋되지 않는다.
 */
@Component
@RequiredArgsConstructor
public class AuditLogger {

	private final AuditLogRepository auditLogRepository;
	private final JsonMapper jsonMapper;
	private final Clock clock;

	@Transactional(propagation = Propagation.MANDATORY)
	public void record(
		final AuditActor actor,
		final AuditAction action,
		final AuditTargetType targetType,
		final Long targetId,
		final AuditChanges changes
	) {
		final String details = changes == null || changes.isEmpty() ? null : jsonMapper.writeValueAsString(changes.asMap());
		if (details != null && details.length() > AuditLog.DETAILS_MAX_LENGTH) {
			// 잘린 JSON을 남기지 않는다. 현재 기록 항목(이름·부서·직무 등 짧은 값)으로는 도달하지 않는 길이다.
			throw new IllegalStateException("감사 로그 details가 최대 길이를 넘었습니다: " + action);
		}
		auditLogRepository.save(AuditLog.of(actor, action, targetType, targetId, details, clock.instant()));
	}
}

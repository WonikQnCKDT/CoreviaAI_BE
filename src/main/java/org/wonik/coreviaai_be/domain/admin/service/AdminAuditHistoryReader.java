package org.wonik.coreviaai_be.domain.admin.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.wonik.coreviaai_be.domain.admin.service.result.AdminHistoryResult;
import org.wonik.global.audit.AuditLog;
import org.wonik.global.audit.AuditLogRepository;
import org.wonik.global.audit.AuditTargetType;
import org.wonik.global.common.pagination.CursorPage;
import org.wonik.global.common.pagination.CursorPageRequest;
import org.wonik.global.user.entity.User;
import org.wonik.global.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 감사 로그(audit_logs) 기반 대상별 변경 이력 조회 공통 컴포넌트.
 * 대상 존재 검증은 호출하는 도메인 서비스가 각 도메인의 404 오류 코드로 먼저 수행한다.
 */
@Component
@RequiredArgsConstructor
class AdminAuditHistoryReader {

	private final AuditLogRepository auditLogRepository;
	private final UserRepository userRepository;

	CursorPage<AdminHistoryResult> findByTarget(
		final AuditTargetType targetType,
		final Long targetId,
		final CursorPageRequest page
	) {
		final List<AuditLog> fetched = auditLogRepository.findByTarget(
			targetType,
			targetId,
			page.cursorId(),
			page.fetchPageable()
		);
		final CursorPage<AuditLog> logs = CursorPage.of(fetched, page.size(), AuditLog::getId);
		final Set<Long> actorIds = logs.items().stream()
			.map(AuditLog::getActorUserId)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
		final Map<Long, String> actorNames = actorIds.isEmpty()
			? Map.of()
			: userRepository.findAllById(actorIds).stream()
				.collect(Collectors.toMap(User::getId, User::getName));
		return logs.map(auditLog -> new AdminHistoryResult(auditLog, actorNames.get(auditLog.getActorUserId())));
	}
}

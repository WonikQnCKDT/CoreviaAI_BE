package org.wonik.coreviaai_be.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wonik.coreviaai_be.domain.admin.service.result.AdminHistoryResult;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.audit.AuditLog;
import org.wonik.global.audit.AuditLogRepository;
import org.wonik.global.audit.AuditTargetType;
import org.wonik.global.common.pagination.CursorCodec;
import org.wonik.global.common.pagination.CursorPage;
import org.wonik.global.common.pagination.CursorPageRequest;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;
import org.wonik.global.user.repository.UserRepository;

class AdminAuditHistoryReaderTest {

	private AuditLogRepository auditLogRepository;
	private UserRepository userRepository;
	private AdminAuditHistoryReader reader;

	@BeforeEach
	void setUp() {
		auditLogRepository = mock(AuditLogRepository.class);
		userRepository = mock(UserRepository.class);
		reader = new AdminAuditHistoryReader(auditLogRepository, userRepository);
	}

	@Test
	@DisplayName("이력이 비어 있으면 사용자 조회를 생략하고 빈 페이지를 반환한다")
	void emptyHistorySkipsUserLookup() {
		final CursorPageRequest page = CursorPageRequest.of(null, 20);
		when(auditLogRepository.findByTarget(eq(AuditTargetType.CODE), eq(1L), eq(null), any()))
			.thenReturn(List.of());

		final CursorPage<AdminHistoryResult> result = reader.findByTarget(AuditTargetType.CODE, 1L, page);

		assertThat(result.items()).isEmpty();
		assertThat(result.hasNext()).isFalse();
		assertThat(result.nextCursor()).isNull();
		verify(userRepository, never()).findAllById(any());
	}

	@Test
	@DisplayName("행위자 ID를 중복 제거해 한 번만 조회하고, 다음 페이지 커서를 계산한다")
	void batchesActorLookupAndComputesNextCursor() throws Exception {
		final AuditLog log3 = auditLog(30L, 1L, AuditAction.I18N_LABEL_CHANGED, AuditTargetType.I18N_LABEL, 7L);
		final AuditLog log2 = auditLog(20L, 1L, AuditAction.I18N_LABEL_CHANGED, AuditTargetType.I18N_LABEL, 7L);
		final AuditLog log1 = auditLog(10L, 2L, AuditAction.I18N_LABEL_CREATED, AuditTargetType.I18N_LABEL, 7L);
		final CursorPageRequest page = CursorPageRequest.of(null, 2);
		when(auditLogRepository.findByTarget(eq(AuditTargetType.I18N_LABEL), eq(7L), eq(null), any()))
			.thenReturn(List.of(log3, log2, log1));
		final User actor = userWithId(1L, "김관리");
		when(userRepository.findAllById(Set.of(1L))).thenReturn(List.of(actor));

		final CursorPage<AdminHistoryResult> result = reader.findByTarget(AuditTargetType.I18N_LABEL, 7L, page);

		assertThat(result.items()).hasSize(2);
		assertThat(result.items().get(0).auditLog().getId()).isEqualTo(30L);
		assertThat(result.items().get(0).actorName()).isEqualTo("김관리");
		assertThat(result.items().get(1).auditLog().getId()).isEqualTo(20L);
		assertThat(result.items().get(1).actorName()).isEqualTo("김관리");
		assertThat(result.hasNext()).isTrue();
		assertThat(result.nextCursor()).isEqualTo(CursorCodec.encode(20L));
		verify(userRepository).findAllById(Set.of(1L));
	}

	@Test
	@DisplayName("행위자가 null이거나 사용자 테이블에 없으면 actorName은 null이다")
	void missingOrNullActorYieldsNullActorName() throws Exception {
		final AuditLog log1 = auditLog(11L, null, AuditAction.CODE_CHANGED, AuditTargetType.CODE, 3L);
		final AuditLog log2 = auditLog(10L, 999L, AuditAction.CODE_CHANGED, AuditTargetType.CODE, 3L);
		final CursorPageRequest page = CursorPageRequest.of(null, 20);
		when(auditLogRepository.findByTarget(eq(AuditTargetType.CODE), eq(3L), eq(null), any()))
			.thenReturn(List.of(log1, log2));
		when(userRepository.findAllById(Set.of(999L))).thenReturn(List.of());

		final CursorPage<AdminHistoryResult> result = reader.findByTarget(AuditTargetType.CODE, 3L, page);

		assertThat(result.items()).hasSize(2);
		assertThat(result.items().get(0).actorName()).isNull();
		assertThat(result.items().get(1).actorName()).isNull();
		assertThat(result.hasNext()).isFalse();
		assertThat(result.nextCursor()).isNull();
	}

	private static AuditLog auditLog(
		final Long id,
		final Long actorUserId,
		final AuditAction action,
		final AuditTargetType targetType,
		final Long targetId
	) throws Exception {
		final AuditLog log = AuditLog.of(
			new AuditActor(actorUserId, "127.0.0.1"),
			action,
			targetType,
			targetId,
			"{}",
			Instant.parse("2026-11-28T10:00:00Z")
		);
		final Field idField = AuditLog.class.getDeclaredField("id");
		idField.setAccessible(true);
		idField.set(log, id);
		return log;
	}

	private static User userWithId(final Long id, final String name) throws Exception {
		final User user = User.create("admin01", name, SystemRole.SYSTEM_ADMIN, null, null);
		final Field idField = User.class.getDeclaredField("id");
		idField.setAccessible(true);
		idField.set(user, id);
		return user;
	}
}

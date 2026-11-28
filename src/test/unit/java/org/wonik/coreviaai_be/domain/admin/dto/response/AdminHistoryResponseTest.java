package org.wonik.coreviaai_be.domain.admin.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wonik.coreviaai_be.domain.admin.service.result.AdminHistoryResult;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.audit.AuditLog;
import org.wonik.global.audit.AuditTargetType;

import tools.jackson.databind.json.JsonMapper;

class AdminHistoryResponseTest {

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	@Test
	@DisplayName("저장된 JSON details를 객체(JsonNode)로 파싱해 반환한다")
	void parsesDetailsJsonIntoObject() {
		final Instant occurredAt = Instant.parse("2026-11-28T10:00:00Z");
		final AuditLog log = AuditLog.of(
			new AuditActor(10L, "127.0.0.1"),
			AuditAction.CODE_CHANGED,
			AuditTargetType.CODE,
			3L,
			"{\"groupCode\":\"RISK_LEVEL\",\"code\":\"NORMAL\",\"sortOrder\":{\"before\":1,\"after\":3}}",
			occurredAt
		);

		final AdminHistoryResponse response = AdminHistoryResponse.from(
			new AdminHistoryResult(log, "관리자"),
			jsonMapper
		);

		assertThat(response.action()).isEqualTo(AuditAction.CODE_CHANGED);
		assertThat(response.actorUserId()).isEqualTo(10L);
		assertThat(response.actorName()).isEqualTo("관리자");
		assertThat(response.ipAddress()).isEqualTo("127.0.0.1");
		assertThat(response.occurredAt()).isEqualTo(occurredAt);
		assertThat(response.details().get("groupCode").asString()).isEqualTo("RISK_LEVEL");
		assertThat(response.details().get("sortOrder").get("before").asInt()).isEqualTo(1);
		assertThat(response.details().get("sortOrder").get("after").asInt()).isEqualTo(3);
	}

	@Test
	@DisplayName("details·actorUserId·actorName이 null이어도 예외 없이 변환한다")
	void handlesNullDetailsAndActor() {
		final Instant occurredAt = Instant.parse("2026-11-28T10:00:00Z");
		final AuditLog log = AuditLog.of(
			new AuditActor(null, null),
			AuditAction.I18N_LABEL_CHANGED,
			AuditTargetType.I18N_LABEL,
			5L,
			null,
			occurredAt
		);

		final AdminHistoryResponse response = AdminHistoryResponse.from(
			new AdminHistoryResult(log, null),
			jsonMapper
		);

		assertThat(response.actorUserId()).isNull();
		assertThat(response.actorName()).isNull();
		assertThat(response.details()).isNull();
		assertThat(response.ipAddress()).isNull();
		assertThat(response.occurredAt()).isEqualTo(occurredAt);
	}
}

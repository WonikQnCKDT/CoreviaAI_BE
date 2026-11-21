package org.wonik.global.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuditChangesTest {

	@Test
	@DisplayName("값이 바뀐 필드만 변경 전·후로 기록한다")
	void recordsOnlyChangedFields() {
		final AuditChanges changes = AuditChanges.create()
			.change("name", "홍길동", "홍길동")
			.change("departmentId", 1L, 2L)
			.change("jobTitle", null, "QA");

		assertThat(changes.asMap()).containsOnlyKeys("departmentId", "jobTitle");
		assertThat(changes.asMap().get("departmentId")).isEqualTo(Map.of("before", 1L, "after", 2L));
	}

	@Test
	@DisplayName("바뀐 값이 없으면 비어 있다")
	void emptyWhenNothingChanged() {
		assertThat(AuditChanges.create().change("name", "a", "a").isEmpty()).isTrue();
	}
}

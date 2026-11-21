package org.wonik.coreviaai_be.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdminErrorLogServicePrefixPatternTest {

	@Test
	@DisplayName("경로 조건이 없으면 전체와 일치하는 '%'")
	void emptyPath() {
		assertThat(AdminErrorLogService.prefixPattern(null)).isEqualTo("%");
		assertThat(AdminErrorLogService.prefixPattern(" ")).isEqualTo("%");
	}

	@Test
	@DisplayName("앞부분 일치 패턴을 만들고 대소문자는 그대로, LIKE 와일드카드와 이스케이프 문자는 글자 그대로 찾게 한다")
	void prefixAndEscape() {
		assertThat(AdminErrorLogService.prefixPattern(" /api/Projects ")).isEqualTo("/api/Projects%");
		assertThat(AdminErrorLogService.prefixPattern("/api/a_b%")).isEqualTo("/api/a\\_b\\%%");
		assertThat(AdminErrorLogService.prefixPattern("/a\\b")).isEqualTo("/a\\\\b%");
	}
}

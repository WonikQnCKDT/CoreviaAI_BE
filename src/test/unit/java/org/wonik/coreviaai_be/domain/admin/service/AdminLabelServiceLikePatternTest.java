package org.wonik.coreviaai_be.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdminLabelServiceLikePatternTest {

	@Test
	@DisplayName("검색어가 없으면 전체와 일치하는 '%'")
	void emptyKeyword() {
		assertThat(AdminLabelService.likePattern(null)).isEqualTo("%");
		assertThat(AdminLabelService.likePattern("  ")).isEqualTo("%");
	}

	@Test
	@DisplayName("소문자로 바꾸고 LIKE 와일드카드(%, _)와 이스케이프 문자를 글자 그대로 찾도록 이스케이프한다")
	void escapesWildcards() {
		assertThat(AdminLabelService.likePattern(" TASK_STATUS ")).isEqualTo("%task\\_status%");
		assertThat(AdminLabelService.likePattern("100%")).isEqualTo("%100\\%%");
		assertThat(AdminLabelService.likePattern("a\\b")).isEqualTo("%a\\\\b%");
	}
}

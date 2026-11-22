package org.wonik.coreviaai_be.domain.code.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CommonCodeTest {

	@Test
	@DisplayName("라벨 키 규칙: 그룹은 codeGroup.<그룹>, 코드는 code.<그룹>.<코드>")
	void labelKeyRule() {
		assertThat(CommonCode.groupLabelKey("TASK_STATUS")).isEqualTo("codeGroup.TASK_STATUS");
		assertThat(CommonCode.codeLabelKey("TASK_STATUS", "IN_PROGRESS")).isEqualTo("code.TASK_STATUS.IN_PROGRESS");
	}

	@Test
	@DisplayName("가장 긴 그룹 코드(50자)·코드 값(40자)으로 만든 라벨 키도 label_key 컬럼(100자) 안에 들어간다")
	void longestLabelKeyFits() {
		final String group = "G".repeat(50);
		final String code = "C".repeat(40);
		assertThat(group).matches(CommonCode.GROUP_CODE_PATTERN);
		assertThat(code).matches(CommonCode.CODE_PATTERN);
		assertThat(CommonCode.groupLabelKey(group).length()).isLessThanOrEqualTo(100);
		assertThat(CommonCode.codeLabelKey(group, code).length()).isLessThanOrEqualTo(100);
	}

	@ParameterizedTest
	@ValueSource(strings = {"A", "TODO", "IN_PROGRESS", "A1", "A_1_B"})
	@DisplayName("코드 형식: 대문자로 시작하는 대문자·숫자·밑줄")
	void validCodes(final String code) {
		assertThat(code).matches(CommonCode.CODE_PATTERN);
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "a", "todo", "1A", "_A", "A-B", "A B", "가"})
	@DisplayName("코드 형식 위반: 빈 값·소문자·숫자 시작·특수문자")
	void invalidCodes(final String code) {
		assertThat(code).doesNotMatch(CommonCode.CODE_PATTERN);
	}

	@Test
	@DisplayName("길이 경계: 코드 값은 40자까지, 그룹 코드는 50자까지 허용")
	void lengthBoundaries() {
		assertThat("A".repeat(40)).matches(CommonCode.CODE_PATTERN);
		assertThat("A".repeat(41)).doesNotMatch(CommonCode.CODE_PATTERN);
		assertThat("A".repeat(50)).matches(CommonCode.GROUP_CODE_PATTERN);
		assertThat("A".repeat(51)).doesNotMatch(CommonCode.GROUP_CODE_PATTERN);
	}
}

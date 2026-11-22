package org.wonik.global.common.masking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * NFR-06 패턴 마스킹. 값만 가리고 키 이름·문장은 남기는지, 가릴 것이 없으면 그대로인지, 긴 입력에서도 빨리 끝나는지 본다.
 * 테스트 값은 모두 가짜다.
 */
class SensitiveValueMaskerTest {

	private static String mask(final String text) {
		return SensitiveValueMasker.mask(text);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@DisplayName("null·빈 문자열은 그대로")
	void nullAndEmpty(final String text) {
		assertThat(mask(text)).isEqualTo(text);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"서버에서 오류가 발생했습니다",
		"Index 3 out of bounds for length 3",
		"Cannot invoke \"org.wonik.Foo.getName()\" because \"foo\" is null",
		"\tat org.wonik.global.jwt.JwtTokenProvider.parse(JwtTokenProvider.java:42)",
		"\tat org.wonik.authserver.PasswordPolicy.check(PasswordPolicy.java:120)",
		"Could not resolve placeholder 'app.jwt.secret'",
		"project 2026-11-21 code PRJ-001 id 1795246429489 version 12",
		"java.lang.Object@6d06d69c",
		"프로젝트 A사 공정 개선 과제"
	})
	@DisplayName("가릴 값이 없는 문장·스택 프레임·고객사·공정 문구는 바꾸지 않는다")
	void unchanged(final String text) {
		assertThat(mask(text)).isEqualTo(text);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
		"password=Secr3t!|password=****",
		"db.password = Secr3t!, user=hong|db.password = ****, user=hong",
		"newPassword: Secr3t!|newPassword: ****",
		"PWD=abc&next=1|PWD=****&next=1",
		"passwd=abc;x=1|passwd=****;x=1",
		"jwt.secret=c2VjcmV0|jwt.secret=****",
		"client_secret: 'abc def'|client_secret: '****'",
		"access_token=abc123 refresh_token=def456|access_token=**** refresh_token=****",
		"apiKey=AKIA123|apiKey=****",
		"api-key: AKIA123|api-key: ****",
		"Cookie: SESSION=abc|Cookie: ****",
		"sessionId=s-123)|sessionId=****)",
		"LoginUser[userId=1, sessionId=abc-123, systemRole=USER]|LoginUser[userId=1, sessionId=****, systemRole=USER]",
		"credentials=x1|credentials=****"
	})
	@DisplayName("비밀 키 이름의 key=value·key: value 값만 가리고 키 이름은 남긴다")
	void secretKeyValue(final String text, final String expected) {
		assertThat(mask(text)).isEqualTo(expected);
	}

	@Test
	@DisplayName("JSON의 \"key\":\"value\"는 따옴표를 남기고 값만 가린다(이스케이프된 따옴표 포함)")
	void jsonKeyValue() {
		assertThat(mask("{\"loginId\":\"hong\",\"password\":\"Se\\\"cr3t\",\"accessToken\" : \"abc\"}"))
			.isEqualTo("{\"loginId\":\"hong\",\"password\":\"****\",\"accessToken\" : \"****\"}");
	}

	@Test
	@DisplayName("JWT는 어디에 있든 통째로 가린다")
	void jwt() {
		final String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.c2lnbmF0dXJl";
		assertThat(mask("invalid jwt " + jwt + " rejected")).isEqualTo("invalid jwt **** rejected");
		assertThat(mask("cookie value(" + jwt + ")")).isEqualTo("cookie value(****)");
	}

	@Test
	@DisplayName("Bearer·Basic 자격 증명은 스킴 이름을 남기고 값을 가린다")
	void authorizationScheme() {
		assertThat(mask("header Bearer abc.def-ghi_jkl= end")).isEqualTo("header Bearer **** end");
		assertThat(mask("Authorization: Basic dXNlcjpwYXNz")).isEqualTo("Authorization: **** ****");
	}

	@Test
	@DisplayName("PostgreSQL 상세 Key (컬럼)=(값)의 값만 가리고 컬럼 이름과 뒤 문구는 남긴다")
	void postgresKeyDetail() {
		assertThat(mask("Detail: Key (login_id)=(hong) already exists."))
			.isEqualTo("Detail: Key (login_id)=(****) already exists.");
		assertThat(mask("Key (group_id, code)=(3, A)B) already exists."))
			.isEqualTo("Key (group_id, code)=(****) already exists.");
		assertThat(mask("Key (department_id)=(99) is not present in table \"departments\"."))
			.isEqualTo("Key (department_id)=(****) is not present in table \"departments\".");
		assertThat(mask("Key (id)=(7) is still referenced from table \"users\"."))
			.isEqualTo("Key (id)=(****) is still referenced from table \"users\".");
		assertThat(mask("Key (email)=(abc")).isEqualTo("Key (email)=(****)");
	}

	@Test
	@DisplayName("PostgreSQL 상세 Failing row contains (...)는 행 값 전체를 가린다")
	void postgresFailingRow() {
		assertThat(mask("Detail: Failing row contains (1, hong, null, 2026-11-21).\nnext"))
			.isEqualTo("Detail: Failing row contains (****)\nnext");
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
		"mail hong.gil-dong+1@example.co.kr sent|mail **** sent",
		"<user@example.com>|<****>"
	})
	@DisplayName("이메일을 가린다")
	void email(final String text, final String expected) {
		assertThat(mask(text)).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
		"tel 010-1234-5678|tel ****",
		"tel 01012345678.|tel ****.",
		"tel 011.123.4567|tel ****",
		"office 02-123-4567|office ****",
		"office 031 1234 5678|office ****",
		"voip 070-1234-5678|voip ****",
		"rrn 900101-1234567|rrn ****",
		"rrn 9001012234567|rrn ****"
	})
	@DisplayName("한국 휴대폰·전화번호·주민등록번호 형태를 가린다")
	void koreanNumbers(final String text, final String expected) {
		assertThat(mask(text)).isEqualTo(expected);
	}

	@ParameterizedTest
	@ValueSource(strings = {"id 1010-1234-5678", "count 12345678", "id 01012345678901", "date 901301-1234567"})
	@DisplayName("앞뒤로 숫자가 붙었거나 날짜가 맞지 않는 숫자열은 번호로 보지 않는다")
	void notKoreanNumbers(final String text) {
		assertThat(mask(text)).isEqualTo(text);
	}

	@Test
	@DisplayName("스택 트레이스의 여러 Caused by 메시지를 모두 가리고 프레임은 그대로 둔다")
	void stackTraceWithCauses() {
		final String trace = """
			java.lang.IllegalStateException: login failed password=Secr3t!
			\tat org.wonik.authserver.AuthService.login(AuthService.java:10)
			Caused by: org.springframework.dao.DataIntegrityViolationException: Key (login_id)=(hong) already exists.
			\tat org.wonik.global.jwt.JwtTokenProvider.parse(JwtTokenProvider.java:42)
			Caused by: java.lang.RuntimeException: token eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.c2ln for user@example.com
			\t... 3 more
			""";

		assertThat(mask(trace)).isEqualTo("""
			java.lang.IllegalStateException: login failed password=****
			\tat org.wonik.authserver.AuthService.login(AuthService.java:10)
			Caused by: org.springframework.dao.DataIntegrityViolationException: Key (login_id)=(****) already exists.
			\tat org.wonik.global.jwt.JwtTokenProvider.parse(JwtTokenProvider.java:42)
			Caused by: java.lang.RuntimeException: token **** for ****
			\t... 3 more
			""");
	}

	@Test
	@DisplayName("마스킹 결과에 원래 값이 남지 않는다(복원 불가)")
	void irreversible() {
		final String masked = mask("password=Secr3t! token=eyJa.eyJb.c mail=a@b.io tel=010-9999-8888");
		assertThat(masked).doesNotContain("Secr3t", "eyJa", "a@b.io", "9999");
	}

	@Test
	@DisplayName("긴 입력·패턴 경계를 반복한 악의적 입력도 시간 제한 안에 끝난다(ReDoS 방지)")
	void longInputsFinishQuickly() {
		final int length = 200_000;
		final String[] inputs = {
			"a".repeat(length),
			"a.".repeat(length / 2),
			"password".repeat(length / 8),
			"password=".repeat(length / 9),
			"\"password\":\"" + "\\\"".repeat(length / 2),
			"Key (x)=(".repeat(length / 9),
			"eyJ.".repeat(length / 4),
			"1".repeat(length),
			"0-".repeat(length / 2),
			"a@".repeat(length / 2),
			"Bearer ".repeat(length / 7),
			"Failing row contains (".repeat(length / 22),
			"x".repeat(length) + "@example.com"
		};
		assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
			for (final String input : inputs) {
				mask(input);
			}
		});
	}
}

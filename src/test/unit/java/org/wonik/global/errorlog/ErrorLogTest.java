package org.wonik.global.errorlog;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 에러 로그 한 건을 만들 때의 마스킹·길이 제한·원인(Caused by) 기록. 테스트 값은 모두 가짜다.
 */
class ErrorLogTest {

	private static final Instant NOW = Instant.parse("2026-11-21T00:00:00Z");

	@Test
	@DisplayName("요청 정보·예외 클래스·마스킹한 메시지와 원인까지 포함한 스택 트레이스를 남긴다")
	void recordsMaskedErrorWithCauses() {
		final IllegalStateException error = new IllegalStateException(
			"login failed password=Secr3t! for hong@example.com",
			new RuntimeException("Key (login_id)=(hong) already exists.", new IllegalArgumentException("tel 010-1234-5678"))
		);

		final ErrorLog log = ErrorLog.of(NOW, "POST", "/api/auth/login", 7L, error);

		assertThat(log.getOccurredAt()).isEqualTo(NOW);
		assertThat(log.getHttpMethod()).isEqualTo("POST");
		assertThat(log.getApiPath()).isEqualTo("/api/auth/login");
		assertThat(log.getUserId()).isEqualTo(7L);
		assertThat(log.getErrorType()).isEqualTo("java.lang.IllegalStateException");
		assertThat(log.getMessage()).isEqualTo("login failed password=**** for ****");
		assertThat(log.getStackTrace())
			.startsWith("java.lang.IllegalStateException: login failed password=**** for ****\n\tat ")
			.contains("Caused by: java.lang.RuntimeException: Key (login_id)=(****) already exists.\n\tat ")
			.contains("Caused by: java.lang.IllegalArgumentException: tel ****\n")
			.contains("\tat " + ErrorLogTest.class.getName())
			.doesNotContain("Secr3t", "hong@", "(hong)", "1234-5678");
	}

	@Test
	@DisplayName("요청 밖 오류·로그인 전 요청·메시지 없는 예외는 해당 값을 비워 둔다")
	void nullableFields() {
		final ErrorLog log = ErrorLog.of(NOW, null, null, null, new NullPointerException());

		assertThat(log.getHttpMethod()).isNull();
		assertThat(log.getApiPath()).isNull();
		assertThat(log.getUserId()).isNull();
		assertThat(log.getMessage()).isNull();
		assertThat(log.getErrorType()).isEqualTo("java.lang.NullPointerException");
		assertThat(log.getStackTrace()).startsWith("java.lang.NullPointerException\n");
	}

	@Test
	@DisplayName("메시지는 가린 뒤 2000자로 자르고, 경로는 300자로 자른다(경계값: 2000자는 그대로)")
	void truncatesAfterMasking() {
		final String exact = "a".repeat(ErrorLog.MESSAGE_MAX_LENGTH);
		assertThat(ErrorLog.of(NOW, "GET", "/x", null, new RuntimeException(exact)).getMessage()).isEqualTo(exact);

		// 마스킹 전에 자르면 2000자 경계에 걸린 JWT 조각이 남는다. 가린 뒤 자르므로 남지 않는다.
		final String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.c2lnbmF0dXJl";
		final String longMessage = "x".repeat(ErrorLog.MESSAGE_MAX_LENGTH - 5) + " " + jwt + " tail";
		final ErrorLog log = ErrorLog.of(NOW, "GET", "/" + "p".repeat(400), null, new RuntimeException(longMessage));

		assertThat(log.getMessage()).hasSize(ErrorLog.MESSAGE_MAX_LENGTH).endsWith(" ****").doesNotContain("eyJ");
		assertThat(log.getApiPath()).hasSize(ErrorLog.API_PATH_MAX_LENGTH);
	}

	@Test
	@DisplayName("메시지가 아주 길어도 저장 길이를 넘지 않는다")
	void veryLongMessage() {
		final String huge = "password=" + "s".repeat(500_000);
		final ErrorLog log = ErrorLog.of(NOW, "GET", "/x", null, new RuntimeException(huge));

		assertThat(log.getMessage()).isEqualTo("password=****");
		assertThat(log.getStackTrace().length()).isLessThanOrEqualTo(ErrorLog.STACK_TRACE_MAX_LENGTH);
	}

	@Test
	@DisplayName("원인 단계와 원인별 프레임 수를 제한하고, 원인 순환이 있어도 끝난다")
	void limitsCausesAndFrames() {
		RuntimeException chain = new RuntimeException("root");
		for (int i = 0; i < StackTraceFormatter.MAX_CAUSES + 3; i++) {
			chain = new RuntimeException("level " + i, chain);
		}
		final String trace = StackTraceFormatter.format(chain);
		assertThat(trace.split("Caused by: ", -1)).hasSize(StackTraceFormatter.MAX_CAUSES + 1);
		assertThat(trace).endsWith("Caused by: ... (원인 " + StackTraceFormatter.MAX_CAUSES + "단계 이후 생략)\n");

		final RuntimeException deep = new RuntimeException("deep");
		final StackTraceElement[] frames = new StackTraceElement[StackTraceFormatter.MAX_FRAMES_PER_CAUSE + 5];
		Arrays.setAll(frames, i -> new StackTraceElement("C", "m", "C.java", i));
		deep.setStackTrace(frames);
		final String deepTrace = StackTraceFormatter.format(deep);
		assertThat(deepTrace.split("\tat ", -1)).hasSize(StackTraceFormatter.MAX_FRAMES_PER_CAUSE + 1);
		assertThat(deepTrace).endsWith("\t... 5 more\n");

		final RuntimeException first = new RuntimeException("first");
		final RuntimeException second = new RuntimeException("second", first);
		first.initCause(second);
		assertThat(StackTraceFormatter.format(first).split("Caused by: ", -1)).hasSize(2);
	}
}

package org.wonik.authserver.domain.credential.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wonik.authserver.domain.auth.policy.LoginLockPolicy;

class LocalCredentialTest {

	private static final Instant NOW = Instant.parse("2026-11-09T00:00:00Z");
	private static final int MAX = LoginLockPolicy.MAX_CONSECUTIVE_FAILURES;

	private LocalCredential newCredential() {
		return LocalCredential.issue(1L, "{noop}hash", NOW);
	}

	@Test
	@DisplayName("4회 연속 실패까지는 잠기지 않고 5회째 실패에서 잠긴다")
	void locksOnFifthConsecutiveFailure() {
		final LocalCredential credential = newCredential();

		for (int i = 1; i < MAX; i++) {
			assertThat(credential.recordLoginFailure(MAX, NOW)).isFalse();
		}
		assertThat(credential.isLocked()).isFalse();

		assertThat(credential.recordLoginFailure(MAX, NOW)).isTrue();
		assertThat(credential.isLocked()).isTrue();
		assertThat(credential.getLockedAt()).isEqualTo(NOW);
	}

	@Test
	@DisplayName("로그인에 성공하면 연속 실패 횟수가 0으로 돌아간다")
	void successResetsFailureCount() {
		final LocalCredential credential = newCredential();
		credential.recordLoginFailure(MAX, NOW);
		credential.recordLoginFailure(MAX, NOW);

		credential.recordLoginSuccess();

		assertThat(credential.getFailedLoginCount()).isZero();
	}

	@Test
	@DisplayName("관리자 초기화는 잠금을 풀고 다음 로그인 후 비밀번호 변경을 요구한다")
	void resetUnlocksAndRequiresChange() {
		final LocalCredential credential = newCredential();
		credential.changePassword("{noop}mine", NOW);
		for (int i = 0; i < MAX; i++) {
			credential.recordLoginFailure(MAX, NOW);
		}

		credential.resetPassword("{noop}temp", NOW.plusSeconds(60));

		assertThat(credential.isLocked()).isFalse();
		assertThat(credential.getFailedLoginCount()).isZero();
		assertThat(credential.isMustChangePassword()).isTrue();
		assertThat(credential.getPasswordHash()).isEqualTo("{noop}temp");
	}

	@Test
	@DisplayName("발급한 비밀번호는 변경 강제 상태이고, 본인이 바꾸면 풀린다")
	void issuedPasswordMustBeChanged() {
		final LocalCredential credential = newCredential();
		assertThat(credential.isMustChangePassword()).isTrue();

		credential.changePassword("{noop}mine", NOW.plusSeconds(60));

		assertThat(credential.isMustChangePassword()).isFalse();
		assertThat(credential.getPasswordChangedAt()).isEqualTo(NOW.plusSeconds(60));
	}
}

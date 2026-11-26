package org.wonik.coreviaai_be.domain.account.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

class AccountRequestTest {

	private static final Instant REVIEWED_AT = Instant.parse("2026-11-26T04:00:00Z");

	@Test
	@DisplayName("신청은 대기 상태이고 아이디와 이름이 모두 같아야 본인 신청이다")
	void submitStartsPendingAndMatchesApplicant() {
		final AccountRequest request = AccountRequest.submit("hong.gd", "홍길동", 3L, "프로젝트 참여");

		assertThat(request.getStatus()).isEqualTo(AccountRequestStatus.PENDING);
		assertThat(request.matchesApplicant("hong.gd", "홍길동")).isTrue();
		assertThat(request.matchesApplicant("hong.gd", "다른이름")).isFalse();
		assertThat(request.getRejectReason()).isNull();
		assertThat(request.getUserId()).isNull();
	}

	@Test
	@DisplayName("승인은 확정 부서와 생성된 사용자를 연결하고 다시 처리할 수 없다")
	void approveThenSecondTransitionIsRejected() {
		final AccountRequest request = AccountRequest.submit("hong.gd", "홍길동", null, "협업");

		request.approve(9L, 20L, 4L, REVIEWED_AT);

		assertThat(request.getStatus()).isEqualTo(AccountRequestStatus.APPROVED);
		assertThat(request.getDepartmentId()).isEqualTo(4L);
		assertThat(request.getUserId()).isEqualTo(20L);
		assertThat(request.getReviewedBy()).isEqualTo(9L);
		assertThat(request.getReviewedAt()).isEqualTo(REVIEWED_AT);
		assertThatThrownBy(() -> request.reject(9L, "중복", REVIEWED_AT))
			.isInstanceOf(CustomException.class)
			.extracting(ex -> ((CustomException) ex).getErrorCode())
			.isEqualTo(ErrorCode.ACCOUNT_REQUEST_ALREADY_REVIEWED);
	}

	@Test
	@DisplayName("반려는 사유가 있어야 하고 앞뒤 공백은 제거한다")
	void rejectRequiresReason() {
		final AccountRequest blank = AccountRequest.submit("a", "이름", null, "목적");
		assertThatThrownBy(() -> blank.reject(1L, "  ", REVIEWED_AT))
			.isInstanceOf(CustomException.class)
			.extracting(ex -> ((CustomException) ex).getErrorCode())
			.isEqualTo(ErrorCode.REJECT_REASON_REQUIRED);

		final AccountRequest request = AccountRequest.submit("a", "이름", null, "목적");
		request.reject(1L, " 소속 확인 불가 ", REVIEWED_AT);
		assertThat(request.getStatus()).isEqualTo(AccountRequestStatus.REJECTED);
		assertThat(request.getRejectReason()).isEqualTo("소속 확인 불가");
		assertThat(request.getUserId()).isNull();
	}
}

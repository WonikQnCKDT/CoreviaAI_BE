package org.wonik.global.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.wonik.global.common.exception.ErrorCode;

class ApiResponseTest {

	@Test
	void errorFromErrorCodeIncludesErrorCodeName() {
		final ApiResponse<?> response = ApiResponse.error(ErrorCode.SESSION_EXPIRED);

		assertThat(response.isSuccess()).isFalse();
		assertThat(response.getResponse()).isNull();
		assertThat(response.getError()).isNotNull();
		assertThat(response.getError().code()).isEqualTo("SESSION_EXPIRED");
		assertThat(response.getError().message()).isEqualTo(ErrorCode.SESSION_EXPIRED.getMessage());
		assertThat(response.getError().status()).isEqualTo(401);
	}

	@Test
	void errorWithPayloadIncludesErrorCodeName() {
		final Map<String, Object> payload = Map.of("assignedTaskCount", 3);
		final ApiResponse<Map<String, Object>> response = ApiResponse.error(payload, ErrorCode.MEMBER_HAS_ASSIGNED_TASKS);

		assertThat(response.isSuccess()).isFalse();
		assertThat(response.getResponse()).isEqualTo(payload);
		assertThat(response.getError().code()).isEqualTo("MEMBER_HAS_ASSIGNED_TASKS");
		assertThat(response.getError().status()).isEqualTo(409);
	}

	@Test
	void errorFromHttpStatusUsesStatusName() {
		final ApiResponse<?> response = ApiResponse.error("입력 오류", HttpStatus.BAD_REQUEST);

		assertThat(response.isSuccess()).isFalse();
		assertThat(response.getError().code()).isEqualTo("BAD_REQUEST");
		assertThat(response.getError().message()).isEqualTo("입력 오류");
		assertThat(response.getError().status()).isEqualTo(400);
	}

	@Test
	void errorFromCustomStatusCodeFallsBackToNumericCode() {
		final ApiResponse<?> response = ApiResponse.error("커스텀 오류", HttpStatusCode.valueOf(599));

		assertThat(response.getError().code()).isEqualTo("HTTP_599");
		assertThat(response.getError().status()).isEqualTo(599);
	}
}

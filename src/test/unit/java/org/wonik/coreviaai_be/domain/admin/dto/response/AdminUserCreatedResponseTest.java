package org.wonik.coreviaai_be.domain.admin.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.wonik.coreviaai_be.domain.admin.service.result.AdminUserCreatedResult;
import org.wonik.coreviaai_be.domain.admin.service.result.AdminUserResult;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;

class AdminUserCreatedResponseTest {

	@Test
	void toStringMasksTemporaryPassword() {
		final User user = User.create("hong.gd", "홍길동", SystemRole.USER, 1L, "개발자");
		final AdminUserCreatedResult result = new AdminUserCreatedResult(new AdminUserResult(user, "개발팀"), "Tmp!Pass1234");
		final AdminUserCreatedResponse response = AdminUserCreatedResponse.from(result);

		assertThat(result.toString()).doesNotContain("Tmp!Pass1234").contains("***");
		assertThat(response.toString()).doesNotContain("Tmp!Pass1234").contains("***");
		assertThat(response.temporaryPassword()).isEqualTo("Tmp!Pass1234");
	}
}

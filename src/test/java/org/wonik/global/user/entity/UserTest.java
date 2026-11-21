package org.wonik.global.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wonik.global.common.type.SystemRole;

class UserTest {

	@Test
	@DisplayName("새 사용자는 활성 상태이고 입력한 부서·직무를 가진다")
	void defaults() {
		final User user = User.create("user01", "이름", SystemRole.USER, 1L, "개발");

		assertThat(user.isActive()).isTrue();
		assertThat(user.getDepartmentId()).isEqualTo(1L);
		assertThat(user.getJobTitle()).isEqualTo("개발");
	}

	@Test
	@DisplayName("시스템 역할을 바꿀 수 있다")
	void changeSystemRole() {
		final User user = User.create("user01", "이름", SystemRole.USER, 1L, null);

		user.changeSystemRole(SystemRole.EXECUTIVE);

		assertThat(user.getSystemRole()).isEqualTo(SystemRole.EXECUTIVE);
	}

	@Test
	@DisplayName("비활성화·재활성화")
	void deactivateAndActivate() {
		final User user = User.create("user01", "이름", SystemRole.USER, 1L, null);

		user.deactivate();
		assertThat(user.isActive()).isFalse();
		assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);

		user.activate();
		assertThat(user.isActive()).isTrue();
	}
}

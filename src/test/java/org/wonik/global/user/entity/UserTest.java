package org.wonik.global.user.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wonik.global.common.type.SystemRole;

class UserTest {

	@Test
	@DisplayName("새 사용자는 활성 상태이고 프로젝트 생성 권한이 없다")
	void defaults() {
		final User user = User.create("user01", "이름", SystemRole.USER, 1L, "개발");

		assertThat(user.isActive()).isTrue();
		assertThat(user.isCanCreateProject()).isFalse();
		assertThat(user.getDepartmentId()).isEqualTo(1L);
		assertThat(user.getJobTitle()).isEqualTo("개발");
	}

	@Test
	@DisplayName("프로젝트 생성 권한은 일반 사용자만 받을 수 있다")
	void projectCreationOnlyForUser() {
		final User executive = User.create("exec01", "경영진", SystemRole.EXECUTIVE, 1L, null);

		assertThatThrownBy(executive::grantProjectCreation).isInstanceOf(IllegalStateException.class);
		assertThat(executive.isCanCreateProject()).isFalse();
	}

	@Test
	@DisplayName("일반 사용자가 아닌 역할로 바뀌면 프로젝트 생성 권한이 회수되고, 일반 사용자 간 변경은 유지된다")
	void roleChangeRevokesProjectCreation() {
		final User user = User.create("user01", "이름", SystemRole.USER, 1L, null);
		user.grantProjectCreation();

		user.changeSystemRole(SystemRole.USER);
		assertThat(user.isCanCreateProject()).isTrue();

		user.changeSystemRole(SystemRole.EXECUTIVE);
		assertThat(user.isCanCreateProject()).isFalse();

		user.changeSystemRole(SystemRole.USER);
		assertThat(user.isCanCreateProject()).isFalse();
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

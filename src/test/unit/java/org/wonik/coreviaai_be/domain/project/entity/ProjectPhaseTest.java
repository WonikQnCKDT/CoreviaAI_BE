package org.wonik.coreviaai_be.domain.project.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProjectPhaseTest {

	@Test
	@DisplayName("사용자 정의 단계는 이름·순서를 변경하고 상태·게이트 초기값을 유지한다")
	void renameAndReorder() {
		final ProjectPhase phase = ProjectPhase.of(1L, "준비", 1, null);

		phase.rename("기획");
		phase.reorder(2);

		assertThat(phase.getName()).isEqualTo("기획");
		assertThat(phase.getPhaseOrder()).isEqualTo(2);
		assertThat(phase.getStatus().name()).isEqualTo("NOT_STARTED");
		assertThat(phase.getGateStatus().name()).isEqualTo("PENDING");
	}
}

package org.wonik.coreviaai_be.domain.wbs.entity.type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus.DONE;
import static org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus.IN_PROGRESS;
import static org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus.REVIEW;
import static org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus.TODO;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class WbsTaskStatusTest {

	@ParameterizedTest
	@EnumSource(WbsTaskStatus.class)
	@DisplayName("하위작업이 없거나 null이면 저장된 자신의 상태를 그대로 반환한다")
	void noSubtasksReturnsStoredStatus(final WbsTaskStatus stored) {
		assertThat(WbsTaskStatus.deriveFromSubtasks(stored, List.of())).isEqualTo(stored);
		assertThat(WbsTaskStatus.deriveFromSubtasks(stored, null)).isEqualTo(stored);
	}

	@Test
	@DisplayName("부모 저장 상태가 미완료여도 하위작업이 모두 DONE이면 DONE으로 파생한다")
	void allSubtasksDone() {
		assertThat(WbsTaskStatus.deriveFromSubtasks(TODO, List.of(DONE))).isEqualTo(DONE);
		assertThat(WbsTaskStatus.deriveFromSubtasks(IN_PROGRESS, List.of(DONE, DONE))).isEqualTo(DONE);
		assertThat(WbsTaskStatus.deriveFromSubtasks(REVIEW, List.of(DONE, DONE))).isEqualTo(DONE);
	}

	@Test
	@DisplayName("하위작업이 모두 TODO면 부모 저장 상태와 무관하게 TODO로 파생한다")
	void allSubtasksTodo() {
		assertThat(WbsTaskStatus.deriveFromSubtasks(DONE, List.of(TODO))).isEqualTo(TODO);
		assertThat(WbsTaskStatus.deriveFromSubtasks(IN_PROGRESS, List.of(TODO, TODO))).isEqualTo(TODO);
	}

	@Test
	@DisplayName("하위작업이 REVIEW와 DONE만으로 구성되면 REVIEW로 파생한다")
	void reviewOrDoneSubtasks() {
		assertThat(WbsTaskStatus.deriveFromSubtasks(TODO, List.of(REVIEW))).isEqualTo(REVIEW);
		assertThat(WbsTaskStatus.deriveFromSubtasks(DONE, List.of(REVIEW, DONE))).isEqualTo(REVIEW);
	}

	@Test
	@DisplayName("하위작업에 TODO·IN_PROGRESS가 섞이면 부모 저장 상태가 DONE이어도 IN_PROGRESS로 파생한다")
	void mixedSubtasksInProgress() {
		assertThat(WbsTaskStatus.deriveFromSubtasks(DONE, List.of(DONE, TODO))).isEqualTo(IN_PROGRESS);
		assertThat(WbsTaskStatus.deriveFromSubtasks(DONE, List.of(DONE, IN_PROGRESS))).isEqualTo(IN_PROGRESS);
		assertThat(WbsTaskStatus.deriveFromSubtasks(TODO, List.of(TODO, REVIEW))).isEqualTo(IN_PROGRESS);
		assertThat(WbsTaskStatus.deriveFromSubtasks(TODO, List.of(IN_PROGRESS))).isEqualTo(IN_PROGRESS);
	}
}

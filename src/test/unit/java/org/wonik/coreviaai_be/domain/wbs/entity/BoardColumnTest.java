package org.wonik.coreviaai_be.domain.wbs.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;

/**
 * 칸반 컬럼 규칙(FR-TSK-02): 기본 컬럼, 기본 이름 라벨 키, WIP 초과 판정 경계.
 */
class BoardColumnTest {

	@Test
	@DisplayName("기본 컬럼: 대기·진행·검토·완료 4개를 상태 코드 순서로 1부터, 기본 이름(null)·WIP 없음으로 만든다")
	void defaults() {
		final List<BoardColumn> columns = BoardColumn.defaults(7L);

		assertThat(columns).extracting(BoardColumn::getStatusCategory)
			.containsExactly(WbsTaskStatus.TODO, WbsTaskStatus.IN_PROGRESS, WbsTaskStatus.REVIEW, WbsTaskStatus.DONE);
		assertThat(columns).extracting(BoardColumn::getSortOrder).containsExactly(1, 2, 3, 4);
		assertThat(columns).allSatisfy(column -> {
			assertThat(column.getProjectId()).isEqualTo(7L);
			assertThat(column.getName()).isNull();
			assertThat(column.getWipLimit()).isNull();
		});
		assertThat(columns).extracting(BoardColumn::defaultLabelKey).containsExactly("code.TASK_STATUS.TODO",
			"code.TASK_STATUS.IN_PROGRESS", "code.TASK_STATUS.REVIEW", "code.TASK_STATUS.DONE");
	}

	@Test
	@DisplayName("이름을 붙이면 라벨 키가 없고, 이름을 비우면 다시 기본 이름 라벨 키를 쓴다")
	void labelKeyFollowsName() {
		final BoardColumn column = BoardColumn.of(7L, "코드 리뷰", WbsTaskStatus.REVIEW, 5, null);
		assertThat(column.defaultLabelKey()).isNull();

		column.changeSettings(null, null);
		assertThat(column.defaultLabelKey()).isEqualTo("code.TASK_STATUS.REVIEW");
	}

	@Test
	@DisplayName("WIP 초과: 한도와 같으면 초과 아님, 한도+1부터 초과, 한도가 없으면 카드가 많아도 초과 아님")
	void wipExceeded() {
		final BoardColumn limited = BoardColumn.of(7L, null, WbsTaskStatus.IN_PROGRESS, 2, 3);
		assertThat(limited.isWipExceeded(0)).isFalse();
		assertThat(limited.isWipExceeded(3)).isFalse();
		assertThat(limited.isWipExceeded(4)).isTrue();

		final BoardColumn unlimited = BoardColumn.of(7L, null, WbsTaskStatus.IN_PROGRESS, 2, null);
		assertThat(unlimited.isWipExceeded(2_000)).isFalse();

		final BoardColumn one = BoardColumn.of(7L, null, WbsTaskStatus.IN_PROGRESS, 2, 1);
		assertThat(one.isWipExceeded(1)).isFalse();
		assertThat(one.isWipExceeded(2)).isTrue();
	}

	@Test
	@DisplayName("같은 설정 판정: 이름·WIP 한도가 모두 같아야 같은 값(null끼리 같음)")
	void sameSettings() {
		final BoardColumn column = BoardColumn.of(7L, null, WbsTaskStatus.TODO, 1, null);
		assertThat(column.hasSameSettings(null, null)).isTrue();
		assertThat(column.hasSameSettings("대기", null)).isFalse();
		assertThat(column.hasSameSettings(null, 5)).isFalse();
	}
}

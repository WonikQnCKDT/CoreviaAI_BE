package org.wonik.coreviaai_be.domain.wbs.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsItemSource;

class MilestoneTest {

	@Test
	@DisplayName("이름·목표일이 모두 같을 때만 같은 값이다. 수정하면 두 값이 바뀐다")
	void changeDetails() {
		final LocalDate date = LocalDate.of(2026, 12, 20);
		final Milestone milestone = Milestone.of(1L, "설계 완료", date, WbsItemSource.MANUAL, 7L);

		assertThat(milestone.hasSameDetails("설계 완료", date)).isTrue();
		assertThat(milestone.hasSameDetails("설계 완료 ", date)).isFalse();
		assertThat(milestone.hasSameDetails("설계 완료", date.plusDays(1))).isFalse();

		milestone.changeDetails("오픈", date.plusDays(1));
		assertThat(milestone.getName()).isEqualTo("오픈");
		assertThat(milestone.getTargetDate()).isEqualTo(date.plusDays(1));
		assertThat(milestone.getSource()).isEqualTo(WbsItemSource.MANUAL);
	}
}

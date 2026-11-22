package org.wonik.coreviaai_be.domain.todo.policy;
import static org.assertj.core.api.Assertions.*;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.wonik.global.common.exception.CustomException;
class TodoDatePolicyTest {
	private static final LocalDate TODAY = LocalDate.of(2026,11,21);
	@Test void urgencyBoundaries() {
		assertThat(TodoDatePolicy.urgency(TODAY,TODAY,3)).isZero();
		assertThat(TodoDatePolicy.urgency(TODAY.plusDays(1),TODAY,3)).isEqualTo(1);
		assertThat(TodoDatePolicy.urgency(TODAY.plusDays(3),TODAY,3)).isEqualTo(1);
		assertThat(TodoDatePolicy.urgency(TODAY.plusDays(4),TODAY,3)).isEqualTo(3);
		assertThat(TodoDatePolicy.urgency(TODAY.minusDays(1),TODAY,3)).isEqualTo(2);
		assertThat(TodoDatePolicy.urgency(null,TODAY,3)).isEqualTo(4);
	}
	@Test void dateRangeAndNull() {
		assertThatCode(() -> TodoDatePolicy.validate(null)).doesNotThrowAnyException();
		assertThatCode(() -> TodoDatePolicy.validate(LocalDate.of(1,1,1))).doesNotThrowAnyException();
		assertThatCode(() -> TodoDatePolicy.validate(LocalDate.of(9999,12,31))).doesNotThrowAnyException();
		assertThatThrownBy(() -> TodoDatePolicy.validate(LocalDate.of(0,12,31))).isInstanceOf(CustomException.class);
		assertThatThrownBy(() -> TodoDatePolicy.validate(LocalDate.of(10000,1,1))).isInstanceOf(CustomException.class);
	}
}

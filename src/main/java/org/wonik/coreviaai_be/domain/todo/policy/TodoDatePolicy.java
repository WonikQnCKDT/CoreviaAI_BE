package org.wonik.coreviaai_be.domain.todo.policy;
import java.time.LocalDate;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
public final class TodoDatePolicy {
	private TodoDatePolicy() { }
	public static void validate(final LocalDate date) {
		if (date != null && (date.getYear() < 1 || date.getYear() > 9999)) {
			throw new CustomException(ErrorCode.INVALID_TODO_DATE);
		}
	}
	public static int urgency(final LocalDate dueDate, final LocalDate today, final int dueSoonDays) {
		if (dueDate == null) { return 4; }
		if (dueDate.equals(today)) { return 0; }
		if (dueDate.isAfter(today) && !dueDate.isAfter(today.plusDays(dueSoonDays))) { return 1; }
		return dueDate.isBefore(today) ? 2 : 3;
	}
}

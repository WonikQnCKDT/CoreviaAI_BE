package org.wonik.coreviaai_be.domain.todo.pagination;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import org.wonik.coreviaai_be.domain.todo.service.result.TodoItem;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

/** The ordering day is frozen for continuation across midnight; rows remain live. */
public record TodoCursor(LocalDate today, int urgency, LocalDate dueSort, int sourceOrder, long id) {
	public static final LocalDate NO_DUE_DATE = LocalDate.of(9999, 12, 31);
	public static String encode(final Long userId, final boolean done, final int days, final LocalDate today, final TodoItem item) {
		final String payload = String.join("|", "todo1", userId.toString(), Boolean.toString(done), Integer.toString(days),
			today.toString(), Integer.toString(item.urgency()),
			(item.dueDate() == null ? NO_DUE_DATE : item.dueDate()).toString(),
			Integer.toString("PERSONAL".equals(item.source()) ? 0 : 1), item.id().toString());
		return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
	}
	public static TodoCursor decode(final String value, final Long userId, final boolean done, final int days) {
		if (value == null || value.isBlank()) { return null; }
		try {
			if (value.length() > 256) { throw new IllegalArgumentException(); }
			final String[] p = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8).split("\\|", -1);
			if (p.length != 9 || !p[0].equals("todo1") || !p[1].equals(userId.toString())
				|| !p[2].equals(Boolean.toString(done)) || Integer.parseInt(p[3]) != days) { throw new IllegalArgumentException(); }
			final TodoCursor cursor = new TodoCursor(LocalDate.parse(p[4]), Integer.parseInt(p[5]), LocalDate.parse(p[6]),
				Integer.parseInt(p[7]), Long.parseLong(p[8]));
			if (cursor.urgency < 0 || cursor.urgency > 4 || cursor.sourceOrder < 0 || cursor.sourceOrder > 1 || cursor.id <= 0
				|| cursor.today.getYear() < 1 || cursor.today.getYear() > 9999 || cursor.dueSort.getYear() < 1
				|| cursor.dueSort.getYear() > 9999) { throw new IllegalArgumentException(); }
			return cursor;
		} catch (final RuntimeException invalid) {
			throw new CustomException(ErrorCode.INVALID_CURSOR);
		}
	}
}

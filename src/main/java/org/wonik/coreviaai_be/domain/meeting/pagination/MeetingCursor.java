package org.wonik.coreviaai_be.domain.meeting.pagination;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

/** 검색 조건과 정렬 키(회의 일시·ID)를 묶어 다른 프로젝트나 검색어에 커서를 재사용하지 못하게 한다. */
public record MeetingCursor(Instant heldAt, long id) {

	private static final String PREFIX = "meeting1";
	private static final int MAX_CURSOR_LENGTH = 1024;

	public static String encode(
		final Long projectId,
		final String normalizedKeyword,
		final Instant heldAt,
		final long id
	) {
		final String keyword = Base64.getUrlEncoder().withoutPadding()
			.encodeToString(normalizedKeyword.getBytes(StandardCharsets.UTF_8));
		final String payload = String.join("|", PREFIX, projectId.toString(), keyword,
			Long.toString(heldAt.getEpochSecond()), Integer.toString(heldAt.getNano()), Long.toString(id));
		return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
	}

	public static MeetingCursor decode(final String value, final Long projectId, final String normalizedKeyword) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			if (value.length() > MAX_CURSOR_LENGTH) {
				throw new IllegalArgumentException();
			}
			final String[] fields = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
				.split("\\|", -1);
			if (fields.length != 6 || !PREFIX.equals(fields[0]) || !projectId.toString().equals(fields[1])) {
				throw new IllegalArgumentException();
			}
			final String cursorKeyword = new String(Base64.getUrlDecoder().decode(fields[2]), StandardCharsets.UTF_8);
			final long epochSecond = Long.parseLong(fields[3]);
			final int nano = Integer.parseInt(fields[4]);
			final long id = Long.parseLong(fields[5]);
			if (!normalizedKeyword.equals(cursorKeyword) || nano < 0 || nano > 999_999_999 || id <= 0) {
				throw new IllegalArgumentException();
			}
			return new MeetingCursor(Instant.ofEpochSecond(epochSecond, nano), id);
		} catch (final RuntimeException invalid) {
			throw new CustomException(ErrorCode.INVALID_CURSOR);
		}
	}
}

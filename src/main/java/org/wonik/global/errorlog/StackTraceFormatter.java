package org.wonik.global.errorlog;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * 에러 로그용 스택 트레이스 문자열. printStackTrace 형식(예외 줄, "\tat" 프레임, "Caused by:")을 따르되
 * 원인마다 프레임 수를 제한해 깊은 프레임워크 스택 때문에 뒤쪽 원인(Caused by)이 잘려 나가지 않게 한다.
 * 각 예외 줄(클래스: 메시지)은 민감값을 가린 뒤 자른다. 프레임(클래스·메서드·파일·줄 번호)은 값이 없어 그대로 둔다.
 */
final class StackTraceFormatter {

	static final int MAX_CAUSES = 8;
	static final int MAX_FRAMES_PER_CAUSE = 30;
	static final int MAX_HEADER_LENGTH = ErrorLog.MESSAGE_MAX_LENGTH;

	private StackTraceFormatter() {
	}

	static String format(final Throwable error) {
		final StringBuilder trace = new StringBuilder();
		final Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
		Throwable current = error;
		int depth = 0;
		while (current != null && depth < MAX_CAUSES && seen.add(current)) {
			if (depth > 0) {
				trace.append("Caused by: ");
			}
			trace.append(ErrorLog.maskAndTruncate(current.toString(), MAX_HEADER_LENGTH)).append('\n');
			final StackTraceElement[] frames = current.getStackTrace();
			final int shown = Math.min(frames.length, MAX_FRAMES_PER_CAUSE);
			for (int i = 0; i < shown; i++) {
				trace.append("\tat ").append(frames[i]).append('\n');
			}
			if (frames.length > shown) {
				trace.append("\t... ").append(frames.length - shown).append(" more\n");
			}
			current = current.getCause();
			depth++;
		}
		if (current != null && depth == MAX_CAUSES) {
			trace.append("Caused by: ... (원인 ").append(MAX_CAUSES).append("단계 이후 생략)\n");
		}
		return trace.toString();
	}
}

package org.wonik.global.audit;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 감사 로그 details에 남길 변경 내용. 필드별 변경 전·후 값을 넣은 순서대로 기록한다.
 * <pre>{"systemRole": {"before": "USER", "after": "EXECUTIVE"}}</pre>
 * 비밀번호 같은 비밀값은 넣지 않는다.
 */
public final class AuditChanges {

	private final Map<String, Object> values = new LinkedHashMap<>();

	private AuditChanges() {
	}

	public static AuditChanges create() {
		return new AuditChanges();
	}

	/** 값이 바뀐 경우에만 기록한다. */
	public AuditChanges change(final String field, final Object before, final Object after) {
		if (!Objects.equals(before, after)) {
			final Map<String, Object> diff = new LinkedHashMap<>();
			diff.put("before", before);
			diff.put("after", after);
			values.put(field, diff);
		}
		return this;
	}

	/** 생성 시의 값처럼 전·후 비교가 아닌 값을 기록한다. */
	public AuditChanges value(final String field, final Object value) {
		values.put(field, value);
		return this;
	}

	public boolean isEmpty() {
		return values.isEmpty();
	}

	Map<String, Object> asMap() {
		return Collections.unmodifiableMap(values);
	}
}

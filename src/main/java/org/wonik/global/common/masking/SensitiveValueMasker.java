package org.wonik.global.common.masking;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 운영 진단 기록(에러 로그 메시지·스택 트레이스)에 섞여 들어온 민감값을 저장 직전에 가린다(NFR-06 패턴 마스킹).
 * 값만 {@link #MASK}로 바꾸고 키 이름·문장 구조는 남겨 원인 파악은 할 수 있게 한다. 원래 값은 복원할 수 없다.
 * <p>
 * 대상: 비밀 키 이름(password, secret, token, authorization, cookie, apiKey, sessionId 등)의 값({@code key=value},
 * {@code "key":"value"}, {@code key: value}), JWT, {@code Bearer}/{@code Basic} 자격 증명, PostgreSQL 상세 메시지의
 * {@code Key (컬럼)=(값)}·{@code Failing row contains (...)} 값, 이메일, 한국 주민등록번호·휴대폰·전화번호 형태.
 * 고객사명·공정 정보는 패턴으로 구분할 수 없어 대상이 아니다(사용자 결정).
 * <p>
 * 정규식은 백트래킹이 폭증하지 않는 형태만 쓴다(ReDoS 방지): 반복은 소유 수량자(*+, ++)나 길이 상한을 두고,
 * 연속 문자열 패턴은 앞쪽 경계(lookbehind)로 연속 구간의 첫 글자에서만 시작해 입력 길이에 선형으로 동작한다.
 * 과하게 가리는 쪽(예: "Invalid token: expired"의 expired)은 허용하고, 덜 가리는 쪽을 피한다.
 */
public final class SensitiveValueMasker {

	public static final String MASK = "****";

	private static final String SECRET_KEY_WORD =
		"(?:password|passwd|pwd|secret|token|authorization|cookie|api[_-]?key|credential|session[_-]?id)";

	/** PostgreSQL 상세: Key (login_id)=(admin) already exists. 값 뒤 문구는 남긴다. */
	private static final Pattern DB_KEY_DETAIL = Pattern.compile("(Key \\([^()\\n]{0,300}+\\)=\\()([^\\n]*+)");
	private static final List<String> DB_KEY_DETAIL_SUFFIXES = List.of(
		") already exists", ") is not present", ") is still referenced", ") conflicts with"
	);

	/** PostgreSQL 상세: Failing row contains (1, admin, ...). 행 전체 값이 들어 있어 괄호 안을 모두 가린다. */
	private static final Pattern DB_FAILING_ROW = Pattern.compile("(Failing row contains \\()[^\\n]*+");

	private static final Pattern AUTH_SCHEME = Pattern.compile(
		"(?i)(?<![A-Za-z0-9])((?:Bearer|Basic)\\s++)[A-Za-z0-9._~+/=-]++"
	);

	private static final Pattern JWT = Pattern.compile(
		"(?<![A-Za-z0-9_-])eyJ[A-Za-z0-9_-]++\\.[A-Za-z0-9_-]++\\.[A-Za-z0-9_-]*+"
	);

	/**
	 * 비밀 키 이름의 값. 키는 비밀 단어를 포함하면 된다(newPassword, jwt.secret, access_token).
	 * 소스 위치(JwtTokenProvider.java:42)는 키로 보지 않는다. 값은 큰따옴표·작은따옴표 문자열 또는 구분자 전까지.
	 */
	private static final Pattern SECRET_KEY_VALUE = Pattern.compile(
		"(?i)(" + SECRET_KEY_WORD + "[\\w.-]{0,40}+(?<!\\.java)(?<!\\.kt)[\"']?\\s{0,10}+[:=]\\s{0,10}+)"
			+ "(\"(?:[^\"\\\\\\n]|\\\\.)*+\"|'[^'\\n]*+'|[^\\s,;&)\\]}\"'<>]++)"
	);

	private static final Pattern EMAIL = Pattern.compile(
		"(?<![A-Za-z0-9._%+-])[A-Za-z0-9._%+-]++@[A-Za-z0-9-]++(?:\\.[A-Za-z0-9-]++)++"
	);

	/** 주민등록번호 형태: 생년월일 6자리 + 성별 1자리 + 6자리(하이픈 선택). */
	private static final Pattern RESIDENT_NUMBER = Pattern.compile(
		"(?<!\\d)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])-?[1-8]\\d{6}(?!\\d)"
	);

	/** 휴대폰(010 등)·지역번호(02, 031 등)·인터넷전화(070) 형태. 구분자는 하이픈·점·공백 또는 없음. */
	private static final Pattern PHONE_NUMBER = Pattern.compile(
		"(?<!\\d)(?:01[016789]|02|0[3-6][1-5]|070)[-. ]?\\d{3,4}[-. ]?\\d{4}(?!\\d)"
	);

	private SensitiveValueMasker() {
	}

	/** null은 null로 돌려준다. 가릴 값이 없으면 입력을 그대로 돌려준다. */
	public static String mask(final String text) {
		if (text == null || text.isEmpty()) {
			return text;
		}
		String masked = replace(text, DB_KEY_DETAIL, SensitiveValueMasker::maskDbKeyDetail);
		masked = replace(masked, DB_FAILING_ROW, matcher -> matcher.group(1) + MASK + ")");
		masked = replace(masked, AUTH_SCHEME, matcher -> matcher.group(1) + MASK);
		masked = replace(masked, JWT, matcher -> MASK);
		masked = replace(masked, SECRET_KEY_VALUE, SensitiveValueMasker::maskKeyValue);
		masked = replace(masked, EMAIL, matcher -> MASK);
		masked = replace(masked, RESIDENT_NUMBER, matcher -> MASK);
		return replace(masked, PHONE_NUMBER, matcher -> MASK);
	}

	private static String maskDbKeyDetail(final Matcher matcher) {
		final String rest = matcher.group(2);
		for (final String suffix : DB_KEY_DETAIL_SUFFIXES) {
			final int index = rest.lastIndexOf(suffix);
			if (index >= 0) {
				return matcher.group(1) + MASK + rest.substring(index);
			}
		}
		return matcher.group(1) + MASK + ")";
	}

	private static String maskKeyValue(final Matcher matcher) {
		final char first = matcher.group(2).charAt(0);
		if (first == '"' || first == '\'') {
			return matcher.group(1) + first + MASK + first;
		}
		return matcher.group(1) + MASK;
	}

	private static String replace(final String text, final Pattern pattern, final Function<Matcher, String> replacement) {
		final Matcher matcher = pattern.matcher(text);
		if (!matcher.find()) {
			return text;
		}
		final StringBuilder result = new StringBuilder(text.length());
		do {
			matcher.appendReplacement(result, Matcher.quoteReplacement(replacement.apply(matcher)));
		} while (matcher.find());
		matcher.appendTail(result);
		return result.toString();
	}
}

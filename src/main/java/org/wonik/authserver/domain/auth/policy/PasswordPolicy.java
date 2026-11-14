package org.wonik.authserver.domain.auth.policy;

import java.util.regex.Pattern;

/**
 * 비밀번호 규칙: 영문·숫자·특수문자를 각각 1자 이상 포함한 8~64자. 대문자는 필수가 아니다.
 * <p>
 * 공백과 한글 등 비ASCII 문자는 허용하지 않는다(키보드 배열·인코딩에 따라 같은 비밀번호를 다시 입력하기 어려움).
 * 최대 64자는 bcrypt 입력 한도(72바이트) 안에 들도록 정했다.
 */
public final class PasswordPolicy {

	public static final int MIN_LENGTH = 8;
	public static final int MAX_LENGTH = 64;

	private static final Pattern RULE = Pattern.compile(
		"^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[^A-Za-z0-9])[\\x21-\\x7E]{" + MIN_LENGTH + "," + MAX_LENGTH + "}$"
	);

	private PasswordPolicy() {
	}

	public static boolean isSatisfiedBy(final String rawPassword) {
		return rawPassword != null && RULE.matcher(rawPassword).matches();
	}
}

package org.wonik.authserver.domain.auth.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;
import org.wonik.authserver.domain.auth.policy.PasswordPolicy;

/**
 * 관리자 비밀번호 초기화용 임시 비밀번호를 만든다. 영문·숫자·특수문자를 반드시 포함해 PasswordPolicy를 만족한다.
 * 화면에서 옮겨 적을 때 헷갈리는 문자(0/O, 1/l/I)는 뺐다.
 */
@Component
public class TemporaryPasswordGenerator {

	private static final int LENGTH = 12;
	private static final String LETTERS = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
	private static final String DIGITS = "23456789";
	private static final String SPECIALS = "!@#$%^&*";
	private static final String ALL = LETTERS + DIGITS + SPECIALS;

	private final SecureRandom random = new SecureRandom();

	public String generate() {
		final List<Character> chars = new ArrayList<>(LENGTH);
		chars.add(pick(LETTERS));
		chars.add(pick(DIGITS));
		chars.add(pick(SPECIALS));
		while (chars.size() < LENGTH) {
			chars.add(pick(ALL));
		}
		Collections.shuffle(chars, random);

		final StringBuilder password = new StringBuilder(LENGTH);
		chars.forEach(password::append);
		final String result = password.toString();
		if (!PasswordPolicy.isSatisfiedBy(result)) {
			throw new IllegalStateException("임시 비밀번호가 비밀번호 규칙을 만족하지 않습니다");
		}
		return result;
	}

	private char pick(final String source) {
		return source.charAt(random.nextInt(source.length()));
	}
}

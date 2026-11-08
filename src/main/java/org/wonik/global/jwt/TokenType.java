package org.wonik.global.jwt;

/**
 * 토큰 용도. AccessToken을 refresh에, RefreshToken을 API 호출에 쓰지 못하도록 typ claim으로 구분한다.
 */
public enum TokenType {
	ACCESS("access"),
	REFRESH("refresh");

	private final String claimValue;

	TokenType(final String claimValue) {
		this.claimValue = claimValue;
	}

	public String claimValue() {
		return claimValue;
	}
}

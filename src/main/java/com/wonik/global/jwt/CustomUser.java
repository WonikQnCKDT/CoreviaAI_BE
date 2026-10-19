package com.wonik.global.jwt;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * Spring Security principal.
 * <p>
 * Spring {@link User}의 {@code username} 자리에는 Discord ID(snowflake)를 넣는다. 이 값은
 * 사람이 읽는 이름이 아니므로, 호출부에서는 {@link #getUsername()} 대신 {@link #getDiscordId()}를 쓴다.
 * (ADR-0008: 사용자 식별자·표시 이름 용어)
 */
public class CustomUser extends User {

	public CustomUser(
		final String discordId,
		final String password,
		final Collection<? extends GrantedAuthority> authorities
	) {
		super(discordId, password, authorities);
	}

	// authserver/erpweb 서비스 분리 준비: CustomUser(global)가 authserver의
	// User 인터페이스에 의존하지 않도록, 필요한 원시 값만 받는 팩토리를 쓴다.
	public static CustomUser of(
		final String discordId,
		final String password,
		final String roleName
	) {
		return new CustomUser(discordId, password, List.of(new SimpleGrantedAuthority(roleName)));
	}

	/** principal의 Discord ID. Spring {@link User#getUsername()}과 같은 값이다. */
	public String getDiscordId() {
		return getUsername();
	}
}

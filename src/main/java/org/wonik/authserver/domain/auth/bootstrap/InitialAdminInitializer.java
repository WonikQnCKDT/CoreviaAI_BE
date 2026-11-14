package org.wonik.authserver.domain.auth.bootstrap;

import java.time.Clock;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.authserver.domain.auth.policy.PasswordPolicy;
import org.wonik.authserver.domain.credential.entity.LocalCredential;
import org.wonik.authserver.domain.credential.repository.LocalCredentialRepository;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;
import org.wonik.global.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 시스템 관리자가 한 명도 없을 때만 환경변수 값으로 첫 관리자(사용자 + 로컬 로그인 정보)를 만든다.
 * <p>
 * 비밀번호를 저장소(SQL·코드)에 남기지 않기 위한 방식이며, 만들어진 계정은 첫 로그인 후 비밀번호를 바꿔야 한다.
 * 관리자가 이미 있으면 환경변수가 남아 있어도 아무것도 하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(InitialAdminProperties.class)
public class InitialAdminInitializer implements ApplicationRunner {

	private final InitialAdminProperties properties;
	private final UserRepository userRepository;
	private final LocalCredentialRepository credentialRepository;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;

	@Override
	@Transactional
	public void run(final ApplicationArguments args) {
		if (userRepository.existsBySystemRole(SystemRole.SYSTEM_ADMIN)) {
			return;
		}
		if (!properties.isConfigured()) {
			log.warn("시스템 관리자 계정이 없습니다. INITIAL_ADMIN_LOGIN_ID·PASSWORD·NAME을 설정하고 다시 기동하세요");
			return;
		}
		if (!PasswordPolicy.isSatisfiedBy(properties.password())) {
			throw new IllegalStateException("INITIAL_ADMIN_PASSWORD가 비밀번호 규칙(영문·숫자·특수문자 포함 8~64자)을 만족하지 않습니다");
		}
		if (userRepository.existsByLoginId(properties.loginId())) {
			throw new IllegalStateException("INITIAL_ADMIN_LOGIN_ID와 같은 아이디가 이미 있습니다");
		}

		final User admin = userRepository.save(User.create(properties.loginId(), properties.name(), SystemRole.SYSTEM_ADMIN));
		credentialRepository.save(LocalCredential.issue(admin.getId(), passwordEncoder.encode(properties.password()), clock.instant()));
		log.info("최초 시스템 관리자 계정을 만들었습니다 - loginId={}", properties.loginId());
	}
}

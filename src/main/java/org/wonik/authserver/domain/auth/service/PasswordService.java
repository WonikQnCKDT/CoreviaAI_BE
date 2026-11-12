package org.wonik.authserver.domain.auth.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.wonik.authserver.domain.auth.policy.PasswordPolicy;
import org.wonik.authserver.domain.auth.session.AuthSessionStore;
import org.wonik.authserver.domain.credential.entity.LocalCredential;
import org.wonik.authserver.domain.credential.repository.LocalCredentialRepository;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.audit.AuditLogger;
import org.wonik.global.audit.AuditTargetType;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 본인 비밀번호 변경과 관리자 비밀번호 초기화(FR-ADM-01). 바뀌는 것은 local_credentials뿐이다.
 * <p>
 * DB 변경을 커밋한 뒤 그 사용자의 모든 세션을 끊는다. 세션 저장소(Redis) 호출을 트랜잭션 밖에 두려고
 * 선언적 트랜잭션 대신 TransactionTemplate을 쓴다. 세션 삭제가 실패하면 예외가 올라가 클라이언트가 재시도할 수 있고,
 * 남은 세션도 최대 idle-timeout(30분) 안에 만료된다.
 */
@Service
@RequiredArgsConstructor
public class PasswordService {

	private final UserRepository userRepository;
	private final LocalCredentialRepository credentialRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthSessionStore sessionStore;
	private final TemporaryPasswordGenerator temporaryPasswordGenerator;
	private final AuditLogger auditLogger;
	private final TransactionTemplate transactionTemplate;
	private final Clock clock;

	public void changeMyPassword(final Long userId, final String currentPassword, final String newPassword) {
		transactionTemplate.executeWithoutResult(status -> {
			final LocalCredential credential = credentialRepository.findByUserIdForUpdate(userId)
				.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
			if (!passwordEncoder.matches(currentPassword, credential.getPasswordHash())) {
				throw new CustomException(ErrorCode.INVALID_PASSWORD);
			}
			if (!PasswordPolicy.isSatisfiedBy(newPassword)) {
				throw new CustomException(ErrorCode.INVALID_NEW_PASSWORD);
			}
			if (passwordEncoder.matches(newPassword, credential.getPasswordHash())) {
				throw new CustomException(ErrorCode.SAME_AS_CURRENT_PASSWORD);
			}
			credential.changePassword(passwordEncoder.encode(newPassword), clock.instant());
		});
		sessionStore.deleteAllByUserId(userId);
	}

	/**
	 * 임시 비밀번호로 초기화하고 잠금을 푼다. 로컬 로그인 정보가 아직 없는 사용자면 새로 발급한다.
	 * 임시 비밀번호는 호출자에게 한 번만 돌려주며 저장·로그하지 않는다. 초기화한 사실(행위자·대상·IP)은
	 * 초기화와 같은 트랜잭션으로 감사 로그에 남긴다(NFR-07).
	 */
	public String resetPassword(final AuditActor actor, final Long targetUserId) {
		final String temporaryPassword = temporaryPasswordGenerator.generate();
		transactionTemplate.executeWithoutResult(status -> {
			if (!userRepository.existsById(targetUserId)) {
				throw new CustomException(ErrorCode.USER_NOT_FOUND);
			}
			final String hash = passwordEncoder.encode(temporaryPassword);
			final Instant now = clock.instant();
			credentialRepository.findByUserIdForUpdate(targetUserId).ifPresentOrElse(
				credential -> credential.resetPassword(hash, now),
				() -> credentialRepository.save(LocalCredential.issue(targetUserId, hash, now))
			);
			auditLogger.record(actor, AuditAction.PASSWORD_RESET, AuditTargetType.USER, targetUserId, null);
		});
		sessionStore.deleteAllByUserId(targetUserId);
		return temporaryPassword;
	}
}

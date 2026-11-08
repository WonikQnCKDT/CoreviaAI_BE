package org.wonik.authserver.domain.credential.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.wonik.authserver.domain.credential.entity.LocalCredential;

import jakarta.persistence.LockModeType;

public interface LocalCredentialRepository extends JpaRepository<LocalCredential, Long> {

	/** 로그인 실패 횟수·비밀번호를 동시에 바꿔도 누락되지 않도록 행 잠금으로 읽는다. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from LocalCredential c where c.userId = :userId")
	Optional<LocalCredential> findByUserIdForUpdate(@Param("userId") Long userId);
}

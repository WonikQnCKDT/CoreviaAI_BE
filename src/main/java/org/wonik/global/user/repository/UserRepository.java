package org.wonik.global.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;

import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByLoginId(String loginId);

	/**
	 * 로그인 처리용. 같은 계정의 동시 로그인을 users 행 잠금으로 순서대로 처리한다
	 * (마지막 로그인 시각 갱신이 낙관적 잠금 충돌로 실패하지 않도록). 잠금 순서는 users → local_credentials로 고정한다.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from User u where u.loginId = :loginId")
	Optional<User> findByLoginIdForUpdate(@Param("loginId") String loginId);

	boolean existsBySystemRole(SystemRole systemRole);

	boolean existsByLoginId(String loginId);
}

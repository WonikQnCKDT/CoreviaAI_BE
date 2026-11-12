package org.wonik.global.user.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;
import org.wonik.global.user.entity.UserStatus;

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

	/**
	 * 관리자 사용자 목록(커서 방식, id 내림차순). null인 조건은 적용하지 않는다.
	 * keywordPattern은 소문자로 바꾸고 %, _, 역슬래시를 이스케이프한 LIKE 패턴(예: %hong%)이다.
	 * 다음 페이지 존재 여부를 알 수 있게 호출자가 size + 1개를 요청한다.
	 */
	@Query("""
		select u from User u
		where (:cursorId is null or u.id < :cursorId)
		  and (:departmentId is null or u.departmentId = :departmentId)
		  and (:status is null or u.status = :status)
		  and (:systemRole is null or u.systemRole = :systemRole)
		  and (:keywordPattern is null
		       or lower(u.loginId) like :keywordPattern escape '\\'
		       or lower(u.name) like :keywordPattern escape '\\')
		order by u.id desc
		""")
	List<User> searchForAdmin(
		@Param("cursorId") Long cursorId,
		@Param("departmentId") Long departmentId,
		@Param("status") UserStatus status,
		@Param("systemRole") SystemRole systemRole,
		@Param("keywordPattern") String keywordPattern,
		Pageable pageable
	);

	/**
	 * 활성 시스템 관리자 전원을 행 잠금으로 읽는다. 관리자 두 명이 서로를 동시에 강등·비활성화해
	 * 활성 관리자가 0명이 되는 경쟁을 막는다.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from User u where u.systemRole = :role and u.status = :status")
	List<User> findAllBySystemRoleAndStatusForUpdate(
		@Param("role") SystemRole role,
		@Param("status") UserStatus status
	);

	boolean existsByDepartmentIdAndStatus(Long departmentId, UserStatus status);

	long countByDepartmentIdAndStatus(Long departmentId, UserStatus status);

	/** 부서별 사용자 수. 결과 행은 [departmentId(Long), count(Long)]. departmentIds가 비어 있으면 호출하지 않는다. */
	@Query("""
		select u.departmentId, count(u) from User u
		where u.departmentId in :departmentIds and u.status = :status
		group by u.departmentId
		""")
	List<Object[]> countByDepartmentIds(
		@Param("departmentIds") Collection<Long> departmentIds,
		@Param("status") UserStatus status
	);
}

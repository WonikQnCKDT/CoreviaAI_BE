package org.wonik.coreviaai_be.domain.account.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.wonik.coreviaai_be.domain.account.entity.AccountRequest;
import org.wonik.coreviaai_be.domain.account.entity.AccountRequestStatus;

public interface AccountRequestRepository extends JpaRepository<AccountRequest, Long> {

	boolean existsByLoginIdAndStatus(String loginId, AccountRequestStatus status);

	Optional<AccountRequest> findFirstByLoginIdAndNameOrderByIdDesc(String loginId, String name);

	/**
	 * 관리자 신청 목록(커서, id 오름차순). 호출자가 size + 1개를 요청한다.
	 */
	@Query("""
		select r from AccountRequest r
		where (:cursorId is null or r.id > :cursorId)
		  and (:status is null or r.status = :status)
		order by r.id asc
		""")
	List<AccountRequest> findPageForAdmin(
		@Param("cursorId") Long cursorId,
		@Param("status") AccountRequestStatus status,
		Pageable pageable
	);
}

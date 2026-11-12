package org.wonik.global.audit;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 감사 로그는 추가·조회만 한다. 수정·삭제 메서드를 쓰지 않는다(FR-ADM-04 인수 기준).
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

	/** 대상별 변경 이력(커서 방식, id 내림차순). 호출자가 size + 1개를 요청한다. */
	@Query("""
		select a from AuditLog a
		where a.targetType = :targetType and a.targetId = :targetId
		  and (:cursorId is null or a.id < :cursorId)
		order by a.id desc
		""")
	List<AuditLog> findByTarget(
		@Param("targetType") AuditTargetType targetType,
		@Param("targetId") Long targetId,
		@Param("cursorId") Long cursorId,
		Pageable pageable
	);
}

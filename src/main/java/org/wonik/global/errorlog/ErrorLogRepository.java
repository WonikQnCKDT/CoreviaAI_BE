package org.wonik.global.errorlog;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 에러 로그는 추가·조회만 한다. 수정·삭제 메서드를 쓰지 않는다(FR-ADM-09, 권한 3.3 시스템 관리자 R).
 */
public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {

	/**
	 * 관리자 목록(커서 방식, id 내림차순 = 최신순). 호출자가 size + 1개를 요청한다.
	 * NULL 파라미터 타입 추론 문제를 피하려고 조건이 없을 때도 값을 넘긴다.
	 *
	 * @param from        발생 시각 하한(포함). 조건이 없으면 아주 이른 시각
	 * @param to          발생 시각 상한(제외). 조건이 없으면 아주 늦은 시각
	 * @param errorType   예외 클래스 이름(정확히 일치). 조건이 없으면 빈 문자열
	 * @param pathPattern 요청 경로 LIKE 패턴(앞부분 일치, 와일드카드는 '\'로 이스케이프). 조건이 없으면 '%'
	 */
	@Query("""
		select e from ErrorLog e
		where (:cursorId is null or e.id < :cursorId)
		  and e.occurredAt >= :from and e.occurredAt < :to
		  and (:errorType = '' or e.errorType = :errorType)
		  and (:pathPattern = '%' or e.apiPath like :pathPattern escape '\\')
		order by e.id desc
		""")
	List<ErrorLog> findPageForAdmin(
		@Param("cursorId") Long cursorId,
		@Param("from") Instant from,
		@Param("to") Instant to,
		@Param("errorType") String errorType,
		@Param("pathPattern") String pathPattern,
		Pageable pageable
	);
}

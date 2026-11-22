package org.wonik.coreviaai_be.domain.i18n.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.wonik.coreviaai_be.domain.i18n.entity.I18nLabel;

public interface I18nLabelRepository extends JpaRepository<I18nLabel, Long> {

	Optional<I18nLabel> findByLabelKey(String labelKey);

	boolean existsByLabelKey(String labelKey);

	/**
	 * 관리자 라벨 목록(커서 방식, id 오름차순). 호출자가 size + 1개를 요청한다.
	 *
	 * @param pattern       소문자 LIKE 패턴(와일드카드는 '\'로 이스케이프). 검색어가 없으면 '%'를 넘겨 전체와 일치시킨다
	 *                      (NULL 파라미터 타입 추론 문제를 피하려고 null을 쓰지 않는다)
	 * @param missingEn     true면 영어 번역이 없는 라벨만
	 * @param missingZhTw   true면 번체 중국어 번역이 없는 라벨만
	 */
	@Query("""
		select l from I18nLabel l
		where (:cursorId is null or l.id > :cursorId)
		  and (lower(l.labelKey) like :pattern escape '\\' or lower(l.textKo) like :pattern escape '\\')
		  and (:missingEn = false or l.textEn is null)
		  and (:missingZhTw = false or l.textZhTw is null)
		order by l.id asc
		""")
	List<I18nLabel> findPageForAdmin(
		@Param("cursorId") Long cursorId,
		@Param("pattern") String pattern,
		@Param("missingEn") boolean missingEn,
		@Param("missingZhTw") boolean missingZhTw,
		Pageable pageable
	);
}

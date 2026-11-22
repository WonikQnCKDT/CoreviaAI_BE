package org.wonik.coreviaai_be.domain.code.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wonik.coreviaai_be.domain.code.entity.CommonCode;

/**
 * 그룹별 조회는 uk_codes_group_id_code(group_id 선두) 인덱스를 쓴다. 그룹당 코드 수가 적어 페이지 없이 전부 읽는다.
 */
public interface CommonCodeRepository extends JpaRepository<CommonCode, Long> {

	List<CommonCode> findAllByGroupIdOrderBySortOrderAscIdAsc(Long groupId);

	List<CommonCode> findAllByGroupIdAndActiveTrueOrderBySortOrderAscIdAsc(Long groupId);

	Optional<CommonCode> findByGroupIdAndCode(Long groupId, String code);
}

package org.wonik.coreviaai_be.domain.code.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wonik.coreviaai_be.domain.code.entity.CodeGroup;

public interface CodeGroupRepository extends JpaRepository<CodeGroup, Long> {

	Optional<CodeGroup> findByGroupCode(String groupCode);

	/** 그룹은 마이그레이션으로만 늘어 수가 적으므로 페이지 없이 전부 읽는다. */
	List<CodeGroup> findAllByOrderByIdAsc();
}

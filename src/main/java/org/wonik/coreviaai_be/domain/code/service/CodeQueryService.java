package org.wonik.coreviaai_be.domain.code.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.code.entity.CodeGroup;
import org.wonik.coreviaai_be.domain.code.repository.CodeGroupRepository;
import org.wonik.coreviaai_be.domain.code.repository.CommonCodeRepository;
import org.wonik.coreviaai_be.domain.code.service.result.GroupCodes;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 화면용 공통 코드 조회(FR-ADM-07). 상태 선택 목록·표시에 쓰도록 사용 중(active) 코드만 정렬 순서대로 준다.
 * 캐시 없이 매번 DB를 읽어, 관리자의 정렬·사용 여부 변경이 재배포 없이 바로 반영된다.
 */
@Service
@RequiredArgsConstructor
public class CodeQueryService {

	private final CodeGroupRepository codeGroupRepository;
	private final CommonCodeRepository commonCodeRepository;

	@Transactional(readOnly = true)
	public GroupCodes findActiveCodes(final String groupCode) {
		final CodeGroup group = codeGroupRepository.findByGroupCode(groupCode)
			.orElseThrow(() -> new CustomException(ErrorCode.CODE_GROUP_NOT_FOUND));
		return new GroupCodes(group, commonCodeRepository.findAllByGroupIdAndActiveTrueOrderBySortOrderAscIdAsc(group.getId()));
	}
}

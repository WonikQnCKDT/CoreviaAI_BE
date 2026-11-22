package org.wonik.coreviaai_be.domain.code.service.result;

import java.util.List;

import org.wonik.coreviaai_be.domain.code.entity.CodeGroup;
import org.wonik.coreviaai_be.domain.code.entity.CommonCode;

/**
 * 한 그룹과 그 그룹의 코드 목록(정렬 순서 오름차순).
 */
public record GroupCodes(CodeGroup group, List<CommonCode> codes) {
}

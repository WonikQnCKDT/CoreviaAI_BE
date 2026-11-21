package org.wonik.coreviaai_be.domain.code.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.coreviaai_be.domain.code.dto.response.GroupCodesResponse;
import org.wonik.coreviaai_be.domain.code.entity.CommonCode;
import org.wonik.coreviaai_be.domain.code.service.CodeQueryService;
import org.wonik.global.common.response.ApiResponse;

import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;

/**
 * 화면용 공통 코드 조회 API(FR-ADM-07). 로그인한 사용자 누구나 호출한다(CoreviaSecurityConfig의 그 외 /api/** 규칙).
 * 관리용 API는 AdminCodeController(/api/admin/code-groups)에 있다.
 */
@RestController
@RequestMapping("/api/codes")
@RequiredArgsConstructor
public class CodeController {

	private final CodeQueryService codeQueryService;

	@GetMapping("/{groupCode}")
	public ApiResponse<GroupCodesResponse> findActiveCodes(
		@PathVariable @Pattern(regexp = CommonCode.GROUP_CODE_PATTERN) final String groupCode
	) {
		return ApiResponse.success(GroupCodesResponse.from(codeQueryService.findActiveCodes(groupCode)));
	}
}

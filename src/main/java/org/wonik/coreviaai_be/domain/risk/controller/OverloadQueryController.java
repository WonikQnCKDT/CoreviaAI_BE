package org.wonik.coreviaai_be.domain.risk.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.coreviaai_be.domain.risk.dto.response.OverloadResponse;
import org.wonik.coreviaai_be.domain.risk.service.OverloadQueryService;
import org.wonik.global.common.response.ApiResponse;
import org.wonik.global.jwt.LoginUser;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

/**
 * 프로젝트 인력 편중 조회(FR-AIR-03). 계산은 일일 배치가 하고, 이 API는 최근 성공 결과만 권한에 맞게 돌려준다.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/risk-overloads")
@RequiredArgsConstructor
public class OverloadQueryController {

	private final OverloadQueryService overloadQueryService;

	@GetMapping
	public ApiResponse<OverloadResponse> find(
		@AuthenticationPrincipal final LoginUser viewer,
		@PathVariable @Positive final Long projectId
	) {
		return ApiResponse.success(OverloadResponse.from(overloadQueryService.find(viewer.userId(), projectId)));
	}
}

package org.wonik.coreviaai_be.domain.user.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.coreviaai_be.domain.user.dto.response.UserSearchResponse;
import org.wonik.coreviaai_be.domain.user.policy.UserSearchKeyword;
import org.wonik.coreviaai_be.domain.user.service.UserDirectoryService;
import org.wonik.global.common.response.ApiResponse;
import org.wonik.global.jwt.LoginUser;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * 일반 사용자용 사용자 이름 검색 API. 프로젝트 생성 ⑤·멤버 초대에서 초대할 사람을 찾는 데 쓴다.
 * 접근 대상은 프로젝트 API와 같다(CoreviaSecurityConfig, 시스템 관리자 차단).
 * <p>
 * 직원 목록 대량 수집을 어렵게 하려고 검색어(2자 이상)를 필수로 받고 결과를 최대 20건으로 제한한다. 상한이 있어 페이지네이션은 없다.
 * 검색어는 로그에 남기지 않는다.
 */
@RestController
@RequiredArgsConstructor
public class UserSearchController {

	public static final String PATH = "/api/users/search";

	private final UserDirectoryService userDirectoryService;

	@GetMapping(PATH)
	public ApiResponse<List<UserSearchResponse>> search(
		@AuthenticationPrincipal final LoginUser loginUser,
		@RequestParam final String keyword,
		@RequestParam(defaultValue = "" + UserDirectoryService.DEFAULT_SIZE)
		@Min(1) @Max(UserDirectoryService.MAX_SIZE) final int size
	) {
		return ApiResponse.success(
			userDirectoryService.searchInvitable(loginUser.userId(), UserSearchKeyword.of(keyword), size).stream()
				.map(UserSearchResponse::from)
				.toList()
		);
	}
}

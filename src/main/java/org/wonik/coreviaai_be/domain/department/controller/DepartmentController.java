package org.wonik.coreviaai_be.domain.department.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.coreviaai_be.domain.department.dto.response.DepartmentResponse;
import org.wonik.coreviaai_be.domain.department.service.DepartmentQueryService;
import org.wonik.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

/**
 * 일반 사용자용 부서 목록 API(프로젝트 주관 부서 선택). 접근 대상은 프로젝트 API와 같다(CoreviaSecurityConfig, 시스템 관리자 차단).
 * 시스템 관리자의 부서 관리는 AdminDepartmentController(/api/admin/departments)에 있다.
 * 결과에 상한(DepartmentQueryService.MAX_DEPARTMENTS)이 있어 페이지네이션은 없다.
 */
@RestController
@RequiredArgsConstructor
public class DepartmentController {

	public static final String PATH = "/api/departments";

	private final DepartmentQueryService departmentQueryService;

	@GetMapping(PATH)
	public ApiResponse<List<DepartmentResponse>> findActive() {
		return ApiResponse.success(departmentQueryService.findActive().stream().map(DepartmentResponse::from).toList());
	}
}

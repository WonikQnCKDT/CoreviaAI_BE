package org.wonik.coreviaai_be.domain.admin.controller;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.coreviaai_be.domain.admin.dto.request.ReassignPmRequest;
import org.wonik.coreviaai_be.domain.project.dto.response.ProjectPmResponse;
import org.wonik.coreviaai_be.domain.project.service.ProjectPmService;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.common.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 시스템 관리자의 PM 재지정(FR-PRJ-04, 요구사항 명세서 3.1 예외). 관리자는 프로젝트 데이터를 볼 수 없으므로
 * 조회 API 없이 코드·새 PM ID만 받고, 응답도 바뀐 PM만 돌려준다. 접근 제어는 CoreviaSecurityConfig의 /api/admin/** 규칙이 맡는다.
 */
@RestController
@RequestMapping("/api/admin/projects")
@RequiredArgsConstructor
public class AdminProjectController {

	private final ProjectPmService pmService;

	@PutMapping("/pm")
	public ApiResponse<ProjectPmResponse> reassignPm(
		final AuditActor actor,
		@Valid @RequestBody final ReassignPmRequest request
	) {
		return ApiResponse.success(ProjectPmResponse.from(pmService.reassign(actor, request.toCommand())));
	}
}

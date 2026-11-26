package org.wonik.coreviaai_be.domain.admin.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.coreviaai_be.domain.project.dto.request.UpdateProjectTemplateRequest;
import org.wonik.coreviaai_be.domain.project.dto.response.ProjectTemplateResponse;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectTemplate;
import org.wonik.coreviaai_be.domain.project.service.ProjectTemplateService;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.common.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 시스템 관리자의 템플릿 기본값 관리(FR-PRJ-09). 접근은 /api/admin/** 규칙이 맡는다.
 * 수정은 이후 생성 프로젝트의 기본값만 바꾸며, 이미 만든 프로젝트는 생성 당시 값을 유지한다.
 */
@RestController
@RequestMapping("/api/admin/project-templates")
@RequiredArgsConstructor
public class AdminProjectTemplateController {

	private final ProjectTemplateService templateService;

	@GetMapping
	public ApiResponse<List<ProjectTemplateResponse>> findAll() {
		return ApiResponse.success(templateService.findAll());
	}

	@PutMapping("/{templateKey}")
	public ApiResponse<ProjectTemplateResponse> update(
		final AuditActor actor,
		@PathVariable final ProjectTemplate templateKey,
		@Valid @RequestBody final UpdateProjectTemplateRequest request
	) {
		return ApiResponse.success(templateService.update(actor, request.toCommand(templateKey)));
	}
}

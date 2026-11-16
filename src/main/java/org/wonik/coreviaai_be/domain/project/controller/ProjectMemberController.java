package org.wonik.coreviaai_be.domain.project.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.coreviaai_be.domain.project.dto.request.ChangeMemberRoleRequest;
import org.wonik.coreviaai_be.domain.project.dto.request.DelegatePmRequest;
import org.wonik.coreviaai_be.domain.project.dto.request.InviteMembersRequest;
import org.wonik.coreviaai_be.domain.project.dto.response.MemberHistoryResponse;
import org.wonik.coreviaai_be.domain.project.dto.response.ProjectMemberResponse;
import org.wonik.coreviaai_be.domain.project.dto.response.ProjectPmResponse;
import org.wonik.coreviaai_be.domain.project.service.ProjectMemberService;
import org.wonik.coreviaai_be.domain.project.service.ProjectPmService;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.common.pagination.CursorPage;
import org.wonik.global.common.pagination.CursorPageRequest;
import org.wonik.global.common.response.ApiResponse;
import org.wonik.global.jwt.LoginUser;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

/**
 * 프로젝트 멤버·역할 관리와 PM 위임 API(FR-PRJ-04). 조회는 상세와 같은 공개 범위(볼 수 없으면 404),
 * 변경은 현재 PM만 할 수 있다(403 PROJECT_PM_ONLY, 경영진 포함). 판정은 서비스가 한다.
 * 관리자 PM 재지정은 /api/admin/projects/pm(AdminProjectController)이 맡는다.
 */
@RestController
@RequestMapping("/api/projects/{projectId}")
@RequiredArgsConstructor
public class ProjectMemberController {

	private final ProjectMemberService memberService;
	private final ProjectPmService pmService;

	/** 멤버 목록(커서, id 오름차순 = 등록순). */
	@GetMapping("/members")
	public ApiResponse<CursorPage<ProjectMemberResponse>> findMembers(
		@AuthenticationPrincipal final LoginUser loginUser,
		@PathVariable @Positive final Long projectId,
		@RequestParam(required = false) final String cursor,
		@RequestParam(defaultValue = "" + CursorPageRequest.DEFAULT_SIZE)
		@Min(1) @Max(CursorPageRequest.MAX_SIZE) final int size
	) {
		final CursorPageRequest page = CursorPageRequest.of(cursor, size);
		return ApiResponse.success(
			memberService.findMembers(loginUser.userId(), projectId, page).map(ProjectMemberResponse::from)
		);
	}

	/** 멤버 이력(커서, id 내림차순 = 최신순). */
	@GetMapping("/members/histories")
	public ApiResponse<CursorPage<MemberHistoryResponse>> findHistories(
		@AuthenticationPrincipal final LoginUser loginUser,
		@PathVariable @Positive final Long projectId,
		@RequestParam(required = false) final String cursor,
		@RequestParam(defaultValue = "" + CursorPageRequest.DEFAULT_SIZE)
		@Min(1) @Max(CursorPageRequest.MAX_SIZE) final int size
	) {
		final CursorPageRequest page = CursorPageRequest.of(cursor, size);
		return ApiResponse.success(
			memberService.findHistories(loginUser.userId(), projectId, page).map(MemberHistoryResponse::from)
		);
	}

	/** 여러 명을 한 번에 초대한다. 응답은 초대한 멤버(userId 순). 재전송하면 409 ALREADY_PROJECT_MEMBER로 끝난다. */
	@PostMapping("/members")
	public ApiResponse<List<ProjectMemberResponse>> invite(
		final AuditActor actor,
		@PathVariable @Positive final Long projectId,
		@Valid @RequestBody final InviteMembersRequest request
	) {
		return ApiResponse.success(
			memberService.invite(actor, projectId, request.toCommand()).stream().map(ProjectMemberResponse::from).toList()
		);
	}

	@PatchMapping("/members/{userId}/role")
	public ApiResponse<ProjectMemberResponse> changeRole(
		final AuditActor actor,
		@PathVariable @Positive final Long projectId,
		@PathVariable @Positive final Long userId,
		@Valid @RequestBody final ChangeMemberRoleRequest request
	) {
		return ApiResponse.success(
			ProjectMemberResponse.from(memberService.changeRole(actor, projectId, userId, request.actor()))
		);
	}

	@DeleteMapping("/members/{userId}")
	public ApiResponse<Void> remove(
		final AuditActor actor,
		@PathVariable @Positive final Long projectId,
		@PathVariable @Positive final Long userId
	) {
		memberService.remove(actor, projectId, userId);
		return ApiResponse.success(null);
	}

	@PostMapping("/pm-delegation")
	public ApiResponse<ProjectPmResponse> delegatePm(
		final AuditActor actor,
		@PathVariable @Positive final Long projectId,
		@Valid @RequestBody final DelegatePmRequest request
	) {
		return ApiResponse.success(ProjectPmResponse.from(pmService.delegate(actor, projectId, request.toCommand())));
	}
}

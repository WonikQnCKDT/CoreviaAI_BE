package org.wonik.coreviaai_be.domain.wbs.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.coreviaai_be.domain.wbs.dto.request.CreateBoardColumnRequest;
import org.wonik.coreviaai_be.domain.wbs.dto.request.MoveBoardCardRequest;
import org.wonik.coreviaai_be.domain.wbs.dto.request.ReorderBoardColumnsRequest;
import org.wonik.coreviaai_be.domain.wbs.dto.request.UpdateBoardColumnRequest;
import org.wonik.coreviaai_be.domain.wbs.dto.response.BoardCardMoveResponse;
import org.wonik.coreviaai_be.domain.wbs.dto.response.BoardColumnsResponse;
import org.wonik.coreviaai_be.domain.wbs.dto.response.BoardResponse;
import org.wonik.coreviaai_be.domain.wbs.service.BoardColumnService;
import org.wonik.coreviaai_be.domain.wbs.service.BoardService;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.common.response.ApiResponse;
import org.wonik.global.jwt.LoginUser;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

/**
 * 칸반 보드 API(FR-TSK-02, 05. 업무·보드). 보드 조회, 카드 이동(드래그 상태 변경), 컬럼 관리(PM).
 * 권한 판정은 서비스가 한다. /api/projects/** 경로라 시스템 관리자는 보안 설정에서 이미 차단된다.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/board")
@RequiredArgsConstructor
public class BoardController {

	private final BoardService boardService;
	private final BoardColumnService boardColumnService;

	/** assigneeUserId를 주면 그 담당자의 카드만 돌려준다(담당자 필터). */
	@GetMapping
	public ApiResponse<BoardResponse> find(
		@AuthenticationPrincipal final LoginUser loginUser,
		@PathVariable @Positive final Long projectId,
		@RequestParam(required = false) @Positive final Long assigneeUserId
	) {
		return ApiResponse.success(BoardResponse.from(boardService.find(loginUser.userId(), projectId, assigneeUserId)));
	}

	/** 같은 컬럼으로 다시 보내면 아무것도 바꾸지 않고 성공한다(멱등). */
	@PutMapping("/cards/{taskId}/column")
	public ApiResponse<BoardCardMoveResponse> moveCard(
		final AuditActor actor,
		@PathVariable @Positive final Long projectId,
		@PathVariable @Positive final Long taskId,
		@Valid @RequestBody final MoveBoardCardRequest request
	) {
		return ApiResponse.success(
			BoardCardMoveResponse.from(boardService.moveCard(actor, projectId, taskId, request.toCommand())));
	}

	@PostMapping("/columns")
	public ApiResponse<BoardColumnsResponse> createColumn(
		final AuditActor actor,
		@PathVariable @Positive final Long projectId,
		@Valid @RequestBody final CreateBoardColumnRequest request
	) {
		return ApiResponse.success(
			BoardColumnsResponse.from(boardColumnService.create(actor, projectId, request.toCommand())));
	}

	@PatchMapping("/columns/{columnId}")
	public ApiResponse<BoardColumnsResponse> updateColumn(
		final AuditActor actor,
		@PathVariable @Positive final Long projectId,
		@PathVariable @Positive final Long columnId,
		@Valid @RequestBody final UpdateBoardColumnRequest request
	) {
		return ApiResponse.success(
			BoardColumnsResponse.from(boardColumnService.update(actor, projectId, columnId, request.toCommand())));
	}

	@PutMapping("/columns/order")
	public ApiResponse<BoardColumnsResponse> reorderColumns(
		final AuditActor actor,
		@PathVariable @Positive final Long projectId,
		@Valid @RequestBody final ReorderBoardColumnsRequest request
	) {
		return ApiResponse.success(
			BoardColumnsResponse.from(boardColumnService.reorder(actor, projectId, request.toCommand())));
	}

	@DeleteMapping("/columns/{columnId}")
	public ApiResponse<BoardColumnsResponse> deleteColumn(
		final AuditActor actor,
		@PathVariable @Positive final Long projectId,
		@PathVariable @Positive final Long columnId
	) {
		return ApiResponse.success(BoardColumnsResponse.from(boardColumnService.delete(actor, projectId, columnId)));
	}
}

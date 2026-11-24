package org.wonik.coreviaai_be.domain.worklog.controller;

import java.time.LocalDate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.coreviaai_be.domain.worklog.dto.request.WorklogTaskRequest;
import org.wonik.coreviaai_be.domain.worklog.service.WorklogTaskService;
import org.wonik.coreviaai_be.domain.worklog.service.result.WorklogTaskPage;
import org.wonik.coreviaai_be.domain.worklog.service.result.WorklogTaskResult;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.common.response.ApiResponse;
import org.wonik.global.jwt.LoginUser;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class WorklogTaskController {
    private final WorklogTaskService service;
    @PutMapping("/api/me/worklogs/{date}/tasks/{taskId}")
    public ApiResponse<WorklogTaskResult> upsert(AuditActor actor, @PathVariable LocalDate date,
        @PathVariable @Positive Long taskId, @Valid @RequestBody WorklogTaskRequest request) {
        return ApiResponse.success(service.upsert(actor, date, taskId, request.toCommand()));
    }
    @GetMapping("/api/me/worklogs/{date}/tasks")
    public ApiResponse<WorklogTaskPage> own(@AuthenticationPrincipal LoginUser viewer, @PathVariable LocalDate date,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.findOwned(viewer.userId(), date, page, size));
    }
    @GetMapping("/api/projects/{projectId}/worklogs")
    public ApiResponse<WorklogTaskPage> project(@AuthenticationPrincipal LoginUser viewer,
        @PathVariable @Positive Long projectId, @RequestParam LocalDate from, @RequestParam LocalDate to,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.findProject(viewer.userId(), projectId, from, to, page, size));
    }
}

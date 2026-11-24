package org.wonik.coreviaai_be.domain.schedule.controller;
import java.time.Instant;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.global.jwt.LoginUser;
import org.wonik.global.common.response.ApiResponse;
import org.wonik.coreviaai_be.domain.schedule.service.CalendarService;
import org.wonik.coreviaai_be.domain.schedule.service.result.CalendarPage;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/me/calendar")
@RequiredArgsConstructor
public class CalendarController {
    private final CalendarService service;
    @GetMapping
    public ApiResponse<CalendarPage> find(@AuthenticationPrincipal LoginUser viewer,
        @RequestParam Instant from, @RequestParam Instant to,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.find(viewer.userId(), from, to, page, size));
    }
}

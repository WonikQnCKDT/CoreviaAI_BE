package org.wonik.coreviaai_be.domain.i18n.controller;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.coreviaai_be.domain.i18n.dto.request.LanguagePreferenceRequest;
import org.wonik.coreviaai_be.domain.i18n.dto.response.LabelBundleResponse;
import org.wonik.coreviaai_be.domain.i18n.entity.type.LabelLanguage;
import org.wonik.coreviaai_be.domain.i18n.service.LabelBundleService;
import org.wonik.coreviaai_be.domain.i18n.service.LanguagePreferenceService;
import org.wonik.coreviaai_be.domain.i18n.service.result.LanguagePreference;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.common.response.ApiResponse;
import org.wonik.global.jwt.LoginUser;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/me/language")
@RequiredArgsConstructor
public class LanguagePreferenceController {
    private final LanguagePreferenceService service;
    private final LabelBundleService labels;

    @GetMapping("/labels")
    public ResponseEntity<ApiResponse<LabelBundleResponse>> labels(
        @AuthenticationPrincipal LoginUser viewer) {
        final var language = LabelLanguage
            .fromTag(service.get(viewer.userId()).effectiveLanguage()).orElseThrow();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(ApiResponse.success(LabelBundleResponse.from(labels.findBundle(language))));
    }
    @GetMapping public ApiResponse<LanguagePreference> get(@AuthenticationPrincipal LoginUser viewer) {
        return ApiResponse.success(service.get(viewer.userId()));
    }
    @PutMapping public ApiResponse<LanguagePreference> update(AuditActor actor,
        @Valid @RequestBody LanguagePreferenceRequest request) {
        return ApiResponse.success(service.update(actor, request.language(), request.version()));
    }
}

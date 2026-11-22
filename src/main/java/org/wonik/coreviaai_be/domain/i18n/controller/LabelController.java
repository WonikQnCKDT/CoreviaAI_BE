package org.wonik.coreviaai_be.domain.i18n.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.coreviaai_be.domain.i18n.dto.response.LabelBundleResponse;
import org.wonik.coreviaai_be.domain.i18n.entity.type.LabelLanguage;
import org.wonik.coreviaai_be.domain.i18n.service.LabelBundleService;
import org.wonik.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

/**
 * 화면용 다국어 라벨 API(FR-ADM-06). 로그인 화면·비밀번호 변경 화면도 문구가 필요해 로그인 없이 호출할 수 있다
 * (CoreviaSecurityConfig에서 이 GET 하나만 공개). 응답은 화면 문구뿐이며 사용자·업무 데이터가 없다.
 * 관리용 API는 AdminLabelController(/api/admin/i18n/labels)에 있다.
 */
@RestController
@RequestMapping(LabelController.PATH)
@RequiredArgsConstructor
public class LabelController {

	public static final String PATH = "/api/i18n/labels";

	private final LabelBundleService labelBundleService;

	/** 표시 언어: lang 파라미터 → Accept-Language → ko. 지원하지 않는 값은 오류 없이 다음 순서로 넘어간다. */
	@GetMapping
	public ResponseEntity<ApiResponse<LabelBundleResponse>> findBundle(
		@RequestParam(required = false) final String lang,
		@RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) final String acceptLanguage
	) {
		final LabelLanguage language = LabelLanguage.resolve(lang, acceptLanguage);
		// 응답이 Accept-Language에 따라 달라지므로 중간 캐시가 다른 언어 응답을 돌려주지 않게 Vary를 붙인다.
		return ResponseEntity.ok()
			.header(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE)
			.body(ApiResponse.success(LabelBundleResponse.from(labelBundleService.findBundle(language))));
	}
}

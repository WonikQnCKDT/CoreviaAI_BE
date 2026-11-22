package org.wonik.coreviaai_be.domain.i18n.dto.response;

import java.util.Map;

import org.wonik.coreviaai_be.domain.i18n.service.result.LabelBundle;

/**
 * 화면용 라벨 묶음. lang은 실제로 적용한 언어 태그(ko, en, zh-TW)라 FE는 요청 값이 아닌 이 값으로 표시 언어를 맞춘다.
 */
public record LabelBundleResponse(String lang, Map<String, String> labels) {

	public static LabelBundleResponse from(final LabelBundle bundle) {
		return new LabelBundleResponse(bundle.language().tag(), bundle.labels());
	}
}

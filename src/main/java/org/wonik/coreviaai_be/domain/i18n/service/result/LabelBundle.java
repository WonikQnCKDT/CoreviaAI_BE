package org.wonik.coreviaai_be.domain.i18n.service.result;

import java.util.Map;

import org.wonik.coreviaai_be.domain.i18n.entity.type.LabelLanguage;

/**
 * 한 언어의 화면 문구 묶음. labels는 라벨 키 → 표시 문구(번역 누락은 한국어)이고 키 오름차순이다.
 */
public record LabelBundle(LabelLanguage language, Map<String, String> labels) {
}

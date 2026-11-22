package org.wonik.coreviaai_be.domain.i18n.service;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.i18n.entity.I18nLabel;
import org.wonik.coreviaai_be.domain.i18n.entity.type.LabelLanguage;
import org.wonik.coreviaai_be.domain.i18n.repository.I18nLabelRepository;
import org.wonik.coreviaai_be.domain.i18n.service.result.LabelBundle;

import lombok.RequiredArgsConstructor;

/**
 * 화면용 라벨 묶음(FR-ADM-06). 서버 캐시를 두지 않고 매 요청 DB에서 읽는다:
 * 관리자가 문구를 바꾸면 다음 조회부터 바로 반영되어(재배포·캐시 무효화 불필요) 인수 기준 "재배포 없이 전 화면 문구 변경"을 지킨다.
 * 라벨 수는 화면 문구 수(수백~수천 행) 수준이라 전체 조회로 충분하다. FE는 화면 진입·언어 전환 때 한 번 받아 메모리에 둔다.
 */
@Service
@RequiredArgsConstructor
public class LabelBundleService {

	private final I18nLabelRepository i18nLabelRepository;

	@Transactional(readOnly = true)
	public LabelBundle findBundle(final LabelLanguage language) {
		final Map<String, String> labels = new TreeMap<>();
		for (final I18nLabel label : i18nLabelRepository.findAll()) {
			labels.put(label.getLabelKey(), label.displayText(language));
		}
		return new LabelBundle(language, Collections.unmodifiableMap(labels));
	}
}

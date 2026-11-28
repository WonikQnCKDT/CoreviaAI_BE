package org.wonik.coreviaai_be.domain.admin.service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.admin.service.result.AdminHistoryResult;
import org.wonik.coreviaai_be.domain.i18n.entity.I18nLabel;
import org.wonik.coreviaai_be.domain.i18n.entity.type.LabelLanguage;
import org.wonik.coreviaai_be.domain.i18n.repository.I18nLabelRepository;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.audit.AuditChanges;
import org.wonik.global.audit.AuditLogger;
import org.wonik.global.audit.AuditTargetType;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.pagination.CursorPage;
import org.wonik.global.common.pagination.CursorPageRequest;

import lombok.RequiredArgsConstructor;

/**
 * 시스템 관리자의 다국어 라벨 관리(FR-ADM-06).
 * <p>
 * 라벨 키는 화면 코드가 참조하는 식별자라 등록 후 바꾸거나 지우지 않는다(명세에 삭제 기능 없음). 관리자는 라벨을 등록하고 문구만 고친다.
 * 문구 변경 이력은 감사 로그로 변경과 같은 트랜잭션에 남긴다(인수 기준 "문구 변경 이력 보관").
 * 수정 감사 로그는 바뀐 언어마다 한 행씩 남긴다: 세 언어의 전·후 문구(각 500자)를 한 행에 담으면 details 최대 길이(4000자)를 넘을 수 있어서다.
 */
@Service
@RequiredArgsConstructor
public class AdminLabelService {

	private static final String LIKE_ESCAPE = "\\";

	private final I18nLabelRepository i18nLabelRepository;
	private final AuditLogger auditLogger;
	private final AdminAuditHistoryReader auditHistoryReader;

	/**
	 * @param keyword  라벨 키·한국어 문구 부분 일치(대소문자 무시). 없거나 비면 전체
	 * @param missing  번역 누락 필터 언어(en, zh-TW). 없으면 필터 없음. 한국어는 필수라 누락이 없으므로 받지 않는다
	 */
	@Transactional(readOnly = true)
	public CursorPage<I18nLabel> findPage(final CursorPageRequest page, final String keyword, final String missing) {
		final LabelLanguage missingLanguage = parseMissingLanguage(missing);
		final List<I18nLabel> fetched = i18nLabelRepository.findPageForAdmin(
			page.cursorId(),
			likePattern(keyword),
			missingLanguage == LabelLanguage.EN,
			missingLanguage == LabelLanguage.ZH_TW,
			page.fetchPageable()
		);
		return CursorPage.of(fetched, page.size(), I18nLabel::getId);
	}

	/** 라벨 변경 이력(최신순, 커서 페이지). */
	@Transactional(readOnly = true)
	public CursorPage<AdminHistoryResult> findHistories(final Long labelId, final CursorPageRequest page) {
		if (!i18nLabelRepository.existsById(labelId)) {
			throw new CustomException(ErrorCode.LABEL_NOT_FOUND);
		}
		return auditHistoryReader.findByTarget(AuditTargetType.I18N_LABEL, labelId, page);
	}

	/** 같은 라벨 키가 있으면 409(재전송도 하나만 생김). */
	@Transactional
	public I18nLabel create(
		final AuditActor actor,
		final String labelKey,
		final String textKo,
		final String textEn,
		final String textZhTw
	) {
		if (i18nLabelRepository.existsByLabelKey(labelKey)) {
			throw new CustomException(ErrorCode.DUPLICATE_LABEL_KEY);
		}
		final I18nLabel saved = saveAndFlush(I18nLabel.create(labelKey, textKo, textEn, textZhTw));
		recordCreated(actor, saved);
		return saved;
	}

	/**
	 * 세 언어 문구를 보낸 값으로 덮어쓴다. 현재와 같은 값이면 version을 보지 않고 아무것도 바꾸지 않아,
	 * 성공한 요청의 재전송이 409나 이력을 만들지 않는다. 값이 다르면 version이 현재와 같아야 한다(다르면 409).
	 */
	@Transactional
	public I18nLabel update(
		final AuditActor actor,
		final Long labelId,
		final String textKo,
		final String textEn,
		final String textZhTw,
		final long version
	) {
		final I18nLabel label = i18nLabelRepository.findById(labelId)
			.orElseThrow(() -> new CustomException(ErrorCode.LABEL_NOT_FOUND));
		final String beforeKo = label.getTextKo();
		final String beforeEn = label.getTextEn();
		final String beforeZhTw = label.getTextZhTw();
		if (label.hasSameTexts(textKo, textEn, textZhTw)) {
			return label;
		}
		if (label.getVersion() != version) {
			throw new CustomException(ErrorCode.CONCURRENT_MODIFICATION);
		}

		label.changeTexts(textKo, textEn, textZhTw);
		// 검사와 커밋 사이에 다른 수정이 끼면 @Version 갱신이 실패해 409(CoreviaExceptionHandler)가 된다.
		i18nLabelRepository.flush();
		recordChanged(actor, label, LabelLanguage.KO, beforeKo);
		recordChanged(actor, label, LabelLanguage.EN, beforeEn);
		recordChanged(actor, label, LabelLanguage.ZH_TW, beforeZhTw);
		return label;
	}

	/** 사전 검사를 통과한 뒤 같은 키가 동시에 저장된 경우, DB 유니크 제약 위반을 중복 오류로 바꾼다. */
	private I18nLabel saveAndFlush(final I18nLabel label) {
		try {
			return i18nLabelRepository.saveAndFlush(label);
		} catch (final DataIntegrityViolationException e) {
			throw new CustomException(ErrorCode.DUPLICATE_LABEL_KEY, e);
		}
	}

	private void recordCreated(final AuditActor actor, final I18nLabel label) {
		auditLogger.record(actor, AuditAction.I18N_LABEL_CREATED, AuditTargetType.I18N_LABEL, label.getId(),
			AuditChanges.create()
				.value("labelKey", label.getLabelKey())
				.value("textKo", label.getTextKo())
				.value("textEn", label.getTextEn())
				.value("textZhTw", label.getTextZhTw()));
	}

	/** 해당 언어 문구가 바뀐 경우에만 한 행을 남긴다. details 예: {"labelKey":"..","lang":"en","text":{"before":..,"after":..}} */
	private void recordChanged(final AuditActor actor, final I18nLabel label, final LabelLanguage language, final String before) {
		final String after = label.textOf(language);
		if (Objects.equals(before, after)) {
			return;
		}
		auditLogger.record(actor, AuditAction.I18N_LABEL_CHANGED, AuditTargetType.I18N_LABEL, label.getId(),
			AuditChanges.create()
				.value("labelKey", label.getLabelKey())
				.value("lang", language.tag())
				.change("text", before, after));
	}

	private static LabelLanguage parseMissingLanguage(final String missing) {
		if (missing == null || missing.isBlank()) {
			return null;
		}
		final LabelLanguage language = LabelLanguage.fromTag(missing)
			.orElseThrow(() -> new CustomException(ErrorCode.INVALID_MISSING_LANGUAGE));
		if (language == LabelLanguage.KO) {
			throw new CustomException(ErrorCode.INVALID_MISSING_LANGUAGE);
		}
		return language;
	}

	/** 소문자 부분 일치 패턴. LIKE 와일드카드(%, _)와 이스케이프 문자는 글자 그대로 찾도록 이스케이프한다(라벨 키에 _가 많음). */
	static String likePattern(final String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return "%";
		}
		final String escaped = keyword.strip().toLowerCase(Locale.ROOT)
			.replace(LIKE_ESCAPE, LIKE_ESCAPE + LIKE_ESCAPE)
			.replace("%", LIKE_ESCAPE + "%")
			.replace("_", LIKE_ESCAPE + "_");
		return "%" + escaped + "%";
	}
}

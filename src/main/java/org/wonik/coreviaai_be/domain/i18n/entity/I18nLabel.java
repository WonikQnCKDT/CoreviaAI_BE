package org.wonik.coreviaai_be.domain.i18n.entity;

import java.util.Objects;

import org.wonik.coreviaai_be.domain.i18n.entity.type.LabelLanguage;
import org.wonik.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 다국어 라벨(FR-ADM-06, i18n_labels). 화면 코드는 라벨 키만 쓰고 표시 문구는 이 행에서 언어별로 가져온다.
 * <p>
 * 라벨 키는 화면 코드·공통 코드가 참조하는 식별자라 바꾸지 않는다(updatable = false, 삭제 기능 없음).
 * 한국어는 필수이고 영어·번체 중국어는 없을 수 있다(NULL = 번역 누락). 빈 문자열은 NULL로 바꿔 저장해
 * 누락 판정과 누락 필터가 NULL 하나만 보게 한다. 누락 번역은 한국어로 대신 표시한다(인수 기준).
 */
@Getter
@Entity
@Table(name = "i18n_labels")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class I18nLabel extends BaseTimeEntity {

	/**
	 * 라벨 키 형식: 영문자로 시작하고 점(.)으로 구분한 영문·숫자·밑줄·하이픈 조각, 100자 이하(label_key 컬럼).
	 * 공통 코드 라벨 키(code.TASK_STATUS.IN_PROGRESS)와 화면 라벨 키(menu.project.list)를 모두 허용한다.
	 */
	public static final String LABEL_KEY_PATTERN = "^(?=.{1,100}$)[A-Za-z][A-Za-z0-9_-]*(\\.[A-Za-z0-9_-]+)*$";
	public static final int MAX_TEXT_LENGTH = 500;
	/** 줄바꿈·탭 등 제어 문자 금지: 라벨은 한 줄 문구이고, 감사 로그 details(JSON) 길이가 이스케이프로 늘어나지 않게 한다. */
	public static final String TEXT_PATTERN = "^[^\\p{Cntrl}]*$";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "label_key", nullable = false, updatable = false, length = 100)
	private String labelKey;

	@Column(name = "text_ko", nullable = false, length = MAX_TEXT_LENGTH)
	private String textKo;

	@Column(name = "text_en", length = MAX_TEXT_LENGTH)
	private String textEn;

	@Column(name = "text_zh_tw", length = MAX_TEXT_LENGTH)
	private String textZhTw;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	private I18nLabel(final String labelKey, final String textKo, final String textEn, final String textZhTw) {
		this.labelKey = labelKey;
		this.textKo = requireKorean(textKo);
		this.textEn = normalizeOptional(textEn);
		this.textZhTw = normalizeOptional(textZhTw);
	}

	/** 문구는 앞뒤 공백을 지우고, 영어·번체 중국어가 비어 있으면 누락(NULL)으로 저장한다. */
	public static I18nLabel create(final String labelKey, final String textKo, final String textEn, final String textZhTw) {
		return new I18nLabel(labelKey, textKo, textEn, textZhTw);
	}

	/**
	 * 세 언어 문구를 보낸 값으로 덮어쓴다(영어·번체 중국어를 비우면 누락으로 돌아간다).
	 *
	 * @return 하나라도 바뀌었으면 true
	 */
	public boolean changeTexts(final String textKo, final String textEn, final String textZhTw) {
		if (hasSameTexts(textKo, textEn, textZhTw)) {
			return false;
		}
		this.textKo = requireKorean(textKo);
		this.textEn = normalizeOptional(textEn);
		this.textZhTw = normalizeOptional(textZhTw);
		return true;
	}

	/** 정규화(앞뒤 공백 제거, 빈 값 = 누락)한 뒤 현재 문구와 같은지. 공백만 다른 값은 같은 문구로 본다. */
	public boolean hasSameTexts(final String textKo, final String textEn, final String textZhTw) {
		return Objects.equals(this.textKo, normalizeOptional(textKo))
			&& Objects.equals(this.textEn, normalizeOptional(textEn))
			&& Objects.equals(this.textZhTw, normalizeOptional(textZhTw));
	}

	/** 해당 언어 문구. 없으면 null(누락). */
	public String textOf(final LabelLanguage language) {
		return switch (language) {
			case KO -> textKo;
			case EN -> textEn;
			case ZH_TW -> textZhTw;
		};
	}

	/** 화면 표시 문구: 해당 언어가 누락이면 한국어. */
	public String displayText(final LabelLanguage language) {
		final String text = textOf(language);
		return text != null ? text : textKo;
	}

	/** 요청 경계에서 공백·길이를 검증하지만, 도메인 불변식(한국어 필수)은 여기서도 지킨다. */
	private static String requireKorean(final String textKo) {
		final String normalized = normalizeOptional(textKo);
		if (normalized == null) {
			throw new IllegalArgumentException("한국어 문구는 비어 있을 수 없습니다");
		}
		return normalized;
	}

	/** 앞뒤 공백 제거, 빈 값은 null. 의미 없는 공백만 다른 값으로 이력이 생기지 않게 한다. */
	public static String normalizeOptional(final String text) {
		if (text == null) {
			return null;
		}
		final String stripped = text.strip();
		return stripped.isEmpty() ? null : stripped;
	}
}

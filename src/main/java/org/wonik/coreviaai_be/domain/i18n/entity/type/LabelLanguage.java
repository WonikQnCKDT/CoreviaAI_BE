package org.wonik.coreviaai_be.domain.i18n.entity.type;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * 다국어 라벨 표시 언어(FR-ADM-06, NFR-19): 한국어(기본)·영어·번체 중국어.
 * <p>
 * 표시 언어는 요청의 {@code lang} 파라미터 → {@code Accept-Language} 헤더 → 기본 한국어 순서로 정한다(사용자별 언어 컬럼은 두지 않음).
 * 지원하지 않는 값은 오류 없이 다음 순서로 넘어간다. 화면 문구 조회가 언어 값 하나 때문에 실패하지 않게 하기 위해서다.
 * 중국어는 번체만 지원하므로 번체를 뜻하는 지역·문자(TW·HK·MO, Hant)가 있을 때만 zh-TW로 본다. 간체(zh, zh-CN)는 지원하지 않는 언어다.
 */
public enum LabelLanguage {
	KO("ko"),
	EN("en"),
	ZH_TW("zh-TW");

	/** 헤더 파싱 상한. 정상 브라우저 값은 수십 자이므로 이보다 긴 값은 무시하고 기본 언어로 둔다. */
	static final int MAX_ACCEPT_LANGUAGE_LENGTH = 200;
	/** BCP 47 태그 길이 상한(RFC 5646 권장 35자). 넘으면 지원하지 않는 값으로 본다. */
	static final int MAX_TAG_LENGTH = 35;

	private static final Set<String> TRADITIONAL_CHINESE_SUBTAGS = Set.of("tw", "hk", "mo", "hant");

	private final String tag;

	LabelLanguage(final String tag) {
		this.tag = tag;
	}

	public String tag() {
		return tag;
	}

	/**
	 * 태그 하나를 지원 언어로 바꾼다. 대소문자·구분자(-, _)는 가리지 않는다. 예: ko-KR → KO, en-US → EN, zh-Hant-TW → ZH_TW.
	 *
	 * @return 지원하지 않거나 형식이 맞지 않으면 빈 값
	 */
	public static Optional<LabelLanguage> fromTag(final String rawTag) {
		if (rawTag == null || rawTag.isBlank() || rawTag.length() > MAX_TAG_LENGTH) {
			return Optional.empty();
		}
		final String[] subtags = rawTag.strip().toLowerCase(Locale.ROOT).replace('_', '-').split("-", -1);
		return switch (subtags[0]) {
			case "ko" -> Optional.of(KO);
			case "en" -> Optional.of(EN);
			case "zh" -> isTraditionalChinese(subtags) ? Optional.of(ZH_TW) : Optional.empty();
			default -> Optional.empty();
		};
	}

	/** lang 파라미터 → Accept-Language → 한국어. 어느 단계도 오류를 내지 않는다. */
	public static LabelLanguage resolve(final String langParam, final String acceptLanguage) {
		if (langParam != null && !langParam.isBlank()) {
			final Optional<LabelLanguage> fromParam = fromTag(langParam);
			if (fromParam.isPresent()) {
				return fromParam.get();
			}
		}
		return fromAcceptLanguage(acceptLanguage).orElse(KO);
	}

	/** 가중치(q) 높은 순서로 처음 지원하는 언어. q=0(거부)과 와일드카드(*)는 고르지 않는다. */
	static Optional<LabelLanguage> fromAcceptLanguage(final String header) {
		if (header == null || header.isBlank() || header.length() > MAX_ACCEPT_LANGUAGE_LENGTH) {
			return Optional.empty();
		}
		final List<Locale.LanguageRange> ranges;
		try {
			ranges = Locale.LanguageRange.parse(header);
		} catch (final IllegalArgumentException e) {
			// 형식이 틀린 헤더는 클라이언트 입력일 뿐이므로 오류로 만들지 않고 기본 언어를 쓴다.
			return Optional.empty();
		}
		return ranges.stream()
			.filter(range -> range.getWeight() > 0)
			.map(range -> fromTag(range.getRange()))
			.flatMap(Optional::stream)
			.findFirst();
	}

	private static boolean isTraditionalChinese(final String[] subtags) {
		for (int i = 1; i < subtags.length; i++) {
			if (TRADITIONAL_CHINESE_SUBTAGS.contains(subtags[i])) {
				return true;
			}
		}
		return false;
	}
}

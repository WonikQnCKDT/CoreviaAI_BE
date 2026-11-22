package org.wonik.coreviaai_be.domain.i18n.entity.type;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class LabelLanguageTest {

	@ParameterizedTest
	@CsvSource({
		"ko, KO", "ko-KR, KO", "KO, KO",
		"en, EN", "en-US, EN", "en_GB, EN",
		"zh-TW, ZH_TW", "zh-tw, ZH_TW", "zh_TW, ZH_TW", "zh-Hant, ZH_TW", "zh-Hant-TW, ZH_TW", "zh-HK, ZH_TW", "zh-MO, ZH_TW"
	})
	@DisplayName("지원 태그는 대소문자·구분자와 지역을 가리지 않고 언어로 바뀐다")
	void fromTagSupported(final String tag, final LabelLanguage expected) {
		assertThat(LabelLanguage.fromTag(tag)).contains(expected);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {" ", "zh", "zh-CN", "zh-Hans", "ja", "fr-FR", "*", "-", "english"})
	@DisplayName("간체 중국어·미지원 언어·빈 값은 지원하지 않는 값이다")
	void fromTagUnsupported(final String tag) {
		assertThat(LabelLanguage.fromTag(tag)).isEmpty();
	}

	@Test
	@DisplayName("태그 길이 경계: 35자는 해석하고 36자는 지원하지 않는 값으로 본다")
	void fromTagLengthBoundary() {
		final String tag35 = "en-" + "a".repeat(32);
		assertThat(tag35).hasSize(35);
		assertThat(LabelLanguage.fromTag(tag35)).contains(LabelLanguage.EN);
		assertThat(LabelLanguage.fromTag(tag35 + "a")).isEmpty();
	}

	@Test
	@DisplayName("표시 언어는 lang 파라미터가 Accept-Language보다 우선한다")
	void paramFirst() {
		assertThat(LabelLanguage.resolve("en", "zh-TW,zh;q=0.9")).isEqualTo(LabelLanguage.EN);
		assertThat(LabelLanguage.resolve("zh-TW", "en")).isEqualTo(LabelLanguage.ZH_TW);
	}

	@Test
	@DisplayName("lang 파라미터가 없거나 지원하지 않으면 Accept-Language, 그것도 없으면 한국어")
	void fallbackOrder() {
		assertThat(LabelLanguage.resolve(null, "en-US,en;q=0.9")).isEqualTo(LabelLanguage.EN);
		assertThat(LabelLanguage.resolve("", "en-US")).isEqualTo(LabelLanguage.EN);
		assertThat(LabelLanguage.resolve("ja", "zh-TW")).isEqualTo(LabelLanguage.ZH_TW);
		assertThat(LabelLanguage.resolve("ja", null)).isEqualTo(LabelLanguage.KO);
		assertThat(LabelLanguage.resolve(null, null)).isEqualTo(LabelLanguage.KO);
		assertThat(LabelLanguage.resolve(null, "")).isEqualTo(LabelLanguage.KO);
	}

	@Test
	@DisplayName("Accept-Language는 가중치 높은 순으로 처음 지원하는 언어를 고르고, q=0과 미지원 언어는 건너뛴다")
	void acceptLanguageWeights() {
		assertThat(LabelLanguage.resolve(null, "ja;q=1.0, en;q=0.5, zh-TW;q=0.8")).isEqualTo(LabelLanguage.ZH_TW);
		assertThat(LabelLanguage.resolve(null, "zh-CN, zh;q=0.9, en;q=0.8")).isEqualTo(LabelLanguage.EN);
		assertThat(LabelLanguage.resolve(null, "en;q=0, ko;q=0.1")).isEqualTo(LabelLanguage.KO);
		assertThat(LabelLanguage.resolve(null, "en;q=0")).isEqualTo(LabelLanguage.KO);
		assertThat(LabelLanguage.resolve(null, "*")).isEqualTo(LabelLanguage.KO);
		assertThat(LabelLanguage.resolve(null, "ja, fr")).isEqualTo(LabelLanguage.KO);
	}

	@Test
	@DisplayName("형식이 틀리거나 지나치게 긴 Accept-Language는 오류 없이 한국어로 처리한다")
	void malformedAcceptLanguage() {
		assertThat(LabelLanguage.resolve(null, "en;q=abc")).isEqualTo(LabelLanguage.KO);
		assertThat(LabelLanguage.resolve(null, ";;;,,,==")).isEqualTo(LabelLanguage.KO);
		assertThat(LabelLanguage.resolve(null, "en;q=2")).isEqualTo(LabelLanguage.KO);
		final String longHeader = "en," + "x".repeat(LabelLanguage.MAX_ACCEPT_LANGUAGE_LENGTH);
		assertThat(LabelLanguage.resolve(null, longHeader)).isEqualTo(LabelLanguage.KO);
	}

	@Test
	@DisplayName("응답에 쓰는 태그는 ko, en, zh-TW")
	void tags() {
		assertThat(LabelLanguage.KO.tag()).isEqualTo("ko");
		assertThat(LabelLanguage.EN.tag()).isEqualTo("en");
		assertThat(LabelLanguage.ZH_TW.tag()).isEqualTo("zh-TW");
	}
}

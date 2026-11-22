package org.wonik.coreviaai_be.domain.i18n.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.wonik.coreviaai_be.domain.i18n.entity.type.LabelLanguage;

class I18nLabelTest {

	@Test
	@DisplayName("문구의 앞뒤 공백을 지우고, 빈 영어·번체 중국어는 누락(null)으로 저장한다")
	void createNormalizes() {
		final I18nLabel label = I18nLabel.create("menu.home", "  홈 ", "   ", "");
		assertThat(label.getTextKo()).isEqualTo("홈");
		assertThat(label.getTextEn()).isNull();
		assertThat(label.getTextZhTw()).isNull();
	}

	@Test
	@DisplayName("번역 누락 언어는 한국어로 표시하고, 번역이 있으면 그 언어로 표시한다")
	void displayFallsBackToKorean() {
		final I18nLabel label = I18nLabel.create("menu.home", "홈", "Home", null);
		assertThat(label.displayText(LabelLanguage.KO)).isEqualTo("홈");
		assertThat(label.displayText(LabelLanguage.EN)).isEqualTo("Home");
		assertThat(label.displayText(LabelLanguage.ZH_TW)).isEqualTo("홈");
		assertThat(label.textOf(LabelLanguage.ZH_TW)).isNull();
	}

	@Test
	@DisplayName("문구 변경: 같은 값(공백만 다른 값 포함)이면 false, 바뀌면 true이고 영어를 비우면 누락으로 돌아간다")
	void changeTexts() {
		final I18nLabel label = I18nLabel.create("menu.home", "홈", "Home", "首頁");
		assertThat(label.changeTexts(" 홈", "Home ", "首頁")).isFalse();
		assertThat(label.changeTexts("홈", "", "首頁")).isTrue();
		assertThat(label.getTextEn()).isNull();
	}

	@Test
	@DisplayName("한국어 문구는 비울 수 없다(도메인 불변식)")
	void koreanRequired() {
		assertThatThrownBy(() -> I18nLabel.create("menu.home", "  ", null, null))
			.isInstanceOf(IllegalArgumentException.class);
		final I18nLabel label = I18nLabel.create("menu.home", "홈", null, null);
		assertThatThrownBy(() -> label.changeTexts(null, null, null)).isInstanceOf(IllegalArgumentException.class);
		assertThat(label.getTextKo()).isEqualTo("홈");
	}

	@ParameterizedTest
	@ValueSource(strings = {"a", "code.TASK_STATUS.IN_PROGRESS", "codeGroup.RISK_LEVEL", "menu.project-list.title", "btn.save_2"})
	@DisplayName("라벨 키 형식: 영문자로 시작하고 점으로 구분한 영문·숫자·밑줄·하이픈")
	void validLabelKeys(final String key) {
		assertThat(key).matches(I18nLabel.LABEL_KEY_PATTERN);
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "1abc", ".menu", "menu.", "menu..home", "menu home", "메뉴.홈", "menu/home", "a\nb"})
	@DisplayName("라벨 키 형식 위반")
	void invalidLabelKeys(final String key) {
		assertThat(key).doesNotMatch(I18nLabel.LABEL_KEY_PATTERN);
	}

	@Test
	@DisplayName("라벨 키 길이 경계: 100자는 허용, 101자는 거부")
	void labelKeyLengthBoundary() {
		assertThat("a".repeat(100)).matches(I18nLabel.LABEL_KEY_PATTERN);
		assertThat("a".repeat(101)).doesNotMatch(I18nLabel.LABEL_KEY_PATTERN);
	}

	@Test
	@DisplayName("문구에는 제어 문자(줄바꿈·탭)를 넣을 수 없고 다국어 문자는 허용한다")
	void textPattern() {
		assertThat("저장 Save 儲存 \"인용\"").matches(I18nLabel.TEXT_PATTERN);
		assertThat("줄\n바꿈").doesNotMatch(I18nLabel.TEXT_PATTERN);
		assertThat("탭\t").doesNotMatch(I18nLabel.TEXT_PATTERN);
	}
}

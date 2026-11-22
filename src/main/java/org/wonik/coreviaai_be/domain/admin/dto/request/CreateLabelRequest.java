package org.wonik.coreviaai_be.domain.admin.dto.request;

import org.wonik.coreviaai_be.domain.i18n.entity.I18nLabel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 다국어 라벨 등록. 한국어는 필수, 영어·번체 중국어는 비우면 번역 누락으로 저장한다(화면은 한국어로 표시).
 */
public record CreateLabelRequest(
	@NotNull(message = "라벨 키를 입력해 주세요")
	@Pattern(regexp = I18nLabel.LABEL_KEY_PATTERN, message = "라벨 키는 영문자로 시작하고 점(.)으로 구분한 영문·숫자·밑줄·하이픈 100자 이하로 입력해 주세요")
	String labelKey,

	@NotBlank(message = "한국어 문구를 입력해 주세요")
	@Size(max = I18nLabel.MAX_TEXT_LENGTH, message = "문구는 500자 이하로 입력해 주세요")
	@Pattern(regexp = I18nLabel.TEXT_PATTERN, message = "문구에는 줄바꿈·탭 같은 제어 문자를 넣을 수 없습니다")
	String textKo,

	@Size(max = I18nLabel.MAX_TEXT_LENGTH, message = "문구는 500자 이하로 입력해 주세요")
	@Pattern(regexp = I18nLabel.TEXT_PATTERN, message = "문구에는 줄바꿈·탭 같은 제어 문자를 넣을 수 없습니다")
	String textEn,

	@Size(max = I18nLabel.MAX_TEXT_LENGTH, message = "문구는 500자 이하로 입력해 주세요")
	@Pattern(regexp = I18nLabel.TEXT_PATTERN, message = "문구에는 줄바꿈·탭 같은 제어 문자를 넣을 수 없습니다")
	String textZhTw
) {
}

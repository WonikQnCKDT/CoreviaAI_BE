package org.wonik.coreviaai_be.domain.menu.dto.request;

import org.wonik.coreviaai_be.domain.i18n.entity.I18nLabel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 메뉴 문구 수정. 메뉴 키는 바꾸지 않고 연결된 라벨의 세 언어를 보낸 값으로 덮어쓴다.
 */
public record UpdateMenuTextRequest(
	@NotBlank(message = "한국어 문구를 입력해 주세요")
	@Size(max = I18nLabel.MAX_TEXT_LENGTH, message = "문구는 500자 이하로 입력해 주세요")
	@Pattern(regexp = I18nLabel.TEXT_PATTERN, message = "문구에는 줄바꿈·탭 같은 제어 문자를 넣을 수 없습니다")
	String textKo,

	@Size(max = I18nLabel.MAX_TEXT_LENGTH, message = "문구는 500자 이하로 입력해 주세요")
	@Pattern(regexp = I18nLabel.TEXT_PATTERN, message = "문구에는 줄바꿈·탭 같은 제어 문자를 넣을 수 없습니다")
	String textEn,

	@Size(max = I18nLabel.MAX_TEXT_LENGTH, message = "문구는 500자 이하로 입력해 주세요")
	@Pattern(regexp = I18nLabel.TEXT_PATTERN, message = "문구에는 줄바꿈·탭 같은 제어 문자를 넣을 수 없습니다")
	String textZhTw,

	@NotNull(message = "수정할 메뉴의 version을 입력해 주세요")
	@PositiveOrZero(message = "version은 0 이상이어야 합니다")
	Long version
) {
}

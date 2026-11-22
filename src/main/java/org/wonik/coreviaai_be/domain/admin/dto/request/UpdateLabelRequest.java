package org.wonik.coreviaai_be.domain.admin.dto.request;

import org.wonik.coreviaai_be.domain.i18n.entity.I18nLabel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 다국어 문구 수정. 세 언어를 모두 보낸 값으로 덮어쓴다(영어·번체 중국어를 비우면 번역 누락으로 돌아감).
 * version은 목록·등록 응답에서 받은 값으로, 그 사이 다른 관리자가 고쳤으면 409로 거부한다(낙관적 잠금).
 * 라벨 키는 바꿀 수 없어 받지 않는다.
 */
public record UpdateLabelRequest(
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

	@NotNull(message = "수정할 라벨의 version을 입력해 주세요")
	@PositiveOrZero(message = "version은 0 이상이어야 합니다")
	Long version
) {
}

package org.wonik.coreviaai_be.domain.admin.dto.response;

import java.time.Instant;

import org.wonik.coreviaai_be.domain.i18n.entity.I18nLabel;

/**
 * 관리자 다국어 라벨 항목. 번역 누락 언어는 null이다(대체 표시 없이 원래 값을 보여 줘야 누락을 알아볼 수 있다).
 * version은 수정 요청에 그대로 돌려보낸다.
 */
public record AdminLabelResponse(
	Long id,
	String labelKey,
	String textKo,
	String textEn,
	String textZhTw,
	long version,
	Instant createdAt,
	Instant updatedAt
) {

	public static AdminLabelResponse from(final I18nLabel label) {
		return new AdminLabelResponse(
			label.getId(),
			label.getLabelKey(),
			label.getTextKo(),
			label.getTextEn(),
			label.getTextZhTw(),
			label.getVersion(),
			label.getCreatedAt(),
			label.getUpdatedAt()
		);
	}
}

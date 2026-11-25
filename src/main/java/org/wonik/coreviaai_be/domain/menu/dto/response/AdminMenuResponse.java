package org.wonik.coreviaai_be.domain.menu.dto.response;

import org.wonik.coreviaai_be.domain.menu.service.result.AdminMenuView;

/**
 * 관리자 메뉴 항목. 영어·번체 중국어가 없으면 null이고, version은 문구 수정 때 다시 보낸다.
 */
public record AdminMenuResponse(
	Long id,
	String menuKey,
	String labelKey,
	String urlPath,
	int sortOrder,
	Long parentId,
	String requiredRole,
	boolean active,
	String textKo,
	String textEn,
	String textZhTw,
	long version
) {

	public static AdminMenuResponse from(final AdminMenuView view) {
		final var menu = view.menu();
		final var label = view.label();
		return new AdminMenuResponse(
			menu.getId(),
			menu.getMenuKey(),
			menu.getLabelKey(),
			menu.getUrlPath(),
			menu.getSortOrder(),
			menu.getParentId(),
			menu.getRequiredRole() == null ? null : menu.getRequiredRole().name(),
			menu.isActive(),
			label.getTextKo(),
			label.getTextEn(),
			label.getTextZhTw(),
			label.getVersion());
	}
}

package org.wonik.coreviaai_be.domain.menu.dto.response;

import org.wonik.coreviaai_be.domain.menu.service.result.VisibleMenu;

/** 화면 메뉴 한 줄. text는 이미 표시 언어로 고른 문구다. */
public record MenuItemResponse(
	String menuKey,
	String labelKey,
	String text,
	String urlPath,
	int sortOrder
) {

	public static MenuItemResponse from(final VisibleMenu menu) {
		return new MenuItemResponse(menu.menuKey(), menu.labelKey(), menu.text(), menu.urlPath(), menu.sortOrder());
	}
}

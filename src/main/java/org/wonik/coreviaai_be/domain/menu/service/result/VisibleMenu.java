package org.wonik.coreviaai_be.domain.menu.service.result;

/**
 * 로그인 사용자에게 보이는 메뉴 한 줄. text는 표시 언어이고, 그 언어 번역이 없으면 한국어다.
 */
public record VisibleMenu(
	String menuKey,
	String labelKey,
	String text,
	String urlPath,
	int sortOrder
) {
}

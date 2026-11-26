package org.wonik.coreviaai_be.domain.menu.service.result;

import org.wonik.coreviaai_be.domain.i18n.entity.I18nLabel;
import org.wonik.coreviaai_be.domain.menu.entity.Menu;

/** 관리자 메뉴 목록·수정 결과. version은 연결된 라벨의 낙관적 잠금 값이다. */
public record AdminMenuView(Menu menu, I18nLabel label) {
}

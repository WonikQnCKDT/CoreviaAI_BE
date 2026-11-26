package org.wonik.coreviaai_be.domain.menu.service;

import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.admin.service.AdminLabelService;
import org.wonik.coreviaai_be.domain.i18n.entity.I18nLabel;
import org.wonik.coreviaai_be.domain.i18n.repository.I18nLabelRepository;
import org.wonik.coreviaai_be.domain.menu.entity.Menu;
import org.wonik.coreviaai_be.domain.menu.repository.MenuRepository;
import org.wonik.coreviaai_be.domain.menu.service.result.AdminMenuView;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 시스템 관리자의 메뉴 문구 관리(FR-ADM-08). 메뉴 키·경로는 바꾸지 않고 연결된 라벨 문구만 고친다.
 * 변경 이력은 라벨 수정과 같은 감사 로그(I18N_LABEL_CHANGED)에 남는다.
 */
@Service
@RequiredArgsConstructor
public class AdminMenuService {

	private final MenuRepository menuRepository;
	private final I18nLabelRepository labelRepository;
	private final AdminLabelService adminLabelService;

	@Transactional(readOnly = true)
	public java.util.List<AdminMenuView> findAll() {
		final var menus = menuRepository.findAllByOrderBySortOrderAscIdAsc();
		if (menus.isEmpty()) {
			return java.util.List.of();
		}
		final var labels = labelRepository.findAllByLabelKeyIn(menus.stream().map(Menu::getLabelKey).toList()).stream()
			.collect(Collectors.toMap(I18nLabel::getLabelKey, Function.identity()));
		return menus.stream()
			.map(menu -> new AdminMenuView(menu, requireLabel(labels.get(menu.getLabelKey()))))
			.toList();
	}

	@Transactional
	public AdminMenuView updateText(
		final AuditActor actor,
		final Long menuId,
		final String textKo,
		final String textEn,
		final String textZhTw,
		final long version
	) {
		final Menu menu = menuRepository.findById(menuId)
			.orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND));
		final I18nLabel label = labelRepository.findByLabelKey(menu.getLabelKey())
			.orElseThrow(() -> new CustomException(ErrorCode.LABEL_NOT_FOUND));
		final I18nLabel updated = adminLabelService.update(actor, label.getId(), textKo, textEn, textZhTw, version);
		return new AdminMenuView(menu, updated);
	}

	private static I18nLabel requireLabel(final I18nLabel label) {
		if (label == null) {
			throw new CustomException(ErrorCode.LABEL_NOT_FOUND);
		}
		return label;
	}
}

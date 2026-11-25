package org.wonik.coreviaai_be.domain.menu.service;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.i18n.entity.I18nLabel;
import org.wonik.coreviaai_be.domain.i18n.entity.type.LabelLanguage;
import org.wonik.coreviaai_be.domain.i18n.repository.I18nLabelRepository;
import org.wonik.coreviaai_be.domain.menu.entity.Menu;
import org.wonik.coreviaai_be.domain.menu.repository.MenuRepository;
import org.wonik.coreviaai_be.domain.menu.service.result.VisibleMenu;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.user.entity.User;
import org.wonik.global.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 로그인 사용자의 메뉴(FR-ADM-08). 문구는 다국어 라벨에서 읽고, 역할이 맞지 않거나 비활성인 메뉴는 빼 준다.
 * 사용자 선호 언어가 있으면 그 언어를 쓰고, 없으면 요청 언어를 쓴다. 번역이 없으면 한국어다.
 */
@Service
@RequiredArgsConstructor
public class MenuQueryService {

	private final MenuRepository menuRepository;
	private final I18nLabelRepository labelRepository;
	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public List<VisibleMenu> findVisible(final Long userId, final String lang, final String acceptLanguage) {
		final User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		final LabelLanguage language = resolveLanguage(user.getPreferredLanguage(), lang, acceptLanguage);
		final List<Menu> visible = menuRepository.findAllByOrderBySortOrderAscIdAsc().stream()
			.filter(menu -> menu.visibleTo(user.getSystemRole()))
			.toList();
		if (visible.isEmpty()) {
			return List.of();
		}
		final var labels = labelRepository.findAllByLabelKeyIn(visible.stream().map(Menu::getLabelKey).toList())
			.stream()
			.collect(Collectors.toMap(I18nLabel::getLabelKey, Function.identity()));
		return visible.stream()
			.map(menu -> {
				final I18nLabel label = labels.get(menu.getLabelKey());
				if (label == null) {
					throw new CustomException(ErrorCode.LABEL_NOT_FOUND);
				}
				return new VisibleMenu(
					menu.getMenuKey(), menu.getLabelKey(), label.displayText(language), menu.getUrlPath(), menu.getSortOrder());
			})
			.toList();
	}

	private static LabelLanguage resolveLanguage(final String preferred, final String lang, final String acceptLanguage) {
		if (preferred != null && !preferred.isBlank()) {
			final var fromPreference = LabelLanguage.fromTag(preferred);
			if (fromPreference.isPresent()) {
				return fromPreference.get();
			}
		}
		return LabelLanguage.resolve(lang, acceptLanguage);
	}
}

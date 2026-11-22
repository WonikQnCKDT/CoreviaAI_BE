package org.wonik.coreviaai_be.domain.wbs.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.wonik.global.user.entity.User;
import org.wonik.global.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 업무 화면(업무 상세·이력, 칸반 보드)에 보여 줄 사용자 이름을 한 번에 읽는다(N+1 없음).
 */
@Component
@RequiredArgsConstructor
public class UserNameReader {

	private final UserRepository userRepository;

	/** null ID는 건너뛴다. 찾지 못한 사용자는 맵에 없고, null 키 조회도 되는 HashMap을 돌려준다. */
	public Map<Long, String> namesOf(final Collection<Long> userIds) {
		final Set<Long> ids = new HashSet<>();
		userIds.stream().filter(Objects::nonNull).forEach(ids::add);
		final Map<Long, String> names = new HashMap<>();
		if (ids.isEmpty()) {
			return names;
		}
		for (final User user : userRepository.findAllById(ids)) {
			names.put(user.getId(), user.getName());
		}
		return names;
	}
}

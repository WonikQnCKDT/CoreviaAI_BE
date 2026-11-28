package org.wonik.coreviaai_be.domain.user.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.department.entity.Department;
import org.wonik.coreviaai_be.domain.department.repository.DepartmentRepository;
import org.wonik.coreviaai_be.domain.user.policy.UserSearchKeyword;
import org.wonik.coreviaai_be.domain.user.service.result.UserSearchResult;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;
import org.wonik.global.user.entity.UserStatus;
import org.wonik.global.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 일반 사용자용 사용자 이름 검색(FR-PRJ-01 ⑤ 멤버 초대, FR-PRJ-04 멤버 초대 공용).
 * 관리자용 목록(/api/admin/users)과 달리 초대할 수 있는 사용자(활성 일반 사용자)만, 상한 건수까지만 돌려준다.
 * 초대 가능 기준은 ProjectMemberService.requireInvitableUsers와 같다(활성 USER).
 */
@Service
@RequiredArgsConstructor
public class UserDirectoryService {

	public static final int DEFAULT_SIZE = 20;
	public static final int MAX_SIZE = 20;

	private final UserRepository userRepository;
	private final DepartmentRepository departmentRepository;

	/**
	 * 이름 부분 일치(대소문자 무시) 검색. 본인은 제외하고 이름 오름차순, 같은 이름은 ID 오름차순이다.
	 *
	 * @param requesterId 검색하는 사용자. 결과에서 제외한다
	 * @param size 1~MAX_SIZE(범위 검증은 요청 경계에서 한다)
	 */
	@Transactional(readOnly = true)
	public List<UserSearchResult> searchInvitable(final Long requesterId, final UserSearchKeyword keyword, final int size) {
		final List<User> users = userRepository.searchByNameForInvite(
			keyword.likePattern(),
			UserStatus.ACTIVE,
			SystemRole.USER,
			requesterId,
			PageRequest.of(0, size)
		);
		final Map<Long, String> departmentNames = departmentNamesOf(users);
		return users.stream()
			.map(user -> new UserSearchResult(user, departmentNames.get(user.getDepartmentId())))
			.toList();
	}

	/** 결과 사용자들의 부서 이름을 한 번의 조회로 읽는다(N+1 방지). */
	private Map<Long, String> departmentNamesOf(final List<User> users) {
		final Set<Long> departmentIds = users.stream()
			.map(User::getDepartmentId)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
		if (departmentIds.isEmpty()) {
			// 부서 없는 사용자의 get(null)이 가능하도록 null 키를 허용하는 HashMap을 쓴다(Map.of()는 get(null)에서 예외)
			return new HashMap<>();
		}
		return departmentRepository.findAllById(departmentIds).stream()
			.collect(Collectors.toMap(Department::getId, Department::getName));
	}
}

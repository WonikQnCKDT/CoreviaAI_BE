package org.wonik.coreviaai_be.domain.todo.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.project.policy.ProjectAccessPolicy;
import org.wonik.coreviaai_be.domain.project.policy.ProjectVisibilityPolicy;
import org.wonik.coreviaai_be.domain.todo.pagination.TodoCursor;
import org.wonik.coreviaai_be.domain.todo.repository.TodoFeedRepository;
import org.wonik.coreviaai_be.domain.todo.service.result.TodoItem;
import org.wonik.global.common.pagination.CursorPage;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoFeedService {
	public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");
	private final TodoFeedRepository repository;
	private final ProjectAccessPolicy accessPolicy;
	private final ProjectVisibilityPolicy visibilityPolicy;
	private final Clock clock;

	public CursorPage<TodoItem> findPage(final Long userId, final boolean done, final int size,
		final int dueSoonDays, final String encodedCursor) {
		final TodoCursor cursor = TodoCursor.decode(encodedCursor, userId, done, dueSoonDays);
		final LocalDate today = cursor == null ? LocalDate.ofInstant(clock.instant(), BUSINESS_ZONE) : cursor.today();
		final User viewer = accessPolicy.getViewer(userId);
		final boolean ordinary = viewer.getSystemRole() == SystemRole.USER;
		final boolean executive = viewer.getSystemRole() == SystemRole.EXECUTIVE;
		final Set<Long> scope = executive ? visibilityPolicy.executiveScope(viewer.getDepartmentId()) : Set.of();
		final List<TodoItem> fetched = repository.findPage(userId, done, size, today, dueSoonDays, cursor,
			ordinary || executive, ordinary, scope);
		final boolean hasNext = fetched.size() > size;
		final List<TodoItem> items = List.copyOf(hasNext ? fetched.subList(0, size) : fetched);
		final String next = hasNext ? TodoCursor.encode(userId, done, dueSoonDays, today, items.get(items.size() - 1)) : null;
		return new CursorPage<>(items, next, hasNext);
	}
}

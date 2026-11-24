package org.wonik.coreviaai_be.domain.schedule.service;
import java.time.Instant;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.project.policy.ProjectAccessPolicy;
import org.wonik.coreviaai_be.domain.project.policy.ProjectVisibilityPolicy;
import org.wonik.coreviaai_be.domain.schedule.repository.CalendarRepository;
import org.wonik.coreviaai_be.domain.schedule.service.result.CalendarPage;
import org.wonik.global.common.time.QueryRangePolicy;
import org.wonik.global.common.type.SystemRole;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {
    private final CalendarRepository repository;
    private final ProjectAccessPolicy access;
    private final ProjectVisibilityPolicy visibility;
    public CalendarPage find(Long userId, Instant from, Instant to, int page, int size) {
        QueryRangePolicy.requireInstantRange(from, to);
        final var user = access.getViewer(userId);
        final boolean executive = user.getSystemRole() == SystemRole.EXECUTIVE;
        final var rows = repository.find(userId, from, to, page, size,
            user.getSystemRole() != SystemRole.SYSTEM_ADMIN, !executive,
            executive ? visibility.executiveScope(user.getDepartmentId()) : Set.of());
        return new CalendarPage(rows.stream().limit(size).toList(), rows.size() > size);
    }
}

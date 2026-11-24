package org.wonik.coreviaai_be.domain.schedule.repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.wonik.coreviaai_be.domain.schedule.service.result.CalendarItem;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CalendarRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private static final String SQL = """
        select * from (
          select e.id, 'PERSONAL' as source, e.title, e.start_at as starts_at, e.end_at as ends_at, e.all_day,
            cast(null as bigint) as project_id, cast(null as varchar(20)) as task_status,
            cast(null as integer) as progress, cast(null as bigint) as spent_minutes
          from personal_events e where e.user_id = :userId
          union all
          select t.id, 'TASK' as source, t.name as title,
            cast(cast(t.start_date as varchar) || ' 00:00:00+09:00' as timestamp with time zone) as starts_at,
            cast(cast(t.end_date + 1 as varchar) || ' 00:00:00+09:00' as timestamp with time zone) as ends_at,
            true as all_day, t.project_id, t.status as task_status, t.progress,
            (select coalesce(sum(wt.spent_minutes), 0) from worklog_tasks wt where wt.task_id = t.id) as spent_minutes
          from wbs_tasks t join projects p on p.id = t.project_id
          where t.assignee_user_id = :userId and :allowTasks = true and p.status <> 'CANCELLED'
            and exists (select 1 from project_features f where f.project_id = p.id and f.feature in ('WBS','KANBAN'))
            and exists (select 1 from project_members m where m.project_id = p.id and m.user_id = :userId)
            and (:ordinary = true or p.visibility = 'COMPANY' or exists
              (select 1 from project_departments d where d.project_id = p.id and d.department_id in (:scope)))
        ) calendar
        where starts_at < :toTime and ends_at > :fromTime
        order by starts_at, source, id limit :fetchSize offset :offset
        """;

    public List<CalendarItem> find(Long userId, Instant from, Instant to, int page, int size,
        boolean allowTasks, boolean ordinary, Set<Long> scope) {
        final var params = new MapSqlParameterSource().addValue("userId", userId)
            .addValue("fromTime", from.atOffset(ZoneOffset.UTC)).addValue("toTime", to.atOffset(ZoneOffset.UTC))
            .addValue("allowTasks", allowTasks).addValue("ordinary", ordinary)
            .addValue("scope", scope.isEmpty() ? Set.of(-1L) : scope)
            .addValue("fetchSize", size + 1).addValue("offset", (long) page * size);
        return jdbc.query(SQL, params, (rs, row) -> new CalendarItem(rs.getLong("id"), rs.getString("source"),
            rs.getString("title"), rs.getObject("starts_at", OffsetDateTime.class).toInstant(),
            rs.getObject("ends_at", OffsetDateTime.class).toInstant(), rs.getBoolean("all_day"),
            rs.getObject("project_id", Long.class), rs.getString("task_status"),
            rs.getObject("progress", Integer.class), rs.getObject("spent_minutes", Long.class)));
    }
}

package org.wonik.coreviaai_be.domain.todo.repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.wonik.coreviaai_be.domain.todo.pagination.TodoCursor;
import org.wonik.coreviaai_be.domain.todo.service.result.TodoItem;
import lombok.RequiredArgsConstructor;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectFeature;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectStatus;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectVisibility;

/** One bounded UNION query; no per-item lookup, persistence, or asynchronous replication. */
@Repository
@RequiredArgsConstructor
public class TodoFeedRepository {
	private final NamedParameterJdbcTemplate jdbc;
	private static final String SQL = """
		select * from (
		 select todo_rows.*,
		   case when due_date = :today then 0
		        when due_date > :today and due_date <= :soon then 1
		        when due_date < :today then 2
		        when due_date is not null then 3 else 4 end as urgency,
		   coalesce(due_date, cast('9999-12-31' as date)) as due_sort
		 from (
		 select t.id, 'PERSONAL' as source, 0 as source_order, t.title, t.due_date, t.done, t.done_at,
		        cast(null as bigint) as project_id, cast(null as varchar(20)) as task_status
		 from personal_todos t where t.user_id = :userId and t.done = :done and t.deleted_at is null
		 union all
		 select w.id, 'TASK' as source, 1 as source_order, w.name as title, w.end_date as due_date,
		        false as done, cast(null as timestamp with time zone) as done_at, w.project_id, w.status as task_status
		 from wbs_tasks w join projects p on p.id = w.project_id
		 where w.assignee_user_id = :userId and w.status <> :completed and :done = false
		   and :allowTasks = true
		   and p.status <> :cancelled
		   and exists (select 1 from project_features pf
		       where pf.project_id = p.id and pf.feature in (:taskFeatures))
		   and exists (select 1 from project_members m where m.project_id = w.project_id and m.user_id = :userId)
		   and (:ordinary = true or p.visibility = :company or exists
		       (select 1 from project_departments pd where pd.project_id = p.id and pd.department_id in (:scope)))
		 ) todo_rows
		) ranked
		where (urgency, due_sort, source_order, id) > (:rank, :dueSort, :sourceOrder, :lastId)
		order by urgency, due_sort, source_order, id limit :fetchSize
		""";

	public List<TodoItem> findPage(final Long userId, final boolean done, final int size, final LocalDate today,
		final int dueSoonDays, final TodoCursor cursor, final boolean allowTasks, final boolean ordinary,
		final Set<Long> executiveScope) {
		final MapSqlParameterSource params = new MapSqlParameterSource()
			.addValue("userId", userId).addValue("done", done).addValue("completed", WbsTaskStatus.DONE.name())
			.addValue("cancelled", ProjectStatus.CANCELLED.name())
			.addValue("taskFeatures", Set.of(ProjectFeature.WBS.name(), ProjectFeature.KANBAN.name()))
			.addValue("allowTasks", allowTasks).addValue("ordinary", ordinary).addValue("company", ProjectVisibility.COMPANY.name())
			.addValue("scope", executiveScope.isEmpty() ? Set.of(-1L) : executiveScope)
			.addValue("today", today).addValue("soon", today.plusDays(dueSoonDays))
			.addValue("rank", cursor == null ? -1 : cursor.urgency())
			.addValue("dueSort", cursor == null ? today : cursor.dueSort())
			.addValue("sourceOrder", cursor == null ? 0 : cursor.sourceOrder())
			.addValue("lastId", cursor == null ? 0L : cursor.id()).addValue("fetchSize", size + 1);
		return jdbc.query(SQL, params, (rs, row) -> {
			final LocalDate dueDate = rs.getObject("due_date", LocalDate.class);
			final OffsetDateTime doneAt = rs.getObject("done_at", OffsetDateTime.class);
			return new TodoItem(rs.getLong("id"), rs.getString("source"), rs.getString("title"), dueDate,
				rs.getBoolean("done"), doneAt == null ? null : doneAt.toInstant(),
				rs.getObject("project_id", Long.class), rs.getString("task_status"), rs.getInt("urgency"),
				!rs.getBoolean("done") && dueDate != null && dueDate.isBefore(today));
		});
	}
}

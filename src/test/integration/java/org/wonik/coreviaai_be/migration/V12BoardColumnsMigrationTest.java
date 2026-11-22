package org.wonik.coreviaai_be.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * V12(board_columns, SCRUM-110) 백필을 V11 상태의 데이터에 적용해 확인한다. 공유 테스트 DB와 섞이지 않게
 * 테스트마다 별도 인메모리 H2(PostgreSQL 모드)를 만들어 V11까지 올리고 데이터를 넣은 뒤 V12를 적용한다.
 */
class V12BoardColumnsMigrationTest {

	private SingleConnectionDataSource dataSource;
	private JdbcTemplate jdbc;

	@BeforeEach
	void migrateToV11() {
		// 연결 하나를 계속 쓴다(연결마다 열고 닫으면 인메모리 DB가 닫혀 제약 평가가 실패한다)
		dataSource = new SingleConnectionDataSource("jdbc:h2:mem:v12-" + System.nanoTime()
			+ ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH", "sa", "", true);
		jdbc = new JdbcTemplate(dataSource);
		flyway("11").migrate();
	}

	@AfterEach
	void close() {
		dataSource.destroy();
	}

	@Test
	@DisplayName("백필: 기존 프로젝트마다 기본 컬럼 4개, 기존 작업은 자기 상태의 기본 컬럼, 상태 변경 시각은 비움")
	void backfill() {
		insertUser();
		final long userId = jdbc.queryForObject("SELECT id FROM users", Long.class);
		final long first = insertProject("P-1", userId);
		final long second = insertProject("P-2", userId);
		final long empty = insertProject("P-3", userId);
		final long todoTask = insertTask(first, "TODO", userId);
		final long doneTask = insertTask(first, "DONE", userId);
		final long otherTask = insertTask(second, "IN_PROGRESS", userId);

		flyway("12").migrate();

		for (final long projectId : List.of(first, second, empty)) {
			final List<Map<String, Object>> columns = jdbc.queryForList("SELECT name, status_category, sort_order,"
				+ " wip_limit FROM board_columns WHERE project_id = ? ORDER BY sort_order", projectId);
			assertThat(columns).extracting(row -> row.get("status_category"))
				.containsExactly("TODO", "IN_PROGRESS", "REVIEW", "DONE");
			assertThat(columns).extracting(row -> ((Number)row.get("sort_order")).intValue())
				.containsExactly(1, 2, 3, 4);
			assertThat(columns).allSatisfy(row -> {
				assertThat(row.get("name")).isNull();
				assertThat(row.get("wip_limit")).isNull();
			});
		}
		assertThat(columnOf(todoTask)).isEqualTo(columnId(first, "TODO"));
		assertThat(columnOf(doneTask)).isEqualTo(columnId(first, "DONE"));
		assertThat(columnOf(otherTask)).isEqualTo(columnId(second, "IN_PROGRESS"));
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM wbs_tasks WHERE status_changed_at IS NOT NULL",
			Integer.class)).isZero();
	}

	@Test
	@DisplayName("제약: 상태·WIP 한도·빈 이름은 CHECK로 거부한다")
	void constraints() {
		flyway("12").migrate();
		insertUser();
		final long projectId = insertProject("P-1", jdbc.queryForObject("SELECT id FROM users", Long.class));

		assertThat(insertColumn(projectId, "'리뷰'", "'BLOCKED'", "NULL")).isFalse();
		assertThat(insertColumn(projectId, "'리뷰'", "'REVIEW'", "0")).isFalse();
		assertThat(insertColumn(projectId, "'  '", "'REVIEW'", "NULL")).isFalse();
		assertThat(insertColumn(projectId, "NULL", "'REVIEW'", "1")).isTrue();
	}

	private boolean insertColumn(final long projectId, final String name, final String status, final String wip) {
		try {
			jdbc.update("INSERT INTO board_columns (project_id, name, status_category, sort_order, wip_limit, created_at,"
				+ " updated_at) VALUES (" + projectId + ", " + name + ", " + status + ", 5, " + wip
				+ ", CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
			return true;
		} catch (final org.springframework.dao.DataAccessException e) {
			return false;
		}
	}

	private void insertUser() {
		jdbc.update("INSERT INTO users (login_id, name, system_role, status, created_at, updated_at)"
			+ " VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", "pm01", "PM", "USER", "ACTIVE");
	}

	private Flyway flyway(final String target) {
		return Flyway.configure()
			.dataSource(dataSource)
			.locations("classpath:db/migration")
			.target(target)
			.load();
	}

	private long insertProject(final String code, final long userId) {
		jdbc.update("INSERT INTO projects (code, name, visibility, methodology, department_type, status, created_by,"
				+ " template, created_at, updated_at) VALUES (?, '프로젝트', 'COMPANY', 'WATERFALL', 'SW', 'PLANNED', ?,"
				+ " 'IT_PROJECT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", code, userId);
		return jdbc.queryForObject("SELECT id FROM projects WHERE code = ?", Long.class, code);
	}

	private long insertTask(final long projectId, final String status, final long userId) {
		jdbc.update("INSERT INTO wbs_tasks (project_id, sort_order, name, start_date, end_date, status, source, created_by,"
			+ " created_at, updated_at) VALUES (?, 1, ?, DATE '2026-12-01', DATE '2026-12-02', ?, 'MANUAL', ?,"
			+ " CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", projectId, status, status, userId);
		return jdbc.queryForObject("SELECT MAX(id) FROM wbs_tasks", Long.class);
	}

	private Long columnOf(final long taskId) {
		return jdbc.queryForObject("SELECT board_column_id FROM wbs_tasks WHERE id = ?", Long.class, taskId);
	}

	private Long columnId(final long projectId, final String status) {
		return jdbc.queryForObject("SELECT id FROM board_columns WHERE project_id = ? AND status_category = ?",
			Long.class, projectId, status);
	}
}

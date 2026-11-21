package org.wonik.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 통합 테스트 사이에 공유 H2의 업무 데이터를 FK 역순으로 비운다.
 * 여러 테스트 클래스가 같은 스프링 컨텍스트(같은 DB)를 쓰므로, 한 클래스가 남긴 행이 다른 클래스의
 * 사용자·부서 삭제를 FK 위반으로 막지 않도록 테이블을 추가하면 여기에도 추가한다.
 */
@Component
@RequiredArgsConstructor
public class DatabaseCleaner {

	private final JdbcTemplate jdbcTemplate;

	public void clearAll() {
		jdbcTemplate.update("DELETE FROM personal_todos");
		jdbcTemplate.update("DELETE FROM ai_drafts");
		jdbcTemplate.update("DELETE FROM milestone_tasks");
		jdbcTemplate.update("DELETE FROM milestones");
		jdbcTemplate.update("DELETE FROM wbs_dependencies");
		// 하위작업이 상위 작업을 참조하므로 하위작업을 먼저 지운다.
		jdbcTemplate.update("DELETE FROM wbs_tasks WHERE parent_task_id IS NOT NULL");
		jdbcTemplate.update("DELETE FROM wbs_tasks");
		jdbcTemplate.update("DELETE FROM project_member_histories");
		jdbcTemplate.update("DELETE FROM project_phases");
		jdbcTemplate.update("DELETE FROM project_members");
		jdbcTemplate.update("DELETE FROM project_actors");
		jdbcTemplate.update("DELETE FROM project_departments");
		jdbcTemplate.update("DELETE FROM project_features");
		jdbcTemplate.update("DELETE FROM projects");
		jdbcTemplate.update("DELETE FROM audit_logs");
		jdbcTemplate.update("DELETE FROM login_history");
		jdbcTemplate.update("DELETE FROM local_credentials");
		jdbcTemplate.update("DELETE FROM users");
		// 부서는 상위 참조를 먼저 끊고 지운다.
		jdbcTemplate.update("UPDATE departments SET parent_id = NULL");
		jdbcTemplate.update("DELETE FROM departments");
	}
}

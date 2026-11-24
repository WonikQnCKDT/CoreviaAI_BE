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
		jdbcTemplate.update("DELETE FROM meeting_create_requests");
		jdbcTemplate.update("DELETE FROM action_items");
		jdbcTemplate.update("DELETE FROM meeting_decisions");
		jdbcTemplate.update("DELETE FROM meeting_attendees");
		jdbcTemplate.update("DELETE FROM meetings");
		jdbcTemplate.update("DELETE FROM personal_todos");
		jdbcTemplate.update("DELETE FROM personal_events");
		jdbcTemplate.update("DELETE FROM worklog_tasks");
		jdbcTemplate.update("DELETE FROM worklogs");
		jdbcTemplate.update("DELETE FROM ai_drafts");
		// 업무 이력은 운영에서 지우지 않는 기록이지만(append-only) 테스트 사이에는 비운다. task_id FK는 없고 actor_user_id만 users를 참조한다.
		jdbcTemplate.update("DELETE FROM task_histories");
		jdbcTemplate.update("DELETE FROM milestone_tasks");
		jdbcTemplate.update("DELETE FROM milestones");
		jdbcTemplate.update("DELETE FROM wbs_dependencies");
		// 하위작업이 상위 작업을 참조하므로 하위작업을 먼저 지운다.
		jdbcTemplate.update("DELETE FROM wbs_tasks WHERE parent_task_id IS NOT NULL");
		jdbcTemplate.update("DELETE FROM wbs_tasks");
		// 칸반 컬럼은 작업(board_column_id)이 참조하므로 작업 다음에 지운다.
		jdbcTemplate.update("DELETE FROM board_columns");
		jdbcTemplate.update("DELETE FROM project_member_histories");
		jdbcTemplate.update("DELETE FROM project_phases");
		jdbcTemplate.update("DELETE FROM project_members");
		jdbcTemplate.update("DELETE FROM project_actors");
		jdbcTemplate.update("DELETE FROM project_departments");
		jdbcTemplate.update("DELETE FROM project_features");
		jdbcTemplate.update("DELETE FROM projects");
		jdbcTemplate.update("DELETE FROM audit_logs");
		jdbcTemplate.update("DELETE FROM error_logs");
		jdbcTemplate.update("DELETE FROM login_history");
		jdbcTemplate.update("DELETE FROM local_credentials");
		jdbcTemplate.update("DELETE FROM users");
		// 부서는 상위 참조를 먼저 끊고 지운다.
		jdbcTemplate.update("UPDATE departments SET parent_id = NULL");
		jdbcTemplate.update("DELETE FROM departments");
		// 공통 코드는 V8 시드를 남기고, 테스트가 만든 TEST_ 그룹과 그 코드만 지운다(테스트는 시드 그룹을 바꾸지 않는다).
		jdbcTemplate.update("DELETE FROM codes WHERE group_id IN (SELECT id FROM code_groups WHERE group_code LIKE 'TEST\\_%')");
		jdbcTemplate.update("DELETE FROM code_groups WHERE group_code LIKE 'TEST\\_%'");
		// 다국어 라벨도 V9 시드를 남기고, 테스트가 만든 라벨(test. 키, TEST_ 코드 그룹의 코드 라벨)만 지운다.
		// 테스트는 시드 라벨의 문구를 바꾸지 않는다(바꾸면 다른 테스트의 기대값이 달라진다).
		jdbcTemplate.update("DELETE FROM i18n_labels WHERE label_key LIKE 'test.%' OR label_key LIKE 'code.TEST\\_%'"
			+ " OR label_key LIKE 'codeGroup.TEST\\_%'");
	}
}

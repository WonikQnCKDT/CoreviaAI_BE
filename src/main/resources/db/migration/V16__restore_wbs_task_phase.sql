-- SCRUM-107: WBS 작업을 사용자 정의 프로젝트 단계에 연결한다.
-- 단계가 없는 프로젝트와 기존 데이터의 호환성을 위해 nullable이며, 서비스에서 같은 프로젝트 소속을 검증한다.
ALTER TABLE wbs_tasks
    ADD COLUMN phase_id BIGINT,
    ADD CONSTRAINT fk_wbs_tasks_phase FOREIGN KEY (phase_id) REFERENCES project_phases (id);

CREATE INDEX idx_wbs_tasks_project_phase ON wbs_tasks (project_id, phase_id);

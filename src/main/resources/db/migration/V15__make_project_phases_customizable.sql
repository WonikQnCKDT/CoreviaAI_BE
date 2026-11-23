-- 프로젝트 단계를 방법론의 고정 enum에서 프로젝트별 사용자 정의 이름·순서로 전환한다.
-- 기존 5단계 데이터는 한국어 이름으로 이관하고 ID·순서·게이트 상태·참조 관계는 유지한다.
-- 애플리케이션은 이 migration과 함께 배포해야 한다. 되돌릴 때는 phase_type을 다시 추가해
-- 현재 이름을 매핑할 수 없는 사용자 정의 단계의 처리 정책을 먼저 결정한 뒤 새 migration으로 복원한다.

ALTER TABLE project_phases ADD COLUMN name VARCHAR(100);

UPDATE project_phases
SET name = CASE phase_type
    WHEN 'ANALYSIS' THEN '분석'
    WHEN 'DESIGN' THEN '설계'
    WHEN 'DEVELOPMENT' THEN '개발'
    WHEN 'TEST' THEN '테스트'
    WHEN 'TRANSITION' THEN '이행'
    ELSE phase_type
END;

ALTER TABLE project_phases ALTER COLUMN name SET NOT NULL;
ALTER TABLE project_phases DROP COLUMN phase_type;
ALTER TABLE project_phases ADD CONSTRAINT ck_project_phases_phase_order CHECK (phase_order >= 1);

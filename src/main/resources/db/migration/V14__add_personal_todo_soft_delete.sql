-- FR-PSN-01 (SCRUM-111): user-approved soft deletion. Do not modify already-applied V13.
-- Expand: existing rows remain active (deleted_at NULL); no personal data or creation keys are removed.
-- Deployment: integrate reserved V8-V12, then V13, then V14 before deploying this API version.
-- Old API versions do not filter tombstones: do not serve old/new API versions concurrently after deletion is enabled.
-- Roll forward by disabling DELETE and keeping tombstone-aware readers. Do not roll back to readers that expose deleted rows.
-- Retention/purge/restore policy is not decided; no automatic physical deletion or restoration is introduced.
ALTER TABLE personal_todos ADD COLUMN deleted_at TIMESTAMP(6) WITH TIME ZONE;
CREATE INDEX idx_personal_todos_user_id_deleted_at_done_due_date
    ON personal_todos (user_id, deleted_at, done, due_date);

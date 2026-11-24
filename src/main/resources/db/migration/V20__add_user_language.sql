ALTER TABLE users ADD COLUMN preferred_language VARCHAR(5);
ALTER TABLE users ADD CONSTRAINT ck_users_preferred_language CHECK (preferred_language IN ('ko', 'en', 'zh-TW'));

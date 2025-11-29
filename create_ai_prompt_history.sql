-- Stores archived versions of ai_prompts rows.
-- Run this after the ai_prompts table exists.

CREATE TABLE IF NOT EXISTS ai_prompt_history (
    id SERIAL PRIMARY KEY,
    prompt_id INTEGER NOT NULL REFERENCES ai_prompts(id),
    prompt_key VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(512),
    prompt_text TEXT NOT NULL,
    version INTEGER NOT NULL,
    change_reason TEXT,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    archived_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ai_prompt_history_key_version
    ON ai_prompt_history (prompt_key, version DESC);

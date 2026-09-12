ALTER TABLE ai_conversation
    ADD COLUMN request_id VARCHAR(64) NULL AFTER session_id,
    ADD UNIQUE KEY uk_ai_conversation_request_role (user_id, session_id, request_id, role);

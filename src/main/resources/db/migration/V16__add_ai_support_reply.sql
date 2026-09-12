ALTER TABLE ai_support_ticket
    ADD COLUMN agent_reply VARCHAR(1000) NULL AFTER latest_message;

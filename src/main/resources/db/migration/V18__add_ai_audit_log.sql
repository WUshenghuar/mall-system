CREATE TABLE ai_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(64) NULL,
    event_type VARCHAR(32) NOT NULL,
    tool_name VARCHAR(64) NULL,
    outcome VARCHAR(32) NOT NULL,
    latency_ms INT NOT NULL DEFAULT 0,
    detail VARCHAR(255) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ai_audit_user_time (user_id, create_time),
    INDEX idx_ai_audit_request (request_id)
) COMMENT='AI客服审计日志';

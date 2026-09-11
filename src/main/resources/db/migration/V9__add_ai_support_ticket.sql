CREATE TABLE ai_support_ticket (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_no VARCHAR(32) NOT NULL,
    member_id BIGINT NOT NULL,
    conversation_id VARCHAR(64) NULL,
    subject VARCHAR(128) NOT NULL,
    latest_message VARCHAR(1000) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0待接管 1处理中 2已解决',
    assigned_user_id BIGINT NULL,
    handled_note VARCHAR(500) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_ai_support_ticket_no (ticket_no),
    INDEX idx_ai_support_ticket_member_status (member_id, status, id),
    INDEX idx_ai_support_ticket_status (status, id)
) COMMENT='平台 AI 客服人工接管工单';

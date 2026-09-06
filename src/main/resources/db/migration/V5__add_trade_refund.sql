CREATE TABLE trade_refund (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no        VARCHAR(64) NOT NULL,
    user_id         BIGINT NOT NULL,
    refund_amount   DECIMAL(10, 2) NOT NULL,
    refund_reason   VARCHAR(500) NOT NULL,
    refund_status   TINYINT NOT NULL DEFAULT 0 COMMENT '0待审批 1已通过 2已驳回 3已退款',
    approver_id     BIGINT NULL,
    approve_comment VARCHAR(500) NULL,
    approve_time    DATETIME NULL,
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    INDEX idx_trade_refund_user_time (user_id, create_time),
    INDEX idx_trade_refund_status_time (refund_status, create_time),
    INDEX idx_trade_refund_order (order_no)
) COMMENT='C端交易退款申请';

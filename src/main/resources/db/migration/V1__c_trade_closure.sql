CREATE TABLE member_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    phone VARCHAR(20) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME,
    update_time DATETIME,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_member_account_member (member_id),
    UNIQUE KEY uk_member_account_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_trade_order_status_time ON trade_order(order_status, create_time);
CREATE UNIQUE INDEX uk_trade_pay_no ON trade_pay(pay_no);

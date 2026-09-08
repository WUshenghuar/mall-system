CREATE TABLE IF NOT EXISTS mk_coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_name VARCHAR(120) NOT NULL,
    coupon_type VARCHAR(32) NOT NULL,
    threshold DECIMAL(10, 2) NOT NULL DEFAULT 0,
    discount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(8) NOT NULL DEFAULT 'CNY',
    max_issue INT NULL,
    issued_count INT NOT NULL DEFAULT 0,
    per_limit INT NOT NULL DEFAULT 1,
    valid_start DATETIME NOT NULL,
    valid_end DATETIME NOT NULL,
    scope VARCHAR(32) NOT NULL DEFAULT 'ALL',
    scope_ids VARCHAR(1000) NULL,
    status TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_mk_coupon_available (status, valid_start, valid_end)
) COMMENT='营销优惠券';

CREATE TABLE IF NOT EXISTS mk_coupon_issue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    issue_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_time DATETIME NULL,
    status TINYINT NOT NULL DEFAULT 0,
    order_no VARCHAR(64) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_mk_coupon_issue_member (member_id, status, issue_time),
    INDEX idx_mk_coupon_issue_coupon (coupon_id, member_id)
) COMMENT='会员优惠券领取记录';

CREATE TABLE IF NOT EXISTS mk_activity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_name VARCHAR(160) NOT NULL,
    activity_type VARCHAR(32) NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_mk_activity_active (start_time, end_time, status)
) COMMENT='营销活动';

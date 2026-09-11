CREATE TABLE IF NOT EXISTS mk_activity_sku (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    seckill_price DECIMAL(10, 2) NULL,
    seckill_stock INT NULL,
    limit_per_user INT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_mk_activity_sku (activity_id, sku_id),
    INDEX idx_mk_activity_sku_activity (activity_id),
    INDEX idx_mk_activity_sku_sku (sku_id)
) COMMENT='营销活动关联商品';

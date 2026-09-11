SET @activity_id_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'trade_order_item' AND column_name = 'activity_id'
);
SET @activity_id_sql := IF(
    @activity_id_exists = 0,
    'ALTER TABLE trade_order_item ADD COLUMN activity_id BIGINT NULL AFTER sku_id',
    'SELECT 1'
);
PREPARE activity_id_stmt FROM @activity_id_sql;
EXECUTE activity_id_stmt;
DEALLOCATE PREPARE activity_id_stmt;

SET @activity_index_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'trade_order_item' AND index_name = 'idx_trade_activity_sku'
);
SET @activity_index_sql := IF(
    @activity_index_exists = 0,
    'ALTER TABLE trade_order_item ADD INDEX idx_trade_activity_sku (activity_id, sku_id, order_no)',
    'SELECT 1'
);
PREPARE activity_index_stmt FROM @activity_index_sql;
EXECUTE activity_index_stmt;
DEALLOCATE PREPARE activity_index_stmt;

SET @activity_reserved_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'trade_order_item' AND column_name = 'activity_stock_reserved'
);
SET @activity_reserved_sql := IF(
    @activity_reserved_exists = 0,
    'ALTER TABLE trade_order_item ADD COLUMN activity_stock_reserved TINYINT NOT NULL DEFAULT 0 AFTER activity_id',
    'SELECT 1'
);
PREPARE activity_reserved_stmt FROM @activity_reserved_sql;
EXECUTE activity_reserved_stmt;
DEALLOCATE PREPARE activity_reserved_stmt;

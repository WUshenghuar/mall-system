UPDATE mk_activity_sku SET limit_per_user = 1 WHERE limit_per_user IS NULL;
ALTER TABLE mk_activity_sku MODIFY limit_per_user INT NOT NULL DEFAULT 1;

SET @activity_sku_unique_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'mk_activity_sku'
      AND index_name = 'uk_mk_activity_sku'
);
SET @activity_sku_unique_sql := IF(
    @activity_sku_unique_exists = 0,
    'ALTER TABLE mk_activity_sku ADD UNIQUE KEY uk_mk_activity_sku (activity_id, sku_id)',
    'SELECT 1'
);
PREPARE activity_sku_unique_stmt FROM @activity_sku_unique_sql;
EXECUTE activity_sku_unique_stmt;
DEALLOCATE PREPARE activity_sku_unique_stmt;

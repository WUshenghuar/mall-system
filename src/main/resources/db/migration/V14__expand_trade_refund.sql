SET @refund_type_exists := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'trade_refund' AND column_name = 'refund_type');
SET @refund_type_sql := IF(@refund_type_exists = 0,
    'ALTER TABLE trade_refund ADD COLUMN refund_type TINYINT NOT NULL DEFAULT 0 AFTER refund_reason', 'SELECT 1');
PREPARE refund_type_stmt FROM @refund_type_sql; EXECUTE refund_type_stmt; DEALLOCATE PREPARE refund_type_stmt;

SET @original_status_exists := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'trade_refund' AND column_name = 'original_order_status');
SET @original_status_sql := IF(@original_status_exists = 0,
    'ALTER TABLE trade_refund ADD COLUMN original_order_status TINYINT NOT NULL DEFAULT 1 AFTER refund_type', 'SELECT 1');
PREPARE original_status_stmt FROM @original_status_sql; EXECUTE original_status_stmt; DEALLOCATE PREPARE original_status_stmt;

SET @evidence_exists := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'trade_refund' AND column_name = 'evidence_urls');
SET @evidence_sql := IF(@evidence_exists = 0,
    'ALTER TABLE trade_refund ADD COLUMN evidence_urls TEXT NULL AFTER original_order_status', 'SELECT 1');
PREPARE evidence_stmt FROM @evidence_sql; EXECUTE evidence_stmt; DEALLOCATE PREPARE evidence_stmt;

SET @return_company_exists := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'trade_refund' AND column_name = 'return_logistics_company');
SET @return_company_sql := IF(@return_company_exists = 0,
    'ALTER TABLE trade_refund ADD COLUMN return_logistics_company VARCHAR(64) NULL', 'SELECT 1');
PREPARE return_company_stmt FROM @return_company_sql; EXECUTE return_company_stmt; DEALLOCATE PREPARE return_company_stmt;

SET @return_no_exists := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'trade_refund' AND column_name = 'return_logistics_no');
SET @return_no_sql := IF(@return_no_exists = 0,
    'ALTER TABLE trade_refund ADD COLUMN return_logistics_no VARCHAR(128) NULL', 'SELECT 1');
PREPARE return_no_stmt FROM @return_no_sql; EXECUTE return_no_stmt; DEALLOCATE PREPARE return_no_stmt;

SET @return_time_exists := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'trade_refund' AND column_name = 'return_submit_time');
SET @return_time_sql := IF(@return_time_exists = 0,
    'ALTER TABLE trade_refund ADD COLUMN return_submit_time DATETIME NULL', 'SELECT 1');
PREPARE return_time_stmt FROM @return_time_sql; EXECUTE return_time_stmt; DEALLOCATE PREPARE return_time_stmt;

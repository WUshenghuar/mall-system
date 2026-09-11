SET @address_country_exists := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'member_address' AND column_name = 'country');
SET @address_country_sql := IF(@address_country_exists = 0,
    'ALTER TABLE member_address ADD COLUMN country VARCHAR(50) NULL AFTER receiver_phone', 'SELECT 1');
PREPARE address_country_stmt FROM @address_country_sql; EXECUTE address_country_stmt; DEALLOCATE PREPARE address_country_stmt;

SET @tax_amount_exists := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'trade_order' AND column_name = 'tax_amount');
SET @tax_amount_sql := IF(@tax_amount_exists = 0,
    'ALTER TABLE trade_order ADD COLUMN tax_amount DECIMAL(10,2) NOT NULL DEFAULT 0 AFTER freight_amount', 'SELECT 1');
PREPARE tax_amount_stmt FROM @tax_amount_sql; EXECUTE tax_amount_stmt; DEALLOCATE PREPARE tax_amount_stmt;

SET @order_currency_exists := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'trade_order' AND column_name = 'currency');
SET @order_currency_sql := IF(@order_currency_exists = 0,
    'ALTER TABLE trade_order ADD COLUMN currency VARCHAR(10) NOT NULL DEFAULT ''USD'' AFTER pay_amount', 'SELECT 1');
PREPARE order_currency_stmt FROM @order_currency_sql; EXECUTE order_currency_stmt; DEALLOCATE PREPARE order_currency_stmt;

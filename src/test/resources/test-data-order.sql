-- Test data for OrderServiceImplTest

INSERT INTO pm_spu (id, spu_code, spu_name, category_id, brand_id, status, create_time, update_time)
VALUES (10, 'SPU-TEST-001', '测试商品', 1, 1, 1, NOW(), NOW());

INSERT INTO pm_sku (id, spu_id, sku_code, price, currency, weight, status, create_time, update_time)
VALUES (100, 10, 'SKU-TEST-001', 29.99, 'USD', 0.5, 1, NOW(), NOW());

INSERT INTO fn_tax_config (id, category_id, tax_rate, tax_type, effective_date, create_time, update_time)
VALUES (1, 1, 5.00, 'VAT', '2025-01-01', NOW(), NOW());

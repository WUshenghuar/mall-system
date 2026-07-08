-- Test data for SkuServiceImplTest

INSERT INTO pm_spu (id, spu_code, spu_name, category_id, status, create_time, update_time)
VALUES (50, 'SPU-TEST-050', 'SKU测试商品', 1, 1, NOW(), NOW());

INSERT INTO pm_sku (id, spu_id, sku_code, price, currency, weight, status, create_time, update_time)
VALUES (300, 50, 'SKU-TEST-300', 29.99, 'USD', 0.5, 1, NOW(), NOW());

INSERT INTO pm_sku (id, spu_id, sku_code, price, currency, weight, status, create_time, update_time)
VALUES (301, 50, 'SKU-TEST-301', 19.99, 'USD', 0.3, 1, NOW(), NOW());

INSERT INTO pm_sku_stock (id, sku_id, warehouse_id, stock, locked_stock, create_time, update_time)
VALUES (1, 300, 1, 100, 0, NOW(), NOW());

INSERT INTO pm_sku_stock (id, sku_id, warehouse_id, stock, locked_stock, create_time, update_time)
VALUES (2, 301, 1, 50, 5, NOW(), NOW());

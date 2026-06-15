-- 预置角色 (店长/运营/客服/财务)
INSERT IGNORE INTO sys_role VALUES
(1, '店长',       'store_manager',   1, 1, '全部权限', NOW(), NOW(), 0),
(2, '运营专员',   'ops_specialist',  2, 1, '商品与活动运营', NOW(), NOW(), 0),
(3, '客服专员',   'cs_specialist',   3, 1, '订单与退款处理', NOW(), NOW(), 0),
(4, '财务专员',   'finance',         4, 1, '查看对账单', NOW(), NOW(), 0);

-- 默认管理员账号 (密码: admin123, BCrypt 加密)
INSERT IGNORE INTO sys_user VALUES
(1, 'admin', '$2a$10$eym7/7xeUDaruwSFuFFVuOx1SsjUK908k2730/c5XQ5E9B07vd6eW', '系统管理员', '13800000000', 'admin@mall.com', NULL, 1, NOW(), NOW(), 0),
(2, 'ops',   '$2a$10$eym7/7xeUDaruwSFuFFVuOx1SsjUK908k2730/c5XQ5E9B07vd6eW', '运营专员',   '13800000001', 'ops@mall.com',   NULL, 1, NOW(), NOW(), 0),
(3, 'cs',    '$2a$10$eym7/7xeUDaruwSFuFFVuOx1SsjUK908k2730/c5XQ5E9B07vd6eW', '客服专员',   '13800000002', 'cs@mall.com',    NULL, 1, NOW(), NOW(), 0),
(4, 'finance','$2a$10$eym7/7xeUDaruwSFuFFVuOx1SsjUK908k2730/c5XQ5E9B07vd6eW', '财务专员',   '13800000003', 'finance@mall.com',NULL, 1, NOW(), NOW(), 0);

-- 分配角色
INSERT IGNORE INTO sys_user_role VALUES
(1, 1, 1),   -- admin → 店长
(2, 2, 2),   -- ops → 运营专员
(3, 3, 3),   -- cs → 客服专员
(4, 4, 4);   -- finance → 财务专员

-- ==================== 商品模块初始数据 ====================

-- 商品分类
INSERT IGNORE INTO pm_category VALUES
(1,  '电子产品',     0, 1, 'LaptopOutlined',   1, 1, NOW(), NOW(), 0),
(2,  '手机平板',     1, 2, 'MobileOutlined',   1, 1, NOW(), NOW(), 0),
(3,  '电脑配件',     1, 2, 'DesktopOutlined',  2, 1, NOW(), NOW(), 0),
(4,  '服饰鞋包',     0, 1, 'ShoppingOutlined', 2, 1, NOW(), NOW(), 0),
(5,  '女装',         4, 2, 'SkinOutlined',     1, 1, NOW(), NOW(), 0),
(6,  '男装',         4, 2, 'UserOutlined',     2, 1, NOW(), NOW(), 0),
(7,  '家居用品',     0, 1, 'HomeOutlined',     3, 1, NOW(), NOW(), 0),
(8,  '美妆个护',     0, 1, 'SmileOutlined',    4, 1, NOW(), NOW(), 0);

-- 品牌
INSERT IGNORE INTO pm_brand VALUES
(1, 'Apple',     '', 'Apple Inc.',          1, 1, NOW(), NOW(), 0),
(2, 'Samsung',   '', 'Samsung Electronics', 2, 1, NOW(), NOW(), 0),
(3, 'Xiaomi',    '', '小米科技',              3, 1, NOW(), NOW(), 0),
(4, 'Huawei',    '', '华为技术',              4, 1, NOW(), NOW(), 0),
(5, 'Nike',      '', 'Nike Inc.',           5, 1, NOW(), NOW(), 0);
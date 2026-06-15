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

-- 菜单权限
INSERT IGNORE INTO sys_menu (id, menu_name, parent_id, order_num, path, perms, menu_type, status, create_time, update_time) VALUES
(1,  '系统管理',   0, 1, '/system',     NULL,                        'M', 1, NOW(), NOW()),
(2,  '用户管理',   1, 1, 'user',        'system:user:list',          'C', 1, NOW(), NOW()),
(3,  '角色管理',   1, 2, 'role',        'system:role:config',        'C', 1, NOW(), NOW()),
(10, '商品管理',   0, 2, '/product',    NULL,                        'M', 1, NOW(), NOW()),
(11, '分类管理',   10,1, 'category',    'product:category:config',   'C', 1, NOW(), NOW()),
(12, 'SPU列表',    10,2, 'spu',         'product:spu:list',          'C', 1, NOW(), NOW()),
(13, '商品添加',   10,3, NULL,          'product:spu:add',           'F', 1, NOW(), NOW()),
(14, '商品编辑',   10,4, NULL,          'product:spu:edit',          'F', 1, NOW(), NOW()),
(15, '商品删除',   10,5, NULL,          'product:spu:delete',        'F', 1, NOW(), NOW()),
(16, '商品上架',   10,6, NULL,          'product:spu:publish',       'F', 1, NOW(), NOW()),
(17, 'SKU列表',    10,7, NULL,          'product:sku:list',          'F', 1, NOW(), NOW()),
(18, 'SKU改价',    10,8, NULL,          'product:sku:price',         'F', 1, NOW(), NOW()),
(19, 'SKU库存',    10,9, NULL,          'product:sku:stock',         'F', 1, NOW(), NOW()),
(20, '品牌管理',   10,10,'brand',       'product:brand:config',      'C', 1, NOW(), NOW()),
(30, '订单管理',   0, 3, '/order',      NULL,                        'M', 1, NOW(), NOW()),
(31, '订单列表',   30,1, 'list',        'order:list',                'C', 1, NOW(), NOW()),
(32, '订单详情',   30,2, NULL,          'order:detail',              'F', 1, NOW(), NOW()),
(33, '退款处理',   30,3, 'refund',      'order:refund:process',      'C', 1, NOW(), NOW()),
(34, '退款审批',   30,4, NULL,          'order:refund:approve',      'F', 1, NOW(), NOW()),
(40, '会员管理',   0, 4, '/member',     NULL,                        'M', 1, NOW(), NOW()),
(41, '会员列表',   40,1, 'list',        'member:list',               'C', 1, NOW(), NOW()),
(42, '会员详情',   40,2, NULL,          'member:detail',             'F', 1, NOW(), NOW()),
(43, '积分调整',   40,3, NULL,          'member:points:adjust',      'F', 1, NOW(), NOW()),
(50, '营销管理',   0, 5, '/marketing',  NULL,                        'M', 1, NOW(), NOW()),
(51, '优惠券列表', 50,1, 'coupon',      'marketing:coupon:list',     'C', 1, NOW(), NOW()),
(52, '优惠券操作', 50,2, NULL,          'marketing:coupon:add',      'F', 1, NOW(), NOW()),
(53, '优惠券审核', 50,3, NULL,          'marketing:coupon:audit',    'F', 1, NOW(), NOW()),
(54, '活动配置',   50,4, 'activity',    'marketing:activity:config', 'C', 1, NOW(), NOW()),
(60, '财务管理',   0, 6, '/finance',    NULL,                        'M', 1, NOW(), NOW()),
(61, '对账列表',   60,1, 'statement',   'finance:statement:list',    'C', 1, NOW(), NOW()),
(62, '对账导出',   60,2, NULL,          'finance:statement:export',  'F', 1, NOW(), NOW()),
(63, '对账确认',   60,3, NULL,          'finance:statement:confirm', 'F', 1, NOW(), NOW()),
(64, '关税配置',   60,4, 'tax',         'finance:tax:config',        'C', 1, NOW(), NOW());

-- 店长拥有全部权限
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE perms IS NOT NULL AND perms != '';

-- 运营：商品+营销权限
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(2,10),(2,11),(2,12),(2,13),(2,14),(2,16),(2,17),(2,20),
(2,50),(2,51),(2,52),(2,54);

-- 客服：订单+退款+会员查看
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(3,30),(3,31),(3,32),(3,33),(3,40),(3,41),(3,42);

-- 财务：只看对账和关税
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(4,60),(4,61),(4,62),(4,63),(4,64);

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

-- ==================== 会员模块初始数据 ====================

-- 测试会员
INSERT IGNORE INTO mm_member VALUES
(1, 'mike@example.com',   '13900000001', '', 'Mike',   1, '', 0, 500,  1250.00, 1, NOW(), NOW(), 0),
(2, 'anna@example.com',   '13900000002', '', 'Anna',   0, '', 1, 1200, 3200.00, 1, NOW(), NOW(), 0),
(3, 'lucas@example.com',  '13900000003', '', 'Lucas',  1, '', 2, 3500, 8900.00, 1, NOW(), NOW(), 0);

-- 会员地址
INSERT IGNORE INTO mm_member_addr VALUES
(1, 1, 'Mike Wang',  '13900000001', 'USA', 'CA', 'San Francisco', '123 Main St',     '94101', 1, NOW(), NOW(), 0),
(2, 2, 'Anna Li',    '13900000002', 'UK',  '',   'London',        '456 Oxford St',   'W1D 1BS', 1, NOW(), NOW(), 0);

-- 积分记录
INSERT IGNORE INTO mm_member_points_log VALUES
(1, 1,  500,  '新用户注册赠送', NOW()),
(2, 1,  200,  '购买商品奖励',   NOW()),
(3, 1,  -100, '兑换优惠券',    NOW()),
(4, 2,  1200, '新用户注册赠送', NOW()),
(5, 3,  3500, '新用户注册赠送', NOW());

-- ==================== 营销模块初始数据 ====================

-- 优惠券
INSERT IGNORE INTO mk_coupon VALUES
(1, '新用户满减券',   'FULL_REDUCTION', 50.00, 10.00, 'USD', 1000, 0, 1, '2026-01-01', '2026-12-31', 'ALL',  '',    2, NOW(), NOW(), 0),
(2, '电子产品折扣券', 'DISCOUNT',        0.00,  0.15, 'USD', 500,  0, 1, '2026-01-01', '2026-12-31', 'CATEGORY', '1,2', 2, NOW(), NOW(), 0);

-- 营销活动
INSERT IGNORE INTO mk_activity VALUES
(1, '618年中大促', 'SECKILL', '2026-06-18 00:00:00', '2026-06-18 23:59:59', 0, NOW(), NOW(), 0);
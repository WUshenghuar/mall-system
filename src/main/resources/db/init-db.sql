-- ============================================================================
-- B2C 跨境电商后台管理系统 — 数据库初始化脚本
-- 数据库：mall_system  字符集：utf8mb4
-- 用法：mysql -u root -p < init-db.sql
-- ============================================================================

-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS mall_system
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE mall_system;

-- ============================================================================
-- 2. 系统管理模块（RBAC 权限）
-- ============================================================================

CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(100) NOT NULL,
    real_name   VARCHAR(50),
    phone       VARCHAR(20),
    email       VARCHAR(100),
    avatar      VARCHAR(255),
    status      TINYINT      DEFAULT 1,
    create_time DATETIME,
    update_time DATETIME,
    deleted     TINYINT      DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    role_name   VARCHAR(50)  NOT NULL,
    role_key    VARCHAR(50)  NOT NULL UNIQUE,
    role_sort   INT          DEFAULT 0,
    status      TINYINT      DEFAULT 1,
    remark      VARCHAR(255),
    create_time DATETIME,
    update_time DATETIME,
    deleted     TINYINT      DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_menu (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    menu_name   VARCHAR(50)  NOT NULL,
    parent_id   BIGINT       DEFAULT 0,
    order_num   INT          DEFAULT 0,
    path        VARCHAR(200),
    component   VARCHAR(255),
    perms       VARCHAR(100),
    icon        VARCHAR(50),
    menu_type   TINYINT      COMMENT 'M目录 C菜单 F按钮',
    visible     TINYINT      DEFAULT 1,
    status      TINYINT      DEFAULT 1,
    create_time DATETIME,
    update_time DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user_role (
    id      BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role_menu (
    id      BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    UNIQUE KEY uk_role_menu (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================================
-- 3. 商品模块
-- ============================================================================

CREATE TABLE IF NOT EXISTS pm_category (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    category_name VARCHAR(50)  NOT NULL,
    parent_id     BIGINT       DEFAULT 0,
    level         INT          DEFAULT 1,
    icon          VARCHAR(255),
    order_num     INT          DEFAULT 0,
    status        TINYINT      DEFAULT 1,
    create_time   DATETIME,
    update_time   DATETIME,
    deleted       TINYINT      DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pm_brand (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    brand_name  VARCHAR(50)  NOT NULL,
    brand_logo  VARCHAR(255),
    brand_desc  VARCHAR(500),
    order_num   INT          DEFAULT 0,
    status      TINYINT      DEFAULT 1,
    create_time DATETIME,
    update_time DATETIME,
    deleted     TINYINT      DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pm_spu (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    spu_code      VARCHAR(50)  NOT NULL UNIQUE,
    spu_name      VARCHAR(100) NOT NULL,
    category_id   BIGINT,
    brand_id      BIGINT,
    description   JSON             COMMENT '多语言描述: {"en":"...","zh":"..."}',
    customs_code  VARCHAR(20)      COMMENT 'HS Code',
    origin_country VARCHAR(50),
    status        TINYINT      DEFAULT 0 COMMENT '0草稿 1上架 2下架',
    sales_count   INT          DEFAULT 0,
    create_time   DATETIME,
    update_time   DATETIME,
    deleted       TINYINT      DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pm_sku (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    spu_id      BIGINT       NOT NULL,
    sku_code    VARCHAR(50)  NOT NULL UNIQUE,
    attrs       JSON             COMMENT '规格属性: [{"k":"颜色","v":"黑色"}]',
    price       DECIMAL(10,2),
    currency    VARCHAR(10)  DEFAULT 'USD',
    cost_price  DECIMAL(10,2),
    weight      DECIMAL(10,3)    COMMENT '重量(kg)',
    images      JSON             COMMENT 'SKU 图片列表',
    status      TINYINT      DEFAULT 1,
    create_time DATETIME,
    update_time DATETIME,
    deleted     TINYINT      DEFAULT 0,
    INDEX idx_spu_id (spu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pm_sku_stock (
    id           BIGINT      PRIMARY KEY AUTO_INCREMENT,
    sku_id       BIGINT      NOT NULL UNIQUE,
    warehouse_id BIGINT      DEFAULT 1,
    stock        INT         DEFAULT 0,
    locked_stock INT         DEFAULT 0 COMMENT '锁定库存（下单未支付）',
    create_time  DATETIME,
    update_time  DATETIME,
    INDEX idx_sku_id (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================================
-- 4. 订单模块
-- ============================================================================

CREATE TABLE IF NOT EXISTS om_order (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    order_no        VARCHAR(32)  NOT NULL UNIQUE,
    user_id         BIGINT,
    member_id       BIGINT,
    total_amount    DECIMAL(10,2),
    currency        VARCHAR(10)  DEFAULT 'USD',
    exchange_rate   DECIMAL(10,4) DEFAULT 1.0000,
    customs_declare DECIMAL(10,2) DEFAULT 0,
    tariff_amount   DECIMAL(10,2) DEFAULT 0,
    tariff_rate     DECIMAL(10,4) DEFAULT 0,
    shipping_fee    DECIMAL(10,2) DEFAULT 0,
    shipping_method VARCHAR(50),
    discount_amount DECIMAL(10,2) DEFAULT 0,
    pay_amount      DECIMAL(10,2),
    order_status    TINYINT      DEFAULT 0 COMMENT '0待支付 1已支付 2已发货 3已签收 4已完成 5已取消',
    pay_status      TINYINT      DEFAULT 0 COMMENT '0未支付 1已支付 2退款中 3已退款',
    logistics_status TINYINT     DEFAULT 0 COMMENT '0未发货 1已出关 2运输中 3已入关 4已签收',
    pay_time        DATETIME,
    delivery_time   DATETIME,
    receive_time    DATETIME,
    create_time     DATETIME,
    update_time     DATETIME,
    deleted         TINYINT      DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS om_order_item (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    order_id      BIGINT       NOT NULL,
    sku_id        BIGINT,
    spu_id        BIGINT,
    sku_name      VARCHAR(200),
    sku_attrs     VARCHAR(500),
    quantity      INT          DEFAULT 1,
    price         DECIMAL(10,2),
    total_price   DECIMAL(10,2),
    tariff_rate   DECIMAL(10,4) DEFAULT 0,
    tariff_amount DECIMAL(10,2) DEFAULT 0,
    create_time   DATETIME,
    update_time   DATETIME,
    deleted       TINYINT      DEFAULT 0,
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS om_order_pay (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    order_id    BIGINT       NOT NULL,
    order_no    VARCHAR(32)  NOT NULL,
    pay_no      VARCHAR(64)  NOT NULL,
    pay_amount  DECIMAL(10,2),
    currency    VARCHAR(10),
    pay_type    VARCHAR(20)  COMMENT 'STRIPE/PAYPAL/ALIPAY',
    pay_status  TINYINT      DEFAULT 0 COMMENT '0待支付 1成功 2失败',
    pay_time    DATETIME,
    create_time DATETIME,
    update_time DATETIME,
    INDEX idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS om_order_refund (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    order_id        BIGINT,
    order_no        VARCHAR(32),
    sku_id          BIGINT,
    quantity        INT,
    refund_amount   DECIMAL(10,2),
    currency        VARCHAR(10)  DEFAULT 'USD',
    refund_reason   VARCHAR(500),
    refund_type     TINYINT      COMMENT '0仅退款 1退货退款',
    refund_status   TINYINT      DEFAULT 0 COMMENT '0待审批 1已通过 2已驳回 3已完成',
    applicant_id    BIGINT,
    approver_id     BIGINT,
    approve_time    DATETIME,
    approve_comment VARCHAR(500),
    create_time     DATETIME,
    update_time     DATETIME,
    deleted         TINYINT      DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS om_order_logistics (
    id                 BIGINT       PRIMARY KEY AUTO_INCREMENT,
    order_id           BIGINT,
    tracking_no        VARCHAR(100),
    carrier            VARCHAR(50)  COMMENT 'DHL/FedEx/UPS/EMS',
    customs_declare_no VARCHAR(50)  COMMENT '报关单号',
    customs_status     TINYINT      DEFAULT 0 COMMENT '0未报关 1已报关 2清关中 3已清关',
    tracking_events    JSON         COMMENT '物流事件列表',
    origin_country     VARCHAR(50),
    dest_country       VARCHAR(50),
    create_time        DATETIME,
    update_time        DATETIME,
    deleted            TINYINT      DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================================
-- 5. 会员模块
-- ============================================================================

CREATE TABLE IF NOT EXISTS mm_member (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT,
    email        VARCHAR(100) UNIQUE,
    phone        VARCHAR(20),
    password     VARCHAR(100),
    nick_name    VARCHAR(50),
    gender       TINYINT      DEFAULT 0,
    avatar       VARCHAR(255),
    level        TINYINT      DEFAULT 0 COMMENT '0普通 1Gold 2Platinum',
    points       INT          DEFAULT 0,
    total_amount DECIMAL(12,2) DEFAULT 0 COMMENT '累计消费金额',
    status       TINYINT      DEFAULT 1,
    create_time  DATETIME,
    update_time  DATETIME,
    deleted      TINYINT      DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mm_member_addr (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    member_id     BIGINT       NOT NULL,
    receiver_name VARCHAR(50),
    phone         VARCHAR(20),
    country       VARCHAR(50),
    province      VARCHAR(50),
    city          VARCHAR(50),
    address       VARCHAR(200),
    postcode      VARCHAR(20),
    is_default    TINYINT      DEFAULT 0,
    create_time   DATETIME,
    update_time   DATETIME,
    deleted       TINYINT      DEFAULT 0,
    INDEX idx_member_id (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mm_member_points_log (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    member_id   BIGINT       NOT NULL,
    points      INT          COMMENT '变更积分(正为增加,负为减少)',
    reason      VARCHAR(255),
    create_time DATETIME,
    INDEX idx_member_id (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================================
-- 5. 营销模块
-- ============================================================================

CREATE TABLE IF NOT EXISTS mk_coupon (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT,
    coupon_name  VARCHAR(50)  NOT NULL,
    coupon_type  VARCHAR(20)  COMMENT 'FULL_REDUCTION/DISCOUNT/SHIPPING',
    threshold    DECIMAL(10,2) COMMENT '满减门槛',
    discount     DECIMAL(10,2) COMMENT '减免金额/折扣率',
    currency     VARCHAR(10)  DEFAULT 'USD',
    max_issue    INT          DEFAULT 0 COMMENT '发行总量',
    issued_count INT          DEFAULT 0 COMMENT '已发行数量',
    per_limit    INT          DEFAULT 1 COMMENT '每人限领',
    valid_start  DATETIME,
    valid_end    DATETIME,
    scope        VARCHAR(20)  COMMENT 'ALL/CATEGORY/SKU',
    scope_ids    VARCHAR(500) COMMENT '适用范围ID(逗号分隔)',
    status       TINYINT      DEFAULT 0 COMMENT '0草稿 1待审核 2已发布 3已结束',
    create_time  DATETIME,
    update_time  DATETIME,
    deleted      TINYINT      DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mk_coupon_issue (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    coupon_id   BIGINT       NOT NULL,
    member_id   BIGINT       NOT NULL,
    issue_time  DATETIME,
    used_time   DATETIME,
    status      TINYINT      DEFAULT 0 COMMENT '0未使用 1已使用 2已过期',
    order_no    VARCHAR(32),
    create_time DATETIME,
    update_time DATETIME,
    INDEX idx_coupon_member (coupon_id, member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mk_activity (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    activity_name VARCHAR(100) NOT NULL,
    activity_type VARCHAR(20)  COMMENT 'SECKILL/DISCOUNT/FULL_REDUCTION',
    start_time    DATETIME,
    end_time      DATETIME,
    status        TINYINT      DEFAULT 0 COMMENT '0未开始 1进行中 2已结束',
    create_time   DATETIME,
    update_time   DATETIME,
    deleted       TINYINT      DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mk_activity_sku (
    id             BIGINT       PRIMARY KEY AUTO_INCREMENT,
    activity_id    BIGINT       NOT NULL,
    sku_id         BIGINT       NOT NULL,
    seckill_price  DECIMAL(10,2),
    seckill_stock  INT          DEFAULT 0 COMMENT '秒杀库存',
    limit_per_user INT          DEFAULT 1 COMMENT '每人限购',
    create_time    DATETIME,
    update_time    DATETIME,
    INDEX idx_activity_sku (activity_id, sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================================
-- 6. 财务模块
-- ============================================================================

CREATE TABLE IF NOT EXISTS fn_statement (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    statement_no  VARCHAR(32)  NOT NULL UNIQUE,
    period_start  DATE,
    period_end    DATE,
    total_amount  DECIMAL(12,2) DEFAULT 0,
    tariff_amount DECIMAL(12,2) DEFAULT 0,
    shipping_fee  DECIMAL(12,2) DEFAULT 0,
    refund_amount DECIMAL(12,2) DEFAULT 0,
    net_amount    DECIMAL(12,2) DEFAULT 0,
    order_count   INT          DEFAULT 0,
    status        TINYINT      DEFAULT 0 COMMENT '0待确认 1已确认',
    create_time   DATETIME,
    update_time   DATETIME,
    deleted       TINYINT      DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS fn_statement_item (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    statement_id  BIGINT       NOT NULL,
    order_no      VARCHAR(32),
    total_amount  DECIMAL(10,2),
    tariff_amount DECIMAL(10,2),
    shipping_fee  DECIMAL(10,2),
    refund_amount DECIMAL(10,2) DEFAULT 0,
    pay_amount    DECIMAL(10,2),
    order_time    DATETIME,
    create_time   DATETIME,
    INDEX idx_statement_id (statement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS fn_tax_config (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    category_id     BIGINT,
    origin_country  VARCHAR(50),
    dest_country    VARCHAR(50),
    tax_rate        DECIMAL(10,4),
    tax_type        VARCHAR(20)  COMMENT 'DUTY/VAT/SALES_TAX',
    effective_date  DATE,
    expire_date     DATE,
    create_time     DATETIME,
    update_time     DATETIME,
    deleted         TINYINT      DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================================
-- 7. 初始数据
-- ============================================================================

-- 预置角色
INSERT IGNORE INTO sys_role VALUES
(1, '店长',     'store_manager',  1, 1, '全部权限',       NOW(), NOW(), 0),
(2, '运营专员', 'ops_specialist', 2, 1, '商品与活动运营', NOW(), NOW(), 0),
(3, '客服专员', 'cs_specialist',  3, 1, '订单与退款处理', NOW(), NOW(), 0),
(4, '财务专员', 'finance',        4, 1, '查看对账单',     NOW(), NOW(), 0);

-- 默认管理员账号（密码: admin123）
INSERT IGNORE INTO sys_user VALUES
(1, 'admin',   '$2a$10$eym7/7xeUDaruwSFuFFVuOx1SsjUK908k2730/c5XQ5E9B07vd6eW', '系统管理员', '13800000000', 'admin@mall.com',   NULL, 1, NOW(), NOW(), 0),
(2, 'ops',     '$2a$10$eym7/7xeUDaruwSFuFFVuOx1SsjUK908k2730/c5XQ5E9B07vd6eW', '运营专员',   '13800000001', 'ops@mall.com',     NULL, 1, NOW(), NOW(), 0),
(3, 'cs',      '$2a$10$eym7/7xeUDaruwSFuFFVuOx1SsjUK908k2730/c5XQ5E9B07vd6eW', '客服专员',   '13800000002', 'cs@mall.com',      NULL, 1, NOW(), NOW(), 0),
(4, 'finance', '$2a$10$eym7/7xeUDaruwSFuFFVuOx1SsjUK908k2730/c5XQ5E9B07vd6eW', '财务专员',   '13800000003', 'finance@mall.com', NULL, 1, NOW(), NOW(), 0);

-- 分配角色
INSERT IGNORE INTO sys_user_role VALUES
(1, 1, 1), (2, 2, 2), (3, 3, 3), (4, 4, 4);

-- 菜单权限
INSERT IGNORE INTO sys_menu (id, menu_name, parent_id, order_num, path, perms, menu_type, status, create_time, update_time) VALUES
(1, '系统管理',0,1,'/system',NULL,'M',1,NOW(),NOW()),
(2, '用户管理',1,1,'user','system:user:list','C',1,NOW(),NOW()),
(3, '角色管理',1,2,'role','system:role:config','C',1,NOW(),NOW()),
(10,'商品管理',0,2,'/product',NULL,'M',1,NOW(),NOW()),
(11,'分类管理',10,1,'category','product:category:config','C',1,NOW(),NOW()),
(12,'SPU列表',10,2,'spu','product:spu:list','C',1,NOW(),NOW()),
(13,'商品添加',10,3,NULL,'product:spu:add','F',1,NOW(),NOW()),
(14,'商品编辑',10,4,NULL,'product:spu:edit','F',1,NOW(),NOW()),
(15,'商品删除',10,5,NULL,'product:spu:delete','F',1,NOW(),NOW()),
(16,'商品上架',10,6,NULL,'product:spu:publish','F',1,NOW(),NOW()),
(17,'SKU列表',10,7,NULL,'product:sku:list','F',1,NOW(),NOW()),
(18,'SKU改价',10,8,NULL,'product:sku:price','F',1,NOW(),NOW()),
(19,'SKU库存',10,9,NULL,'product:sku:stock','F',1,NOW(),NOW()),
(20,'品牌管理',10,10,'brand','product:brand:config','C',1,NOW(),NOW()),
(30,'订单管理',0,3,'/order',NULL,'M',1,NOW(),NOW()),
(31,'订单列表',30,1,'list','order:list','C',1,NOW(),NOW()),
(32,'订单详情',30,2,NULL,'order:detail','F',1,NOW(),NOW()),
(33,'退款处理',30,3,'refund','order:refund:process','C',1,NOW(),NOW()),
(34,'退款审批',30,4,NULL,'order:refund:approve','F',1,NOW(),NOW()),
(40,'会员管理',0,4,'/member',NULL,'M',1,NOW(),NOW()),
(41,'会员列表',40,1,'list','member:list','C',1,NOW(),NOW()),
(42,'会员详情',40,2,NULL,'member:detail','F',1,NOW(),NOW()),
(43,'积分调整',40,3,NULL,'member:points:adjust','F',1,NOW(),NOW()),
(50,'营销管理',0,5,'/marketing',NULL,'M',1,NOW(),NOW()),
(51,'优惠券列表',50,1,'coupon','marketing:coupon:list','C',1,NOW(),NOW()),
(52,'优惠券操作',50,2,NULL,'marketing:coupon:add','F',1,NOW(),NOW()),
(53,'优惠券审核',50,3,NULL,'marketing:coupon:audit','F',1,NOW(),NOW()),
(54,'活动配置',50,4,'activity','marketing:activity:config','C',1,NOW(),NOW()),
(60,'财务管理',0,6,'/finance',NULL,'M',1,NOW(),NOW()),
(61,'对账列表',60,1,'statement','finance:statement:list','C',1,NOW(),NOW()),
(62,'对账导出',60,2,NULL,'finance:statement:export','F',1,NOW(),NOW()),
(63,'对账确认',60,3,NULL,'finance:statement:confirm','F',1,NOW(),NOW()),
(64,'关税配置',60,4,'tax','finance:tax:config','C',1,NOW(),NOW());

-- 店长拥有全部权限
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT 1, id FROM sys_menu WHERE perms IS NOT NULL AND perms != '';
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(2,10),(2,11),(2,12),(2,13),(2,14),(2,16),(2,17),(2,20),(2,50),(2,51),(2,52),(2,54),
(3,30),(3,31),(3,32),(3,33),(3,40),(3,41),(3,42),
(4,60),(4,61),(4,62),(4,63),(4,64);

-- 商品分类
INSERT IGNORE INTO pm_category VALUES
(1, '电子产品', 0, 1, 'LaptopOutlined',  1, 1, NOW(), NOW(), 0),
(2, '手机平板', 1, 2, 'MobileOutlined',  1, 1, NOW(), NOW(), 0),
(3, '电脑配件', 1, 2, 'DesktopOutlined', 2, 1, NOW(), NOW(), 0),
(4, '服饰鞋包', 0, 1, 'ShoppingOutlined',2, 1, NOW(), NOW(), 0),
(5, '女装',     4, 2, 'SkinOutlined',    1, 1, NOW(), NOW(), 0),
(6, '男装',     4, 2, 'UserOutlined',    2, 1, NOW(), NOW(), 0),
(7, '家居用品', 0, 1, 'HomeOutlined',    3, 1, NOW(), NOW(), 0),
(8, '美妆个护', 0, 1, 'SmileOutlined',   4, 1, NOW(), NOW(), 0);

-- 品牌
INSERT IGNORE INTO pm_brand VALUES
(1, 'Apple',   '', 'Apple Inc.',           1, 1, NOW(), NOW(), 0),
(2, 'Samsung', '', 'Samsung Electronics',  2, 1, NOW(), NOW(), 0),
(3, 'Xiaomi',  '', '小米科技',             3, 1, NOW(), NOW(), 0),
(4, 'Huawei',  '', '华为技术',             4, 1, NOW(), NOW(), 0),
(5, 'Nike',    '', 'Nike Inc.',            5, 1, NOW(), NOW(), 0);

-- 测试会员
INSERT IGNORE INTO mm_member VALUES
(1, 'mike@example.com',  '13900000001', '', 'Mike',  1, '', 0, 500,  1250.00, 1, NOW(), NOW(), 0),
(2, 'anna@example.com',  '13900000002', '', 'Anna',  0, '', 1, 1200, 3200.00, 1, NOW(), NOW(), 0),
(3, 'lucas@example.com', '13900000003', '', 'Lucas', 1, '', 2, 3500, 8900.00, 1, NOW(), NOW(), 0);

-- 会员地址
INSERT IGNORE INTO mm_member_addr VALUES
(1, 1, 'Mike Wang', '13900000001', 'USA', 'CA', 'San Francisco', '123 Main St',    '94101',   1, NOW(), NOW(), 0),
(2, 2, 'Anna Li',   '13900000002', 'UK',  '',   'London',        '456 Oxford St',  'W1D 1BS', 1, NOW(), NOW(), 0);

-- 积分记录
INSERT IGNORE INTO mm_member_points_log VALUES
(1, 1,  500,  '新用户注册赠送', NOW()),
(2, 1,  200,  '购买商品奖励',   NOW()),
(3, 1,  -100, '兑换优惠券',    NOW()),
(4, 2,  1200, '新用户注册赠送', NOW()),
(5, 3,  3500, '新用户注册赠送', NOW());

-- ==================== 营销模块 ====================

-- 优惠券
INSERT IGNORE INTO mk_coupon VALUES
(1, '新用户满减券',   'FULL_REDUCTION', 50.00, 10.00, 'USD', 1000, 0, 1, '2026-01-01', '2026-12-31', 'ALL',  '',    2, NOW(), NOW(), 0),
(2, '电子产品折扣券', 'DISCOUNT',        0.00,  0.15, 'USD', 500,  0, 1, '2026-01-01', '2026-12-31', 'CATEGORY', '1,2', 2, NOW(), NOW(), 0);

-- 营销活动
INSERT IGNORE INTO mk_activity VALUES
(1, '618年中大促', 'SECKILL', '2026-06-18 00:00:00', '2026-06-18 23:59:59', 0, NOW(), NOW(), 0);

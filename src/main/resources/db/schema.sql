-- 系统用户表
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

-- 角色表
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

-- 菜单/权限表
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

-- 用户-角色关联
CREATE TABLE IF NOT EXISTS sys_user_role (
    id      BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 角色-菜单关联
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id      BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    UNIQUE KEY uk_role_menu (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== 商品模块 ====================

-- 商品分类表
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

-- 品牌表
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

-- 商品 SPU 表
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

-- 商品 SKU 表
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

-- SKU 库存表（DB持久化，Redis为热缓存）
CREATE TABLE IF NOT EXISTS pm_sku_stock (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    sku_id      BIGINT       NOT NULL UNIQUE,
    warehouse_id BIGINT      DEFAULT 1,
    stock       INT          DEFAULT 0,
    locked_stock INT         DEFAULT 0 COMMENT '锁定库存（下单未支付）',
    create_time DATETIME,
    update_time DATETIME,
    INDEX idx_sku_id (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== 订单模块 ====================

-- 订单表
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

-- 订单明细表
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

-- 支付记录表
CREATE TABLE IF NOT EXISTS om_order_pay (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    order_id      BIGINT       NOT NULL,
    order_no      VARCHAR(32)  NOT NULL,
    pay_no        VARCHAR(64)  NOT NULL,
    pay_amount    DECIMAL(10,2),
    currency      VARCHAR(10),
    pay_type      VARCHAR(20)  COMMENT 'STRIPE/PAYPAL/ALIPAY',
    pay_status    TINYINT      DEFAULT 0 COMMENT '0待支付 1成功 2失败',
    pay_time      DATETIME,
    create_time   DATETIME,
    update_time   DATETIME,
    INDEX idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 退款表
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

-- 物流表
CREATE TABLE IF NOT EXISTS om_order_logistics (
    id                BIGINT       PRIMARY KEY AUTO_INCREMENT,
    order_id          BIGINT,
    tracking_no       VARCHAR(100),
    carrier           VARCHAR(50)  COMMENT 'DHL/FedEx/UPS/EMS',
    customs_declare_no VARCHAR(50) COMMENT '报关单号',
    customs_status    TINYINT      DEFAULT 0 COMMENT '0未报关 1已报关 2清关中 3已清关',
    tracking_events   JSON         COMMENT '物流事件列表',
    origin_country    VARCHAR(50),
    dest_country      VARCHAR(50),
    create_time       DATETIME,
    update_time       DATETIME,
    deleted           TINYINT      DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== 会员模块 ====================

-- 会员表
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

-- 会员地址表
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

-- 积分变更日志表
CREATE TABLE IF NOT EXISTS mm_member_points_log (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    member_id   BIGINT       NOT NULL,
    points      INT          COMMENT '变更积分(正为增加,负为减少)',
    reason      VARCHAR(255),
    create_time DATETIME,
    INDEX idx_member_id (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== 营销模块 ====================

-- 优惠券表
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

-- 优惠券发放记录表
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

-- 营销活动表
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

-- 活动关联商品表
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

-- ==================== 财务模块 ====================

-- 对账单表
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

-- 对账单明细表
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

-- 关税配置表
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
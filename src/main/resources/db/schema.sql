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
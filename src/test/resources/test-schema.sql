-- Test schema for H2 (MySQL compatibility mode)
-- Only tables needed for service tests

CREATE TABLE IF NOT EXISTS pm_spu (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    spu_code      VARCHAR(50)  NOT NULL UNIQUE,
    spu_name      VARCHAR(100) NOT NULL,
    category_id   BIGINT,
    brand_id      BIGINT,
    description   TEXT,
    customs_code  VARCHAR(20),
    origin_country VARCHAR(50),
    status        TINYINT      DEFAULT 0,
    sales_count   INT          DEFAULT 0,
    create_time   DATETIME,
    update_time   DATETIME,
    deleted       TINYINT      DEFAULT 0
);

CREATE TABLE IF NOT EXISTS pm_sku (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    spu_id      BIGINT       NOT NULL,
    sku_code    VARCHAR(50)  NOT NULL UNIQUE,
    attrs       TEXT,
    price       DECIMAL(10,2),
    currency    VARCHAR(10)  DEFAULT 'USD',
    cost_price  DECIMAL(10,2),
    weight      DECIMAL(10,3),
    images      TEXT,
    status      TINYINT      DEFAULT 1,
    create_time DATETIME,
    update_time DATETIME,
    deleted     TINYINT      DEFAULT 0
);

CREATE TABLE IF NOT EXISTS pm_sku_stock (
    id           BIGINT      PRIMARY KEY AUTO_INCREMENT,
    sku_id       BIGINT      NOT NULL UNIQUE,
    warehouse_id BIGINT      DEFAULT 1,
    stock        INT         DEFAULT 0,
    locked_stock INT         DEFAULT 0,
    create_time  DATETIME,
    update_time  DATETIME
);

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
    order_status    TINYINT      DEFAULT 0,
    pay_status      TINYINT      DEFAULT 0,
    logistics_status TINYINT     DEFAULT 0,
    pay_time        DATETIME,
    delivery_time   DATETIME,
    receive_time    DATETIME,
    create_time     DATETIME,
    update_time     DATETIME,
    deleted         TINYINT      DEFAULT 0
);

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
    deleted       TINYINT      DEFAULT 0
);

CREATE TABLE IF NOT EXISTS om_order_refund (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    order_id        BIGINT,
    order_no        VARCHAR(32),
    sku_id          BIGINT,
    quantity        INT,
    refund_amount   DECIMAL(10,2),
    currency        VARCHAR(10)  DEFAULT 'USD',
    refund_reason   VARCHAR(500),
    refund_type     TINYINT,
    refund_status   TINYINT      DEFAULT 0,
    applicant_id    BIGINT,
    approver_id     BIGINT,
    approve_time    DATETIME,
    approve_comment VARCHAR(500),
    create_time     DATETIME,
    update_time     DATETIME,
    deleted         TINYINT      DEFAULT 0
);

CREATE TABLE IF NOT EXISTS fn_tax_config (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    category_id   BIGINT,
    origin_country VARCHAR(50),
    dest_country  VARCHAR(50),
    tax_rate      DECIMAL(5,2) NOT NULL,
    tax_type      VARCHAR(20)  DEFAULT 'VAT',
    effective_date DATE        NOT NULL,
    expire_date   DATE,
    create_time   DATETIME,
    update_time   DATETIME,
    deleted       TINYINT      DEFAULT 0
);

CREATE TABLE IF NOT EXISTS mk_coupon (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    coupon_name VARCHAR(100) NOT NULL,
    coupon_type VARCHAR(20)  DEFAULT 'FULL_REDUCTION',
    threshold   DECIMAL(10,2),
    discount    DECIMAL(10,2),
    currency    VARCHAR(10)  DEFAULT 'USD',
    max_issue   INT          DEFAULT 0,
    issued_count INT         DEFAULT 0,
    per_limit   INT          DEFAULT 1,
    valid_start DATETIME,
    valid_end   DATETIME,
    scope       VARCHAR(20)  DEFAULT 'ALL',
    scope_ids   VARCHAR(500),
    status      TINYINT      DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME,
    deleted     TINYINT      DEFAULT 0
);

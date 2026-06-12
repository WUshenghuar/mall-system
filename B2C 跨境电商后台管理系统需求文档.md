# B2C 跨境电商后台管理系统 — 需求设计文档

> **版本:** v1.0
> **日期:** 2026-05-28
> **状态:** 待实现
> **技术栈:** Java 23 + Spring Boot 3+ + Spring Security + JWT + MyBatis-Plus 3.5 + MySQL 8 + Redis 7 + RabbitMQ + Elasticsearch 8 + Vue 3 (Composition API) + Ant Design Vue + Maven

---

## 1. 项目概述

### 1.1 项目定位

面向跨境电商企业的多角色运营管理后台，覆盖商品、订单、会员、营销、财务全链路运营管理。支持多币种交易、关税计算、国际物流追踪、多语言商品描述等跨境电商核心特性。

### 1.2 目标用户与角色

| 角色                     | 职责      | 权限范围                       |
| ---------------------- | ------- | -------------------------- |
| **店长 (Store Manager)** | 全局运营管理  | 全部数据 + 系统配置 + 审批退款/活动      |
| **运营专员 (Operations)**  | 商品运营与活动 | 商品上下架、分类配置、营销活动（无权删除商品、调价） |
| **客服专员 (CS)**          | 订单服务    | 查看订单、处理退款、更新物流（无权修改商品）     |
| **财务专员 (Finance)**     | 资金对账    | 只读查看对账单、导出报表               |

### 1.3 核心模块

| 模块 | 说明 |
|------|------|
| **商品管理** | SPU/SKU 管理、多级分类、品牌、多语言描述、多币种定价、海关 HS Code |
| **订单流转** | 订单全生命周期（下单→支付→发货→签收→完成）、退款流程、国际物流追踪 |
| **会员管理** | 会员信息、等级体系、积分体系、国际地址管理 |
| **营销优惠券** | 优惠券定义/发放/核销、活动配置、秒杀场景 |
| **财务对账** | 自动对账单、关税配置、汇率管理、Excel 导出 |

---

## 2. 技术架构

### 2.1 架构分层

```
┌─────────────────────────────────────────────────────┐
│              Vue 3 + Ant Design Vue SPA                │
│            (动态路由 + 权限菜单 + 按钮级控制)          │
├─────────────────────────────────────────────────────┤
│                  Nginx 反向代理/负载均衡               │
├─────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────┐   │
│  │          Spring Boot REST API (mall-web)      │   │
│  │  ┌──────────┐ ┌─────────┐ ┌───────────┐    │   │
│  │  │ mall-    │ │ mall-   │ │ mall-     │    │   │
│  │  │ product  │ │ order   │ │ member    │    │   │
│  │  ├──────────┤ ├─────────┤ ├───────────┤    │   │
│  │  │ mall-    │ │ mall-   │ │ mall-     │    │   │
│  │  │ marketing│ │ finance │ │ search    │    │   │
│  │  └──────────┘ └─────────┘ └───────────┘    │   │
│  ├──────────────────────────────────────────────┤   │
│  │  mall-security (JWT + RBAC + @DataScope)     │   │
│  └──────────────────────────────────────────────┘   │
├──────────┬──────────┬───────────┬───────────────────┤
│  MySQL   │  Redis   │ RabbitMQ  │ Elasticsearch     │
│ (主从+读) │ (缓存/锁) │ (异步削峰) │ (商品搜索)        │
└──────────┴──────────┴───────────┴───────────────────┘
```

### 2.2 技术栈详表

| 层次        | 技术                    | 版本             | 用途                |
| --------- | --------------------- | -------------- | ----------------- |
| **后端框架**  | Spring Boot           | 2.7.x          | 业务框架              |
| **安全认证**  | Spring Security + JWT | 2.7.x / 0.12.x | RBAC 权限 + 无状态认证   |
| **ORM**   | MyBatis-Plus          | 3.5.x          | 数据持久化 + 自动填充 + 分页 |
| **关系数据库** | MySQL                 | 8.0 (主从)       | 核心业务数据            |
| **缓存**    | Redis                 | 7.x            | 商品缓存/分布式锁/购物车     |
| **消息队列**  | RabbitMQ              | 3.x            | 异步订单处理/库存削峰       |
| **搜索引擎**  | Elasticsearch         | 8.x            | 商品全文检索            |
| **文件存储**  | MinIO                 | -              | 商品图片/报关单附件        |
| **前端**    | Vue 3 + Ant Design Vue  | -              | 管理后台 UI           |
| **构建工具**  | Maven                 | 3.8+           | 多模块构建管理           |

### 2.3 项目模块划分 (Maven 多模块)

```
mall-system/
├── mall-common/                    # 公共模块
│   ├── entity/                     # BaseEntity 基类
│   ├── result/                     # 统一返回结果 (Result/ResultCode)
│   ├── exception/                  # 全局异常处理 + BusinessException
│   ├── config/                     # MyBatis-Plus 配置 (分页/自动填充)
│   └── utils/                      # 工具类
├── mall-security/                  # 安全模块
│   ├── jwt/                        # JWT 生成/解析/过滤器
│   ├── config/                     # Spring Security 配置
│   ├── annotation/                 # @DataScope 数据权限注解
│   └── aspect/                     # 数据权限 AOP 切面
├── mall-product/                   # 商品模块
│   ├── controller/                 # SpuController, SkuController, CategoryController, BrandController
│   ├── service/                    # SPU/SKU/分类/品牌/库存服务
│   ├── mapper/                     # MyBatis-Plus Mapper
│   └── entity/                     # Spu, Sku, Category, Brand, SkuStock
├── mall-order/                     # 订单模块
│   ├── controller/                 # OrderController, RefundController
│   ├── service/                    # 订单服务/退款服务/物流服务
│   ├── mapper/
│   ├── entity/                     # Order, OrderItem, OrderPay, OrderRefund
│   └── mq/                         # RabbitMQ 消息监听 (订单消费者)
├── mall-member/                    # 会员模块
│   ├── controller/                 # MemberController
│   ├── service/                    # 会员服务/积分服务
│   ├── mapper/
│   └── entity/                     # Member, MemberAddr, MemberPointsLog
├── mall-marketing/                 # 营销模块
│   ├── controller/                 # CouponController, ActivityController
│   ├── service/                    # 优惠券服务/活动服务/秒杀服务
│   ├── mapper/
│   └── entity/                     # Coupon, CouponIssue, Activity, ActivitySku
├── mall-finance/                   # 财务模块
│   ├── controller/                 # StatementController, TaxConfigController
│   ├── service/                    # 对账服务/汇率服务
│   ├── mapper/
│   └── entity/                     # Statement, StatementItem, TaxConfig
├── mall-search/                    # 搜索模块
│   ├── service/                    # ES 索引服务/搜索服务
│   ├── job/                        # 定时同步任务
│   └── repository/                 # ES Repository
├── mall-web/                       # Web 启动层 (统一入口)
│   ├── OaApplication.java          # 启动类
│   ├── controller/                 # 各模块 Controller (统一放在此)
│   └── resources/
│       ├── application.yml
│       ├── application-dev.yml
│       ├── mapper/                 # MyBatis-Plus XML 映射
│       └── schema.sql / data.sql   # 数据库初始化 SQL
├── pom.xml                         # 父 POM
```

---

## 3. 数据库设计

### 3.1 表结构总览

#### 系统权限模块 (5 表)

```
sys_user      → 系统用户表 (关联员工)
sys_role      → 角色表
sys_menu      → 菜单/权限表 (三级: 目录/菜单/按钮)
sys_user_role → 用户-角色关联
sys_role_menu → 角色-菜单关联
```

#### 商品模块 (6 表)

```
pm_category     → 商品分类 (支持多级树形)
pm_brand        → 品牌表
pm_spu          → SPU (标准化产品单元)
pm_spu_images   → SPU 图片/多语言描述
pm_sku          → SKU (库存量单位)
pm_sku_stock    → SKU 库存 (支持多仓库)
```

#### 订单模块 (6 表)

```
om_order          → 订单主表
om_order_item     → 订单明细
om_order_pay      → 支付记录
om_order_refund   → 退款记录
om_order_logistics → 物流信息
om_order_log      → 订单操作日志
```

#### 会员模块 (3 表)

```
mm_member          → 会员表
mm_member_addr     → 收货地址 (国际地址)
mm_member_points_log → 积分变动日志
```

#### 营销模块 (4 表)

```
mk_coupon          → 优惠券定义
mk_coupon_issue    → 优惠券发放
mk_activity        → 活动 (秒杀/满减)
mk_activity_sku    → 活动参与商品
```

#### 财务模块 (3 表)

```
fn_statement       → 对账单
fn_statement_item  → 对账单明细
fn_tax_config      → 关税税率配置
```

### 3.2 核心表结构

#### 商品 SPU 表 (pm_spu)

```sql
CREATE TABLE pm_spu (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    spu_code        VARCHAR(32)     NOT NULL UNIQUE,     -- SPU 编码
    spu_name        VARCHAR(200)    NOT NULL,             -- SPU 名称
    category_id     BIGINT          NOT NULL,             -- 分类 ID
    brand_id        BIGINT,                               -- 品牌 ID
    -- 多语言描述 (JSON: {"en":"...","zh":"...","fr":"..."})
    description     JSON,
    -- 跨境字段
    customs_code    VARCHAR(20),                          -- HS Code (海关编码，如 8517.12.00)
    origin_country  VARCHAR(50),                          -- 原产国
    declared_value  DECIMAL(12,2),                        -- 报关单价
    -- 状态
    status          TINYINT         DEFAULT 0,             -- 0草稿 1上架 2下架
    sales_count     INT             DEFAULT 0,             -- 销量
    rating          DECIMAL(2,1)    DEFAULT 0,             -- 评分
    create_time     DATETIME,
    update_time     DATETIME,
    deleted         TINYINT         DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 商品 SKU 表 (pm_sku)

```sql
CREATE TABLE pm_sku (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    spu_id          BIGINT          NOT NULL,              -- 所属 SPU
    sku_code        VARCHAR(32)     NOT NULL UNIQUE,       -- SKU 编码 (如条形码)
    attrs           JSON,                                 -- 规格属性: [{"k":"颜色","v":"黑色"},{"k":"容量","v":"128GB"}]
    price           DECIMAL(12,2)   NOT NULL,              -- 售价 (USD)
    currency        VARCHAR(3)      DEFAULT 'USD',         -- 定价币种
    cost_price      DECIMAL(12,2),                         -- 成本价
    weight          DECIMAL(10,2),                         -- 重量 (kg, 用于运费计算)
    images          VARCHAR(1000),                         -- SKU 图片 (逗号分隔)
    status          TINYINT         DEFAULT 1,             -- 0禁用 1启用
    create_time     DATETIME,
    update_time     DATETIME,
    deleted         TINYINT         DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 订单主表 (om_order) — 跨境电商特色

```sql
CREATE TABLE om_order (
    id                BIGINT          PRIMARY KEY AUTO_INCREMENT,
    order_no          VARCHAR(32)     NOT NULL UNIQUE,     -- 订单号 (分布式ID)
    user_id           BIGINT          NOT NULL,            -- 后台操作用户
    member_id         BIGINT,                              -- 会员ID
    -- 金额体系 (多币种)
    total_amount      DECIMAL(12,2)   NOT NULL,            -- 商品总金额 (本币)
    currency          VARCHAR(3)      DEFAULT 'USD',       -- 交易币种
    exchange_rate     DECIMAL(10,6),                       -- 下单时冻结汇率
    -- 跨境费用
    customs_declare   DECIMAL(12,2)   DEFAULT 0,           -- 报关金额
    tariff_amount     DECIMAL(12,2)   DEFAULT 0,           -- 关税金额
    tariff_rate       DECIMAL(5,2)    DEFAULT 0,           -- 适用关税率
    shipping_fee      DECIMAL(10,2)   DEFAULT 0,           -- 国际运费
    shipping_method   VARCHAR(50),                         -- 运输方式 (空运/海运/快递)
    discount_amount   DECIMAL(12,2)   DEFAULT 0,           -- 优惠抵扣
    pay_amount        DECIMAL(12,2),                       -- 实付金额 = total + tariff + shipping - discount
    -- 状态
    order_status      TINYINT         DEFAULT 0,            -- 0待支付 1已支付 2已发货 3已签收 4已完成 5已取消
    pay_status        TINYINT         DEFAULT 0,            -- 0未支付 1已支付 2退款中 3已退款
    logistics_status  TINYINT         DEFAULT 0,            -- 0未发货 1已出关 2运输中 3已入关 4已签收
    -- 时间
    pay_time          DATETIME,
    delivery_time     DATETIME,
    receive_time      DATETIME,
    create_time       DATETIME,
    update_time       DATETIME,
    deleted           TINYINT         DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 索引
CREATE INDEX idx_order_no ON om_order(order_no);
CREATE INDEX idx_member_id ON om_order(member_id);
CREATE INDEX idx_order_status ON om_order(order_status);
CREATE INDEX idx_create_time ON om_order(create_time);
```

#### 订单物流表 (om_order_logistics)

```sql
CREATE TABLE om_order_logistics (
    id                BIGINT          PRIMARY KEY AUTO_INCREMENT,
    order_id          BIGINT          NOT NULL,
    tracking_no       VARCHAR(64),                         -- 物流运单号
    carrier           VARCHAR(50),                         -- 物流商 (DHL/FedEx/UPS/EMS)
    -- 海关信息
    customs_declare_no VARCHAR(64),                        -- 海关报关单号
    customs_status    TINYINT         DEFAULT 0,            -- 0未报关 1已报关 2清关中 3已清关
    -- 物流节点 (JSON 存储轨迹)
    tracking_events   JSON,                               -- [{"time":"...","location":"...","desc":"..."}]
    origin_country    VARCHAR(50),                         -- 发件国
    dest_country      VARCHAR(50),                         -- 目的国
    create_time       DATETIME,
    update_time       DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 财务对账单表 (fn_statement)

```sql
CREATE TABLE fn_statement (
    id                BIGINT          PRIMARY KEY AUTO_INCREMENT,
    statement_no      VARCHAR(32)     NOT NULL UNIQUE,     -- 对账单号
    period_start      DATE            NOT NULL,            -- 周期开始
    period_end        DATE            NOT NULL,            -- 周期结束
    currency          VARCHAR(3)      DEFAULT 'USD',       -- 结算币种
    total_trade_amount DECIMAL(14,2)  DEFAULT 0,           -- 总交易额
    total_refund      DECIMAL(14,2)   DEFAULT 0,           -- 总退款
    total_tariff      DECIMAL(14,2)   DEFAULT 0,           -- 总关税
    total_shipping    DECIMAL(14,2)   DEFAULT 0,           -- 总运费
    net_amount        DECIMAL(14,2)   DEFAULT 0,           -- 净结算 = 交易 - 退款 + 关税
    order_count       INT             DEFAULT 0,           -- 订单数
    status            TINYINT         DEFAULT 0,            -- 0待确认 1已确认 2已结算
    confirmed_by      BIGINT,                              -- 确认人
    confirm_time      DATETIME,
    create_time       DATETIME,
    update_time       DATETIME,
    deleted           TINYINT         DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 关税税率配置表 (fn_tax_config)

```sql
CREATE TABLE fn_tax_config (
    id                BIGINT          PRIMARY KEY AUTO_INCREMENT,
    category_id       BIGINT,                              -- 商品分类 (NULL 表示全局默认)
    origin_country    VARCHAR(50),                         -- 原产国
    dest_country      VARCHAR(50),                         -- 目的国
    tax_rate          DECIMAL(5,2)    NOT NULL,            -- 税率 (%)
    tax_type          VARCHAR(20)     DEFAULT 'VAT',       -- 税种: VAT / GST / Duty
    effective_date    DATE            NOT NULL,            -- 生效日期
    expire_date       DATE,                                -- 失效日期
    create_time       DATETIME,
    update_time       DATETIME,
    deleted           TINYINT         DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 4. RBAC 权限矩阵

### 4.1 角色定义

| 角色 Key | 角色名 | 说明 |
|----------|-------|------|
| `store_manager` | 店长 | 全部权限，等同超级管理员 |
| `ops_specialist` | 运营专员 | 商品管理 + 营销配置（无权删除/调价） |
| `cs_specialist` | 客服专员 | 订单查看 + 退款处理（无权修改商品） |
| `finance` | 财务专员 | 对账单只读 + 导出 |

### 4.2 权限矩阵

权限点命名规范：`module:resource:action`

| 权限点 | 店长 | 运营 | 客服 | 财务 |
|--------|:----:|:----:|:----:|:----:|
| `*:*:*` | ✅ | ❌ | ❌ | ❌ |
| **商品模块** | | | | |
| `product:spu:list` | ✅ | ✅ | ❌ | ❌ |
| `product:spu:add` | ✅ | ✅ | ❌ | ❌ |
| `product:spu:edit` | ✅ | ✅ | ❌ | ❌ |
| `product:spu:delete` | ✅ | ❌ | ❌ | ❌ |
| `product:spu:publish` | ✅ | ✅ | ❌ | ❌ |
| `product:sku:list` | ✅ | ✅ | ❌ | ❌ |
| `product:sku:price` | ✅ | ❌ | ❌ | ❌ |
| `product:sku:stock` | ✅ | ✅ | ❌ | ❌ |
| `product:category:config` | ✅ | ✅ | ❌ | ❌ |
| `product:brand:config` | ✅ | ✅ | ❌ | ❌ |
| **订单模块** | | | | |
| `order:list` | ✅ | ❌ | ✅ | ❌ |
| `order:detail` | ✅ | ❌ | ✅ | ❌ |
| `order:logistics:edit` | ✅ | ❌ | ✅ | ❌ |
| `order:refund:process` | ✅ | ❌ | ✅ | ❌ |
| `order:refund:approve` | ✅ | ❌ | ❌ | ❌ |
| **会员模块** | | | | |
| `member:list` | ✅ | ❌ | ✅ | ❌ |
| `member:detail` | ✅ | ❌ | ✅ | ❌ |
| `member:points:adjust` | ✅ | ❌ | ❌ | ❌ |
| **营销模块** | | | | |
| `marketing:coupon:list` | ✅ | ✅ | ❌ | ❌ |
| `marketing:coupon:add` | ✅ | ✅ | ❌ | ❌ |
| `marketing:coupon:audit` | ✅ | ❌ | ❌ | ❌ |
| `marketing:activity:config` | ✅ | ✅ | ❌ | ❌ |
| **财务模块** | | | | |
| `finance:statement:list` | ✅ | ❌ | ❌ | ✅ |
| `finance:statement:export` | ✅ | ❌ | ❌ | ✅ |
| `finance:statement:confirm` | ✅ | ❌ | ❌ | ❌ |
| `finance:tax:config` | ✅ | ❌ | ❌ | ❌ |
| **系统管理** | | | | |
| `system:user:list` | ✅ | ❌ | ❌ | ❌ |
| `system:user:add` | ✅ | ❌ | ❌ | ❌ |
| `system:user:edit` | ✅ | ❌ | ❌ | ❌ |
| `system:role:config` | ✅ | ❌ | ❌ | ❌ |
| `system:menu:config` | ✅ | ❌ | ❌ | ❌ |

### 4.3 权限实现方式

- **后端:** Spring Security `@PreAuthorize("hasAuthority('xxx')")` + JWT Token 鉴权
- **数据权限:** `@DataScope(deptAlias = "o")` AOP 切面 + MyBatis-Plus 拦截器自动拼装 SQL 条件
- **前端:** 路由守卫根据 `permissions` 动态过滤菜单 + 按钮级 `v-permission` 指令

---

## 5. 高并发架构设计

### 5.1 Redis 缓存策略

| 场景 | 数据结构 | Key 示例 | TTL | 说明 |
|------|---------|---------|-----|------|
| **商品详情** | String (JSON) | `product:spu:{id}` | 30min | 手动失效 |
| **SKU 库存** | String | `stock:sku:{id}` | 持久 | 下单预扣 |
| **用户购物车** | Hash | `cart:user:{id}` | 7天 | field=skuId, value=qty |
| **分类树** | String | `category:tree` | 1h | 变更时清除 |
| **分布式锁** | String (NX/EX) | `lock:order:create:{skuId}` | 10s | 防超卖 |

### 5.2 RabbitMQ 异步消息

| 队列 | 用途 | 消费者 | 说明 |
|------|------|--------|------|
| `order.create` | 下单异步处理 | 扣库存 → 生成订单 → 积分累计 | 削峰，避免数据库被打穿 |
| `order.paid` | 支付成功 | 更新订单状态 → 通知发货 | 解耦支付回调 |
| `order.refund` | 退款处理 | 发起退款 → 退库存 | 异步审核流程 |
| `search.sync` | ES 索引同步 | 商品索引增量更新 | 通过 Canal 监听 MySQL binlog |
| `logistics.update` | 物流状态 | 更新物流跟踪信息 | 对接第三方物流 API |

### 5.3 防超卖方案 (Redis + MQ)

```
用户下单
  │
  ├─ ① Redis 分布式锁 (lock:sku:{id}, NX EX 10)
  ├─ ② Redis DECR 预扣库存 (stock:sku:{id})
  ├─ ③ 若库存 < 0，回滚并返回 "库存不足"
  ├─ ④ 发送 MQ `order.create` 消息
  └─ ⑤ 立即返回 "订单处理中"

  MQ Consumer 异步处理:
  ├─ ⑥ 消费 `order.create`
  ├─ ⑦ INSERT om_order + om_order_item (事务)
  ├─ ⑧ UPDATE pm_sku_stock 实际库存 (乐观锁)
  ├─ ⑨ 计算关税
  ├─ ⑩ 生成支付记录
  └─ ⑪ 发送支付通知
```

### 5.4 Elasticsearch 商品搜索

索引映射:

```json
{
  "mall_product": {
    "properties": {
      "spuId":        { "type": "long" },
      "spuName":      { "type": "text", "analyzer": "ik_smart" },
      "categoryPath": { "type": "keyword" },
      "brand":        { "type": "keyword" },
      "minPrice":     { "type": "double" },
      "currency":     { "type": "keyword" },
      "attrs":        { "type": "nested" },
      "description":  { "type": "text", "analyzer": "ik_smart" },
      "salesCount":   { "type": "long" },
      "rating":       { "type": "double" },
      "status":       { "type": "byte" }
    }
  }
}
```

搜索路径: `前端搜索 → ES 检索 → 根据 spuId 回 MySQL 补全 → Redis 缓存结果`

### 5.5 MySQL 主从读写分离

```
写请求 (增删改) → Master
读请求 (查询)   → Slave1 / Slave2 (轮询)

实现: AbstractRoutingDataSource + AOP @DataSource("slave") 注解
```

---

## 6. API 接口概览

### 6.1 接口列表

| 模块 | 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|------|
| **认证** | POST | `/api/auth/login` | 公开 | 登录获取 Token |
| | GET | `/api/auth/userinfo` | 登录 | 获取用户信息+权限 |
| **商品-SPU** | GET | `/api/product/spu/page` | `product:spu:list` | SPU 分页查询 |
| | POST | `/api/product/spu` | `product:spu:add` | 新增 SPU |
| | PUT | `/api/product/spu/{id}` | `product:spu:edit` | 编辑 SPU |
| | DELETE | `/api/product/spu/{id}` | `product:spu:delete` | 删除 SPU |
| | PUT | `/api/product/spu/{id}/publish` | `product:spu:publish` | 上下架 |
| **商品-SKU** | GET | `/api/product/sku/list/{spuId}` | `product:sku:list` | SKU 列表 |
| | POST | `/api/product/sku` | `product:spu:edit` | 新增 SKU |
| | PUT | `/api/product/sku/price` | `product:sku:price` | 批量调价 |
| | PUT | `/api/product/sku/stock` | `product:sku:stock` | 更新库存 |
| **商品-分类** | GET | `/api/product/category/tree` | `product:category:config` | 分类树 |
| | POST | `/api/product/category` | `product:category:config` | 新增分类 |
| **订单** | GET | `/api/order/page` | `order:list` | 订单分页 |
| | GET | `/api/order/{id}` | `order:detail` | 订单详情 |
| | POST | `/api/order/create` | 须登录 | 创建订单 (异步) |
| | PUT | `/api/order/{id}/logistics` | `order:logistics:edit` | 更新物流 |
| **退款** | POST | `/api/order/refund` | `order:refund:process` | 发起退款 |
| | PUT | `/api/order/refund/{id}/approve` | `order:refund:approve` | 审批退款 |
| **会员** | GET | `/api/member/page` | `member:list` | 会员分页 |
| | GET | `/api/member/{id}` | `member:detail` | 会员详情 |
| **优惠券** | GET | `/api/marketing/coupon/page` | `marketing:coupon:list` | 优惠券分页 |
| | POST | `/api/marketing/coupon` | `marketing:coupon:add` | 新增优惠券 |
| | PUT | `/api/marketing/coupon/{id}/audit` | `marketing:coupon:audit` | 审核优惠券 |
| **活动** | GET | `/api/marketing/activity/page` | `marketing:activity:config` | 活动列表 |
| | POST | `/api/marketing/activity` | `marketing:activity:config` | 新增活动 |
| **对账单** | GET | `/api/finance/statement/page` | `finance:statement:list` | 对账单分页 |
| | GET | `/api/finance/statement/{id}` | `finance:statement:list` | 对账明细 |
| | GET | `/api/finance/statement/{id}/export` | `finance:statement:export` | 导出 Excel |
| | PUT | `/api/finance/statement/{id}/confirm` | `finance:statement:confirm` | 确认对账 |
| **关税** | GET | `/api/finance/tax/page` | `finance:tax:config` | 税率列表 |
| | POST | `/api/finance/tax` | `finance:tax:config` | 新增税率 |
| **系统** | GET | `/api/system/user/page` | `system:user:list` | 用户分页 |
| | GET | `/api/system/role/list` | `system:role:config` | 角色列表 |
| | PUT | `/api/system/role/{id}/menu` | `system:role:config` | 分配菜单权限 |

---

## 7. 前端设计

### 7.1 路由结构

```
/login                                          → 登录页
/dashboard                                      → 首页统计看板

/product                                        → 商品管理 (菜单)
  /product/category                             → 分类管理
  /product/brand                                → 品牌管理
  /product/spu                                  → SPU 列表
  /product/spu/add                              → 新增 SPU
  /product/spu/:id                              → SPU 编辑
  /product/sku/:spuId                           → SKU 管理

/order                                          → 订单管理
  /order/list                                   → 订单列表
  /order/:id                                    → 订单详情
  /order/refund                                 → 退款处理

/member                                         → 会员管理
  /member/list                                  → 会员列表
  /member/:id                                   → 会员详情

/marketing                                      → 营销管理
  /marketing/coupon                             → 优惠券管理
  /marketing/activity                           → 活动管理
  /marketing/seckill                            → 秒杀配置

/finance                                        → 财务管理
  /finance/statement                            → 对账单
  /finance/statement/:id                        → 对账明细
  /finance/tax                                  → 关税配置

/system                                         → 系统管理 (仅店长)
  /system/user                                  → 用户管理
  /system/role                                  → 角色管理
  /system/menu                                  → 菜单权限
```

### 7.2 权限动态路由

- **路由守卫:** 在 `router.beforeEach` 中，根据 `store.user.permissions` 过滤可访问路由
- **按钮指令:** `<el-button v-permission="'product:spu:add'">新增SPU</el-button>`
- **侧边栏:** 只渲染有权限的菜单项，无权限自动隐藏

### 7.3 页面布局 (AdminLayout)

```
┌─────────────────────────────────────────────────┐
│  Logo           顶栏 (角色/用户/通知/退出)        │
├──────────┬──────────────────────────────────────┤
│          │  面包屑                               │
│  侧边栏   ├──────────────────────────────────────┤
│  (权限    │                                      │
│   菜单)   │    <router-view />                    │
│          │    (keep-alive 缓存列表页)              │
│          │                                      │
└──────────┴──────────────────────────────────────┘
```

---

## 8. 项目实施计划

### 8.1 分阶段实施

| 阶段 | 内容 | 预估工期 | 关键产出 |
|------|------|---------|---------|
| **Phase 1** | 项目脚手架搭建 (Maven 多模块 + Vue 初始化) | 0.5天 | 可编译项目结构 |
| **Phase 2** | 基础设施层 (统一返回/异常/基础实体/MyBatis-Plus配置) | 0.5天 | common 模块 |
| **Phase 3** | RBAC 权限体系 (Spring Security/JWT/权限表/登录) | 1天 | 认证授权完成 |
| **Phase 4** | 商品模块 (分类/品牌/SPU/SKU/Redis 缓存) | 2天 | 商品 CRUD 完整 |
| **Phase 5** | ES 搜索引擎集成 (索引创建/同步/搜索 API) | 1天 | 商品搜索可用 |
| **Phase 6** | 订单模块 (MVC/MQ 异步/分布式锁/退款流程) | 2天 | 订单全流程 |
| **Phase 7** | 会员模块 (CRUD/积分/地址) | 0.5天 | 会员管理 |
| **Phase 8** | 营销模块 (优惠券/活动/秒杀) | 1.5天 | 营销功能 |
| **Phase 9** | 财务模块 (对账单/关税/汇率/EasyExcel) | 1天 | 财务对账 |
| **Phase 10** | 前端路由 + 权限菜单 + 核心页面 | 2天 | 完整 UI |
| **Phase 11** | Docker 部署 + 集成测试 | 0.5天 | 容器化部署 |

**总预估:** ~12.5 人天 (约 3 周，单人开发)

### 8.2 技术亮点 (简历适用)

| 亮点 | 具体实现 | 面试描述 |
|------|---------|---------|
| 🔐 **RBAC 权限体系** | Spring Security + JWT + `@PreAuthorize` + 动态路由 | "按钮级权限控制，路由根据后端返回权限点动态渲染" |
| ⚡ **高并发库存扣减** | Redis 预扣 + 分布式锁 + MQ 异步削峰 | "Redis Lua 脚本原子扣减 + MQ 异步下单，TPS 提升约 10 倍" |
| 🔍 **多语言搜索引擎** | Elasticsearch 8 + IK 分词 | "商品中英文混合搜索、分类聚合、价格排序，近实时索引同步" |
| 🌐 **跨境电商** | 多币种体系 + 关税计算 + HS Code | "支持多币种交易，自动计算各国关税，汇率冻结在订单发生时" |
| 📊 **财务对账** | Quartz 定时任务 + EasyExcel | "自动生成周期对账单，支持 Excel 多语言导出，汇率冻结防波动" |
| 📦 **全容器化部署** | Docker Compose (MySQL+Redis+MQ+ES+App) | "一键部署开发/生产环境，适用于 CI/CD" |

---

## 9. 补充说明

### 9.1 数据权限设计

- **店长:** 无数据范围限制，可查看全部数据
- **运营专员:** 仅可操作自己负责的商品分类
- **客服专员:** 仅可查看和操作自己处理的订单 (通过 `@DataScope` 注解)
- **财务专员:** 可查看全部对账单 (只读)
- **实现:** 自定义 `@DataScope` 注解 + AOP 切面 + MyBatis-Plus InnerInterceptor 自动拼装 SQL

### 9.2 关税计算流程

```
下单时:
  ① 遍历订单中每个 SKU 的商品分类
  ② 根据分类 + 原产国 + 目的国 查询 fn_tax_config
  ③ 若无精确匹配，使用该分类默认税率
  ④ tariff = Σ(单价 × 数量 × 税率%)
  ⑤ 写入 om_order.tariff_amount + om_order.tariff_rate
```

### 9.3 多语言商品描述

SPU 的 `description` 字段使用 JSON 格式存储多语言内容:

```json
{
  "en": "iPhone 15 Pro Max features a...",
  "zh": "iPhone 15 Pro Max 搭载...",
  "fr": "L'iPhone 15 Pro Max est doté d'un..."
}
```

- 前端根据系统语言或用户偏好选择展示语言
- ES 索引中分别建立 `description_en`, `description_zh` 等字段

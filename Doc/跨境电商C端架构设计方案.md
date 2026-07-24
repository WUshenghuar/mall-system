# 跨境电商全栈系统 - C端架构设计方案

> 在现有B端后台管理系统基础上，补充C端用户前台功能
> 实现完整的电商业务闭环：用户浏览 → 购物车 → 下单 → 支付 → 物流 → 售后

---

## 一、整体架构设计

### 1.1 系统架构图

```
┌─────────────────────────────────────────────────────────────┐
│                    前端层（Vue 3）                            │
├──────────────────────┬──────────────────────────────────────┤
│   C端用户前台         │   B端运营后台（已有）                  │
│   ├── 商品浏览        │   ├── 商品管理                        │
│   ├── 购物车          │   ├── 订单管理                        │
│   ├── 下单结算        │   ├── 会员管理                        │
│   ├── 支付中心        │   ├── 营销活动                        │
│   ├── 物流追踪        │   ├── 财务报表                        │
│   ├── 个人中心        │   └── AI客服管理                      │
│   └── AI客服          │                                      │
└──────────┬───────────┴──────────────────────┬───────────────┘
           │                                  │
           │  REST + SSE                      │  REST
           ▼                                  ▼
┌─────────────────────────────────────────────────────────────┐
│              Spring Boot 3.2.5（业务层）                     │
├──────────────────────┬──────────────────────────────────────┤
│ mall-web（C端API）    │ mall-admin（B端API，已有）             │
│ mall-member          │ mall-system（系统管理，已有）           │
│ mall-order           │ mall-product（商品管理，已有）          │
│ mall-product         │ mall-ai（AI集成，已有）                 │
│ mall-ai（AI集成）     │                                      │
│ mall-trade（交易中心）│                                      │
│ mall-security        │                                      │
└──────────┬───────────┴──────────────────────┬───────────────┘
           │                                  │
           │                                  │
           ▼                                  ▼
┌─────────────────────────────────────────────────────────────┐
│                   中间件层                                     │
├─────────────────────────────────────────────────────────────┤
│  MySQL 8.0  │  Redis 7  │  RabbitMQ 3  │  ES 8  │  MinIO   │
│  (主数据)    │  (缓存)   │  (消息队列)  │ (搜索)  │ (存储)   │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 模块划分

```
mall-system/
├── mall-common/                    # 公共模块（已有）
├── mall-security/                  # 安全认证（已有）
├── mall-system/                    # 系统管理（已有）
├── mall-product/                   # 商品服务（已有）
├── mall-order/                     # 订单服务（已有）
├── mall-member/                    # 会员服务（已有）
├── mall-search/                    # 搜索服务（已有）
├── mall-ai/                        # AI服务（已有）
├── mall-web/                       # C端API（新增）
├── mall-trade/                     # 交易中心（新增）
├── mall-logistics/                 # 物流服务（新增）
└── mall-admin/                     # B端管理（已有）
```

---

## 二、C端模块详细设计

### 2.1 模块职责

| 模块 | 职责 | 核心API |
|------|------|---------|
| **mall-web** | C端入口，聚合调用 | 所有C端接口 |
| **mall-trade** | 交易中心，购物车/结算 | /api/trade/* |
| **mall-logistics** | 物流追踪，状态流转 | /api/logistics/* |
| **mall-member** | 用户中心，地址/积分/收藏 | /api/member/* |
| **mall-product** | 商品服务（已扩展C端查询） | /api/product/* |
| **mall-order** | 订单服务（已扩展支付/物流） | /api/order/* |

### 2.2 模块依赖关系

```
mall-web（C端入口）
    ├── mall-trade（交易中心）
    │   ├── mall-product（商品查询/库存）
    │   ├── mall-member（用户信息/地址）
    │   └── mall-order（订单创建）
    ├── mall-logistics（物流追踪）
    │   └── mall-order（订单状态更新）
    └── mall-ai（AI客服）
        ├── mall-product（商品查询）
        └── mall-order（订单查询）
```

---

## 三、数据库设计

### 3.1 C端核心表设计

```sql
-- ============================================
-- 1. 购物车表
-- ============================================
CREATE TABLE trade_cart (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,             -- 用户ID
    sku_id          BIGINT NOT NULL,             -- SKU ID
    quantity        INT NOT NULL DEFAULT 1,       -- 数量
    checked         TINYINT DEFAULT 1,            -- 是否选中 1是 0否
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    UNIQUE INDEX uk_user_sku (user_id, sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 2. 收货地址表
-- ============================================
CREATE TABLE member_address (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    receiver_name   VARCHAR(50) NOT NULL,         -- 收货人姓名
    receiver_phone  VARCHAR(20) NOT NULL,         -- 收货人电话
    province        VARCHAR(50),                  -- 省
    city            VARCHAR(50),                  -- 市
    district        VARCHAR(50),                  -- 区
    detail_address  VARCHAR(200),                 -- 详细地址
    is_default      TINYINT DEFAULT 0,            -- 是否默认
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0,
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 3. 订单表（扩展）
-- ============================================
CREATE TABLE trade_order (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no            VARCHAR(64) NOT NULL,     -- 订单号（雪花算法）
    user_id             BIGINT NOT NULL,
    order_status        TINYINT NOT NULL,         -- 订单状态
    total_amount        DECIMAL(10,2) NOT NULL,   -- 总金额
    discount_amount     DECIMAL(10,2) DEFAULT 0,  -- 优惠金额
    freight_amount      DECIMAL(10,2) DEFAULT 0,  -- 运费
    pay_amount          DECIMAL(10,2) NOT NULL,   -- 实付金额
    pay_type            TINYINT,                  -- 支付方式 1支付宝 2微信
    pay_time            DATETIME,                 -- 支付时间
    delivery_time       DATETIME,                 -- 发货时间
    receive_time        DATETIME,                 -- 收货时间
    receiver_name       VARCHAR(50),              -- 收货人
    receiver_phone      VARCHAR(20),              -- 收货电话
    receiver_address    VARCHAR(500),             -- 收货地址
    remark              VARCHAR(500),             -- 订单备注
    source_type         TINYINT DEFAULT 1,        -- 订单来源 1APP 2H5 3小程序
    create_time         DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT DEFAULT 0,
    INDEX idx_user (user_id),
    INDEX idx_order_no (order_no),
    INDEX idx_status (order_status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 4. 订单商品快照表
-- ============================================
CREATE TABLE trade_order_item (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id        BIGINT NOT NULL,
    order_no        VARCHAR(64) NOT NULL,
    sku_id          BIGINT NOT NULL,
    sku_name        VARCHAR(200),                -- 商品名称（快照）
    sku_price       DECIMAL(10,2),               -- 商品单价（快照）
    quantity        INT NOT NULL,                -- 数量
    total_amount    DECIMAL(10,2),               -- 小计
    sku_image       VARCHAR(500),                -- 商品图片（快照）
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order (order_id),
    INDEX idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 5. 支付记录表
-- ============================================
CREATE TABLE trade_pay (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no        VARCHAR(64) NOT NULL,
    pay_no          VARCHAR(64),                 -- 支付流水号
    pay_type        TINYINT NOT NULL,            -- 支付方式
    pay_amount      DECIMAL(10,2) NOT NULL,      -- 支付金额
    pay_status      TINYINT DEFAULT 0,           -- 0待支付 1支付成功 2支付失败
    pay_time        DATETIME,                    -- 支付时间
    callback_time   DATETIME,                    -- 回调时间
    callback_content TEXT,                       -- 回调内容
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order (order_no),
    INDEX idx_pay_no (pay_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 6. 物流信息表
-- ============================================
CREATE TABLE trade_logistics (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no        VARCHAR(64) NOT NULL,
    logistics_no    VARCHAR(64),                 -- 物流单号
    logistics_company VARCHAR(50),               -- 物流公司
    logistics_status TINYINT DEFAULT 0,          -- 物流状态
    logistics_info  TEXT,                        -- 物流轨迹（JSON）
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 7. 商品收藏表
-- ============================================
CREATE TABLE member_favorite (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    spu_id          BIGINT NOT NULL,             -- 商品SPU ID
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    UNIQUE INDEX uk_user_spu (user_id, spu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 8. 商品浏览记录表
-- ============================================
CREATE TABLE member_browse_history (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    spu_id          BIGINT NOT NULL,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_time (user_id, create_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 3.2 订单状态流转

```
状态枚举：
0: 待支付 (WAIT_PAY)
1: 待发货 (WAIT_DELIVER)
2: 待收货 (WAIT_RECEIVE)
3: 已完成 (COMPLETED)
4: 已取消 (CANCELLED)
5: 退款中 (REFUNDING)
6: 已退款 (REFUNDED)

状态流转图：
                    ┌────────────┐
                    │  创建订单   │
                    └─────┬──────┘
                          │
                          ▼
                    ┌────────────┐
           ┌───────│  待支付     │───────┐
           │       └─────┬──────┘       │
           │             │              │
           │ 超时取消     │ 支付成功      │ 用户取消
           ▼             ▼              ▼
    ┌────────────┐ ┌────────────┐ ┌────────────┐
    │  已取消     │ │  待发货     │ │  退款中     │
    └────────────┘ └─────┬──────┘ └─────┬──────┘
                         │              │
                         │ 发货         │ 退款成功
                         ▼              ▼
                  ┌────────────┐ ┌────────────┐
                  │  待收货     │ │  已退款     │
                  └─────┬──────┘ └────────────┘
                        │
                        │ 确认收货
                        ▼
                  ┌────────────┐
                  │  已完成     │
                  └────────────┘
```

---

## 四、核心业务流程

### 4.1 购物车流程

```
用户操作流程：
1. 添加商品到购物车
   ├── 校验商品是否存在
   ├── 校验SKU是否有效
   ├── 校验库存是否充足
   └── 更新或插入购物车表

2. 查看购物车
   ├── 查询购物车列表
   ├── 关联查询商品信息（名称、价格、图片）
   └── 计算总价

3. 修改购物车
   ├── 修改数量（校验库存）
   ├── 选中/取消选中
   └── 删除商品

4. 去结算
   ├── 获取选中的商品
   ├── 获取用户地址列表
   ├── 计算运费
   ├── 计算优惠（优惠券）
   └── 返回结算信息
```

### 4.2 下单流程

```
创建订单流程（事务性）：

1. 参数校验
   ├── 校验购物车商品是否选中
   ├── 校验收货地址是否存在
   └── 校验优惠券是否有效

2. 库存校验与扣减（Redis Lua原子操作）
   ├── 逐个SKU校验库存
   ├── Redis预扣库存
   └── 校验失败则回滚

3. 价格计算
   ├── 商品总价 = ∑(SKU单价 × 数量)
   ├── 运费计算（满免/按重量/按地区）
   ├── 优惠券抵扣
   └── 实付金额 = 商品总价 + 运费 - 优惠

4. 生成订单
   ├── 生成订单号（雪花算法）
   ├── 插入trade_order表
   ├── 插入trade_order_item表（商品快照）
   ├── 扣减库存（MQ异步）
   └── 清空购物车（已选中的）

5. 发送消息
   ├── 订单创建成功消息
   └── 启动支付超时定时任务（30分钟）
```

### 4.3 支付流程

```
支付流程：

1. 创建支付单
   ├── 校验订单状态（必须是待支付）
   ├── 生成支付单号
   ├── 调用支付SDK（支付宝/微信沙箱）
   └── 返回支付参数给前端

2. 前端调起支付
   ├── 支付宝：调用支付宝SDK
   └── 微信：调用微信JSAPI

3. 支付回调
   ├── 验证签名（防伪造）
   ├── 更新支付单状态
   ├── 更新订单状态（待支付 → 待发货）
   ├── 发送支付成功消息
   └── 通知各个服务（库存实际扣减、积分增加等）

4. 支付超时处理
   ├── 定时任务扫描30分钟未支付订单
   ├── 自动取消订单
   └── 回滚Redis库存
```

### 4.4 物流流程

```
物流流程：

1. 商家发货（后台操作）
   ├── 填写物流单号
   ├── 选择物流公司
   ├── 更新订单状态（待发货 → 待收货）
   └── 初始化物流信息表

2. 物流信息推送（模拟）
   ├── 定时任务模拟物流轨迹
   ├── 更新物流信息表（JSON格式存储轨迹）
   └── 推送状态变更消息

3. 用户确认收货
   ├── 校验订单状态（必须是待收货）
   ├── 更新订单状态（待收货 → 已完成）
   ├── 增加用户积分
   └── 发送确认收货消息

4. 自动确认收货
   ├── 定时任务扫描15天未确认收货的订单
   └── 自动确认收货
```

---

## 五、API设计

### 5.1 C端核心API

```yaml
# ==================== 购物车 API ====================
POST   /api/trade/cart                 # 添加到购物车
GET    /api/trade/cart                 # 查询购物车
PUT    /api/trade/cart/{id}            # 更新购物车（数量/选中）
DELETE /api/trade/cart/{id}            # 删除购物车商品
DELETE /api/trade/cart/batch           # 批量删除
GET    /api/trade/cart/count           # 购物车数量

# ==================== 结算 API ====================
GET    /api/trade/settle               # 获取结算信息
POST   /api/trade/settle/check         # 校验结算参数

# ==================== 订单 API ====================
POST   /api/trade/order                # 创建订单
GET    /api/trade/order/list           # 订单列表
GET    /api/trade/order/{orderNo}      # 订单详情
POST   /api/trade/order/{orderNo}/cancel  # 取消订单
POST   /api/trade/order/{orderNo}/confirm  # 确认收货
GET    /api/trade/order/{orderNo}/logistics  # 物流信息

# ==================== 支付 API ====================
POST   /api/trade/pay/create           # 创建支付
GET    /api/trade/pay/status/{orderNo}  # 查询支付状态
POST   /api/trade/pay/notify/alipay    # 支付宝回调
POST   /api/trade/pay/notify/wechat    # 微信回调

# ==================== 地址 API ====================
GET    /api/member/address             # 地址列表
POST   /api/member/address             # 新增地址
PUT    /api/member/address/{id}        # 修改地址
DELETE /api/member/address/{id}        # 删除地址
PUT    /api/member/address/{id}/default  # 设为默认

# ==================== 商品 API（C端） ====================
GET    /api/product/spu/{id}           # 商品详情
GET    /api/product/sku/{id}           # SKU详情
GET    /api/product/spu/list           # 商品列表
POST   /api/member/favorite            # 收藏商品
DELETE /api/member/favorite/{spuId}    # 取消收藏
GET    /api/member/favorite/list       # 收藏列表
POST   /api/member/browse              # 浏览记录
GET    /api/member/browse/list         # 浏览历史
```

### 5.2 请求/响应示例

```json
// 添加到购物车
// POST /api/trade/cart
{
    "skuId": 123456,
    "quantity": 2
}

// 响应
{
    "code": 200,
    "message": "success",
    "data": {
        "cartId": 789,
        "quantity": 2,
        "totalQuantity": 5  // 购物车总数量
    }
}

// ====================

// 创建订单
// POST /api/trade/order
{
    "addressId": 123,
    "couponId": 456,        // 可选
    "remark": "请尽快发货",
    "items": [
        {"skuId": 111, "quantity": 2},
        {"skuId": 222, "quantity": 1}
    ]
}

// 响应
{
    "code": 200,
    "message": "success",
    "data": {
        "orderNo": "20260722000001",
        "payAmount": 299.00,
        "expireTime": "2026-07-22T12:00:00"  // 30分钟超时
    }
}
```

---

## 六、高并发场景设计

### 6.1 库存扣减方案

```
三层防护策略：

Layer 1: Redis Lua 预扣（毫秒级）
├── 原子操作：判断库存 + 扣减库存
├── Lua脚本保证原子性
└── 失败直接返回，不打到DB

Layer 2: MQ 异步（削峰）
├── 订单创建成功后发送MQ消息
├── 消费端批量扣减数据库库存
└── 控制消费速率

Layer 3: DB 乐观锁（兜底）
├── UPDATE SET stock = stock - #{quantity}
│   WHERE sku_id = #{skuId} AND stock >= #{quantity}
├── CAS语义保证不超卖
└── 失败重试或回滚Redis
```

**Redis Lua脚本示例**：
```lua
-- KEYS[1]: 库存key (stock:sku:{skuId})
-- ARGV[1]: 扣减数量

local stock = tonumber(redis.call('get', KEYS[1]))
if stock == nil then
    return -1  -- 库存不存在
end

if stock < tonumber(ARGV[1]) then
    return 0   -- 库存不足
end

redis.call('decrby', KEYS[1], ARGV[1])
return 1       -- 扣减成功
```

### 6.2 秒杀活动设计

```
秒杀架构：

1. 前端
   ├── 按钮倒计时（活动开始前禁用）
   ├── 按钮防抖（点击后禁用60秒）
   └── 随机延迟提交（防止瞬间并发）

2. Nginx层
   ├── IP限流（每秒最多1次请求）
   ├── 用户限频（同一用户最多1次/秒）
   └── 黑名单（封禁恶意IP）

3. Redis层
   ├── Lua预扣库存（原子操作）
   ├── 内存标记（本地缓存库存状态）
   └── 库存为0直接返回，不打到DB

4. MQ层
   ├── 秒杀请求进入MQ队列
   ├── 消费端限流消费
   └── 超出库存的请求直接丢弃

5. DB层
   ├── 乐观锁扣库存
   ├── 订单创建
   └── 失败回滚
```

### 6.3 商品详情页缓存策略

```
多级缓存架构：

L1: 浏览器缓存
├── 静态资源CDN
├── Cache-Control: max-age=300
└── 减少网络请求

L2: Nginx缓存
├── 缓存热点商品详情
├── 缓存时间5分钟
└── 减少后端压力

L3: Redis缓存
├── 缓存商品基本信息（hash结构）
├── 缓存库存信息（string结构）
├── 缓存时间30分钟
└── 主动刷新 + 被动过期

L4: DB查询
├── 查询MySQL
└── 回写Redis缓存
```

---

## 七、代码结构设计

### 7.1 mall-web 模块结构

```
mall-web/
├── pom.xml
└── src/main/java/com/mall/web/
    ├── MallWebApplication.java
    ├── config/
    │   ├── WebConfig.java
    │   └── CartConfig.java
    ├── controller/
    │   ├── cart/
    │   │   └── CartController.java
    │   ├── order/
    │   │   └── OrderController.java
    │   ├── pay/
    │   │   └── PayController.java
    │   ├── member/
    │   │   ├── AddressController.java
    │   │   ├── FavoriteController.java
    │   │   └── BrowseController.java
    │   └── product/
    │       └── ProductController.java
    ├── service/
    │   ├── CartService.java
    │   ├── OrderService.java
    │   ├── PayService.java
    │   ├── AddressService.java
    │   └── impl/
    │       ├── CartServiceImpl.java
    │       ├── OrderServiceImpl.java
    │       ├── PayServiceImpl.java
    │       └── AddressServiceImpl.java
    ├── dto/
    │   ├── CartDTO.java
    │   ├── OrderCreateDTO.java
    │   ├── SettleInfoDTO.java
    │   └── OrderDetailVO.java
    └── vo/
        ├── CartVO.java
        ├── OrderVO.java
        └── LogisticsVO.java
```

### 7.2 mall-trade 模块结构

```
mall-trade/
├── pom.xml
└── src/main/java/com/mall/trade/
    ├── MallTradeApplication.java
    ├── config/
    │   ├── TradeConfig.java
    │   └── PayConfig.java
    ├── entity/
    │   ├── TradeCart.java
    │   ├── TradeOrder.java
    │   ├── TradeOrderItem.java
    │   ├── TradePay.java
    │   └── TradeLogistics.java
    ├── mapper/
    │   ├── TradeCartMapper.java
    │   ├── TradeOrderMapper.java
    │   ├── TradeOrderItemMapper.java
    │   ├── TradePayMapper.java
    │   └── TradeLogisticsMapper.java
    ├── service/
    │   ├── CartService.java
    │   ├── SettleService.java
    │   ├── OrderService.java
    │   ├── PayService.java
    │   ├── StockService.java
    │   └── impl/
    │       ├── CartServiceImpl.java
    │       ├── SettleServiceImpl.java
    │       ├── OrderServiceImpl.java
    │       ├── PayServiceImpl.java
    │       └── StockServiceImpl.java
    ├── listener/
    │   ├── OrderCreateListener.java
    │   ├── PaySuccessListener.java
    │   └── OrderTimeoutListener.java
    └── job/
        ├── OrderTimeoutJob.java
        └── AutoConfirmJob.java
```

---

## 八、与AI客服系统的集成

### 8.1 Function Calling 扩展

```python
# 新增C端相关的Function Calling工具

tools = [
    # 已有的B端工具
    {"name": "query_order", ...},
    {"name": "query_product", ...},
    {"name": "check_return_eligibility", ...},

    # 新增C端工具
    {
        "name": "query_user_orders",
        "description": "查询用户的订单列表",
        "parameters": {
            "order_status": {"type": "integer", "description": "订单状态"},
            "page": {"type": "integer", "default": 1}
        }
    },
    {
        "name": "query_cart",
        "description": "查询用户的购物车",
        "parameters": {}
    },
    {
        "name": "query_logistics",
        "description": "查询订单物流信息",
        "parameters": {
            "order_no": {"type": "string", "description": "订单号"}
        }
    },
    {
        "name": "calculate_shipping",
        "description": "计算运费",
        "parameters": {
            "address_id": {"type": "integer", "description": "收货地址ID"},
            "sku_ids": {"type": "array", "description": "商品SKU ID列表"}
        }
    }
]
```

### 8.2 AI客服场景扩展

```
C端用户可能的问题：

1. 商品咨询
   ├── "这个商品有什么规格？"
   ├── "这个商品有货吗？"
   └── "有没有类似的商品？"

2. 订单查询
   ├── "我的订单到哪了？"
   ├── "什么时候能发货？"
   └── "我想取消订单"

3. 支付问题
   ├── "怎么支付？"
   ├── "支付失败了怎么办？"
   └── "能用花呗吗？"

4. 物流问题
   ├── "发的什么快递？"
   ├── "能改地址吗？"
   └── "预计什么时候到？"

5. 售后问题
   ├── "怎么退货？"
   ├── "退货运费谁出？"
   └── "退款什么时候到账？"
```

---

## 九、开发计划

### Phase 1: C端基础功能（Week 1-2）

```
Week 1:
├── Day 1-2: 创建mall-web和mall-trade模块
├── Day 3-4: 购物车功能（增删改查）
└── Day 5-7: 商品详情页（已有ES支持）

Week 2:
├── Day 1-3: 结算模块（地址/优惠/运费）
├── Day 4-5: 订单创建（事务/库存扣减）
└── Day 6-7: 支付模块（支付宝/微信沙箱）
```

### Phase 2: 物流与售后（Week 3）

```
├── Day 1-2: 物流模块（发货/轨迹/收货）
├── Day 3-4: 售后模块（退款/退货）
└── Day 5-7: 测试 + Bug修复
```

### Phase 3: AI集成与优化（Week 4-5）

```
Week 4:
├── Day 1-3: AI Function Calling扩展
├── Day 4-5: SSE流式输出打通
└── Day 6-7: 联调测试

Week 5:
├── Day 1-3: 评测体系
├── Day 4-5: 高并发设计
└── Day 6-7: Docker化部署
```

---

## 十、面试亮点总结

### 10.1 项目亮点

```
1. 完整的电商业务闭环
   ├── C端：浏览 → 购物车 → 下单 → 支付 → 物流 → 售后
   ├── B端：商品管理 → 订单管理 → 会员管理 → 营销活动
   └── AI：智能客服 + 语义搜索 + NL2SQL分析

2. 高并发场景设计
   ├── Redis Lua预扣库存
   ├── MQ异步削峰
   ├── 多级缓存架构
   └── 分布式锁方案

3. AI能力集成
   ├── LangGraph Agent有向图
   ├── RAG混合检索
   ├── Function Calling打通业务API
   └── LLM-as-Judge评测体系

4. 工程化能力
   ├── Docker Compose容器化
   ├── SSE流式输出
   ├── 可观测性（LangFuse）
   └── 完整的单元测试
```

### 10.2 面试话术

```
项目介绍（30秒版）：
"我做了一套跨境电商全栈系统，包含C端用户前台和B端运营后台。
C端支持商品浏览、购物车、下单支付、物流追踪；后台支持商品管理、
订单管理、会员营销、财务报表。最大的亮点是集成了AI智能客服——
基于LangGraph构建Agent，能查订单、查商品、处理退货，还有完整的
评测体系。技术栈是Java + Python + ES + Redis + MQ + Docker。"

被追问"高并发怎么处理"：
"我设计了三层防护策略：Redis Lua预扣库存（毫秒级），
MQ异步削峰（控制消费速率），DB乐观锁兜底（CAS语义）。
秒杀场景有Nginx限流 + Redis内存标记 + 异步下单。"

被追问"AI客服怎么保证不胡说"：
"三层防护：RAG知识库限定信息来源，Function Calling的
Schema校验防止LLM传非法参数，回答生成后再过一遍LLM自检。"
```

---

## 附录：技术栈清单

```
后端：
├── Java 23 + Spring Boot 3.2.5
├── Spring Security + JWT
├── MyBatis-Plus 3.5.7
├── MySQL 8.0
├── Redis 7
├── RabbitMQ 3
├── Elasticsearch 8.10
├── MinIO（对象存储）
└── Python 3.11 + FastAPI + LangGraph

前端：
├── Vue 3 + TypeScript
├── Vite
├── Pinia（状态管理）
├── Element Plus（UI组件）
└── Axios（HTTP请求）

DevOps：
├── Docker + Docker Compose
├── Nginx
├── GitHub Actions（CI/CD）
└── LangFuse（AI可观测性）

测试：
├── JUnit 5
├── Mockito
├── H2 Database
└── Swagger/OpenAPI
```

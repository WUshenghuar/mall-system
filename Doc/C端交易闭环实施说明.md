# C 端交易闭环实施说明

## 已落地能力

- 独立会员账号：`member_account` 与后台 `sys_user` 分离，手机号密码使用 BCrypt。
- JWT 增加 `principalType`；会员只能访问 C 端交易和会员资源。
- 匿名商品目录：`/api/store/categories`、`/api/store/products`、`/api/store/products/{spuId}`。
- 购物车、地址、订单、支付、物流均按会员 ID 进行归属校验。
- 下单使用 Redis Lua 预占（Redis 可用时）与数据库条件锁库存；取消和超时会释放锁定库存。
- 支付成功只可将待支付订单推进至待发货；重复回调不会重复扣减库存。
- 每五分钟扫描并取消超过三十分钟的待支付订单。
- RabbitMQ 事件在事务提交后发布：`trade.order.created`、`trade.order.paid`、`trade.order.cancelled`。

## 关键接口

| 用途 | 路径 |
|---|---|
| 会员注册/登录 | `POST /api/member/auth/register`、`POST /api/member/auth/login` |
| 前台商品 | `GET /api/store/products`、`GET /api/store/products/{spuId}` |
| 结算复核 | `GET /api/trade/settle`、`POST /api/trade/settle/check` |
| 创建订单 | `POST /api/trade/order` |
| 创建支付/查询 | `POST /api/trade/pay/create`、`GET /api/trade/pay/status/{orderNo}` |
| 开发期模拟支付 | `POST /api/trade/pay/{payNo}/simulate-success` |
| 后台发货 | `POST /api/order/trade/{orderNo}/ship` |

模拟支付仅在 `dev` 配置的 `trade.payment.simulation-enabled=true` 时可用。

## 本地运行

1. 复制 `.env.example` 为 `.env`，替换所有 `[REDACTED_SECRET]` 占位符。
2. 执行 `docker compose up -d` 启动 MySQL、Redis、RabbitMQ、Elasticsearch、MinIO。
3. 启动后端后，Flyway 会执行 `V1__c_trade_closure.sql`。
4. 在 `mall-storefront` 执行 `npm install` 与 `npm run dev`，默认访问 `http://localhost:5174`。

## 首期边界

优惠券、运费、ES 索引修复、真实支付签名校验与 AI 客服均不属于当前购买闭环；真实支付渠道接入前必须关闭模拟支付并补充渠道签名校验。

# C 端交易闭环实施说明

## 当前进度（2026-09-04）

本轮 C 端购买闭环已完成并推送至 `origin/master`，提交为 `216410e feat: 完成C端交易闭环`。后端全量测试与 C 端生产构建均已通过。

本轮新增和收口的实现：

- 建立 Flyway 前向迁移、Docker Compose 与环境变量化基础设施连接配置。
- 增加会员认证、C 端目录服务、结算复核、订单/库存状态机、超时取消任务及后台发货。
- Redis 批量 Lua 预占与数据库条件更新协同，确保取消、超时与支付失败路径均能释放库存。
- 为交易事件配置事务后发布、持久化队列、死信队列以及 RabbitMQ 发布确认参数。
- 交付 `mall-storefront` 移动 H5：商品、账户、地址、购物车、结算模拟支付、订单取消/确认收货/物流、收藏与足迹入口。

下一轮优先事项：真实支付渠道验签和金额核验、事务 outbox 与投递重试、服务端结算快照令牌、ES 索引修复、移动端实机多视口验收，随后再开展 AI 客服。

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

`application.yml` 会自动导入项目根目录 `.env`，因此启动后端时直接执行 `mvn spring-boot:run` 即可；无需再手动将 `.env` 逐项导入 PowerShell 环境变量。

## 首期边界

优惠券、运费、ES 索引修复、真实支付签名校验与 AI 客服均不属于当前购买闭环；真实支付渠道接入前必须关闭模拟支付并补充渠道签名校验。

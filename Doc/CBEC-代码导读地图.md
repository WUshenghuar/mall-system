# CBEC 代码导读地图（按阅读顺序）

> 生成时间：2026-09-11。目的：按“最快读懂全项目”的顺序标注每个关键类的一句话职责与面试可讲点。
> 代码规模：以下规模仅作阅读导航，不作为当前统计口径；AI 已具备 Java SSE + Python 对话/RAG + 受控只读工具 Demo，高级能力仍在规划中。
> 阅读前提：先读 `Doc/当前实现基线与文档口径.md`（30 分钟建立全局）。

---

## 第 0 步：全局骨架（0.5 天）

| 读什么 | 位置 | 一句话职责 | 面试可讲点 |
|---|---|---|---|
| 统一响应 | `common/result/Result.java` | 所有接口返回 `{code,message,data,timestamp}` | 业务错误走 body code，HTTP 保持 200 |
| 错误码 | `common/result/ResultCode.java` | 成功/失败/未授权等常量 | — |
| 业务异常 | `common/exception/BusinessException.java` | 带 code 的运行时异常 | 与全局异常处理器配合 |
| 全局异常 | `common/exception/GlobalExceptionHandler.java` | 把异常转成统一 Result | ⚠️ 业务异常未映射 HTTP 状态码（前端只认 body code） |
| 实体基类 | `common/entity/BaseEntity.java` | id/createTime/updateTime/deleted 公共字段 | 逻辑删除 + 自动填充 |
| 自动填充 | `common/config/MyMetaObjectHandler.java` | MyBatis-Plus 自动填创建/更新时间 | — |

> 看懂 `Result` 的约定后，后端所有 Controller 都能读懂了——它们只是"调用 Service 返回 Result"。

---

## 第 1 步：安全与权限（0.5 天，面试高频）

| 读什么 | 位置 | 一句话职责 | 面试可讲点 |
|---|---|---|---|
| 安全链 | `security/config/SecurityConfig.java` | 放行登录/商品/支付回调，其余需认证 | CORS 白名单为 18103/18102/3000；B/C 端避开 Windows 保留端口 |
| JWT 过滤器 | `security/jwt/JwtAuthenticationFilter.java` | 从 Header 取 token，验签后写入上下文 | 无效 token 不拦截，只跳过（靠 Security 兜底） |
| token 生成 | `security/jwt/JwtTokenProvider.java` | 签发/解析 JWT，含 principalType | MEMBER 与 SYS_USER 双身份 |
| 用户体系 | `security/user/LoginUser.java` / `MemberPrincipal.java` / `UserDetailsServiceImpl.java` | B 端用户 / C 端会员分离 | `principalType` 区分两套认证 |
| 行级权限 | `security/annotation/DataScope.java` + `aspect/DataScopeAspect.java` | 注解 + AOP 注入 SQL 过滤 | ⚠️ **重点**：store_manager 全量 / cs_specialist 仅分配订单 / 运营按分类，切面把 scopeSql 写入 ThreadLocal（DataScopeContext），由 MyBatis 拦截拼 SQL |
| 当前用户 | `security/user/CurrentMember.java` | 从上下文取当前 C 端会员 | — |

> 面试提问概率最高的是"@DataScope 怎么做到行级权限"和"B 端用户和 C 端会员怎么共用一个 Security 体系"——把这两个类读透。

---

## 第 2 步：数据库全局（0.5 天，读表胜读代码）

`src/main/resources/db/schema.sql` + `db/migration/V1~V5`

核心表分四组，按依赖顺序读：

| 表组 | 表 | 关系 |
|---|---|---|
| 系统 | sys_user / sys_role / sys_menu | RBAC |
| 商品 | pm_spu / pm_sku / pm_sku_stock / pm_brand / pm_category | SPU 一对多 SKU，SKU 一对多库存 |
| 会员 | mm_member / member_account / member_address / member_favorite / member_browse_history / mm_member_points_log | 账号与资料分离，积分流水按会员归属查询，确认收货触发成长奖励，退款回滚 |
| **交易（重点）** | trade_cart / trade_order / trade_order_item / trade_pay / trade_logistics / trade_refund | C 端交易 |
| 后台订单 | order / order_item / order_pay / order_logistics / order_refund | ⚠️ 与 trade_* 并存（历史演进，需统一） |
| 营销 | coupon / coupon_issue / activity / activity_sku | 优惠券 + 活动商品关联（活动价/库存/限购） |
| 财务 | finance_statement / finance_statement_item / tax_config | 结算单 + 税率 |

---

## 第 3 步：交易闭环 trade（1~3 天，全项目核心）

> 这是 C 端购买的主链路，也是面试最深挖的部分。按"一次下单"的时间顺序读。

| 顺序 | 类 | 一句话职责 | 面试可讲点 |
|---|---|---|---|
| 1 | `trade/controller/CartController` + `service/CartServiceImpl` | 购物车 CRUD | 会员归属校验 |
| 2 | `trade/service/SettlementService` + `finance/service/TaxConfigService` + `marketing/service/ActivityService` | 结算预览（活动价/优惠券/库存/税费核对） | 价格与税费由服务端重算 |
| 3 | `trade/service/SettlementSnapshotService` | 结算快照（Redis 存 15 分钟） | 一次性、防价格篡改 |
| 4 | `trade/service/RedisStockReservationService` | Redis Lua 预占库存 | ⚠️ **重点**：多 SKU 原子预占 + 事务回滚自动释放 |
| 5 | `trade/service/impl/TradeOrderServiceImpl` | 下单：校验→锁普通/活动库存→建单→MQ | ⚠️ **重点**：活动价快照、优惠券占用回滚 + cancelExpiredOrders 超时关单 |
| 6 | `trade/service/PayService` | 支付单创建、模拟支付与支付宝回调校验 | 真实商户渠道联调未完成 |
| 7 | `order/service/impl/OrderServiceImpl` | B 端后台订单处理（发货/状态机） | ⚠️ trade 与 order 两套模型的关系 |
| 8 | `trade/service/TradeRefundService` | C 端退款/退货退款售后 | 仅退款：待审→待退款→已退款；退货退款：待审→待用户退货→待平台收货→已退款；驳回恢复原订单状态，完成退款补回库存 |
| 9 | `trade/service/LogisticsService` | 物流记录查询 | — |

**读懂 trade 的捷径**：先画一张状态机——
待支付 → 待发货 → 待收货 → 已完成；待支付可取消/超时关单；待发货可申请仅退款，待收货/已完成可申请退货退款并提交退货物流。
然后读 `TradeOrderServiceImpl` 的 public 方法列表（上面已列出），基本覆盖全链路。

---

## 第 4 步：外围业务（1~2 天，可快速扫读）

| 模块 | 文件数 | 一句话 | 面试可讲点 |
|---|---|---|---|
| product | 20 | SPU/SKU/品牌/分类/库存 | HS 编码（跨境） |
| member | 23 | 会员资料/地址/积分/收藏/足迹 | 手机号账号与资料分离 |
| marketing | 15 | 优惠券发放核销/活动商品关联/秒杀（`ActivityServiceImpl`、`SeckillServiceImpl`） | 优惠券行锁与条件扣量、唯一键 upsert、秒杀并发控制 |
| finance | 11 | 结算单/税率/EasyExcel 导出 | 跨境税率配置 |
| search | 4 | ES 索引/全文搜索 | ik_smart 分词 + 增量同步（未完整） |
| system | 11 | 用户/角色/菜单 CRUD | RBAC |
| web | 30 | 全部 Controller（薄层） | 只做参数→service 转发 |

---

## 第 5 步：B 端前端 mall-web（1 天挑重点）

- 布局：`src/layouts/AdminLayout.vue` + 动态路由
- 请求封装：`src/utils/request.js`（拦截器、token 注入）
- **优先读 4 个页面**：`product/index.vue`（增删改查范式）、`order/index.vue`、`order/refund.vue`、`system/index.vue`（角色权限）
- 组合式函数：`src/composables/useCrudModal.js` / `usePagination.js`（看懂后所有页面都通）

---

## 第 6 步：C 端前端 mall-storefront（0.5 天）

- 全部只有 8 个视图 ~360 行，半天读完整
- 重点：`src/api.js`（axios 封装 + ⚠️ 401/403 拦截器 bug）、`src/router.js`（会员路由守卫）
- 响应式：`src/styles.css` 末尾 `@media(min-width:1025px)` 桌面断点（本次新增）

---

## 第 7 步：AI 客服 Agent（1 天，项目差异化）

| 顺序 | 类/文件 | 一句话职责 | 面试可讲点 |
|---|---|---|---|
| 1 | `ai/controller/AiChatController` | 复用会员 JWT，建立 Java SSE 出口 | 业务鉴权不交给模型 |
| 2 | `ai/service/impl/AiChatServiceImpl` | 保存会话并路由订单/物流/退款/商品只读查询 | 资源归属校验、工具白名单 |
| 3 | `ai/service/AiGatewayClient` | Java 23 兼容地转发 Python SSE | 跨语言边界、超时和错误映射 |
| 4 | `mall-ai-service/app/api/chat.py` | 校验内部令牌并输出 `thinking/tool_call/sources/text/done` | SSE 事件协议、订单/会员/税费只读来源 |
| 5 | `mall-ai-service/app/rag/retriever.py` | ES BM25 检索平台规则，失败时回退本地 Agent | RAG 降级和来源展示 |
| 6 | `mall-storefront/src/components/CustomerServiceWidget.vue` | C 端聊天浮窗、快捷问题、来源和转人工入口 | 44px 触控区、登录边界 |
| 7 | `ai/controller/AiSupportTicketController` + `AiSupportTicketServiceImpl` | 平台客服工单创建、认领、解决 | 状态条件更新、会员/客服权限分离 |

---

## 常见问题速查（面试自检）

1. **一次下单的完整链路？** 购物车 → 结算快照 → Redis Lua 预占库存 → 建单（trade_order）→ MQ 发事件 → 支付 → 扣减 → 物流。
2. **库存怎么防超卖？** Redis Lua 原子 DECRBY + 事务回滚补偿；Redis 不可用降级为数据库条件更新。
3. **C 端会员与 B 端用户如何区分？** JWT principalType 字段，过滤器按类型加载不同 Principal。
4. **行级数据权限？** @DataScope 注解 + AOP 向 SQL 注入成员过滤条件。
5. **为什么有两套订单表？** order（早期/后台）与 trade（C 端交易）并存，文档标注为待统一的技术债——如实说明即可。
6. **业务错误为何 HTTP 200？** GlobalExceptionHandler 未加 @ResponseStatus，靠 body code 传达——前端需自行判断 code。

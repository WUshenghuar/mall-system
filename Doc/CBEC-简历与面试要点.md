---
title: CBEC 项目简历与面试要点
updated: 2026-09-11
---

# CBEC 项目简历与面试要点

## 一句话定位

单平台 B2C 跨境电商系统：Java 负责交易、库存、支付回调、营销和权限，Vue 负责 B/C 端，Python 负责 AI 客服编排；重点突出“真实业务闭环 + AI 安全接入”，不是多商户平台。

## 项目结构

```text
CBEC/
├─ src/main/java/com/mall/       Java 23 + Spring Boot 单 Maven 后端
│  ├─ product/                   SPU、SKU、库存、C 端商品目录
│  ├─ trade/                     购物车、结算、订单、支付、物流、退款
│  ├─ marketing/                优惠券、活动、秒杀
│  ├─ member/                    会员、地址、收藏、足迹
│  ├─ finance/                   对账单、税率、导出
│  ├─ security/                  JWT、B/C 双身份、RBAC、数据权限骨架
│  ├─ search/                    Elasticsearch 商品索引与搜索
│  └─ ai/                        AI 网关、会话持久化、受控工具路由
├─ mall-web/                     Vue 3 + Ant Design Vue，B 端运营台
├─ mall-storefront/              Vue 3 + Vant，C 端响应式交易前台
├─ mall-ai-service/              Python FastAPI + LangGraph + ES BM25
├─ src/main/resources/db/        Flyway 迁移与数据库结构
└─ docker-compose.yml             MySQL、Redis、RabbitMQ、ES、MinIO、AI
```

## 可写进简历的真实亮点

- 设计并实现购物车→15 分钟一次性 Redis 结算快照→Redis Lua 多 SKU 库存预占→订单→支付模拟/回调→物流→退款/退货退款审核的 B2C 交易闭环，并在退款完成事务中补回数据库、Redis 与活动库存，同时刷新回补缓存 TTL。
- 将优惠券校验、门槛计算、折扣金额、会员券原子核销接入结算和订单事务，避免只在前端展示优惠。
- 处理优惠券高并发领取：先锁定优惠券行再校验每人限领并条件扣减发行量；未支付订单取消和超时关单恢复券的可用状态，并用会员与订单号条件更新避免误恢复其他订单的券。
- 打通会员账户页的等级、积分余额和积分流水查询，B 端积分调整通过会员归属接口同步到 C 端；确认收货后自动累计消费、奖励积分并按阈值更新等级，已完成订单退款时反向回滚。
- 打通 B 端活动类型配置到 C 端活动展示，后端按起止时间筛选进行中活动，并为数据库必填字段提供服务层兜底。
- 打通活动商品配置：B 端可关联 SKU 并维护活动价、活动库存和每人限购，C 端活动接口返回关联商品；通过唯一键和服务层 upsert 防止重复配置。
- 将活动价接入服务端结算和订单明细快照，秒杀活动库存使用数据库条件更新，下单取消时释放活动库存，避免只改前端展示价格。
- 将 B 端按分类/原产国/目的国配置的税率接入 C 端结算，校验单订单币种一致性，并把税费与币种落库到交易订单和对账明细。
- 修复 Windows 本地 Elasticsearch 的不可用 `ik_smart` 映射，改用内置分词并在全量同步时自动重建旧索引，保证搜索服务可启动、可同步、可查询。
- 将退货退款建模为待审→待用户退货→待平台收货→已退款状态机，保存售后凭证与退货物流，并用条件更新保证审核、提交物流和完成退款的幂等性。
- 完善 B 端订单运营看板：后端按今日/过去 24 小时聚合 24 个小时桶，前端支持范围切换与 30 秒轮询刷新。
- 基于 Java SSE 网关 + Python FastAPI/LangGraph 构建平台 AI 客服，支持 BM25 知识检索、中文 FAQ fallback、会话保存和流式事件。
- 实现订单、物流、退款、商品、会员优惠券、会员资料和税费币种七类受控只读工具；Java 侧校验 JWT、会员归属和资源权限，模型不直接访问业务数据库。
- 增加平台人工接管工单：会员可从客服浮窗提交工单，B 端客服按权限认领并解决，状态通过条件更新避免并发重复认领。
- 增加 Prompt Injection 前置拦截、`tool_call/sources/text/done` SSE 协议、知识来源展示与异常降级，保留退款/取消订单等高风险操作人工处理边界。
- 提供 `evaluate.py` 确定性策略评测和 pytest 回归，覆盖问候、退款、物流、优惠券、商品与注入拦截场景，当前 6/6 通过。
- 通过全量 `mvn test` 72/72、AI Python 测试 10/10、B/C 两端生产构建，修复 Java 23 Windows Redis loopback 与 SSE 异步分发问题。

## 30 秒项目介绍

我做的是一个单平台 B2C 跨境电商系统。后端采用 Java 23 和 Spring Boot，覆盖商品、会员、购物车、结算、库存、支付、物流、优惠券和退款；B 端负责运营管理，C 端负责真实购买流程。后续我又增加了 Python FastAPI + LangGraph 的 AI 客服层，通过 Java SSE 网关输出流式回答。AI 只能通过 Java 侧授权的只读工具查询订单、物流、退款、商品和优惠券，配合 Elasticsearch RAG、Prompt Injection 防护和本地 fallback，核心交易不依赖模型。

## 关键流程

```text
C 端 JWT
  → Java Controller / Service
  → 业务归属与状态校验
  → 结算快照 / Redis Lua / MySQL 事务
  → MQ 事件、支付、物流、退款状态机

C 端客服
  → Java AiChatController
  → 会员鉴权、会话保存、只读工具路由
  → Python FastAPI / LangGraph
  → ES BM25 或本地 fallback
  → SSE: thinking → tool_call → sources → text → done
```

## 高频面试问题

| 问题 | 回答重点 |
|---|---|
| 为什么 Java 和 Python 分层？ | Java 保证事务、权限和数据一致性；Python 使用 LangGraph/RAG 等 AI 生态。 |
| 为什么用 SSE？ | 客服主要是服务端单向流式输出，SSE 比 WebSocket 简单，兼容 HTTP 和代理。 |
| 如何防止订单越权？ | Controller 从 JWT 获取 memberId，服务层使用 `getOwnedByOrderNo` 查询并校验归属。 |
| AI 如何减少幻觉？ | 知识问题走 RAG；真实业务数据走只读工具；没有依据时 fallback，前端展示 sources。 |
| Prompt Injection 怎么防？ | 输入先过策略检测；系统约束只回答平台问题；高风险写操作不暴露给模型。 |
| 为什么不能让 AI 直接退款？ | 退款是高风险写操作，需要确认、权限、幂等、审计和人工审批。 |
| 结算快照解决什么问题？ | 服务端保存短时一次性商品/地址快照，防止前端篡改价格或购物车内容。 |
| 库存如何防超卖？ | Redis Lua 原子扣减可用库存，数据库条件更新锁定库存，事务回滚时补偿 Redis。 |
| AI 服务或 ES 挂了怎么办？ | 检索失败返回空上下文，使用本地 LangGraph 规则回答；核心交易不依赖 AI。 |
| 当前还不是生产级的地方？ | 仍缺向量 RAG/Rerank、模型 Function Calling、LangFuse/OTel 和真实支付；当前退货退款是单平台 MVP。 |

## 运行与证据

- 一键启动：[启动全部.cmd](../启动全部.cmd)
- 项目基线：[当前实现基线与文档口径.md](当前实现基线与文档口径.md)
- 代码阅读：[CBEC-代码导读地图.md](CBEC-代码导读地图.md)
- 最近逻辑提交：退货退款售后闭环、`a26b061`（活动价结算与 Java 23 Redis 兼容）、`01b54ff`（活动商品关联）。

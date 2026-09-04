---
title: AI客服 Agent - 跨境电商智能助手实施方案
created: 2026-09-04
status: 待实施
---

# AI 客服 Agent——跨境电商智能助手实施方案

## 1. 实施目标

在现有 CBEC Java 业务系统上落地可验证的 AI 客服闭环：登录用户通过 Vue 3 发起流式对话，Agent 使用 LangGraph 进行意图路由，可通过 RAG 回答商品/政策问题，或通过受控工具查询真实订单、商品、退货和物流数据；所有请求可观测、可评测，风险操作可确认或转人工。

## 2. 实施边界

### P0

- FastAPI AI 微服务、LangGraph 单 Agent、LLM 客户端封装
- Spring Boot AI 代理模块、JWT/RBAC 鉴权和 SSE 转发
- ES 8 BM25 + dense_vector 混合检索、知识库管理
- 订单/商品/退货/物流/商品搜索/运费估算工具
- Prompt Injection 防护、工具风险分级、人工接管和审计
- checkpoint 断线恢复、幂等控制、LangFuse/OTel 观测
- 50+ 测试集、LLM-as-Judge + 代码评测

### P1

- NL2SQL 销售分析（只读视图、字段白名单、AST 校验）
- 商品文案生成、图片理解、多语言客服
- MCP 工具适配层（仅在工具跨 Agent 复用时引入）

## 3. 实施阶段

| 阶段 | 工期 | 主要任务 | 完成标准 |
|---|---:|---|---|
| 0. 基线 | 1 天 | 核对现有 API、权限、订单状态机和 ES；建立 AI 模块与环境配置 | 两个服务可启动，敏感配置不入库 |
| 1. 对话闭环 | 2 天 | LLM 客户端、会话、SSE、前端聊天组件、超时重试 | 登录用户可多轮流式对话 |
| 2. RAG | 3 天 | 文档分段、Embedding、ES 索引、BM25/向量召回、Rerank | 文档可管理，回答可返回来源 |
| 3. 工具 Agent | 3 天 | IntentRouter、Tool Calling、Schema 校验、业务 API 代理 | 五类意图可路由，数据来自真实 API |
| 4. 安全治理 | 2 天 | 注入检测、越权校验、风险分级、人工转接、审计 | 高风险操作确认后才执行 |
| 5. 可靠性 | 2 天 | checkpoint、幂等键、状态校验、熔断和降级 | 断线可恢复，重试不重复写入 |
| 6. 评测上线 | 3 天 | 测试集、Judge、代码评测、Trace、灰度和回滚 | 可回归、可定位、可降级 |

## 4. 代码落地顺序

1. 新增 'mall-ai' Java 模块：Controller、Service、DTO、权限、审计和数据库迁移。
2. 新增 'mall-ai-service' Python 服务：配置、LLM 客户端、LangGraph、工具执行器和 RAG。
3. 实现 '/api/ai/chat'，统一 SSE 事件：thinking、tool_call、text、error、done。
4. 建立 ai_conversation、ai_agent_checkpoint、ai_knowledge_doc、ai_audit_log 表。
5. 建立 ES ai_knowledge 索引和知识库 CRUD/同步。
6. 接入只读业务工具；退款、取消订单等写工具默认不开放。
7. 接入 LangFuse/OTel，再增加评测和灰度发布。

## 5. 关键安全要求

- 工具使用白名单、Schema、权限、资源归属和超时校验。
- 输入执行长度限制、Prompt Injection 检测、上下文隔离和敏感信息过滤。
- 高风险工具需要用户二次确认；高金额退款需要人工审批。
- 写操作必须带幂等键并进行状态机校验；所有动作写入审计日志。
- NL2SQL 只允许访问只读视图，执行前进行字段白名单、AST、LIMIT、超时和行数校验。

## 6. 测试与验收

- 单元测试：Agent 节点、工具 Schema、权限、幂等、脱敏和错误映射。
- 集成测试：LLM → Agent → 工具 → Spring Boot API → SSE 全链路。
- 安全测试：Prompt Injection、越权订单、敏感字段、重复退款和异常工具调用。
- 评测测试：至少 50 条 FAQ/商品/订单/退货/物流用例；同时记录准确性、任务完成率、延迟、Token 和用户反馈。
- 上线条件：核心交易不依赖 AI；AI 服务异常时返回安全兜底，且可通过开关快速禁用。

## 7. 相关文档

- AI客服Agent-跨境电商智能助手设计方案.md
- 跨境电商全栈系统-需求设计文档.md
- 跨境电商全栈系统-项目概要文档.md
- 跨境电商全栈系统-实现流程文档.md

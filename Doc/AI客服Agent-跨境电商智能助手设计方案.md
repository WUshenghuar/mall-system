---
created: 2026-07-20
tags:
  - ai/customer-service
  - architecture
  - project/design
  - ai/agent
source: "[[AI应用开发工程师学习路线.md]]"
---

# AI 客服 Agent — 跨境电商智能助手

> 基于 B2C 跨境电商后台管理系统（`mall-system`）的 AI 能力扩展
> 定位：在现有 Spring Boot 后端上集成 AI Agent，实现智能客服、语义搜索、NL2SQL 分析

---

## 一、整体架构

### 1.1 双层架构

```
┌─────────────────────────────────────────────────────┐
│                   Vue 3 Frontend                     │
│  商品管理 ｜ AI 聊天气泡 ｜ 数据看板 ｜ SSE 流式打字机    │
└──────────────────────┬──────────────────────────────┘
                       │ REST + SSE (EventSource)
┌──────────────────────┴──────────────────────────────┐
│          Spring Boot (业务层 · 已有)                  │
│  ┌──────────┐ ┌────────┐ ┌──────────┐              │
│  │ mall-    │ │ mall-  │ │ mall-    │              │
│  │ product  │ │ order  │ │ member   │              │
│  ├──────────┤ ├────────┤ ├──────────┤              │
│  │ mall-ai  │ │ mall-  │ │ mall-    │  ← 新增模块   │
│  │ integration│ search │ │ security │              │
│  └──────────┘ └────────┘ └──────────┘              │
│  RBAC / JWT / Redis / RabbitMQ / MySQL / ES 8       │
└──────────────────────┬──────────────────────────────┘
                       │ HTTP 转发
┌──────────────────────┴──────────────────────────────┐
│         Python FastAPI (AI 微服务层 · 新增)           │
│  ┌────────────────────────────────────────────────┐ │
│  │  LangGraph Agent (ReAct 模式)                   │ │
│  │  ├─ 意图识别 → 条件路由                           │ │
│  │  ├─ RAG 混合检索 (Hybrid Search + Rerank)       │ │
│  │  ├─ Function Calling → 调 Spring Boot API       │ │
│  │  └─ SSE 流式输出                                 │ │
│  └────────────────────────────────────────────────┘ │
└──────┬──────────────────────┬───────────────────────┘
       │                      │
  ┌────┴────┐          ┌─────┴─────┐
  │ LLM API │          │  ES 8     │
  │ Claude  │          │ dense_vec │
  │ GPT-4o  │          │ + BM25    │
  └─────────┘          └───────────┘
```

**设计原则**：
- Java 锚定业务层（事务、鉴权、持久化、高并发）
- Python 锚定 AI 层（LangChain、Embedding、Agent 编排）
- 两套服务各自 Docker 容器部署，通过 HTTP + SSE 通信

### 1.2 技术栈扩展

| 层次 | 技术 | 版本 | 用途 |
|------|------|------|------|
| **AI 微服务** | Python 3.11 + FastAPI | - | AI 编排层 |
| **Agent 框架** | LangChain + LangGraph | 最新 | Agent 有向图编排 |
| **AI 网关** | LiteLLM | - | 多模型路由 + 降级 |
| **可观测性** | LangFuse | - | Trace + 评测 |
| **ES 向量检索** | Elasticsearch 8 `dense_vector` | 8.x | 已有资产，直接复用 |
| **新模块** | `mall-ai-integration` | - | Spring Boot AI 代理模块 |

### 1.3 Maven 模块新增

```
mall-system/
├── mall-ai-integration/              # 新增：AI 集成模块
│   └── src/main/java/com/mall/ai/
│       ├── controller/
│       │   ├── AiChatController.java       # SSE 聊天代理
│       │   ├── AiKnowledgeController.java  # 知识库管理
│       │   └── AiEvalController.java       # 评测接口
│       ├── service/
│       │   ├── AiChatService.java          # 对话管理
│       │   ├── AiKnowledgeService.java     # 知识库 CRUD
│       │   └── AiEvalService.java          # 评测服务
│       ├── entity/
│       │   ├── AiConversation.java         # 对话记录
│       │   ├── AiKnowledgeDoc.java         # 知识库文档
│       │   ├── AiEvalTestcase.java         # 评测测试集
│       │   └── AiEvalRun.java              # 评测运行记录
│       ├── mapper/                         # MyBatis-Plus Mapper
│       └── dto/                            # 数据传输对象
├── mall-search/                       # 已有：扩展向量检索
└── pom.xml                            # 添加 mall-ai-integration 模块
```

---

## 二、AI 功能矩阵

### 2.1 核心功能

| 功能 | 技术栈 | 面试可讲深度 | 优先级 |
|------|--------|-------------|--------|
| **AI 客服 Agent** | LangGraph + RAG + Function Calling + SSE | ★★★★★ | P0 |
| **语义搜索** | ES Hybrid Search + Query Rewrite + Rerank | ★★★★ | P0 |
| **商品运营套件** | Prompt + Vision API + Few-shot | ★★★ | P1 |
| **NL2SQL 分析** | LLM → SQL → 执行 → ECharts | ★★★★★ | P1 |
| **AI 评测体系** | LLM-as-Judge + 自动跑分 + 回归 | ★★★★★ | P0（贯穿全程） |

### 2.2 面试杀伤力排序

| 功能 | 为什么拉开差距 |
|------|--------------|
| **评测体系** | 99% 应届生不做。能聊"怎么衡量 AI 质量"就是降维打击 |
| **NL2SQL Agent** | Function Calling 调用 Spring Boot API，展示了后端+AI 的双向打通 |
| **语义搜索** | Hybrid Search + Rerank 完整的检索 Pipeline，不是调个 Embedding API 就完事 |
| **AI 客服** | LangGraph 有向图 + 条件路由 + Fallback，体现了架构设计能力 |

---

## 三、AI 客服 Agent 详细设计

### 3.1 Agent 工作流 (LangGraph)

```
用户输入
   │
   ▼
┌─────────────┐
│ 意图识别      │ ← LLM 判断意图类型
│ IntentRouter │
└──────┬──────┘
       │
   ┌───┼───┬───────────────┐
   │   │   │               │
   ▼   ▼   ▼               ▼
简单问答  复杂任务  查订单/退货  打招呼
   │   │   │               │
   ▼   ▼   ▼               ▼
RAG 检索 工具调用   Spring    直接回复
(ES      (多步     Boot API
dense_   Agent    调用
vector)   协作)
   │   │   │               │
   └───┼───┴───────────────┘
       │
       ▼
  ┌────────┐
  │ 质量检查 │ ← LLM 自检回答质量
  │ Guard   │
  └──┬─────┘
     │
  合格/不合格
     │
     ▼
  SSE 流式输出
```

### 3.2 Function Calling 工具集

Agent 通过 Function Calling 调用 Spring Boot 的已有 API：

| 工具名 | 触发场景 | 对应后端 API |
|--------|---------|-------------|
| `query_order` | "查一下我的订单" | `GET /api/order/by-order-no/{orderNo}` |
| `query_product` | "这个商品还有吗" | `GET /api/product/spu/{id}` |
| `check_return_eligibility` | "能退货吗？" | `GET /api/order/refund/check-eligibility/{orderNo}` |
| `track_logistics` | "货到哪了" | `GET /api/order/{id}/logistics` |
| `search_products` | "帮我找找红色裙子" | `GET /api/search/product?keyword=...` |
| `calc_shipping` | "运费多少钱" | `GET /api/order/shipping/estimate` |

**关键设计**：
```python
# 工具调用安全设计
def execute_tool(tool_name: str, args: dict) -> dict:
    # ① Schema 校验：防止 LLM 胡乱传参
    validate_args(tool_name, args)
    
    # ② 超时裁剪：防止调用卡死
    result = call_with_timeout(api_endpoint, args, timeout=5)
    
    # ③ 错误 Fallback：调用失败后 LLM 重新规划
    if result.is_error:
        return {"fallback": True, "message": "暂时查不到，请稍后再试"}
    
    return result
```

### 3.3 SSE 流式输出全链路

```
Python FastAPI                    Spring Boot                      Vue 3
═══════════════                   ═══════════════                 ══════
                                  POST /api/ai/chat
                                         │
  StreamingResponse ◄── SSE 转发 ── SseEmitter
  │                                        │
  ├─ data: {type:"thinking"}               │  fetch + ReadableStream
  ├─ data: {type:"tool_call", ...}         │  │
  ├─ data: {type:"text", content:"正在"}    │  ├─ 解析 SSE data:
  ├─ data: {type:"text", content:"正在查"}  │  ├─ 增量追加到最后一条消息
  ├─ data: {type:"text", content:"正在查询"}│  ├─ 光标闪烁效果
  └─ data: {type:"done", tokens:1024}      │  └─ 完成后显示反馈按钮
```

---

## 四、RAG 混合检索设计

### 4.1 检索 Pipeline

```
用户问题："这个商品能退吗？"
       │
       ▼
  Query Rewrite ── LLM 改写："退货政策是什么？适用于什么条件？"
       │
       ▼
  双路召回
  ┌────┴────┐
  │         │
  BM25     dense_vector
  (关键词)  (语义)
  │         │
  └────┬────┘
       │
  合并 Top 20 结果
       │
       ▼
  Rerank (LLM 精排 Top 5)
       │
       ▼
  LLM 增强生成
```

### 4.2 ES 8 向量检索配置

利用已有的 Elasticsearch 8，无需引入额外向量数据库：

```json
// ES 索引：ai_knowledge
{
  "ai_knowledge": {
    "mappings": {
      "properties": {
        "title":        { "type": "text", "analyzer": "ik_smart" },
        "content":      { "type": "text", "analyzer": "ik_smart" },
        "category":     { "type": "keyword" },
        "content_vector": {
          "type": "dense_vector",
          "dims": 1536,
          "similarity": "cosine"
        }
      }
    }
  }
}
```

### 4.3 ES 向量检索对比 Spring Data 方案

| 维度 | Spring Data ES | Python ES Client |
|------|---------------|-----------------|
| `dense_vector` 支持 | ❌ 官方 starter 不支持脚本评分 | ✅ 原生支持 |
| 向量维度灵活性 | 需自定义 repository | 完全控制 mapping |
| Hybrid Search | 难实现 | 一条 bool query 搞定 |
| **结论** | **AI 相关检索交给 Python 层** | **推荐方案** |

---

## 五、新增数据库设计

```sql
-- AI 对话记录
CREATE TABLE ai_conversation (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id    VARCHAR(64) NOT NULL,        -- 会话 ID
    user_id       BIGINT NOT NULL,             -- 操作用户
    role          VARCHAR(16) NOT NULL,         -- user / assistant / system
    content       TEXT,                         -- 消息文本
    tool_calls    JSON,                         -- Agent 调用的工具记录
    tokens_used   INT DEFAULT 0,               -- Token 消耗
    feedback      TINYINT DEFAULT NULL,         -- 用户反馈: 1赞 -1踩 NULL未评
    latency_ms    INT DEFAULT 0,                -- 响应延迟 (ms)
    model         VARCHAR(32),                  -- 使用的模型
    create_time   DATETIME,
    INDEX idx_session (session_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- AI 知识库文档
CREATE TABLE ai_knowledge_doc (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    title         VARCHAR(200) NOT NULL,
    content       TEXT NOT NULL,                -- 文档正文
    category      VARCHAR(50) NOT NULL DEFAULT 'faq',  -- 分类: policy / product / faq / logistics
    status        TINYINT DEFAULT 1,            -- 0禁用 1启用
    es_doc_id     VARCHAR(64),                  -- ES 索引中的文档 ID
    create_time   DATETIME,
    update_time   DATETIME,
    deleted       TINYINT DEFAULT 0,
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- AI 评测测试集
CREATE TABLE ai_eval_testcase (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    query           TEXT NOT NULL,              -- 用户问题
    expected_answer TEXT NOT NULL,              -- 期望回答
    category        VARCHAR(32) NOT NULL,       -- 分类: order / product / return / logistics
    difficulty      TINYINT DEFAULT 1,          -- 1简单 2中等 3困难
    create_time     DATETIME,
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- AI 评测运行记录
CREATE TABLE ai_eval_run (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    model           VARCHAR(32) NOT NULL,        -- 评测的模型版本
    prompt_version  VARCHAR(32),                 -- Prompt 版本
    avg_score       DECIMAL(4,2),                -- 平均分 (0.00-100.00)
    total_tokens    INT DEFAULT 0,               -- 总 Token 消耗
    avg_latency_ms  INT DEFAULT 0,               -- 平均延迟
    run_time        DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- AI 评测结果明细
CREATE TABLE ai_eval_result (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    run_id          BIGINT NOT NULL,             -- 关联运行记录
    testcase_id     BIGINT NOT NULL,             -- 关联测试用例
    score           DECIMAL(4,2),                -- LLM-as-Judge 打分
    response        TEXT,                        -- Agent 实际回答
    latency_ms      INT DEFAULT 0,
    tokens_used     INT DEFAULT 0,
    FOREIGN KEY (run_id) REFERENCES ai_eval_run(id),
    FOREIGN KEY (testcase_id) REFERENCES ai_eval_testcase(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 六、Python AI 微服务项目结构

```
mall-system/
├── mall-ai-service/                     # 新增：AI 微服务独立项目
│   ├── Dockerfile
│   ├── requirements.txt
│   ├── .env.example
│   ├── app/
│   │   ├── __init__.py
│   │   ├── main.py                      # FastAPI 启动入口
│   │   ├── config.py                    # 配置中心（环境变量）
│   │   ├── agent/
│   │   │   ├── __init__.py
│   │   │   ├── agent.py                 # LangGraph Agent 图编排
│   │   │   ├── tools.py                 # Function Calling 工具定义
│   │   │   └── prompts.py               # 系统 Prompt 模板
│   │   ├── rag/
│   │   │   ├── __init__.py
│   │   │   ├── retriever.py             # Hybrid Search + Rerank
│   │   │   └── embedding.py             # Embedding 模型封装
│   │   ├── api/
│   │   │   ├── __init__.py
│   │   │   ├── chat.py                  # SSE 聊天接口
│   │   │   ├── knowledge.py             # 知识库管理接口
│   │   │   └── eval.py                  # 评测接口
│   │   ├── models/
│   │   │   ├── __init__.py
│   │   │   └── schemas.py               # Pydantic 数据模型
│   │   └── utils/
│   │       ├── __init__.py
│   │       ├── llm.py                   # LLM 客户端封装
│   │       └── tracing.py               # LangFuse 追踪
│   └── tests/
│       ├── test_agent.py
│       ├── test_retriever.py
│       └── test_chat.py
```

---

## 七、评测体系（核心差异化）

### 7.1 评测流程

```
离线评测（开发阶段，每次 Prompt 改动后触发）
══════════════════════════════════════════════
  Step 1: 加载测试集（MySQL ai_eval_testcase 表，50-100 条）
  Step 2: 逐条运行 Agent，记录回答 + 延迟 + Token
  Step 3: LLM-as-Judge 打分（另一个 LLM 对比期望回答和实际回答）
  Step 4: 计算平均分，记录到 ai_eval_run 表
  Step 5: 输出对比报告（vs 上一版本）

在线监控（运行阶段）
══════════════════════
  Step 1: LangFuse 追踪每次请求（Trace ID）
  Step 2: 用户点赞/点踩（反馈到 ai_conversation.feedback）
  Step 3: 仪表盘：响应时间、Token 消耗趋势、用户满意度
```

### 7.2 LLM-as-Judge Prompt

```
你是一个 AI 客服回答质量评审员。请对以下回答打分（0-100）：

【用户问题】
{query}

【期望回答】
{expected_answer}

【实际回答】
{actual_response}

打分标准：
- 90-100：准确、完整、友好，完全符合期望
- 70-89：基本正确，但缺少部分细节
- 50-69：部分正确，但有明显遗漏或错误
- 0-49：回答错误或与问题无关

只返回数字，不要任何解释。
```

### 7.3 可对比的场景

| 实验 | 变量 | 观测指标 |
|------|------|---------|
| 换模型 | GPT-4o → Claude 3.5 Sonnet | 平均分变化、延迟变化 |
| 改 Prompt | 系统提示词不同版本 | 平均分变化、特定分类表现 |
| 调整检索策略 | BM25 vs Hybrid vs Hybrid + Rerank | 回答准确率 |
| 添加工具 | 增加/减少 Function | 任务完成率 |

---

## 八、面试应答策略

### 8.1 项目介绍（30 秒版）

> "我在跨境电商后台基础上，用 Python FastAPI + LangGraph 构建了一套 AI 客服 Agent 系统。它通过意图识别路由到 RAG 检索或 Function Calling，能查订单、查商品、处理退货等，全部通过 SSE 流式输出到前端。而且我建了一套评测体系，每次改动都能量化对比。"

### 8.2 常见追问 & 回答

| 面试官问题 | 你的回答 |
|-----------|---------|
| "为什么用 Python 做 AI 层，而不是 Java？" | "Java 的 AI 生态不够成熟——LangChain、LangGraph、Embedding 模型绑定都是 Python 原生的。我用双层的核心逻辑是：业务层在 Java 做（事务、权限、高并发），AI 编排在 Python 做，各取所长。中间通过 HTTP + SSE 通信，各自 Docker 独立部署。" |
| "Agent 怎么保证不胡说八道？" | "三层防护：第一层——RAG 知识库限定了信息来源，LLM 只能基于检索到的文档回答；第二层——Function Calling 的 Schema 校验，防止 LLM 传非法参数；第三层——质量检查节点，回答生成后再过一遍 LLM 自检，发现幻觉就重新生成。" |
| "怎么验证 Agent 变好了还是变差了？" | "我有 50-100 条测试集覆盖各类场景。每次改 Prompt 或者换模型，全量跑一遍，LLM-as-Judge 自动打分。我对比过 GPT-4o 和 Claude 在我们场景下的表现，Claude 在中文客服场景高 5 个百分点。" |
| "怎么处理并发请求？" | "Python 层用 FastAPI 的 async 处理 IO 密集型任务；Spring Boot 层用 SseEmitter 异步转发，不阻塞 Tomcat 线程池；LLM 请求如果过多，可以用 LiteLLM 做多模型路由和限流。" |
| "这个项目跟其他 AI 项目有什么不同？" | "大多数人的 AI 项目是一个功能加一个 Demo。我的项目是：多个 AI 功能有机整合 + 生产级 Java 后端 + 完整的评测体系。不是调 API，是做工程。" |

### 8.3 简历描述

```
跨境电商智能客服系统
├── 基于 LangGraph 构建 AI Agent 有向图，实现意图路由 + RAG 检索 + Function Calling
├── Function Calling 打通已有 Spring Boot 业务 API（订单、商品、退货），实现自然语言交互
├── 使用 ES 8 dense_vector 实现 Hybrid Search + Rerank 检索 Pipeline
├── SSE 流式输出 + Vue 3 打字机效果，优化用户体验
├── 建立 LLM-as-Judge 评测体系，50+ 测试集自动化回归
└── 技术栈：Python FastAPI + LangGraph + LangFuse + SSE + ES 8
```

---

## 九、项目实施计划

### Phase 1：AI 微服务骨架（第 1 周）

| 步骤 | 产出 | 可验证标准 |
|------|------|-----------|
| 1.1 Python FastAPI 项目初始化 | 可启动的 FastAPI 服务 | `curl localhost:8000/health` 返回 OK |
| 1.2 LLM 客户端封装 | 可调用 GPT-4o / Claude API | 接口返回正常回答 |
| 1.3 SSE 流式接口 | `POST /api/chat` 返回 SSE | 前端收到逐字输出 |
| 1.4 数据库 + MyBatis-Plus 实体 | ai_conversation 表可读写 | 增删改查正常 |

### Phase 2：RAG 检索（第 2 周）

| 步骤 | 产出 | 可验证标准 |
|------|------|-----------|
| 2.1 ES 8 dense_vector 索引创建 | ai_knowledge 索引就绪 | ES 返回 mapping 正确 |
| 2.2 Embedding + 文档写入 | 知识库文档写入 ES | 检索返回相关结果 |
| 2.3 Hybrid Search + Rerank | 混合检索 Pipeline | 对比纯 BM25 召回率提升 |
| 2.4 知识库管理 API | Spring Boot CRUD + ES 同步 | 前后端管理知识库 |

### Phase 3：Agent + Function Calling（第 3 周）

| 步骤 | 产出 | 可验证标准 |
|------|------|-----------|
| 3.1 LangGraph 意图识别路由 | Agent 根据意图分流 | 测试 3 类意图路由正确 |
| 3.2 Function Calling 工具定义 | 5 个工具可调用 Spring Boot API | Agent 查订单返回真实数据 |
| 3.3 质量检查节点 | 回答自检 + 重试 | 低质量回答自动改正 |
| 3.4 Spring Boot AiProxyController | SSE 转发 + 鉴权 | 全链路流式打通 |

### Phase 4：评测 + 优化（第 4 周）

| 步骤 | 产出 | 可验证标准 |
|------|------|-----------|
| 4.1 构建 50 条测试集 | ai_eval_testcase 表数据 | 覆盖 5 个分类 |
| 4.2 LLM-as-Judge 打分流水线 | 可运行评测 | 跑完一轮输出报告 |
| 4.3 LangFuse 接入 | Trace 链路可视化 | 每轮对话可追踪 |
| 4.4 前端反馈闭环 | 点赞/点踩 + SSE 打字机 | 完整 UX |

---

## 十、与现有项目的集成关系

```
现有模块                    AI 集成方式
────────────────────────────────────────────────────
mall-product               │  Function Calling: query_product / search_products
mall-order                 │  Function Calling: query_order / track_logistics
mall-order (refund)        │  Function Calling: check_return_eligibility
mall-search (ES)           │  扩展为 Hybrid Search + dense_vector 向量检索
mall-finance               │  NL2SQL: 销售分析（P1）
mall-security              │  AI 接口复用已有 JWT 鉴权
Vue 3 Frontend             │  新增 AI 聊天气泡组件（悬浮按钮）
```

---

> **设计原则总结**：
> 1. 不取代现有系统，而是作为 AI 增强层叠加上去
> 2. 所有 AI 功能都有评测指标可量化，不做凭感觉调 Prompt
> 3. 面试时能讲清楚"为什么"和"怎么衡量"，不只是"做了什么"

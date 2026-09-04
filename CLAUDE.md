# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

B2C 跨境电商全栈系统 — 覆盖 C 端用户前台 + B 端运营后台 + AI 智能客服的完整电商全栈项目。单模块 Spring Boot 单体应用，集成 RBAC 权限控制，AI 客服 Agent 采用双层架构（Java 业务层 + Python AI 微服务层）。

**Tech stack:** Java 23 + Spring Boot 3.2.5 + Spring Security + JWT + MyBatis-Plus 3.5.7 + MySQL 8 + Redis 7 + RabbitMQ + Elasticsearch 8 + Vue 3 (Composition API) + Ant Design Vue 4 + Vite 5 + Python 3.11 + FastAPI + LangGraph + LangFuse

> **Current status (2026-09-03):** The B-end operations console and C-end transaction APIs are implemented. The C-end user-facing frontend and AI implementation are not yet present; `mall-ai-service/` currently contains only package initializers. `mvn test` passes 26 tests and the `mall-web` production build passes. Known business gaps are tracked in `Doc/跨境电商全栈系统-实现流程文档.md`.

## 项目结构

```
├── pom.xml                          # 单模块 Maven 项目 (非多模块)
├── src/main/java/com/mall/
│   ├── common/                      # 共享基类、工具、配置
│   │   ├── config/                  #   MinIO / Redis / Rabbit / MyBatis-Plus / OpenAPI 配置
│   │   ├── entity/BaseEntity.java   #   公共实体基类 (id, createTime, updateTime, deleted)
│   │   ├── exception/               #   BusinessException + GlobalExceptionHandler
│   │   ├── result/                  #   Result<T> / ResultCode (统一响应)
│   │   └── service/FileService.java #   MinIO 文件上传服务
│   ├── security/                    # JWT 认证 + Spring Security + DataScope AOP
│   │   ├── annotation/DataScope.java
│   │   ├── aspect/DataScopeAspect.java
│   │   ├── jwt/                     #   JwtTokenProvider, JwtAuthenticationFilter
│   │   └── user/                    #   LoginUser, UserDetailsServiceImpl
│   ├── system/                      # 系统管理：用户 / 角色 / 菜单
│   ├── product/                     # 商品：SPU / SKU / 分类 / 品牌 / 库存
│   ├── order/                       # 订单生命周期 + 退款 / 物流 / MQ
│   ├── member/                      # 会员资料 / 地址 / 积分 / 收藏 / 浏览历史
│   ├── marketing/                   # 优惠券 / 活动 / 秒杀
│   ├── finance/                     # 结算单 / 税率 / EasyExcel 导出
│   ├── search/                      # ES 商品索引 + 全文搜索
│   ├── trade/                       # C 端交易：购物车 / 下单 / 支付 / 物流
│   └── web/                         # 启动入口 + 所有 Controller (薄层)
│       └── MallApplication.java
├── src/main/resources/
│   ├── application.yml              # 主配置 (MySQL / Redis / Rabbit / MinIO / JWT / ES / OpenAPI)
│   ├── application-dev.yml          # Dev 配置 (排除 Redis/Rabbit 自动配置)
│   ├── db/                          # SQL 脚本 (schema.sql / init-db.sql / data.sql)
│   └── mapper/                      # MyBatis XML (system/SysMenuMapper.xml 等)
├── src/main/java/com/mall/
│   └── ai/                          # AI 集成模块（Spring Boot → Python AI 代理层）
│       ├── controller/              #   SSE 聊天代理、知识库管理、评测接口
│       ├── service/                 #   对话管理、知识库 CRUD、评测服务
│       ├── entity/                  #   AiConversation / AiKnowledgeDoc / AiEval*
│       ├── mapper/                  #   MyBatis-Plus Mapper
│       └── dto/                     #   数据传输对象
├── src/main/resources/
│   └── db/                          # AI 相关 SQL (ai-schema.sql)
├── src/test/java/                   # 单元测试 (H2 内嵌数据库)
├── mall-web/                        # 前端 Vue 3 SPA (独立项目)
│   └── src/
│       ├── api/                     #   Axios API 层
│       ├── composables/             #   组合式函数 (useCrudModal, usePagination)
│       ├── layouts/AdminLayout.vue  #   后台布局
│       ├── router/                  #   Vue Router
│       ├── store/                   #   Pinia (user.js)
│       ├── utils/                   #   request.js (拦截器), date.js
│       └── views/                   #   页面: login / dashboard / system / product / order / member / marketing / finance
└── mall-ai-service/                 # Python AI 微服务（新增，独立项目）
```

> `com.mall.ai` 和下方 Python 文件树是 AI 阶段的规划目标；当前仓库尚未创建对应 Java 实现，`mall-ai-service/` 仅有 `app/` 下的空包初始化文件。

**mall-ai-service/ (Python AI 微服务规划目标):**
```
├── mall-ai-service/
│   ├── Dockerfile
│   ├── requirements.txt
│   ├── .env.example
│   └── app/
│       ├── __init__.py
│       ├── main.py                    # FastAPI 启动入口
│       ├── config.py                  # 配置中心
│       ├── agent/                     # LangGraph Agent 图编排
│       │   ├── agent.py               # Agent 有向图
│       │   ├── tools.py               # Function Calling 工具
│       │   └── prompts.py             # Prompt 模板
│       ├── rag/                       # RAG 检索
│       │   ├── retriever.py           # Hybrid Search + Rerank
│       │   └── embedding.py           # Embedding 封装
│       ├── api/                       # API 路由
│       │   ├── chat.py                # SSE 聊天
│       │   ├── knowledge.py           # 知识库管理
│       │   └── eval.py                # 评测接口
│       ├── models/
│       │   └── schemas.py             # Pydantic 模型
│       └── utils/
│           ├── llm.py                 # LLM 客户端
│           └── tracing.py             # LangFuse 追踪
```

**Layering within each business package:** `entity/` → `mapper/` → `service/` → (in `web`) `controller/`

## Build & Run Commands

```bash
# 构建项目 (从项目根目录)
mvn clean package

# 跳过测试构建
mvn clean package -DskipTests

# 运行所有测试
mvn test

# 运行单个测试类
mvn test -Dtest=OrderServiceImplTest

# 启动应用
mvn spring-boot:run

# 启动应用 (dev profile，排除 Redis/RabbitMQ)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 前端开发服务器 (在 mall-web/ 目录下)
cd mall-web
npm run dev     # Vite dev server → http://localhost:5173
npm run build   # 生产构建 → dist/

# Python AI 微服务 (在 mall-ai-service/ 目录下)
cd mall-ai-service
pip install -r requirements.txt          # 安装依赖
uvicorn app.main:app --reload --port 8000  # 启动 AI 服务
```

**Maven wrapper** 已配置 (`.mvn/`)，支持 `./mvnw` 命令。

## Infrastructure Dependencies

本地开发需要以下服务（默认端口）：

| Service | Port | Purpose |
|---|---|---|
| MySQL 8 | 3306 | 核心业务数据 |
| Redis 7 | 6379 | 缓存、分布式锁、购物车 |
| RabbitMQ 3.x | 5672/15672 | 异步订单处理、库存削峰 |
| Elasticsearch 8 | 9200 | 商品全文搜索 + AI 知识库向量检索 |
| MinIO | 9000/9001 | 文件存储（商品图片、报关文档） |

Dev profile (`application-dev.yml`) 会排除 Redis 和 RabbitMQ 的自动配置，方便本地开发。

**AI 微服务** (`mall-ai-service/`) 为独立 Python FastAPI 服务，与 Spring Boot 通过 HTTP/SSE 通信，各自独立部署。

## 业务模块职责

| 包 | 职责 |
|---|---|
| `common` | `BaseEntity`, `Result`/`ResultCode`, `BusinessException`, `GlobalExceptionHandler`, 基础配置 |
| `security` | JWT 认证、Spring Security 配置、`@DataScope` 注解 + AOP 行级数据权限 |
| `system` | 系统管理：用户/角色/菜单 CRUD、分配角色、修改状态 |
| `product` | SPU/SKU、分类树、品牌、多仓库库存、HS 编码 |
| `order` | 订单生命周期(创建→支付→发货→签收→完成)、退款、物流、RabbitMQ 异步处理 |
| `member` | 会员资料、收货地址、积分记录、收藏夹、浏览历史 |
| `marketing` | 优惠券(定义/发放/核销)、活动、秒杀 |
| `finance` | 结算单、税率配置、EasyExcel 导出 |
| `search` | Elasticsearch 商品索引、全文搜索、定时同步任务 |
| `trade` | **C 端交易中心**：购物车 CRUD、下单(库存校验+金额计算+锁库存)、支付回调、物流追踪 |
| `web` | Spring Boot 入口、所有 Controller（B 端 + C 端）、application.yml、MyBatis XML mapper |
| `ai` | AI 集成：SSE 聊天代理转发、知识库管理、评测接口（Java 代理层） |

**AI 微服务 (`mall-ai-service/`):** Python FastAPI + LangGraph Agent 编排、RAG 混合检索 (Hybrid Search + Rerank)、Function Calling 调 Spring Boot API、LLM-as-Judge 评测体系

**数据权限模型:** `@DataScope` 注解 + AOP 根据用户角色注入 SQL 过滤（店长可见全部，运营可见本分类，客服可见分配订单，财务只读）

**角色层级:** 店长（完全访问）→ 运营（商品/分类/营销，无删除/定价）→ 客服（订单/物流，不可改商品）→ 财务（只读结算单/导出）

**前端 (`mall-web/`):** Vue 3 + Ant Design Vue 4 + Pinia + Vue Router + Axios，按角色动态路由

## 测试

使用 H2 内嵌数据库，测试类位于 `src/test/java/`：

- `OrderServiceImplTest` — 订单服务
- `RefundServiceImplTest` — 退款服务
- `CouponServiceImplTest` — 优惠券服务
- `SkuServiceImplTest` — SKU 服务

## Key Design Documents

- **`Doc/跨境电商全栈系统-项目概要文档.md`** — 项目定位 + 三大子系统 + 技术架构 + 面试亮点（面试核心参考资料）
- **`Doc/跨境电商全栈系统-需求设计文档.md`** — 全栈需求规格：B端 + C端 + AI、角色体系、API 设计、数据库总览
- **`Doc/跨境电商C端架构设计方案.md`** — C 端业务流程、订单状态流转、API 设计、数据库设计
- **`Doc/AI客服Agent-跨境电商智能助手设计方案.md`** — AI 客服 Agent 架构设计：双层架构、LangGraph 工作流、RAG 检索、评测体系
- **`Doc/跨境电商全栈系统-实现流程文档.md`** — 剩余工作实施路线图：4 个 Phase、依赖关系、每步验证标准
- **`Doc/2026-05-28-b2c-cross-border-ecommerce-implementation.md`** — 早期实施计划（历史参考）

## CodeGraph

This project has a CodeGraph MCP server (`codegraph_*` tools) configured. CodeGraph is a tree-sitter-parsed knowledge graph of every symbol, edge, and file.

### When to prefer codegraph over native search

Use codegraph for **structural** questions — what calls what, what would break, where is X defined. Use native grep/read only for **literal text** queries (string contents, comments, log messages).

| Question | Tool |
|---|---|
| "Where is X defined?" / "Find symbol named X" | `codegraph_search` |
| "What calls function Y?" | `codegraph_callers` |
| "What does Y call?" | `codegraph_callees` |
| "How does X reach Y? / trace the flow from X to Y" | `codegraph_trace` |
| "What would break if I changed Z?" | `codegraph_impact` |
| "Show me Y's signature / source / docstring" | `codegraph_node` |
| "Give me focused context for a task/area" | `codegraph_context` |
| "See several related symbols' source at once" | `codegraph_explore` |
| "What files exist under path/" | `codegraph_files` |
| "Is the index healthy?" | `codegraph_status` |

### Rules of thumb

- **Answer directly — don't delegate exploration.** For "how does X work" / architecture, answer with 2-3 codegraph calls: `codegraph_context` first, then ONE `codegraph_explore`. For flow from X to Y, `codegraph_trace` is one call.
- **Trust codegraph results** — they come from AST parse. Don't re-verify with grep.
- **Don't grep first** when looking up a symbol by name — `codegraph_search` is faster.
- **Index lag:** the file watcher debounces ~500ms behind writes; don't re-query immediately after editing.

## 工作约束

1. **语言**：所有回答使用中文。
2. **容错停止**：任何工具调用连续失败超过 3 次后，停止工作并报告错误原因，不要继续重试。
3. **大文件写入**：当写入内容过多时，分批次写入：
   - 首次创建文件并写入前半部分内容
   - 后续使用追加模式写入剩余内容
   - 每次写入不超过 100 行代码
4. **Skill / Plugin 抉择**：每次开始工作前，主动判断当前任务适合调用哪些 skill 和 MCP plugin，优先使用已有工具而非从头手写。

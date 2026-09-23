# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

B2C 跨境电商全栈系统 — 覆盖 C 端用户前台 + B 端运营后台 + AI 智能客服的完整电商全栈项目。单模块 Spring Boot 单体应用，集成 RBAC 权限控制，AI 客服 Agent 采用双层架构（Java 业务层 + Python AI 微服务层）。

**Tech stack:** Java 23 + Spring Boot 3.2.5 + Spring Security + JWT + MyBatis-Plus 3.5.7 + MySQL 8 + Redis 7 + RabbitMQ + Elasticsearch 8 + Vue 3 (Composition API) + Ant Design Vue 4 + Vite 5 + Python 3.11 + FastAPI + LangGraph + optional OpenTelemetry/LangFuse

> **Current status (2026-09-17):** The B-end operations console, C-end transaction MVP, AI customer-service P0 (SSE, retrieval, read-only tools, audit, and human handoff), and the C-end responsive storefront are implemented. `mall-storefront/` uses Vue 3 + Vant: mobile single-column + bottom navigation at ≤680px and desktop top navigation + product grid at ≥1025px; `mock-server.mjs` remains a backend-free preview tool. Runtime architecture, ports, startup, verification, and gaps are tracked in `Doc/当前实现基线与文档口径.md` and `Doc/B端与C端功能差距审计.md`.

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
│   ├── security/                    # JWT 认证 + Spring Security
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
│       ├── controller/              #   SSE 聊天、知识库、人工工单接口
│       ├── service/                 #   对话、知识库、人工工单与审计服务
│       ├── entity/                  #   AiConversation / AiSupportTicket / AiAuditLog
│       ├── mapper/                  #   MyBatis-Plus Mapper
│       └── dto/                     #   数据传输对象
├── src/main/resources/
│   └── db/migration/                 # Flyway migrations, including AI conversation/ticket/audit tables
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

> `com.mall.ai` 和下方 Python 文件树曾是 AI 阶段规划目标；当前仓库已存在 Java SSE 网关、会话/审计/工单服务和 Python 对话/RAG P0 实现，模型 tool plan 与离线评测也已接入；生产级模型、标注质量闭环和多实例治理仍未完成。

**mall-ai-service/ (Python AI 微服务当前结构):**
```
├── mall-ai-service/
│   ├── Dockerfile
│   ├── requirements.txt
│   ├── .env.example
│   └── app/
│       ├── __init__.py
│       ├── main.py                    # FastAPI 启动入口
│       ├── config.py                  # 配置中心
│       ├── agent/                     # LangGraph 本地兜底和只读工具定义
│       │   ├── agent.py
│       │   └── tools.py
│       ├── rag/                       # BM25、可选向量/RRF、可选 Rerank
│       │   ├── retriever.py
│       │   └── embedding.py
│       ├── api/                       # /internal/chat、tool-plan、knowledge
│       │   ├── chat.py
│       │   └── knowledge.py
│       ├── observability.py           # 本地 SLA 窗口指标
│       └── telemetry.py               # 可选 OTel/LangFuse OTLP
│   ├── evaluate.py                    # 离线策略/RAG/Rerank/可选 Judge 评测
│   └── tests/                         # pytest 回归
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
npm run dev     # Vite dev server → http://localhost:18103
npm run build   # 生产构建 → dist/

# Python AI 微服务 (在 mall-ai-service/ 目录下)
cd mall-ai-service
pip install -r requirements.txt          # 安装依赖
uvicorn app.main:app --reload --port 8101  # 直启 AI 服务；Compose 映射到宿主机 18101
```

**Maven wrapper** 已配置 (`.mvn/`)，支持 `./mvnw` 命令。

## Infrastructure Dependencies

本地开发需要以下服务（默认端口）：

| Service | Port | Purpose |
|---|---|---|
| MySQL 8 | 3306 | 核心业务数据 |
| Redis 7 | 16379（宿主机）/ 6379（容器） | 缓存、分布式锁、购物车 |
| RabbitMQ 3.x | 18110/18111（宿主机）/ 5672/15672（容器） | 异步订单处理、库存削峰 |
| Elasticsearch 8 | 19200（宿主机）/ 9200（容器） | 商品全文搜索 + AI 知识库检索 |
| MinIO | 18120/18121（宿主机）/ 9000/9001（容器） | 文件存储（商品图片、报关文档） |

Dev profile (`application-dev.yml`) 会排除 Redis 和 RabbitMQ 的自动配置，方便本地开发。

**AI 微服务** (`mall-ai-service/`) 为独立 Python FastAPI 服务，与 Spring Boot 通过 HTTP/SSE 通信，各自独立部署。

## 业务模块职责

| 包 | 职责 |
|---|---|
| `common` | `BaseEntity`, `Result`/`ResultCode`, `BusinessException`, `GlobalExceptionHandler`, 基础配置 |
| `security` | JWT 认证、Spring Security 配置、方法级权限 |
| `system` | 系统管理：用户/角色/菜单 CRUD、分配角色、修改状态 |
| `product` | SPU/SKU、分类树、品牌、基础库存与 HS 编码 |
| `order` | 订单生命周期(创建→支付→发货→签收→完成)、退款、物流、RabbitMQ 异步处理 |
| `member` | 会员资料、收货地址、积分记录、收藏夹、浏览历史 |
| `marketing` | 优惠券(定义/发放/核销)、活动、秒杀 |
| `finance` | 结算单、税率配置、EasyExcel 导出 |
| `search` | Elasticsearch 商品索引、全文搜索、定时同步任务 |
| `trade` | **C 端交易中心**：购物车 CRUD、下单(库存校验+金额计算+锁库存)、支付回调、物流追踪 |
| `web` | Spring Boot 入口、所有 Controller（B 端 + C 端）、application.yml、MyBatis XML mapper |
| `ai` | AI 集成：SSE 聊天、知识库、只读工具规划、审计和人工工单（P0 已实现） |

**AI 微服务 (`mall-ai-service/`):** 当前为 Python FastAPI + LangGraph 本地兜底；支持 BM25、可选 Embedding/RRF、可选 Cohere 兼容 Rerank、OpenAI-compatible tool plan 和离线评测。生产级模型、标注评测、外部 OTel/LangFuse 后端仍依赖环境配置和后续治理。

**数据权限模型:** 方法级 `@PreAuthorize` 已生效；通用行级权限尚未实现，人工客服工单写操作通过条件 SQL 限定认领人。

**角色层级:** 店长（完全访问）→ 运营（商品/分类/营销，无删除/定价）→ 客服（订单/物流，不可改商品）→ 财务（只读结算单/导出）

**前端 (`mall-web/`):** Vue 3 + Ant Design Vue 4 + Pinia + Vue Router + Axios，按角色动态路由

## 测试

使用 H2 内嵌数据库，测试类位于 `src/test/java/`；当前工作树 `mvn test` 已通过 132/132。Python 测试和离线评测需要安装 `mall-ai-service/requirements.txt` 依赖，容器验证需要 Docker Desktop。

- `OrderServiceImplTest` — 订单服务
- `RefundServiceImplTest` — 退款服务
- `CouponServiceImplTest` — 优惠券服务
- `SkuServiceImplTest` — SKU 服务

## Key Design Documents

- **`Doc/当前实现基线与文档口径.md`** — 当前代码、目录、端口、启动方式和文档权威顺序（优先阅读）
- **`Doc/B端与C端功能差距审计.md`** — 当前实现与完整 B/C 端需求的差距及 P0/P1/P2 排期
- **`Doc/C端交易闭环实施说明.md`** — C 端购买闭环 MVP 的接口、边界和运行方式
- **`Doc/跨境电商全栈系统-项目概要文档.md`** — 项目定位 + 三大子系统 + 技术架构 + 面试亮点（面试核心参考资料）
- **`Doc/跨境电商全栈系统-需求设计文档.md`** — 全栈需求规格：B端 + C端 + AI、角色体系、API 设计、数据库总览
- **`Doc/跨境电商C端架构设计方案.md`** — C 端业务流程、订单状态流转、API 设计、数据库设计
- **`Doc/AI客服Agent-跨境电商智能助手设计方案.md`** — AI 客服 Agent 架构设计：双层架构、LangGraph 工作流、RAG 检索、评测体系
- **`Doc/跨境电商全栈系统-实现流程文档.md`** — 剩余工作实施路线图：4 个 Phase、依赖关系、每步验证标准
- **`Doc/AI客服Agent-跨境电商智能助手实施方案.md`** — AI 后续实施阶段、验收、可靠性和安全边界
- **`Doc/CBEC-代码导读地图.md`** — 按阅读顺序理解关键代码和面试重点
- **`Doc/CBEC-简历与面试要点.md`** — 基于当前代码证据的项目表述和面试问答
- **`Doc/CBEC-交接文档-Codex-20260906.md`** — 2026-09-06 的历史交接快照，不替代当前基线

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

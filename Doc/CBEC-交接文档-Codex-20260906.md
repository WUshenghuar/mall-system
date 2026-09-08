# CBEC — C 端响应式改造交接文档（给 Codex 的推进上下文）

> 生成时间：2026-09-06。本文汇总本次会话已完成的工作、当前仓库状态和后续推进方向，供 Codex 无缝接手。

## 1. 本次会话做了什么

### 1.1 代码改造：C 端桌面自适应（响应式）
- 目标：同一套 `mall-storefront/` 代码，按浏览器宽度自适应——手机打开是移动布局，电脑打开是桌面布局。
- 实现方式：**移动端 CSS 一行未动**，仅做三件事：
  1. `src/App.vue`：把底部 `nav.app-nav` 移入 `header.app-header`（DOM 复用：移动端仍是 fixed 底部导航，桌面端经 CSS 转为顶栏横向导航）。
  2. `src/styles.css`：末尾追加 `@media (min-width: 1025px)` 桌面断点块——`.shell` 上限放宽到 1280px、`.app-header` 横排、`.app-nav` 顶栏化、`.page` 加内边距、`.product` 卡片化；另把 `.app-header a` 选择器收紧为 `.app-header a.brand`（避免误伤 header 内的导航链接）。
  3. `src/views/CatalogView.vue`：商品列表外包 `<div class="product-grid">`，桌面断点下用 `repeat(auto-fill, minmax(270px,1fr))` 形成多列卡片网格。
  4. 三个带底部留白 scoped 样式页（`OrderDetailView.vue`、`RefundView.vue`、`ActivityView.vue`）各补了一条 `@media (min-width:1025px)` 覆盖（桌面去掉 88px 底部留白、内容限宽居中）。
- 断点口径：≤680px 移动单列+底部导航（原样）／681–1024px 保持既有居中圆角卡片／≥1025px 桌面顶栏+多列网格+1280px 限宽。C 端根容器 `.shell` 原本就是 `max-width:680px` 的居中移动壳，桌面“模拟手机”观感正源于此，本次只放开桌面断点。

### 1.2 新增本地工具（不参与后端）
- `mall-storefront/mock-server.mjs`：无后端 mock API（监听 18100，返回商品/购物车/订单/地址等假数据），供本地预览 C 端页面，不依赖 Java 后端与 MySQL。
- `desktop-preview-shots.mjs`、`mobile-preview-shots.mjs`（仓库根）：复用 CDP（Chrome DevTools Protocol）截图方法，分别以 1440×900（桌面）与 375×812（手机）视口对主要路由截图。
- 验证结果：桌面 1440px 截图 6 张、手机 375px 截图 5 张均已核对——桌面呈现顶栏导航+4 列商品网格+居中卡片；手机 nav 实测 `position:fixed, y:752, h:60`，底边正好贴视口 812，与改造前行为一致。

### 1.3 文档同步（已随代码口径更新）
- `CLAUDE.md`：Current status 更新为 “mobile-first responsive … ≥1025px desktop layout”，并提及 `mock-server.mjs`。
- `Doc/当前实现基线与文档口径.md`：架构图 storefront 一行、C 端前台能力描述、本轮验证口径三处同步。
- `Doc/B端与C端功能差距审计.md`：C 端缺口第 10 条由“移动端响应式基础…验收未完成”改为“两套布局已验收；仍需补齐触控/悬停细节、性能指标、真机回归”。
- `Doc/C端交易闭环实施说明.md`：交付描述与“下一轮优先事项”同步。
- `Doc/跨境电商C端架构设计方案.md`：落地架构修订段同步（历史多模块设计段未动）。
- `Doc/跨境电商全栈系统-项目概要文档.md`：状态快照与“三、C 端用户前台”章节同步为 2026-09-06 口径。

## 2. 当前仓库状态

- 分支 `master`，最近提交 `e5c3735 fix(web): improve authentication error feedback`。
- **本次改造相关改动尚未提交**，全部停留在工作区（见下方清单）。
- 需注意：工作区另有**会话开始前就存在的本地改动**（Java/搜索/测试/mall-web vite 配置），不属于本次范围，提交时请按文件区分：
  - 本次范围：`CLAUDE.md`、`Doc/` 下 5 份、`mall-storefront/src/**`、`mall-storefront/mock-server.mjs`、`desktop-preview-shots.mjs`、`mobile-preview-shots.mjs`。
  - 会话前已有：`mall-web/vite.config.js`、`src/main/java/com/mall/product/**`、`src/main/java/com/mall/search/**`、`src/test/java/com/mall/search/`、`mall-storefront/screenshots/`。
- 后台当前运行中：mock 8080（任务可忽略）、vite dev 5174（已占用）；如 Codex 要继续预览可复用，端口冲突时先停旧进程。

## 3. 给 Codex 的推进建议（优先级从高到低）

1. **先提交本次改动**（建议拆两个 commit：① storefront 响应式改造+mock+截图脚本；② 文档同步），避免与既有本地改动混淆。
2. **681–1024px 中间视口**（iPad 横屏/小桌面）：目前复用旧居中圆角卡，可评估是否需要第三套断点或保持现状。
3. **C 端“像电商”补强**（对简历价值最高）：给商品加封面图（placeholder 即可）、补商品详情视图、价格与加购联动——mock-server 与 storefront 均支持快速迭代。
4. **移动真机回归与触控/悬停细节**：补差异（桌面 hover、移动 touch），并跑一次真实后端 + 真机/浏览器 DevTools 的多视口回归。
5. **后端继续推进时**：注意 `src/main/java/com/mall/search/**` 等会话前改动是否与本仓库当前实现冲突，先 `git diff` 审阅再继续。

## 4. 快速验证命令

```bash
# 无后端本地预览（两个终端）
cd mall-storefront && node mock-server.mjs   # :18100 mock
cd mall-storefront && npm run dev            # :5174 dev server，浏览器拖宽验证自适应

# 截图验证（需要 Edge headless 实例，参见两个 *.mjs 顶部说明）
node desktop-preview-shots.mjs
node mobile-preview-shots.mjs

# 回退本次代码改造（如不需要）
git checkout -- mall-storefront/src
```

## 5. 关键文件速查

| 文件 | 状态 |
|---|---|
| `mall-storefront/src/App.vue` | 改：nav 移入 header |
| `mall-storefront/src/styles.css` | 改：追加桌面断点 + 选择器收紧 |
| `mall-storefront/src/views/CatalogView.vue` | 改：`.product-grid` 包装 |
| `mall-storefront/src/views/OrderDetailView.vue` / `RefundView.vue` / `ActivityView.vue` | 改：桌面留白/限宽覆盖 |
| `mall-storefront/mock-server.mjs` | 新增：无后端 mock |
| `desktop-preview-shots.mjs` / `mobile-preview-shots.mjs` | 新增：截图验证 |
| `CLAUDE.md` + `Doc/` 5 份 | 改：口径同步 |

## 6. AI Agent 技术栈缺口核对（2026-09-06，以代码为准）

> 目标：把 C 端悬浮客服 + AI Agent 做成可运行的"对话闭环 → RAG → 工具 Agent → 安全治理"四阶段。以下结论逐层核对代码后得出，非文档规划。

### 6.1 已有（可复用，无需新增基础设施）

- Java 后端地基完整：Spring Boot 3.2.5 + Security/JWT + MyBatis-Plus + MySQL + Redis + RabbitMQ + Elasticsearch 8（`spring-boot-starter-data-elasticsearch` 已在 pom），商品搜索已用 `ik_smart` 分词。
- B/C 前端（Vue 3 + axios + Pinia + Router）可扩展 SSE 消费；浏览器原生 `EventSource`/fetch stream 即可，无需新 npm 包。
- AI 规划文档齐备：`Doc/AI客服Agent-跨境电商智能助手设计方案.md`（目标架构）与 `Doc/AI客服Agent-跨境电商智能助手实施方案.md`（分阶段落地）。

### 6.2 缺口：三层实现代码全部为零

| 层 | 现状 | 需要新建 |
|---|---|---|
| Python AI 服务 `mall-ai-service/` | 仅空骨架（app/agent、api、models、rag、utils、tests 下只有 `__init__.py`/`.gitkeep`）；**无 requirements.txt / main.py / 配置** | `main.py`、`config.py`、LLM 客户端、LangGraph Agent 图、RAG、SSE 路由、LangFuse 埋点；依赖清单：fastapi、uvicorn、sse-starlette、langgraph、langchain、elasticsearch、litellm 或 openai、langfuse |
| Java AI 模块 `src/main/java/com/mall/ai/` | 仅空包（controller/dto/entity/mapper/service 全是 .gitkeep） | `AiChatController`（`POST /api/ai/chat` SSE）、`AiChatService`（鉴权+转发+会话落库）、Entity/Mapper；需在 pom 增加 HTTP 客户端依赖（RestClient/WebClient）用于转发 Python |
| 前端入口 `mall-storefront/` | 无任何 chat/SSE 引用 | 悬浮客服气泡组件（各页右下角 + 关键页主动唤起）、SSE 消费、流式打字机 UI |

### 6.3 缺口：数据与外部依赖

- 数据库：db 迁移脚本仅到 `V5__add_trade_refund.sql`，**无任何 ai_ 表**；需新增 V6+：`ai_conversation`、`ai_agent_checkpoint`、`ai_knowledge_doc`、`ai_audit_log`（建表 SQL 见设计方案文档第五章）。
- ES：`dense_vector` 能力未启用；需建 `ai_knowledge` 索引（BM25 + 向量双路）——RAG 阶段再做，可后置。
- LLM API Key：application.yml 与 mall-ai-service 均无模型配置；需接入 DeepSeek/OpenAI 等（DeepSeek 成本最低），敏感配置走环境变量不入库。
- 可选外部件：Embedding API（RAG 阶段）、LiteLLM 网关（多模型路由，可省）、LangFuse（观测/评测，P0 后期）。

### 6.4 建议推进顺序（增量可验证）

1. **对话闭环**（半天~1 天）：FastAPI + `/chat` SSE + 单模型直连 → Java `AiChatController` 透传 → storefront 悬浮气泡。验收：登录用户能多轮流式对话。
2. **RAG**（2~3 天）：建表/建索引 → 文档分段 → Embedding → BM25+向量召回 + Rerank。验收：政策/商品类问题返回带来源。
3. **工具 Agent**（2~3 天）：LangGraph 意图路由 → Function Calling 回调 Spring Boot 已有 API（订单/商品/物流/退货查询）。验收："查我的订单"返回真实数据。
4. **安全治理 + 评测**（3~4 天）：Prompt 注入检测、越权校验、高风险操作确认、人工转接、审计；50+ 测试集 + LLM-as-Judge。

> 提醒：`mall-ai-service/` 的规划目录（agent/api/rag/utils）与设计文档对齐，可直接按目录填充代码，避免重构。

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

B2C Cross-Border E-Commerce backend management system — a modular monolith covering product (SPU/SKU), order, member, marketing/coupon, and finance modules with RBAC access control.

**Tech stack:** Java 23 + Spring Boot 3.x + Spring Security + JWT + MyBatis-Plus 3.5 + MySQL 8 + Redis 7 + RabbitMQ + Elasticsearch 8 + Vue 3 (Composition API) + Ant Design Vue + Vite

> **Current state (2026-05-31):** Early implementation phase. The multi-module Maven structure under `mall-system/` is not yet created — code scaffolding is the first task. The root `pom.xml` is a placeholder; the real parent POM will live at `mall-system/pom.xml`.

## Build & Run Commands

```bash
# Build the whole project (from mall-system/ once created)
cd mall-system && mvn clean package

# Build skipping tests
mvn clean package -DskipTests

# Run all tests
mvn test

# Run a single test class in a specific module
mvn test -pl mall-product -Dtest=SpuServiceTest

# Start the application (from mall-system/ directory)
mvn spring-boot:run -pl mall-web

# Start with dev profile
mvn spring-boot:run -pl mall-web -Dspring-boot.run.profiles=dev

# Install Maven wrapper (if mvnw not available — .mvn/wrapper/ JAR is not yet checked in)
mvn wrapper:wrapper
```

## Infrastructure Dependencies

Local development requires these services (default ports):

| Service | Port | Purpose |
|---|---|---|
| MySQL 8 | 3306 | Core business data |
| Redis 7 | 6379 | Caching, distributed locks, cart |
| RabbitMQ 3.x | 5672/15672 | Async order processing, stock peak-clipping |
| Elasticsearch 8 | 9200 | Product full-text search |
| MinIO | 9000/9001 | File storage (product images, customs docs) |

## Architecture

**Module structure (Maven multi-module under `mall-system/`):**

| Module | Concern |
|---|---|
| `mall-common` | Shared: `BaseEntity`, `Result`/`ResultCode`, `BusinessException`, `GlobalExceptionHandler`, MyBatis-Plus config |
| `mall-security` | JWT auth, Spring Security config, `@DataScope` annotation + AOP for row-level data permission |
| `mall-product` | SPU/SKU, categories (tree), brands, multi-warehouse stock, multi-currency pricing, HS codes |
| `mall-order` | Order lifecycle (create→pay→ship→sign→complete), refunds, international logistics, RabbitMQ async handling |
| `mall-member` | Member profiles, tier system, points, international addresses |
| `mall-marketing` | Coupons (define/issue/verify), activities, flash-sale/seckill |
| `mall-finance` | Statements, tax config, exchange rates, Excel export |
| `mall-search` | Elasticsearch product indexing, full-text search, scheduled sync jobs |
| `mall-web` | Spring Boot entry point (`MallApplication`), all controllers, `application.yml`, MyBatis XML mappers |

**Layering within each business module:** `controller/` → `service/` → `mapper/` → `entity/`

**Data permission model:** `@DataScope` annotation on controller methods + AOP aspect injects SQL filtering based on user's role (store manager sees all, ops sees own category, CS sees assigned orders, finance is read-only).

**Role hierarchy:** Store Manager (full access) → Operations (product/category/marketing, no delete/pricing) → CS (orders/logistics, no product modification) → Finance (read-only statements/export).

**Frontend** (separate `mall-web/` frontend project): Vue 3 SPA with dynamic routing by role, Pinia state, Element Plus components, Vite build.

## Key Design Documents

- `B2C 跨境电商后台管理系统 — 需求设计文档.md` — full requirements spec: roles, modules, DB schema, API design
- `2026-05-28-b2c-cross-border-ecommerce-implementation.md` — phased implementation plan with task checklist

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

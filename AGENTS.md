# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Project Overview

B2C cross-border e-commerce full-stack system: a single-module Spring Boot backend plus a Vue 3 B-end operations console. The backend is organized by domain packages (`product`, `order`, `trade`, `member`, `marketing`, `finance`, `search`, `system`) and protected by JWT/RBAC.

**Tech stack:** Java 23 + Spring Boot 3.2.5 + Spring Security + JWT + MyBatis-Plus 3.5.7 + MySQL 8 + Redis 7 + RabbitMQ + Elasticsearch 8 + MinIO + Vue 3 + Ant Design Vue 4 + Vite

> **Current state (2026-09-03):** The root `pom.xml` is the active single-module build. B-end management features and C-end transaction APIs are implemented; the C-end frontend and AI service remain planned. `mvn test` passes 26 tests and `mall-web` production build passes.

## Build & Run Commands

```bash
# Build the backend from the repository root
mvn clean package

# Build skipping tests
mvn clean package -DskipTests

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=SkuServiceImplTest

# Start the application from the repository root
mvn spring-boot:run

# Start with dev profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Install Maven wrapper (if mvnw not available — .mvn/wrapper/ JAR is not yet checked in)
mvn wrapper:wrapper
```

## Infrastructure Dependencies

Local development requires these services (default ports):

| Service | Port | Purpose |
|---|---|---|
| MySQL 8 | 3306 | Core business data |
| Redis 7 | 16379 (宿主机) / 6379 (容器) | Caching, distributed locks, cart |
| RabbitMQ 3.x | 5672/15672 | Async order processing, stock peak-clipping |
| Elasticsearch 8 | 9200 | Product full-text search |
| MinIO | 9000/9001 | File storage (product images, customs docs) |

## Architecture

**Domain package structure inside the single Maven module:**

| Module | Concern |
|---|---|
| `common` | Shared entities, results, exceptions, infrastructure configuration |
| `security` | JWT auth, Spring Security config, `@DataScope` scaffold |
| `product` | SPU/SKU, categories, brands, and stock |
| `order` | B-end order lifecycle, refunds, logistics, RabbitMQ handling |
| `trade` | C-end cart, order creation, payment records, and logistics APIs |
| `member` | Member profiles, addresses, points, favorites, browse history |
| `marketing` | Coupons, activities, and flash-sale/seckill configuration |
| `finance` | Statements, tax config, and Excel export |
| `search` | Elasticsearch product search and index jobs |
| `web` | Spring Boot entry point (`MallApplication`) and all controllers |

**Layering within each business module:** `controller/` → `service/` → `mapper/` → `entity/`

**Data permission model:** `@DataScope` and its AOP context exist, but the generated SQL is not yet consumed by MyBatis queries; treat row-level permissions as incomplete. Method-level `@PreAuthorize` permissions are active.

**Role hierarchy:** Store Manager (full access) → Operations (product/category/marketing, no delete/pricing) → CS (orders/logistics, no product modification) → Finance (read-only statements/export).

**Frontend** (separate `mall-web/` project): Vue 3 SPA with dynamic routing by role, Pinia state, Ant Design Vue components, and Vite build. It currently contains the B-end console; C-end pages are not implemented.

## Key Design Documents

- `Doc/跨境电商全栈系统-需求设计文档.md` — full requirements spec: roles, modules, DB schema, API design
- `Doc/跨境电商全栈系统-实现流程文档.md` — phased implementation plan and current status snapshot

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

# 咔嗒造物智能客服 Agent 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为咔嗒造物商城（KlackCraft）落地跨栈强隔离的智能客服 Agent 系统，覆盖闲聊导购、热销/高浏览推荐与聊天内确认下单、订单发货与《消保法》/商城规范售后咨询、异步工单转人工及兜底澄清五大核心能力。

**Architecture:** Java 服务端（`sz-boot-parent`）作为唯一公网门面，复用 Sa-Token 双端鉴权后清洗伪造头并透传 `/client/cs/**` 与 `/admin/cs/**` 至内网 Python 微服务 `sz-agent`，同时向内网暴露带权属二次校验的 `/internal/agent/**` 商城接口。Python 侧（FastAPI + LangChain v1 `create_agent` + LangGraph + SQLAlchemy 2.0 + PostgreSQL 16 + Milvus + MinerU）全量持久化会话、消息、工单、知识库元信息与 LangGraph Checkpoint，并以显式主图路由与专业子图完成安全编排与下单 `interrupt` 中断恢复。

**Tech Stack:** Python 3.12+ (`uv`)、FastAPI、LangChain `>=1.0` (`create_agent`, `context_schema`, `ToolRuntime`)、LangGraph + `langgraph-checkpoint-postgres` (`AsyncPostgresSaver`)、DeepSeek (`langchain-deepseek`, `deepseek-chat`)、阿里 Qwen Embedding (`text-embedding-v4`, 1024 维)、MinerU (`mineru-open-sdk`)、SQLAlchemy 2.0 + Alembic + `asyncpg`、Milvus Standalone (`pymilvus`)、Java 21 + Spring Boot 3.5.5 + Sa-Token + MyBatis-Flex + Liquibase、Vue 3 + TypeScript + Element Plus。

**Spec:** [docs/superpowers/specs/2026-11-26-kada-cs-agent-design.md](file:///Users/car/Project/library_system/docs/superpowers/specs/2026-11-26-kada-cs-agent-design.md)

---

## Global Constraints

- **Python 基线与包管理**：`sz-agent/` 使用 `uv` 管理 Python `>=3.12`，核心依赖含 `fastapi`, `uvicorn[standard]`, `pydantic-settings`, `httpx`, `sqlalchemy[asyncio]`, `asyncpg`, `alembic`, `psycopg[binary]`, `psycopg-pool`, `langchain>=1.0`, `langgraph`, `langgraph-checkpoint-postgres`, `langchain-deepseek`, `langchain-openai`, `pymilvus`, `mineru-open-sdk`, `pypdf`, `reportlab`, `pytest`, `pytest-asyncio`, `aiosqlite`。
- **模型与 Embedding 固定参数**：LLM 统一走 `ChatDeepSeek(model="deepseek-chat")`（路由/结构化提取 `temperature=0.0`，导购/闲聊 `0.3~0.6`）；Embedding 统一走 DashScope OpenAI 兼容接口 `OpenAIEmbeddings(model="text-embedding-v4", dimensions=1024, encoding_format="float", check_embedding_ctx_length=False)`。
- **Java 服务路径与环境配置**：`sz-service-admin` 的 `application-dev.yml` 与 `application-local.yml` 配置了 `server.servlet.context-path: /api`（端口 `9991`），因此 `JAVA_INTERNAL_BASE_URL` 本地默认值为 `http://127.0.0.1:9991/api`；代理/内网密钥在 `sz-boot-parent/config/{dev,local,preview,prod}/agent.yml` 通过 `AGENT_INTERNAL_SECRET` 与 `AGENT_BASE_URL` 注入。
- **跨栈安全隔离**：`sz-agent` 仅绑定内网地址（默认 `127.0.0.1:8000`）；Java ↔ Python 双向强制校验 `X-Internal-Secret`（常量时间比较）；Java 透传前剥离外部传入的 `X-Internal-Secret`、`X-User-Id`、`X-Admin-Id`、`X-Admin-Name`、`X-Request-Id`；`user_id` 绝不出现在任何 LLM 工具对外参数 Schema，必须通过 `ToolRuntime[AgentRuntimeContext]` 注入；LangGraph `thread_id` 固定格式 `f"u:{user_id}:s:{session_id}"`。
- **数据库边界**：所有客服业务表与 Checkpoint 均落 PostgreSQL；MySQL 零客服业务表，仅在 `sz-boot-parent/sz-service/sz-service-admin/src/main/resources/db/changelog/business/1.3.0/016_customer_service_menus.sql` 注册菜单与按钮权限（挂载在商城管理父菜单 `pid='6288292f0b0e466382ababe6fefb1ed1'` 下）。
- **商城现有边界保留**：不改造 `sz-service-websocket`（9993）；不放开 `ShopOrderAfterSaleServiceImpl` 对 `status=1`（已付款未发货）订单的自助售后限制（由客服引导转人工工单）；`direct-create` 仅校验库存充足不提前预扣库存（支付时走 `/client/shopOrder/order/payment` 统一扣减），且拦截 `ShopFlashSaleService.getAvailableByProductId` 返回生效中秒杀活动的商品；待支付订单卡片跳转路径为 `/order_payment?orderNo=<orderNo>`。
- **知识库检索与解析规则**：Milvus Collection 名 `kada_cs_kb_chunks`（HNSW `M=16, efConstruction=200`, `COSINE`, 1024 维），检索固定过滤 `scope == "public"` 与相似度 `score >= 0.35`；`MinerUParser` 支持 `cloud_api`（主路径 + 无 Token Flash 模式自动按页拆分 PDF 兜底）、`local_http`、`local_cli`。
- **Git 提交边界**：仓库当前存在未提交的 MQ 相关文件，每个任务结束提交时**严禁使用 `git add .` 或 `git add -A`**，必须显式列出本任务新增/修改的具体文件路径。

---

## Review Focus

1. **跨用户越权访问（IDOR）**：用户 B 携带合法登录态请求用户 A 的 `session_id`、`ticket_id`、`order_no` 或 `address_id` → Python 仓储层与 Java `/internal/agent/**` 均必须拒绝访问（403/404），对应测试：`tests/test_repositories.py::test_session_and_ticket_user_isolation`、`AgentMallServiceImplTest::directCreate_rejectsOtherUsersAddress`、`tests/test_api_e2e.py::test_cross_user_access_forbidden`。
2. **下单幂等性与秒杀/缺货拦截**：相同 `(user_id, idempotency_key)` 重复提交 `direct-create` 返回同一 `orderNo` 且仅建单 1 次、不提前扣减库存；生效中秒杀商品禁止普通直购，对应测试：`AgentMallServiceImplTest::directCreate_isIdempotentAndDoesNotDeductStock`、`AgentMallServiceImplTest::directCreate_rejectsActiveFlashSaleProduct`。
3. **Prompt Injection 工具越权防线**：`shopping_tools` 与 `after_sales_tools` 的工具 JSON Schema 均不包含 `user_id` 参数；即使诱导模型，工具也只能读取 `ToolRuntime.context.user_id`，对应测试：`tests/test_tools_security.py::test_tools_schema_hides_user_id_and_uses_runtime_context`。
4. **中断挂起防卡死与禁止自由文本确认下单**：会话处于 `order_confirm` 中断挂起态时，若用户直接发送新文本消息而非调用 `/resume/stream`，必须先自动以 `cancel` 清理旧草稿检查点再路由新消息，绝不触发 `direct-create`，对应测试：`tests/test_graph_flows.py::test_new_message_during_interrupt_auto_cancels_draft`、`tests/test_api_e2e.py::test_chat_stream_auto_cancels_pending_interrupt`。
5. **SSE 流式断连清理与防空头伪造**：前端断开连接时 Java `SseEmitter` 主动取消上游 `HttpClient` 请求、Python 侦测 `request.is_disconnected()` 中止生成并清理临时流状态；Java 转发层覆盖客户端伪造的 `X-User-Id` / `X-Internal-Secret`，对应测试：`AgentProxyServiceTest::forwardClientStream_stripsSpoofedHeadersAndCancelsOnDisconnect`、`tests/test_api_e2e.py::test_sse_disconnect_stops_generation`。

---

## File Structure

### 1. Python 微服务 `sz-agent/`
- `sz-agent/pyproject.toml`, `sz-agent/.env.example`, `sz-agent/docker-compose.yml`, `sz-agent/README.md`: 工程依赖、环境变量模板、PostgreSQL 16 + Milvus Standalone 本地容器编排与启动说明。
- `sz-agent/app/config.py`: `Settings` (`pydantic-settings`) 统一配置管理。
- `sz-agent/app/security/auth.py`: `verify_internal_secret`、`get_client_user_id`、`get_admin_actor` FastAPI 鉴权依赖。
- `sz-agent/app/db/base.py`, `sz-agent/app/db/session.py`, `sz-agent/app/db/models.py`: SQLAlchemy 2.0 异步引擎、会话工厂与 5 张业务表（`cs_sessions`, `cs_messages`, `cs_tickets`, `cs_ticket_replies`, `kb_documents`）。
- `sz-agent/alembic.ini`, `sz-agent/alembic/env.py`, `sz-agent/alembic/versions/0001_init_cs_tables.py`: Alembic 异步迁移配置与初始建表脚本。
- `sz-agent/app/repositories/session_repo.py`, `ticket_repo.py`, `kb_repo.py`: 按 `user_id` 强隔离的会话/消息、工单/回复与知识库文档仓储。
- `sz-agent/app/kb/mineru_parser.py`: `MinerUParser`（支持 `cloud_api` + Flash 超页自动拆分、`local_http`、`local_cli`）。
- `sz-agent/app/kb/chunker.py`: Markdown 标题 + 中国法条正则结构切分器（500~800 字，overlap 80~100 字）。
- `sz-agent/app/kb/embeddings.py`: 阿里 Qwen `text-embedding-v4`（1024 维）封装。
- `sz-agent/app/kb/milvus_store.py`: `kada_cs_kb_chunks` 集合初始化、插入、按 `doc_id` 删除及带阈值向量检索。
- `sz-agent/app/kb/ingestion.py`, `sz-agent/app/kb/seed.py`: SHA-256 去重入库编排与 `knowledge_docs/` 离线导入 CLI。
- `sz-agent/app/clients/mall_client.py`: 带 `X-Internal-Secret` 的异步 Java `/internal/agent/**` HTTP 客户端。
- `sz-agent/app/agent/context.py`, `state.py`, `llm.py`, `prompts.py`: `AgentRuntimeContext`、`CustomerServiceState`、`OrderDraft`、DeepSeek 客户端与提示词模板。
- `sz-agent/app/agent/tools/shopping_tools.py`, `after_sales_tools.py`: 6 个售前导购下单工具 + 4 个订单售后法规工具（均通过 `ToolRuntime` 读取 `user_id`）。
- `sz-agent/app/agent/subgraphs/recommend_order.py`, `consult_after_sales.py`: `recommend_order_subgraph`（含 `confirm_order_interrupt` 与 `execute_create_order`）与 `consult_after_sales_subgraph`。
- `sz-agent/app/agent/nodes/chitchat.py`, `handoff.py`, `fallback.py`, `sz-agent/app/agent/graph.py`: 寒暄、结构化摘要转人工、兜底节点及带 `AsyncPostgresSaver` 的主图装配。
- `sz-agent/app/schemas/cs.py`, `sz-agent/app/services/chat_service.py`: Pydantic 请求响应契约、SSE 事件流编排、15s 心跳、防中断卡死清理与落库事务。
- `sz-agent/app/routers/client_cs.py`, `admin_cs.py`, `sz-agent/app/main.py`: 用户端/管理端内部接口路由与 FastAPI `lifespan`。
- `sz-agent/knowledge_docs/*.pdf`, `sz-agent/scripts/build_knowledge_pdfs.py`: 4 份权威消费法规与商城服务规范 PDF 生成脚本及文档。
- `sz-agent/tests/*.py`: 全套单元、安全隔离与端到端异步测试。

### 2. Java 服务端 `sz-boot-parent/`
- `sz-boot-parent/config/{dev,local,preview,prod}/agent.yml`, `sz-boot-parent/sz-service/sz-service-admin/src/main/resources/application.yml`: `sz.agent.*` 外部配置文件导入。
- `sz-boot-parent/sz-common/sz-common-security/src/main/java/com/sz/security/config/SaTokenConfig.java`: 放行 `/internal/agent/**` 由内网拦截器接管。
- `sz-boot-parent/sz-service/sz-service-admin/src/main/java/com/sz/admin/shop_system/agent/`:
  - `config/AgentProperties.java`, `config/AgentInternalAuthInterceptor.java`, `config/AgentWebMvcConfig.java`
  - `pojo/dto/AgentMallDTO.java`, `pojo/vo/AgentMallVO.java`
  - `service/AgentMallService.java`, `service/impl/AgentMallServiceImpl.java`, `service/AgentProxyService.java`
  - `controller/AgentInternalController.java`, `controller/ClientCsController.java`, `controller/AdminCsController.java`
- `sz-boot-parent/sz-service/sz-service-admin/src/main/java/com/sz/admin/shop_system/service/ShopOrderService.java`, `service/impl/ShopOrderServiceImpl.java`: 抽取带显式 `long userId` 参数的 `frontPageForUser`、`frontDetailForUser`、`directCreateForAgent`。
- `sz-boot-parent/sz-service/sz-service-admin/src/main/java/com/sz/admin/shop_system/mapper/ShopProductMapper.java`, `src/main/resources/mapper/shop_system/ShopProductMapper.xml`: 上架商品推荐/搜索查询。
- `sz-boot-parent/sz-service/sz-service-admin/src/main/resources/db/changelog/business/1.3.0/016_customer_service_menus.sql`: `sz-admin` 客服菜单与权限点注册。
- `sz-boot-parent/sz-service/sz-service-admin/src/test/java/com/sz/admin/shop_system/agent/*.java`: 拦截器、内网商城服务、透传取消与防伪造头单元测试。

### 3. 前端 `sz-pc/` 与 `sz-admin/`
- `sz-pc/src/api/types/cs.ts`, `sz-pc/src/api/modules/cs.ts`, `sz-pc/src/api/index.ts`: 会话、SSE 流式对话/恢复及我的工单接口封装。
- `sz-pc/src/components/CustomerService/CustomerServiceWidget.vue`, `ChatMessageCards.vue`, `MyTicketPanel.vue`, `sz-pc/src/App.vue`: 全局悬浮智能客服抽屉（`/login` 隐藏，未登录引导跳转）。
- `sz-admin/src/api/types/shop_system/cs.ts`, `sz-admin/src/api/modules/shop_system/cs.ts`: 工单管理与知识库文档管理接口封装。
- `sz-admin/src/views/shop_system/csTicket/index.vue`, `components/TicketDetailDrawer.vue`: 「客服工单管理」页面与会话回放/回复抽屉。
- `sz-admin/src/views/shop_system/csKnowledge/index.vue`, `components/KbUploadDialog.vue`: 「客服知识库管理」页面与 PDF 上传弹窗。

---

### Task 1: `sz-agent` 工程脚手架、配置模型与内网鉴权基座

**Files:**
- Create: `sz-agent/pyproject.toml`
- Create: `sz-agent/.env.example`
- Create: `sz-agent/docker-compose.yml`
- Create: `sz-agent/app/__init__.py`
- Create: `sz-agent/app/config.py`
- Create: `sz-agent/app/security/__init__.py`
- Create: `sz-agent/app/security/auth.py`
- Create: `sz-agent/app/main.py`
- Create: `sz-agent/tests/__init__.py`
- Create: `sz-agent/tests/test_auth_and_config.py`

**Interfaces:**
- Produces: `Settings` & `get_settings() -> Settings` (`app/config.py`); `verify_internal_secret(x_internal_secret: str | None)`, `get_client_user_id(x_user_id: str | None) -> int`, `get_admin_actor(x_admin_id: str | None, x_admin_name: str | None) -> AdminActor` (`app/security/auth.py`); `create_app() -> FastAPI` (`app/main.py`).

- [ ] **Step 1: 创建 `sz-agent/pyproject.toml`、`.env.example` 与 `docker-compose.yml` 并同步虚拟环境**

在 `sz-agent/pyproject.toml` 指定 `requires-python = ">=3.12"` 与 `Global Constraints` 全部依赖；`.env.example` 包含 spec 第 9.1 节全部变量（默认 `APP_HOST=127.0.0.1`、`APP_PORT=8000`、`JAVA_INTERNAL_BASE_URL=http://127.0.0.1:9991/api`、`QWEN_EMBEDDING_DIMENSIONS=1024`、`MINERU_MODE=cloud_api`）；`docker-compose.yml` 编排 `postgres:16-alpine` (5432) 与 Milvus Standalone (`etcd` + `minio` + `milvus-standalone` 19530/9091)。执行 `cd sz-agent && uv sync --all-extras` 确认环境安装成功。

- [ ] **Step 2: 编写配置与内网鉴权失败测试**

```python
# sz-agent/tests/test_auth_and_config.py
import pytest
from fastapi.testclient import TestClient

def test_health_requires_no_secret_and_internal_routes_reject_invalid_secret(monkeypatch):
    monkeypatch.setenv("AGENT_INTERNAL_SECRET", "test-secret-xyz")
    from app.config import get_settings
    get_settings.cache_clear()
    from app.main import create_app
    client = TestClient(create_app())
    assert client.get("/health").status_code == 200
    assert client.get("/internal/cs/client/ping").status_code == 401
    assert client.get("/internal/cs/client/ping", headers={"X-Internal-Secret": "wrong"}).status_code == 401
    assert client.get("/internal/cs/client/ping", headers={"X-Internal-Secret": "test-secret-xyz"}).status_code == 401  # missing X-User-Id
    res = client.get("/internal/cs/client/ping", headers={"X-Internal-Secret": "test-secret-xyz", "X-User-Id": "10086"})
    assert res.status_code == 200 and res.json()["user_id"] == 10086

def test_admin_actor_decodes_url_encoded_name(monkeypatch):
    monkeypatch.setenv("AGENT_INTERNAL_SECRET", "test-secret-xyz")
    from app.config import get_settings
    get_settings.cache_clear()
    from app.main import create_app
    client = TestClient(create_app())
    res = client.get(
        "/internal/cs/admin/ping",
        headers={"X-Internal-Secret": "test-secret-xyz", "X-Admin-Id": "1", "X-Admin-Name": "%E5%AE%A2%E6%9C%8D%E5%B0%8F%E5%8D%A1"},
    )
    assert res.status_code == 200 and res.json() == {"admin_id": 1, "admin_name": "客服小卡"}
```

- [ ] **Step 3: 运行测试确认失败**

Run: `cd sz-agent && uv run pytest tests/test_auth_and_config.py -v`
Expected: FAIL (`ModuleNotFoundError: No module named 'app'`)

- [ ] **Step 4: 实现 `app/config.py`、`app/security/auth.py` 与 `app/main.py` 最小骨架**

`Settings(BaseSettings)` 加载环境变量并提供 `@lru_cache def get_settings() -> Settings`；`verify_internal_secret` 使用 `hmac.compare_digest` 校验 `X-Internal-Secret`；`get_client_user_id` 解析正整数 `X-User-Id`（无效返回 401）；`get_admin_actor` 解析 `X-Admin-Id` 与 `urllib.parse.unquote(x_admin_name)`；`create_app()` 注册 `/health` 与 `/internal/cs/{client,admin}/ping`（供鉴权冒烟，后续 Task 8 挂载完整路由）。

- [ ] **Step 5: 运行测试确认通过**

Run: `cd sz-agent && uv run pytest tests/test_auth_and_config.py -v`
Expected: PASS (2 passed)

- [ ] **Step 6: 提交**

```bash
git add sz-agent/pyproject.toml sz-agent/uv.lock sz-agent/.env.example sz-agent/docker-compose.yml sz-agent/app/__init__.py sz-agent/app/config.py sz-agent/app/security/__init__.py sz-agent/app/security/auth.py sz-agent/app/main.py sz-agent/tests/__init__.py sz-agent/tests/test_auth_and_config.py
git commit -m "chore(agent): bootstrap sz-agent project with settings and internal auth"
```

---

### Task 2: PostgreSQL 5 张表模型、Alembic 迁移与隔离仓储层

**Files:**
- Create: `sz-agent/app/db/__init__.py`
- Create: `sz-agent/app/db/base.py`
- Create: `sz-agent/app/db/models.py`
- Create: `sz-agent/app/db/session.py`
- Create: `sz-agent/alembic.ini`
- Create: `sz-agent/alembic/env.py`
- Create: `sz-agent/alembic/versions/0001_init_cs_tables.py`
- Create: `sz-agent/app/repositories/__init__.py`
- Create: `sz-agent/app/repositories/session_repo.py`
- Create: `sz-agent/app/repositories/ticket_repo.py`
- Create: `sz-agent/app/repositories/kb_repo.py`
- Create: `sz-agent/tests/test_repositories.py`

**Interfaces:**
- Consumes: `Settings` (`app/config.py`)
- Produces:
  - ORM 模型：`CsSession`, `CsMessage`, `CsTicket`, `CsTicketReply`, `KbDocument`
  - `SessionRepository(session)`: `create_session(user_id, title)`, `list_sessions(user_id, limit)`, `get_session_for_user(session_id, user_id)`, `append_message(...)`, `list_messages_for_user(session_id, user_id)`, `list_messages_for_admin(session_id)`, `set_pending_interrupt(session_id, user_id, pending: bool, intent: str | None)`
  - `TicketRepository(session)`: `create_ticket(user_id, session_id, category, title, summary, related_order_no)`, `list_tickets_for_user(...)`, `get_ticket_detail_for_user(ticket_id, user_id)`, `add_user_followup(ticket_id, user_id, content)`, `list_tickets_for_admin(...)`, `get_ticket_detail_for_admin(ticket_id)`, `add_admin_reply(ticket_id, admin_id, admin_name, content, close_ticket: bool)`, `close_ticket(ticket_id)`
  - `KbRepository(session)`: `get_by_sha256(file_sha256)`, `create_document(...)`, `update_status(doc_id, status, chunk_count, error_msg)`, `list_documents(...)`, `get_document(doc_id)`, `delete_document(doc_id)`

- [ ] **Step 1: 编写仓储层与 IDOR 隔离失败测试（覆盖 Review Focus #1）**

```python
# sz-agent/tests/test_repositories.py
import pytest

@pytest.mark.asyncio
async def test_session_and_ticket_user_isolation(async_db_session):
    from app.repositories.session_repo import SessionRepository
    from app.repositories.ticket_repo import TicketRepository
    s_repo = SessionRepository(async_db_session)
    t_repo = TicketRepository(async_db_session)

    s_a = await s_repo.create_session(user_id=101, title="咨询键盘")
    await s_repo.append_message(s_a.id, 101, "user", "推荐一款机械键盘")
    assert await s_repo.get_session_for_user(s_a.id, user_id=202) is None
    assert await s_repo.list_messages_for_user(s_a.id, user_id=202) is None

    ticket = await t_repo.create_ticket(101, s_a.id, "order_issue", "催发货", "用户询问何时发货", "ORD1001")
    assert ticket.status == "OPEN" and ticket.ticket_no.startswith("TK")
    assert await t_repo.get_ticket_detail_for_user(ticket.id, user_id=202) is None

@pytest.mark.asyncio
async def test_ticket_state_machine_and_session_system_message(async_db_session):
    from app.repositories.session_repo import SessionRepository
    from app.repositories.ticket_repo import TicketRepository
    s_repo = SessionRepository(async_db_session)
    t_repo = TicketRepository(async_db_session)

    s_a = await s_repo.create_session(user_id=101, title="退款问题")
    ticket = await t_repo.create_ticket(101, s_a.id, "after_sale", "未发货退款", "申请取消", "ORD1002")

    replied = await t_repo.add_admin_reply(ticket.id, admin_id=1, admin_name="客服A", content="已为您安排拦截", close_ticket=False)
    assert replied.status == "REPLIED"
    msgs = await s_repo.list_messages_for_user(s_a.id, 101)
    assert any(m.role == "system" and ticket.ticket_no in m.content and "已为您安排拦截" in m.content for m in msgs)

    reopened = await t_repo.add_user_followup(ticket.id, user_id=101, content="请问多久退回余额？")
    assert reopened.status == "OPEN"

    closed = await t_repo.close_ticket(ticket.id)
    assert closed.status == "CLOSED"
    with pytest.raises(ValueError, match="已关闭"):
        await t_repo.add_user_followup(ticket.id, user_id=101, content="再问一句")
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd sz-agent && uv run pytest tests/test_repositories.py -v`
Expected: FAIL (`ModuleNotFoundError: No module named 'app.db'`)

- [ ] **Step 3: 实现 ORM 模型、Alembic 初始迁移与三个仓储类**

- `app/db/models.py` 定义 spec 第 6.1 节全部 5 张表及索引；UUID 主键与 JSONB 列使用 SQLAlchemy `.with_variant(JSON(), "sqlite")` / `Uuid(as_uuid=True)` 兼顾 PostgreSQL 原生类型与测试 SQLite `aiosqlite` 内存库。
- `alembic/versions/0001_init_cs_tables.py` 创建 5 张表及所有指定索引；`alembic/env.py` 从 `get_settings().postgres_dsn` 读取异步连接串。
- `TicketRepository.add_admin_reply` 在写入 `cs_ticket_replies` 的同时，向关联 `cs_sessions` 写入一条 `role="system"` 的 `cs_messages` 通知（spec 第 6.2 节）；`add_user_followup` 仅允许 `OPEN` / `REPLIED` 工单并把状态切回 `OPEN`。

- [ ] **Step 4: 运行测试确认通过**

Run: `cd sz-agent && uv run pytest tests/test_repositories.py -v`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add sz-agent/app/db sz-agent/app/repositories sz-agent/alembic.ini sz-agent/alembic sz-agent/tests/test_repositories.py
git commit -m "feat(agent): add PostgreSQL models, Alembic migration, and isolated repositories"
```

---

### Task 3: Java 内网商城数据与幂等建单接口 `/internal/agent/**`

**Files:**
- Create: `sz-boot-parent/config/dev/agent.yml`
- Create: `sz-boot-parent/config/local/agent.yml`
- Create: `sz-boot-parent/config/preview/agent.yml`
- Create: `sz-boot-parent/config/prod/agent.yml`
- Modify: `sz-boot-parent/sz-service/sz-service-admin/src/main/resources/application.yml:9-18`
- Modify: `sz-boot-parent/sz-common/sz-common-security/src/main/java/com/sz/security/config/SaTokenConfig.java:52-63`
- Create: `sz-boot-parent/sz-service/sz-service-admin/src/main/java/com/sz/admin/shop_system/agent/config/AgentProperties.java`
- Create: `sz-boot-parent/sz-service/sz-service-admin/src/main/java/com/sz/admin/shop_system/agent/config/AgentInternalAuthInterceptor.java`
- Create: `sz-boot-parent/sz-service/sz-service-admin/src/main/java/com/sz/admin/shop_system/agent/config/AgentWebMvcConfig.java`
- Create: `sz-boot-parent/sz-service/sz-service-admin/src/main/java/com/sz/admin/shop_system/agent/pojo/dto/AgentMallDTO.java`
- Create: `sz-boot-parent/sz-service/sz-service-admin/src/main/java/com/sz/admin/shop_system/agent/pojo/vo/AgentMallVO.java`
- Modify: `sz-boot-parent/sz-service/sz-service-admin/src/main/java/com/sz/admin/shop_system/mapper/ShopProductMapper.java`
- Modify: `sz-boot-parent/sz-service/sz-service-admin/src/main/resources/mapper/shop_system/ShopProductMapper.xml`
- Modify: `sz-boot-parent/sz-service/sz-service-admin/src/main/java/com/sz/admin/shop_system/service/ShopOrderService.java`
- Modify: `sz-boot-parent/sz-service/sz-service-admin/src/main/java/com/sz/admin/shop_system/service/impl/ShopOrderServiceImpl.java`
- Create: `sz-boot-parent/sz-service/sz-service-admin/src/main/java/com/sz/admin/shop_system/agent/service/AgentMallService.java`
- Create: `sz-boot-parent/sz-service/sz-service-admin/src/main/java/com/sz/admin/shop_system/agent/service/impl/AgentMallServiceImpl.java`
- Create: `sz-boot-parent/sz-service/sz-service-admin/src/main/java/com/sz/admin/shop_system/agent/controller/AgentInternalController.java`
- Create: `sz-boot-parent/sz-service/sz-service-admin/src/test/java/com/sz/admin/shop_system/agent/AgentInternalAuthInterceptorTest.java`
- Create: `sz-boot-parent/sz-service/sz-service-admin/src/test/java/com/sz/admin/shop_system/agent/AgentMallServiceImplTest.java`

**Interfaces:**
- Produces:
  - `ShopOrderService`: `frontPageForUser(Long userId, ShopOrderDTO.FrontList dto)`, `frontDetailForUser(Long userId, String orderNo)`, `directCreateForAgent(Long userId, AgentMallDTO.DirectCreateOrder dto) -> AgentMallVO.DirectOrderResult`
  - HTTP `/internal/agent/**`（spec 第 7.3 节 8 个端点：`GET /products/recommend`, `GET /products/search`, `GET /products/{productId}`, `GET /users/{userId}/addresses`, `GET /users/{userId}/orders`, `GET /users/{userId}/orders/{orderNo}`, `GET /users/{userId}/after-sales`, `POST /users/{userId}/orders/direct-create`）

- [ ] **Step 1: 编写 `AgentInternalAuthInterceptorTest` 与 `AgentMallServiceImplTest` 失败测试（覆盖 Review Focus #1, #2）**

测试断言覆盖：
1. `AgentInternalAuthInterceptorTest::preHandle_rejectsMissingOrInvalidSecretWith401` 与 `preHandle_allowsValidConstantTimeSecret`；
2. `AgentMallServiceImplTest::directCreate_rejectsOtherUsersAddress`（用户 A 传入用户 B 的 `addressId` 抛业务异常）；
3. `AgentMallServiceImplTest::directCreate_rejectsActiveFlashSaleProduct`（`ShopFlashSaleService.getAvailableByProductId` 非空时拒绝直购）；
4. `AgentMallServiceImplTest::directCreate_isIdempotentAndDoesNotDeductStock`（重复 `idempotencyKey` 命中使用 Redis 幂等键返回原 `orderNo`，首次创建写入 `paid=0, status=0, seckillId=0`，初始化支付倒计时且不修改 SKU/商品库存，返回 `paymentUrl="/order_payment?orderNo=" + orderNo`）；
5. `AgentMallServiceImplTest::getOrderDetail_returnsShipmentRecordWithoutFakeTrackingNodes`（查询 `shop_express_info` 发货记录返回单号、模板名、发货状态与发货时间，并附 `shop_order_status` 时间轴）。

- [ ] **Step 2: 运行测试确认编译/测试失败**

Run: `mvn -f sz-boot-parent/pom.xml -pl sz-service/sz-service-admin -am test-compile -Dtest=AgentInternalAuthInterceptorTest,AgentMallServiceImplTest`
Expected: FAIL (新增类尚未创建)

- [ ] **Step 3: 配置外部 `agent.yml` 与内网安全拦截器**

- 新增 `config/{dev,local,preview,prod}/agent.yml`（`sz.agent.enabled: true`、`base-url: ${AGENT_BASE_URL:http://127.0.0.1:8000}`、`internal-secret: ${AGENT_INTERNAL_SECRET:kada-local-internal-secret}`、`connect-timeout-ms: 5000`、`stream-timeout-ms: 300000`），在 `application.yml` 导入 `file:config/${spring.profiles.active}/agent.yml`。
- 修改 `SaTokenConfig.java`，在用户端与管理端拦截器 `.excludePathPatterns(...)` 均追加 `"/internal/agent/**"`；`AgentWebMvcConfig` 仅将 `AgentInternalAuthInterceptor` 挂载到 `"/internal/agent/**"`。

- [ ] **Step 4: 抽取显式 `userId` 订单方法并实现 `AgentMallServiceImpl` 与 `AgentInternalController`**

- 在 `ShopProductMapper(.java/.xml)` 新增 `selectAgentRecommendProducts`（按 `hot`: `is_hot DESC, sales DESC, browse DESC`；`sales`: `sales DESC, browse DESC`；`browse`: `browse DESC, sales DESC`，仅 `is_del=0 AND is_show=1 AND stock>0`）及 `selectAgentSearchProducts`（关键词/分类/价格区间过滤）。
- 在 `ShopOrderServiceImpl` 将原 `frontPage`/`frontDetail` 委托给 `frontPageForUser(userId, dto)`/`frontDetailForUser(userId, orderNo)`；新增 `directCreateForAgent(userId, dto)`：使用 Redis key `shop:agent:order:idemp:{userId}:{idempotencyKey}` 幂等锁与结果缓存（TTL 24h），校验上架、非生效中秒杀、SKU 归属与 `stock >= quantity`、地址归属 `userId`，按商品 `isPostage`/`postage` 计算运费，写入 `shop_order`（`paid=0, status=0, seckillId=0`）与 `shop_order_status` 并调 `initializeOrderPaymentCountdown(order)`。
- `AgentMallServiceImpl.getOrderDetail` 复用 `frontDetailForUser` 并补充查询 `ShopExpressInfoMapper` + `ShopExpressTemplate` 与 `ShopOrderAfterSaleMapper`，绝不虚构在途物流节点。

- [ ] **Step 5: 运行 Java 测试确认通过**

Run: `mvn -f sz-boot-parent/pom.xml -pl sz-service/sz-service-admin -am test -Dtest=AgentInternalAuthInterceptorTest,AgentMallServiceImplTest`
Expected: PASS (`Tests run: 5, Failures: 0, Errors: 0`)

- [ ] **Step 6: 提交**

```bash
git add sz-boot-parent/config/dev/agent.yml sz-boot-parent/config/local/agent.yml sz-boot-parent/config/preview/agent.yml sz-boot-parent/config/prod/agent.yml sz-boot-parent/sz-service/sz-service-admin/src/main/resources/application.yml sz-boot-parent/sz-common/sz-common-security/src/main/java/com/sz/security/config/SaTokenConfig.java sz-boot-parent/sz-service/sz-service-admin/src/main/java/com/sz/admin/shop_system/agent sz-boot-parent/sz-service/sz-service-admin/src/main/java/com/sz/admin/shop_system/mapper/ShopProductMapper.java sz-boot-parent/sz-service/sz-service-admin/src/main/resources/mapper/shop_system/ShopProductMapper.xml sz-boot-parent/sz-service/sz-service-admin/src/main/java/com/sz/admin/shop_system/service/ShopOrderService.java sz-boot-parent/sz-service/sz-service-admin/src/main/java/com/sz/admin/shop_system/service/impl/ShopOrderServiceImpl.java sz-boot-parent/sz-service/sz-service-admin/src/test/java/com/sz/admin/shop_system/agent
git commit -m "feat(java): add internal agent mall endpoints and idempotent direct order creation"
```

---

### Task 4: Java 公网超薄鉴权透传层 (`/client/cs/**` & `/admin/cs/**`) + Liquibase 菜单脚本

**Files:**
- Create: `sz-boot-parent/sz-service/sz-service-admin/src/main/java/com/sz/admin/shop_system/agent/service/AgentProxyService.java`
- Create: `sz-boot-parent/sz-service/sz-service-admin/src/main/java/com/sz/admin/shop_system/agent/controller/ClientCsController.java`
- Create: `sz-boot-parent/sz-service/sz-service-admin/src/main/java/com/sz/admin/shop_system/agent/controller/AdminCsController.java`
- Create: `sz-boot-parent/sz-service/sz-service-admin/src/main/resources/db/changelog/business/1.3.0/016_customer_service_menus.sql`
- Create: `sz-boot-parent/sz-service/sz-service-admin/src/test/java/com/sz/admin/shop_system/agent/AgentProxyServiceTest.java`

**Interfaces:**
- Consumes: `AgentProperties`, `StpUserUtil`, `LoginUtils`
- Produces:
  - `AgentProxyService`: `forwardJson(...) -> ResponseEntity<byte[]>`, `forwardMultipart(...) -> ResponseEntity<byte[]>`, `forwardSse(...) -> SseEmitter`
  - 公网 `/client/cs/**`（7 个端点）与 `/admin/cs/**`（8 个端点），权限码：`cs.ticket.query_table`、`cs.ticket.reply`、`cs.ticket.close`、`cs.kb.query_table`、`cs.kb.upload`、`cs.kb.delete`。

- [ ] **Step 1: 编写 `AgentProxyServiceTest` 失败测试（覆盖 Review Focus #5 与伪造头清洗）**

使用本地 JDK `com.sun.net.httpserver.HttpServer` 启动桩上游服务，断言：
1. `forwardJson_stripsClientSpoofedHeadersAndInjectsTrustedIdentity`：模拟外部伪造 `X-User-Id: 99999`、`X-Internal-Secret: fake`、`X-Admin-Id: 1`，验证上游实际收到服务端注入的 `X-User-Id: 10086` 与配置密钥；
2. `forwardSse_relaysEventsAndCancelsUpstreamOnEmitterCompletion`：上游推送 `event: meta` + `event: token`，验证 `SseEmitter` 收到事件且触发 `onCompletion`/`onTimeout` 时立即关闭底层流。

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -f sz-boot-parent/pom.xml -pl sz-service/sz-service-admin -am test -Dtest=AgentProxyServiceTest`
Expected: FAIL (`AgentProxyService` 不存在)

- [ ] **Step 3: 实现 `AgentProxyService`、`ClientCsController`、`AdminCsController` 与 `016_customer_service_menus.sql`**

- `AgentProxyService` 使用 JDK `java.net.http.HttpClient` + `Thread.startVirtualThread(...)` 处理 `SseEmitter(300_000L)` 逐行解析 SSE（`event:`, `data:`）并转发；绑定 `onCompletion`/`onTimeout`/`onError` 中断流读取并关闭 `InputStream`。
- `ClientCsController` 全部标注 `@SaCheckLogin(type = "shop_user")`，从 `StpUserUtil.getLoginIdAsLong()` 取 `userId`；设置 `X-Accel-Buffering: no` 与 `Cache-Control: no-cache`。
- `AdminCsController` 全部标注 `@SaCheckLogin` 与对应 `@SaCheckPermission(...)`，从 `LoginUtils.getLoginUser()` 提取 `adminId` 与 URL 编码后的 `adminName`；`/kb/documents/upload` 支持 `MultipartFile` 转发。
- `016_customer_service_menus.sql` 幂等插入（`WHERE NOT EXISTS`）两个二级菜单到 `pid='6288292f0b0e466382ababe6fefb1ed1'`：路径 `/shop/cs-ticket`、`component='/shop_system/csTicket/index'`（`dynamicRouter.ts` 拼接 `'/src/views' + component + '.vue'`，必须带前导 `/`）；路径 `/shop/cs-knowledge`、`component='/shop_system/csKnowledge/index'`；并插入全部 6 个按钮权限（`cs.ticket.*`、`cs.kb.*`）。

- [ ] **Step 4: 运行 Java 测试确认通过**

Run: `mvn -f sz-boot-parent/pom.xml -pl sz-service/sz-service-admin -am test -Dtest=AgentInternalAuthInterceptorTest,AgentMallServiceImplTest,AgentProxyServiceTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add sz-boot-parent/sz-service/sz-service-admin/src/main/java/com/sz/admin/shop_system/agent/service/AgentProxyService.java sz-boot-parent/sz-service/sz-service-admin/src/main/java/com/sz/admin/shop_system/agent/controller/ClientCsController.java sz-boot-parent/sz-service/sz-service-admin/src/main/java/com/sz/admin/shop_system/agent/controller/AdminCsController.java sz-boot-parent/sz-service/sz-service-admin/src/main/resources/db/changelog/business/1.3.0/016_customer_service_menus.sql sz-boot-parent/sz-service/sz-service-admin/src/test/java/com/sz/admin/shop_system/agent/AgentProxyServiceTest.java
git commit -m "feat(java): add thin Sa-Token proxy controllers and Liquibase menus for CS agent"
```

---

### Task 5: 知识库流水线（MinerU + 结构切块 + Qwen Embedding + Milvus）

**Files:**
- Create: `sz-agent/app/kb/__init__.py`
- Create: `sz-agent/app/kb/mineru_parser.py`
- Create: `sz-agent/app/kb/chunker.py`
- Create: `sz-agent/app/kb/embeddings.py`
- Create: `sz-agent/app/kb/milvus_store.py`
- Create: `sz-agent/app/kb/ingestion.py`
- Create: `sz-agent/app/kb/seed.py`
- Create: `sz-agent/tests/test_kb_pipeline.py`

**Interfaces:**
- Consumes: `Settings`, `KbRepository`
- Produces:
  - `MinerUParser.parse_pdf(pdf_path: Path) -> str`
  - `chunk_markdown(markdown_text: str, doc_id: str, doc_title: str, category: str, min_chars=500, max_chars=800, overlap=90) -> list[KbChunk]`
  - `get_qwen_embeddings(settings: Settings) -> OpenAIEmbeddings`
  - `MilvusKbStore`: `ensure_collection()`, `upsert_chunks(chunks, vectors)`, `delete_by_doc_id(doc_id)`, `search(query_vector, categories: list[str] | None = None, top_k: int = 5, min_score: float = 0.35) -> list[KbSearchHit]`
  - `KbRetriever(embeddings, milvus_store)`: `retrieve(query: str, categories: list[str] | None = None, top_k: int = 5) -> list[KbSearchHit]`
  - `KbIngestionService`: `ingest_document(doc_id: UUID)`, `delete_document(doc_id: UUID)`

- [ ] **Step 1: 编写知识库解析、切块、Flash 拆分与删重入库失败测试**

```python
# sz-agent/tests/test_kb_pipeline.py
import pytest
from pypdf import PdfWriter

def test_chunk_markdown_preserves_law_articles_and_section_path():
    from app.kb.chunker import chunk_markdown
    md = "# 第三章 经营者的义务\n\n## 第二十五条\n" + ("经营者采用网络方式销售商品，消费者有权自收到商品之日起七日内退货。" * 6) + "\n\n## 第二十六条\n格式条款规定。"
    chunks = chunk_markdown(md, doc_id="d1", doc_title="消保法", category="consumer_law", min_chars=120, max_chars=260, overlap=40)
    assert len(chunks) >= 2
    assert all(c.section_path.startswith("第三章 经营者的义务") for c in chunks)
    assert any("第二十六条" in c.content for c in chunks)

@pytest.mark.asyncio
async def test_mineru_flash_fallback_splits_oversized_pdf(tmp_path):
    from app.kb.mineru_parser import MinerUParser
    pdf_path = tmp_path / "big.pdf"
    writer = PdfWriter()
    for _ in range(25):
        writer.add_blank_page(width=200, height=200)
    with pdf_path.open("wb") as f:
        writer.write(f)
    calls = []
    parser = MinerUParser(mode="cloud_api", api_token="", flash_max_pages=10, flash_runner=lambda p: (calls.append(p.name) or f"# part {len(calls)}"))
    md = await parser.parse_pdf(pdf_path)
    assert len(calls) == 3 and "part 1" in md and "part 3" in md

@pytest.mark.asyncio
async def test_ingestion_deletes_old_vectors_before_upsert_and_handles_failure(async_db_session, tmp_path):
    from app.repositories.kb_repo import KbRepository
    from app.kb.ingestion import KbIngestionService
    pdf_path = tmp_path / "law.pdf"
    pdf_path.write_bytes(b"%PDF-1.4 sample")
    repo = KbRepository(async_db_session)
    doc = await repo.create_document(title="消保法", category="consumer_law", file_name="law.pdf", file_path=str(pdf_path), file_sha256="abc123", file_size=15, mineru_mode="cloud_api", uploaded_by=1)

    events = []
    class FakeParser:
        async def parse_pdf(self, path): return "# 第一条\n保护消费者合法权益。"
    class FakeEmbed:
        async def aembed_documents(self, texts): return [[0.1] * 1024 for _ in texts]
    class FakeStore:
        async def delete_by_doc_id(self, doc_id): events.append(("del", str(doc_id)))
        async def upsert_chunks(self, chunks, vectors): events.append(("upsert", len(chunks), len(vectors[0])))

    svc = KbIngestionService(session=async_db_session, parser=FakeParser(), embeddings=FakeEmbed(), store=FakeStore(), storage_dir=tmp_path)
    await svc.ingest_document(doc.id)
    updated = await repo.get_document(doc.id)
    assert updated.status == "INDEXED" and updated.chunk_count >= 1
    assert events[0][0] == "del" and events[1][0] == "upsert" and events[1][2] == 1024

    class BrokenParser:
        async def parse_pdf(self, path): raise RuntimeError("MinerU timeout")
    svc_fail = KbIngestionService(session=async_db_session, parser=BrokenParser(), embeddings=FakeEmbed(), store=FakeStore(), storage_dir=tmp_path)
    await svc_fail.ingest_document(doc.id)
    failed = await repo.get_document(doc.id)
    assert failed.status == "FAILED" and "MinerU timeout" in (failed.error_msg or "")
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd sz-agent && uv run pytest tests/test_kb_pipeline.py -v`
Expected: FAIL (`ModuleNotFoundError: No module named 'app.kb'`)

- [ ] **Step 3: 实现 `mineru_parser.py`、`chunker.py`、`embeddings.py`、`milvus_store.py`、`ingestion.py` 与 `seed.py`**

- `mineru_parser.py` 支持 `cloud_api`（`mineru-open-sdk`，有 `MINERU_API_TOKEN` 走 Precision，无 Token 走 Flash 并在页数超限时用 `pypdf` 按 `flash_max_pages` 拆分后合并 Markdown）、`local_http`、`local_cli`；并把原始 Markdown 缓存到 `{kb_storage_dir}/markdown/{doc_id}.md`。
- `chunker.py` 解析 Markdown 标题栈为 `section_path`，优先按 `^第[一二三四五六七八九十百千零〇0-9]+条` 边界合并/切块（默认 500~800 字符、overlap 80~100 字符）。
- `embeddings.py` 返回配置好 `model="text-embedding-v4"`、`dimensions=1024`、`encoding_format="float"`、`check_embedding_ctx_length=False` 的 `OpenAIEmbeddings`。
- `milvus_store.py` 建立 `kada_cs_kb_chunks` 集合（spec 第 5.4 节全部 9 字段 + `HNSW/COSINE`），查询固定拼装 `scope == "public"` 过滤并剔除 `score < min_score` 的切片。

- [ ] **Step 4: 运行测试确认通过**

Run: `cd sz-agent && uv run pytest tests/test_kb_pipeline.py -v`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add sz-agent/app/kb sz-agent/tests/test_kb_pipeline.py
git commit -m "feat(agent): implement MinerU parser, legal chunker, Qwen embeddings, and Milvus store"
```

---

### Task 6: Python 内网商城客户端与 LangChain 安全工具集

**Files:**
- Create: `sz-agent/app/clients/__init__.py`
- Create: `sz-agent/app/clients/mall_client.py`
- Create: `sz-agent/app/agent/__init__.py`
- Create: `sz-agent/app/agent/context.py`
- Create: `sz-agent/app/agent/tools/__init__.py`
- Create: `sz-agent/app/agent/tools/shopping_tools.py`
- Create: `sz-agent/app/agent/tools/after_sales_tools.py`
- Create: `sz-agent/tests/test_tools_security.py`

**Interfaces:**
- Consumes: `Settings`, `KbRetriever`, Java `/internal/agent/**`
- Produces:
  - `AgentRuntimeContext(user_id: int, session_id: str, trace_id: str)`
  - `MallInternalClient`: `recommend_products`, `search_products`, `get_product_detail`, `list_user_addresses`, `list_user_orders`, `get_order_detail`, `list_user_after_sales`, `direct_create_order`
  - `build_shopping_tools(mall_client, kb_retriever) -> list` (6 个工具：`list_hot_and_popular_products`, `search_products`, `get_product_detail_and_skus`, `search_presale_kb`, `list_user_addresses`, `prepare_order_draft`)
  - `build_after_sales_tools(mall_client, kb_retriever) -> list` (4 个工具：`list_my_orders`, `get_my_order_detail`, `get_my_after_sale_status`, `search_policy_and_law_kb`)

- [ ] **Step 1: 编写工具安全隔离与下单草稿校验失败测试（覆盖 Review Focus #3）**

```python
# sz-agent/tests/test_tools_security.py
from types import SimpleNamespace
import pytest

def test_tools_schema_hides_user_id_and_uses_runtime_context():
    from app.agent.tools.shopping_tools import build_shopping_tools
    from app.agent.tools.after_sales_tools import build_after_sales_tools
    all_tools = build_shopping_tools(mall_client=object(), kb_retriever=None) + build_after_sales_tools(mall_client=object(), kb_retriever=None)
    for t in all_tools:
        schema = t.args_schema.model_json_schema() if hasattr(t.args_schema, "model_json_schema") else t.get_input_schema().model_json_schema()
        assert "user_id" not in schema.get("properties", {}), f"{t.name} must not expose user_id to LLM"

@pytest.mark.asyncio
async def test_prepare_order_draft_rejects_empty_address_and_active_flash_sale():
    from app.agent.context import AgentRuntimeContext
    from app.agent.tools.shopping_tools import build_shopping_tools

    class FakeMall:
        def __init__(self, active_flash: bool, addresses: list):
            self.active_flash = active_flash
            self.addresses = addresses
            self.seen_user_ids = []
        async def get_product_detail(self, product_id: int):
            return {"id": product_id, "productName": "咔嗒键盘", "image": "/img.png", "isShow": 1, "activeFlashSale": self.active_flash, "isPostage": 1, "postage": "0.00", "skus": [{"skuId": 11, "sku": "红轴/白", "price": "399.00", "stock": 10}]}
        async def list_user_addresses(self, user_id: int):
            self.seen_user_ids.append(user_id)
            return self.addresses

    rt = SimpleNamespace(context=AgentRuntimeContext(user_id=777, session_id="s1", trace_id="t1"))
    tool_flash = {t.name: t for t in build_shopping_tools(FakeMall(True, [{"id": 1}]), None)}["prepare_order_draft"]
    res_flash = await tool_flash.coroutine(product_id=1, sku_id=11, quantity=1, runtime=rt)
    assert res_flash["ok"] is False and "秒杀" in res_flash["message"]

    mall_no_addr = FakeMall(False, [])
    tool_no_addr = {t.name: t for t in build_shopping_tools(mall_no_addr, None)}["prepare_order_draft"]
    res_no_addr = await tool_no_addr.coroutine(product_id=1, sku_id=11, quantity=1, runtime=rt)
    assert mall_no_addr.seen_user_ids == [777]
    assert res_no_addr["ok"] is False and "/user/account/address" in res_no_addr["message"]

    mall_ok = FakeMall(False, [{"id": 9, "realName": "张三", "phone": "13800000000", "fullAddress": "上海市浦东新区", "isDefault": 1}])
    tool_ok = {t.name: t for t in build_shopping_tools(mall_ok, None)}["prepare_order_draft"]
    res_ok = await tool_ok.coroutine(product_id=1, sku_id=11, quantity=2, runtime=rt)
    assert res_ok["ok"] is True and res_ok["draft"]["total_pay_price"] == "798.00" and res_ok["draft"]["address_id"] == 9
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd sz-agent && uv run pytest tests/test_tools_security.py -v`
Expected: FAIL (`ModuleNotFoundError: No module named 'app.clients'`)

- [ ] **Step 3: 实现 `mall_client.py`、`context.py` 与两组 LangChain 工具**

- `MallInternalClient` 使用 `httpx.AsyncClient(base_url=settings.java_internal_base_url)` 并在每个请求头自动携带 `X-Internal-Secret`。
- 所有需要用户身份的工具（`list_user_addresses`、`prepare_order_draft`、`list_my_orders`、`get_my_order_detail`、`get_my_after_sale_status`）函数签名中接收 `runtime: ToolRuntime[AgentRuntimeContext]`，由 LangChain 隐藏不暴露给大模型，内部读取 `runtime.context.user_id` 调用 `mall_client`。
- `search_presale_kb` 固定检索 `["product_guide", "faq"]`；`search_policy_and_law_kb` 检索 `consumer_law` / `mall_policy` / `faq` 并返回结构化条文出处及正文。

- [ ] **Step 4: 运行测试确认通过**

Run: `cd sz-agent && uv run pytest tests/test_tools_security.py -v`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add sz-agent/app/clients sz-agent/app/agent/__init__.py sz-agent/app/agent/context.py sz-agent/app/agent/tools sz-agent/tests/test_tools_security.py
git commit -m "feat(agent): add internal mall HTTP client and runtime-isolated LangChain tools"
```

---

### Task 7: LangGraph 主图路由、5 大分支/子图、`interrupt` 下单确认与 Checkpointer

**Files:**
- Create: `sz-agent/app/agent/llm.py`
- Create: `sz-agent/app/agent/state.py`
- Create: `sz-agent/app/agent/prompts.py`
- Create: `sz-agent/app/agent/subgraphs/__init__.py`
- Create: `sz-agent/app/agent/subgraphs/recommend_order.py`
- Create: `sz-agent/app/agent/subgraphs/consult_after_sales.py`
- Create: `sz-agent/app/agent/nodes/__init__.py`
- Create: `sz-agent/app/agent/nodes/chitchat.py`
- Create: `sz-agent/app/agent/nodes/handoff.py`
- Create: `sz-agent/app/agent/nodes/fallback.py`
- Create: `sz-agent/app/agent/graph.py`
- Create: `sz-agent/tests/test_graph_flows.py`

**Interfaces:**
- Consumes: `AgentRuntimeContext`, `build_shopping_tools`, `build_after_sales_tools`, `MallInternalClient`, `TicketRepository`, `Settings`
- Produces:
  - `CustomerServiceState`, `OrderDraft`, `RouterDecision`, `HandoffSummary` (`app/agent/state.py`)
  - `build_customer_service_graph(checkpointer, mall_client, kb_retriever, ticket_repo_factory, llm_factory) -> CompiledStateGraph` (`app/agent/graph.py`)

- [ ] **Step 1: 编写主图 5 分支、`interrupt` 下单恢复与防卡死失败测试（覆盖 Review Focus #4）**

使用假模型（Stub ChatModel / 注入 `llm_factory`）与 `langgraph.checkpoint.memory.InMemorySaver`（单测内替代 PG Checkpointer，PG 集成单独标记）测试：
1. `test_intent_router_routes_all_5_branches_and_low_confidence_to_fallback`（`<0.6` 强制走 `fallback_node` 列出 4 项服务）；
2. `test_recommend_order_interrupt_confirm_and_cancel`：产出 `pending_order_draft` 后图在 `confirm_order_interrupt` 挂起并返回 `__interrupt__` payload；传入 `Command(resume={"action": "confirm", "address_id": 88})` 后调 `mall_client.direct_create_order` 生成 `order_created` 卡片（含 `/order_payment?orderNo=...`）；传入 `action="cancel"` 则不调建单接口；
3. `test_new_message_during_interrupt_auto_cancels_draft`：挂起后模拟清理调用 `Command(resume={"action": "cancel"})` 后再发新文本消息，断言绝不触发 `direct_create_order` 且正常进入新意图路由；
4. `test_handoff_clarifies_empty_first_turn_and_creates_ticket_with_context`：首句仅发“转人工”先追问 1 句；带有订单/问题描述时直接创建 `OPEN` 工单并产出 `ticket_created` 卡片；
5. `test_after_sales_status1_refund_guides_to_ticket_and_attaches_citations`：查询 `status=1`（待发货）退款时说明不可自助售后并引导人工工单，回答法规条款附带 `rag_citations`。

- [ ] **Step 2: 运行测试确认失败**

Run: `cd sz-agent && uv run pytest tests/test_graph_flows.py -v`
Expected: FAIL (`ModuleNotFoundError: No module named 'app.agent.graph'`)

- [ ] **Step 3: 实现主图、两个 `create_agent` 子图及 `chitchat`/`handoff`/`fallback` 节点**

- `recommend_order.py` 与 `consult_after_sales.py` 使用 `from langchain.agents import create_agent` 配合 `context_schema=AgentRuntimeContext` 构建专业顾问；并在 `recommend_order_subgraph` 内部通过 `interrupt({"type": "order_confirm", "draft": draft})` 实现挂起确认，`execute_create_order` 仅在 `action == "confirm"` 时以 `draft["draft_id"]` 作为 `idempotency_key` 建单。
- `graph.py` 组装 `StateGraph(CustomerServiceState, context_schema=AgentRuntimeContext)`，编译时传入 `checkpointer`（生产走 `AsyncPostgresSaver`）。

- [ ] **Step 4: 运行测试确认通过**

Run: `cd sz-agent && uv run pytest tests/test_graph_flows.py -v`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add sz-agent/app/agent/llm.py sz-agent/app/agent/state.py sz-agent/app/agent/prompts.py sz-agent/app/agent/subgraphs sz-agent/app/agent/nodes sz-agent/app/agent/graph.py sz-agent/tests/test_graph_flows.py
git commit -m "feat(agent): implement LangGraph main router, create_agent subgraphs, and order interrupt flow"
```

---

### Task 8: FastAPI 前后台路由与 SSE 流式编排服务

**Files:**
- Create: `sz-agent/app/schemas/__init__.py`
- Create: `sz-agent/app/schemas/cs.py`
- Create: `sz-agent/app/services/__init__.py`
- Create: `sz-agent/app/services/chat_service.py`
- Create: `sz-agent/app/routers/__init__.py`
- Create: `sz-agent/app/routers/client_cs.py`
- Create: `sz-agent/app/routers/admin_cs.py`
- Modify: `sz-agent/app/main.py`
- Create: `sz-agent/tests/test_api_e2e.py`

**Interfaces:**
- Consumes: `SessionRepository`, `TicketRepository`, `KbRepository`, `KbIngestionService`, `build_customer_service_graph`, `verify_internal_secret`, `get_client_user_id`, `get_admin_actor`
- Produces:
  - `/internal/cs/client/**`（会话列表/创建、消息历史、SSE `/chat/stream`、SSE `/resume/stream`、我的工单列表/详情/追问）
  - `/internal/cs/admin/**`（工单分页/详情含关联会话回放/回复/关闭、KB 文档列表/上传/重建/删除）

- [ ] **Step 1: 编写 FastAPI 端到端与 SSE 流式契约失败测试（覆盖 Review Focus #1, #4, #5）**

测试断言：
1. `test_cross_user_access_forbidden`：用户 B 请求用户 A 的 `/internal/cs/client/sessions/{sid}/messages` 与 `/tickets/{tid}` 返回 404/403；
2. `test_chat_stream_emits_meta_token_card_interrupt_done_and_persists_db`：验证 SSE 标准 `event: meta` / `token` / `interrupt` / `done` 格式，并落库 `cs_messages` 与 `has_pending_interrupt=True`；
3. `test_chat_stream_auto_cancels_pending_interrupt`：若 `has_pending_interrupt=True` 时用户又请求 `/chat/stream`，服务端先自动执行 `cancel` 恢复清理旧检查点后再处理新消息；
4. `test_admin_reply_updates_session_and_user_followup_reopens_ticket`：管理员通过 `/internal/cs/admin/tickets/{tid}/reply` 回复后，用户查询原会话消息列表可见系统回复摘要；用户追问后工单重新变为 `OPEN`；
5. `test_kb_upload_and_delete_lifecycle`：通过 `/internal/cs/admin/kb/documents/upload` 上传 PDF（带 SHA-256 去重校验），触发后台入库，并测试删除接口清理向量与 PG 记录；
6. `test_sse_disconnect_stops_generation`：模拟客户端断开（`is_disconnected()` 返回 `True`），验证流提前中止且不把半截残缺内容误标为完成中断态。

- [ ] **Step 2: 运行测试确认失败**

Run: `cd sz-agent && uv run pytest tests/test_api_e2e.py -v`
Expected: FAIL

- [ ] **Step 3: 实现 `schemas/cs.py`、`services/chat_service.py`、`routers/{client_cs,admin_cs}.py` 与更新 `app/main.py`**

- `ChatService` 使用 `graph.astream(..., stream_mode=["messages", "updates"], context=AgentRuntimeContext(...), config={"configurable": {"thread_id": f"u:{user_id}:s:{session_id}"}})` 输出统一 SSE 事件；15s 无产出发送 `: heartbeat\n\n`；若收到 `await request.is_disconnected()` 及时取消后台流式任务。
- `app/main.py` 在 `lifespan` 初始化 `AsyncPostgresSaver`、`MilvusKbStore.ensure_collection()`、`MallInternalClient` 与编译后的图实例（支持测试依赖注入覆盖 `app.state`）。

- [ ] **Step 4: 运行 Python 全量测试确认通过**

Run: `cd sz-agent && uv run pytest -v`
Expected: PASS (Task 1, 2, 5, 6, 7, 8 所有测试全部通过)

- [ ] **Step 5: 提交**

```bash
git add sz-agent/app/schemas sz-agent/app/services sz-agent/app/routers sz-agent/app/main.py sz-agent/tests/test_api_e2e.py
git commit -m "feat(agent): add FastAPI client/admin CS routers and SSE streaming service"
```

---

### Task 9: `sz-pc` 前台悬浮智能客服抽屉与卡片交互

**Files:**
- Create: `sz-pc/src/api/types/cs.ts`
- Create: `sz-pc/src/api/modules/cs.ts`
- Modify: `sz-pc/src/api/index.ts:1-16`
- Create: `sz-pc/src/components/CustomerService/ChatMessageCards.vue`
- Create: `sz-pc/src/components/CustomerService/MyTicketPanel.vue`
- Create: `sz-pc/src/components/CustomerService/CustomerServiceWidget.vue`
- Modify: `sz-pc/src/App.vue:1-3`

**Interfaces:**
- Consumes: Java `/client/cs/**` (Task 4), `useAuthStore` (`sz-pc/src/stores/modules/auth.ts`), `import.meta.env.VITE_API_URL`
- Produces: 全局挂载 `<CustomerServiceWidget />`；支持 `fetch` + `ReadableStream` 解析 SSE（携带 `Authorization: Bearer ${token}`）

- [ ] **Step 1: 创建 `sz-pc/src/api/types/cs.ts` 与 `sz-pc/src/api/modules/cs.ts` 并导出至 `sz-pc/src/api/index.ts`**

定义会话、消息、推荐商品卡、下单确认卡、待支付订单卡（含 `/order_payment?orderNo=...`）、引用卡与工单接口类型；`streamCsChat` / `streamCsResume` 使用原生 `fetch` + `AbortController` 逐块解码 SSE `event:` 与 `data:`（避免 Axios 不支持流式逐行回调）。

- [ ] **Step 2: 实现 `ChatMessageCards.vue`、`MyTicketPanel.vue` 与 `CustomerServiceWidget.vue`**

- `CustomerServiceWidget.vue`：监听当前路由，`/login` 时隐藏；未登录点击悬浮按钮触发提示并 `router.push({ path: '/login', query: { redirect: route.fullPath } })`；抽屉提供「在线咨询」与「我的工单」双 Tab、新建/切换会话、首屏快捷引导芯片与流式中止按钮。
- `ChatMessageCards.vue`：渲染商品推荐卡（跳 `/product/:id` 或一键发“帮我买这件”）、下单确认卡（支持切换地址下拉与防连点「确认提交订单」/「暂不下单」触发 `resume/stream`）、待支付订单卡（点击跳转 `/order_payment?orderNo=...`）、工单已生成卡与可折叠法规引用条。
- `MyTicketPanel.vue`：展示工单列表、详情时间轴及在 `OPEN`/`REPLIED` 状态下的追问输入框。

- [ ] **Step 3: 在 `sz-pc/src/App.vue` 挂载 `<CustomerServiceWidget />`**

- [ ] **Step 4: 运行 `sz-pc` 类型检查与构建验证**

Run: `npm --prefix sz-pc run type-check && npm --prefix sz-pc run build`
Expected: PASS (零 TS 报错且 Vite 构建成功)

- [ ] **Step 5: 提交**

```bash
git add sz-pc/src/api/types/cs.ts sz-pc/src/api/modules/cs.ts sz-pc/src/api/index.ts sz-pc/src/components/CustomerService sz-pc/src/App.vue
git commit -m "feat(pc): add floating KlackCraft intelligent CS drawer with order interrupt cards"
```

---

### Task 10: `sz-admin` 后台「客服工单管理」与「客服知识库管理」

**Files:**
- Create: `sz-admin/src/api/types/shop_system/cs.ts`
- Create: `sz-admin/src/api/modules/shop_system/cs.ts`
- Create: `sz-admin/src/views/shop_system/csTicket/components/TicketDetailDrawer.vue`
- Create: `sz-admin/src/views/shop_system/csTicket/index.vue`
- Create: `sz-admin/src/views/shop_system/csKnowledge/components/KbUploadDialog.vue`
- Create: `sz-admin/src/views/shop_system/csKnowledge/index.vue`

**Interfaces:**
- Consumes: Java `/admin/cs/**` (Task 4), `ProTable` (`sz-admin/src/components/ProTable/index.vue`), `ADMIN_MODULE`
- Produces: 组件路径 `/shop_system/csTicket/index` 与 `/shop_system/csKnowledge/index`（与 `016_customer_service_menus.sql` 一一对应）

- [ ] **Step 1: 创建 `sz-admin/src/api/types/shop_system/cs.ts` 与 `sz-admin/src/api/modules/shop_system/cs.ts`**

封装工单分页、详情、回复、关闭接口与知识库分页、`FormData` PDF 上传、重新入库、删除接口。

- [ ] **Step 2: 实现「客服工单管理」页面与 `TicketDetailDrawer.vue`**

沿用 `ProTable` 规范展示工单编号、分类、状态（`OPEN`/`REPLIED`/`CLOSED`）、关联订单号与时间；详情抽屉分三区展示前情摘要、折叠历史会话回放、回复时间轴与「发送回复」/「回复并关闭工单」操作（校验 `cs.ticket.reply` / `cs.ticket.close` 权限）。

- [ ] **Step 3: 实现「客服知识库管理」页面与 `KbUploadDialog.vue`**

支持 PDF 上传（分类 `consumer_law` / `mall_policy` / `faq` / `product_guide`）、`PENDING`/`PARSING` 态自动轻量轮询刷新、失败「重新解析入库」与二次确认「删除」。

- [ ] **Step 4: 运行 `sz-admin` 类型检查与构建验证**

Run: `npm --prefix sz-admin run type-check && npm --prefix sz-admin run build-only`
Expected: PASS (零 TS 报错且构建成功)

- [ ] **Step 5: 提交**

```bash
git add sz-admin/src/api/types/shop_system/cs.ts sz-admin/src/api/modules/shop_system/cs.ts sz-admin/src/views/shop_system/csTicket sz-admin/src/views/shop_system/csKnowledge
git commit -m "feat(admin): add customer service ticket and knowledge base management views"
```

---

### Task 11: 内置法规与规范 PDF 生成、文档与四端全量联调验证

**Files:**
- Create: `sz-agent/scripts/build_knowledge_pdfs.py`
- Create: `sz-agent/knowledge_docs/01_consumer_rights_protection_law.pdf`
- Create: `sz-agent/knowledge_docs/02_consumer_rights_implementation_regulations.pdf`
- Create: `sz-agent/knowledge_docs/03_online_7day_no_reason_return_measures.pdf`
- Create: `sz-agent/knowledge_docs/04_klackcraft_mall_service_policy.pdf`
- Create: `sz-agent/README.md`

**Interfaces:**
- Consumes: `app/kb/seed.py` (Task 5)
- Produces: 4 份可被 MinerU 正常扫描解析的标准中文 PDF（含完整法条与已确认商城口径：现货 48h 发货、7 天无理由含已拆封轴体/键帽/客制化除外、质量问题商家包邮/非质量问题买家承担运费、待发货 `status=1` 走人工工单退款）与完整启动手册。

- [ ] **Step 1: 检索权威法条原文并生成 4 份标准 PDF**

核对《中华人民共和国消费者权益保护法》《中华人民共和国消费者权益保护法实施条例》《网络购买商品七日无理由退货暂行办法》最新权威条文及已确认的《咔嗒造物商城发货时效、运费与售后退换货服务规范》，通过 `sz-agent/scripts/build_knowledge_pdfs.py`（中文字体渲染为清晰分页、标题层级分明的 PDF）产出 4 份文档至 `sz-agent/knowledge_docs/`，并验证 `pypdf` 可正常读取页数且 `app/kb/chunker.py` 能切分出准确 `section_path`。

- [ ] **Step 2: 编写 `sz-agent/README.md` 本地启动与端到端验收指南**

记录 `docker compose up -d`、Alembic `uv run alembic upgrade head`、`uv run python -m app.kb.seed`、FastAPI 启动、`sz-boot-parent` / `sz-pc` / `sz-admin` 联调启动顺序及环境变量说明。

- [ ] **Step 3: 执行四端全量自动化检查**

```bash
cd /Users/car/Project/library_system/sz-agent && uv run pytest -v
mvn -f /Users/car/Project/library_system/sz-boot-parent/pom.xml -pl sz-service/sz-service-admin -am test -Dtest=AgentInternalAuthInterceptorTest,AgentMallServiceImplTest,AgentProxyServiceTest
npm --prefix /Users/car/Project/library_system/sz-pc run type-check && npm --prefix /Users/car/Project/library_system/sz-pc run build
npm --prefix /Users/car/Project/library_system/sz-admin run type-check && npm --prefix /Users/car/Project/library_system/sz-admin run build-only
```
Expected: Python pytest、Java 单元测试、`sz-pc` 构建、`sz-admin` 构建全部 PASS。

- [ ] **Step 4: 提交**

```bash
git add sz-agent/scripts/build_knowledge_pdfs.py sz-agent/knowledge_docs sz-agent/README.md
git commit -m "docs(agent): add built-in consumer law and mall policy PDFs with setup guide"
```

---

## Self-Review Checkpoints
- **规格全覆盖**：对应设计文档第 1~9 节（跨栈隔离 4 防线、5 大意图及防中断卡死、MinerU + Qwen 1024 + Milvus `kada_cs_kb_chunks`、PG 5 表 + 016 菜单、Java `/internal/agent/**` & `/client/cs/**` & `/admin/cs/**`、PC 悬浮窗 + Admin 双页面、4 份 PDF 与 Docker 环境）。
- **一致性检查**：`thread_id` 统一 `f"u:{user_id}:s:{session_id}"`；支付跳转统一 `/order_payment?orderNo=...`；`sz-admin` 组件路径与 `016_customer_service_menus.sql` 一致（`/shop_system/csTicket/index`、`/shop_system/csKnowledge/index`）；Java 测试上下文默认路径 `/api`。

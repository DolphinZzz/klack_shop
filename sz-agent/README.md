# sz-agent — 咔嗒造物商城（KlackCraft）智能客服 Agent 微服务

`sz-agent` 是咔嗒造物商城的 Python 内网智能客服微服务，基于 FastAPI、LangChain v1（`create_agent` + `ToolRuntime`）、LangGraph（显式 `StateGraph` 路由 + 子图 + `AsyncPostgresSaver` + `interrupt` 下单确认）、SQLAlchemy 2.0（PostgreSQL 16）、Milvus（Qwen `text-embedding-v4` 1024 维向量检索）与 MinerU 构建。

> **安全边界**：`sz-agent` 仅绑定内网接口（默认 `127.0.0.1:8000`），不直接暴露给公网。用户端（`/client/cs/**`）与管理端（`/admin/cs/**`）请求均由 `sz-boot-parent` 完成 Sa-Token 鉴权后，携带 `X-Internal-Secret` 与可信身份头（`X-User-Id` / `X-Admin-Id`）透传至本服务。

---

## 1. 目录结构

```text
sz-agent/
├── alembic/                  # PostgreSQL 业务表迁移脚本
├── app/
│   ├── agent/                # LangGraph 主图、子图、DeepSeek LLM 工厂与工具
│   ├── clients/              # 调用 Java /internal/agent/** 的内网 HTTP 客户端
│   ├── db/                   # SQLAlchemy 异步引擎与 5 张核心业务表模型
│   ├── kb/                   # MinerU 解析器、结构感知切块器、Milvus 存储与种子脚本
│   ├── repositories/         # 会话、消息、工单、知识库异步仓储
│   ├── routers/              # /internal/cs/client/** 与 /internal/cs/admin/**
│   ├── schemas/              # Pydantic 请求/响应契约
│   ├── security/             # X-Internal-Secret 常量时间校验与身份头依赖
│   └── services/             # SSE 流式对话编排、中断恢复与断连保护
├── knowledge_docs/           # 4 份内置消费者保护法规与商城服务规范 PDF
├── scripts/
│   └── build_knowledge_pdfs.py  # 生成内置知识库标准中文 PDF
├── tests/                    # pytest 单元测试与端到端 Mock 测试
├── .env.example              # 环境变量模板
├── alembic.ini
├── docker-compose.yml        # PostgreSQL 16 + etcd + MinIO + Milvus Standalone
└── pyproject.toml
```

---

## 2. 环境准备与基础设施启动

### 2.1 前置依赖

- Python `>= 3.12` 与 [`uv`](https://docs.astral.sh/uv/)
- Docker / Docker Compose
- JDK 21 & Maven（运行 `sz-boot-parent`）
- Node.js 20+ & npm（运行 `sz-pc` / `sz-admin`）
- 可用凭据：`DEEPSEEK_API_KEY`、`DASHSCOPE_API_KEY`（阿里云 DashScope），可选 `MINERU_API_TOKEN`（用于 MinerU Cloud Precision 解析；未配置时自动按页拆分降级 Flash 模式或切换 `local_http` / `local_cli`）

### 2.2 启动 PostgreSQL 与 Milvus

```bash
cd sz-agent
docker compose up -d
```

等待容器健康检查通过（`kada-cs-postgres`、`kada-cs-etcd`、`kada-cs-minio`、`kada-cs-milvus`）：

```bash
docker compose ps
```

### 2.3 配置 `.env` 并安装依赖

```bash
cd sz-agent
cp .env.example .env
uv sync --extra dev
```

按实际环境填写 `.env`：

| 变量名 | 默认值 / 示例 | 说明 |
| :--- | :--- | :--- |
| `APP_HOST` / `APP_PORT` | `127.0.0.1` / `8000` | 仅监听内网回环或容器内网地址 |
| `AGENT_INTERNAL_SECRET` | `kada-local-internal-secret` | 必须与 `sz-boot-parent/config/<env>/agent.yml` 中的 `sz.agent.internal-secret` 一致 |
| `JAVA_INTERNAL_BASE_URL` | `http://127.0.0.1:9991/api` | Java `sz-service-admin` 上下文根路径（`local` 环境为 `9991/api`） |
| `POSTGRES_DSN` | `postgresql+asyncpg://kada:kada_pass@127.0.0.1:5432/kada_cs` | 客服会话/消息/工单/KB/Checkpoint 数据库连接串 |
| `MILVUS_URI` | `http://127.0.0.1:19530` | Milvus Standalone gRPC 地址 |
| `MILVUS_COLLECTION` | `kada_cs_kb_chunks` | 知识库向量集合名称（1024 维 `COSINE` HNSW） |
| `DEEPSEEK_API_KEY` | *(必填)* | DeepSeek API 密钥 |
| `DASHSCOPE_API_KEY` | *(必填)* | 阿里云百炼 / DashScope API 密钥（`text-embedding-v4`） |
| `MINERU_MODE` | `cloud_api` | 支持 `cloud_api` / `local_http` / `local_cli` |
| `MINERU_API_TOKEN` | *(推荐填写)* | MinerU Cloud Precision 模式 Token |
| `KB_STORAGE_DIR` | `./storage/kb_docs` | 上传 PDF 与解析 Markdown 缓存目录 |

---

## 3. 数据库迁移与知识库初始化

### 3.1 执行 Alembic 迁移

```bash
cd sz-agent
uv run alembic upgrade head
```

> 说明：LangGraph Checkpoint 相关表（`checkpoints` 等）会在 FastAPI 应用首次启动 `lifespan` 时由 `AsyncPostgresSaver.setup()` 自动建表。

### 3.2 生成 / 检查内置 PDF 并初始化知识库向量

仓库已内置 4 份标准中文 PDF 于 `sz-agent/knowledge_docs/`（如需重新生成可运行 `uv run python scripts/build_knowledge_pdfs.py`）：

1. `01_consumer_rights_protection_law.pdf` — 《中华人民共和国消费者权益保护法》（`consumer_law`）
2. `02_consumer_rights_implementation_regulations.pdf` — 《中华人民共和国消费者权益保护法实施条例》（`consumer_law`）
3. `03_online_7day_no_reason_return_measures.pdf` — 《网络购买商品七日无理由退货暂行办法》（`consumer_law`）
4. `04_klackcraft_mall_service_policy.pdf` — 《咔嗒造物商城发货时效、运费与售后退换货服务规范》（`mall_policy`）

执行幂等种子入库（按 `file_sha256` 去重，自动调用 MinerU 解析、结构感知切块、Qwen Embedding 向量化并写入 Milvus）：

```bash
cd sz-agent
uv run python -m app.kb.seed
```

---

## 4. 四端联调启动顺序

1. **启动基础设施**：
   - 商城基础依赖（MySQL、Redis）就绪；
   - `sz-agent` 依赖（PostgreSQL 16、Milvus）通过 `cd sz-agent && docker compose up -d` 启动。
2. **启动 Python 智能客服服务（`127.0.0.1:8000`）**：
   ```bash
   cd sz-agent
   uv run uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
   ```
   健康检查：`curl -s http://127.0.0.1:8000/health` 返回 `{"status":"ok"}`。
3. **启动 Java 商城后端（`127.0.0.1:9991/api`）**：
   - 使用 `local` profile 启动 `sz-boot-parent/sz-service/sz-service-admin`（Liquibase 会自动执行 `016_customer_service_menus.sql` 注册「客服工单管理」与「客服知识库管理」菜单及权限码）；
   - 确认 `sz-boot-parent/config/local/agent.yml` 中的 `sz.agent.base-url` 与 `sz.agent.internal-secret` 与 `sz-agent/.env` 一致。
4. **启动 PC 商城前端（`http://localhost:9850`）**：
   ```bash
   cd sz-pc
   npm install
   npm run dev
   ```
5. **启动管理后台前端（`http://localhost:9848`）**：
   ```bash
   cd sz-admin
   npm install
   npm run dev
   ```

---

## 5. 全量自动化验证命令

### 5.1 Python（`sz-agent`）

```bash
cd sz-agent
uv run pytest -v
```

### 5.2 Java（`sz-boot-parent`）

```bash
mvn -f sz-boot-parent/pom.xml -pl sz-service/sz-service-admin -am test \
  -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=AgentInternalAuthInterceptorTest,AgentMallServiceImplTest,AgentProxyServiceTest
```

### 5.3 PC 商城前端（`sz-pc`）

```bash
npm --prefix sz-pc run type-check && npm --prefix sz-pc run build
```

### 5.4 管理后台前端（`sz-admin`）

```bash
npm --prefix sz-admin run type-check
npm --prefix sz-admin run build-only
```

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """`sz-agent` 智能客服应用的运行时配置。"""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    # 服务监听与内网安全配置
    app_host: str = "127.0.0.1"
    app_port: int = 8000
    agent_internal_secret: str = ""
    java_internal_base_url: str = "http://127.0.0.1:9991/api"
    java_internal_timeout_seconds: float = 15.0

    # 数据库与向量存储配置
    postgres_dsn: str = "postgresql+asyncpg://kada:kada_pass@127.0.0.1:5432/kada_cs"
    milvus_uri: str = "http://127.0.0.1:19530"
    milvus_collection: str = "kada_cs_kb_chunks"

    # 大语言模型与向量嵌入模型配置
    deepseek_api_key: str = ""
    deepseek_base_url: str = "https://api.deepseek.com"
    deepseek_model: str = "deepseek-chat"
    dashscope_api_key: str = ""
    dashscope_base_url: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    qwen_embedding_model: str = "text-embedding-v4"
    qwen_embedding_dimensions: int = 1024

    # MinerU 解析与知识库文档存储配置
    mineru_mode: str = "cloud_api"
    mineru_api_token: str = ""
    mineru_base_url: str = ""
    kb_storage_dir: str = "./storage/kb_docs"


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    """返回从环境变量加载并缓存的 `Settings` 实例。"""
    return Settings()

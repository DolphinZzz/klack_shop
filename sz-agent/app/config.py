from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Runtime settings for the sz-agent customer service application."""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    # Service & internal security
    app_host: str = "127.0.0.1"
    app_port: int = 8000
    agent_internal_secret: str = ""
    java_internal_base_url: str = "http://127.0.0.1:9991/api"
    java_internal_timeout_seconds: float = 15.0

    # Database & vector store
    postgres_dsn: str = "postgresql+asyncpg://kada:kada_pass@127.0.0.1:5432/kada_cs"
    milvus_uri: str = "http://127.0.0.1:19530"
    milvus_collection: str = "kada_cs_kb_chunks"

    # LLM & embedding models
    deepseek_api_key: str = ""
    deepseek_base_url: str = "https://api.deepseek.com"
    deepseek_model: str = "deepseek-chat"
    dashscope_api_key: str = ""
    dashscope_base_url: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    qwen_embedding_model: str = "text-embedding-v4"
    qwen_embedding_dimensions: int = 1024

    # MinerU & KB document storage
    mineru_mode: str = "cloud_api"
    mineru_api_token: str = ""
    mineru_base_url: str = ""
    kb_storage_dir: str = "./storage/kb_docs"


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    """Return a cached Settings instance loaded from environment variables."""
    return Settings()

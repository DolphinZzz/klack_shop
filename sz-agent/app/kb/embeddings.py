from __future__ import annotations

from langchain_openai import OpenAIEmbeddings

from app.config import Settings, get_settings

DASHSCOPE_EMBEDDING_MAX_BATCH_SIZE = 10


def get_qwen_embeddings(
    settings: Settings | None = None,
    *,
    chunk_size: int = DASHSCOPE_EMBEDDING_MAX_BATCH_SIZE,
) -> OpenAIEmbeddings:
    """Build an OpenAIEmbeddings client targeting DashScope Qwen text-embedding-v4."""
    cfg = settings or get_settings()
    if not cfg.dashscope_api_key:
        raise ValueError("DASHSCOPE_API_KEY must be configured to create Qwen embeddings")

    return OpenAIEmbeddings(
        model=cfg.qwen_embedding_model,
        api_key=cfg.dashscope_api_key,
        base_url=cfg.dashscope_base_url,
        dimensions=cfg.qwen_embedding_dimensions,
        chunk_size=chunk_size,
        model_kwargs={"encoding_format": "float"},
        check_embedding_ctx_length=False,
    )

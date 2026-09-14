from dataclasses import dataclass
import math
import os


def _env(name: str) -> str:
    return os.getenv(name, "").strip()


def _bounded_float(name: str, default: float, minimum: float, maximum: float | None = None) -> float:
    raw = _env(name)
    if not raw:
        return default
    try:
        value = float(raw)
    except ValueError:
        return default
    if not math.isfinite(value) or value < minimum or (maximum is not None and value > maximum):
        return default
    return value


_MODEL_API_BASE = _env("AI_MODEL_API_BASE").rstrip("/")
_MODEL_API_KEY = _env("AI_MODEL_API_KEY")
_EMBEDDING_API_BASE = _env("AI_EMBEDDING_API_BASE").rstrip("/")
_EMBEDDING_API_KEY = _env("AI_EMBEDDING_API_KEY")
_RERANK_API_BASE = _env("AI_RERANK_API_BASE").rstrip("/")
_RERANK_API_KEY = _env("AI_RERANK_API_KEY")


@dataclass(frozen=True)
class Settings:
    enabled: bool = _env("AI_ENABLED").lower() not in {"0", "false", "no", "off"}
    service_token: str = _env("AI_SERVICE_TOKEN") or "change-me-local-only"
    model_api_base: str = _MODEL_API_BASE
    model_api_key: str = _MODEL_API_KEY
    model_name: str = _env("AI_MODEL_NAME")
    model_include_usage: bool = _env("AI_MODEL_INCLUDE_USAGE").lower() in {"1", "true", "yes", "on"}
    embedding_api_base: str = _EMBEDDING_API_BASE or _MODEL_API_BASE
    embedding_api_key: str = _EMBEDDING_API_KEY or (_MODEL_API_KEY if not _EMBEDDING_API_BASE else "")
    embedding_model: str = _env("AI_EMBEDDING_MODEL")
    embedding_dimensions: int = int(_env("AI_EMBEDDING_DIMENSIONS") or "1536")
    embedding_min_score: float = float(_env("AI_EMBEDDING_MIN_SCORE") or "0.7")
    rerank_api_base: str = _RERANK_API_BASE
    rerank_api_key: str = _RERANK_API_KEY
    rerank_model: str = _env("AI_RERANK_MODEL")
    sla_p95_ms: float = _bounded_float("AI_SLA_P95_MS", 2000, 1)
    sla_error_rate: float = _bounded_float("AI_SLA_ERROR_RATE", 0.05, 0, 1)
    elasticsearch_url: str = (_env("AI_ELASTICSEARCH_URL") or "http://elasticsearch:9200").rstrip("/")

    @property
    def has_model(self) -> bool:
        return bool(self.model_api_base and self.model_api_key and self.model_name)

    @property
    def has_embedding(self) -> bool:
        return bool(self.embedding_api_base and self.embedding_api_key and self.embedding_model and self.embedding_dimensions > 0)

    @property
    def has_reranker(self) -> bool:
        return bool(self.rerank_api_base and self.rerank_api_key and self.rerank_model)


settings = Settings()

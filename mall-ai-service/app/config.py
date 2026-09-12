from dataclasses import dataclass
import os


def _env(name: str) -> str:
    return os.getenv(name, "").strip()


_MODEL_API_BASE = _env("AI_MODEL_API_BASE").rstrip("/")
_MODEL_API_KEY = _env("AI_MODEL_API_KEY")
_EMBEDDING_API_BASE = _env("AI_EMBEDDING_API_BASE").rstrip("/")
_EMBEDDING_API_KEY = _env("AI_EMBEDDING_API_KEY")


@dataclass(frozen=True)
class Settings:
    service_token: str = _env("AI_SERVICE_TOKEN") or "change-me-local-only"
    model_api_base: str = _MODEL_API_BASE
    model_api_key: str = _MODEL_API_KEY
    model_name: str = _env("AI_MODEL_NAME")
    embedding_api_base: str = _EMBEDDING_API_BASE or _MODEL_API_BASE
    embedding_api_key: str = _EMBEDDING_API_KEY or (_MODEL_API_KEY if not _EMBEDDING_API_BASE else "")
    embedding_model: str = _env("AI_EMBEDDING_MODEL")
    embedding_dimensions: int = int(_env("AI_EMBEDDING_DIMENSIONS") or "1536")
    embedding_min_score: float = float(_env("AI_EMBEDDING_MIN_SCORE") or "0.7")
    elasticsearch_url: str = (_env("AI_ELASTICSEARCH_URL") or "http://elasticsearch:9200").rstrip("/")

    @property
    def has_model(self) -> bool:
        return bool(self.model_api_base and self.model_api_key and self.model_name)

    @property
    def has_embedding(self) -> bool:
        return bool(self.embedding_api_base and self.embedding_api_key and self.embedding_model and self.embedding_dimensions > 0)


settings = Settings()

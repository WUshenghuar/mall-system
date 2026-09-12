from dataclasses import dataclass
import os


@dataclass(frozen=True)
class Settings:
    service_token: str = os.getenv("AI_SERVICE_TOKEN", "change-me-local-only")
    model_api_base: str = os.getenv("AI_MODEL_API_BASE", "").rstrip("/")
    model_api_key: str = os.getenv("AI_MODEL_API_KEY", "")
    model_name: str = os.getenv("AI_MODEL_NAME", "")
    embedding_api_base: str = os.getenv("AI_EMBEDDING_API_BASE", os.getenv("AI_MODEL_API_BASE", "")).rstrip("/")
    embedding_api_key: str = os.getenv("AI_EMBEDDING_API_KEY", os.getenv("AI_MODEL_API_KEY", ""))
    embedding_model: str = os.getenv("AI_EMBEDDING_MODEL", "")
    embedding_dimensions: int = int(os.getenv("AI_EMBEDDING_DIMENSIONS", "1536"))
    embedding_min_score: float = float(os.getenv("AI_EMBEDDING_MIN_SCORE", "0.7"))
    elasticsearch_url: str = os.getenv("AI_ELASTICSEARCH_URL", "http://elasticsearch:9200").rstrip("/")

    @property
    def has_model(self) -> bool:
        return bool(self.model_api_base and self.model_api_key and self.model_name)

    @property
    def has_embedding(self) -> bool:
        return bool(self.embedding_api_base and self.embedding_api_key and self.embedding_model and self.embedding_dimensions > 0)


settings = Settings()

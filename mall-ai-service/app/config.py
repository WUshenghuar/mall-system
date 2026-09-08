from dataclasses import dataclass
import os


@dataclass(frozen=True)
class Settings:
    service_token: str = os.getenv("AI_SERVICE_TOKEN", "change-me-local-only")
    model_api_base: str = os.getenv("AI_MODEL_API_BASE", "").rstrip("/")
    model_api_key: str = os.getenv("AI_MODEL_API_KEY", "")
    model_name: str = os.getenv("AI_MODEL_NAME", "")
    elasticsearch_url: str = os.getenv("AI_ELASTICSEARCH_URL", "http://elasticsearch:9200").rstrip("/")

    @property
    def has_model(self) -> bool:
        return bool(self.model_api_base and self.model_api_key and self.model_name)


settings = Settings()

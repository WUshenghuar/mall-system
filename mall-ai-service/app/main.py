from fastapi import FastAPI, Header, HTTPException

from app.api.chat import router as chat_router
from app.api.knowledge import router as knowledge_router
from app.config import settings
from app.rag.retriever import ensure_seeded
from app.observability import metrics
from app.telemetry import configure_telemetry, telemetry_configured

app = FastAPI(title="CBEC AI Customer Service", version="0.1.0")
configure_telemetry()
app.include_router(chat_router)
app.include_router(knowledge_router)


@app.on_event("startup")
async def seed_knowledge():
    try:
        await ensure_seeded()
    except Exception:
        # ES 未就绪时保留客服降级回复，后续检索会自动重试。
        pass


@app.get("/health")
async def health():
    return {
        "status": "ok" if settings.enabled else "disabled",
        "service": "cbec-ai-customer-service",
        "enabled": settings.enabled,
        "modelConfigured": settings.enabled and settings.has_model,
        "embeddingConfigured": settings.enabled and settings.has_embedding,
        "rerankerConfigured": settings.enabled and settings.has_reranker,
        "toolPlanningEnabled": settings.enabled and settings.has_model,
        "telemetryConfigured": telemetry_configured(),
    }


@app.get("/internal/metrics")
async def internal_metrics(x_ai_service_token: str = Header(default="")):
    if x_ai_service_token != settings.service_token:
        raise HTTPException(status_code=401, detail="invalid AI service token")
    return metrics.snapshot(settings.sla_p95_ms, settings.sla_error_rate)

"""FastAPI 服务入口，负责挂载客服路由并初始化可选基础设施。"""
from contextlib import asynccontextmanager

from fastapi import FastAPI, Header, HTTPException

from app.api.chat import router as chat_router
from app.api.knowledge import router as knowledge_router
from app.config import settings
from app.rag.retriever import ensure_seeded
from app.observability import metrics
from app.telemetry import configure_telemetry, langfuse_configured, telemetry_configured

@asynccontextmanager
async def lifespan(_app: FastAPI):
    # 搜索引擎启动失败不阻塞应用；检索层会在后续请求中重试并可本地回退。
    try:
        await ensure_seeded()
    except Exception:
        # ES 未就绪时保留客服降级回复，后续检索会自动重试。
        pass
    yield


app = FastAPI(title="CBEC AI Customer Service", version="0.1.0", lifespan=lifespan)
configure_telemetry()
app.include_router(chat_router)
app.include_router(knowledge_router)


@app.get("/health")
async def health():
    return {
        "status": "ok" if settings.enabled else "disabled",
        "service": "cbec-ai-customer-service",
        "enabled": settings.enabled,
        "modelConfigured": settings.enabled and settings.has_model,
        "modelUsageEnabled": settings.enabled and settings.has_model and settings.model_include_usage,
        "embeddingConfigured": settings.enabled and settings.has_embedding,
        "rerankerConfigured": settings.enabled and settings.has_reranker,
        "langfuseConfigured": settings.enabled and langfuse_configured(),
        "toolPlanningEnabled": settings.enabled and settings.has_model,
        "telemetryConfigured": telemetry_configured(),
    }


@app.get("/internal/metrics")
async def internal_metrics(x_ai_service_token: str = Header(default="")):
    if not settings.service_token or x_ai_service_token != settings.service_token:
        raise HTTPException(status_code=401, detail="invalid AI service token")
    return metrics.snapshot(settings.sla_p95_ms, settings.sla_error_rate)

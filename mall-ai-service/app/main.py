from fastapi import FastAPI

from app.api.chat import router as chat_router
from app.rag.retriever import ensure_seeded

app = FastAPI(title="CBEC AI Customer Service", version="0.1.0")
app.include_router(chat_router)


@app.on_event("startup")
async def seed_knowledge():
    try:
        await ensure_seeded()
    except Exception:
        # ES 未就绪时保留客服降级回复，后续检索会自动重试。
        pass


@app.get("/health")
async def health():
    return {"status": "ok", "service": "cbec-ai-customer-service"}

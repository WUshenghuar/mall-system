import json
from collections.abc import AsyncIterator

from fastapi import APIRouter, Header, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field

from app.config import settings
from app.rag.retriever import retrieve
from app.utils.llm import stream_reply

router = APIRouter()


class ChatMessage(BaseModel):
    role: str
    content: str


class ChatRequest(BaseModel):
    memberId: int
    conversationId: str = Field(min_length=1, max_length=64)
    message: str = Field(min_length=1, max_length=1000)
    businessContext: str = Field(default="", max_length=1000)
    history: list[ChatMessage] = Field(default_factory=list)


def sse(payload: dict) -> str:
    return f"event: message\ndata: {json.dumps(payload, ensure_ascii=False)}\n\n"


@router.post("/internal/chat")
async def chat(request: ChatRequest, x_ai_service_token: str = Header(default="")):
    if x_ai_service_token != settings.service_token:
        raise HTTPException(status_code=401, detail="invalid AI service token")

    async def events() -> AsyncIterator[str]:
        yield sse({"type": "thinking"})
        try:
            history = [item.model_dump() for item in request.history]
            context = await retrieve(request.message) if not request.businessContext else []
            if context:
                yield sse({"type": "sources", "items": [{"title": item["title"], "category": item["category"]} for item in context]})
            async for chunk in stream_reply(request.message, history, context, request.businessContext):
                yield sse({"type": "text", "content": chunk})
            yield sse({"type": "done"})
        except Exception:
            yield sse({"type": "error", "message": "客服服务暂不可用，请稍后重试"})

    return StreamingResponse(events(), media_type="text/event-stream", headers={"Cache-Control": "no-cache"})

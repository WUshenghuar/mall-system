import json
from collections.abc import AsyncIterator
from typing import Literal

from fastapi import APIRouter, Header, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field

from app.agent.agent import should_suggest_handoff
from app.config import settings
from app.rag.retriever import retrieve
from app.utils.llm import stream_reply

router = APIRouter()

BUSINESS_SOURCES = {
    "query_order": ("订单只读查询", "business"),
    "query_logistics": ("物流只读查询", "logistics"),
    "query_refund": ("售后只读查询", "after_sales"),
    "query_product": ("商品目录查询", "product"),
    "query_coupon": ("优惠券查询", "marketing"),
    "query_member": ("会员资料查询", "member"),
    "query_tax": ("税费与币种查询", "finance"),
}


class ChatMessage(BaseModel):
    role: Literal["user", "assistant"]
    content: str


class ChatRequest(BaseModel):
    memberId: int
    conversationId: str = Field(min_length=1, max_length=64)
    message: str = Field(min_length=1, max_length=1000)
    businessContext: str = Field(default="", max_length=1000)
    businessTool: Literal["", "query_order", "query_logistics", "query_refund", "query_product", "query_coupon", "query_member", "query_tax"] = ""
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
            context = []
            if request.businessContext:
                yield sse({"type": "tool_call", "name": request.businessTool or "read_only_business_lookup", "status": "completed"})
                title, category = BUSINESS_SOURCES.get(request.businessTool, ("业务只读查询", "business"))
                yield sse({"type": "sources", "items": [{"title": title, "category": category}]})
            else:
                context = await retrieve(request.message)
                if not context and should_suggest_handoff(request.message):
                    yield sse({"type": "handoff_suggested"})
            if context:
                yield sse({"type": "sources", "items": [{"title": item["title"], "category": item["category"]} for item in context]})
            async for chunk in stream_reply(request.message, history, context, request.businessContext):
                yield sse({"type": "text", "content": chunk})
            yield sse({"type": "done"})
        except Exception:
            yield sse({"type": "error", "message": "客服服务暂不可用，请稍后重试"})

    return StreamingResponse(events(), media_type="text/event-stream", headers={"Cache-Control": "no-cache"})

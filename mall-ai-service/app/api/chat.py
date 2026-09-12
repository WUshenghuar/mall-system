import json
from collections.abc import AsyncIterator
from typing import Literal

from fastapi import APIRouter, Header, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field

from app.agent.agent import should_suggest_handoff
from app.config import settings
from app.rag.retriever import retrieve
from app.utils.llm import plan_tool as plan_tool_request, stream_reply

router = APIRouter()

BUSINESS_SOURCES = {
    "query_order": ("订单只读查询", "business"),
    "query_logistics": ("物流只读查询", "logistics"),
    "query_refund": ("售后只读查询", "after_sales"),
    "query_product": ("商品目录查询", "product"),
    "query_coupon": ("优惠券查询", "marketing"),
    "query_member": ("会员资料查询", "member"),
    "query_tax": ("税费与币种查询", "finance"),
    "query_activity": ("进行中活动查询", "marketing"),
    "query_return_eligibility": ("退款/退货资格查询", "after_sales"),
}


def business_tool_names(value: str) -> list[str]:
    return [name for name in (item.strip() for item in value.split(",")) if name in BUSINESS_SOURCES][:3]


class ChatMessage(BaseModel):
    role: Literal["user", "assistant"]
    content: str


class ChatRequest(BaseModel):
    memberId: int = Field(gt=0)
    conversationId: str = Field(min_length=1, max_length=64)
    message: str = Field(min_length=1, max_length=1000)
    businessContext: str = Field(default="", max_length=6000)
    businessTool: str = Field(default="", max_length=128)
    history: list[ChatMessage] = Field(default_factory=list)
    toolResults: list[str] = Field(default_factory=list, max_length=3)


def sse(payload: dict) -> str:
    return f"event: message\ndata: {json.dumps(payload, ensure_ascii=False)}\n\n"


@router.post("/internal/tool-plan")
async def tool_plan(request: ChatRequest, x_ai_service_token: str = Header(default="")):
    if x_ai_service_token != settings.service_token:
        raise HTTPException(status_code=401, detail="invalid AI service token")
    history = [item.model_dump() for item in request.history]
    return await plan_tool_request(request.message, history, request.toolResults)


@router.post("/internal/chat")
async def chat(request: ChatRequest, x_ai_service_token: str = Header(default="")):
    if x_ai_service_token != settings.service_token:
        raise HTTPException(status_code=401, detail="invalid AI service token")

    async def events() -> AsyncIterator[str]:
        yield sse({"type": "thinking"})
        if not settings.enabled:
            yield sse({"type": "handoff_suggested"})
            yield sse({"type": "text", "content": "AI 客服当前暂时停用，请点击转人工客服获取帮助。"})
            yield sse({"type": "done"})
            return
        try:
            history = [item.model_dump() for item in request.history]
            context = []
            if request.businessContext:
                tool_names = business_tool_names(request.businessTool)
                for name in tool_names or ["read_only_business_lookup"]:
                    yield sse({"type": "tool_call", "name": name, "status": "completed"})
                source_items = [
                    {"title": BUSINESS_SOURCES.get(name, ("业务只读查询", "business"))[0],
                     "category": BUSINESS_SOURCES.get(name, ("业务只读查询", "business"))[1]}
                    for name in tool_names or [""]
                ]
                yield sse({"type": "sources", "items": source_items})
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

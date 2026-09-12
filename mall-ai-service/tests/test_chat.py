import asyncio
from types import SimpleNamespace

import app.api.chat as chat_module
from app.api.chat import ChatRequest, business_tool_names, chat

def test_business_tool_names_filters_unknown_tools_and_caps_batch():
    assert business_tool_names("query_member,query_product,drop_database,query_tax,query_order") == [
        "query_member", "query_product", "query_tax"
    ]


def test_disabled_chat_streams_handoff_without_dependencies(monkeypatch):
    monkeypatch.setattr(chat_module, "settings", SimpleNamespace(enabled=False, service_token="token"))
    request = ChatRequest(memberId=9, conversationId="disabled-test", message="查询订单")
    response = asyncio.run(chat(request, "token"))

    async def collect():
        return [item async for item in response.body_iterator]

    body = "".join(item.decode() if isinstance(item, bytes) else item for item in asyncio.run(collect()))
    assert "handoff_suggested" in body
    assert "AI 客服当前暂时停用" in body

import asyncio
from types import SimpleNamespace

from app.utils import llm


# Regression: GLM-4.5-Air planner exceeded the 2s planner budget in default thinking mode.
# Found by /qa on 2026-09-14.
def test_glm45air_planner_disables_thinking(monkeypatch):
    captured = {}

    class Response:
        def raise_for_status(self):
            return None

        def json(self):
            return {"choices": [{"message": {"tool_calls": [
                {"id": "call-logistics", "function": {"name": "query_logistics", "arguments": "{}"}},
                {"id": "call-member", "function": {"name": "query_member", "arguments": "{}"}},
            ]}}]}

    class Client:
        async def __aenter__(self):
            return self

        async def __aexit__(self, *args):
            return None

        async def post(self, url, headers, json):
            captured["payload"] = json
            return Response()

    monkeypatch.setattr(llm, "settings", SimpleNamespace(
        has_model=True,
        model_api_base="https://open.bigmodel.cn/api/paas/v4",
        model_api_key="secret",
        model_name="glm-4.5-air",
    ))
    monkeypatch.setattr(llm.httpx, "AsyncClient", lambda **kwargs: Client())

    result = asyncio.run(llm.plan_tool("查询我的会员积分和包裹物流", []))

    assert captured["payload"]["thinking"] == {"type": "disabled"}
    assert result["tools"] == [
        {"tool": "query_logistics", "arguments": {}, "callId": "call-logistics"},
        {"tool": "query_member", "arguments": {}, "callId": "call-member"},
    ]


def test_glm45air_reply_disables_thinking(monkeypatch):
    captured = {}

    class Stream:
        def raise_for_status(self):
            return None

        async def __aenter__(self):
            return self

        async def __aexit__(self, *args):
            return None

        async def aiter_lines(self):
            yield 'data: {"choices":[{"delta":{"content":"ok"}}]}'
            yield "data: [DONE]"

    class Client:
        async def __aenter__(self):
            return self

        async def __aexit__(self, *args):
            return None

        def stream(self, method, url, headers, json):
            captured["payload"] = json
            return Stream()

    monkeypatch.setattr(llm, "_model_retry_at", 0.0)
    monkeypatch.setattr(llm, "settings", SimpleNamespace(
        enabled=True,
        has_model=True,
        model_api_base="https://open.bigmodel.cn/api/paas/v4",
        model_api_key="secret",
        model_name="glm-4.5-air",
    ))
    monkeypatch.setattr(llm.httpx, "AsyncClient", lambda **kwargs: Client())

    async def collect():
        return "".join([chunk async for chunk in llm.stream_reply("商品", [], [{"content": "真实商品资料"}])])

    assert asyncio.run(collect()) == "ok"
    assert captured["payload"]["thinking"] == {"type": "disabled"}

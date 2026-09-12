import asyncio
from types import SimpleNamespace

from app.utils import llm


def test_model_tool_plan_returns_only_allowed_function_call(monkeypatch):
    class Response:
        def raise_for_status(self):
            return None

        def json(self):
            return {"choices": [{"message": {"tool_calls": [{
                "function": {
                    "name": "query_logistics",
                    "arguments": '{"order_no":"T202609071234567890"}',
                }
            }]}}]}

    class Client:
        async def __aenter__(self):
            return self

        async def __aexit__(self, *args):
            return None

        async def post(self, url, headers, json):
            assert url == "https://model.test/v1/chat/completions"
            assert json["tool_choice"] == "auto"
            assert any(item["function"]["name"] == "query_logistics" for item in json["tools"])
            assert any(item["function"]["name"] == "query_activity" for item in json["tools"])
            return Response()

    monkeypatch.setattr(llm, "settings", SimpleNamespace(
        has_model=True,
        model_api_base="https://model.test/v1",
        model_api_key="secret",
        model_name="test-chat",
    ))
    monkeypatch.setattr(llm.httpx, "AsyncClient", lambda **kwargs: Client())

    result = asyncio.run(llm.plan_tool("我的包裹到哪了", []))

    assert result == {"tool": "query_logistics", "arguments": {"order_no": "T202609071234567890"}}


def test_model_tool_plan_keeps_at_most_three_allowed_calls(monkeypatch):
    class Response:
        def raise_for_status(self):
            return None

        def json(self):
            return {"choices": [{"message": {"tool_calls": [
                {"function": {"name": "query_member", "arguments": "{}"}},
                {"function": {"name": "query_logistics", "arguments": '{"order_no":"T202609071234567890"}'}},
                {"function": {"name": "query_product", "arguments": "{}"}},
                {"function": {"name": "query_tax", "arguments": "{}"}},
            ]}}]}

    class Client:
        async def __aenter__(self):
            return self

        async def __aexit__(self, *args):
            return None

        async def post(self, url, headers, json):
            return Response()

    monkeypatch.setattr(llm, "settings", SimpleNamespace(
        has_model=True, model_api_base="https://model.test/v1", model_api_key="secret", model_name="test-chat"))
    monkeypatch.setattr(llm.httpx, "AsyncClient", lambda **kwargs: Client())

    result = asyncio.run(llm.plan_tool("我的会员和物流", []))

    assert result == {"tools": [
        {"tool": "query_member", "arguments": {}},
        {"tool": "query_logistics", "arguments": {"order_no": "T202609071234567890"}},
        {"tool": "query_product", "arguments": {}},
    ]}

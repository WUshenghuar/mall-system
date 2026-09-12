import json
from collections.abc import AsyncIterator

import httpx

from app.agent.agent import is_english_message, is_prompt_injection, local_agent, should_suggest_handoff
from app.agent.tools import TOOL_DEFINITIONS, TOOL_NAMES
from app.config import settings


async def plan_tool(message: str, history: list[dict[str, str]]) -> dict:
    if is_prompt_injection(message) or not getattr(settings, "enabled", True) or not settings.has_model:
        return {"tool": "", "arguments": {}}
    messages = [{"role": "system", "content": "你是平台客服意图路由器。只允许选择只读查询工具，不执行任何写操作；无法确定时不要选择工具。"}]
    messages.extend(history[-10:])
    messages.append({"role": "user", "content": message})
    payload = {
        "model": settings.model_name,
        "messages": messages,
        "tools": TOOL_DEFINITIONS,
        "tool_choice": "auto",
        "temperature": 0,
    }
    try:
        async with httpx.AsyncClient(timeout=10) as client:
            response = await client.post(
                f"{settings.model_api_base}/chat/completions",
                headers={"Authorization": f"Bearer {settings.model_api_key}"},
                json=payload,
            )
            response.raise_for_status()
        calls = response.json().get("choices", [{}])[0].get("message", {}).get("tool_calls", [])
        if not isinstance(calls, list):
            return {"tool": "", "arguments": {}}
        plans = []
        for call in calls[:3]:
            if not isinstance(call, dict):
                continue
            function = call.get("function", {})
            if not isinstance(function, dict) or function.get("name") not in TOOL_NAMES:
                continue
            try:
                arguments = json.loads(function.get("arguments") or "{}")
            except (TypeError, json.JSONDecodeError):
                continue
            if isinstance(arguments, dict):
                plans.append({"tool": function["name"], "arguments": arguments})
        if len(plans) == 1:
            return plans[0]
        return {"tools": plans} if plans else {"tool": "", "arguments": {}}
    except Exception:
        return {"tool": "", "arguments": {}}


async def stream_reply(message: str, history: list[dict[str, str]], context: list[dict], business_context: str = "") -> AsyncIterator[str]:
    if not getattr(settings, "enabled", True):
        yield "AI 客服当前暂时停用，请转人工客服获取帮助。"
        return
    if is_prompt_injection(message):
        answer = local_agent.invoke({"message": message})["answer"]
        for index in range(0, len(answer), 12):
            yield answer[index:index + 12]
        return
    if business_context:
        for index in range(0, len(business_context), 12):
            yield business_context[index:index + 12]
        return
    if not context:
        answer = local_agent.invoke({"message": message})["answer"]
        if should_suggest_handoff(message):
            answer = ("I couldn't find verified platform information for that question. Please provide an order number or contact a human agent."
                      if is_english_message(message) else "我暂时没有查到可确认的相关平台资料，无法直接判断。你可以补充订单号或转人工客服。")
        for index in range(0, len(answer), 12):
            yield answer[index:index + 12]
        return
    if not settings.has_model:
        domains = ("支付", "会员", "订单", "物流", "退款", "优惠券", "商品", "税费", "币种")
        answers = context[:2] if sum(domain in message for domain in domains) > 1 else context[:1]
        answer = "\n".join(item["content"] for item in answers) if answers else local_agent.invoke({"message": message})["answer"]
        for index in range(0, len(answer), 12):
            yield answer[index:index + 12]
        return

    sources = "\n".join(item["content"] for item in context)
    messages = [{"role": "system", "content": f"你是海路集市的平台客服。使用用户提问的语言回答；只回答平台业务问题，不执行退款、取消订单或修改账户等操作。只能依据以下平台资料回答；资料未覆盖的问题必须明确说无法确认并建议转人工，不得凭常识补充：\n{sources}"}]
    messages.extend(history[-10:])
    if not messages or messages[-1].get("content") != message:
        messages.append({"role": "user", "content": message})
    headers = {"Authorization": f"Bearer {settings.model_api_key}"}
    payload = {"model": settings.model_name, "messages": messages, "stream": True, "temperature": 0.3}
    async with httpx.AsyncClient(timeout=45) as client:
        async with client.stream("POST", f"{settings.model_api_base}/chat/completions", headers=headers, json=payload) as response:
            response.raise_for_status()
            async for line in response.aiter_lines():
                if not line.startswith("data: ") or line == "data: [DONE]":
                    continue
                delta = json.loads(line[6:]).get("choices", [{}])[0].get("delta", {}).get("content")
                if delta:
                    yield delta

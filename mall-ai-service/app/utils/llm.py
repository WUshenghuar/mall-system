import json
from collections.abc import AsyncIterator

import httpx

from app.agent.agent import is_english_message, is_prompt_injection, local_agent, should_suggest_handoff
from app.agent.tools import TOOL_DEFINITIONS, TOOL_NAMES
from app.config import settings
from app.telemetry import span


def safe_history(history: list[dict[str, str]]) -> list[dict[str, str]]:
    result = []
    for item in history[-10:]:
        if not isinstance(item, dict):
            continue
        role, content = item.get("role"), item.get("content")
        if role not in {"user", "assistant"} or not isinstance(content, str) or not content.strip():
            continue
        if is_prompt_injection(content):
            continue
        result.append({"role": role, "content": content.strip()[:1000]})
    return result


async def plan_tool(message: str, history: list[dict[str, str]], tool_results: list[str] | None = None) -> dict:
    with span("ai.tool_plan"):
        return await _plan_tool(message, history, tool_results)


async def _plan_tool(message: str, history: list[dict[str, str]], tool_results: list[str] | None = None) -> dict:
    if is_prompt_injection(message) or not getattr(settings, "enabled", True) or not settings.has_model:
        return {"tool": "", "arguments": {}}
    messages = [{"role": "system", "content": "你是平台客服意图路由器。只允许选择只读查询工具，不执行任何写操作；无法确定时不要选择工具。"}]
    messages.extend(safe_history(history))
    results = [item.strip()[:2000] for item in (tool_results or []) if isinstance(item, str) and item.strip()][:3]
    if results:
        messages.append({"role": "system", "content": "以下只读工具已执行，不要重复这些查询；仅在仍缺少必要信息时选择其它只读工具：\n" + "\n".join(results)})
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
    if not context and not business_context:
        answer = local_agent.invoke({"message": message})["answer"]
        if should_suggest_handoff(message):
            answer = ("I couldn't find verified platform information for that question. Please provide an order number or contact a human agent."
                      if is_english_message(message) else "我暂时没有查到可确认的相关平台资料，无法直接判断。你可以补充订单号或转人工客服。")
        for index in range(0, len(answer), 12):
            yield answer[index:index + 12]
        return
    if not settings.has_model:
        if business_context:
            for index in range(0, len(business_context), 12):
                yield business_context[index:index + 12]
            return
        domains = ("支付", "会员", "订单", "物流", "退款", "优惠券", "商品", "税费", "币种")
        answers = context[:2] if sum(domain in message for domain in domains) > 1 else context[:1]
        answer = "\n".join(item["content"] for item in answers) if answers else local_agent.invoke({"message": message})["answer"]
        for index in range(0, len(answer), 12):
            yield answer[index:index + 12]
        return

    sources = "\n".join(item["content"] for item in context)
    if business_context:
        sources += ("\n真实业务查询结果（优先依据，不得改写其中的订单、金额、状态或时间）：\n" + business_context)
    messages = [{"role": "system", "content": f"你是海路集市的平台客服。使用用户提问的语言回答；只回答平台业务问题，不执行退款、取消订单或修改账户等操作。只能依据以下平台资料和真实业务查询结果回答；资料未覆盖的问题必须明确说无法确认并建议转人工，不得凭常识补充：\n{sources}"}]
    messages.extend(safe_history(history))
    if not messages or messages[-1].get("content") != message:
        messages.append({"role": "user", "content": message})
    headers = {"Authorization": f"Bearer {settings.model_api_key}"}
    payload = {"model": settings.model_name, "messages": messages, "stream": True, "temperature": 0.3}
    emitted = False
    try:
        async with httpx.AsyncClient(timeout=45) as client:
            async with client.stream("POST", f"{settings.model_api_base}/chat/completions", headers=headers, json=payload) as response:
                response.raise_for_status()
                async for line in response.aiter_lines():
                    if not line.startswith("data: ") or line == "data: [DONE]":
                        continue
                    delta = json.loads(line[6:]).get("choices", [{}])[0].get("delta", {}).get("content")
                    if delta:
                        emitted = True
                        yield delta
    except Exception:
        if emitted:
            raise
    if not emitted:
        answer = business_context or "\n".join(item["content"] for item in context[:2]) or local_agent.invoke({"message": message})["answer"]
        for index in range(0, len(answer), 12):
            yield answer[index:index + 12]

import json
import re
from collections.abc import AsyncIterator
from time import monotonic

import httpx

from app.agent.agent import is_english_message, is_prompt_injection, local_agent, should_suggest_handoff
from app.agent.tools import TOOL_DEFINITIONS, TOOL_NAMES
from app.config import settings
from app.telemetry import record_tool_plan, set_span_attributes, span


_model_retry_at = 0.0
MAX_REPLY_CHARS = 4000
CALL_ID_PATTERN = re.compile(r"^[A-Za-z0-9_-]{1,64}$")


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


def unique_call_id(value: object, fallback: str, used: set[str]) -> str:
    candidate = value.strip() if isinstance(value, str) and CALL_ID_PATTERN.fullmatch(value.strip()) else fallback
    if candidate in used:
        candidate = fallback
        suffix = 1
        while candidate in used:
            candidate = f"{fallback}-{suffix}"
            suffix += 1
    used.add(candidate)
    return candidate


async def plan_tool(message: str, history: list[dict[str, str]], tool_results: list | None = None) -> dict:
    started = monotonic()
    outcome = "error"
    try:
        with span("ai.tool_plan", {"langfuse.observation.type": "tool", "gen_ai.system": "openai",
                                   "gen_ai.request.model": settings.model_name, "langfuse.observation.model.name": settings.model_name}):
            result = await _plan_tool(message, history, tool_results)
        planner_error = result.pop("_planner_error", False)
        outcome = "error" if planner_error else "planned" if result.get("tool") or result.get("tools") else "empty"
        return result
    finally:
        record_tool_plan(round((monotonic() - started) * 1000, 2), outcome)


async def _plan_tool(message: str, history: list[dict[str, str]], tool_results: list | None = None) -> dict:
    if is_prompt_injection(message) or not getattr(settings, "enabled", True) or not settings.has_model:
        return {"tool": "", "arguments": {}}
    messages = [{"role": "system", "content": "你是平台客服意图路由器。只允许选择只读查询工具，不执行任何写操作；无法确定时不要选择工具。"}]
    history_messages = safe_history(history)
    if history_messages and history_messages[-1]["role"] == "user" and history_messages[-1]["content"] == message:
        history_messages = history_messages[:-1]
    messages.extend(history_messages)
    structured_results = []
    legacy_results = []
    used_call_ids = set()
    for item in (tool_results or [])[:3]:
        raw = item.model_dump() if hasattr(item, "model_dump") else item
        if isinstance(raw, dict):
            call_id, tool, arguments, content = raw.get("callId"), raw.get("tool"), raw.get("arguments"), raw.get("content")
            if (isinstance(call_id, str) and 1 <= len(call_id) <= 64 and isinstance(tool, str) and tool in TOOL_NAMES
                    and isinstance(arguments, dict) and isinstance(content, str) and content.strip()):
                structured_results.append((unique_call_id(call_id, f"call-{len(structured_results) + 1}", used_call_ids),
                                           tool, arguments, content.strip()[:2000]))
        elif isinstance(raw, str) and raw.strip():
            legacy_results.append(raw.strip()[:2000])
    if structured_results:
        messages.append({"role": "user", "content": message})
        messages.append({"role": "assistant", "tool_calls": [
            {"id": call_id, "type": "function", "function": {
                "name": tool, "arguments": json.dumps(arguments, ensure_ascii=False, separators=(",", ":"))
            }} for call_id, tool, arguments, _ in structured_results
        ]})
        messages.extend({"role": "tool", "tool_call_id": call_id, "name": tool, "content": content}
                        for call_id, tool, _, content in structured_results)
    else:
        if legacy_results:
            messages.append({"role": "system", "content": "以下只读工具已执行，不要重复这些查询；仅在仍缺少必要信息时选择其它只读工具：\n" + "\n".join(legacy_results)})
        messages.append({"role": "user", "content": message})
    payload = {
        "model": settings.model_name,
        "messages": messages,
        "tools": TOOL_DEFINITIONS,
        "tool_choice": "auto",
        "temperature": 0,
    }
    try:
        async with httpx.AsyncClient(timeout=2) as client:
            response = await client.post(
                f"{settings.model_api_base}/chat/completions",
                headers={"Authorization": f"Bearer {settings.model_api_key}"},
                json=payload,
            )
            response.raise_for_status()
        body = response.json()
        usage = usage_attributes(body.get("usage", {}))
        if usage:
            set_span_attributes(usage)
        calls = body.get("choices", [{}])[0].get("message", {}).get("tool_calls", [])
        if not isinstance(calls, list):
            return {"tool": "", "arguments": {}}
        plans = []
        used_plan_ids = set()
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
                plan = {"tool": function["name"], "arguments": arguments}
                call_id = call.get("id")
                if isinstance(call_id, str) and 1 <= len(call_id.strip()) <= 64:
                    plan["callId"] = unique_call_id(call_id, f"call-{len(plans) + 1}", used_plan_ids)
                plans.append(plan)
        if len(plans) == 1:
            return plans[0]
        return {"tools": plans} if plans else {"tool": "", "arguments": {}}
    except Exception:
        return {"tool": "", "arguments": {}, "_planner_error": True}


async def stream_reply(message: str, history: list[dict[str, str]], context: list[dict], business_context: str = "") -> AsyncIterator[str]:
    if not getattr(settings, "enabled", True):
        yield "AI 客服当前暂时停用，请转人工客服获取帮助。"
        return
    if is_prompt_injection(message):
        answer = local_agent.invoke({"message": message})["answer"]
        for chunk in text_chunks(answer):
            yield chunk
        return
    if not context and not business_context:
        answer = local_agent.invoke({"message": message})["answer"]
        if should_suggest_handoff(message):
            answer = ("I couldn't find verified platform information for that question. Please provide an order number or contact a human agent."
                      if is_english_message(message) else "我暂时没有查到可确认的相关平台资料，无法直接判断。你可以补充订单号或转人工客服。")
        for chunk in text_chunks(answer):
            yield chunk
        return
    if not settings.has_model:
        if business_context:
            for chunk in text_chunks(business_context):
                yield chunk
            return
        domains = ("支付", "会员", "订单", "物流", "退款", "优惠券", "商品", "税费", "币种")
        answers = context[:2] if sum(domain in message for domain in domains) > 1 else context[:1]
        answer = "\n".join(item["content"] for item in answers) if answers else local_agent.invoke({"message": message})["answer"]
        for chunk in text_chunks(answer):
            yield chunk
        return
    if monotonic() < _model_retry_at:
        answer = business_context or "\n".join(item["content"] for item in context[:2]) or local_agent.invoke({"message": message})["answer"]
        for chunk in text_chunks(answer):
            yield chunk
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
    if getattr(settings, "model_include_usage", False):
        payload["stream_options"] = {"include_usage": True}
    emitted = False
    emitted_chars = 0
    try:
        with span("ai.model.stream", {"ai.model": settings.model_name, "gen_ai.system": "openai",
                                       "gen_ai.request.model": settings.model_name,
                                       "langfuse.observation.model.name": settings.model_name,
                                       "langfuse.observation.type": "generation"}):
            async with httpx.AsyncClient(timeout=45) as client:
                async with client.stream("POST", f"{settings.model_api_base}/chat/completions", headers=headers, json=payload) as response:
                    response.raise_for_status()
                    async for line in response.aiter_lines():
                        if not line.startswith("data: ") or line == "data: [DONE]":
                            continue
                        chunk = json.loads(line[6:])
                        usage = usage_attributes(chunk.get("usage", {}))
                        if usage:
                            set_span_attributes(usage)
                        choices = chunk.get("choices", [])
                        delta = choices[0].get("delta", {}).get("content") if isinstance(choices, list) and choices else None
                        if delta:
                            remaining = MAX_REPLY_CHARS - emitted_chars
                            if remaining <= 0:
                                break
                            delta = delta[:remaining]
                            emitted = True
                            emitted_chars += len(delta)
                            yield delta
                            if emitted_chars >= MAX_REPLY_CHARS:
                                break
    except Exception:
        _mark_model_failure()
        if emitted:
            raise
    if not emitted:
        _mark_model_failure()
        answer = business_context or "\n".join(item["content"] for item in context[:2]) or local_agent.invoke({"message": message})["answer"]
        for chunk in text_chunks(answer):
            yield chunk
    else:
        _mark_model_success()


def usage_attributes(usage: dict) -> dict:
    if not isinstance(usage, dict):
        return {}
    aliases = {"input_tokens": "input", "prompt_tokens": "input", "output_tokens": "output",
               "completion_tokens": "output", "total_tokens": "total"}
    details = {}
    attributes = {}
    for source, target in aliases.items():
        value = usage.get(source)
        if isinstance(value, int) and not isinstance(value, bool) and value >= 0:
            details[target] = value
            attributes[f"gen_ai.usage.{target}_tokens"] = value
    if details:
        attributes["langfuse.observation.usage_details"] = json.dumps(details, separators=(",", ":"))
    return attributes


def text_chunks(answer: str):
    answer = answer[:MAX_REPLY_CHARS]
    for index in range(0, len(answer), 12):
        yield answer[index:index + 12]


def _mark_model_failure() -> None:
    global _model_retry_at
    # ponytail: process-local cooldown avoids a dependency; use a shared breaker when running multiple workers.
    _model_retry_at = monotonic() + 30


def _mark_model_success() -> None:
    global _model_retry_at
    _model_retry_at = 0.0

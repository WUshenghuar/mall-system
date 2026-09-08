import json
from collections.abc import AsyncIterator

import httpx

from app.agent.agent import local_agent
from app.config import settings


async def stream_reply(message: str, history: list[dict[str, str]], context: list[dict], business_context: str = "") -> AsyncIterator[str]:
    if business_context:
        for index in range(0, len(business_context), 12):
            yield business_context[index:index + 12]
        return
    if not settings.has_model:
        answer = context[0]["content"] if context else local_agent.invoke({"message": message})["answer"]
        for index in range(0, len(answer), 12):
            yield answer[index:index + 12]
        return

    sources = "\n".join(item["content"] for item in context)
    messages = [{"role": "system", "content": f"你是海路集市的平台客服。只回答平台业务问题，不执行退款、取消订单或修改账户等操作。优先依据以下平台资料回答：\n{sources}"}]
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

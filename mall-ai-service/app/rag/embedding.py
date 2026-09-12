from typing import Any

import httpx

from app.config import settings


async def embed_text(text: str) -> list[float] | None:
    if not settings.has_embedding:
        return None
    async with httpx.AsyncClient(timeout=5) as client:
        return await _request(client, text)


async def _request(client: Any, text: str) -> list[float]:
    response = await client.post(
        f"{settings.embedding_api_base}/embeddings",
        headers={"Authorization": f"Bearer {settings.embedding_api_key}"},
        json={"model": settings.embedding_model, "input": text},
    )
    response.raise_for_status()
    vector = response.json()["data"][0]["embedding"]
    if not isinstance(vector, list) or len(vector) != settings.embedding_dimensions:
        raise ValueError("embedding dimensions do not match configuration")
    return vector

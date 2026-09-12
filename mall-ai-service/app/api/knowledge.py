from fastapi import APIRouter, Header, HTTPException, Path
from pydantic import BaseModel, Field
import httpx

from app.config import settings
from app.rag.retriever import DISABLED_IDS, INDEX, ensure_seeded

router = APIRouter()


class KnowledgePayload(BaseModel):
    title: str = Field(min_length=1, max_length=120)
    category: str = Field(min_length=1, max_length=64)
    content: str = Field(min_length=1, max_length=5000)
    enabled: bool = True


def verify_token(token: str) -> None:
    if token != settings.service_token:
        raise HTTPException(status_code=401, detail="invalid AI service token")


async def search_documents() -> list[dict]:
    payload = {"size": 100, "query": {"match_all": {}}}
    async with httpx.AsyncClient(timeout=10) as client:
        response = await client.post(f"{settings.elasticsearch_url}/{INDEX}/_search", json=payload)
        if response.status_code == 404:
            await ensure_seeded()
            response = await client.post(f"{settings.elasticsearch_url}/{INDEX}/_search", json=payload)
        response.raise_for_status()
    return [hit.get("_source", {}) for hit in response.json().get("hits", {}).get("hits", [])]


@router.get("/internal/knowledge")
async def list_knowledge(x_ai_service_token: str = Header(default="")):
    verify_token(x_ai_service_token)
    try:
        return {"items": await search_documents()}
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=503, detail="knowledge service unavailable") from exc


# ponytail: single-index upsert plus enabled flag; add version/publish when governance requires it.
@router.put("/internal/knowledge/{doc_id}")
async def save_knowledge(
    payload: KnowledgePayload,
    doc_id: str = Path(min_length=1, max_length=64, pattern=r"^[A-Za-z0-9_-]+$"),
    x_ai_service_token: str = Header(default=""),
):
    verify_token(x_ai_service_token)
    document = {"id": doc_id, **payload.model_dump()}
    if document["enabled"]:
        DISABLED_IDS.discard(doc_id)
    else:
        DISABLED_IDS.add(doc_id)
    try:
        async with httpx.AsyncClient(timeout=10) as client:
            response = await client.put(f"{settings.elasticsearch_url}/{INDEX}/_doc/{doc_id}?refresh=true", json=document)
            response.raise_for_status()
        return document
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=503, detail="knowledge service unavailable") from exc

import asyncio
from types import SimpleNamespace

from fastapi import HTTPException
import pytest

from app.api import knowledge
from app.rag import retriever


def test_knowledge_write_does_not_overwrite_when_embedding_fails(monkeypatch):
    async def fail_embedding(text):
        raise RuntimeError("embedding unavailable")

    monkeypatch.setattr(knowledge, "settings", SimpleNamespace(has_embedding=True, service_token="token"))
    monkeypatch.setattr(knowledge, "embed_text", fail_embedding)

    class UnexpectedClient:
        async def __aenter__(self):
            raise AssertionError("ES should not be written when embedding fails")

        async def __aexit__(self, *args):
            return None

    monkeypatch.setattr(knowledge.httpx, "AsyncClient", lambda **kwargs: UnexpectedClient())

    async def run():
        try:
            await knowledge.save_knowledge(
                knowledge.KnowledgePayload(title="退款规则", category="after_sales", content="内容"),
                "faq",
                "token",
            )
        except HTTPException as exc:
            assert exc.status_code == 503
        else:
            raise AssertionError("embedding failure must be visible to the caller")

    asyncio.run(run())


def test_embedding_mapping_conflict_is_reported(monkeypatch):
    monkeypatch.setattr(retriever, "settings", SimpleNamespace(
        has_embedding=True,
        embedding_dimensions=2,
        elasticsearch_url="http://es.test",
    ))

    class Response:
        def __init__(self, status_code):
            self.status_code = status_code

        def raise_for_status(self):
            if self.status_code >= 400:
                raise RuntimeError("mapping conflict")

        def json(self):
            return {"hits": {"hits": []}}

    class Client:
        async def __aenter__(self):
            return self

        async def __aexit__(self, *args):
            return None

        async def head(self, url):
            return Response(200)

        async def put(self, url, **kwargs):
            return Response(400 if url.endswith("/_mapping") else 200)

    monkeypatch.setattr(retriever.httpx, "AsyncClient", lambda **kwargs: Client())

    with pytest.raises(RuntimeError, match="mapping conflict"):
        asyncio.run(retriever.ensure_seeded())


def test_embedding_backfill_pages_and_refreshes_once(monkeypatch):
    monkeypatch.setattr(retriever, "settings", SimpleNamespace(elasticsearch_url="http://es.test"))
    search_pages = [
        [{"_id": "a", "sort": ["a"], "_source": {"title": "A", "content": "a"}},
         {"_id": "b", "sort": ["b"], "_source": {"title": "B", "content": "b"}}],
        [],
    ]
    calls = []

    class Response:
        def raise_for_status(self):
            return None

        def json(self):
            return {"hits": {"hits": search_pages.pop(0)}}

    class Client:
        async def __aenter__(self):
            return self

        async def __aexit__(self, *args):
            return None

        async def post(self, url, **kwargs):
            calls.append((url, kwargs.get("json")))
            if url.endswith("/_search"):
                return Response()
            return SimpleNamespace(raise_for_status=lambda: None, json=lambda: {})

    async def fake_embed(text):
        return [0.1, 0.2]

    monkeypatch.setattr(retriever.httpx, "AsyncClient", lambda **kwargs: Client())
    monkeypatch.setattr(retriever, "embed_text", fake_embed)

    asyncio.run(retriever._backfill_embeddings())

    searches = [item for item in calls if item[0].endswith("/_search")]
    refreshes = [item for item in calls if item[0].endswith("/_refresh")]
    assert len(searches) == 2
    assert searches[1][1]["search_after"] == ["b"]
    assert len(refreshes) == 1

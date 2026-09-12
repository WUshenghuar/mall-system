import asyncio
from types import SimpleNamespace

from app.rag import embedding


def test_openai_compatible_embedding_response_is_read(monkeypatch):
    class Response:
        def raise_for_status(self):
            return None

        def json(self):
            return {"data": [{"embedding": [0.1, 0.2]}]}

    class Client:
        async def __aenter__(self):
            return self

        async def __aexit__(self, *args):
            return None

        async def post(self, url, headers, json):
            assert url == "https://embedding.test/v1/embeddings"
            assert json == {"model": "test-embedding", "input": "退款规则"}
            return Response()

    monkeypatch.setattr(embedding, "settings", SimpleNamespace(
        has_embedding=True,
        embedding_api_base="https://embedding.test/v1",
        embedding_api_key="secret",
        embedding_model="test-embedding",
        embedding_dimensions=2,
    ))
    monkeypatch.setattr(embedding.httpx, "AsyncClient", lambda **kwargs: Client())

    assert asyncio.run(embedding.embed_text("退款规则")) == [0.1, 0.2]

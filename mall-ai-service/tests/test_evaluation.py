from evaluate import CASES, evaluate
from app.api.chat import ChatRequest
from app.rag import embedding
from app.rag.retriever import DISABLED_IDS, SEED_DOCS, fallback_hits, hybrid_hits, retrieve
from app.utils.llm import stream_reply
from pydantic import ValidationError
import asyncio


def test_deterministic_policy_evaluation_is_green():
    assert len(CASES) >= 50
    result = evaluate()
    assert result == {"passed": len(CASES), "total": len(CASES), "score": 100}


def test_knowledge_seed_covers_core_platform_faqs():
    assert {"refund", "coupon", "logistics", "order", "payment", "member"} <= {item["id"] for item in SEED_DOCS}
    assert all(item["title"] and item["content"] for item in SEED_DOCS)


def test_rag_fallback_matches_multitopic_chinese_query():
    assert {item["id"] for item in fallback_hits("支付和会员服务怎么用？")} >= {"payment", "member"}


def test_rag_fallback_matches_english_faqs():
    assert fallback_hits("How do I request a refund?")[0]["id"] == "refund-en"
    assert fallback_hits("Where is my package?")[0]["id"] == "logistics-en"
    assert fallback_hits("How do coupons work?")[0]["id"] == "coupon-en"
    assert [item["id"] for item in fallback_hits("Where is my package?")] == ["logistics-en"]


def test_local_fallback_honors_disabled_knowledge():
    DISABLED_IDS.add("coupon")
    try:
        assert "coupon" not in {item["id"] for item in fallback_hits("优惠券规则")}
    finally:
        DISABLED_IDS.discard("coupon")


def test_unmatched_question_has_no_knowledge_fallback():
    assert fallback_hits("完全不相关的旅行天气问题xyz") == []


def test_local_rag_combines_two_relevant_topics():
    async def collect():
        context = [{"content": "支付说明"}, {"content": "会员服务"}]
        return "".join([chunk async for chunk in stream_reply("支付和会员服务", [], context)])

    assert asyncio.run(collect()) == "支付说明\n会员服务"


def test_hybrid_rag_prioritizes_documents_recalled_by_both_paths():
    documents = {item["id"]: item for item in SEED_DOCS}
    hits = hybrid_hits(
        [{"_id": "coupon", "_source": documents["coupon"]}, {"_id": "refund", "_source": documents["refund"]}],
        [{"_id": "refund", "_source": documents["refund"]}, {"_id": "tax", "_source": documents["tax"]}],
    )

    assert [hit["_id"] for hit in hits] == ["refund", "coupon", "tax"]


def test_retrieve_fuses_bm25_and_vector_results(monkeypatch):
    documents = {item["id"]: item for item in SEED_DOCS}

    class Response:
        status_code = 200

        def __init__(self, hits):
            self.hits = hits

        def raise_for_status(self):
            return None

        def json(self):
            return {"hits": {"hits": self.hits}}

    class Client:
        async def __aenter__(self):
            return self

        async def __aexit__(self, *args):
            return None

        async def post(self, url, **kwargs):
            if "knn" not in kwargs["json"]:
                assert kwargs["json"]["query"]["bool"]["must"][0]["multi_match"]["minimum_should_match"] == "30%"
                assert kwargs["json"]["min_score"] == 3.0
            return Response(
                [{"_id": "coupon", "_score": 10.0, "_source": documents["coupon"]}, {"_id": "refund", "_score": 5.0, "_source": documents["refund"]}]
                if "knn" not in kwargs["json"]
                else [{"_id": "refund", "_score": 0.9, "_source": documents["refund"]}]
            )

    async def fake_embed(query):
        return [0.1, 0.2]

    monkeypatch.setattr(embedding.httpx, "AsyncClient", lambda **kwargs: Client())
    monkeypatch.setattr("app.rag.retriever.embed_text", fake_embed)

    result = asyncio.run(retrieve("退货规则"))

    assert [item["id"] for item in result] == ["refund", "coupon"]


def test_retrieve_ignores_low_similarity_vector_results(monkeypatch):
    class Response:
        status_code = 200

        def raise_for_status(self):
            return None

        def json(self):
            return {"hits": {"hits": [] if "knn" not in self.body else [{"_id": "coupon", "_score": 0.2, "_source": SEED_DOCS[1]}]}}

    class Client:
        async def __aenter__(self):
            return self

        async def __aexit__(self, *args):
            return None

        async def post(self, url, **kwargs):
            response = Response()
            response.body = kwargs["json"]
            return response

    async def fake_embed(query):
        return [0.1, 0.2]

    monkeypatch.setattr(embedding.httpx, "AsyncClient", lambda **kwargs: Client())
    monkeypatch.setattr("app.rag.retriever.embed_text", fake_embed)

    assert asyncio.run(retrieve("完全不相关的旅行天气问题xyz")) == []


def test_business_tool_schema_rejects_unknown_tool():
    try:
        ChatRequest(conversationId="s", message="查询", businessTool="drop_database")
    except ValidationError:
        return
    raise AssertionError("unknown business tool was accepted")


def test_business_tool_schema_accepts_member_and_tax_lookup():
    assert ChatRequest(memberId=9, conversationId="s", message="查询会员积分", businessTool="query_member").businessTool == "query_member"
    assert ChatRequest(memberId=9, conversationId="s", message="查询税费", businessTool="query_tax").businessTool == "query_tax"

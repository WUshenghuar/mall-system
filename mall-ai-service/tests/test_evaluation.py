from evaluate import CASES, evaluate, evaluate_rerank, evaluate_retrieval, evaluate_with_judge, generated_answer, judge_answer, parse_judge_response, redact_eval_text
from app.api.chat import ChatRequest
from app.rag import embedding, retriever
from app.rag.retriever import DISABLED_IDS, SEED_DOCS, fallback_hits, filter_relevant, hybrid_hits, rerank_hits, retrieve
from app.utils.llm import stream_reply
from pydantic import ValidationError
import asyncio
from types import SimpleNamespace


def test_deterministic_policy_evaluation_is_green():
    assert len(CASES) >= 50
    result = evaluate()
    assert result == {"passed": len(CASES), "total": len(CASES), "score": 100}


def test_retrieval_regression_baseline_is_green():
    assert evaluate_retrieval() == {"queries": 6, "hitAt3": 1.0, "mrr": 1.0}


def test_rerank_regression_baseline_is_green():
    assert evaluate_rerank() == {"queries": 3, "hitAt1": 1.0, "mrr": 1.0}


def test_retrieve_falls_back_when_the_rag_budget_expires(monkeypatch):
    async def slow_retrieve(query):
        await asyncio.sleep(1)

    monkeypatch.setattr(retriever, "RAG_BUDGET_SECONDS", 0.01)
    monkeypatch.setattr(retriever, "_retrieve", slow_retrieve)

    assert asyncio.run(retrieve("优惠券")) == fallback_hits("优惠券")


def test_judge_response_requires_bounded_json_contract():
    assert parse_judge_response('{"score":2,"grounded":true}') == {"score": 2, "grounded": True}
    assert parse_judge_response("```json\n{\"score\":3,\"grounded\":true}\n```") is None
    assert redact_eval_text("订单 T202609071234567890，邮箱 a@example.com，电话 13800138000") == "订单 [ORDER_NO]，邮箱 [EMAIL]，电话 [NUMBER]"


def test_judge_stays_offline_without_model(monkeypatch):
    monkeypatch.setattr("evaluate.settings", SimpleNamespace(has_model=False))

    assert asyncio.run(evaluate_with_judge(2)) == {"configured": False, "cases": 2, "evaluated": 0, "passed": False}


def test_judge_reads_openai_compatible_json_response(monkeypatch):
    class Response:
        def raise_for_status(self):
            return None

        def json(self):
            return {"choices": [{"message": {"content": '{"score":2,"grounded":true}'}}]}

    class Client:
        async def __aenter__(self):
            return self

        async def __aexit__(self, *args):
            return None

        async def post(self, url, headers, json):
            assert url == "https://model.test/v1/chat/completions"
            assert json["temperature"] == 0
            assert "期望标记：订单" in json["messages"][1]["content"]
            return Response()

    monkeypatch.setattr("evaluate.settings", SimpleNamespace(
        has_model=True, model_api_base="https://model.test/v1", model_api_key="secret", model_name="judge-test"))
    monkeypatch.setattr("evaluate.httpx.AsyncClient", lambda **kwargs: Client())

    assert asyncio.run(judge_answer("订单", "订单状态正常", "订单")) == {"score": 2, "grounded": True}


def test_judge_candidate_comes_from_customer_service_stream(monkeypatch):
    async def fake_stream(message, history, context, business_context=""):
        yield "模型候选回答"

    monkeypatch.setattr("evaluate.stream_reply", fake_stream)

    assert asyncio.run(generated_answer("查询订单")) == "模型候选回答"


def test_judge_gate_rejects_low_quality_scores(monkeypatch):
    monkeypatch.setattr("evaluate.settings", SimpleNamespace(has_model=True))

    async def fake_generated(message):
        return "候选回答"

    async def fake_judge(message, answer, expected):
        return {"score": 0, "grounded": False}

    monkeypatch.setattr("evaluate.generated_answer", fake_generated)
    monkeypatch.setattr("evaluate.judge_answer", fake_judge)

    result = asyncio.run(evaluate_with_judge(2))

    assert result["evaluated"] == 2
    assert result["passed"] is False


def test_knowledge_seed_covers_core_platform_faqs():
    assert {"refund", "coupon", "logistics", "order", "payment", "member"} <= {item["id"] for item in SEED_DOCS}
    assert all(item["title"] and item["content"] for item in SEED_DOCS)


def test_rag_fallback_matches_multitopic_chinese_query():
    assert {item["id"] for item in fallback_hits("支付和会员服务怎么用？")} >= {"payment", "member"}
    assert [item["id"] for item in fallback_hits("退款规则是什么")] == ["refund"]


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


def test_local_rag_combines_two_relevant_topics(monkeypatch):
    monkeypatch.setattr("app.utils.llm.settings", SimpleNamespace(enabled=True, has_model=False))

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


def test_rerank_prefers_exact_title_match():
    documents = {item["id"]: item for item in SEED_DOCS}
    hits = rerank_hits(
        "退款规则",
        [{"_id": "coupon", "_source": documents["coupon"]}, {"_id": "refund", "_source": documents["refund"]}],
    )

    assert [hit["_id"] for hit in hits] == ["refund", "coupon"]


def test_rag_keeps_chinese_domain_for_mixed_order_number_query():
    documents = {item["id"]: item for item in SEED_DOCS}
    hits = filter_relevant(
        "查询我的物流订单 T202609071234567890",
        [{"_id": "coupon", "_source": documents["coupon"]},
         {"_id": "logistics", "_source": documents["logistics"]},
         {"_id": "order", "_source": documents["order"]}],
    )

    assert [hit["_id"] for hit in hits] == ["logistics", "order"]


def test_rerank_prefers_english_title_phrase():
    hits = rerank_hits("red dress", [
        {"_id": "content", "_source": {"title": "Summer collection", "content": "A red dress"}},
        {"_id": "title", "_source": {"title": "Red Dress", "content": "A summer item"}},
    ])

    assert [hit["_id"] for hit in hits] == ["title", "content"]


def test_production_rerank_orders_candidates_and_keeps_unranked_tail(monkeypatch):
    monkeypatch.setattr(retriever, "_rerank_retry_at", 0.0)
    outcomes = []
    monkeypatch.setattr(retriever, "record_rerank", outcomes.append)
    class Response:
        def raise_for_status(self):
            return None

        def json(self):
            return {"results": [{"index": 1, "relevance_score": 0.9}, {"index": 0, "relevance_score": 0.2}]}

    class Client:
        async def __aenter__(self):
            return self

        async def __aexit__(self, *args):
            return None

        async def post(self, url, headers, json):
            assert url == "https://rerank.test/rerank"
            assert json["model"] == "rerank-test"
            assert json["top_n"] == 3
            assert len(json["documents"]) == 3
            return Response()

    monkeypatch.setattr(retriever, "settings", SimpleNamespace(
        has_reranker=True, rerank_api_base="https://rerank.test", rerank_api_key="secret", rerank_model="rerank-test"))
    monkeypatch.setattr(retriever.httpx, "AsyncClient", lambda **kwargs: Client())
    hits = [
        {"_id": "a", "_source": {"title": "A", "content": "first"}},
        {"_id": "b", "_source": {"title": "B", "content": "second"}},
        {"_id": "c", "_source": {"title": "C", "content": "third"}},
    ]

    result = asyncio.run(retriever.production_rerank("second", hits))

    assert [hit["_id"] for hit in result] == ["b", "a", "c"]
    assert outcomes == ["success"]


def test_production_rerank_falls_back_on_invalid_response(monkeypatch):
    monkeypatch.setattr(retriever, "_rerank_retry_at", 0.0)
    outcomes = []
    monkeypatch.setattr(retriever, "record_rerank", outcomes.append)
    class Response:
        def raise_for_status(self):
            return None

        def json(self):
            return {"results": [{"index": 99, "relevance_score": 1.0}]}

    class Client:
        async def __aenter__(self):
            return self

        async def __aexit__(self, *args):
            return None

        async def post(self, *args, **kwargs):
            return Response()

    monkeypatch.setattr(retriever, "settings", SimpleNamespace(
        has_reranker=True, rerank_api_base="https://rerank.test", rerank_api_key="secret", rerank_model="rerank-test"))
    monkeypatch.setattr(retriever.httpx, "AsyncClient", lambda **kwargs: Client())
    hits = [{"_id": "a", "_source": {"title": "A"}}, {"_id": "b", "_source": {"title": "B"}}]

    result = asyncio.run(retriever.production_rerank("query", hits))

    assert result == hits
    assert outcomes == ["failed"]


def test_production_rerank_rejects_boolean_score(monkeypatch):
    monkeypatch.setattr(retriever, "_rerank_retry_at", 0.0)
    outcomes = []
    monkeypatch.setattr(retriever, "record_rerank", outcomes.append)

    class Response:
        def raise_for_status(self):
            return None

        def json(self):
            return {"results": [{"index": 0, "relevance_score": True}]}

    class Client:
        async def __aenter__(self):
            return self

        async def __aexit__(self, *args):
            return None

        async def post(self, *args, **kwargs):
            return Response()

    monkeypatch.setattr(retriever, "settings", SimpleNamespace(
        has_reranker=True, rerank_api_base="https://rerank.test", rerank_api_key="secret", rerank_model="rerank-test"))
    monkeypatch.setattr(retriever.httpx, "AsyncClient", lambda **kwargs: Client())
    hits = [{"_id": "a", "_source": {"title": "A"}}, {"_id": "b", "_source": {"title": "B"}}]

    result = asyncio.run(retriever.production_rerank("query", hits))

    assert result == hits
    assert outcomes == ["failed"]


def test_production_rerank_rejects_oversized_response(monkeypatch):
    monkeypatch.setattr(retriever, "_rerank_retry_at", 0.0)
    outcomes = []
    monkeypatch.setattr(retriever, "record_rerank", outcomes.append)

    class Response:
        def raise_for_status(self):
            return None

        def json(self):
            return {"results": [
                {"index": 0, "relevance_score": 0.9},
                {"index": 1, "relevance_score": 0.8},
                {"index": 0, "relevance_score": 0.7},
            ]}

    class Client:
        async def __aenter__(self):
            return self

        async def __aexit__(self, *args):
            return None

        async def post(self, *args, **kwargs):
            return Response()

    monkeypatch.setattr(retriever, "settings", SimpleNamespace(
        has_reranker=True, rerank_api_base="https://rerank.test", rerank_api_key="secret", rerank_model="rerank-test"))
    monkeypatch.setattr(retriever.httpx, "AsyncClient", lambda **kwargs: Client())
    hits = [{"_id": "a", "_source": {"title": "A"}}, {"_id": "b", "_source": {"title": "B"}}]

    result = asyncio.run(retriever.production_rerank("query", hits))

    assert result == hits
    assert outcomes == ["failed"]


def test_production_rerank_skips_provider_during_cooldown(monkeypatch):
    monkeypatch.setattr(retriever, "_rerank_retry_at", float("inf"))
    monkeypatch.setattr(retriever, "settings", SimpleNamespace(has_reranker=True))
    outcomes = []
    monkeypatch.setattr(retriever, "record_rerank", outcomes.append)

    result = asyncio.run(retriever.production_rerank("query", [
        {"_id": "a", "_source": {"title": "A"}}, {"_id": "b", "_source": {"title": "B"}}
    ]))

    assert [hit["_id"] for hit in result] == ["a", "b"]
    assert outcomes == ["cooldown"]


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

    assert [item["id"] for item in result] == ["refund"]


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

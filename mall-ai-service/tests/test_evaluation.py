from evaluate import evaluate
from app.api.chat import ChatRequest
from app.rag.retriever import SEED_DOCS, fallback_hits
from app.utils.llm import stream_reply
from pydantic import ValidationError
import asyncio


def test_deterministic_policy_evaluation_is_green():
    result = evaluate()
    assert result == {"passed": 6, "total": 6, "score": 100}


def test_knowledge_seed_covers_core_platform_faqs():
    assert {"refund", "coupon", "logistics", "order", "payment", "member"} <= {item["id"] for item in SEED_DOCS}
    assert all(item["title"] and item["content"] for item in SEED_DOCS)


def test_rag_fallback_matches_multitopic_chinese_query():
    assert {item["id"] for item in fallback_hits("支付和会员服务怎么用？")} >= {"payment", "member"}


def test_local_rag_combines_two_relevant_topics():
    async def collect():
        context = [{"content": "支付说明"}, {"content": "会员服务"}]
        return "".join([chunk async for chunk in stream_reply("支付和会员服务", [], context)])

    assert asyncio.run(collect()) == "支付说明\n会员服务"


def test_business_tool_schema_rejects_unknown_tool():
    try:
        ChatRequest(conversationId="s", message="查询", businessTool="drop_database")
    except ValidationError:
        return
    raise AssertionError("unknown business tool was accepted")


def test_business_tool_schema_accepts_member_and_tax_lookup():
    assert ChatRequest(memberId=9, conversationId="s", message="查询会员积分", businessTool="query_member").businessTool == "query_member"
    assert ChatRequest(memberId=9, conversationId="s", message="查询税费", businessTool="query_tax").businessTool == "query_tax"

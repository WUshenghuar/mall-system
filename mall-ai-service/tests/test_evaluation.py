from evaluate import evaluate
from app.rag.retriever import SEED_DOCS


def test_deterministic_policy_evaluation_is_green():
    result = evaluate()
    assert result == {"passed": 6, "total": 6, "score": 100}


def test_knowledge_seed_covers_core_platform_faqs():
    assert {"refund", "coupon", "logistics", "order", "payment", "member"} <= {item["id"] for item in SEED_DOCS}
    assert all(item["title"] and item["content"] for item in SEED_DOCS)

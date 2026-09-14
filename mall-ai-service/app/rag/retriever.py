import asyncio
import json
import math
import re
from time import monotonic

import httpx

from app.config import settings
from app.rag.embedding import embed_text
from app.telemetry import record_rerank, span

INDEX = "ai_knowledge"
BM25_MIN_SCORE = 3.0
_rerank_retry_at = 0.0
DISABLED_IDS: set[str] = set()
CHINESE_TERMS = {
    "refund": ("退款", "退货", "售后"), "coupon": ("优惠券", "优惠", "券包", "折扣券"),
    "logistics": ("物流", "快递", "运单", "包裹", "配送", "追踪", "轨迹"),
    "order": ("订单", "下单", "待支付", "待发货", "待收货"), "payment": ("支付", "付款", "结算"),
    "member": ("会员", "积分", "等级", "成长"), "tax": ("税费", "关税", "税金", "币种"),
}
ENGLISH_TERMS = {
    "refund": ("refund", "return", "money back"), "coupon": ("coupon", "discount", "promo", "voucher"),
    "logistics": ("tracking", "track", "delivery", "shipment", "package"), "order": ("order",),
    "payment": ("payment", "pay", "checkout"), "member": ("member", "membership", "points", "loyalty"),
    "tax": ("tax", "tariff", "duty", "currency"),
}
DOMAIN_CATEGORIES = {"refund": "after_sales", "coupon": "marketing", "logistics": "logistics", "order": "orders",
                     "payment": "payment", "member": "member", "tax": "finance"}
ENGLISH_STOP_WORDS = {"the", "is", "my", "do", "i", "a", "an", "to", "of", "can", "you", "what", "where", "how", "are", "and"}
SEED_DOCS = [
    {"id": "refund", "title": "退款规则", "category": "after_sales", "content": "待发货订单可申请仅退款。客服仅查询进度和规则，退款审批由平台售后流程处理。"},
    {"id": "coupon", "title": "优惠券规则", "category": "marketing", "content": "优惠券在优惠页领取，在会员中心查看。每人可领取次数、有效期和使用门槛以券面展示为准。"},
    {"id": "logistics", "title": "物流查询", "category": "logistics", "content": "订单发货后可在订单详情查看承运商和运单号。跨境运输状态以实际物流轨迹为准。"},
    {"id": "order", "title": "订单状态", "category": "orders", "content": "订单状态会经历待支付、待发货、待收货和已完成。待支付订单可在订单详情取消，待收货订单可确认收货。"},
    {"id": "payment", "title": "支付说明", "category": "payment", "content": "结算页会在提交订单前重新核对价格、库存和优惠券。当前 Demo 使用模拟支付，真实支付需要配置平台商户参数。"},
    {"id": "member", "title": "会员服务", "category": "member", "content": "会员登录后可以查看等级、积分余额和积分流水，管理地址、优惠券、收藏商品和浏览足迹。客服只能查询当前登录会员可访问的数据。"},
    {"id": "tax", "title": "税费与币种", "category": "finance", "content": "结算会根据商品分类、原产国、目的国和生效税率计算税费，并在订单中保存税费和币种；当前单笔订单要求商品币种一致。"},
    {"id": "refund-en", "title": "Refund policy", "category": "after_sales", "content": "Orders that have not shipped can request a refund. Refund approval is handled by the platform after-sales process."},
    {"id": "coupon-en", "title": "Coupon policy", "category": "marketing", "content": "Claim available coupons on the Offers page and view them in the member center. Limits, expiry dates, and minimum spend are shown on each coupon."},
    {"id": "logistics-en", "title": "Delivery tracking", "category": "logistics", "content": "To track a package, view the carrier and tracking number on the order details page after shipment. Cross-border delivery status follows the actual tracking events."},
    {"id": "order-en", "title": "Order status", "category": "orders", "content": "An order moves through pending payment, processing, shipped, and completed. Pending-payment orders can be cancelled from order details."},
    {"id": "payment-en", "title": "Payment information", "category": "payment", "content": "Before an order is submitted, checkout rechecks price, stock, and coupons. The current demo uses simulated payment."},
    {"id": "member-en", "title": "Membership service", "category": "member", "content": "After signing in, members can view their level, points balance, and points history. Customer service can only query data for the signed-in member."},
    {"id": "tax-en", "title": "Taxes and currency", "category": "finance", "content": "Checkout calculates tax from product category, origin, destination, and effective tax rates, then stores the tax and currency on the order."},
]


def is_english_query(query: str) -> bool:
    return bool(re.search(r"[a-zA-Z]", query)) and not bool(re.search(r"[\u4e00-\u9fff]", query))


async def ensure_seeded() -> None:
    properties = {"id": {"type": "keyword"}, "title": {"type": "text"}, "content": {"type": "text"}, "category": {"type": "keyword"}, "enabled": {"type": "boolean"}}
    vector_mapping = {"type": "dense_vector", "dims": settings.embedding_dimensions, "index": True, "similarity": "cosine"}
    if settings.has_embedding:
        properties["embedding"] = vector_mapping
    mapping = {"mappings": {"properties": properties}}
    async with httpx.AsyncClient(timeout=10) as client:
        response = await client.head(f"{settings.elasticsearch_url}/{INDEX}")
        if response.status_code == 404:
            response = await client.put(f"{settings.elasticsearch_url}/{INDEX}", json=mapping)
            if response.status_code not in (200, 201):
                response.raise_for_status()
        elif response.status_code != 200:
            response.raise_for_status()
        if settings.has_embedding:
            response = await client.put(f"{settings.elasticsearch_url}/{INDEX}/_mapping", json={"properties": {"embedding": vector_mapping}})
            response.raise_for_status()
        lines = []
        for document in SEED_DOCS:
            seeded = {"enabled": True, **document}
            lines.extend([json.dumps({"create": {"_index": INDEX, "_id": document["id"]}}), json.dumps(seeded)])
        response = await client.post(f"{settings.elasticsearch_url}/_bulk", content="\n".join(lines) + "\n", headers={"Content-Type": "application/x-ndjson"})
        response.raise_for_status()
        if settings.has_embedding:
            asyncio.create_task(_backfill_embeddings())


async def _backfill_embeddings() -> None:
    try:
        async with asyncio.timeout(60):
            async with httpx.AsyncClient(timeout=5) as client:
                search_after = None
                updated = False
                while True:
                    payload = {
                        "size": 100, "_source": ["id", "title", "content"], "sort": [{"id": "asc"}],
                        "query": {"bool": {"must_not": {"exists": {"field": "embedding"}}}},
                    }
                    if search_after:
                        payload["search_after"] = search_after
                    response = await client.post(f"{settings.elasticsearch_url}/{INDEX}/_search", json=payload)
                    response.raise_for_status()
                    hits = response.json().get("hits", {}).get("hits", [])
                    if not hits:
                        break
                    for hit in hits:
                        source = hit.get("_source", {})
                        vector = await embed_text(f"{source.get('title', '')}\n{source.get('content', '')}")
                        if vector is None:
                            return
                        update = await client.post(f"{settings.elasticsearch_url}/{INDEX}/_update/{hit.get('_id')}", json={"doc": {"embedding": vector}})
                        update.raise_for_status()
                        updated = True
                    search_after = hits[-1].get("sort")
                    if not search_after:
                        break
                if updated:
                    refresh = await client.post(f"{settings.elasticsearch_url}/{INDEX}/_refresh")
                    refresh.raise_for_status()
    except Exception:
        return


async def retrieve(query: str) -> list[dict]:
    payload = {"size": 6, "min_score": BM25_MIN_SCORE, "query": {"bool": {"must": [{"multi_match": {"query": query, "fields": ["title^3", "content"], "minimum_should_match": "30%"}}], "should": [{"term": {"enabled": True}}, {"bool": {"must_not": {"exists": {"field": "enabled"}}}}], "minimum_should_match": 1}}}
    try:
        async with httpx.AsyncClient(timeout=5) as client:
            response = await client.post(f"{settings.elasticsearch_url}/{INDEX}/_search", json=payload)
            if response.status_code == 404:
                await ensure_seeded()
                response = await client.post(f"{settings.elasticsearch_url}/{INDEX}/_search", json=payload)
            response.raise_for_status()
            bm25_hits = filter_relevant(query, [hit for hit in response.json().get("hits", {}).get("hits", [])
                                                if hit.get("_score") is not None and hit["_score"] >= BM25_MIN_SCORE])
            vector_hits = []
            try:
                vector = await embed_text(query)
                if vector:
                    vector_response = await client.post(f"{settings.elasticsearch_url}/{INDEX}/_search", json={"size": 6, "knn": {"field": "embedding", "query_vector": vector, "k": 6, "num_candidates": 20, "filter": {"bool": {"should": [{"term": {"enabled": True}}, {"bool": {"must_not": {"exists": {"field": "enabled"}}}}], "minimum_should_match": 1}}}})
                    if vector_response.status_code < 400:
                        vector_hits = filter_relevant(query, [hit for hit in vector_response.json().get("hits", {}).get("hits", [])
                                                              if hit.get("_score") is not None and hit["_score"] >= settings.embedding_min_score])
            except Exception:
                vector_hits = []
        hits = hybrid_hits(bm25_hits, vector_hits) if vector_hits else bm25_hits
        hits = rerank_hits(query, hits)
        hits = await production_rerank(query, hits)
        hits = [hit.get("_source", {}) for hit in hits]
        return hits[:3] or fallback_hits(query)
    except Exception:
        return fallback_hits(query)


def hybrid_hits(bm25_hits: list[dict], vector_hits: list[dict]) -> list[dict]:
    # ponytail: deterministic fallback is enough until labeled relevance data supports a learned reranker.
    ranked: dict[str, dict] = {}
    for hits in (bm25_hits, vector_hits):
        for rank, hit in enumerate(hits):
            source = hit.get("_source", {})
            key = str(hit.get("_id") or source.get("id") or "")
            if not key:
                continue
            entry = ranked.setdefault(key, {"hit": hit, "score": 0.0})
            entry["score"] += 1 / (60 + rank + 1)
    return [entry["hit"] for entry in sorted(ranked.values(), key=lambda item: item["score"], reverse=True)]


def rerank_hits(query: str, hits: list[dict]) -> list[dict]:
    lowered = query.lower()
    english_tokens = re.findall(r"[a-z][a-z0-9]+", lowered)
    english_terms = {term for term in english_tokens if term not in ENGLISH_STOP_WORDS}
    chinese_text = "".join(re.findall(r"[\u4e00-\u9fff]", query))
    chinese_terms = {term for terms in CHINESE_TERMS.values() for term in terms if term in query}
    chinese_terms.update(chinese_text[index:index + 2] for index in range(len(chinese_text) - 1))
    keywords = english_terms | chinese_terms
    if not keywords:
        return hits
    phrase = "".join(term for term in english_tokens if term not in ENGLISH_STOP_WORDS) if english_terms else chinese_text
    scored = []
    for index, hit in enumerate(hits):
        source = hit.get("_source", {})
        title = str(source.get("title", "")).lower()
        content = str(source.get("content", "")).lower()
        score = sum(4 if keyword in title else 1 for keyword in keywords if keyword in title or keyword in content)
        if phrase and phrase in re.sub(r"\s+", "", title):
            score += 8
        scored.append((score, index, hit))
    return [hit for _, _, hit in sorted(scored, key=lambda item: (-item[0], item[1]))]


async def production_rerank(query: str, hits: list[dict]) -> list[dict]:
    if len(hits) < 2 or not getattr(settings, "has_reranker", False):
        return hits
    if monotonic() < _rerank_retry_at:
        record_rerank("cooldown")
        return hits
    documents = [_candidate_text(hit) for hit in hits]
    payload = {"model": settings.rerank_model, "query": query[:1000], "documents": documents,
               "top_n": min(3, len(documents))}
    try:
        # ponytail: one bounded rerank call; timeout/fallback protects chat latency until a circuit breaker is needed.
        with span("ai.rag.rerank", {"ai.rerank.candidates": len(hits), "langfuse.observation.type": "retriever"}):
            async with httpx.AsyncClient(timeout=3) as client:
                response = await client.post(
                    f"{settings.rerank_api_base}/rerank",
                    headers={"Authorization": f"Bearer {settings.rerank_api_key}"},
                    json=payload,
                )
                response.raise_for_status()
        results = response.json().get("results")
        if not isinstance(results, list) or len(results) > len(hits):
            record_rerank("failed")
            _mark_rerank_failure()
            return hits
        scored = []
        seen = set()
        for result in results:
            if not isinstance(result, dict):
                record_rerank("failed")
                _mark_rerank_failure()
                return hits
            index, score = result.get("index"), result.get("relevance_score")
            if (isinstance(index, bool) or not isinstance(index, int) or index < 0 or index >= len(hits)
                    or index in seen or isinstance(score, bool) or not isinstance(score, (int, float))
                    or not math.isfinite(float(score))):
                record_rerank("failed")
                _mark_rerank_failure()
                return hits
            seen.add(index)
            scored.append((float(score), index))
        if not scored:
            record_rerank("failed")
            _mark_rerank_failure()
            return hits
        ranked = [hits[index] for _, index in sorted(scored, key=lambda item: (-item[0], item[1]))]
        ranked.extend(hit for index, hit in enumerate(hits) if index not in seen)
        record_rerank("success")
        _mark_rerank_success()
        return ranked
    except Exception:
        record_rerank("failed")
        _mark_rerank_failure()
        return hits


def _candidate_text(hit: dict) -> str:
    source = hit.get("_source") if isinstance(hit, dict) else {}
    source = source if isinstance(source, dict) else {}
    return f"{source.get('title', '')}\n{source.get('content', '')}"[:4000]


def _mark_rerank_failure() -> None:
    global _rerank_retry_at
    # ponytail: process-local cooldown avoids a dependency; use a shared breaker when running multiple workers.
    _rerank_retry_at = monotonic() + 30


def _mark_rerank_success() -> None:
    global _rerank_retry_at
    _rerank_retry_at = 0.0


def filter_relevant(query: str, hits: list[dict]) -> list[dict]:
    terms = ENGLISH_TERMS if is_english_query(query) else CHINESE_TERMS
    scopes = {scope for scope, keywords in terms.items() if any(keyword in query.lower() for keyword in keywords)}
    if not scopes:
        return hits
    categories = {DOMAIN_CATEGORIES[scope] for scope in scopes}
    return [hit for hit in hits if str(hit.get("_id", "")).removesuffix("-en") in scopes
            or hit.get("_source", {}).get("category") in categories]


def fallback_hits(query: str) -> list[dict]:
    english_terms = {term for term in re.findall(r"[a-z][a-z0-9]+", query.lower())
                     if term not in ENGLISH_STOP_WORDS} if is_english_query(query) else set()
    chinese_terms = {term for terms in CHINESE_TERMS.values() for term in terms if term in query} if not english_terms else set()
    english_scopes = {scope for scope, terms in ENGLISH_TERMS.items()
                      if any(term in query.lower() for term in terms)}
    ranked = []
    for document in SEED_DOCS:
        title = document["title"]
        text = title + document["content"]
        if english_terms:
            terms = set(re.findall(r"[a-z][a-z0-9]+", text.lower()))
            score = sum(10 for term in english_terms if term in terms)
            if document["id"].endswith("-en") and document["id"].removesuffix("-en") in english_scopes:
                score += 5
        elif chinese_terms:
            terms = CHINESE_TERMS.get(document["id"], ())
            score = sum(10 for term in terms if term in chinese_terms)
        else:
            score = 0
        if score and document["id"] not in DISABLED_IDS:
            ranked.append((score, document))
    return [document for _, document in sorted(ranked, key=lambda item: item[0], reverse=True)[:3]]

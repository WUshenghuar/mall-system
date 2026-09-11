import json

import httpx

from app.config import settings

INDEX = "ai_knowledge"
SEED_DOCS = [
    {"id": "refund", "title": "退款规则", "category": "after_sales", "content": "待发货订单可申请仅退款。客服仅查询进度和规则，退款审批由平台售后流程处理。"},
    {"id": "coupon", "title": "优惠券规则", "category": "marketing", "content": "优惠券在优惠页领取，在会员中心查看。每人可领取次数、有效期和使用门槛以券面展示为准。"},
    {"id": "logistics", "title": "物流查询", "category": "logistics", "content": "订单发货后可在订单详情查看承运商和运单号。跨境运输状态以实际物流轨迹为准。"},
    {"id": "order", "title": "订单状态", "category": "orders", "content": "订单状态会经历待支付、待发货、待收货和已完成。待支付订单可在订单详情取消，待收货订单可确认收货。"},
    {"id": "payment", "title": "支付说明", "category": "payment", "content": "结算页会在提交订单前重新核对价格、库存和优惠券。当前 Demo 使用模拟支付，真实支付需要配置平台商户参数。"},
    {"id": "member", "title": "会员服务", "category": "member", "content": "会员登录后可以管理地址、查看优惠券、收藏商品和浏览足迹。客服只能查询当前登录会员可访问的数据。"},
]


async def ensure_seeded() -> None:
    mapping = {"mappings": {"properties": {"title": {"type": "text"}, "content": {"type": "text"}, "category": {"type": "keyword"}}}}
    async with httpx.AsyncClient(timeout=10) as client:
        await client.put(f"{settings.elasticsearch_url}/{INDEX}", json=mapping)
        lines = []
        for document in SEED_DOCS:
            lines.extend([json.dumps({"index": {"_index": INDEX, "_id": document["id"]}}), json.dumps(document)])
        await client.post(f"{settings.elasticsearch_url}/_bulk", content="\n".join(lines) + "\n", headers={"Content-Type": "application/x-ndjson"})


async def retrieve(query: str) -> list[dict]:
    payload = {"size": 3, "query": {"multi_match": {"query": query, "fields": ["title^3", "content"]}}}
    try:
        async with httpx.AsyncClient(timeout=5) as client:
            response = await client.post(f"{settings.elasticsearch_url}/{INDEX}/_search", json=payload)
            if response.status_code == 404:
                await ensure_seeded()
                response = await client.post(f"{settings.elasticsearch_url}/{INDEX}/_search", json=payload)
            response.raise_for_status()
        hits = [hit["_source"] for hit in response.json()["hits"]["hits"]]
        return hits or fallback_hits(query)
    except Exception:
        return fallback_hits(query)


def fallback_hits(query: str) -> list[dict]:
    grams = {query[index:index + 2] for index in range(len(query) - 1)}
    ranked = []
    for document in SEED_DOCS:
        title = document["title"]
        text = title + document["content"]
        title_grams = {title[index:index + 2] for index in range(len(title) - 1)}
        score = sum(10 for gram in title_grams if gram in query) + sum(gram in text for gram in grams)
        if score:
            ranked.append((score, document))
    return [document for _, document in sorted(ranked, key=lambda item: item[0], reverse=True)[:3]]

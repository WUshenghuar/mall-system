import re
from typing import TypedDict

from langgraph.graph import END, START, StateGraph


class ChatState(TypedDict):
    message: str
    answer: str


def is_prompt_injection(message: str) -> bool:
    normalized = message.lower()
    return any(token in normalized for token in ("忽略", "系统提示", "system prompt", "指令", "绕过", "命令", "ignore previous"))


def is_english_message(message: str) -> bool:
    return bool(re.search(r"[a-zA-Z]", message)) and not re.search(r"[\u4e00-\u9fff]", message)


def should_suggest_handoff(message: str) -> bool:
    normalized = message.lower()
    if is_prompt_injection(message):
        return False
    if any(token in normalized for token in ("你好", "您好", "在吗", "谢谢", "感谢", "嗨", "能帮我吗", "我想咨询", "早上好", "晚上好")):
        return False
    if "thank you" in normalized or "good morning" in normalized or "good evening" in normalized:
        return False
    return not {"hello", "hi", "thanks"}.intersection(re.findall(r"[a-z]+", normalized))


def local_answer(message: str) -> str:
    normalized = message.lower()
    if is_prompt_injection(message):
        return ("I can only answer questions about platform products, orders, delivery, refund status, coupons, and membership."
                if is_english_message(message) else "我只能回答平台商品、订单、物流、退款进度、优惠券和会员相关的问题。")
    if is_english_message(message):
        if any(token in normalized for token in ("refund", "return", "money back")):
            return "I can help check your refund status and policy. Refund approval is handled by the platform after-sales process."
        if any(token in normalized for token in ("tracking", "shipment", "package", "delivery")):
            return "Please share your order number so I can help check the latest delivery status."
        if any(token in normalized for token in ("member", "membership", "points", "loyalty")):
            return "You can view your membership level and points in the member center."
        if any(token in normalized for token in ("activity", "promotion", "campaign", "sale", "deal")):
            return "You can view active promotions and their eligible products on the Offers page."
        if any(token in normalized for token in ("coupon", "discount", "promo")):
            return "You can claim available coupons on the Offers page and view them in your member center."
        if any(token in normalized for token in ("order", "product", "item")):
            return "I can help with products, orders, delivery, refunds, coupons, and membership services."
        return "Hi, I’m the HaiLu Market customer service assistant. I can help with products, orders, delivery, refunds, coupons, and membership services."
    if any(token in normalized for token in ("退款", "退货")):
        return "我可以协助查看退款进度和规则；退款审批由平台售后流程处理，客服不会直接执行退款。"
    if any(token in normalized for token in ("物流", "快递", "运单")):
        return "请告诉我订单号，下一阶段将为你接入实时物流查询。"
    if any(token in normalized for token in ("活动", "促销", "限时", "秒杀")):
        return "你可以在优惠页查看当前进行中的活动及活动商品。"
    if any(token in normalized for token in ("优惠券", "优惠")):
        return "优惠券可在优惠页领取，并在会员中心查看。每张券是否可领以页面状态为准。"
    return "你好，我是海路集市平台客服。你可以咨询商品、订单、物流、退款进度、优惠券或会员服务。"


def build_local_agent():
    graph = StateGraph(ChatState)
    graph.add_node("respond", lambda state: {"answer": local_answer(state["message"])})
    graph.add_edge(START, "respond")
    graph.add_edge("respond", END)
    return graph.compile()


local_agent = build_local_agent()

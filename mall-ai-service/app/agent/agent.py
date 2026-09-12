import re
from typing import TypedDict

from langgraph.graph import END, START, StateGraph


class ChatState(TypedDict):
    message: str
    answer: str


def is_prompt_injection(message: str) -> bool:
    normalized = message.lower()
    return any(token in normalized for token in ("忽略", "系统提示", "system prompt", "指令", "绕过", "命令", "ignore previous"))


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
        return "我只能回答平台商品、订单、物流、退款进度、优惠券和会员相关的问题。"
    if any(token in normalized for token in ("退款", "退货")):
        return "我可以协助查看退款进度和规则；退款审批由平台售后流程处理，客服不会直接执行退款。"
    if any(token in normalized for token in ("物流", "快递", "运单")):
        return "请告诉我订单号，下一阶段将为你接入实时物流查询。"
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

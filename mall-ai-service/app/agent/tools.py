TOOL_NAMES = {
    "query_order",
    "query_logistics",
    "query_refund",
    "query_product",
    "query_coupon",
    "query_member",
    "query_tax",
    "query_activity",
}

TOOL_DEFINITIONS = [
    {
        "type": "function",
        "function": {
            "name": "query_order",
            "description": "只读查询当前登录会员的订单状态和金额",
            "parameters": {"type": "object", "properties": {"order_no": {"type": "string"}}, "additionalProperties": False},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "query_logistics",
            "description": "只读查询当前登录会员订单的物流轨迹",
            "parameters": {"type": "object", "properties": {"order_no": {"type": "string"}}, "additionalProperties": False},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "query_refund",
            "description": "只读查询当前登录会员订单的退款或售后状态",
            "parameters": {"type": "object", "properties": {"order_no": {"type": "string"}}, "additionalProperties": False},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "query_product",
            "description": "只读搜索平台已上架商品",
            "parameters": {"type": "object", "properties": {"keyword": {"type": "string"}}, "additionalProperties": False},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "query_coupon",
            "description": "只读查询当前登录会员已有优惠券或平台当前可领取优惠券",
            "parameters": {"type": "object", "properties": {}, "additionalProperties": False},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "query_member",
            "description": "只读查询当前登录会员等级和积分",
            "parameters": {"type": "object", "properties": {}, "additionalProperties": False},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "query_tax",
            "description": "只读查询当前登录会员订单的税费和币种",
            "parameters": {"type": "object", "properties": {"order_no": {"type": "string"}}, "additionalProperties": False},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "query_activity",
            "description": "只读查询平台当前正在进行的活动和活动时间",
            "parameters": {"type": "object", "properties": {}, "additionalProperties": False},
        },
    },
]

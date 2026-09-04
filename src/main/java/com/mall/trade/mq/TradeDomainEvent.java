package com.mall.trade.mq;

public record TradeDomainEvent(String type, String orderNo) { }

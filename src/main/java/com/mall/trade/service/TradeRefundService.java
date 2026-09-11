package com.mall.trade.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.trade.entity.TradeRefund;

import java.util.List;

public interface TradeRefundService {
    TradeRefund apply(String orderNo, Long userId, String reason, Integer refundType, List<String> evidenceUrls);
    IPage<TradeRefund> selectMemberPage(Long userId, Integer page, Integer size);
    TradeRefund getOwnedById(Long id, Long userId);
    IPage<TradeRefund> selectAdminPage(Integer page, Integer size, Integer status);
    void approve(Long id, Long approverId, String comment);
    void reject(Long id, Long approverId, String comment);
    void complete(Long id, Long operatorId, String comment);
    void submitReturn(Long id, Long userId, String company, String trackingNo);
}

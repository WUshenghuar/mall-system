package com.mall.trade.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.trade.entity.TradeRefund;

public interface TradeRefundService {
    TradeRefund apply(String orderNo, Long userId, String reason);
    IPage<TradeRefund> selectMemberPage(Long userId, Integer page, Integer size);
    TradeRefund getOwnedById(Long id, Long userId);
    IPage<TradeRefund> selectAdminPage(Integer page, Integer size, Integer status);
    void approve(Long id, Long approverId, String comment);
    void reject(Long id, Long approverId, String comment);
    void complete(Long id, Long operatorId, String comment);
}

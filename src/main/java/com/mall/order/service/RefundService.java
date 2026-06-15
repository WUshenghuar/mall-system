package com.mall.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.order.entity.OrderRefund;

public interface RefundService {
    IPage<OrderRefund> selectPage(Integer page, Integer size, Integer refundStatus);
    OrderRefund getById(Long id);
    /** 客服发起退款 */
    void apply(OrderRefund refund, Long userId);
    /** 店长审批通过 */
    void approve(Long id, Long approverId, String comment);
    /** 店长驳回 */
    void reject(Long id, Long approverId, String comment);
}

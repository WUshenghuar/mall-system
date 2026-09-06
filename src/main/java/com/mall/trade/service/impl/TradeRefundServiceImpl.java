package com.mall.trade.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.exception.BusinessException;
import com.mall.trade.entity.TradeOrder;
import com.mall.trade.entity.TradeRefund;
import com.mall.trade.mapper.TradeOrderMapper;
import com.mall.trade.mapper.TradeRefundMapper;
import com.mall.trade.service.TradeRefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TradeRefundServiceImpl implements TradeRefundService {
    private final TradeRefundMapper refundMapper;
    private final TradeOrderMapper orderMapper;

    @Override @Transactional(rollbackFor = Exception.class)
    public TradeRefund apply(String orderNo, Long userId, String reason) {
        TradeOrder order = orderMapper.selectOne(Wrappers.<TradeOrder>lambdaQuery()
                .eq(TradeOrder::getOrderNo, orderNo).eq(TradeOrder::getUserId, userId));
        if (order == null) throw new BusinessException("订单不存在或无权申请退款");
        if (order.getOrderStatus() != 1) throw new BusinessException("仅待发货订单可申请退款");
        if (refundMapper.selectCount(Wrappers.<TradeRefund>lambdaQuery().eq(TradeRefund::getOrderNo, orderNo)
                .in(TradeRefund::getRefundStatus, 0, 1, 3)) > 0) throw new BusinessException("该订单已有退款申请");
        if (orderMapper.transitionOwned(orderNo, userId, 1, 5) != 1) throw new BusinessException("订单状态已变更，请刷新后重试");
        TradeRefund refund = new TradeRefund();
        refund.setOrderNo(orderNo); refund.setUserId(userId); refund.setRefundAmount(order.getPayAmount());
        refund.setRefundReason(reason); refund.setRefundStatus(0); refundMapper.insert(refund);
        return refund;
    }

    @Override public IPage<TradeRefund> selectMemberPage(Long userId, Integer page, Integer size) {
        return refundMapper.selectPage(new Page<>(page, Math.min(Math.max(size, 1), 100)),
                Wrappers.<TradeRefund>lambdaQuery().eq(TradeRefund::getUserId, userId).orderByDesc(TradeRefund::getCreateTime));
    }

    @Override public TradeRefund getOwnedById(Long id, Long userId) {
        TradeRefund refund = refundMapper.selectById(id);
        if (refund == null || !userId.equals(refund.getUserId())) throw new BusinessException("退款申请不存在或无权访问");
        return refund;
    }

    @Override public IPage<TradeRefund> selectAdminPage(Integer page, Integer size, Integer status) {
        return refundMapper.selectPage(new Page<>(page, Math.min(Math.max(size, 1), 100)),
                Wrappers.<TradeRefund>lambdaQuery().eq(status != null, TradeRefund::getRefundStatus, status).orderByDesc(TradeRefund::getCreateTime));
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, Long approverId, String comment) { transition(id, 0, 1, approverId, comment, false); }
    @Override @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, Long approverId, String comment) { transition(id, 0, 2, approverId, comment, true); }
    @Override @Transactional(rollbackFor = Exception.class)
    public void complete(Long id, Long operatorId, String comment) { transition(id, 1, 3, operatorId, comment, false); }

    private void transition(Long id, int expected, int target, Long operatorId, String comment, boolean restoreOrder) {
        TradeRefund refund = refundMapper.selectById(id);
        if (refund == null) throw new BusinessException("退款申请不存在");
        if (refundMapper.transitionStatus(id, expected, target, operatorId, comment) != 1) throw new BusinessException("退款申请状态已变更");
        int orderTarget = restoreOrder ? 1 : target == 3 ? 6 : 5;
        if (orderMapper.transitionOwned(refund.getOrderNo(), refund.getUserId(), 5, orderTarget) != 1) throw new BusinessException("订单退款状态异常");
    }
}

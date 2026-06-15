package com.mall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.order.entity.OrderRefund;
import com.mall.order.mapper.RefundMapper;
import com.mall.order.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {
    private final RefundMapper refundMapper;

    @Override
    public IPage<OrderRefund> selectPage(Integer page, Integer size, Integer refundStatus) {
        LambdaQueryWrapper<OrderRefund> wrapper = Wrappers.<OrderRefund>lambdaQuery()
                .eq(refundStatus != null, OrderRefund::getRefundStatus, refundStatus)
                .orderByDesc(OrderRefund::getCreateTime);
        return refundMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public OrderRefund getById(Long id) {
        return refundMapper.selectById(id);
    }

    @Override
    @Transactional
    public void apply(OrderRefund refund, Long userId) {
        refund.setApplicantId(userId);
        refund.setRefundStatus(0);
        refundMapper.insert(refund);
    }

    @Override
    @Transactional
    public void approve(Long id, Long approverId, String comment) {
        OrderRefund refund = refundMapper.selectById(id);
        if (refund == null) throw new RuntimeException("退款申请不存在");
        if (refund.getRefundStatus() != 0) throw new RuntimeException("该申请已处理");
        refund.setRefundStatus(1);
        refund.setApproverId(approverId);
        refund.setApproveTime(LocalDateTime.now());
        refund.setApproveComment(comment);
        refundMapper.updateById(refund);
    }

    @Override
    @Transactional
    public void reject(Long id, Long approverId, String comment) {
        OrderRefund refund = refundMapper.selectById(id);
        if (refund == null) throw new RuntimeException("退款申请不存在");
        refund.setRefundStatus(2);
        refund.setApproverId(approverId);
        refund.setApproveTime(LocalDateTime.now());
        refund.setApproveComment(comment);
        refundMapper.updateById(refund);
    }
}

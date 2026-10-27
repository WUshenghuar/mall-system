package com.mall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.exception.BusinessException;
import com.mall.order.entity.OrderRefund;
import com.mall.order.mapper.RefundMapper;
import com.mall.order.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 处理后台退款申请的录入、审批和驳回，并记录经办人及处理意见。 */
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

    /** 保存客服提交的退款申请，并将其置为待审批状态。 */
    @Override
    @Transactional
    public void apply(OrderRefund refund, Long userId) {
        refund.setApplicantId(userId);
        refund.setRefundStatus(0);
        refundMapper.insert(refund);
    }

    /** 仅审批待处理申请，避免重复审批覆盖原审核结果。 */
    @Override
    @Transactional
    public void approve(Long id, Long approverId, String comment) {
        OrderRefund refund = refundMapper.selectById(id);
        if (refund == null) throw new BusinessException("退款申请不存在");
        if (refund.getRefundStatus() != 0) throw new BusinessException("该申请已处理");
        refund.setRefundStatus(1);
        refund.setApproverId(approverId);
        refund.setApproveTime(LocalDateTime.now());
        refund.setApproveComment(comment);
        refundMapper.updateById(refund);
    }

    /** 记录驳回人、时间和意见，供申请人及后台后续查看。 */
    @Override
    @Transactional
    public void reject(Long id, Long approverId, String comment) {
        OrderRefund refund = refundMapper.selectById(id);
        if (refund == null) throw new BusinessException("退款申请不存在");
        refund.setRefundStatus(2);
        refund.setApproverId(approverId);
        refund.setApproveTime(LocalDateTime.now());
        refund.setApproveComment(comment);
        refundMapper.updateById(refund);
    }
}

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
import com.mall.trade.service.TradeOrderService;
import com.mall.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TradeRefundServiceImpl implements TradeRefundService {
    private final TradeRefundMapper refundMapper;
    private final TradeOrderMapper orderMapper;
    private final MemberService memberService;
    private final TradeOrderService tradeOrderService;

    @Override @Transactional(rollbackFor = Exception.class)
    public TradeRefund apply(String orderNo, Long userId, String reason, Integer refundType, List<String> evidenceUrls) {
        TradeOrder order = orderMapper.selectOne(Wrappers.<TradeOrder>lambdaQuery()
                .eq(TradeOrder::getOrderNo, orderNo).eq(TradeOrder::getUserId, userId));
        if (order == null) throw new BusinessException("订单不存在或无权申请退款");
        int type = refundType == null ? 0 : refundType;
        if (type != 0 && type != 1) throw new BusinessException("退款类型不支持");
        int originalStatus = order.getOrderStatus() == null ? -1 : order.getOrderStatus();
        if (type == 0 && originalStatus != 1) throw new BusinessException("仅待发货订单可申请仅退款");
        if (type == 1 && originalStatus != 2 && originalStatus != 3) throw new BusinessException("仅待收货或已完成订单可退货");
        if (evidenceUrls != null && evidenceUrls.size() > 5) throw new BusinessException("最多上传 5 张凭证");
        if (refundMapper.selectCount(Wrappers.<TradeRefund>lambdaQuery().eq(TradeRefund::getOrderNo, orderNo)
                .in(TradeRefund::getRefundStatus, 0, 1, 3, 4)) > 0) throw new BusinessException("该订单已有退款申请");
        if (orderMapper.transitionOwned(orderNo, userId, originalStatus, 5) != 1) throw new BusinessException("订单状态已变更，请刷新后重试");
        TradeRefund refund = new TradeRefund();
        refund.setOrderNo(orderNo); refund.setUserId(userId); refund.setRefundAmount(order.getPayAmount());
        refund.setRefundReason(reason); refund.setRefundType(type); refund.setOriginalOrderStatus(originalStatus);
        refund.setEvidenceUrls(joinEvidence(evidenceUrls)); refund.setRefundStatus(0); refundMapper.insert(refund);
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
                Wrappers.<TradeRefund>lambdaQuery()
                        .in(status != null && status == 1, TradeRefund::getRefundStatus, 1, 4)
                        .eq(status != null && status != 1, TradeRefund::getRefundStatus, status)
                        .orderByDesc(TradeRefund::getCreateTime));
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, Long approverId, String comment) { transition(id, 0, 1, approverId, comment, false); }
    @Override @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, Long approverId, String comment) { transition(id, 0, 2, approverId, comment, true); }
    @Override @Transactional(rollbackFor = Exception.class)
    public void complete(Long id, Long operatorId, String comment) {
        TradeRefund refund = refundMapper.selectById(id);
        if (refund == null) throw new BusinessException("退款申请不存在");
        if (Integer.valueOf(1).equals(refund.getRefundType()) && !Integer.valueOf(4).equals(refund.getRefundStatus())) {
            throw new BusinessException("请先提交退货物流，等待平台收货");
        }
        int expected = Integer.valueOf(1).equals(refund.getRefundType()) ? 4 : 1;
        transition(id, expected, 3, operatorId, comment, false);
        tradeOrderService.restoreStockForRefund(refund.getOrderNo(), refund.getUserId());
        if (Integer.valueOf(1).equals(refund.getRefundType()) && Integer.valueOf(3).equals(refund.getOriginalOrderStatus())) {
            memberService.recordOrderRefund(refund.getUserId(), refund.getRefundAmount(), refund.getOrderNo());
        }
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void submitReturn(Long id, Long userId, String company, String trackingNo) {
        if (!StringUtils.hasText(company) || !StringUtils.hasText(trackingNo)) {
            throw new BusinessException("退货物流信息不能为空");
        }
        if (refundMapper.submitReturn(id, userId, company.trim(), trackingNo.trim()) != 1) {
            throw new BusinessException("当前退款状态不可提交退货物流");
        }
    }

    private void transition(Long id, int expected, int target, Long operatorId, String comment, boolean restoreOrder) {
        TradeRefund refund = refundMapper.selectById(id);
        if (refund == null) throw new BusinessException("退款申请不存在");
        if (refundMapper.transitionStatus(id, expected, target, operatorId, comment) != 1) throw new BusinessException("退款申请状态已变更");
        int orderTarget = restoreOrder
                ? (refund.getOriginalOrderStatus() == null ? 1 : refund.getOriginalOrderStatus())
                : target == 3 ? 6 : 5;
        if (orderMapper.transitionOwned(refund.getOrderNo(), refund.getUserId(), 5, orderTarget) != 1) throw new BusinessException("订单退款状态异常");
    }

    private String joinEvidence(List<String> evidenceUrls) {
        if (evidenceUrls == null) return null;
        String value = evidenceUrls.stream().filter(StringUtils::hasText).map(String::trim).distinct()
                .limit(5).collect(Collectors.joining(","));
        return value.isBlank() ? null : value;
    }
}

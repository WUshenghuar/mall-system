package com.mall.trade.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.exception.BusinessException;
import com.mall.trade.entity.TradeOrder;
import com.mall.trade.entity.TradeRefund;
import com.mall.trade.mapper.TradeOrderMapper;
import com.mall.trade.mapper.TradeRefundMapper;
import com.mall.trade.mq.TradeEventPublisher;
import com.mall.trade.service.TradeRefundService;
import com.mall.trade.service.TradeOrderService;
import com.mall.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/** 处理退款申请和审核状态流转，并协调订单状态、库存返还及会员权益更新。 */
/**
 * 退款服务实现类
 * 实现了TradeRefundService接口，处理订单退款相关的业务逻辑
 */
@Service
@RequiredArgsConstructor
public class TradeRefundServiceImpl implements TradeRefundService {

    /** 退款申请查询及退款状态流转的数据访问入口。 */
    private final TradeRefundMapper refundMapper;
    /** 校验和更新退款申请关联订单状态的数据访问入口。 */
    private final TradeOrderMapper orderMapper;
    /** 更新已完成订单退款后的会员权益记录。 */
    private final MemberService memberService;
    /** 在退款完成时执行订单商品库存恢复。 */
    private final TradeOrderService tradeOrderService;
    /** 发布退款申请、审批及完成等业务事件。 */
    private final TradeEventPublisher tradeEventPublisher;

    /**
     * 创建退款申请并将订单置为退款处理中。
     *
     * <p>只允许订单所有者申请；申请类型决定可申请的订单状态。通过检查凭证数和重复申请后，
     * 使用条件状态迁移冻结订单，再保存退款单并发布事件。所有写操作处于同一事务中，
     * 任一步骤失败都会回滚退款单及订单状态。</p>
     *
     * @param orderNo 订单号
     * @param userId 申请会员 ID
     * @param reason 退款原因
     * @param refundType 退款类型，0 表示仅退款，1 表示退货退款；空值按仅退款处理
     * @param evidenceUrls 申请凭证图片地址
     * @return 新建的退款申请
     */
    @Override @Transactional(rollbackFor = Exception.class)
    public TradeRefund apply(String orderNo, Long userId, String reason, Integer refundType, List<String> evidenceUrls) {
        // 同时按订单号和用户 ID 查询，确保申请人只能对自己的订单发起退款。
        TradeOrder order = orderMapper.selectOne(Wrappers.<TradeOrder>lambdaQuery()
                .eq(TradeOrder::getOrderNo, orderNo).eq(TradeOrder::getUserId, userId));
        if (order == null) throw new BusinessException("订单不存在或无权申请退款");

        // 未传类型时兼容为仅退款；其余仅接受系统定义的两种退款流程。
        int type = refundType == null ? 0 : refundType;
        if (type != 0 && type != 1) throw new BusinessException("退款类型不支持");
        int originalStatus = order.getOrderStatus() == null ? -1 : order.getOrderStatus();

        // 仅退款适用于待发货订单；退货退款要求订单已发货或已完成。
        if (type == 0 && originalStatus != 1) throw new BusinessException("仅待发货订单可申请仅退款");
        if (type == 1 && originalStatus != 2 && originalStatus != 3) throw new BusinessException("仅待收货或已完成订单可退货");

        // 限制凭证数量，重复申请检查覆盖所有正在处理及已进入退货/退款阶段的状态。
        if (evidenceUrls != null && evidenceUrls.size() > 5) throw new BusinessException("最多上传 5 张凭证");
        if (refundMapper.selectCount(Wrappers.<TradeRefund>lambdaQuery().eq(TradeRefund::getOrderNo, orderNo)
                .in(TradeRefund::getRefundStatus, 0, 1, 3, 4)) > 0) throw new BusinessException("该订单已有退款申请");

        // 通过原状态条件更新将订单置为退款处理中，防止并发的支付、发货或其他退款覆盖状态。
        if (orderMapper.transitionOwned(orderNo, userId, originalStatus, 5) != 1) throw new BusinessException("订单状态已变更，请刷新后重试");

        // 退款金额取订单实际支付金额；同时保留原订单状态，供拒绝申请时恢复。
        TradeRefund refund = new TradeRefund();
        refund.setOrderNo(orderNo); refund.setUserId(userId); refund.setRefundAmount(order.getPayAmount());
        refund.setRefundReason(reason); refund.setRefundType(type); refund.setOriginalOrderStatus(originalStatus);
        refund.setEvidenceUrls(joinEvidence(evidenceUrls)); refund.setRefundStatus(0); refundMapper.insert(refund);

        // 退款单和订单状态落库后发布事件，供通知或后续异步流程使用。
        tradeEventPublisher.publish("refund_applied", orderNo);
        return refund;
    }

    /** 分页查询当前会员的退款申请，最新申请排在前面，页大小限制为 1 至 100。 */
    @Override public IPage<TradeRefund> selectMemberPage(Long userId, Integer page, Integer size) {
        return refundMapper.selectPage(new Page<>(page, Math.min(Math.max(size, 1), 100)),
                Wrappers.<TradeRefund>lambdaQuery().eq(TradeRefund::getUserId, userId).orderByDesc(TradeRefund::getCreateTime));
    }

    /** 查询退款申请并校验归属，避免会员通过猜测申请 ID 读取他人退款信息。 */
    @Override public TradeRefund getOwnedById(Long id, Long userId) {
        TradeRefund refund = refundMapper.selectById(id);
        if (refund == null || !userId.equals(refund.getUserId())) throw new BusinessException("退款申请不存在或无权访问");
        return refund;
    }

    /**
     * 分页查询后台退款申请。
     *
     * <p>status 为 1 时同时筛选审批中和等待退货物流/收货的申请（状态 1、4）；
     * 其他非空状态按单一状态筛选，空值表示不限制状态。</p>
     */
    @Override public IPage<TradeRefund> selectAdminPage(Integer page, Integer size, Integer status) {
        return refundMapper.selectPage(new Page<>(page, Math.min(Math.max(size, 1), 100)),
                Wrappers.<TradeRefund>lambdaQuery()
                        .in(status != null && status == 1, TradeRefund::getRefundStatus, 1, 4)
                        .eq(status != null && status != 1, TradeRefund::getRefundStatus, status)
                        .orderByDesc(TradeRefund::getCreateTime));
    }

    /** 审批退款申请；仅允许申请中状态（0）迁移到审批通过状态（1）。 */
    @Override @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, Long approverId, String comment) { transition(id, 0, 1, approverId, comment, false); }

    /** 驳回退款申请；仅允许申请中状态（0）迁移到已驳回状态（2），并恢复原订单状态。 */
    @Override @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, Long approverId, String comment) { transition(id, 0, 2, approverId, comment, true); }

    /**
     * 完成退款并恢复订单商品库存。
     *
     * <p>退货退款必须先进入已提交退货物流的状态；仅退款则从审批通过状态完成。
     * 退款状态迁移成功后补回库存；对于已完成订单的退货退款，还要回退对应会员完成订单权益。</p>
     */
    @Override @Transactional(rollbackFor = Exception.class)
    public void complete(Long id, Long operatorId, String comment) {
        TradeRefund refund = refundMapper.selectById(id);
        if (refund == null) throw new BusinessException("退款申请不存在");

        // 退货类型必须先由会员提交物流并进入状态 4，不能跳过实际退货环节直接退款。
        if (Integer.valueOf(1).equals(refund.getRefundType()) && !Integer.valueOf(4).equals(refund.getRefundStatus())) {
            throw new BusinessException("请先提交退货物流，等待平台收货");
        }

        // 退货退款从状态 4 完成；仅退款从审批通过状态 1 完成，目标状态均为 3。
        int expected = Integer.valueOf(1).equals(refund.getRefundType()) ? 4 : 1;
        transition(id, expected, 3, operatorId, comment, false);

        // 退款完成后恢复商品库存；只有原订单已完成的退货退款需要同步撤销会员完成订单权益。
        tradeOrderService.restoreStockForRefund(refund.getOrderNo(), refund.getUserId());
        if (Integer.valueOf(1).equals(refund.getRefundType()) && Integer.valueOf(3).equals(refund.getOriginalOrderStatus())) {
            memberService.recordOrderRefund(refund.getUserId(), refund.getRefundAmount(), refund.getOrderNo());
        }
    }

    /** 会员提交退货承运公司及运单号；字段先做非空校验和去除首尾空格，再由 Mapper 条件更新状态。 */
    @Override @Transactional(rollbackFor = Exception.class)
    public void submitReturn(Long id, Long userId, String company, String trackingNo) {
        if (!StringUtils.hasText(company) || !StringUtils.hasText(trackingNo)) {
            throw new BusinessException("退货物流信息不能为空");
        }
        if (refundMapper.submitReturn(id, userId, company.trim(), trackingNo.trim()) != 1) {
            throw new BusinessException("当前退款状态不可提交退货物流");
        }
    }

    /**
     * 执行退款单与订单的原子状态迁移。
     *
     * <p>退款单按预期状态条件更新，防止重复审批；订单必须仍处于退款处理中（状态 5）。
     * 驳回时恢复退款申请前的订单状态；完成时将订单置为退款完成（状态 6）；
     * 其余审批过程保持订单处于退款处理中。两类状态更新及事件发布均处于调用方事务内。</p>
     *
     * @param id 退款申请 ID
     * @param expected 允许迁移的退款申请当前状态
     * @param target 目标退款申请状态
     * @param operatorId 审核或操作人员 ID
     * @param comment 审核意见或处理说明
     * @param restoreOrder 是否在退款申请被驳回时恢复原订单状态
     */
    private void transition(Long id, int expected, int target, Long operatorId, String comment, boolean restoreOrder) {
        TradeRefund refund = refundMapper.selectById(id);
        if (refund == null) throw new BusinessException("退款申请不存在");

        // Mapper 将 expected 状态作为更新条件，避免并发操作对同一退款申请重复处理。
        if (refundMapper.transitionStatus(id, expected, target, operatorId, comment) != 1) throw new BusinessException("退款申请状态已变更");

        // 退款审批与订单状态必须一起成功，避免退款单和订单状态分叉。
        int orderTarget = restoreOrder
                ? (refund.getOriginalOrderStatus() == null ? 1 : refund.getOriginalOrderStatus())
                : target == 3 ? 6 : 5;
        if (orderMapper.transitionOwned(refund.getOrderNo(), refund.getUserId(), 5, orderTarget) != 1) throw new BusinessException("订单退款状态异常");

        // 根据退款单的新状态映射事件名称，通知下游退款流程或消息处理方。
        tradeEventPublisher.publish(target == 3 ? "refund_completed" : target == 2 ? "refund_rejected" : "refund_approved", refund.getOrderNo());
    }

    /**
     * 将凭证地址去空、去重后以逗号拼接，最多保留五条。
     *
     * <p>空列表或全部为空白的输入均保存为 null，避免产生无意义的分隔字符串。</p>
     */
    private String joinEvidence(List<String> evidenceUrls) {
        if (evidenceUrls == null) return null;
        String value = evidenceUrls.stream().filter(StringUtils::hasText).map(String::trim).distinct()
                .limit(5).collect(Collectors.joining(","));
        return value.isBlank() ? null : value;
    }
}

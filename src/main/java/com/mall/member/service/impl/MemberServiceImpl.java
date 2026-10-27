package com.mall.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.exception.BusinessException;
import com.mall.member.entity.Member;
import com.mall.member.entity.MemberPointsLog;
import com.mall.member.mapper.MemberMapper;
import com.mall.member.mapper.MemberPointsLogMapper;
import com.mall.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** 管理会员资料、积分流水和订单累计消费，并依据消费额重新计算会员等级。 */
/**
 * 会员服务实现类
 * 提供会员相关的业务逻辑处理，包括会员信息查询、积分管理、等级更新等功能
 */
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    // 会员等级阈值常量定义
    private static final BigDecimal GOLD_THRESHOLD = new BigDecimal("1000");      // 黄金会员消费金额阈值
    private static final BigDecimal PLATINUM_THRESHOLD = new BigDecimal("5000");  // 白金会员消费金额阈值


    // 数据访问层依赖
    private final MemberMapper memberMapper;          // 会员数据访问接口
    private final MemberPointsLogMapper pointsLogMapper;  // 会员积分流水数据访问接口

    @Override
    public IPage<Member> selectPage(Integer page, Integer size, String keyword, Integer level) {
        LambdaQueryWrapper<Member> wrapper = Wrappers.<Member>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(Member::getNickName, keyword)
                        .or()
                        .like(Member::getEmail, keyword))
                .eq(level != null, Member::getLevel, level)
                .orderByDesc(Member::getCreateTime);
        return memberMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 根据会员ID获取会员信息
     * @param id 会员ID，Long类型
     * @return 返回对应的Member对象，如果不存在则返回null
     */
    @Override
    public Member getById(Long id) { // 重写接口中的getById方法
        return memberMapper.selectById(id); // 调用memberMapper的selectById方法查询数据库
    }

    /** 锁定会员记录后校验余额并同时写入积分流水，确保余额和账目一起提交。 */
    @Override
    @Transactional
    public void adjustPoints(Long id, int points, String reason) {
        Member member = memberMapper.selectByIdForUpdate(id);
        if (member == null) throw new BusinessException("会员不存在");
        long adjusted = (member.getPoints() == null ? 0L : member.getPoints()) + points;
        if (adjusted < 0) throw new BusinessException("积分余额不足");
        if (adjusted > Integer.MAX_VALUE) throw new BusinessException("积分数值超限");
        member.setPoints((int) adjusted);
        memberMapper.updateById(member);

        MemberPointsLog log = new MemberPointsLog();
        log.setMemberId(id);
        log.setPoints(points);
        log.setReason(reason);
        pointsLogMapper.insert(log);
    }

    /**
     * 根据会员ID获取会员的总消费金额
     * @param id 会员ID
     * @return 会员的总消费金额，如果会员不存在则返回0
     */
    @Override
    public BigDecimal getTotalAmount(Long id) {
        // 通过会员ID查询会员信息
        Member member = memberMapper.selectById(id);
        // 使用三元运算符判断会员是否存在，存在则返回总金额，不存在则返回BigDecimal.ZERO(0)
        return member != null ? member.getTotalAmount() : BigDecimal.ZERO;
    }

    /**
     * 查询会员积分日志列表
     * @param id 会员ID
     * @return 会员积分日志列表，按创建时间降序排列，最多返回20条记录
     */
    @Override
    public List<MemberPointsLog> listPointsLogs(Long id) {
        // 使用MyBatis-Plus的LambdaQueryWrapper构建查询条件
        return pointsLogMapper.selectList(Wrappers.<MemberPointsLog>lambdaQuery()
                // 设置查询条件：会员ID等于传入的id
                .eq(MemberPointsLog::getMemberId, id)
                // 按创建时间降序排列
                .orderByDesc(MemberPointsLog::getCreateTime)
                // 限制查询结果最多返回20条记录
                .last("LIMIT 20"));
    }

    /** 订单完成时累计实付金额、按整数金额奖励积分，并基于新的累计消费刷新等级。 */
    @Override
    @Transactional
    public void recordOrderCompletion(Long id, BigDecimal amount, String orderNo) {
        if (amount == null || amount.signum() <= 0) return;
        Member member = memberMapper.selectByIdForUpdate(id);
        if (member == null) throw new BusinessException("会员不存在");
        BigDecimal total = member.getTotalAmount() == null ? BigDecimal.ZERO : member.getTotalAmount();
        int points = amount.setScale(0, RoundingMode.DOWN).intValue();
        member.setTotalAmount(total.add(amount).setScale(2, RoundingMode.HALF_UP));
        member.setPoints((member.getPoints() == null ? 0 : member.getPoints()) + points);
        member.setLevel(resolveLevel(member.getTotalAmount()));
        memberMapper.updateById(member);
        if (points > 0) {
            MemberPointsLog log = new MemberPointsLog();
            log.setMemberId(id);
            log.setPoints(points);
            log.setReason("订单完成奖励：" + orderNo);
            pointsLogMapper.insert(log);
        }
    }

    @Override
    @Transactional
    public void recordOrderRefund(Long id, BigDecimal amount, String orderNo) {
        if (amount == null || amount.signum() <= 0) return;
        Member member = memberMapper.selectByIdForUpdate(id);
        if (member == null) throw new BusinessException("会员不存在");
        BigDecimal total = member.getTotalAmount() == null ? BigDecimal.ZERO : member.getTotalAmount();
        int points = amount.setScale(0, RoundingMode.DOWN).intValue();
        member.setTotalAmount(total.subtract(amount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
        member.setPoints(Math.max(0, (member.getPoints() == null ? 0 : member.getPoints()) - points));
        member.setLevel(resolveLevel(member.getTotalAmount()));
        memberMapper.updateById(member);
        if (points > 0) {
            MemberPointsLog log = new MemberPointsLog();
            log.setMemberId(id);
            log.setPoints(-points);
            log.setReason("订单退款扣回：" + orderNo);
            pointsLogMapper.insert(log);
        }
    }

    /**
     * 根据总金额解析会员等级
     * @param totalAmount 交易总金额
     * @return 返回会员等级，0表示普通会员，1表示黄金会员，2表示白金会员
     */
    private int resolveLevel(BigDecimal totalAmount) {
        // 使用三元运算符判断总金额对应的会员等级
        // 首先判断是否达到白金会员阈值，若达到则返回2
        // 若未达到，则判断是否达到黄金会员阈值，若达到则返回1
        // 否则返回0，表示普通会员
        return totalAmount.compareTo(PLATINUM_THRESHOLD) >= 0 ? 2
                : totalAmount.compareTo(GOLD_THRESHOLD) >= 0 ? 1 : 0;
    }
}

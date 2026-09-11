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

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {
    private static final BigDecimal GOLD_THRESHOLD = new BigDecimal("1000");
    private static final BigDecimal PLATINUM_THRESHOLD = new BigDecimal("5000");
    private final MemberMapper memberMapper;
    private final MemberPointsLogMapper pointsLogMapper;

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

    @Override
    public Member getById(Long id) {
        return memberMapper.selectById(id);
    }

    @Override
    public void update(Member member) {
        memberMapper.updateById(member);
    }

    @Override
    @Transactional
    public void adjustPoints(Long id, int points, String reason) {
        Member member = memberMapper.selectById(id);
        if (member == null) throw new BusinessException("会员不存在");
        member.setPoints(member.getPoints() + points);
        memberMapper.updateById(member);

        MemberPointsLog log = new MemberPointsLog();
        log.setMemberId(id);
        log.setPoints(points);
        log.setReason(reason);
        pointsLogMapper.insert(log);
    }

    @Override
    public BigDecimal getTotalAmount(Long id) {
        Member member = memberMapper.selectById(id);
        return member != null ? member.getTotalAmount() : BigDecimal.ZERO;
    }

    @Override
    public List<MemberPointsLog> listPointsLogs(Long id) {
        return pointsLogMapper.selectList(Wrappers.<MemberPointsLog>lambdaQuery()
                .eq(MemberPointsLog::getMemberId, id)
                .orderByDesc(MemberPointsLog::getCreateTime)
                .last("LIMIT 20"));
    }

    @Override
    @Transactional
    public void recordOrderCompletion(Long id, BigDecimal amount, String orderNo) {
        if (amount == null || amount.signum() <= 0) return;
        Member member = memberMapper.selectById(id);
        if (member == null) throw new BusinessException("会员不存在");
        BigDecimal total = member.getTotalAmount() == null ? BigDecimal.ZERO : member.getTotalAmount();
        int points = amount.setScale(0, RoundingMode.DOWN).intValue();
        member.setTotalAmount(total.add(amount).setScale(2, RoundingMode.HALF_UP));
        member.setPoints((member.getPoints() == null ? 0 : member.getPoints()) + points);
        member.setLevel(member.getTotalAmount().compareTo(PLATINUM_THRESHOLD) >= 0 ? 2
                : member.getTotalAmount().compareTo(GOLD_THRESHOLD) >= 0 ? 1 : 0);
        memberMapper.updateById(member);
        if (points > 0) {
            MemberPointsLog log = new MemberPointsLog();
            log.setMemberId(id);
            log.setPoints(points);
            log.setReason("订单完成奖励：" + orderNo);
            pointsLogMapper.insert(log);
        }
    }
}

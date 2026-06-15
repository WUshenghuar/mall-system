package com.mall.member.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.member.entity.Member;

import java.math.BigDecimal;

public interface MemberService {
    IPage<Member> selectPage(Integer page, Integer size, String keyword, Integer level);
    Member getById(Long id);
    void update(Member member);
    void adjustPoints(Long id, int points, String reason);
    BigDecimal getTotalAmount(Long id);
}

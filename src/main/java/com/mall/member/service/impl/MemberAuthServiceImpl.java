package com.mall.member.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.common.exception.BusinessException;
import com.mall.member.entity.Member;
import com.mall.member.entity.MemberAccount;
import com.mall.member.mapper.MemberAccountMapper;
import com.mall.member.mapper.MemberMapper;
import com.mall.member.service.MemberAuthService;
import com.mall.security.user.MemberPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberAuthServiceImpl implements MemberAuthService {
    private final MemberMapper memberMapper;
    private final MemberAccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Member register(String phone, String password, String nickName) {
        if (accountMapper.selectCount(Wrappers.<MemberAccount>lambdaQuery()
                .eq(MemberAccount::getPhone, phone)) > 0) throw new BusinessException("手机号已注册");
        Member member = new Member();
        member.setPhone(phone);
        member.setNickName(nickName);
        member.setStatus(1);
        member.setLevel(0);
        member.setPoints(0);
        memberMapper.insert(member);
        MemberAccount account = new MemberAccount();
        account.setMemberId(member.getId());
        account.setPhone(phone);
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setStatus(1);
        accountMapper.insert(account);
        return member;
    }

    @Override
    public MemberPrincipal authenticate(String phone, String password) {
        MemberAccount account = accountMapper.selectOne(Wrappers.<MemberAccount>lambdaQuery()
                .eq(MemberAccount::getPhone, phone).last("LIMIT 1"));
        if (account == null || !passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new BusinessException(401, "手机号或密码错误");
        }
        return principal(account);
    }

    @Override
    public MemberPrincipal loadPrincipal(Long memberId) {
        MemberAccount account = accountMapper.selectOne(Wrappers.<MemberAccount>lambdaQuery()
                .eq(MemberAccount::getMemberId, memberId).last("LIMIT 1"));
        if (account == null) throw new BusinessException(401, "会员账户不存在");
        return principal(account);
    }

    @Override
    public Member getProfile(Long memberId) {
        Member member = memberMapper.selectById(memberId);
        if (member == null || !Integer.valueOf(1).equals(member.getStatus())) {
            throw new BusinessException("会员不存在或已禁用");
        }
        return member;
    }

    private MemberPrincipal principal(MemberAccount account) {
        if (!Integer.valueOf(1).equals(account.getStatus())) throw new BusinessException(401, "会员账户已禁用");
        Member member = memberMapper.selectById(account.getMemberId());
        if (member == null || !Integer.valueOf(1).equals(member.getStatus())) {
            throw new BusinessException(401, "会员不存在或已禁用");
        }
        return new MemberPrincipal(account.getMemberId(), account.getPhone(), account.getPasswordHash(), account.getStatus());
    }
}

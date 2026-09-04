package com.mall.member.service;

import com.mall.member.entity.Member;
import com.mall.security.user.MemberPrincipal;

public interface MemberAuthService {
    Member register(String phone, String password, String nickName);
    MemberPrincipal authenticate(String phone, String password);
    MemberPrincipal loadPrincipal(Long memberId);
    Member getProfile(Long memberId);
}

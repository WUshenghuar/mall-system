package com.mall.security.user;

import com.mall.common.exception.BusinessException;
import org.springframework.security.core.Authentication;

/** 仅从会员主体提取身份，阻止后台 JWT 访问会员专属资源。 */
public final class CurrentMember {
    private CurrentMember() { }

    public static Long id(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof MemberPrincipal member) {
            return member.getMemberId();
        }
        throw new BusinessException(403, "仅会员账户可访问");
    }
}

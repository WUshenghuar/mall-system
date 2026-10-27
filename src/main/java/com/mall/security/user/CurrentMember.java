package com.mall.security.user;

import com.mall.common.exception.BusinessException;
import org.springframework.security.core.Authentication;

/** 仅从会员主体提取身份，阻止后台 JWT 访问会员专属资源。 */
/**
 * CurrentMember类，用于处理当前会员相关的操作
 * 该类采用单例模式设计，确保全局只有一个实例
 */
public final class CurrentMember {
    /**
     * 私有构造方法
     * 该构造方法私有化，目的是防止外部代码直接创建CurrentMember类的实例
     * 这是一种实现单例模式的设计，确保类只有一个实例
     */
    private CurrentMember() { }

    public static Long id(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof MemberPrincipal member) {
            return member.getMemberId();
        }
        throw new BusinessException(403, "仅会员账户可访问");
    }
}

package com.mall.security.user;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** C 端会员的 Spring Security 主体，固定授予 ROLE_MEMBER 并依据账户状态启用。 */
/**
 * MemberPrincipal 类实现了 UserDetails 接口，用于表示会员用户的主体信息
 * 该类使用了 Lombok 的 @Getter 注解，自动为所有字段生成 getter 方法
 */
@Getter
public class MemberPrincipal implements UserDetails {
    // 会员ID，唯一标识一个会员
    private final Long memberId;
    // 会员手机号，同时作为用户名使用
    private final String phone;
    // 会员密码，用于身份验证
    private final String password;
    private final Integer status;

    public MemberPrincipal(Long memberId, String phone, String password, Integer status) {
        this.memberId = memberId;
        this.phone = phone;
        this.password = password;
        this.status = status;
    }

    /**
     * 获取用户权限集合的方法
     * @return 返回一个包含用户权限的Collection集合
     *         这里返回的是固定的"ROLE_MEMBER"权限
     */
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        // 使用List.of()创建一个不可变的列表，包含一个SimpleGrantedAuthority对象
        // 该对象代表"ROLE_MEMBER"角色权限
        return List.of(new SimpleGrantedAuthority("ROLE_MEMBER"));
    }
    @Override public String getUsername() { return phone; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return Integer.valueOf(1).equals(status); }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return Integer.valueOf(1).equals(status); }
}

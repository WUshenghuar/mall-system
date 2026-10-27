package com.mall.security.user;

import com.mall.system.entity.SysUser;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** 后台用户的 Spring Security 主体，分别暴露带 ROLE_ 前缀的角色和原样功能权限。 */
/**
 * 登录用户实体类，实现了UserDetails接口，用于Spring Security认证
 * 该类封装了用户的基本信息、角色和权限
 */
@Getter
public class LoginUser implements UserDetails {
    private final Long userId;        // 用户ID
    private final String username;    // 用户名
    private final String password;    // 密码
    private final String realName;    // 真实姓名
    private final List<String> roles; // 用户角色列表
    private final List<String> permissions; // 用户权限列表

    public LoginUser(SysUser user, List<String> roles, List<String> permissions) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.realName = user.getRealName();
        this.roles = roles != null ? roles : List.of();
        this.permissions = permissions != null ? permissions : List.of();
    }

    /**
     * 获取当前用户的权限集合
     * @return 权限集合，包含角色权限和功能权限
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 创建权限集合列表
        List<GrantedAuthority> authorities = new ArrayList<>();
        // 角色权限加 ROLE_ 前缀
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        // 功能权限不加前缀
        for (String perm : permissions) {
            authorities.add(new SimpleGrantedAuthority(perm));
        }
        return authorities;
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
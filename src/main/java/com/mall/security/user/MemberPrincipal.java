package com.mall.security.user;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class MemberPrincipal implements UserDetails {
    private final Long memberId;
    private final String phone;
    private final String password;
    private final Integer status;

    public MemberPrincipal(Long memberId, String phone, String password, Integer status) {
        this.memberId = memberId;
        this.phone = phone;
        this.password = password;
        this.status = status;
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_MEMBER"));
    }
    @Override public String getUsername() { return phone; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return Integer.valueOf(1).equals(status); }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return Integer.valueOf(1).equals(status); }
}

package com.mall.security.user;

import com.mall.system.entity.SysUser;
import com.mall.system.mapper.SysMenuMapper;
import com.mall.system.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserDetailsServiceImplTest {
    @Test
    void knownDevelopmentPasswordHashIsBlockedByDefault() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("$2a$10$eym7/7xeUDaruwSFuFFVuOx1SsjUK908k2730/c5XQ5E9B07vd6eW");
        when(userMapper.selectByUsername("admin")).thenReturn(user);

        UserDetailsServiceImpl service = new UserDetailsServiceImpl(userMapper, mock(SysMenuMapper.class));

        assertThatThrownBy(() -> service.loadUserByUsername("admin"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("默认密码");
    }
}

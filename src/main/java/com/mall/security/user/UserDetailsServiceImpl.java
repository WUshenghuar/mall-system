package com.mall.security.user;

import com.mall.system.entity.SysUser;
import com.mall.system.mapper.SysMenuMapper;
import com.mall.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final SysUserMapper userMapper;
    private final SysMenuMapper menuMapper;

    @Override
    public LoginUser loadUserByUsername(String username) {
        SysUser user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户名或密码错误");
        }
        return buildLoginUser(user);
    }

    public LoginUser loadByUserId(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return buildLoginUser(user);
    }

    private LoginUser buildLoginUser(SysUser user) {
        List<String> roles = userMapper.selectRoleKeysByUserId(user.getId());
        List<String> permissions = menuMapper.selectPermsByUserId(user.getId());
        return new LoginUser(user, roles, permissions);
    }
}
package com.mall.security.user;

import com.mall.system.entity.SysUser;
import com.mall.system.mapper.SysMenuMapper;
import com.mall.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/** 加载后台用户的实时角色与权限，非开发环境拒绝使用内置默认密码。 */
/**
 * 用户服务实现类，实现了UserDetailsService接口，用于处理用户认证相关逻辑
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    // 开发环境默认密码的哈希值
    private static final String DEVELOPMENT_PASSWORD_HASH =
            "$2a$10$eym7/7xeUDaruwSFuFFVuOx1SsjUK908k2730/c5XQ5E9B07vd6eW";
    // 用户数据访问对象
    private final SysUserMapper userMapper;
    // 菜单数据访问对象
    private final SysMenuMapper menuMapper;
    // 是否允许使用默认账户的配置
    @Value("${security.allow-default-accounts:false}")
    private boolean allowDefaultAccounts;

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
        if (!allowDefaultAccounts && DEVELOPMENT_PASSWORD_HASH.equals(user.getPassword())) {
            throw new UsernameNotFoundException("默认密码仅允许在开发环境使用，请先修改密码");
        }
        List<String> roles = userMapper.selectRoleKeysByUserId(user.getId());
        List<String> permissions = menuMapper.selectPermsByUserId(user.getId());
        return new LoginUser(user, roles, permissions);
    }
}

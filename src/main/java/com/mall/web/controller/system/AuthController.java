package com.mall.web.controller.system;

import com.mall.common.exception.BusinessException;
import com.mall.common.result.Result;
import com.mall.security.jwt.JwtTokenProvider;
import com.mall.security.user.LoginUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/** 提供后台用户名密码登录、JWT 签发和当前登录用户信息查询。 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    /** 委托 Spring Security 认证管理器校验后台用户名和密码。 */
    private final AuthenticationManager authenticationManager;
    /** 为认证成功的后台用户签发 JWT。 */
    private final JwtTokenProvider jwtTokenProvider;

    /** 后台登录入口；认证成功时返回 JWT、用户标识、姓名及权限列表。 */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
            );
            LoginUser loginUser = (LoginUser) auth.getPrincipal();
            String token = jwtTokenProvider.generateToken(loginUser.getUserId(), loginUser.getUsername());
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("userId", loginUser.getUserId());
            data.put("realName", loginUser.getRealName());
            data.put("permissions", loginUser.getPermissions());
            return Result.success(data);
        } catch (BadCredentialsException e) {
            throw new BusinessException(401, "用户名或密码错误");
        }
    }

    /** 返回当前认证后台用户的基本资料和权限列表。 */
    @GetMapping("/userinfo")
    public Result<Map<String, Object>> getUserInfo(Authentication auth) {
        LoginUser loginUser = (LoginUser) auth.getPrincipal();
        Map<String, Object> data = new HashMap<>();
        data.put("userId", loginUser.getUserId());
        data.put("username", loginUser.getUsername());
        data.put("realName", loginUser.getRealName());
        data.put("permissions", loginUser.getPermissions());
        return Result.success(data);
    }

    /** 后台登录请求；用户名和密码均不能为空。 */
    @Data
    public static class LoginRequest {
        @NotBlank private String username;
        @NotBlank private String password;
    }
}

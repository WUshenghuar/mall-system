package com.mall.web.controller.system;

import com.mall.common.exception.BusinessException;
import com.mall.common.result.Result;
import com.mall.security.jwt.JwtTokenProvider;
import com.mall.security.user.LoginUser;
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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest req) {
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

    @Data
    public static class LoginRequest {
        @NotBlank private String username;
        @NotBlank private String password;
    }
}
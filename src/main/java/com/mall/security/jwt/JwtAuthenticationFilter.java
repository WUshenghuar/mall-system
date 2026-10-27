package com.mall.security.jwt;

import com.mall.security.user.LoginUser;
import com.mall.security.user.MemberPrincipal;
import com.mall.security.user.UserDetailsServiceImpl;
import com.mall.member.service.MemberAuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** 校验 Bearer JWT，并按令牌身份类型重新加载后台用户或会员主体。 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;
    private final MemberAuthService memberAuthService;

    /** 验证令牌后从对应账户源重建主体，避免仅凭令牌内容建立认证上下文。 */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String token = extractToken(request);
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            var principal = "MEMBER".equals(jwtTokenProvider.getPrincipalType(token))
                    ? memberAuthService.loadPrincipal(userId) : userDetailsService.loadByUserId(userId);
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}

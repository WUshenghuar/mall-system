package com.mall.security.config;

import com.mall.security.jwt.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/** 配置无状态 JWT 认证和 HTTP 访问边界；细粒度权限由方法级安全注解控制。 */
/**
 * Spring Security安全配置类
 * 配置JWT认证、跨域请求、安全过滤器链等安全相关功能
 */
@Configuration // 标记为配置类，用于定义Spring Bean
@EnableWebSecurity // 启用Spring Security的安全功能
@EnableMethodSecurity // 启用基于方法的注解安全控制
@RequiredArgsConstructor // 使用Lombok生成构造函数，自动注入final字段
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtFilter; // JWT认证过滤器依赖注入

    /** 放行登录、公开浏览、支付通知及容器内部派发，其余请求统一要求已认证身份。 */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.ASYNC).permitAll()
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/member/auth/register", "/api/member/auth/login").permitAll()
                .requestMatchers("/api/store/**", "/api/search/**").permitAll()
                .requestMatchers("/api/trade/pay/notify/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:18103", "http://localhost:18102",
                "http://127.0.0.1:18103", "http://127.0.0.1:18102",
                "http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

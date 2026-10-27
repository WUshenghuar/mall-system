package com.mall.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/** 负责员工与会员 JWT 的签发和解析，并在启动时校验签名密钥强度。 */
/**
 * JWT令牌提供者组件，用于生成、解析和验证JWT令牌
 */
@Component
public class JwtTokenProvider {
    // 用于签名JWT的密钥
    private final SecretKey key;
    // 令牌过期时间（毫秒）
    private final long expiration;

    /**
     * 构造函数，初始化JWT密钥和过期时间
     * @param secret JWT密钥字符串
     * @param expiration 令牌过期时间（毫秒），默认为86400000（24小时）
     */
    public JwtTokenProvider(@Value("${jwt.secret:}") String secret,
                            @Value("${jwt.expiration:86400000}") long expiration) {
        // 将密钥字符串转换为字节数组
        byte[] keyBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        // 检查密钥长度是否足够
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
        // 使用密钥字节数组创建HMAC密钥
        this.key = Keys.hmacShaKeyFor(keyBytes);
        // 设置过期时间
        this.expiration = expiration;
    }

    public String generateToken(Long userId, String username) {
        return generateToken(userId, username, "STAFF");
    }

    /** 为 C 端会员签发带 MEMBER 身份标记的令牌，与后台员工令牌区分开。 */
    public String generateMemberToken(Long memberId, String phone) {
        return generateToken(memberId, phone, "MEMBER");
    }

    private String generateToken(Long userId, String username, String principalType) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("principalType", principalType)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(key)
                .compact();
    }

    /**
     * 解析JWT令牌并返回其中的声明(Claims)信息
     * @param token JWT格式的令牌字符串
     * @return 包含用户信息的Claims对象
     */
    public Claims parseToken(String token) {
        // 使用JWT解析器，配置验证密钥
        return Jwts.parser()
                // 设置用于验证令牌签名的密钥
                .verifyWith(key)
                // 构建解析器实例
                .build()
                // 解析签名后的令牌，获取其中的声明信息
                .parseSignedClaims(token)
                // 返回载荷部分(Claims)
                .getPayload();
    }

    /**
     * 验证JWT令牌的有效性
     * @param token 需要验证的JWT令牌字符串
     * @return 如果令牌有效返回true，无效返回false
     */
    public boolean validateToken(String token) {
        try {
            // 尝试解析令牌，如果解析成功则说明令牌有效
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // 捕获JWT异常或非法参数异常，说明令牌无效
            return false;
        }
    }

    /**
     * 从令牌中获取用户ID的方法
     * @param token 用户认证令牌字符串
     * @return 解析后得到的用户ID(Long类型)
     */
    public Long getUserIdFromToken(String token) {
        // 调用parseToken方法解析token，然后获取其中userId字段的值并转换为Long类型返回
        return parseToken(token).get("userId", Long.class);
    }

    /**
     * 根据token获取主体类型
     * @param token 用户认证token
     * @return 主体类型字符串，如果token中未指定则返回"STAFF"
     */
    public String getPrincipalType(String token) {
        // 解析token并获取principalType字段值
        String type = parseToken(token).get("principalType", String.class);
        // 如果type为null则返回"STAFF"，否则返回type
        return type == null ? "STAFF" : type;
    }
}

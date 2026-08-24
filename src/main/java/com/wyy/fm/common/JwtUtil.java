package com.wyy.fm.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 * 
 * 作用：生成、解析、校验 JWT token
 * 
 * JWT 是什么：
 * - JSON Web Token，一种无状态的认证方式
 * - 服务端不存储 session，所有信息都在 token 里
 * - 前端拿到 token 后，每次请求都带上，服务端验证即可
 * 
 * JWT 结构：
 * - Header：算法和类型
 * - Payload：实际数据（如 userId）
 * - Signature：签名（防篡改）
 * 
 * 流程：
 * 1. 用户登录成功 → 服务端生成 token → 返回给前端
 * 2. 前端存到 localStorage
 * 3. 后续请求放到 Header：Authorization: Bearer {token}
 * 4. 服务端解析 token，拿到 userId，验证是否过期
 */
@Component  // @Component：标记为 Spring 组件，自动注册为 Bean（可以被 @Autowired 注入）
public class JwtUtil {

    /**
     * JWT 签名密钥
     * - 从配置文件读取：application.yml 里的 jwt.secret
     * - @Value：Spring 注解，注入配置值
     * - 生产环境必须用强密钥（至少 256 位）
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * token 有效期（毫秒）
     * - 从配置文件读取：application.yml 里的 jwt.expiration
     * - 默认值：86400000 毫秒 = 24 小时
     * - 过期后需要重新登录
     */
    @Value("${jwt.expiration:86400000}")
    private long expiration; // 默认 24 小时

    /**
     * 生成签名密钥
     * - 将字符串转换为 HMAC-SHA 算法需要的密钥
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT token
     * 
     * @param userId 用户 ID（存到 token 里）
     * @return JWT token 字符串
     * 
     * 示例：
     * String token = jwtUtil.generateToken(1L);
     * // 返回："eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNjI5..."
     */
    public String generateToken(Long userId) {
        Date now = new Date();  // 当前时间
        Date expiryDate = new Date(now.getTime() + expiration);  // 过期时间

        return Jwts.builder()
                .subject(String.valueOf(userId))  // payload 里的 sub 字段（存 userId）
                .issuedAt(now)                    // payload 里的 iat 字段（签发时间）
                .expiration(expiryDate)           // payload 里的 exp 字段（过期时间）
                .signWith(getSigningKey())        // 用密钥签名
                .compact();                       // 生成最终 token 字符串
    }

    /**
     * 从 token 解析用户 ID
     * 
     * @param token JWT token
     * @return 用户 ID
     * 
     * 示例：
     * Long userId = jwtUtil.getUserIdFromToken("eyJhbGci...");
     * // 返回：1L
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())  // 验证签名
                .build()
                .parseSignedClaims(token)     // 解析 token
                .getPayload();                // 拿到 payload
        return Long.parseLong(claims.getSubject());  // subject 里存的是 userId
    }

    /**
     * 校验 token 是否有效
     * 
     * @param token JWT token
     * @return true：有效，false：无效（过期、篡改、格式错误）
     * 
     * 使用场景：
     * - 每次请求时校验 token
     * - 如果无效，返回 401 未登录
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;  // 解析成功，token 有效
        } catch (Exception e) {
            return false;  // 解析失败，token 无效
        }
    }
}

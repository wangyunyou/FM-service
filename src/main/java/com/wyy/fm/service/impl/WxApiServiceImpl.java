package com.wyy.fm.service.impl;

import com.wyy.fm.common.BusinessException;
import com.wyy.fm.common.ErrorCode;
import com.wyy.fm.service.WxApiService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

/**
 * 微信 API 服务实现类
 * 
 * 作用：调用微信后端接口（code2Session）
 * 
 * 流程：
 * 1. 小程序调用 wx.login() 获取 code
 * 2. 前端将 code 传给后端
 * 3. 后端用 code + appid + secret 调用微信接口
 * 4. 微信返回 openid（用户在当前小程序的唯一标识）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxApiServiceImpl implements WxApiService {

    /**
     * 微信小程序 appid
     * - 从配置文件读取：application.yml 里的 wx.miniapp.appid
     */
    @Value("${wx.miniapp.appid}")
    private String appid;

    /**
     * 微信小程序 secret
     * - 从配置文件读取：application.yml 里的 wx.miniapp.secret
     * - 敏感信息，不要硬编码在代码里
     */
    @Value("${wx.miniapp.secret}")
    private String secret;

    // HTTP 客户端（用于调用微信接口）
    private final RestTemplate restTemplate;

    /**
     * 是否启用本地 mock 登录（默认 false）
     *
     * 为什么需要：
     * - 没拿到真实 appid/secret 时，本地无法走通微信接口，登录必挂
     * - 开启后任意 code 都能换出稳定的 openid，方便本地联调和接口测试
     *
     * 安全边界：
     * - 默认关闭，只允许在 dev profile 里显式打开
     * - 开启后相当于“任何人拿到接口就能注册登录”，生产环境绝不允许置 true
     */
    @Value("${wx.miniapp.mock-enabled:false}")
    private boolean mockEnabled;

    /**
     * mock openid 前缀：让假用户在 users 表里一眼可识别，便于清理
     */
    private static final String MOCK_OPENID_PREFIX = "mock-openid-";

    /**
     * mock openid 里哈希部分的长度（16 进制字符数）
     * - 加上前缀后总长远小于 users.openid 的 VARCHAR(64) 限制
     */
    private static final int MOCK_OPENID_HASH_LENGTH = 32;

    /**
     * 启动时把 mock 状态大声记进日志
     * - 避免“本地忘了关”被带上生产，从启动日志就能发现
     */
    @PostConstruct
    void warnIfMockEnabled() {
        if (mockEnabled) {
            log.warn("微信登录 mock 已开启（wx.miniapp.mock-enabled=true），任意 code 均可换取 token，严禁用于生产");
        }
    }

    /**
     * 微信 code2Session 接口地址
     * 
     * 参数说明：
     * - appid：小程序 appid
     * - secret：小程序 secret
     * - js_code：小程序 wx.login() 获取的 code
     * - grant_type：固定值 "authorization_code"
     * 
     * {appid}、{secret}、{code} 是占位符，RestTemplate 会自动替换
     */
    private static final String CODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code";

    /**
     * 用 code 换取 openid
     * 
     * @param code 小程序登录凭证
     * @return openid
     * @throws BusinessException 如果调用失败
     */
    @Override
    public String code2Session(String code) {
        // 0. 本地 mock 分支：不请求微信，直接由 code 推导一个稳定 openid
        //    同一 code 永远得到同一 openid，因此重复登录不会创建新用户
        if (mockEnabled) {
            String mockOpenid = buildMockOpenid(code);
            log.warn("mock 登录生效，跳过微信 code2Session: openid={}", mockOpenid);
            return mockOpenid;
        }

        // 1. 准备请求参数
        Map<String, String> params = Map.of(
                "appid", appid,
                "secret", secret,
                "code", code
        );

        try {
            // 2. 调用微信接口
            @SuppressWarnings("unchecked")
            Map<String, Object> result = restTemplate.getForObject(
                    CODE2SESSION_URL, Map.class, params);

            // 3. 校验返回结果
            if (result == null) {
                throw new BusinessException(ErrorCode.WX_LOGIN_FAILED);
            }

            // 如果返回了 errcode 且不等于 0，说明调用失败
            if (result.containsKey("errcode") && !result.get("errcode").equals(0)) {
                log.error("微信 code2Session 失败: {}", result);  // 记录错误日志
                throw new BusinessException(ErrorCode.WX_LOGIN_FAILED, "微信登录失败: " + result.get("errmsg"));
            }

            // 4. 返回 openid
            return (String) result.get("openid");
        } catch (BusinessException e) {
            // 业务异常直接抛出（不包装）
            throw e;
        } catch (Exception e) {
            // 其他异常（网络错误、超时等）包装为业务异常
            log.error("调用微信 code2Session 异常", e);
            throw new BusinessException(ErrorCode.WX_API_ERROR);
        }
    }

    /**
     * 生成 mock openid
     *
     * 为什么用哈希而不是直接用 code：
     * - 微信 code 长度不定且可能包含特殊字符，截断会丢唯一性
     * - SHA-256 取前 32 位，既保证同 code 可复现，又不会超出 openid 字段长度
     *
     * 注：这里只用于伪造本地测试身份，不承担安全职责
     */
    private String buildMockOpenid(String code) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(code.getBytes(StandardCharsets.UTF_8));
            String hash = HexFormat.of().formatHex(digest)
                    .substring(0, MOCK_OPENID_HASH_LENGTH);
            return MOCK_OPENID_PREFIX + hash;
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 是 JDK 必备算法，理论上不可达；保留日志避免空 catch 吞异常
            log.error("生成 mock openid 失败", e);
            throw new BusinessException(ErrorCode.WX_API_ERROR, "本地 mock 登录不可用");
        }
    }
}

package com.wyy.fm.service.impl;

import com.wyy.fm.common.BusinessException;
import com.wyy.fm.common.ErrorCode;
import com.wyy.fm.service.WxApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
}

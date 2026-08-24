package com.wyy.fm.service.impl;

import com.wyy.fm.service.WxApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WxApiServiceImpl implements WxApiService {

    @Value("${wx.miniapp.appid}")
    private String appid;

    @Value("${wx.miniapp.secret}")
    private String secret;

    private final RestTemplate restTemplate;

    private static final String CODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code";

    @Override
    public String code2Session(String code) {
        Map<String, String> params = Map.of(
                "appid", appid,
                "secret", secret,
                "code", code
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = restTemplate.getForObject(
                    CODE2SESSION_URL, Map.class, params);

            if (result == null) {
                throw new RuntimeException("微信接口返回空");
            }

            if (result.containsKey("errcode") && !result.get("errcode").equals(0)) {
                log.error("微信 code2Session 失败: {}", result);
                throw new RuntimeException("微信登录失败: " + result.get("errmsg"));
            }

            return (String) result.get("openid");
        } catch (Exception e) {
            log.error("调用微信 code2Session 异常", e);
            throw new RuntimeException("微信登录失败", e);
        }
    }
}

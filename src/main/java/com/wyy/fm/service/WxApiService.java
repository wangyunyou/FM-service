package com.wyy.fm.service;

/**
 * 微信 API 服务 — 调用微信后端接口
 */
public interface WxApiService {

    /**
     * code 换 openid（调用微信 code2Session 接口）
     */
    String code2Session(String code);
}

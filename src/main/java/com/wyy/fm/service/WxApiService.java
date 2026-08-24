package com.wyy.fm.service;

/**
 * 微信 API 服务接口
 * 
 * 作用：封装微信后端接口调用
 * 
 * 实现类：WxApiServiceImpl
 */
public interface WxApiService {

    /**
     * 用 code 换取 openid
     * 
     * 调用微信 code2Session 接口：
     * https://api.weixin.qq.com/sns/jscode2session
     * 
     * @param code 小程序 wx.login() 获取的临时登录凭证
     * @return openid 用户在当前小程序的唯一标识
     * @throws com.wyy.fm.common.BusinessException 如果调用失败
     */
    String code2Session(String code);
}

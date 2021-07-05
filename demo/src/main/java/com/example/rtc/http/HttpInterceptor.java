package com.example.rtc.http;

import android.net.Uri;

import okhttp3.Request;

/**
 * Http请求拦截器
 */
public interface HttpInterceptor {
    /**
     * 发起Http请求前回调，用于修改Header,Query等请求参数
     *
     * @param request
     * @param headerBuilder 修改Header内容实例
     * @param queryBuilder  修改Query内容实例
     */
    void onPreRequest(HttpRequest request, Request.Builder headerBuilder, Uri.Builder queryBuilder);
}

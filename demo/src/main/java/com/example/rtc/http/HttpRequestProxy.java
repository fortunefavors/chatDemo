package com.example.rtc.http;

import android.net.Uri;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.Request;

/**
 * Http请求统一代理定义(不同模块可能需要不同的公共参数,可使用不同的proxy实现)
 */
public class HttpRequestProxy implements HttpInterceptor {
    public static final HttpInterceptor interceptor = new HttpRequestProxy();
    private static final Map<String, String> HEADERS = new HashMap<>();
    public static final String QUERY_SECURITY_KEY = "securityKey";

    static {
        HEADERS.put("Accept", "application/json");
        HEADERS.put("Accept-Charset", "utf-8");
        HEADERS.put("Accept-Encoding", "gzip");
        HEADERS.put("Accept-Language", Locale.getDefault().toString().replace("_", "-"));
        HEADERS.put("n-app-info", "apptype=xiaodu");
    }

    /**
     * 设置特殊的Http头
     */
    public static void setHeader(String name, String value) {
        HEADERS.put(name, value);
    }

    private static final Map<String, String> PARAMETERS = new HashMap<>();

    /**
     * 设置全局公用的参数
     */
    public static void setParameter(String name, String value) {
        PARAMETERS.remove(name);
        if (!TextUtils.isEmpty(value)) {
            PARAMETERS.put(name, value);
        }
    }

    /**
     * 异步请求(回调在请求线程执行)
     */
    public static void async(HttpRequest request, HttpRequest.Callback callback) {
        request.setHttpInterceptor(interceptor);
        request.async(callback);
    }

    /**
     * 异步请求(回调在UI线程执行)
     */
    public static void asyncUI(HttpRequest request, HttpRequest.Callback callback) {
        request.setHttpInterceptor(interceptor);
        request.asyncUI(callback);
    }

    @Override
    public void onPreRequest(HttpRequest request, Request.Builder headerBuilder, Uri.Builder queryBuilder) {
        for (Map.Entry<String, String> entry : HEADERS.entrySet()) {
            headerBuilder.addHeader(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> entry : PARAMETERS.entrySet()) {
            queryBuilder.appendQueryParameter(entry.getKey(), entry.getValue());
        }
        queryBuilder.appendQueryParameter("apptype", "xiaodu");
    }
}
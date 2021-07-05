package com.example.rtc.http;

import android.net.Uri;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Request;
import okhttp3.RequestBody;

public class Get extends HttpRequest {
    private Map<String, String> params = new HashMap<>();

    public Get(Uri uri) {
        super(uri, Method.GET);
    }

    public Get(Uri uri, HttpInterceptor interceptor) {
        super(uri, Method.GET, interceptor);
    }

    Get(Uri uri, Method method, HttpInterceptor interceptor) {
        super(uri, method, interceptor);
    }

    public void setParam(String key, String value) {
        params.put(key, value);
    }

    @Override
    protected RequestBody fillRequest(Request.Builder builder, Uri.Builder uriBuilder) {
        builder.addHeader(CONTENT_TYPE, MEDIA_TYPE_JSON);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            uriBuilder.appendQueryParameter(entry.getKey(), entry.getValue());
        }
        return null;
    }
}

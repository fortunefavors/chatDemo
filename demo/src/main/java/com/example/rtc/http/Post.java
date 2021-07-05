package com.example.rtc.http;

import android.net.Uri;

import com.xiaoyu.utils.JsonUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;

public class Post extends HttpRequest {
    private Map<String, Object> params = new HashMap<>();

    public Post(Uri uri) {
        super(uri, Method.POST);
    }

    public Post(Uri uri, HttpInterceptor interceptor) {
        super(uri, Method.POST, interceptor);
    }

    Post(Uri uri, Method method, HttpInterceptor interceptor) {
        super(uri, method, interceptor);
    }

    private boolean isFrom = false;

    /**
     * 以Form方式工作
     */
    public void workAsForm() {
        isFrom = true;
    }

    private boolean needMultiPart = false;

    public void setParam(String key, Object value) {
        params.put(key, value);
        if (!needMultiPart) {
            needMultiPart = value instanceof File || value instanceof byte[];
        }
    }

    private Object jsonObject;

    public void setJsonObject(Object object) {
        this.jsonObject = object;
    }

    @Override
    protected RequestBody fillRequest(Request.Builder builder, Uri.Builder uriBuilder) {
        RequestBody requestBody;
        if (jsonObject != null) {
            builder.addHeader(CONTENT_TYPE, MEDIA_TYPE_JSON);
            if (jsonObject instanceof String) {
                requestBody = RequestBody.create(MediaType.parse(MEDIA_TYPE_JSON), (String) jsonObject);
            } else {
                requestBody = RequestBody.create(MediaType.parse(MEDIA_TYPE_JSON), JsonUtil.toString(jsonObject));
            }
        } else if (needMultiPart) {
            MultipartBody.Builder mBuilder = new MultipartBody.Builder();
            mBuilder.setType(MultipartBody.ALTERNATIVE);
            Object value;
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                value = entry.getValue();
                if (value instanceof File) {
                    mBuilder.addFormDataPart(entry.getKey(), ((File) value).getName(), RequestBody.create(MEDIA_TYPE_STREAM, (File) value));
                } else if (value instanceof byte[]) {
                    mBuilder.addFormDataPart(entry.getKey(), null, RequestBody.create(MEDIA_TYPE_STREAM, (byte[]) value));
                } else {
                    mBuilder.addFormDataPart(entry.getKey(), String.valueOf(value));
                }
            }
            requestBody = mBuilder.build();
        } else if (isFrom) {
            builder.addHeader(CONTENT_TYPE, MEDIA_TYPE_STRING);
            FormBody.Builder fBuilder = new FormBody.Builder();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                fBuilder.add(entry.getKey(), String.valueOf(entry.getValue()));
            }
            requestBody = fBuilder.build();
        } else {
            builder.addHeader(CONTENT_TYPE, MEDIA_TYPE_JSON);
            requestBody = RequestBody.create(MediaType.parse(MEDIA_TYPE_JSON), JsonUtil.toString(params));
        }
        return requestBody;
    }
}

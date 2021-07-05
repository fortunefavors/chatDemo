package com.example.rtc.http;


import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.util.logging.Logger;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

public abstract class HttpRequest {
    private final static Logger LOGGER = Logger.getLogger("HttpRequest");
    final static String CONTENT_TYPE = "Content-Type";
    final static String MEDIA_TYPE_JSON = "application/json;charset=utf-8";
    final static String MEDIA_TYPE_STRING = "text/plain;charset=utf-8";
    final static MediaType MEDIA_TYPE_STREAM = MediaType.parse("application/octet-stream;charset=utf-8");
    final static Handler handler = new Handler(Looper.getMainLooper());

    enum Method {
        GET("GET"), PUT("PUT"), POST("POST"), DELETE("DELETE");

        public String value;

        Method(String value) {
            this.value = value;
        }
    }

    private final Uri uri;
    private final Method method;

    HttpRequest(Uri uri, Method method) {
        this.uri = uri;
        this.method = method;
    }

    HttpRequest(Uri uri, Method method, HttpInterceptor interceptor) {
        this.uri = uri;
        this.method = method;
        this.interceptor = interceptor;
    }

    private HttpInterceptor interceptor;

    public void setHttpInterceptor(HttpInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    String uriStr;

    final void fillRequest(Request.Builder builder) {
        Uri.Builder uriBuilder = uri.buildUpon();
        if (interceptor != null) {
            interceptor.onPreRequest(this, builder, uriBuilder);
        }
        RequestBody requestBody = fillRequest(builder, uriBuilder);
        uriStr = uriBuilder.toString();
        //if (BuildConfig.SELF_DEBUG) {
        LOGGER.info(uriStr + " > " + method.value);
        //}
        builder.url(uriStr);
        switch (method) {
            case GET:
                builder.get();
                break;
            case DELETE:
                builder.delete();
                break;
            case POST:
                builder.post(requestBody);
                break;
            case PUT:
                builder.put(requestBody);
                break;
        }
    }

    int sequence;

    /**
     * 参数将通过{@link HttpResponse#sequence}原样返回
     */
    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    Object[] args;

    /**
     * 参数将通过{@link HttpResponse#args}原样返回
     */
    public void setOrgArgs(Object... args) {
        this.args = args;
    }

    protected abstract RequestBody fillRequest(Request.Builder builder, Uri.Builder uriBuilder);

    /**
     * 异步请求(回调在请求线程执行)
     */
    public void async(Callback callback) {
        async(callback, false);
    }

    /**
     * 异步请求(回调在UI线程执行)
     */
    public void asyncUI(Callback callback) {
        async(callback, true);
    }

    /**
     * 同步请求
     *
     * @return
     */
    public HttpResponse sync() {
        return HttpClient.sync(this);
    }

    /**
     * 异步请求
     */
    private void async(Callback callback, boolean onUi) {
        HttpClient.async(this, (response) -> {
            //if (BuildConfig.SELF_DEBUG) {
            if (response.code < 300) {
                LOGGER.info(response.uriStr + " < " + response.code);
            } else {
                LOGGER.severe(response.uriStr + " < " + response.code);
            }
            //}
            //TODO 统一请求处理
            if (callback != null) {
                if (onUi) {
                    handler.post(() -> callback.onComplete(response));
                } else {
                    callback.onComplete(response);
                }
            }
        });
    }

    public interface Callback {
        void onComplete(HttpResponse response);
    }
}

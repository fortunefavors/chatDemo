package com.example.rtc.http;


import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

class HttpClient {
    private static OkHttpClient client;

    static {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        //builder.sslSocketFactory(HttpsTrustManager.createSSLSocketFactory());
        builder.sslSocketFactory(HttpsTrustManager.createSSLSocketFactory(), HttpsTrustManager.getTrustManager());
        builder.readTimeout(8, TimeUnit.SECONDS);
        builder.writeTimeout(8, TimeUnit.SECONDS);
        builder.connectTimeout(8, TimeUnit.SECONDS);
        builder.connectionPool(new ConnectionPool(5, 1, TimeUnit.MINUTES));
        builder.connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT));
        client = builder.build();
    }

    static void async(HttpRequest request, HttpRequest.Callback callback) {
        Request.Builder builder = new Request.Builder();
        request.fillRequest(builder);
        client.newCall(builder.build()).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null) {
                    try {
                        HttpResponse response = new HttpResponse();
                        response.code = -1;
                        response.success = false;
                        response.exception = e;
                        response.update(request);
                        callback.onComplete(response);
                    } catch (Exception exp) {
                        exp.printStackTrace();
                    }
                }
            }

            @Override
            public void onResponse(Call call, Response resp) {
                if (callback != null) {
                    try {
                        HttpResponse response = new HttpResponse();
                        response.code = resp.code();
                        response.success = resp.isSuccessful();
                        ResponseBody body = resp.body();
                        if (body != null) {
                            response.content = body.string();
                        }
                        response.update(request);
                        callback.onComplete(response);
                    } catch (Exception exp) {
                        exp.printStackTrace();
                    }
                }
            }
        });
    }

    static HttpResponse sync(HttpRequest request) {
        Request.Builder builder = new Request.Builder();
        request.fillRequest(builder);
        Response resp = null;
        try {
            resp = client.newCall(builder.build()).execute();
        } catch (Exception e) {
        }

        HttpResponse response = null;
        if (resp == null) {
            response = new HttpResponse();
            response.code = -1;
            response.success = false;
            response.update(request);
        } else {
            response = new HttpResponse();
            response.code = resp.code();
            response.success = resp.isSuccessful();
            ResponseBody body = resp.body();
            if (body != null) {
                try {
                    response.content = body.string();
                } catch (Exception e) {
                }
            }
            response.update(request);
        }
        return response;
    }
}

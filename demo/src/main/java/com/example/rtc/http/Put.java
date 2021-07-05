package com.example.rtc.http;

import android.net.Uri;

public class Put extends Post {

    public Put(Uri uri) {
        super(uri, Method.PUT, null);
    }

    public Put(Uri uri, HttpInterceptor interceptor) {
        super(uri, Method.PUT, interceptor);
    }
}
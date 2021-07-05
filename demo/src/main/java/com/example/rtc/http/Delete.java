package com.example.rtc.http;

import android.net.Uri;

public class Delete extends Get {

    public Delete(Uri uri) {
        super(uri, Method.DELETE, null);
    }

    public Delete(Uri uri, HttpInterceptor interceptor) {
        super(uri, Method.DELETE, interceptor);
    }
}

package com.example.rtc.http;

import java.io.IOException;
import java.util.Arrays;

public class HttpResponse {
    String uriStr;
    public boolean success;
    public int code;
    public IOException exception;
    public String content;
    public int sequence;
    public Object[] args;

    void update(HttpRequest request) {
        uriStr = request.uriStr;
        sequence = request.sequence;
        args = request.args;
    }

    @Override
    public String toString() {
        return "HttpResponse{" +
                "code=" + code +
                "，success=" + success +
                ", sequence=" + sequence +
                ", args=" + Arrays.toString(args) +
                ", exception=" + exception +
                ", content='" + content + '\'' +
                '}';
    }
}

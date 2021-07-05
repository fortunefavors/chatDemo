package com.example.rtc;

import android.app.Application;
import android.net.Uri;

import com.xiaoyu.rtc.wrapper.MediaCustomKey;
import com.xiaoyu.rtc.wrapper.Member;
import com.xiaoyu.rtc.wrapper.SharedPreferencesUtil;
import com.xiaoyu.rtc.wrapper.ToastUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DemoApplication extends Application {
    /**
     * 由Rtc平台分配的appId
     */
    public final static String APP_ID = "1005";
    /**
     * 演示用Token服务地址（使用时请注意与rtc-core.aar中限制的环境匹配）
     */
    private final static String TOKEN_SERVER = "https://bdavwsqa.zaijia.cn/"; //沙盒演示测试token生成服务
    /**
     * 设置Demo安装的设备类型
     */
    public static MediaCustomKey MEDIA_CUSTOM_KEY;

    /**
     * 获得测试用的token服务地址，正式上线应由接入方后台自行提供（后台对接生成算法）
     *
     * @param appId 唯一应用标识 {@link #APP_ID}
     * @param uid   唯一用户表示
     * @return token服务地址
     */
    public static Uri getTokenUri(String appId, String uid) {
        return Uri.parse(TOKEN_SERVER + String.format(Locale.getDefault(), "api/getToken?appid=%s&uid=%s", appId, uid));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //示例代码，仅做演示参考
        SharedPreferencesUtil.INSTANCE.init(this);
        ToastUtil.init(this);
    }

    public static List<Member> members = new ArrayList<>();

    public static boolean changeMediaCustomKey = true;
}

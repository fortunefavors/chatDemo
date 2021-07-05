package com.xiaoyu.rtc.wrapper;

import com.xiaoyu.open.call.RtcCallIntent;

/**
 * 呼入拦截器，测试任务定制开发
 */
public interface RtcIncomingCallInterceptor {
    /**
     * 当收到用户来电时回调
     *
     * @param intent 来电Intent
     */
    boolean onIncomingCall(RtcCallIntent intent);
}

package com.xiaoyu.rtc.wrapper;

import com.xiaoyu.open.RtcUri;
import com.xiaoyu.open.call.RtcCallIntent;
import com.xiaoyu.open.call.RtcCallMode;
import com.xiaoyu.open.call.RtcCallStateInfo;
import com.xiaoyu.open.call.RtcIncomingCallListener;
import com.xiaoyu.open.call.RtcObservedChangedListener;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Rtc呼入Intent队列管理，在同时收到多个呼入请求时有用
 */
public class RtcIncomingManager implements RtcIncomingCallListener, RtcObservedChangedListener {
    public final static RtcIncomingManager INSTANCE = new RtcIncomingManager();

    private RtcIncomingManager() {

    }

    private RtcIncomingCallInterceptor incomingCallInterceptor;

    /**
     * 设置呼入拦截器
     */
    public synchronized void setIncomingCallInterceptor(RtcIncomingCallInterceptor interceptor) {
        incomingCallInterceptor = interceptor;
    }

    @Override
    public synchronized void onIncomingCall(RtcCallIntent intent) {
        if (incomingCallInterceptor == null || !incomingCallInterceptor.onIncomingCall(intent)) {
            LOGGER.info("onIncomingCall: callIndex=" + intent.callIndex);
            offer(intent);
            if (callActivityListener != null) {
                callActivityListener.onStartCallActivity(intent);
            }
        } else {
            LOGGER.severe("onIncomingCall: callIndex=" + intent.callIndex + ", skip by interceptor");
        }
    }

    @Override
    public synchronized void onIncomingCallDisconnected(RtcCallStateInfo status) {
        LOGGER.info("onIncomingCallDisconnected: callIndex=" + status.callIndex + ", reason=" + status.reason);
        remove(status.callIndex);
    }

    @Override
    public synchronized void onObservedStateChanged(int callIndex, boolean observed) {
        LOGGER.info("onObservedStateChanged: callIndex=" + callIndex + ", observed=" + observed);
        if (observed || callActivityListener == null) {
            return;
        }
        //被回家看看 音视频通话 切换辅助
        RtcCallIntent intent = new RtcCallIntent();
        intent.direction = RtcCallIntent.Direction.CONNECTED;
        intent.callIndex = callIndex;
        intent.callMode = RtcCallMode.CallMode_AudioVideo;
        intent.peerUri = new RtcUri("NaN");
        offer(intent);
        callActivityListener.onStartCallActivity(null);
    }


    private Queue<RtcCallIntent> incomingQueue = new LinkedBlockingQueue<>();

    private void offer(RtcCallIntent intent) {
        if (!RtcCallIntent.Direction.OUTGOING.equals(intent.direction)) {
            incomingQueue.offer(intent);
        }
    }

    /**
     * 清理待接听队列，防止被重复处理。
     * 需要在呼入的callIndex被接通/挂断时调用
     *
     * @param callIndex 需要清理的callIndex
     */
    public synchronized void remove(int callIndex) {
        for (Iterator<RtcCallIntent> it = incomingQueue.iterator(); it.hasNext(); ) {
            if (it.next().callIndex == callIndex) {
                it.remove();
            }
        }
    }

    /**
     * 获取并移除呼入队列顶部的intent
     *
     * @return 无呼入intent时返回null
     */
    public synchronized RtcCallIntent poll() {
        return incomingQueue.poll();
    }

    /**
     * 获取呼入队列顶部的intent
     *
     * @return 无呼入intent时返回null
     */
    public synchronized RtcCallIntent peek() {
        return incomingQueue.peek();
    }

    /**
     * CallActivity启动时机监听器
     */
    public interface CallActivityListener {
        void onStartCallActivity(RtcCallIntent intent);
    }

    private CallActivityListener callActivityListener;

    public void setCallActivityListener(CallActivityListener listener) {
        this.callActivityListener = listener;
    }

    private int callUIShowing = 0;

    public synchronized void callUIEnter() {
        callUIShowing++;
    }

    public synchronized void callUIExit() {
        callUIShowing--;
        if (callUIShowing < 0) {
            callUIShowing = 0;
        }
        if (callUIShowing == 0 && !incomingQueue.isEmpty() && callActivityListener != null) {
            LOGGER.info("callUIExit: onStartCallActivity");
            callActivityListener.onStartCallActivity(null);
        }
    }
}
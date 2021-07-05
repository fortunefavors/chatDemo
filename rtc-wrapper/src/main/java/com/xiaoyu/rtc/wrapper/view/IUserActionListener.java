package com.xiaoyu.rtc.wrapper.view;

import com.xiaoyu.open.RtcUri;
import com.xiaoyu.open.call.RtcCallMode;
import com.xiaoyu.open.video.RtcConfereeLayout;

/**
 * 通话事件处理器
 */
interface IUserActionListener {
    /**
     * 切换通话模式
     *
     * @return 返回切换后的通话模式 {@link RtcCallMode}
     */
    RtcCallMode switchCallMode();

    /**
     * 切换静音模式
     *
     * @return 静音返回true，否则返回false
     */
    boolean switchAudioMute();

    /**
     * 恢复音频播放
     */
    void recoverAudio();

    /**
     * 切换摄像头
     *
     * @param tag 按钮前一个状态是否选择
     */
    int switchCamera(boolean tag);

    /**
     * 接听通话
     *
     * @param callIndex 会话ID
     * @param callMode  通话模式 {@link RtcCallMode}
     * @param more
     */
    void answerCall(int callIndex, RtcCallMode callMode, boolean more);

    /**
     * 挂断通话
     */
    void dropCall();

    /**
     * 拒接来电
     *
     * @param callIndex 需要拒接的来电标识
     * @param reason    拒接的业务原因
     */
    void rejectIncomingCall(int callIndex, String reason);

    /**
     * 点击邀请按钮
     */
    void onAddConferee();

    /**
     * 取消邀请指定uri
     */
    void cancelConferee(RtcUri uri);

    /**
     * 转呼PSTN
     */
    void forwardPSTN();

    /**
     * 发送提醒事件
     */
    void sendBuzzer();

    /**
     * 回家看看转通话
     */
    void forwardCall();

    /**
     * 切换为扬声器模式/听筒模式
     *
     * @param on 设置为null，将取消限制，跟随系统行为自动切换
     */
    void setSpeakerphoneOn(Boolean on);

    /**
     * 当手动切换某个窗口为全屏时会回调
     *
     * @param layoutInfoId 全屏窗口{@link RtcConfereeLayout#id}
     */
    void onForceTargetLayout(int layoutInfoId);

    /**
     * 开始通话中录制
     */
    void startRecording();

    /**
     * 停止通话中录制
     */
    void stopRecording();


    boolean switchVideoMute();
}

package com.xiaoyu.rtc.wrapper.view;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.xiaoyu.open.RtcContext;
import com.xiaoyu.open.RtcContextCache;
import com.xiaoyu.open.RtcGlobalConfig;
import com.xiaoyu.open.RtcUri;
import com.xiaoyu.open.audio.RtcAudioOutputListener;
import com.xiaoyu.open.audio.RtcAudioService;
import com.xiaoyu.open.call.RtcCallIntent;
import com.xiaoyu.open.call.RtcCallMode;
import com.xiaoyu.open.call.RtcCallService;
import com.xiaoyu.open.call.RtcCallStateInfo;
import com.xiaoyu.open.call.RtcCallStatusAdapter;
import com.xiaoyu.open.call.RtcConferee;
import com.xiaoyu.open.call.RtcConfereeState;
import com.xiaoyu.open.call.RtcMakeCallResult;
import com.xiaoyu.open.call.RtcReason;
import com.xiaoyu.open.net.RtcNetworkType;
import com.xiaoyu.open.uri.RtcConferenceUri;
import com.xiaoyu.open.video.RtcConfereeLayout;
import com.xiaoyu.open.video.RtcConfereeLayouts;
import com.xiaoyu.open.video.RtcVideoService;
import com.xiaoyu.rtc.wrapper.Member;
import com.xiaoyu.rtc.wrapper.R;
import com.xiaoyu.rtc.wrapper.RtcIncomingManager;
import com.xiaoyu.rtc.wrapper.ToastUtil;
import com.xiaoyu.ui.CallDebugView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static com.xiaoyu.open.call.RtcCallMode.CallMode_Observer;
import static com.xiaoyu.open.call.RtcCallMode.CallMode_Tel;
import static com.xiaoyu.rtc.wrapper.view.ConfereeViewGroup.LayoutStatus.LOCAL;
import static com.xiaoyu.rtc.wrapper.view.ConfereeViewGroup.LayoutStatus.OBSERVER;
import static com.xiaoyu.rtc.wrapper.view.ConfereeViewGroup.LayoutStatus.TEL;

/**
 * 通话界面视图控制器
 */
public class CallViewController extends RtcCallStatusAdapter implements View.OnClickListener, RtcAudioOutputListener {
    private final static Logger LOGGER = Logger.getLogger("CallViewController");
    protected Context context;
    private ViewGroup contentView;
    /**
     * 与会者窗口布局组合（包含本地预览）
     */
    private ConfereeViewGroup confereeViewGroup;
    /**
     * 呼出中功能按钮视图
     */
    private CallOutgoingView outgoingView;
    /**
     * 呼入中功能按钮视图
     */
    private CallIncomingView incomingView;
    /**
     * 通话中收到呼入功能按钮视图
     */
    private CallIncomingMoreView moreIncomingView;
    /**
     * 通话中功能按钮视图
     */
    private CallToolbarView toolbarOverlay;
    /**
     * Rtc音频服务
     */
    private RtcAudioService audioService;
    /**
     * Rtc视频服务
     */
    private RtcVideoService videoService;
    /**
     * Rtc通话服务
     */
    private RtcCallService callService;
    /**
     * CallView状态监听器
     */
    private CallViewListener callViewListener;

    /**
     * CallView状态监听器
     */
    public interface CallViewListener {
        /**
         * 通话结束，应该销毁Activity
         */
        void onCallFinished();
    }

    private void onCallFinished() {
        //尽可能早的释放视频显示组件，否者设备上可能出现本地预览画面黑屏的问题
        if (confereeViewGroup != null) {
            confereeViewGroup.onDestroy();
        }
        if (callViewListener != null) {
            callViewListener.onCallFinished();
        }
    }

    /**
     * Debug功能 --  工作方式（摄像头切换逻辑）
     */
    private boolean workAtApp;

    /**
     * 本地预览窗口监听器，特殊设备可以通过此监听器回调获得TextureView，用于和Camera绑定显示本地画面
     */
    private ConfereeViewGroup.LocalPreviewListener localPreviewListener;

    public CallViewController(Context context, ViewGroup contentView) {
        RtcContext rtcContext = RtcContextCache.get();
        this.context = context;
        this.contentView = contentView;
        this.callService = rtcContext.getCallService();
        this.audioService = rtcContext.getAudioService();
        this.videoService = rtcContext.getVideoService();
        if (context instanceof CallViewListener) {
            this.callViewListener = (CallViewListener) context;
        }
        this.audioService.setAudioOutputListener(this);
        this.callService.addCallStatusListener(this);
    }

    private boolean isLandscape;

    public void updatePreviewOrientation(int angle) {
        this.videoService.updatePreviewOrientation(angle);
        isLandscape = angle % 2 == 0;
        if (confereeViewGroup != null) {
            confereeViewGroup.updatePreviewOrientation(isLandscape);
        }
    }

    public void updateActivityScreenOrientation(int screenOrientation) {
        this.videoService.updateActivityScreenOrientation(screenOrientation);
        isLandscape = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE == screenOrientation || ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE == screenOrientation;
        if (confereeViewGroup != null) {
            confereeViewGroup.updatePreviewOrientation(isLandscape);
        }
    }

    public CallViewController(Context context, ViewGroup contentView, ConfereeViewGroup.LocalPreviewListener localPreviewListener) {
        this(context, contentView);
        this.localPreviewListener = localPreviewListener;
    }

    public void setWorkAtApp(boolean atApp) {
        this.workAtApp = atApp;
    }

    public void onDestroy() {
        callService.sendCameraEvent(true);
        audioService.setAudioOutputListener(null);//避免内存泄漏
        callService.removeCallStatusListener(this);//避免内存泄露
        if (contentView != null) {
            if (incomingView != null) {
                contentView.removeView(incomingView);
                incomingView = null;
            }

            if (outgoingView != null) {
                contentView.removeView(outgoingView);
                outgoingView = null;
            }

            if (toolbarOverlay != null) {
                contentView.removeView(toolbarOverlay);
                toolbarOverlay = null;
            }
        }

        LOGGER.info("onDestroy");
    }

    private boolean autoAnswer;

    /**
     * Debug功能 -- 自动接听
     */
    public void setAutoAnswer(boolean autoAnswer) {
        this.autoAnswer = autoAnswer;
    }

    private String userInfo;

    /**
     * Debug功能 --  用户信息
     */
    public void setUserInfo(String reason) {
        this.userInfo = reason;
    }

    private boolean replaceCurrentCall;

    /**
     * Debug功能 --  替换当前通话
     */
    public void setReplaceCurrentCall(boolean replaceCurrentCall) {
        this.replaceCurrentCall = replaceCurrentCall;
    }

    private boolean forceCallOut;

    /**
     * Debug功能 --  强制呼出
     */
    public void setForceCallOut(boolean forceCallOut) {
        this.forceCallOut = forceCallOut;
    }

    private String buzzerFilePath;

    public void setBuzzerFilePath(String filePath) {
        buzzerFilePath = filePath;
    }

    private RtcCallIntent mIntent;

    /**
     * 设置初始化通话界面信息
     */
    public void setCallIntent(RtcCallIntent intent) {
        if (intent == null) {
            onCallFinished();
            return;
        }

        mIntent = intent;
        createConfereeViewGroup();//创建与会者窗口管理视图
        setCallMode(intent.callMode);//记录通话模式
        if (RtcCallIntent.Direction.INCOMING.equals(intent.direction)) {
            if (autoAnswer) { //自动接听
                if (callMode == RtcCallMode.CallMode_AudioOnly) {
                    userActionListener.answerCall(intent.callIndex, RtcCallMode.CallMode_AudioOnly, false);
                } else {
                    userActionListener.answerCall(intent.callIndex, RtcCallMode.CallMode_AudioVideo, false);
                }
            } else {//初始化呼入中视图
                if (contentView != null && incomingView == null) {
                    incomingView = createCallIncomingView(intent);
                    contentView.addView(incomingView);
                }
            }
        } else if (RtcCallIntent.Direction.OUTGOING.equals(intent.direction)) {
            //初始化呼出中视图
            if (contentView != null && outgoingView == null) {
                outgoingView = createCallOutgoingView(intent);
                contentView.addView(outgoingView);
            }
            intent.force = forceCallOut;//设置强制呼出标记
            RtcMakeCallResult result = callService.makeCall(intent);//发起呼叫
            LOGGER.info("makeCallResult: " + result);
            if (!result.success) {
                ToastUtil.showText(RtcReason.getStringRes(result.reason), 3000);
                onCallFinished();
            } else {
                //makeCall流程 callIndex在Rtc受理呼出请求后才分配，所以这里需要更新
                mIntent.callIndex = result.callIndex;

                // 如果是云会议就静音
                if (intent.peerUri instanceof RtcConferenceUri) {
                    isAudioMute = true;
                    callService.muteAudio(true);
                }
            }
        } else if (RtcCallIntent.Direction.CONNECTED.equals(intent.direction)) {
            callConnected(intent.callIndex);//被回家看看，转为通话时走到此处
        } else {
            onCallFinished(); //无法识别的Direction
        }
    }

    private boolean connected = false;
    private boolean connecting = false;

    /**
     * 通话接通， View切换
     */
    private synchronized void callConnected(int callIndex) {
        RtcIncomingManager.INSTANCE.remove(callIndex);//清理待接听队列
        if (connected) {
            return;
        }
        connected = true;
        connecting = false;
        if (contentView != null && outgoingView != null) {
            contentView.removeView(outgoingView);//回收呼出视图
            outgoingView = null;
        }

        if (contentView != null && incomingView != null) {
            contentView.removeView(incomingView);//回收呼入视图
            incomingView = null;
        }

        if (contentView != null && toolbarOverlay == null) {
            toolbarOverlay = createCallToolbarView(callMode);//初始化通话中功能按钮试图
            contentView.addView(toolbarOverlay);
        }

        if (RtcGlobalConfig.isDebug) {//DEBUG功能展示，不推荐似乎用
            if (contentView != null) {
                contentView.addView(new CallDebugView(context));
            }
        }

        //填充可用于邀请的被叫信息列表
        createAddConfereeMemberViews();
        //尝试处理更多的呼入请求
        onMoreIncomingCall();

        if (isAudioMute) {
            LOGGER.info("callConnected mute");
            if (confereeViewGroup != null) {
                confereeViewGroup.updateLocalMicMute(true);
            }

            if (toolbarOverlay != null) {
                toolbarOverlay.updateAudioMutedBtn(true);
            }
        }
    }

    private RtcCallMode callMode = RtcCallMode.CallMode_AudioVideo;
    private boolean isAudioMute = false;

    private void setCallMode(RtcCallMode callMode) {
        this.callMode = callMode;

        if (confereeViewGroup != null) {
            confereeViewGroup.changeLocalCallMode(callMode);
        }
    }

    private RtcCallIntent mMoreIntent;

    /**
     * 尝试处理更多的呼入请求
     */
    public void onMoreIncomingCall() {
        if (mMoreIntent != null) {
            ToastUtil.showText("padding onMoreIncomingCall");
            LOGGER.info("padding onMoreIncomingCall");
            return;
        }
        //表示有更多人的呼入，后续的逻辑需要循环处理，直到RtcIntentQueue.poll()返回null为止
        //TODO 这里需要后续细化
        if (connected) {
            mMoreIntent = RtcIncomingManager.INSTANCE.poll();
            LOGGER.info("onMoreIncomingCall: mMoreIntent=" + mMoreIntent);
            if (mMoreIntent != null) {
                if (RtcCallIntent.Direction.CONNECTED.equals(mMoreIntent.direction)) {
                    mMoreIntent = null;
                    return;//双方通话接通后会进入此处
                }
                //正常通话中，收到第三方呼入会进入到此处
                if (autoAnswer) {
                    userActionListener.answerCall(mMoreIntent.callIndex, mMoreIntent.callMode, true);
                } else {
                    moreIncomingView = createCallMoreIncomingView(mMoreIntent);
                    contentView.addView(moreIncomingView);
                }
            }
        } else {
            RtcCallIntent intent = RtcIncomingManager.INSTANCE.peek();
            LOGGER.info("onMoreIncomingCall: mIntent=" + intent);
            //1. 被回家看看中，有其他呼入并接通时会运行到此处
            //2. 呼入等待接听过程中，有回家看看被自动接听后会运行到此处
            if (intent != null && RtcCallIntent.Direction.CONNECTED.equals(intent.direction)) {
                mIntent = intent;//此时需要更新intent信息, 否则后续无法挂断通话
                callConnected(intent.callIndex);
            } else if (connecting) {
                LOGGER.info("wait base call connected");
            } else {
                mMoreIntent = intent;
                if (mMoreIntent == null) {
                    return;
                }
                if (autoAnswer) {
                    userActionListener.answerCall(mMoreIntent.callIndex, mMoreIntent.callMode, true);
                } else {
                    moreIncomingView = createCallMoreIncomingView(mMoreIntent);
                    contentView.addView(moreIncomingView);
                }
            }
        }
    }

    private void dismissMoreIncomingView(int callIndex) {
        if (mMoreIntent != null && mMoreIntent.callIndex == callIndex) {
            if (contentView != null && moreIncomingView != null) {
                contentView.removeView(moreIncomingView);
                moreIncomingView = null;
            }
            mMoreIntent = null;
            onMoreIncomingCall();//尝试切换下一个
        }
    }

    @Override
    public void onCallStatusChanged(RtcCallStateInfo status) {
        LOGGER.info("onCallStatusChanged: " + status.state + ", callIndex:" + status.callIndex + " = " + mIntent.callIndex + ", reason=" + status.reason + ", userInfo=" + status.userInfo);
        if (callStateChangedListener != null) {
            callStateChangedListener.onCallStateChanged(status);
        }
        switch (status.state) {
            case CALL_STATE_CONNECTED: {
                if (status.callIndex == mIntent.callIndex) {//正常P2P呼叫接通时走到此处
                    connecting = false;
                    callConnected(status.callIndex);
                } else {
                    //呼入等待接听过程中，有回家看看被自动接听时会运行到此处
                    LOGGER.warning("onCallStatusChanged: not intent.index " + mIntent.callIndex + " connected");
                }
                break;
            }
            case CALL_STATE_DISCONNECTED: {
                //因为此处的逻辑，需要特别小心维护mIntent.callIndex关系
                if (status.callIndex == mIntent.callIndex) {
                    connecting = false;
                    connected = false; //防止销毁过程中，onConfereeLayoutChanged回调导致crash
                    ToastUtil.showText(RtcReason.getStringRes(status.reason), 3000);
                    onCallFinished();
                } else {
                    dismissMoreIncomingView(status.callIndex);
                }
                break;
            }
        }
    }

    @Override
    public void onAddConfereeResult(RtcConfereeState result) {
        LOGGER.info("onAddConfereeResult: " + result);
        if (confereeViewGroup != null) {
            confereeViewGroup.onAddConfereeResult(result);
        }
        ToastUtil.showText(result.toString());
    }

    /**
     * 记录当前大窗口Layout信息
     */
    private RtcConfereeLayout forceTargetLayout;
    private List<RtcConfereeLayout> lastLayout = Collections.emptyList();

    @Override
    public void onConfereeLayoutChanged(RtcConfereeLayouts layouts) {
        if (!connected) {
            return;//呼入中，回家看看被自动接听时会运行到此处
        }
        LOGGER.info("handleLayoutChanged: " + layouts);
        if (layouts.allIsObserver) {
            onCallFinished();
            return;
        }
        lastLayout = layouts.layouts;
        if (confereeViewGroup != null) {
            // 本地PSTN呼叫时，界面上可能有些不同
            if (!lastLayout.isEmpty()) {
                confereeViewGroup.setLayoutStatus(CallMode_Observer.equals(callMode) ? OBSERVER : CallMode_Tel.equals(callMode) ? TEL : LOCAL);
                //由于RtcCallService.answerCall 接口callMode参数的定义限制，这里需要业务修正切换通话模式；
                //if (layouts.allIsTelephone) {
                //从音视频模式切换回PSTN的时候需要通知SDK更改模式，以节约Video输入的消耗
                //callService.changeCallMode(CallMode_Tel);
                //}
            }
            confereeViewGroup.updateConfereeLayouts(lastLayout);
        }
        if (layouts.layouts == null || layouts.layouts.isEmpty()) {
            forceTargetLayout = null;
        } else {
            forceTargetLayout = layouts.layouts.get(0);
        }
    }

    @Override
    public void onParticipantsChanged(List<RtcUri> uris) {
        LOGGER.info("onParticipantsChanged: " + uris);
    }

    @Override
    public void onVideoStatusChange(VideoStatus status) {
        ToastUtil.showText("onVideoStatusChange: " + status);
    }

    @Override
    public void onCallReplacedEvent(CallReplaced info) {
        ToastUtil.showText("onCallReplacedEvent");
        if (mIntent != null) {
            mIntent.callIndex = info.callIndex;
        }
        callConnected(info.callIndex);
        /*
        if (RtcCallMode.CallMode_Observed.equals(info.callMode)) {
            onCallFinished();
        }
         */
    }

    @Override
    public void onBuzzerEvent(RtcUri uri) {
        ToastUtil.showText("onBuzzerEvent: " + uri.getUid());
        callService.startPlayingSound(buzzerFilePath);
    }

    @Override
    public void onForwardPSTNAvailableEvent() {
        ToastUtil.showText("onForwardPSTNAvailableEvent");
    }

    @Override
    public void onPSTNForwardedEvent(PSTNInfo info) {
        ToastUtil.showText("onPSTNForwardedEvent: " + info);
    }

    @Override
    public void onCustomerServiceEvent(CustomerServiceInfo info) {
        ToastUtil.showText("onCustomerServiceEvent: " + info);
    }

    @Override
    public void onConfereesLimitEvent(ConfereesLimit limit) {
        ToastUtil.showText("onConfereesLimitEvent: " + limit);
    }

    @Override
    public void onHowlingEvent(boolean howling) {
        ToastUtil.showText("onHowlingEvent: " + howling);
    }

    private void createConfereeViewGroup() {
        if (contentView != null) {
            confereeViewGroup = new ConfereeViewGroup(context, localPreviewListener);
            confereeViewGroup.updatePreviewOrientation(isLandscape);
            confereeViewGroup.setUserActionListener(userActionListener);
            confereeViewGroup.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            confereeViewGroup.setLayoutStatus(CallMode_Observer.equals(mIntent.callMode) ? OBSERVER : CallMode_Tel.equals(mIntent.callMode) ? TEL : LOCAL);
            confereeViewGroup.changeLocalCallMode(callMode);
            contentView.addView(confereeViewGroup);
        }
    }

    private CallOutgoingView createCallOutgoingView(RtcCallIntent intent) {
        return new CallOutgoingView(context, intent, userActionListener);
    }

    private CallIncomingView createCallIncomingView(RtcCallIntent intent) {
        return new CallIncomingView(context, intent, userActionListener);
    }

    private CallToolbarView createCallToolbarView(RtcCallMode callMode) {
        CallToolbarView view = new CallToolbarView(context, callMode, userActionListener);
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return view;
    }

    private CallIncomingMoreView createCallMoreIncomingView(RtcCallIntent intent) {
        CallIncomingMoreView moreIncomingView = new CallIncomingMoreView(context, intent, userActionListener);
        moreIncomingView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return moreIncomingView;
    }

    //----------------------------- IUserActionListener --------------------------
    private IUserActionListener userActionListener = new IUserActionListener() {
        @Override
        public void answerCall(int callIndex, RtcCallMode callMode, boolean more) {
            if (more) {
                if (!connected && mMoreIntent != null) {
                    mIntent = mMoreIntent;//原来的呼出未接通，听贴新呼入需要更新mIntent，否正界面会消失
                }
                RtcIncomingManager.INSTANCE.remove(callIndex);
            }
            dismissMoreIncomingView(callIndex);
            setCallMode(callMode);
            if (mIntent.callIndex == callIndex) {
                connecting = true;
            }
            callService.answerCall(callIndex, replaceCurrentCall, callMode);

            // 如果静音了复位
            if (isAudioMute && mIntent.peerUri instanceof RtcConferenceUri) {
                LOGGER.info("reset mute");

                isAudioMute = false;
                callService.muteAudio(false);

                if (confereeViewGroup != null) {
                    confereeViewGroup.updateLocalMicMute(false);
                }

                if (toolbarOverlay != null) {
                    toolbarOverlay.updateAudioMutedBtn(false);
                }
            }
        }

        @Override
        public int switchCamera(boolean tag) {
            if (workAtApp) {
                videoService.switchCamera();
                return R.string.switch_camera;
            }
            if (tag) {
                videoService.releaseCamera();
                if (confereeViewGroup != null) {
                    confereeViewGroup.changeLocalCameraStatus(false);
                }
                return R.string.open_camera;
            } else {
                videoService.requestCamera();
                if (confereeViewGroup != null) {
                    confereeViewGroup.changeLocalCameraStatus(true);
                }
                return R.string.close_camera;
            }
        }

        @Override
        public RtcCallMode switchCallMode() {
            RtcCallMode _callMode = callMode == RtcCallMode.CallMode_AudioVideo ? RtcCallMode.CallMode_AudioOnly : RtcCallMode.CallMode_AudioVideo;
            setCallMode(_callMode);
            callService.changeCallMode(_callMode);
            return _callMode;
        }

        @Override
        public void dropCall() {
            callService.dropCall();
            if (callViewListener != null) {
                callViewListener.onCallFinished();
            }
        }

        @Override
        public void rejectIncomingCall(int callIndex, String reason) {
            dismissMoreIncomingView(callIndex);
            callService.rejectIncomingCall(callIndex, userInfo);
        }

        @Override
        public boolean switchAudioMute() {
            isAudioMute = !isAudioMute;

            if (confereeViewGroup != null) {
                confereeViewGroup.updateLocalMicMute(isAudioMute);
            }
            callService.muteAudio(isAudioMute);
            return isAudioMute;
        }

        private boolean isVideoMute = false;

        @Override
        public boolean switchVideoMute() {
            isVideoMute = !isVideoMute;
            callService.muteVideo(isVideoMute);
            return isVideoMute;
        }

        @Override
        public void recoverAudio() {
            audioService.requestAudioFocus();
        }

        @Override
        public void onAddConferee() {
            setAddConfereeViewVisibility(View.VISIBLE);
        }

        @Override
        public void cancelConferee(RtcUri uri) {
            callService.cancelConferee(uri);
        }

        @Override
        public void forwardPSTN() {
            callService.forwardPSTN();
            if (confereeViewGroup != null) {
                confereeViewGroup.setLayoutStatus(TEL);
                confereeViewGroup.changeLocalCallMode(CallMode_Tel);
            }
        }

        @Override
        public void forwardCall() {
            if (mIntent == null) {
                return;
            }
            setCallMode(RtcCallMode.CallMode_AudioVideo);
            if (confereeViewGroup != null) {
                confereeViewGroup.setLayoutStatus(LOCAL);
                confereeViewGroup.updateConfereeLayouts(lastLayout);
            }
            callService.changeCallMode(RtcCallMode.CallMode_AudioVideo);
        }

        @Override
        public void sendBuzzer() {
            if (forceTargetLayout == null) {
                ToastUtil.showText("振铃对象还未出现...");
                return;
            }
            callService.sendBuzzerEvent(forceTargetLayout.peerUri);
        }


        @Override
        public void startRecording() {
            if (recording != null) {
                recording.startRecording();
            }
        }

        @Override
        public void stopRecording() {
            if (recording != null) {
                recording.stopRecording();
            }
        }

        @Override
        public void onForceTargetLayout(int layoutInfoId) {
            callService.forceTargetLayout(layoutInfoId);
        }

        @Override
        public void setSpeakerphoneOn(Boolean on) {
            audioService.setSpeakerphoneOn(on);
        }
    };

    public interface Recording {
        void startRecording();

        void stopRecording();
    }

    private Recording recording;

    public void setRecording(Recording recording) {
        this.recording = recording;
    }

    private List<Member> members = new ArrayList<>();

    public void setAddCalleeMembers(List<Member> members) {
        this.members.clear();
        if (members != null) {
            this.members.addAll(members);
        }
    }

    private LinearLayout addCalleeView;

    /**
     * Debug功能 -- 待邀请成员列表视图
     */
    private void createAddConfereeMemberViews() {
        if (contentView == null || members.isEmpty()) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(context);
        addCalleeView = new LinearLayout(context);
        addCalleeView.setOrientation(LinearLayout.HORIZONTAL);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
        addCalleeView.setLayoutParams(layoutParams);
        addCalleeView.setVisibility(View.INVISIBLE);
        for (Member member : members) {
            if (member == null) {
                continue;
            }
            View child = inflater.inflate(R.layout.debug_add_member, addCalleeView, false);
            child.setId(R.id.custom_id_0);
            child.setTag(member);
            child.setOnClickListener(this);
            ((TextView) child.findViewById(R.id.callee_nick)).setText(member.nick);
            ((TextView) child.findViewById(R.id.callee_uri)).setText(member.uri);
            LOGGER.info("createAddConfereeMemberViews: " + member.nick + " " + member.uri);
            addCalleeView.addView(child);
        }
        contentView.addView(addCalleeView);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.custom_id_0) {
            setAddConfereeViewVisibility(View.INVISIBLE);
            Member member = (Member) v.getTag();
            if (callService != null) {
                RtcUri uri = new RtcUri(member.uri);
                if (confereeViewGroup != null) {
                    RtcConfereeLayout layout = new RtcConfereeLayout();
                    layout.layoutState = RtcConfereeLayout.LayoutState.kLayoutStateAddConferee;
                    layout.videoMuteReason = RtcConfereeLayout.VideoMuteReason.MuteByUser;
                    layout.peerUri = uri;
                    layout.peerName = member.nick;
                    confereeViewGroup.addConferee(layout);
                }
                RtcConferee conferee = RtcConferee.createForAddConferee(uri);
                if (autoPstn) {
                    conferee.autoPSTN(member.no);
                }
                conferee.enablePush(offlinePush);
                callService.addConferee(conferee);
            }
        }
    }

    private boolean autoPstn;

    public void setAutoPstn(boolean autoPstn) {
        this.autoPstn = autoPstn;
    }

    private boolean offlinePush;

    public void setOfflinePush(boolean offlinePush) {
        this.offlinePush = offlinePush;
    }

    private void setAddConfereeViewVisibility(int visibility) {
        if (addCalleeView == null) {
            return;
        }
        addCalleeView.setVisibility(visibility);
    }


    /**
     * 通话状态监听器，用于某些特殊业务需求
     */
    public interface CallStateChangedListener {
        /**
         * 在通话结束时回调
         */
        void onCallStateChanged(RtcCallStateInfo status);
    }

    private CallStateChangedListener callStateChangedListener;

    public void setCallStateChangedListener(CallStateChangedListener listener) {
        callStateChangedListener = listener;
    }

    @Override
    public void onOutputDeviceChanged(OutputDevice device) {
        if (toolbarOverlay != null)
            toolbarOverlay.updateOutputDevice(device);
    }

    @Override
    public void onNetworkTypeChanged(RtcNetworkType type) {
        LOGGER.info("onNetworkTypeChanged：" + type);
    }

    @Override
    public void onOutputFocusChanged(OutputFocus focus) {
        LOGGER.info("onOutputFocusChanged: " + focus);
        if (toolbarOverlay != null) {
            toolbarOverlay.onAudioFocusChanged(focus);
        }
    }
}
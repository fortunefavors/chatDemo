package com.xiaoyu.rtc.wrapper.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.OvershootInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.xiaoyu.open.RtcUri;
import com.xiaoyu.open.call.RtcConfereeState;
import com.xiaoyu.open.call.RtcReason;
import com.xiaoyu.open.video.RtcConfereeLayout;
import com.xiaoyu.open.video.RtcVideoView;
import com.xiaoyu.rtc.wrapper.R;

import java.util.Objects;
import java.util.logging.Logger;


/**
 * 通用(软解)与会者窗口布局封装，包含视频显示以及窗口状态提示等逻辑
 */
@SuppressLint("ViewConstructor")
public class ConfereeView extends ViewGroup {
    private static final Logger LOGGER = Logger.getLogger("ConfereeView");
    private static final int LAYOUT_ANIMATION_DURATION = 800;
    public static final int LOCAL_VIEW_ID = Short.MIN_VALUE;
    private ImageView mBackgroundHead;
    private RtcVideoView mVideoView;
    private View mLoadingView;
    private View mLoadingViewMini;
    private TextView mUserName;
    private TextView mStatusView;
    private TextView mStatusView2;
    private ImageView mMicIcon;
    private ImageView mMicIconBackground;
    private CellRectView mCellRect;
    private ImageView mCancelAddConferee;
    /**
     * 是否为本地窗口
     */
    public boolean isLocal;
    private LayoutPosition mPosition;
    private CellEventListener mCellEventListener;
    private GestureDetector mGestureDetector;
    private SurfaceGestureListener mGestureListener;
    private int mLastDragX, mLastDragY;
    /**
     * 窗口唯一标识
     */
    public final String viewTag;
    /**
     * 窗口缓存布局数据
     */
    public RtcConfereeLayout confereeLayout;
    private Resources mResources;
    private TextView muteReason;

    public ConfereeView(Context context, String viewTag, CellEventListener cellEventListener, boolean playCreateAnimation, boolean isLocal) {
        this(context, viewTag, cellEventListener, playCreateAnimation, isLocal, null);
    }

    /**
     * 本地窗口初始化监听器，特殊的工作模式会使用到
     */
    private ConfereeViewGroup.LocalPreviewListener mLocalPreviewListener;

    public ConfereeView(Context context, String viewTag, CellEventListener cellEventListener, boolean playCreateAnimation, ConfereeViewGroup.LocalPreviewListener localPreviewListener) {
        this(context, viewTag, cellEventListener, playCreateAnimation, true, localPreviewListener);
    }

    private ConfereeView(Context context, String viewTag, CellEventListener cellEventListener, boolean playCreateAnimation, boolean isLocal, ConfereeViewGroup.LocalPreviewListener localPreviewListener) {
        super(context);
        this.viewTag = viewTag;
        this.mCellEventListener = cellEventListener;
        this.isLocal = isLocal;
        this.mGestureListener = new SurfaceGestureListener();
        this.mResources = context.getResources();
        this.mLocalPreviewListener = localPreviewListener;
        loadView(context, playCreateAnimation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfereeView videoView = (ConfereeView) o;
        return viewTag.equals(videoView.viewTag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(viewTag);
    }

    private float mDensity;
    /**
     * 全屏/小窗口判断阈值
     */
    private int mWidthThreshold;

    private void loadView(Context context, boolean playCreateAnimation) {
        inflate(context, R.layout.conferee_view, this);
        DisplayMetrics metrics = mResources.getDisplayMetrics();
        mDensity = metrics.density;
        mVideoView = findViewById(R.id.rtc_video_view);
        if (mLocalPreviewListener != null) {
            mVideoView.enableCustomRenderMode();
        }
        mVideoView.forceFullScreen();
        mBackgroundHead = findViewById(R.id.background_head);
        mLoadingView = findViewById(R.id.loading_view_big);
        mLoadingViewMini = findViewById(R.id.loading_view_mini);
        mUserName = findViewById(R.id.user_name);
        mStatusView = findViewById(R.id.status_view);
        mStatusView2 = findViewById(R.id.status_view2);
        mMicIcon = findViewById(R.id.mic_icon);
        mMicIconBackground = findViewById(R.id.mic_icon_background);
        mCellRect = findViewById(R.id.cell_rect);
        mCancelAddConferee = findViewById(R.id.cancel_add_conferee);
        muteReason = findViewById(R.id.mute_reason);
        mCancelAddConferee.setOnClickListener(view -> {
            if (mCellEventListener != null) {
                mCellEventListener.onCancelAddConferee(confereeLayout.peerUri);
            }
        });
        mBackgroundHead.bringToFront();

        mGestureDetector = new GestureDetector(getContext(), mGestureListener);
        mGestureDetector.setIsLongpressEnabled(true);
        mGestureDetector.setOnDoubleTapListener(mGestureListener);

        if (playCreateAnimation) {
            TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, -1.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, -1.0f, Animation.RELATIVE_TO_PARENT, 0.0f);
            animation.setDuration(LAYOUT_ANIMATION_DURATION);
            startAnimation(animation);
        }

        setVisibility(muteReason, View.VISIBLE);
    }

    public void updatePreviewOrientation(boolean isLandscape) {
        DisplayMetrics metrics = mResources.getDisplayMetrics();
        if (isLandscape) {
            if (metrics.widthPixels > metrics.heightPixels) {
                mWidthThreshold = metrics.widthPixels / 3;
            } else {
                mWidthThreshold = metrics.heightPixels / 3;
            }
        } else {
            if (metrics.widthPixels > metrics.heightPixels) {
                mWidthThreshold = metrics.widthPixels / 3;
            } else {
                mWidthThreshold = metrics.widthPixels / 2;
            }
        }
        LOGGER.info("updatePreviewOrientation: mWidthThreshold=" + mWidthThreshold +
                ", width=" + metrics.widthPixels +
                ", height=" + metrics.heightPixels +
                ", isLandscape=" + isLandscape
        );
    }

    /**
     * 比较conferee是否相同
     */
    public boolean equalsConfereeLayout(RtcConfereeLayout confereeLayout) {
        return this.confereeLayout != null && this.confereeLayout.equals(confereeLayout);
    }

    /**
     * 邀请第三方结果通知
     */
    public boolean onAddConfereeResult(RtcConfereeState result) {
        boolean match = confereeLayout != null && confereeLayout.peerUri.equals(result.peerUri);
        if (match) {
            if (result.success) {
                mCellEventListener.onExitAnimationEnd(this);
                return false;
            }
            //TODO 这里实际还要考虑result.userInfo
            if (RtcReason.CANCEL.equals(result.reason)) {
                setText(mStatusView, R.string.call_pending_cancel);
            } else {
                setText(mStatusView, R.string.call_pending_fail);
            }
            layoutSelf();
            return true;
        }
        return false;
    }

    /**
     * 设置/更新与会者布局信息
     */
    public void setConfereeLayout(RtcConfereeLayout confereeLayout) {
        this.confereeLayout = confereeLayout;
        muteReason.setText(confereeLayout.videoMuteReason + "");
        bindVideoView(this.confereeLayout);
        updateLayoutViewState(this.confereeLayout);
    }

    /**
     * 将布局信息绑定到视频显示组件
     */
    protected void bindVideoView(RtcConfereeLayout info) {
        mVideoView.setConfereeLayout(info);
        if (isLocal && mLocalPreviewListener != null) {
            mVideoView.setVisibility(View.VISIBLE);
            mLocalPreviewListener.onPostInit(mVideoView.getTextureView());
        }
    }

    /**
     * 窗口状态提示信息更新
     */
    private void updateLayoutViewState(RtcConfereeLayout info) {
        //各种状态检查
        updateLayoutState(info);
        //语音模式的优先级高于摄像头关闭
        if (isNoVideo) {
            //背景设置
            if (isCameraDisable && !(isAudioOnlyLocal || isAudioOnly)) {

            } else if (isPstn) {

            } else {

            }
            setVisibility(mVideoView, INVISIBLE);
        } else {
            setVisibility(mVideoView, VISIBLE);
        }

        //注意bringToFront的顺序,会发生覆盖
        if (isNoVideo) {
            setVisibility(mBackgroundHead, VISIBLE);
            mBackgroundHead.bringToFront();

            if (!isRequesting) {
                setVisibility(mUserName, VISIBLE);
                mUserName.bringToFront();
                String name = info.peerName;
                mUserName.setText(isLocal ? getResources().getString(R.string.calling_local_name) : name);

                setVisibility(mStatusView, VISIBLE);
                mStatusView.bringToFront();

                setVisibility(mStatusView2, INVISIBLE);//这个需要到布局阶段才能确定是否显示
                mStatusView2.bringToFront();

                setVisibility(mLoadingView, INVISIBLE);
                setVisibility(mLoadingViewMini, INVISIBLE);
            } else {
                setVisibility(mLoadingView, VISIBLE);
                setVisibility(mLoadingViewMini, VISIBLE);
                mLoadingView.bringToFront();
                mLoadingViewMini.bringToFront();

                setVisibility(mUserName, INVISIBLE);
                setVisibility(mStatusView, INVISIBLE);
                setVisibility(mStatusView2, INVISIBLE);
            }
            if (isAddConferee) {
                setVisibility(mCancelAddConferee, VISIBLE);
                mCancelAddConferee.bringToFront();
            }
        } else {
            if (mVideoView != null) {
                mVideoView.bringToFront();
            }

            //延后loading的消失
            mBackgroundHead.bringToFront();
            mLoadingView.bringToFront();
            mLoadingViewMini.bringToFront();


            setVisibility(mUserName, INVISIBLE);
            setVisibility(mStatusView, INVISIBLE);
            setVisibility(mStatusView2, INVISIBLE);

            //延后loading的消失
            postDelayed(() -> {
                setVisibility(mLoadingView, INVISIBLE);
                setVisibility(mLoadingViewMini, INVISIBLE);
                setVisibility(mBackgroundHead, INVISIBLE);
            }, isLocal ? 0 : 0);
        }

        if (info.isAudioMute && !isObserver) {
            setVisibility(mMicIconBackground, VISIBLE);
            mMicIconBackground.bringToFront();
            setVisibility(mMicIcon, VISIBLE);
            mMicIcon.bringToFront();
        } else {
            setVisibility(mMicIconBackground, INVISIBLE);
            setVisibility(mMicIcon, INVISIBLE);
        }
        muteReason.bringToFront();
        layoutSelf();
    }

    private boolean isVideoUnsupported = false;
    private boolean isAudioOnly = false;
    private boolean isAudioOnlyLocal = false;
    private boolean isLocalTelephone = false;
    private boolean isPstn = false;
    /**
     * 不包含请求状态
     */
    private boolean isNoVideo = false;
    private boolean isRequesting = false;
    protected boolean isCameraDisable = false;
    protected boolean isObserver = false;
    private boolean isAddConferee = false;

    /**
     * 更新内部状态信息
     */
    private void updateLayoutState(RtcConfereeLayout info) {
        RtcConfereeLayout.LayoutState videoState = confereeLayout.layoutState;
        isVideoUnsupported = RtcConfereeLayout.LayoutState.kLayoutStateVideoEncodeUnsupported.equals(videoState);
        isAudioOnlyLocal = RtcConfereeLayout.LayoutState.kLayoutStateReceivedAudioOnly == videoState;
        isAudioOnly = RtcConfereeLayout.LayoutState.kLayoutStateAudioOnly.equals(videoState);
        isCameraDisable = RtcConfereeLayout.LayoutState.kLayoutStateMute.equals(videoState) && RtcConfereeLayout.VideoMuteReason.MuteByCameraDisabled.equals(info.videoMuteReason);
        isPstn = RtcConfereeLayout.LayoutState.kLayoutStateTelephone.equals(videoState);
        isObserver = RtcConfereeLayout.LayoutState.kLayoutStateObserving.equals(videoState);
        isAddConferee = RtcConfereeLayout.LayoutState.kLayoutStateAddConferee.equals(videoState);
        isRequesting = RtcConfereeLayout.LayoutState.kLayoutStateRequesting.equals(videoState);
        isLocalTelephone = RtcConfereeLayout.LayoutState.kLayoutStateLocalTelephone.equals(videoState);
        isNoVideo = isPstn || isRequesting || isCameraDisable || isAudioOnlyLocal || isAudioOnly || isAddConferee || info.isVideoMute ||
                isVideoUnsupported || isLocalTelephone ||
                RtcConfereeLayout.LayoutState.kLayoutStateNoBandwidth.equals(videoState) ||
                RtcConfereeLayout.LayoutState.kLayoutStateNoDecoder.equals(videoState) ||
                RtcConfereeLayout.LayoutState.kLayoutStateMute.equals(videoState);
    }

    /**
     * updateNoVideoLayoutContent 与 onLayout递归调用了
     */
    private String lastState = "";

    /**
     * 更新无视频是显示文案
     *
     * @param isFull 是否为全屏
     */
    private void updateNoVideoLayoutContent(boolean isFull) {
        setVisibility(mCellRect, isFull ? INVISIBLE : VISIBLE);
        if (!isNoVideo) {
            return;
        }
        String state = isFull + "." + isLocal + "." + isCameraDisable + "." + true + "." + isAudioOnly + "." + isAudioOnlyLocal + "." + isObserver + "." + isRequesting + "." + isVideoUnsupported;
        if (state.equals(lastState)) {
            if (isFull) {
                if ((isAudioOnlyLocal && !isLocal) || (isCameraDisable && isLocal)) {
                    setVisibility(mStatusView2, VISIBLE);
                }
            }
            return;//防止无限触发onLayout
        }
        lastState = state;
        //LOGGER.info("updateNoVideoLayoutContent: name=" + confereeLayout.peerName + " isFull=" + isFull + ", isLocal=" + isLocal + ", isCameraDisable=" + isCameraDisable + ", isAudioOnlyLocal=" + isAudioOnlyLocal + ", isAudioOnly=" + isAudioOnly + ", isObserver=" + isObserver + ", isNoVideo=" + isNoVideo + ", isRequesting=" + isRequesting);
        mUserName.setMaxWidth(isFull ? 600 : isObserver ? 96 : 192);//这个方法不能放到onLayout中去,否则无限触发onLayout
        //语音模式的优先级高于摄像头关闭
        if (isFull) {//全屏
            if (isVideoUnsupported) {
                setText(mStatusView, R.string.calling_video_unsupported);
            } else if (isAudioOnlyLocal) {
                if (!isLocal) {
                    setVisibility(mStatusView2, VISIBLE);
                    setText(mStatusView, R.string.calling_audio_only_local_tips1);
                    setText(mStatusView2, R.string.calling_audio_only_local_tips2);
                } else {
                    setText(mStatusView, R.string.calling_audio_only);
                }
            } else if (isAudioOnly || isPstn || isLocalTelephone) {
                setText(mStatusView, R.string.calling_audio_only);
            } else if (isNoVideo) {
                if (isCameraDisable) {
                    setText(mStatusView, isLocal ? R.string.calling_camera_disabled_local_tips1 : R.string.calling_camera_disabled_remote_tips1);
                    if (isLocal) {
                        setVisibility(mStatusView2, VISIBLE);
                        setText(mStatusView2, R.string.calling_camera_disabled_local_tips2);
                    }
                } else {
                    RtcConfereeLayout.LayoutState videoState = confereeLayout.layoutState;
                    RtcConfereeLayout.VideoMuteReason muteReason = confereeLayout.videoMuteReason;
                    if (isObserver) {
                        setText(mStatusView, R.string.calling_watching);
                    } else if (RtcConfereeLayout.LayoutState.kLayoutStateNoBandwidth.equals(videoState) || RtcConfereeLayout.LayoutState.kLayoutStateNoDecoder.equals(videoState)) {
                        setText(mStatusView, R.string.calling_video_requesting);
                    } else if (muteReason != null) {
                        if (RtcConfereeLayout.VideoMuteReason.MuteByBWLimit.equals(muteReason)) {
                            setText(mStatusView, R.string.calling_video_pause_mute_by_bwlimit);
                        } else if (RtcConfereeLayout.VideoMuteReason.MuteByNoInput.equals(muteReason)) {
                            setText(mStatusView, R.string.calling_video_pause_mute_by_noinput);
                        } else {
                            setText(mStatusView, R.string.calling_video_pause);
                        }
                    }
                }
            }
        } else {//半屏
            setVisibility(mStatusView2, INVISIBLE);//因为有动画,这里有坑
            if (isObserver) {
                setText(mStatusView, R.string.calling_watching);
            } else if (isAddConferee) {
                setText(mStatusView, R.string.call_pending);
            } else if (isAudioOnlyLocal || isAudioOnly || isPstn || isLocalTelephone) {
                setText(mStatusView, R.string.calling_audio_only);
            } else if (isNoVideo) {
                if (isCameraDisable) {
                    setText(mStatusView, R.string.calling_camera_disabled);
                } else {
                    setText(mStatusView, R.string.calling_video_pause);
                }
            }
        }
    }


    private void setText(TextView v, int resId) {
        if (v != null) {
            setText(v, mResources.getText(resId));
        }
    }

    private void setText(TextView v, CharSequence cs) {
        if (isSame(cs, v.getText())) {
            return;
        }
        v.setText(cs);
    }

    private boolean isSame(CharSequence css, CharSequence cs) {
        if (css == null && cs == null) {
            return true;
        } else if (css != null && cs != null) {
            return (cs.toString().equals(css.toString()));
        }
        return false;
    }

    private boolean canLayout(View view) {
        if (view == null) {
            return false;
        }
        Object visibility = view.getTag(R.id.resource_id_3);
        return visibility != null && (int) visibility == VISIBLE;
    }

    private void setVisibility(View view, int visibility) {
        if (view != null) {
            view.setVisibility(INVISIBLE);
            view.setTag(R.id.resource_id_3, visibility);
        }
    }

    private void showView(View view) {
        if (view != null) {
            Object visibility = view.getTag(R.id.resource_id_3);
            if (visibility != null) {
                view.setVisibility((int) visibility);
            } else {
                view.setVisibility(INVISIBLE);
            }
        }
    }

    @SuppressLint("WrongCall")
    private void layoutSelf() {
        if (mPosition != null) {
            onLayout(true, mPosition.l, mPosition.t, mPosition.r, mPosition.b);
            invalidate();
        } else {
            requestLayout();
        }
    }

    private void layoutView(View v, int l, int t, int r, int b) {
        if (v != null) {
            showView(v);
            int ol = v.getLeft();
            int ot = v.getTop();
            int or = v.getRight();
            int ob = v.getBottom();

            if (ol != l || ot != t || or != r || ob != b) {
                v.layout(l, t, r, b);
            }
        }
    }

    @Override
    protected synchronized void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mPosition == null) {
            mPosition = new LayoutPosition(l, t, r, b);
        } else {
            mPosition.setPosition(l, t, r, b);
        }
        //窗口完整宽高
        int width = r - l;
        int height = b - t;
        //LOGGER.info("onLayout: " + contact.displayName + " " + width + ", " + height);
        boolean isFull = width >= mWidthThreshold;
        if (!isFull && isObserver) {
            width = 144;
        }
        updateNoVideoLayoutContent(isFull);

        //目标对象布局坐标
        int targetL;
        int targetT;
        int targetR;
        int targetB;

        int borderWidth = 0;
        //绘制矩形边框(始终全屏)
        if (canLayout(mCellRect)) {
            targetL = 0;
            targetT = 0;
            targetR = width;
            targetB = height;
            layoutView(mCellRect, targetL, targetT, targetR, targetB);
            borderWidth = 1;
            //LOGGER.info("onLayout: mCellRect " + targetL + ", " + targetT + ", " + targetR + ", " + targetB);
        }

        //绘制头像背景
        if (canLayout(mBackgroundHead)) {//全屏时此布局不可见
            targetL = borderWidth;
            targetT = borderWidth;
            targetR = (width - borderWidth);
            targetB = (height - borderWidth);
            layoutView(mBackgroundHead, targetL, targetT, targetR, targetB);
            //LOGGER.info("onLayout: mBackgroundHead " + targetL + ", " + targetT + ", " + targetR + ", " + targetB);
        }

        //绘制视频区域
        if (canLayout(mVideoView)) {
            //full screen
            if ((l + borderWidth) >= r || (t + borderWidth) >= b) {
                targetL = 0;
                targetT = 0;
                targetR = 1;
                targetB = 1;
                layoutView(mVideoView, targetL, targetT, targetR, targetB);
                //LOGGER.info("onLayout: mVideoView r l b t is invalidate, " + confereeLayout.peerName);
            } else {
                targetL = borderWidth;
                targetT = borderWidth;
                targetR = width - borderWidth;
                targetB = height - borderWidth;
                layoutView(mVideoView, targetL, targetT, targetR, targetB);
                //LOGGER.info("onLayout: mVideoView " + targetL + ", " + targetT + ", " + targetR + ", " + targetB + ", " + confereeLayout.peerName);
            }
        }

        //某些差值
        int poor1;
        int poor2;
        int poor3;
        //距离和,多个控件相对布局时使用
        int dist1;

        //对象的布局宽度
        int targetW;
        int targetH;

        //绘制mute mic
        if (canLayout(mMicIcon)) {
            targetW = isFull ? 58 : 42;
            targetH = isFull ? 58 : 42;
            poor1 = isFull ? 32 : 12;//上边距
            poor2 = isFull ? 32 : 22;//右边距
            targetL = width - targetW - poor2;
            targetT = poor1;
            targetR = targetL + targetW;
            targetB = targetT + targetH;
            //LOGGER.info("onLayout: mMicIcon " + targetL + ", " + targetT + ", " + targetR + ", " + targetB);
            layoutView(mMicIcon, targetL, targetT, targetR, targetB);

            //mic background
            targetL = borderWidth;
            targetT = borderWidth;
            targetR = width - borderWidth;
            targetB = height - borderWidth;
            layoutView(mMicIconBackground, targetL, targetT, targetR, targetB);
        }

        //一次绘制status2 status userName 从下往上(因为字体间隙的关系,不能加行距)
        dist1 = 0;
        if (isFull && canLayout(mStatusView2)) {
            poor1 = 48;//字体大小
            poor2 = 49;//右边距
            poor3 = 32;//下边距
            mStatusView2.setTextSize(poor1);
            mStatusView2.measure(0, 0);
            targetW = mStatusView2.getMeasuredWidth();
            targetH = mStatusView2.getMeasuredHeight();
            targetR = width - poor2;
            targetL = targetR - targetW;
            targetB = height - poor3;
            targetT = targetB - targetH;
            //LOGGER.info("onLayout: mStatusView2 " + targetL + ", " + targetT + ", " + targetR + ", " + targetB + ", " + confereeLayout.peerName);
            layoutView(mStatusView2, targetL, targetT, targetR, targetB);
            dist1 = poor3 + targetH - 6;
        }

        if (canLayout(mStatusView)) {
            poor1 = isFull ? 24 : 12;//字体大小
            poor2 = isFull ? 60 : 24;//右边距
            poor3 = isFull ? (dist1 > 0 ? dist1 : 78) : 16;//下边距
            mStatusView.setTextSize(poor1);
            mStatusView.measure(0, 0);
            targetW = mStatusView.getMeasuredWidth();
            targetH = mStatusView.getMeasuredHeight();
            targetR = width - poor2;
            targetL = targetR - targetW;
            targetB = height - poor3;
            targetT = targetB - targetH;
            // LOGGER.info("onLayout: mStatusView " + targetL + ", " + targetT + ", " + targetR + ", " + targetB);
            layoutView(mStatusView, targetL, targetT, targetR, targetB);
            dist1 = poor3 + targetH + (isFull ? (dist1 > 0 ? -10 : 6) : 0);//全屏模式下显示两行和显示三行时mUserName到mStatusView的距离不一样
        }

        if (canLayout(mUserName)) {
            poor1 = isFull ? 30 : 14;//字体大小
            poor2 = isFull ? 60 : 24;//右边距
            poor3 = dist1;//下边距
            mUserName.setTextSize(poor1);
            mUserName.measure(0, 0);
            targetW = mUserName.getMeasuredWidth();
            targetH = mUserName.getMeasuredHeight();
            targetR = width - poor2;
            targetL = targetR - targetW;
            targetB = height - poor3;
            targetT = targetB - targetH;
            //LOGGER.info("onLayout: mUserName " + targetL + ", " + targetT + ", " + targetR + ", " + targetB + ", " + isFull);
            layoutView(mUserName, targetL, targetT, targetR, targetB);
            //dist1 = poor3 + targetH;
        }

        //绘制loading
        if (canLayout(mLoadingView) || canLayout(mLoadingViewMini)) {
            View view;
            if (isFull) {
                layoutView(mLoadingViewMini, 0, 0, 0, 0);
                view = mLoadingView;
            } else {
                layoutView(mLoadingView, 0, 0, 0, 0);
                view = mLoadingViewMini;
            }
            view.measure(0, 0);
            targetW = view.getMeasuredWidth();
            targetH = view.getMeasuredHeight();
            targetL = borderWidth + (width - targetW) / 2;
            targetT = borderWidth + (height - targetH) / 2;
            targetR = targetL + targetW;//(width - borderWidth);
            targetB = targetT + targetH; //(height - borderWidth);
            layoutView(view, targetL, targetT, targetR, targetB);
            //LOGGER.info("onLayout: mLoadingView " + targetL + ", " + targetT + ", " + targetR + ", " + targetB);
        }

        //cancel add conferee
        if (canLayout(mCancelAddConferee)) {
            Drawable drawable = mCancelAddConferee.getDrawable();
            targetL = width - drawable.getIntrinsicWidth() / 10 * 9;
            targetT = -drawable.getIntrinsicWidth() / 10;
            targetR = (int) (targetL + 22 * mDensity);
            targetB = (int) (targetT + 22 * mDensity);
            mCancelAddConferee.setPadding(-10, -10, -10, -10);
            layoutView(mCancelAddConferee, targetL, targetT, targetR, targetB);
        }
        layoutView(muteReason, l, height / 2, r, height / 2 + 50);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        onDestroy();
    }

    public void onDestroy() {
        if (mVideoView != null) {
            removeView(mVideoView);
            mVideoView = null; //加快RtcVideoView垃圾回收
        }
    }

    public void onResume() {
        mVideoView.onResume();
    }

    public void onPause() {
        mVideoView.onPause();
    }

    /**
     * 播放退出动画
     */
    public void playExitAnimation() {
        TranslateAnimation animation = new TranslateAnimation(0, -5, 0, 0);
        animation.setInterpolator(new OvershootInterpolator());
        animation.setInterpolator(getContext(), android.R.interpolator.accelerate_quint);
        animation.setDuration(20);
        animation.setRepeatCount(30);
        animation.setRepeatMode(Animation.REVERSE);
        animation.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation arg0) {
            }

            @Override
            public void onAnimationRepeat(Animation arg0) {
            }

            @Override
            public void onAnimationEnd(Animation arg0) {
                mCellEventListener.onExitAnimationEnd(ConfereeView.this);
            }
        });
        startAnimation(animation);
    }

    public boolean isRectVisible = false;
    public boolean isFullScreen = false;
    public boolean isDragged = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastDragX = (int) event.getRawX();
                mLastDragY = (int) event.getRawY();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mLastDragX = 0;
                mLastDragY = 0;
                break;
        }
        mGestureDetector.onTouchEvent(event);
        return true;
    }

    /**
     * 手势事件处理
     */
    private class SurfaceGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float dx = e2.getRawX() - mLastDragX;
            float dy = e2.getRawY() - mLastDragY;
            boolean result = mCellEventListener.onScroll(e1, e2, dx, dy, ConfereeView.this);
            mLastDragX = (int) e2.getRawX();
            mLastDragY = (int) e2.getRawY();
            return result;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return mCellEventListener.onDoubleTap(e, ConfereeView.this);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (isObserver || isAddConferee) {
                return false; //回家看看/邀请窗口不接收点击事件
            }
            return mCellEventListener.onSingleTap(e, ConfereeView.this);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            mCellEventListener.onLongPress(e, ConfereeView.this);
        }
    }

    /**
     * 窗口事件监听器
     */
    public interface CellEventListener {
        /**
         * 长按
         */
        void onLongPress(MotionEvent e, ConfereeView view);

        /**
         * 双击
         */
        boolean onDoubleTap(MotionEvent e, ConfereeView view);

        /**
         * 单击
         */
        boolean onSingleTap(MotionEvent e, ConfereeView view);

        /**
         * 滚动
         */
        boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY, ConfereeView view);

        /**
         * {@link ConfereeView#playExitAnimation()} 动画完成回调
         */
        void onExitAnimationEnd(ConfereeView view);

        /**
         * 取消邀请点击事件
         */
        void onCancelAddConferee(RtcUri uri);
    }

    public static abstract class CellEventAdapter implements CellEventListener {
        @Override
        public void onLongPress(MotionEvent e, ConfereeView view) {

        }

        @Override
        public boolean onDoubleTap(MotionEvent e, ConfereeView view) {
            return false;
        }

        @Override
        public boolean onSingleTap(MotionEvent e, ConfereeView view) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY, ConfereeView view) {
            return false;
        }

        @Override
        public void onExitAnimationEnd(ConfereeView view) {

        }

        @Override
        public void onCancelAddConferee(RtcUri uri) {

        }
    }

    /**
     * 布局位置记录
     */
    private static class LayoutPosition {
        private int l;
        private int t;
        private int r;
        private int b;

        LayoutPosition(int l, int t, int r, int b) {
            super();
            this.l = l;
            this.t = t;
            this.r = r;
            this.b = b;
        }

        void setPosition(int l, int t, int r, int b) {
            this.l = l;
            this.t = t;
            this.r = r;
            this.b = b;
        }
    }
}
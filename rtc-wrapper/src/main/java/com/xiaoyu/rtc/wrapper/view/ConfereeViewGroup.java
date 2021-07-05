package com.xiaoyu.rtc.wrapper.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;

import com.xiaoyu.open.RtcUri;
import com.xiaoyu.open.call.RtcCallMode;
import com.xiaoyu.open.call.RtcConfereeState;
import com.xiaoyu.open.video.RtcConfereeLayout;
import com.xiaoyu.open.video.RtcVideoCapturer;
import com.xiaoyu.rtc.wrapper.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * 与会者窗口布局组合封装，包含本地+远端窗口排列、窗口位置交换等逻辑
 */
public class ConfereeViewGroup extends ViewGroup {
    private static final Logger LOGGER = Logger.getLogger("ConfereeGroupView");
    private static final int THUMB_CELL_COUNT = 4;//纵向可以放置多少个窗口
    private static final int LAYOUT_ANIMATION_DURATION = 400;
    //UI views
    protected ConfereeView mLocalVideoView;
    //UI data
    private int mCellWidth;
    private int mCellHeight;
    private int mCellPadding;
    private volatile boolean mLocalFullScreen = true;
    private volatile int animatingCount = 0;
    private volatile boolean isPause = false;

    private LayoutStatus mLayoutStatus = LayoutStatus.LOCAL;
    private List<ConfereeView> mThumbConfereeViews = new CopyOnWriteArrayList<>();
    private List<ConfereeView> mAddConfereeViews = new CopyOnWriteArrayList<>();
    private List<RtcConfereeLayout> mCachedConfereeLayouts = new ArrayList<>(0);

    private Runnable resetAnimatingStateRunnable = () -> updateAnimatingState(false, false);
    /**
     * 设置本地预览窗口监听器
     */
    private LocalPreviewListener localPreviewListener;

    public ConfereeViewGroup(Context context, LocalPreviewListener localPreviewListener) {
        super(context);
        this.localPreviewListener = localPreviewListener;
        initView();
    }

    private void initView() {
        Resources resources = getResources();
        setBackgroundColor(resources.getColor(R.color.colorNoVideoBackground));
        mCellPadding = (int) resources.getDimension(R.dimen.cellPadding);
        setClipChildren(false);
        createLocalVideoView();
    }

    private IUserActionListener mUserActionListener;

    /**
     * 设置窗口切换监听器
     */
    public void setUserActionListener(IUserActionListener forceLayoutListener) {
        this.mUserActionListener = forceLayoutListener;
    }

    public void setLayoutStatus(LayoutStatus status) {
        mLayoutStatus = status;
        LOGGER.info("setLayoutStatus: " + status);
        if (LayoutStatus.OBSERVER.equals(mLayoutStatus)) {
            if (mLocalVideoView != null) {
                mLocalVideoView.setVisibility(GONE);
            }
        } else if (LayoutStatus.TEL.equals(mLayoutStatus)) {
            if (mLocalVideoView != null) {
                mLocalVideoView.setVisibility(GONE);
            }
        } else {
            if (mLocalVideoView != null) {
                mLocalVideoView.setVisibility(VISIBLE);
            }
        }
        requestLayout();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        int largeSide = Math.min(w, h);
        mCellHeight = (largeSide - mCellPadding) / THUMB_CELL_COUNT - mCellPadding;
        mCellWidth = (int) ((float) mCellHeight * 1.777777777777778f);
        requestLayout();
    }

    public void onDestroy() {
        for (ConfereeView videoView : mThumbConfereeViews) {
            videoView.onDestroy();
        }
        mThumbConfereeViews.clear();
        if (mLocalVideoView != null) {
            mLocalVideoView.onDestroy();
            mLocalVideoView = null;
        }
    }

    public void onResume() {
        if (mLocalVideoView != null) {
            mLocalVideoView.onResume();
        }

        isPause = false;
        updateConfereeLayouts(mCachedConfereeLayouts);

        for (ConfereeView videoView : mThumbConfereeViews) {
            videoView.onResume();
        }
    }

    public void onPause() {
        if (mLocalVideoView != null) {
            mLocalVideoView.onPause();
        }
        for (ConfereeView videoView : mThumbConfereeViews) {
            videoView.onPause();
        }
        isPause = true;
    }

    private boolean delayUpdateConferee = false;

    /**
     * 更新与会者布局信息
     */
    public synchronized void updateConfereeLayouts(List<RtcConfereeLayout> layouts) {
        mCachedConfereeLayouts = layouts;
        if (isPause) {
            return;
        }
        if (animatingCount != 0) {
            delayUpdateConferee = true;
            return;
        } else {
            delayUpdateConferee = false;
        }
        if (mCachedConfereeLayouts.isEmpty()) {
            mLocalFullScreen = true;
        }
        //通话刚接通时，本地需要变小窗口
        if (mThumbConfereeViews.isEmpty() && mLocalFullScreen) {
            mLocalFullScreen = false;
        }

        if (LayoutStatus.OBSERVER.equals(mLayoutStatus)) {
            if (!mCachedConfereeLayouts.isEmpty()) {
                mCachedConfereeLayouts = mCachedConfereeLayouts.subList(0, 1);
            }
        }

        //清除已经退出的cell
        List<ConfereeView> toDel = new ArrayList<>();
        boolean isFound;
        for (ConfereeView videoView : mThumbConfereeViews) {
            isFound = false;
            for (RtcConfereeLayout info : mCachedConfereeLayouts) {
                if (videoView.equalsConfereeLayout(info)) {
                    isFound = true;
                    break;
                }
            }
            if (!isFound) {
                toDel.add(videoView);
            }
        }
        for (ConfereeView videoView : toDel) {
            removeView(videoView);
            mThumbConfereeViews.remove(videoView);
        }

        ConfereeView oldView;
        for (int i = 0; i < mCachedConfereeLayouts.size(); i++) {
            RtcConfereeLayout info = mCachedConfereeLayouts.get(i);
            if (i < mThumbConfereeViews.size()) {//这个位置是否有cell
                oldView = mThumbConfereeViews.get(i);
                if (oldView.equalsConfereeLayout(info)) {
                    oldView.setConfereeLayout(info);//位置不变
                } else { //挪到当前位置
                    int position = -1;
                    ConfereeView newView = null;
                    for (int j = 0; j < mThumbConfereeViews.size(); j++) {
                        oldView = mThumbConfereeViews.get(j);
                        if (oldView.equalsConfereeLayout(info)) {
                            position = j;
                            newView = oldView;
                            break;
                        }
                    }
                    if (newView == null) {
                        newView = createRemoteCell(info);
                        mThumbConfereeViews.add(i, newView);
                    } else {
                        newView.setConfereeLayout(info);
                        if (!mLocalFullScreen && i == 0) {
                            swapToMainCell(newView, false);
                        } else {
                            ConfereeView index0 = mThumbConfereeViews.get(0);
                            mThumbConfereeViews.set(0, newView);
                            mThumbConfereeViews.set(position, index0);
                        }
                    }
                }
            } else {//直接创建一个cell
                ConfereeView newView = createRemoteCell(info);
                mThumbConfereeViews.add(newView);
            }
        }
        animateSmallCells(mCachedConfereeLayouts);
        requestLayout();
    }

    private void animateSmallCells(List<RtcConfereeLayout> layouts) {
        for (int i = 0; i < layouts.size(); i++) {
            if (!mLocalFullScreen && i == 0) {
                continue;
            }
            RtcConfereeLayout info = layouts.get(i);
            int cellIndex = getCellIndex(info);
            if (cellIndex == -1) {
                continue;
            }
            if (i != cellIndex) {
                animateCellToIndex(mThumbConfereeViews.get(cellIndex), cellIndex, i);
            }
        }
    }

    private int getCellIndex(RtcConfereeLayout info) {
        for (int i = 0; i < mThumbConfereeViews.size(); i++) {
            if (mThumbConfereeViews.get(i).equalsConfereeLayout(info)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 对端窗口位置移动
     */
    private void animateCellToIndex(final ConfereeView videoView, final int fromIndex, final int toIndex) {
        //LOGGER.info("animateCellToIndex, from " + fromIndex + " to " + toIndex + ", " + videoView.confereeLayout.peerName);
        ResizeMoveAnimation animationMove = new ResizeMoveAnimation(videoView, getSmallCellRect(fromIndex), getSmallCellRect(toIndex), LAYOUT_ANIMATION_DURATION);
        animationMove.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                updateAnimatingState(true);
                if (mThumbConfereeViews.size() > fromIndex && mThumbConfereeViews.size() > toIndex) {
                    mThumbConfereeViews.remove(videoView);
                    mThumbConfereeViews.add(toIndex, videoView);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                updateAnimatingState(false);
                requestLayout();
            }
        });
        videoView.startAnimation(animationMove);
    }

    /**
     * 将指定窗口与当前大窗交换
     */
    protected void swapToMainCell(final ConfereeView thumbView, final boolean forceLayout) {
        if (mThumbConfereeViews.isEmpty()) {
            LOGGER.severe("swapToMainCell: skip view=" + thumbView.viewTag + " by conferees empty");
            return;
        }
        ConfereeView firstView = mThumbConfereeViews.get(0);
        final ConfereeView mainView = mLocalFullScreen ? mLocalVideoView : firstView;
        if (thumbView == mainView) {
            updateAnimatingState(false);
            LOGGER.severe("swapToMainCell: skip view=" + mainView.viewTag);
            return;
        }
        final boolean isLocalThumb = thumbView.isLocal;
        //先交换能有效防止重复的动画
        int index = mThumbConfereeViews.indexOf(thumbView);
        if (!isLocalThumb && index > -1) {
            Collections.swap(mThumbConfereeViews, 0, index);
        }
        //通知SDK请求指定窗口高码率视频
        if (forceLayout) {
            setForceTargetLayout(thumbView);
        }
        final boolean isLocalMain = mainView.isLocal;
        LOGGER.info("swapToMainCell: mainView=" + mainView.viewTag + ", thumbView=" + thumbView.viewTag);
        Rect mainViewRect = getCellRect(mainView);
        Rect thumbViewRect = getCellRect(thumbView);
        //本地窗口切换成小窗口时，需要保持在所有小窗的第一个位置
        ResizeMoveAnimation mainToThumbAni = new ResizeMoveAnimation(mainView, mainViewRect, isLocalMain ? getCellRect(firstView) : thumbViewRect, LAYOUT_ANIMATION_DURATION);
        mainView.startAnimation(mainToThumbAni);

        ResizeMoveAnimation thumbToMainAni = new ResizeMoveAnimation(thumbView, thumbViewRect, mainViewRect, LAYOUT_ANIMATION_DURATION);
        thumbToMainAni.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                updateAnimatingState(true);
                mainView.bringToFront();
                thumbView.bringToFront();
                if (!isLocalMain && !isLocalThumb) {
                    mLocalVideoView.bringToFront();
                }
                for (ConfereeView videoView : mThumbConfereeViews) {
                    if (!thumbView.equals(videoView) && !mainView.equals(videoView)) {
                        videoView.bringToFront();
                    }
                }
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                updateAnimatingState(false);
                mLocalFullScreen = isLocalThumb;
                requestLayout();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        thumbView.startAnimation(thumbToMainAni);
    }

    private ConfereeView createRemoteCell(RtcConfereeLayout layoutInfo) {
        ConfereeView videoView = getVideoView("cv_" + layoutInfo.id, false, layoutInfo);
        videoView.bringToFront();
        addView(videoView);
        return videoView;
    }

    private RtcConfereeLayout localLayoutInfo;

    private RtcCallMode localCallMode = RtcCallMode.CallMode_AudioVideo;


    /**
     * 更改CallMode后需要手动调用此方法刷新本窗口
     *
     * @param callMode 当前的通话模式
     */
    public void changeLocalCallMode(RtcCallMode callMode) {
        localCallMode = callMode;
        if (mLocalVideoView != null) {
            if (RtcCallMode.CallMode_AudioOnly.equals(localCallMode)) {
                localLayoutInfo.layoutState = RtcConfereeLayout.LayoutState.kLayoutStateReceivedAudioOnly;
                mLocalVideoView.setConfereeLayout(localLayoutInfo);
            } else {
                changeLocalCameraStatus(localCameraStatus);
            }
        }
    }

    private boolean localCameraStatus = true;

    /**
     * Camera状态改变后需要手动调用此方法刷新本窗口
     *
     * @param enable Camera是否启用
     */
    public void changeLocalCameraStatus(boolean enable) {
        localCameraStatus = enable;
        if (RtcCallMode.CallMode_AudioOnly.equals(localCallMode)) {
            return;
        }

        if (mLocalVideoView != null) {
            if (localCameraStatus) {
                localLayoutInfo.layoutState = RtcConfereeLayout.LayoutState.kLayoutStateReceived;
                localLayoutInfo.videoMuteReason = RtcConfereeLayout.VideoMuteReason.MuteByUser;
            } else {
                localLayoutInfo.layoutState = RtcConfereeLayout.LayoutState.kLayoutStateMute;
                localLayoutInfo.videoMuteReason = RtcConfereeLayout.VideoMuteReason.MuteByCameraDisabled;
            }
            mLocalVideoView.setConfereeLayout(localLayoutInfo);
        }
    }


    /**
     * 更改Mic 状态后需要手动调用此方法刷新本窗口
     *
     * @param mute 是否mute
     */
    public void updateLocalMicMute(boolean mute) {
        if (mLocalVideoView != null) {
            localLayoutInfo.isAudioMute = mute;
            mLocalVideoView.setConfereeLayout(localLayoutInfo);
        }
    }

    private void createLocalVideoView() {
        localLayoutInfo = RtcConfereeLayout.getLocalLayout();
        localLayoutInfo.layoutState = RtcCallMode.CallMode_AudioVideo.equals(localCallMode) ? RtcConfereeLayout.LayoutState.kLayoutStateReceived : RtcConfereeLayout.LayoutState.kLayoutStateAudioOnly;
        localLayoutInfo.videoSourceId = RtcVideoCapturer.SOURCE_ID_LOCAL_PREVIEW;
        localLayoutInfo.id = -1;

        mLocalVideoView = getVideoView("lcv_" + ConfereeView.LOCAL_VIEW_ID, true, localLayoutInfo);
        mLocalVideoView.bringToFront();
        addView(mLocalVideoView);
    }

    private SimpleCellEventListener cellEventListener = new SimpleCellEventListener();

    protected ConfereeView getVideoView(String viewTag, boolean isLocal, RtcConfereeLayout info) {
        ConfereeView videoView;
        if (isLocal) {
            if (localPreviewListener != null) {
                videoView = new ConfereeView(getContext(), viewTag, cellEventListener, false, localPreviewListener);
            } else {
                videoView = new ConfereeView(getContext(), viewTag, cellEventListener, false, true);
            }
        } else {
            videoView = new ConfereeView(getContext(), viewTag, cellEventListener, false, false);
        }
        videoView.setConfereeLayout(info);
        videoView.setId(ConfereeView.LOCAL_VIEW_ID);
        videoView.updatePreviewOrientation(isLandscape);
        return videoView;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int measuredWidth = 1280;
        if (widthSpecMode == MeasureSpec.AT_MOST || widthSpecMode == MeasureSpec.EXACTLY) {
            measuredWidth = widthSpecSize;
        }

        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        int measuredHeight = 720;
        if (heightSpecMode == MeasureSpec.AT_MOST || heightSpecMode == MeasureSpec.EXACTLY) {
            measuredHeight = heightSpecSize;
        }

        int thumbWspec = MeasureSpec.makeMeasureSpec(mCellWidth, MeasureSpec.EXACTLY);
        int thumbHspec = MeasureSpec.makeMeasureSpec(mCellHeight, MeasureSpec.EXACTLY);
        ConfereeView videoView;
        for (int i = 0; i < mThumbConfereeViews.size(); i++) {
            videoView = mThumbConfereeViews.get(i);
            videoView.measure(thumbWspec, thumbHspec);
        }
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    private void updateAnimatingState(boolean animating) {
        updateAnimatingState(animating, true);
    }

    private void updateAnimatingState(boolean animating, boolean reset) {
        boolean animEnd;
        synchronized (ConfereeViewGroup.this) {
            if (!animating && !reset) {
                animatingCount = 0;
            } else {
                this.animatingCount += animating ? 1 : -1;
                if (animatingCount < 0) {
                    animatingCount = 0;
                }
            }
            animEnd = animatingCount == 0;
            this.removeCallbacks(resetAnimatingStateRunnable);
            if (reset) {
                this.postDelayed(resetAnimatingStateRunnable, 2000);
            }
        }
        if (animEnd && delayUpdateConferee) {
            updateConfereeLayouts(mCachedConfereeLayouts);
        }
    }

    private synchronized boolean isAnimating() {
        return animatingCount != 0;
    }

    private Rect getSmallCellRect(int cellIndex) {
        int left = mCellPadding;
        int top = cellIndex * (mCellPadding + mCellHeight) + mCellPadding;
        return new Rect(left, top, left + mCellWidth, top + mCellHeight);
    }

    private Rect getCellRect(final ConfereeView thumbCell) {
        return new Rect(thumbCell.getLeft(), thumbCell.getTop(), thumbCell.getRight(), thumbCell.getBottom());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (isAnimating()) {
            return;
        }
        int mThumbVideoViewTop = mCellPadding;
        int mThumbVideoViewLeft = mCellPadding;
        int cellSize = mThumbConfereeViews.size();
        int callIndex = 0;
        //布局本地窗口
        if (mLocalFullScreen) {
            layoutFullScreenVideoView(mLocalVideoView, l, t, r, b);
        } else {
            //先绘制远端大窗口
            if (cellSize > 0) {
                layoutFullScreenVideoView(mThumbConfereeViews.get(0), l, t, r, b);
                callIndex = 1;
            }
            layoutThumbVideoView(mLocalVideoView, mThumbVideoViewLeft, mThumbVideoViewTop);
            mThumbVideoViewTop += mCellHeight + mCellPadding;
        }

        //布局远端通话窗口
        for (int i = callIndex; i < cellSize; i++) {
            layoutThumbVideoView(mThumbConfereeViews.get(i), mThumbVideoViewLeft, mThumbVideoViewTop);
            mThumbVideoViewTop += mCellHeight + mCellPadding;
        }

        //布局pending窗口
        for (ConfereeView addView : mAddConfereeViews) {
            layoutThumbVideoView(addView, mThumbVideoViewLeft, mThumbVideoViewTop);
            mThumbVideoViewTop += mCellHeight + mCellPadding;
        }
    }

    /**
     * 将指定窗口布局为全屏
     */
    private void layoutFullScreenVideoView(ConfereeView videoView, int l, int t, int r, int b) {
        if (videoView == null) {
            LOGGER.info("layoutFullScreenVideoView: videoView is null");
            return;
        }
        videoView.isRectVisible = false;
        videoView.isFullScreen = true;
        videoView.bringToFront();
        videoView.layout(l, t, r, b);
    }

    /**
     * 将指定窗口布局为小屏
     */
    private void layoutThumbVideoView(ConfereeView videoView, int left, int top) {
        if (videoView == null) {
            LOGGER.info("layoutThumbVideoView: videoView is null");
            return;
        }
        videoView.isRectVisible = true;
        videoView.isFullScreen = false;
        videoView.bringToFront();
        if (videoView.isDragged) {
            videoView.layout(videoView.getLeft(), videoView.getTop(), videoView.getRight(), videoView.getBottom());
        } else {
            videoView.layout(left, top, left + mCellWidth, top + mCellHeight);
        }
    }

    private boolean isLandscape;

    public void updatePreviewOrientation(boolean isLandscape) {
        this.isLandscape = isLandscape;
        if (mLocalVideoView != null) {
            mLocalVideoView.updatePreviewOrientation(isLandscape);
        }
        for (ConfereeView view : mThumbConfereeViews) {
            view.updatePreviewOrientation(isLandscape);
        }
        for (ConfereeView view : mThumbConfereeViews) {
            view.updatePreviewOrientation(isLandscape);
        }
    }

    public class SimpleCellEventListener extends ConfereeView.CellEventAdapter {
        @Override
        public boolean onSingleTap(MotionEvent e, ConfereeView view) {
            if (view.isFullScreen) {
                //TODO 做点什么事情
            } else {
                if (isAnimating()) {
                    return false;
                }
                //切换为全屏
                swapToMainCell(view, true);
            }
            return true;
        }

        @Override
        public void onExitAnimationEnd(ConfereeView view) {
            removeView(view);
            mAddConfereeViews.remove(view);
        }

        @Override
        public void onCancelAddConferee(RtcUri uri) {
            if (mUserActionListener != null) {
                mUserActionListener.cancelConferee(uri);
            }
        }
    }

    enum LayoutStatus {
        TEL,
        LOCAL,
        OBSERVER
    }

    private void setForceTargetLayout(ConfereeView videoView) {
        if (mUserActionListener != null) {
            mUserActionListener.onForceTargetLayout(videoView.confereeLayout.id);
        }
    }

    /**
     * 本地预览窗口监听器
     */
    public interface LocalPreviewListener {
        /**
         * 本地预览窗口被加载时回调
         *
         * @param textureView 用于显示本地预览画面的视图
         */
        void onPostInit(TextureView textureView);
    }

    /**
     * 邀请第三方窗口布局
     */
    public void addConferee(RtcConfereeLayout... layouts) {
        boolean create;
        for (RtcConfereeLayout layout : layouts) {
            create = true;
            layout.id = layout.peerUri.hashCode();
            for (ConfereeView view : mAddConfereeViews) {
                if (view.equalsConfereeLayout(layout)) {
                    create = false;
                    view.setConfereeLayout(layout);
                    break;
                }
            }
            if (create) {
                ConfereeView videoView = new ConfereeView(getContext(), "av_" + layout.id, cellEventListener, false, false);
                videoView.setConfereeLayout(layout);
                mAddConfereeViews.add(videoView);
                videoView.bringToFront();
                addView(videoView);
            }
        }
        requestLayout();
    }

    /**
     * 邀请第三方结果
     */
    public void onAddConfereeResult(RtcConfereeState result) {
        ConfereeView view;
        for (ConfereeView mAddConfereeView : mAddConfereeViews) {
            view = mAddConfereeView;
            if (view.onAddConfereeResult(result)) {
                view.playExitAnimation();
                break;
            }
        }
    }
}
package com.xiaoyu.rtc.wrapper.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.Nullable;
import android.support.v4.view.animation.PathInterpolatorCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;

import com.xiaoyu.rtc.wrapper.R;

import java.util.logging.Logger;

public class BigBallLoadingView extends View {
    private Paint p;
    private Path path;
    private Bitmap mBitmap;
    private Bitmap mBitmap2;
    private float MOVE_DISTANCE = 28;
    private float left1 = 0;
    private float left2 = MOVE_DISTANCE;
    private Interpolator interpolator1;
    private boolean isAttachedToWindow = false;

    private Logger LOGGER = Logger.getLogger("BigBallLoadingView");

    public BigBallLoadingView(Context context) {
        super(context);
        initView(context);
    }

    public BigBallLoadingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    private void initView(Context context) {
        mBitmap = ((BitmapDrawable) context.getResources().getDrawable(R.drawable.blue_ball_big)).getBitmap();
        mBitmap2 = ((BitmapDrawable) context.getResources().getDrawable(R.drawable.orange_ball_big)).getBitmap();
        p = new Paint();
        path = new Path();
    }


    private ValueAnimator animator2;
    private ValueAnimator animator1;

    protected void onCreate() {
        LOGGER.info("onCreate");
        if (interpolator1 == null) {
            interpolator1 = PathInterpolatorCompat.create(0.33f, 0.0f, 0.15f, 1.0f);
        }
        animator2 = ValueAnimator.ofFloat(0, MOVE_DISTANCE);
        animator2.setInterpolator(interpolator1);
        animator2.addUpdateListener(animatorUpdateListener2);
        animator2.addListener(animatorListener2);
        animator2.setDuration(666);
        animator2.start();

        animator1 = ValueAnimator.ofFloat(0, MOVE_DISTANCE);
        animator1.setInterpolator(interpolator1);
        animator1.addUpdateListener(animatorUpdateListener1);
        animator1.addListener(animatorListener1);
        animator1.setDuration(666);


    }


    ValueAnimator.AnimatorUpdateListener animatorUpdateListener2 = animation -> {
        float value = (float) animation.getAnimatedValue();
        left1 = value;
        left2 = MOVE_DISTANCE - value;
        BigBallLoadingView.this.invalidate();
    };


    ValueAnimator.AnimatorUpdateListener animatorUpdateListener1 = animation -> {
        float value = (float) animation.getAnimatedValue();
        left1 = MOVE_DISTANCE - value;
        left2 = value;
        BigBallLoadingView.this.invalidate();
    };


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBitmap2 == null || mBitmap2.isRecycled() || mBitmap == null || mBitmap.isRecycled()) {
            return;
        }
        path.reset();
        canvas.drawBitmap(mBitmap, left1, 0, p);
        canvas.drawBitmap(mBitmap2, left2, 0, p);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(45, 16);
    }

    private Animator.AnimatorListener animatorListener2 = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (isAttachedToWindow) {
                animator1.start();
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };


    private Animator.AnimatorListener animatorListener1 = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (isAttachedToWindow) {
                animator2.start();
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {


        }
    };


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        LOGGER.info("onAttachedToWindow: " + isAttachedToWindow + " " + this);
        isAttachedToWindow = true;
        onCreate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        LOGGER.info("onDetachedFromWindow: " + isAttachedToWindow + " " + this);
        isAttachedToWindow = false;
        if (animator2 != null) {
            animator2.cancel();
        }
        if (animator1 != null) {
            animator1.cancel();
        }
    }
}

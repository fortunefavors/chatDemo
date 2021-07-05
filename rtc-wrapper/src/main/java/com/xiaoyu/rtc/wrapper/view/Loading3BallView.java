package com.xiaoyu.rtc.wrapper.view;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.animation.PathInterpolatorCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.xiaoyu.rtc.wrapper.R;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Loading3BallView extends RelativeLayout {

    private static final Logger LOGGER = Logger.getLogger("Loading3BallView");

    public ImageView ball_1;
    public ImageView ball_2;
    public ImageView ball_3;

    private static float loading_view_ball_up_translation_y;
    private static float loading_view_ball_down_translation_y;

    private boolean mIsAnimating = false;

    private Handler mHandler = new Handler();

    public Loading3BallView(@NonNull Context context) {
        this(context, null);
    }

    public Loading3BallView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.loading_3_ball_view, this, true);
        ball_1 = findViewById(R.id.ball_1);
        ball_2 = findViewById(R.id.ball_2);
        ball_3 = findViewById(R.id.ball_3);

        loading_view_ball_up_translation_y = getResources().getDimension(R.dimen.loading_view_ball_up_translation_y);
        loading_view_ball_down_translation_y = getResources().getDimension(R.dimen.loading_view_ball_down_translation_y);
    }

    @Override
    protected void onAttachedToWindow() {
        LOGGER.info("onAttachedToWindow");
        super.onAttachedToWindow();
        startAnimate();
    }

    @Override
    protected void onDetachedFromWindow() {
        LOGGER.info("onDetachedFromWindow");
        super.onDetachedFromWindow();
        stopAnimate();
    }

    public void startAnimate() {
        LOGGER.info("startAnimate: mIsAnimating: " + mIsAnimating);
        if (mIsAnimating) {
            return;
        }
        mIsAnimating = true;

        ball_1.postDelayed(() -> startAnimation1(ball_1), DELAY);

        ball_2.postDelayed(() -> startAnimation1(ball_2), DELAY + DELAY);

        ball_3.postDelayed(() -> startAnimation1(ball_3), DELAY + DELAY + DELAY);
    }

    public void stopAnimate() {
        LOGGER.info("stopAnimaate");
        ball_1.clearAnimation();
        ball_2.clearAnimation();
        ball_3.clearAnimation();
        mIsAnimating = false;
    }

    public boolean isAnimating() {
        return mIsAnimating;
    }

    public static final int DELAY = 233;
    public static final int DURATION_1 = 333;
    public static final int DURATION_2 = 667;
    public static final int DURATION_3 = 333;

    public static final Interpolator slowInterpolator = PathInterpolatorCompat.create(0.25f, 0.10f, 0.25f, 1.0f); //缓入缓出

    private void startAnimation1(final View view) {
        if (view == null || !mIsAnimating) {
            return;
        }
        List<Animator> animatorList1 = new ArrayList<>();
        ObjectAnimator translateAnimator1 = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0.0f, -loading_view_ball_up_translation_y);
        translateAnimator1.setInterpolator(slowInterpolator);
        translateAnimator1.setDuration(DURATION_1);
        animatorList1.add(translateAnimator1);

        ObjectAnimator alphaAnimator1 = ObjectAnimator.ofFloat(view, View.ALPHA, 0.3f, 1.0f);
        alphaAnimator1.setInterpolator(slowInterpolator);
        alphaAnimator1.setDuration(DURATION_1);
        animatorList1.add(alphaAnimator1);

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animatorList1);
        animatorSet.start();
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mHandler.postDelayed(() -> startAnimation2(view), 10);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    private void startAnimation2(final View view) {
        if (view == null || !mIsAnimating) {
            return;
        }

        List<Animator> animatorList2 = new ArrayList<>();
        ObjectAnimator translateAnimator2 = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, -loading_view_ball_up_translation_y, loading_view_ball_down_translation_y);
        translateAnimator2.setInterpolator(slowInterpolator);
        translateAnimator2.setDuration(DURATION_2);
        animatorList2.add(translateAnimator2);

        ObjectAnimator alphaAnimator2 = ObjectAnimator.ofFloat(view, View.ALPHA, 1.0f, 0.3f);
        alphaAnimator2.setInterpolator(slowInterpolator);
        alphaAnimator2.setDuration(DURATION_2);
        animatorList2.add(alphaAnimator2);


        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animatorList2);
        animatorSet.start();
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mHandler.postDelayed(() -> startAnimation3(view), 10);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    private void startAnimation3(final View view) {
        if (view == null || !mIsAnimating) {
            return;
        }

        List<Animator> animatorList3 = new ArrayList<>();
        ObjectAnimator translateAnimator3 = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, loading_view_ball_down_translation_y, 0.0f);
        translateAnimator3.setInterpolator(slowInterpolator);
        translateAnimator3.setDuration(DURATION_3);
        animatorList3.add(translateAnimator3);

        ObjectAnimator alphaAnimator3 = ObjectAnimator.ofFloat(view, View.ALPHA, 0.3f, 0.3f);
        alphaAnimator3.setInterpolator(slowInterpolator);
        alphaAnimator3.setDuration(DURATION_3);
        animatorList3.add(alphaAnimator3);


        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animatorList3);
        animatorSet.start();
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mHandler.postDelayed(() -> startAnimation1(view), 10);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }


}
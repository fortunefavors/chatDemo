package com.xiaoyu.rtc.wrapper.view;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

public class AlphaLinearLayout extends LinearLayout {

    private ObjectAnimator mAnimatorOut;
    private boolean mIsClick = false;

    public AlphaLinearLayout(Context context) {
        super(context);
        init();
    }

    public AlphaLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AlphaLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public AlphaLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        mAnimatorOut = ObjectAnimator.ofFloat(this, "alpha", 0.3f, 1f);
        mAnimatorOut.setDuration(100);
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        mIsClick = l != null;
        super.setOnClickListener(l);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mIsClick && isEnabled()) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    this.setAlpha(0.3f);
                    break;
                case MotionEvent.ACTION_UP:
                    mAnimatorOut.start();
                    break;
                case MotionEvent.ACTION_CANCEL:
                    mAnimatorOut.start();
                    break;
            }
        }
        return super.onTouchEvent(event);
    }
}

package com.xiaoyu.rtc.wrapper.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CellRectView extends View {

    private Paint mPaint;
    private RectF mRectF = new RectF();

    private int mRectColor = ColorUtil.C30_FFFFFF;
    private int mBorderWidth = 2;

    public CellRectView(Context context) {
        super(context);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Style.STROKE);
    }

    public CellRectView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Style.STROKE);
    }

    public CellRectView(Context context, int rectColor, int borderWidth) {
        super(context);

        mRectColor = rectColor;
        mBorderWidth = borderWidth;

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mPaint.setColor(mRectColor);
        mPaint.setStrokeWidth(mBorderWidth);
        mRectF.set(0, 0, getWidth(), getHeight());
        if (mBorderWidth > 0) {
            canvas.drawRect(mRectF, mPaint);
        }
    }
}

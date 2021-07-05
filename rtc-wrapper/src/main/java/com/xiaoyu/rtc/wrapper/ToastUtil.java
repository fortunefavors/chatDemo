package com.xiaoyu.rtc.wrapper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


public class ToastUtil {
    private static ExpandToast toast;
    private static IconToast iconToast;
    private static Context mContext;
    private static float mDensity = 1.0f;

    public static void init(Context context) {
        mContext = context;
        mDensity = context.getResources().getDisplayMetrics().density;
    }


    public synchronized static void showText(String textString) {
        if (toast == null && mContext != null) {
            toast = new ExpandToast(mContext);
        }
        if (null != toast) {
            toast.show(textString);
        }
    }

    public synchronized static void showText(int textId, int milliseconds) {
        if (toast == null && mContext != null) {
            toast = new ExpandToast(mContext);
        }
        if (null != toast) {
            toast.show(textId, milliseconds);
        }
    }

    public synchronized static void showText(String textString, int milliseconds) {
        if (toast == null && mContext != null) {
            toast = new ExpandToast(mContext);
        }
        if (null != toast) {
            toast.show(textString, milliseconds);
        }
    }

    public synchronized static void showIconText(int textId, int resId) {
        if (iconToast == null && mContext != null) {
            iconToast = new IconToast(mContext, resId);
        }
        if (null != iconToast) {
            iconToast.show(textId);
        }
    }

    public synchronized static void showIconText(String textString, int resId) {
        if (iconToast == null && mContext != null) {
            iconToast = new IconToast(mContext, resId);
        }
        if (null != iconToast) {
            iconToast.show(textString);
        }
    }

    public synchronized static void showIconText(int textId, int resId, int milliseconds) {
        if (iconToast == null && mContext != null) {
            iconToast = new IconToast(mContext, resId);
        }
        if (null != iconToast) {
            iconToast.show(textId, milliseconds);
        }
    }

    public synchronized static void showIconText(String textString, int resId, int milliseconds) {
        if (iconToast == null && mContext != null) {
            iconToast = new IconToast(mContext, resId);
        }
        if (null != iconToast) {
            iconToast.show(textString, milliseconds);
        }
    }

    private static class ExpandToast {
        private static final int FLOAT_VIEW_DISPLAY_TIME = 2000;

        private Toast mToast;
        private TextView mTextView;

        ExpandToast(Context mContext) {
            mToast = new Toast(mContext);
            LayoutInflater inflater = LayoutInflater.from(mContext);
            mTextView = (TextView) inflater.inflate(R.layout.toast_view, null);
            mToast.setView(mTextView);
        }

        public void show(int textId) {
            show(textId, FLOAT_VIEW_DISPLAY_TIME);
        }

        void show(int textId, int milliseconds) {
            if (milliseconds <= 0) {
                return;
            }
            Context context = mContext;
            if (context != null) {
                String text = context.getString(textId);
                show(text, milliseconds);
            } else {
                mTextView.setText(textId);
                mToast.setDuration(milliseconds);
                mToast.show();
            }
        }

        void show(String text) {
            show(text, FLOAT_VIEW_DISPLAY_TIME);
        }

        void show(String text, int milliseconds) {
            if (milliseconds <= 0) {
                return;
            }

            int pad18 = (int) (18 * mDensity);
            int pad12 = (int) (12 * mDensity);
            text = text.replaceAll("[\r\n]", "");
            if (text.length() <= 8) {
                mTextView.setPadding(pad18, pad12, pad18, pad12);
            } else {
                mTextView.setPadding(pad18, pad18, pad18, pad18);
            }

            if (text.length() > 24) {
                mTextView.setMaxWidth((int) (206 * mDensity));
            }

            mTextView.setText(text);
            mToast.setDuration(milliseconds);
            mToast.show();
        }

    }

    private static class IconToast {
        private static final int FLOAT_VIEW_DISPLAY_TIME = 2000;

        private Toast mToast;
        private TextView mTextView;
        private ImageView mIconView;

        IconToast(Context mContext, int resId) {

            mToast = new Toast(mContext);
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View view = inflater.inflate(R.layout.toast_icon_view, null);
            mTextView = view.findViewById(R.id.textview);
            mIconView = view.findViewById(R.id.icon);
            mToast.setView(view);
            mIconView.setImageResource(resId);
        }

        void show(int textId) {
            show(textId, FLOAT_VIEW_DISPLAY_TIME);
        }

        void show(int textId, int milliseconds) {
            if (milliseconds <= 0) {
                return;
            }
            Context context = mContext;
            if (context != null) {
                String text = context.getString(textId);
                show(text, milliseconds);
            } else {
                mTextView.setText(textId);
                mToast.setDuration(milliseconds);
                mToast.show();
            }
        }

        void show(String text) {
            show(text, FLOAT_VIEW_DISPLAY_TIME);
        }

        void show(String text, int milliseconds) {
            if (milliseconds <= 0) {
                return;
            }
            text = text.replaceAll("[\r\n]", "");

            if (text.length() > 24) {
                mTextView.setMaxWidth((int) (170 * mDensity));
            }
            mTextView.setText(text);
            mToast.setDuration(milliseconds);
            mToast.show();
        }
    }
}
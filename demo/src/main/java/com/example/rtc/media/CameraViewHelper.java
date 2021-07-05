package com.example.rtc.media;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;
import java.util.logging.Logger;

public class CameraViewHelper {
    public interface CameraPreviewCallBack {
        SurfaceTexture getSurfaceTexture();
    }

    private final static Logger LOGGER = Logger.getLogger("CameraViewHelper");
    private static CameraPreviewCallBack callBack;

    public static void setPreviewCallBack(CameraPreviewCallBack callBack) {
        CameraViewHelper.callBack = callBack;
    }

    private static WeakReference<FrameLayout> currentTextureView;

    public static void previewCamera(FrameLayout view, Context context) {
        if (view == null || context == null) {
            return;
        }
        view.removeAllViews();
        TextureView textureView = new TextureView(context);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        textureView.setLayoutParams(lp);
        view.addView(textureView);
        previewCamera(textureView, false);
        currentTextureView = new WeakReference<>(view);
    }

    /**
     * 将本地预览画面绘制到指定TextureView上
     */
    public static void previewCamera(final TextureView view, boolean isCall) {
        if (view == null) {
            return;
        }
        if (isCall) {
            if (currentTextureView != null) {
                FrameLayout fl = currentTextureView.get();
                if (fl != null) {
                    fl.removeAllViews();
                }
            }
        }
        final SurfaceTexture newSt = callBack.getSurfaceTexture();
        SurfaceTexture oldSt = view.getSurfaceTexture();
        if (newSt.equals(oldSt)) {
            return;
        }
        view.setSurfaceTexture(newSt);
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                LOGGER.info("onViewAttachedToWindow " + v);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                LOGGER.info("onViewDetachedFromWindow " + v);
            }
        });
        view.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                LOGGER.info("onSurfaceTextureAvailable " + width + " " + height + " " + view);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                LOGGER.info("onSurfaceTextureDestroyed " + view);
                view.setSurfaceTextureListener(null);
                surface.setOnFrameAvailableListener(null);
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
        view.post(() -> {
            LOGGER.info("startPreview, display " + (View.VISIBLE == view.getVisibility()));
            view.setVisibility(View.VISIBLE);
            view.requestLayout();
        });
    }
}
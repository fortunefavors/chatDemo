package com.example.rtc.media;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import com.xiaoyu.camera.CameraListenerAdapter;
import com.xiaoyu.camera.CameraModule;
import com.xiaoyu.camera.ICameraModule;
import com.xiaoyu.camera.omx.CameraNativeHandler;
import com.xiaoyu.open.video.RtcVideoCapturer;
import com.xiaoyu.open.video.RtcVideoDataSource;
import com.xiaoyu.open.video.RtcVideoFilter;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class CameraService implements RtcVideoCapturer, CameraViewHelper.CameraPreviewCallBack {
    private final Logger LOGGER = Logger.getLogger("DemoCameraService");
    @SuppressLint("StaticFieldLeak")
    private static ICameraModule cameraModule;
    @SuppressLint("StaticFieldLeak")
    public static CameraService INSTANCE;

    public static void init(Context context) {
        CameraNativeHandler.init();
        INSTANCE = new CameraService(context);
        CameraViewHelper.setPreviewCallBack(INSTANCE);
    }

    private Context context;

    private CameraService(Context context) {
        this.context = context;
    }

    private RtcVideoFilter videoFilter;

    @Override
    public void onCreate(RtcVideoDataSource videoSource, RtcVideoFilter videoFilter) {
        this.videoFilter = videoFilter;
    }

    @Override
    public boolean isFrontCamera() {
        return true;
    }

    @Override
    public Integer getSensorOrientation() {
        try {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(0, info); //设备只有一颗摄像头
            return info.orientation;
        } catch (Throwable e) {
            return null;
        }
    }

    private int width;
    private int height;

    @Override
    public void onParametersUpdated(int width, int height, int frameRate) {
        LOGGER.info("onParametersUpdated: width=" + width + ", height=" + height + ", frameRate=" + frameRate);
        if (this.width != width || this.height != height) {
            if (cameraModule != null) {
                closeCamera();
                cameraModule = null;
            }
            this.width = width;
            this.height = height;
        }
        if (cameraModule == null) {
            LOGGER.info("onParametersUpdated: create cameraModule");
            cameraModule = new CameraModule(context, new CameraListenerAdapter() {
            }, this.width, this.height, null);
            if (videoFilter.isEnable()) {
                videoFilter.bindSurfaceTexture(cameraModule.getSurfaceTextureName(), cameraModule.getSurfaceTexture());
                videoFilter.setPreviewSize(width, height);
            } else {
                cameraModule.detachFromGLContext();//本地预览与TextureView绑定使用,必须调用此方法才能正常工作
            }
        }
    }


    @Override
    public void notifyVideoNoInput() {
        cameraModule.stopCamera();
        cameraModule.startCamera();
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public SurfaceTexture getSurfaceTexture() {
        return cameraModule.getSurfaceTexture();
    }

    private void openCamera() {
        cameraModule.startCamera();
    }

    private void closeCamera() {
        cameraModule.stopCamera();
    }

    private Set<String> sourceSet = new HashSet<>();

    @Override
    public void onStartCapture(String sourceId, int width, int height) {
        LOGGER.info("onStartCapture, sourceId=" + sourceId + ", width=" + width + ", height=" + height);
        if (sourceSet.add(sourceId) && sourceSet.size() == 1) {
            openCamera();
        }
    }

    @Override
    public void onStopCapture(String sourceId) {
        LOGGER.info("onStopCapture, sourceId=" + sourceId);
        if (sourceSet.remove(sourceId) && sourceSet.isEmpty()) {
            closeCamera();
        }
    }

    @Override
    public void onUpdatePreviewOrientation(int rotation) {
        LOGGER.info("onUpdatePreviewOrientation: " + rotation);
    }

    @Override
    public void onSwitchCamera() {
        LOGGER.info("onSwitchCamera");
    }

    @Override
    public void onRequestCamera() {
        LOGGER.info("onRequestCamera");
        openCamera();
    }

    @Override
    public void onReleaseCamera() {
        LOGGER.info("onReleaseCamera");
        closeCamera();
    }
}
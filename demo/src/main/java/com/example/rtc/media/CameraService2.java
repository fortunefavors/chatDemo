package com.example.rtc.media;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Range;
import android.view.Surface;

import com.xiaoyu.open.video.RtcVideoCapturer;
import com.xiaoyu.open.video.RtcVideoDataSource;
import com.xiaoyu.open.video.RtcVideoFilter;
import com.xiaoyu.open.video.RtcVideoInputExtend;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class CameraService2 implements RtcVideoCapturer, CameraViewHelper.CameraPreviewCallBack {
    private final static Logger LOGGER = Logger.getLogger("CameraService2");
    public static CameraService2 INSTANCE;
    private CameraDeviceProxy mCameraDevice;
    final Object lock = new Object();

    public static void init(Context context) {
        INSTANCE = new CameraService2(context);
        CameraViewHelper.setPreviewCallBack(INSTANCE);
    }

    private static class CameraDeviceStateCallback extends CameraDevice.StateCallback {
        private final CameraService2 capture;
        private final int requestIndex;

        private CameraDeviceStateCallback(CameraService2 capture, int requestIndex) {
            this.capture = capture;
            this.requestIndex = requestIndex;
        }

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            synchronized (capture.lock) {
                if (capture.requestIndex != requestIndex) {
                    try {
                        cameraDevice.close();
                    } catch (Exception e) {
                        LOGGER.severe("onOpened: " + e.getLocalizedMessage());
                    }
                    return;
                }
                LOGGER.info("onOpened: " + cameraDevice);
                capture.mCameraDevice = new CameraDeviceProxy(cameraDevice);
                capture.createCameraPreviewSession();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            capture.closeCamera();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            capture.closeCamera();
        }
    }

    private CameraManager cameraManager;
    private SurfaceTexture previewSurfaceTexture;
    private Handler handler;

    private CameraService2(Context context) {
        LOGGER.info("init");
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        int[] mTextures = new int[1];
        GLES20.glGenTextures(1, mTextures, 0);
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        previewSurfaceTexture = new SurfaceTexture(mTextures[0]);
        previewSurfaceTexture.detachFromGLContext();
        handler = new Handler();
    }


    @Override
    public SurfaceTexture getSurfaceTexture() {
        return previewSurfaceTexture;
    }

    private RtcVideoFilter videoFilter;

    @Override
    public void onCreate(RtcVideoDataSource videoSource, RtcVideoFilter videoFilter) {
        this.videoFilter = videoFilter;
    }

    private static int DEF_PREVIEW_WIDTH = 1280;
    private static int DEF_PREVIEW_HEIGHT = 720;

    @Override
    public void onParametersUpdated(int width, int height, int frameRate) {
        LOGGER.info("onParametersUpdated: width=" + width + ", height=" + height + ", frameRate=" + frameRate);
        if (DEF_PREVIEW_WIDTH != width || DEF_PREVIEW_HEIGHT != height) {
            DEF_PREVIEW_WIDTH = width;
            DEF_PREVIEW_HEIGHT = height;
            rest();
        }
    }

    private int mWidth = DEF_PREVIEW_WIDTH;
    private int mHeight = DEF_PREVIEW_HEIGHT;
    private String mSourceId = RtcVideoCapturer.SOURCE_ID_LOCAL_PREVIEW;

    private void rest() {
        mSourceId = RtcVideoCapturer.SOURCE_ID_LOCAL_PREVIEW;
        mWidth = DEF_PREVIEW_WIDTH;
        mHeight = DEF_PREVIEW_HEIGHT;
        resetCameraId();
    }

    void resetCameraId() {
        List<Integer> facings = new ArrayList<>();
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null) {
                    facings.add(facing);
                }
            }
        } catch (Throwable e) {
            LOGGER.severe(Log.getStackTraceString(e));
        }
        Collections.sort(facings);
        if (!facings.isEmpty()) {
            mCameraFacing = facings.get(0);
        } else {
            mCameraFacing = CameraCharacteristics.LENS_FACING_FRONT;
        }
    }

    @Override
    public void onReleaseCamera() {
        closeCamera();
        rest();
    }

    @Override
    public void onStartCapture(String sourceId, int width, int height) {
        if (mSourceId == null || !mSourceId.equals(sourceId)) {
            mSourceId = sourceId;
        }

        if (mWidth != width || mHeight != height) {
            mWidth = width;
            mHeight = height;
            closeCamera();
        }
        resetCameraId();
        LOGGER.info("onStartCapture: sourceId=" + mSourceId + ", width=" + mWidth + ", height=" + mHeight);
        onRequestCamera();
    }

    @Override
    public void onStopCapture(String sourceId) {
        LOGGER.info("onStopCapture: sourceId=" + mSourceId);
        closeCamera();
        rest();
    }

    @Override
    public void onUpdatePreviewOrientation(int rotation) {
        LOGGER.info("onUpdatePreviewOrientation: " + rotation);
    }

    @Override
    public void onDestroy() {
        LOGGER.info("onDestroy");
    }

    @Override
    public void notifyVideoNoInput() {
        LOGGER.info("notifyVideoNoInput");
        closeCamera();
        onRequestCamera();
    }

    @Override
    public void onSwitchCamera() {
        if (mCameraOpened.get()) {
            LOGGER.info("onSwitchCamera");
            closeCamera();
            switchCameraFacing();
            onRequestCamera();
        } else {
            LOGGER.severe("onSwitchCamera exit, camera not open");
        }
    }

    private void switchCameraFacing() {
        if (mCameraFacing == CameraCharacteristics.LENS_FACING_FRONT) {
            mCameraFacing = CameraCharacteristics.LENS_FACING_BACK;
        } else {
            mCameraFacing = CameraCharacteristics.LENS_FACING_FRONT;
        }
    }

    @Override
    public boolean isFrontCamera() {
        return mCameraFacing == CameraCharacteristics.LENS_FACING_FRONT;
    }

    @Override
    public Integer getSensorOrientation() {
        return mSensorOrientation;
    }

    protected void closeCamera() {
        synchronized (lock) {
            if (mCameraOpened.compareAndSet(true, false)) {
                requestIndex++;
                LOGGER.info("closeCamera");
                try {
                    if (null != mCaptureSession) {
                        mCaptureSession.close();
                        mCaptureSession = null;
                    }
                    if (null != mCameraDevice) {
                        mCameraDevice.close();
                        mCameraDevice = null;
                    }
                } catch (Exception e) {
                    LOGGER.severe(Log.getStackTraceString(e));
                }
            }
        }
    }

    private String mCameraId;
    private Range<Integer>[] mFps;
    private int mCameraFacing = CameraCharacteristics.LENS_FACING_FRONT;
    private AtomicBoolean mCameraOpened = new AtomicBoolean(false);

    private int requestIndex;
    private Integer mSensorOrientation;

    int mPreviewWidth = -1;
    int mPreviewHeight = -1;

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestCamera() {
        synchronized (lock) {
            if (!mCameraOpened.compareAndSet(false, true)) {
                return;
            }
            requestIndex++;
            LOGGER.info("onRequestCamera");
            try {
                for (String cameraId : cameraManager.getCameraIdList()) {
                    CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                    Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    if (facing == null || facing != mCameraFacing) {
                        continue;
                    }
                    StreamConfigurationMap mStreamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    if (mStreamConfigurationMap == null) {
                        continue;
                    }
                    mFps = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
                    if (mFps != null) {
                        Arrays.sort(mFps, (o1, o2) -> {
                            if (o1.getLower() > o2.getLower()) {
                                return 1;
                            } else if (o1.getLower() < o2.getLower()) {
                                return -1;
                            } else return o1.getUpper().compareTo(o2.getUpper());
                        });
                    }
                    mCameraId = cameraId;
                    mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                    /*
                    int[] formats = mStreamConfigurationMap.getOutputFormats();
                    Size[] sizes;
                    for (int format : formats) {
                        LOGGER.severe("---- supportFormat: " + format);
                        sizes = mStreamConfigurationMap.getOutputSizes(format);
                        if (sizes != null) {
                            for (Size size : sizes) {
                                LOGGER.severe("------ supportSize: " + size);
                            }
                        }
                    }
                     */
                    //TODO 这里有一个最佳分辨率的筛选过程， demo实现省略
                    mPreviewWidth = mWidth;
                    mPreviewHeight = mHeight;
                    videoFilter.setPreviewSize(mPreviewWidth, mPreviewHeight);
                    if (previewSurfaceTexture != null) {
                        previewSurfaceTexture.setDefaultBufferSize(mPreviewWidth, mPreviewHeight);
                    }
                    LOGGER.info("setPreviewSize: " + mPreviewWidth + ", height=" + mPreviewHeight);
                    LOGGER.info("mPreviewSize: mPreviewWidth=" + mPreviewWidth + ", mPreviewHeight=" + mPreviewHeight + ", mSensorOrientation=" + mSensorOrientation);
                    break;
                }
            } catch (Throwable e) {
                LOGGER.severe(Log.getStackTraceString(e));
            }
            try {
                cameraManager.openCamera(mCameraId, new CameraDeviceStateCallback(this, requestIndex), handler);
            } catch (Throwable e) {
                mCameraOpened.set(false);
                LOGGER.severe(Log.getStackTraceString(e));
            }
        }
    }

    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;
    private CameraCaptureSession mCaptureSession;

    private void createCameraPreviewSession() {
        try {
            List<Surface> surfaces = new ArrayList<>();
            //TODO 如果有本地预览的Surface，请第一个放入surfaces中
            surfaces.add(new Surface(previewSurfaceTexture));
            RtcVideoInputExtend.SurfaceInfo inputSurface = RtcVideoInputExtend.INSTANCE.getInputSurface();
            if (inputSurface.surface != null) {
                surfaces.add(inputSurface.surface);
            }
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(); //不同模式有不同的新能表现
            for (Surface surface : surfaces) {
                mPreviewRequestBuilder.addTarget(surface);
            }
            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            synchronized (lock) {
                                if (mCameraDevice == null || mCameraDevice.isClose()) {
                                    return;
                                }
                                mCaptureSession = cameraCaptureSession;
                                try {
                                    if (mFps != null) {
                                        for (Range<Integer> fps : mFps) {
                                            if (fps.getLower() == 15) {
                                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fps);
                                                LOGGER.info("onConfigured: select fps " + fps);
                                                break;
                                            }
                                        }
                                    }
                                    //mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                    //mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                                    //mPreviewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_USE_SCENE_MODE);
                                    mPreviewRequest = mPreviewRequestBuilder.build();
                                    mCaptureSession.setRepeatingRequest(mPreviewRequest, null, null);
                                } catch (CameraAccessException e) {
                                    LOGGER.severe(Log.getStackTraceString(e));
                                }
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            LOGGER.severe("onConfigureFailed: " + cameraCaptureSession);
                        }
                    }
            );
        } catch (CameraAccessException e) {
            LOGGER.severe(Log.getStackTraceString(e));
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static class CameraDeviceProxy {
        private CameraDevice device;
        private boolean isClose;

        CameraDeviceProxy(CameraDevice device) {
            this.device = device;
            this.isClose = false;
        }

        void createCaptureSession(List<Surface> outputs, CameraCaptureSession.StateCallback callback) throws CameraAccessException {
            device.createCaptureSession(outputs, callback, null);
        }

        CaptureRequest.Builder createCaptureRequest() throws CameraAccessException {
            return device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        }

        void close() {
            device.close();
            isClose = true;
        }

        boolean isClose() {
            return isClose;
        }
    }
}
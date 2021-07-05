package com.example.rtc;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.example.rtc.media.CameraViewHelper;
import com.xiaoyu.open.RtcGlobalConfig;
import com.xiaoyu.open.call.RtcCallIntent;
import com.xiaoyu.rtc.wrapper.MediaCustomKey;
import com.xiaoyu.rtc.wrapper.RtcIncomingManager;
import com.xiaoyu.rtc.wrapper.view.CallViewController;

import java.util.logging.Logger;

public class CallActivity extends Activity implements CallViewController.CallViewListener {
    private final static Logger LOGGER = Logger.getLogger("CallActivity");
    /**
     * 通话相关UI默认实现
     */
    private CallViewController viewController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RtcCallIntent callIntent = getIntent().getParcelableExtra("_key_call_intent_");
        if (callIntent == null) {
            callIntent = RtcIncomingManager.INSTANCE.poll();
        }
        if (callIntent == null) {
            finish();
            return;
        }
        RtcIncomingManager.INSTANCE.callUIEnter();
        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        win.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        setContentView(R.layout.activity_call);
        ViewGroup content = findViewById(R.id.call_content);
        if (RtcGlobalConfig.supportMultipleVideoInput()) {
            //设备模式，本地画面预览由Camera持有者完成
            viewController = new CallViewController(this, content, textureView -> CameraViewHelper.previewCamera(textureView, true));
        } else {
            //APP模式，本地画面预览由rtc内部完成
            viewController = new CallViewController(this, content);
        }
        viewController.setWorkAtApp(MediaCustomKey.APP.equals(DemoApplication.MEDIA_CUSTOM_KEY) || MediaCustomKey.PAD.equals(DemoApplication.MEDIA_CUSTOM_KEY));
        viewController.setAutoAnswer(true);
        viewController.setCallIntent(callIntent);
        viewController.setAddCalleeMembers(DemoApplication.members); //示例代码 填充可邀请用户列表

        int orientation = getRequestedOrientation();
        LOGGER.info("onCreate(" + this + "), orientation=" + orientation + ", callIndex=" + callIntent.callIndex);
        //0度
        //90度
        //180度
        //270度
        //LOGGER.severe("onOrientationChanged(" + this + "), orientation=" + orientation + ", " + screenOrientation);
        OrientationEventListener orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                    return;
                }
                int ori = orientation;
                if (orientation > 330 || orientation < 30) { //0度
                    orientation = 0;
                } else if (orientation > 60 && orientation < 120) { //90度
                    orientation = 90;
                } else if (orientation > 150 && orientation < 210) { //180度
                    orientation = 180;
                } else if (orientation > 240 && orientation < 300) { //270度
                    orientation = 270;
                } else {
                    return;
                }
                //LOGGER.severe("onOrientationChanged(" + this + "), orientation=" + orientation + ", " + screenOrientation);
                if (screenOrientation != orientation) {
                    screenOrientation = orientation;
                    int display = getWindowManager().getDefaultDisplay().getRotation();
                    LOGGER.info("onOrientationChanged(" + this + "), orientation=" + orientation + ", ori=" + ori + ", " + display);
                    switch (screenOrientation) {
                        case 90:
                            updatePreviewOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                            break;
                        case 180:
                            updatePreviewOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                            break;
                        case 270:
                            updatePreviewOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                            break;
                        default:
                            updatePreviewOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                            break;
                    }
                }
            }
        };
        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable();
        } else {
            orientationEventListener.disable();
        }
    }

    private int screenOrientation = -1;
    private int lastOrientation;

    private void updatePreviewOrientation(int orientation) {
        LOGGER.info("updatePreviewOrientation(" + this + "), orientation=" + orientation);
        this.lastOrientation = orientation;
        viewController.updateActivityScreenOrientation(orientation);
        setRequestedOrientation(orientation);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        viewController.onMoreIncomingCall();
    }

    @Override
    public synchronized void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        viewController.updateActivityScreenOrientation(lastOrientation);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        RtcIncomingManager.INSTANCE.callUIExit();
        if (viewController != null) {
            viewController.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;//吃掉所有的返回操作
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onCallFinished() {
        if (!isFinishing()) {
            finish();
        }
    }
}
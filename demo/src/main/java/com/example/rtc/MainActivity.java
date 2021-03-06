package com.example.rtc;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Toast;

import com.example.rtc.http.Get;
import com.example.rtc.http.HttpRequestProxy;
import com.example.rtc.media.AudioCapture;
import com.example.rtc.media.CameraService;
import com.example.rtc.media.CameraService2;
import com.google.gson.reflect.TypeToken;
import com.xiaoyu.open.RtcContext;
import com.xiaoyu.open.RtcContextCache;
import com.xiaoyu.open.RtcExtend;
import com.xiaoyu.open.RtcGlobalConfig;
import com.xiaoyu.open.RtcToken;
import com.xiaoyu.open.RtcUri;
import com.xiaoyu.open.audio.RtcAudioService;
import com.xiaoyu.open.call.RtcCallIntent;
import com.xiaoyu.open.call.RtcCallMode;
import com.xiaoyu.open.call.RtcCallService;
import com.xiaoyu.open.uri.RtcConferenceUri;
import com.xiaoyu.open.uri.RtcSipUri;
import com.xiaoyu.open.uri.RtcTelUri;
import com.xiaoyu.open.video.RtcVideoService;
import com.xiaoyu.rtc.wrapper.MediaCustomKey;
import com.xiaoyu.rtc.wrapper.Member;
import com.xiaoyu.rtc.wrapper.RtcIncomingManager;
import com.xiaoyu.rtc.wrapper.SharedPreferencesUtil;
import com.xiaoyu.rtc.wrapper.ToastUtil;
import com.xiaoyu.utils.JsonUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class MainActivity extends Activity implements View.OnClickListener,
        View.OnLongClickListener,
        CheckBox.OnCheckedChangeListener,
        RtcContext.Callback,
        RtcIncomingManager.CallActivityListener {
    private final static Logger LOGGER = Logger.getLogger("MainActivity");
    private RadioButton cbWorkModeApp;
    private RadioButton cbWorkModePuffer;
    private RadioButton cbWorkModePuffer1L;
    private RadioButton cbWorkModePuffer1S;
    private RadioButton cbWorkModePufferX8;
    private RadioButton cbWorkModePad;
    private RadioButton[] cbWorkModeGroup;

    private EditText etUid;
    private EditText etNick;
    private Button btUid;
    private ViewGroup areaCall;
    private EditText etConfNo;
    private EditText etSipNo;
    private EditText etTelNo;
    private EditText etCall;
    private EditText etPstn;
    private CheckBox cbPstn;
    private CheckBox cbPush;

    private void checkAndRequest(String[] permissions) {
        List<String> requestList = new ArrayList<>();
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestList.add(permission);
            }
        }
        if (!requestList.isEmpty()) {
            ActivityCompat.requestPermissions(this, requestList.toArray(new String[0]), Integer.MAX_VALUE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        LOGGER.info("onRequestPermissionsResult: requestCode=" + requestCode);
        LOGGER.info("onRequestPermissionsResult: permissions=" + Arrays.toString(permissions));
        LOGGER.info("onRequestPermissionsResult: grantResults=" + Arrays.toString(permissions));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //????????????
        checkAndRequest(new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.INTERNET,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_NETWORK_STATE
        });


        findViewById(R.id.ll_work_mode_app).setOnClickListener(this);
        cbWorkModeApp = findViewById(R.id.cb_work_mode_app);
        cbWorkModeApp.setOnCheckedChangeListener(this);

        findViewById(R.id.ll_work_mode_pf).setOnClickListener(this);
        cbWorkModePuffer = findViewById(R.id.cb_work_mode_pf);
        cbWorkModePuffer.setOnCheckedChangeListener(this);

        findViewById(R.id.ll_work_mode_1s).setOnClickListener(this);
        cbWorkModePuffer1S = findViewById(R.id.cb_work_mode_1s);
        cbWorkModePuffer1S.setOnCheckedChangeListener(this);

        findViewById(R.id.ll_work_mode_1l).setOnClickListener(this);
        cbWorkModePuffer1L = findViewById(R.id.cb_work_mode_1l);
        cbWorkModePuffer1L.setOnCheckedChangeListener(this);

        findViewById(R.id.ll_work_mode_x8).setOnClickListener(this);
        cbWorkModePufferX8 = findViewById(R.id.cb_work_mode_x8);
        cbWorkModePufferX8.setOnCheckedChangeListener(this);

        findViewById(R.id.ll_work_mode_pad).setOnClickListener(this);
        cbWorkModePad = findViewById(R.id.cb_work_mode_pad);
        cbWorkModePad.setOnCheckedChangeListener(this);

        cbWorkModeGroup = new RadioButton[]{cbWorkModeApp, cbWorkModePuffer, cbWorkModePuffer1S, cbWorkModePuffer1L, cbWorkModePufferX8, cbWorkModePad};

        etUid = findViewById(R.id.et_uid);
        btUid = findViewById(R.id.bt_uid);
        etNick = findViewById(R.id.et_nick);
        btUid.setOnClickListener(this);
        areaCall = findViewById(R.id.area_call);
        etConfNo = findViewById(R.id.et_conf_no);
        findViewById(R.id.bt_conf_no).setOnClickListener(this);
        etSipNo = findViewById(R.id.et_sip_no);
        findViewById(R.id.bt_sip_no).setOnClickListener(this);
        etTelNo = findViewById(R.id.et_tel_no);
        findViewById(R.id.bt_tel_no).setOnClickListener(this);
        etCall = findViewById(R.id.et_call);
        etPstn = findViewById(R.id.et_pstn);
        cbPstn = findViewById(R.id.cb_pstn);
        cbPush = findViewById(R.id.cb_push);
        Button btCall = findViewById(R.id.bt_call);
        btCall.setOnClickListener(this);
        loadCache();
        disabledWorkModeGroup();
    }

    private static class Callee {
        String uid;
        String tel;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Callee callee = (Callee) o;
            return Objects.equals(uid, callee.uid);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uid);
        }

    }

    private List<Callee> calleeList = new ArrayList<>();

    private void loadCache() {
        String uid = SharedPreferencesUtil.INSTANCE.getStringValue("plt_media_key", "");
        if (TextUtils.isEmpty(uid)) {
            changeWorkMode(MediaCustomKey.APP);
        } else {
            changeWorkMode(MediaCustomKey.valueOf(uid));
        }
        uid = SharedPreferencesUtil.INSTANCE.getStringValue("plt_login_uid", "");
        etUid.setText(uid);
        uid = SharedPreferencesUtil.INSTANCE.getStringValue("plt_login_nick", "");
        etNick.setText(uid);
        uid = SharedPreferencesUtil.INSTANCE.getStringValue("plt_conf_no", "");
        etConfNo.setText(uid);
        uid = SharedPreferencesUtil.INSTANCE.getStringValue("plt_sip_no", "");
        etSipNo.setText(uid);
        uid = SharedPreferencesUtil.INSTANCE.getStringValue("plt_tel_no", "");
        etTelNo.setText(uid);
        //?????????????????????
        uid = SharedPreferencesUtil.INSTANCE.getStringValue("plt_callee_list", "");
        if (!TextUtils.isEmpty(uid)) {
            try {
                List<Callee> list = JsonUtil.toObject(uid, new TypeToken<List<Callee>>() {
                }.getType());
                calleeList.clear();
                calleeList.addAll(list);
                for (Callee callee : calleeList) {
                    fillCalleeArea(callee);
                }
                resetCache(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void resetCache(boolean update) {
        DemoApplication.members.clear();
        Member member;
        for (Callee callee : calleeList) {
            member = new Member();
            member.uri = callee.uid;
            member.no = callee.tel;
            member.nick = callee.uid;
            DemoApplication.members.add(member);
        }
        if (update) {
            SharedPreferencesUtil.INSTANCE.putString("plt_callee_list", JsonUtil.toString(calleeList)).apply();
        }
    }

    private void fillCalleeArea(Callee callee) {
        Button view = (Button) getLayoutInflater().inflate(R.layout.callee_item, areaCall, false);
        view.setId(R.id.custom_id_0);
        String text = callee.uid;
        if (!TextUtils.isEmpty(callee.tel)) {
            text += " && " + callee.tel;
        }
        view.setText(text);
        view.setTag(callee);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 5, 0, 0);
        view.setLayoutParams(params);
        view.setOnClickListener(this);
        view.setOnLongClickListener(this);
        areaCall.addView(view);
    }

    @Override
    protected void onResume() {
        super.onResume();
        disabledWorkModeGroup();
    }

    @Override
    public void onClick(View v) {
        int vid = v.getId();
        switch (v.getId()) {
            case R.id.bt_uid:
                access();
                break;
            case R.id.bt_conf_no:
                makeCallConf();
                break;
            case R.id.bt_sip_no:
                makeSipCall();
                break;
            case R.id.bt_tel_no:
                makeTelCall();
                break;
            case R.id.bt_call:
                addCallee();
                break;
            case R.id.custom_id_0:
                makeCall((Callee) v.getTag());
                break;
            default: {
                if (vid == R.id.ll_work_mode_app) {
                    cbWorkModeApp.performClick();
                } else if (vid == R.id.ll_work_mode_pf) {
                    cbWorkModePuffer.performClick();
                } else if (vid == R.id.ll_work_mode_1s) {
                    cbWorkModePuffer1S.performClick();
                } else if (vid == R.id.ll_work_mode_1l) {
                    cbWorkModePuffer1L.performClick();
                } else if (vid == R.id.ll_work_mode_x8) {
                    cbWorkModePufferX8.performClick();
                } else if (vid == R.id.ll_work_mode_pad) {
                    cbWorkModePad.performClick();
                }
            }
            break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v.getId() == R.id.custom_id_0) {
            calleeList.remove(v.getTag());
            areaCall.removeView(v);
            resetCache(true);
        }
        return true;
    }

    private String uid;

    private boolean loginOut = false;
    private RtcToken rtcToken;

    /**
     * ??????/??????
     */
    private synchronized void access() {
        Object tag = etUid.getTag();
        if (tag == null || (boolean) tag) {
            uid = etUid.getText().toString().trim();
            if (TextUtils.isEmpty(uid)) {
                Toast.makeText(this, "uid??????????????????????????????", Toast.LENGTH_LONG).show();
                return;
            }
            etUid.setText(uid);
            final String nick = etNick.getText().toString().trim();
            etNick.setText(nick);
            SharedPreferencesUtil.INSTANCE.putString("plt_login_uid", uid).putString("plt_login_nick", nick).apply();
            etUid.setEnabled(false);
            etNick.setEnabled(false);

            //??????uid ??????token, ???????????????????????????????????????????????????????????????????????????
            Get tokenGet = new Get(DemoApplication.getTokenUri(DemoApplication.APP_ID, uid), HttpRequestProxy.interceptor);
            btUid.setEnabled(false);
            tokenGet.asyncUI(response -> {
                btUid.setEnabled(true);
                if (response.success) {
                    DemoApplication.changeMediaCustomKey = false;
                    disabledWorkModeGroup();
                    LOGGER.info("token response: " + response.content);
                    //????????????????????????token
                    Token token = JsonUtil.toObject(response.content, Token.class);
                    //??????RtcToken
                    rtcToken = new RtcToken(DemoApplication.APP_ID, uid, token.token);
                    //??????RtcExtend
                    RtcExtend rtcExtend = new RtcExtend("plt_demo");
                    rtcExtend.displayName = nick;//???????????????????????????
                    //mediaCustomKey??????????????????????????????????????????APP???????????????Rtc????????????????????????(MediaCustomKey??????????????????????????????
                    rtcExtend.mediaCustomKey = DemoApplication.MEDIA_CUSTOM_KEY.getMediaCustomKey();
                    //???????????????mediaCustomKey???appId=1005????????????????????????appId??????????????????
                    if ("1005".equals(DemoApplication.APP_ID) && !"APP".equals(rtcExtend.mediaCustomKey)) {
                        rtcExtend.mediaCustomKey = "a91b7a09_" + rtcExtend.mediaCustomKey;
                    }
                    try {
                        //????????????Camera?????????????????????; ??????APP?????????????????????
                        Camera.CameraInfo info = new Camera.CameraInfo();
                        Camera.getCameraInfo(0, info);
                        rtcExtend.rotation = info.orientation;
                    } catch (Exception e) {
                        LOGGER.severe(Log.getStackTraceString(e));
                    }
                    //???????????????RtcContext, DuMiRtcContext?????????????????????????????????
                    RtcContext.Builder.create(getApplicationContext())
                            .setCallback(this) //??????????????????????????????????????????
                            .setToken(rtcToken)
                            .setExtend(rtcExtend)
                            .enableConsoleLog(true)
                            .build();
                    etUid.setTag(false);
                    btUid.setText("?????????: " + rtcToken.uri.getUid());
                    loginOut = false;
                } else {
                    etUid.setEnabled(true);
                    etNick.setEnabled(true);
                    LOGGER.info("token response: " + response.code + ", " + response.exception);
                    Toast.makeText(this, "token????????????", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            loginOut = true;
            areaCall.setVisibility(View.INVISIBLE);
            RtcContext context = RtcContextCache.get();
            if (context != null) {
                context.reset();//??????????????????
            }
            etUid.setEnabled(true);
            etUid.setTag(true);
            etNick.setEnabled(true);
            btUid.setText("??????");
        }
    }

    private void addCallee() {
        String calleeUid = etCall.getText().toString();
        if (TextUtils.isEmpty(calleeUid)) {
            Toast.makeText(this, "?????????uid??????????????????????????????", Toast.LENGTH_LONG).show();
            return;
        }
        calleeUid = calleeUid.trim();
        if (TextUtils.isEmpty(calleeUid)) {
            Toast.makeText(this, "?????????uid??????????????????????????????", Toast.LENGTH_LONG).show();
            return;
        }
        Callee callee = new Callee();
        callee.uid = calleeUid;
        if (calleeList.contains(callee) || calleeUid.equals(uid)) {
            Toast.makeText(this, "????????? " + calleeUid + " ??????????????????????????????", Toast.LENGTH_LONG).show();
            return;
        }
        callee.tel = etPstn.getText().toString().trim();

        calleeList.add(callee);
        fillCalleeArea(callee);
        resetCache(true);
    }

    private void makeCallConf() {
        String confNo = etConfNo.getText().toString();
        if (TextUtils.isEmpty(confNo)) {
            Toast.makeText(this, "?????????????????????????????????????????????", Toast.LENGTH_LONG).show();
            return;
        }
        confNo = confNo.trim();
        if (TextUtils.isEmpty(confNo)) {
            Toast.makeText(this, "?????????????????????????????????????????????", Toast.LENGTH_LONG).show();
            return;
        }
        SharedPreferencesUtil.INSTANCE.putString("plt_conf_no", confNo).apply();
        RtcCallIntent intent = RtcCallIntent.createForMakeCall(RtcCallMode.CallMode_AudioVideo, new RtcConferenceUri(confNo));
        intent.enablePush(cbPush.isChecked());
        Intent _intent = new Intent(this, CallActivity.class);
        _intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        _intent.putExtra("_key_call_intent_", (Parcelable) intent);
        startActivity(_intent);
    }

    private void makeSipCall() {
        String sipNo = etSipNo.getText().toString();
        if (TextUtils.isEmpty(sipNo)) {
            Toast.makeText(this, "SIP????????????????????????????????????", Toast.LENGTH_LONG).show();
            return;
        }
        sipNo = sipNo.trim();
        if (TextUtils.isEmpty(sipNo)) {
            Toast.makeText(this, "SIP????????????????????????????????????", Toast.LENGTH_LONG).show();
            return;
        }
        SharedPreferencesUtil.INSTANCE.putString("plt_sip_no", sipNo).apply();
        RtcCallIntent intent = RtcCallIntent.createForMakeCall(RtcCallMode.CallMode_AudioOnly, new RtcSipUri(sipNo));
        intent.enablePush(cbPush.isChecked());
        Intent _intent = new Intent(this, CallActivity.class);
        _intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        _intent.putExtra("_key_call_intent_", (Parcelable) intent);
        startActivity(_intent);
    }

    private void makeTelCall() {
        String telNo = etTelNo.getText().toString();
        if (TextUtils.isEmpty(telNo)) {
            Toast.makeText(this, "TEL????????????????????????????????????", Toast.LENGTH_LONG).show();
            return;
        }
        telNo = telNo.trim();
        if (TextUtils.isEmpty(telNo)) {
            Toast.makeText(this, "TEL????????????????????????????????????", Toast.LENGTH_LONG).show();
            return;
        }
        SharedPreferencesUtil.INSTANCE.putString("plt_tel_no", telNo).apply();
        RtcCallIntent intent = RtcCallIntent.createForMakeCall(RtcCallMode.CallMode_Tel, new RtcTelUri(telNo));
        intent.enablePush(cbPush.isChecked());
        Intent _intent = new Intent(this, CallActivity.class);
        _intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        _intent.putExtra("_key_call_intent_", (Parcelable) intent);
        startActivity(_intent);
    }

    private void makeCall(Callee callee) {
        RtcCallIntent intent = RtcCallIntent.createForMakeCall(RtcCallMode.CallMode_AudioVideo, new RtcUri(callee.uid));
        if (cbPstn.isChecked()) {
            if (TextUtils.isEmpty(callee.tel)) {
                intent.autoPSTN();
            } else {
                intent.autoPSTN(callee.tel);
            }
        }
        intent.enablePush(cbPush.isChecked());
        Intent _intent = new Intent(this, CallActivity.class);
        _intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        _intent.putExtra("_key_call_intent_", (Parcelable) intent);
        startActivity(_intent);
    }

    //*************************** RtcContext.Callback ********************************
    @Override
    public void onPreInit(Context context) {
        //????????????????????????rtc????????????????????????
        //rtc??????????????????????????????,??????????????????
    }

    @Override
    public void onPostInit(Context context, RtcContext rtcContext) {
        //????????????????????????rtc??????????????????????????????
        //??????context??????,??????????????????
        RtcContextCache.set(rtcContext);
        //???????????????????????????
        RtcIncomingManager.INSTANCE.setCallActivityListener(this);

        RtcCallService callService = rtcContext.getCallService();
        //?????????????????????
        callService.setIncomingCallListener(RtcIncomingManager.INSTANCE);
        //??????????????????/?????????????????????
        callService.setObservedChangedListener(RtcIncomingManager.INSTANCE);

        RtcAudioService audioService = rtcContext.getAudioService();
        //???????????????????????????
        audioService.setAudioInputListener(() -> ToastUtil.showText("audio no input"));
        switch (DemoApplication.MEDIA_CUSTOM_KEY) {
            case APP:
                //app???????????????????????????????????????????????????????????????????????????????????????
                audioService.enableHeadsetManager();
                //APP?????????????????????????????????
                //APP????????????????????????????????????AudioManager.STREAM_VOICE_CALL
                break;
            case PUFFER:
            case PUFFER1S_1C:
            case PUFFER1L:
            case PUFFERX8:
            case PAD:
                //????????????????????????????????????
                //????????????????????????????????????
                audioService.setAudioInputSampleInfo(16000, 2, 0, 20);
                //?????????????????????????????????????????????
                audioService.setAudioCapturer(new AudioCapture(1001, AudioFormat.CHANNEL_IN_STEREO, false));
                //?????????????????????????????????????????????
                //audioService.setAudioRenderer(new AudioRenderer(true, false));
                //?????????????????????????????????????????????
                audioService.setAudioOutputStreamType(AudioManager.STREAM_MUSIC);
                //????????????????????????????????????
                //audioService.setAudioOutputSampleInfo(16000, 1);
                break;
            default: {
                throw new RuntimeException("no explicit device type specified");
            }
        }

        RtcVideoService videoService = rtcContext.getVideoService();
        //???????????????????????????
        videoService.setVideoInputListener(() -> ToastUtil.showText("video no input"));

        //??????????????????????????????
        if (RtcGlobalConfig.supportMultipleVideoInput()) {
            //?????????????????????????????????
            CameraService.init(context);
            videoService.setVideoCapture(CameraService.INSTANCE);
        } else if (MediaCustomKey.PAD.equals(DemoApplication.MEDIA_CUSTOM_KEY)) {
            //?????????????????????????????????
            CameraService2.init(context);
            videoService.setVideoCapture(CameraService2.INSTANCE);
        }
    }

    //*************************** RtcAuthListener ********************************
    @Override
    public synchronized void onAuthStatusChanged(AuthStatus status) {
        if (status.success) {
            if (loginOut) { //????????????????????????/????????????demo UI?????????
                RtcContext context = RtcContextCache.get();
                if (context != null) {
                    context.reset();//??????????????????
                }
                return;
            }
            btUid.setText("??????: " + rtcToken.uri.getUid());
            Toast.makeText(this, "????????????", Toast.LENGTH_LONG).show();
            areaCall.setVisibility(View.VISIBLE);
        } else {
            if (AuthFailCode.NO_ERROR.equals(status.failCode)) {
                Toast.makeText(this, "????????????: " + status.failCode, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "????????????: " + status.failCode, Toast.LENGTH_LONG).show();
            }
            areaCall.setVisibility(View.INVISIBLE);
            etUid.setEnabled(true);
            etUid.setTag(true);
            btUid.setText("??????");
        }
    }

    //***************************  RtcIncomingCallListener ********************************
    @Override
    public void onStartCallActivity(RtcCallIntent intent) {
        if (intent != null && intent.callMode == RtcCallMode.CallMode_Observed) {
            RtcIncomingManager.INSTANCE.remove(intent.callIndex);//?????????????????????
            RtcContextCache.get().getCallService().answerCall(intent.callIndex, false, intent.callMode);
            ToastUtil.showText(intent.peerName + " ??????????????????");
            return;
        }
        Intent _intent = new Intent(this, CallActivity.class);
        _intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(_intent);
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int vid = buttonView.getId();
        if (vid == R.id.cb_work_mode_app) {
            if (isChecked) {
                changeWorkMode(MediaCustomKey.APP);
            }
        } else if (vid == R.id.cb_work_mode_pf) {
            if (isChecked) {
                changeWorkMode(MediaCustomKey.PUFFER);
            }
        } else if (vid == R.id.cb_work_mode_1s) {
            if (isChecked) {
                changeWorkMode(MediaCustomKey.PUFFER1S_1C);
            }
        } else if (vid == R.id.cb_work_mode_1l) {
            if (isChecked) {
                changeWorkMode(MediaCustomKey.PUFFER1L);
            }
        } else if (vid == R.id.cb_work_mode_x8) {
            if (isChecked) {
                changeWorkMode(MediaCustomKey.PUFFERX8);
            }
        } else if (vid == R.id.cb_work_mode_pad) {
            if (isChecked) {
                changeWorkMode(MediaCustomKey.PAD);
            }
        }
    }

    private void changeWorkMode(MediaCustomKey mode) {
        checkBoxGroup(mode.ordinal() + 1, cbWorkModeGroup);
        SharedPreferencesUtil.INSTANCE.putString("plt_media_key", mode.name()).apply();
        DemoApplication.MEDIA_CUSTOM_KEY = mode;
    }

    private void checkBoxGroup(int index, RadioButton[] group) {
        for (RadioButton box : group) {
            box.setChecked(false);
        }
        group[index - 1].setChecked(true);
    }

    private void disabledWorkModeGroup() {
        if (DemoApplication.changeMediaCustomKey) {
            ViewGroup parent;
            for (View view : cbWorkModeGroup) {
                parent = (ViewGroup) view.getParent();
                for (int i = 0; i < parent.getChildCount(); i++) {
                    parent.getChildAt(i).setEnabled(true);
                }
            }
        } else {
            ViewGroup parent;
            for (View view : cbWorkModeGroup) {
                parent = (ViewGroup) view.getParent();
                for (int i = 0; i < parent.getChildCount(); i++) {
                    parent.getChildAt(i).setEnabled(false);
                }
            }
        }
    }
}

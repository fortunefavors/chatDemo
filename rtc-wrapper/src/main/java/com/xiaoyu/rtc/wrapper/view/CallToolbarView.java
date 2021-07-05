package com.xiaoyu.rtc.wrapper.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.xiaoyu.open.RtcContextCache;
import com.xiaoyu.open.audio.RtcAudioOutputListener;
import com.xiaoyu.open.call.RtcCallMode;
import com.xiaoyu.rtc.wrapper.R;
import com.xiaoyu.rtc.wrapper.ToastUtil;

/**
 * 通话中功能按钮布局封装
 */
@SuppressLint("ViewConstructor")
public class CallToolbarView extends RelativeLayout implements View.OnClickListener, View.OnLongClickListener {

    private IUserActionListener userActionListener;

    private ImageView callModeImg;
    private TextView callModeTxt;
    private ImageView audioMuteImg;
    private ImageView recordImg;
    private TextView recordTxt;
    private TextView tvCamera;
    private TextView tvVideoMute;
    private ImageView ivAudioOutMute;

    private View vAudioFocus;
    private ImageView ivAudioFocus;
    private TextView tvAudioFocus;

    private View vOutputDevice;
    private ImageView ivOutputDevice;
    private TextView tvOutputDevice;

    public CallToolbarView(Context context, RtcCallMode callMode, IUserActionListener userActionListener) {
        super(context);
        this.userActionListener = userActionListener;

        onCreateView(context, callMode);
    }

    protected void onCreateView(Context context, RtcCallMode callMode) {
        inflate(context, R.layout.call_toolbar_view, this);

        callModeImg = findViewById(R.id.switch_call_mode_img);
        callModeTxt = findViewById(R.id.switch_call_mode_txt);
        audioMuteImg = findViewById(R.id.audio_mute_img);

        View recordArea = findViewById(R.id.recording);
        recordArea.setTag(true);
        recordArea.setOnClickListener(this);
        recordImg = findViewById(R.id.recording_img);
        recordTxt = findViewById(R.id.recording_txt);

        ivAudioOutMute = findViewById(R.id.iv_switch_out_mute);
        View outMute = findViewById(R.id.switch_out_mute);
        outMute.setOnClickListener(this);

        vAudioFocus = findViewById(R.id.switch_focus);
        vAudioFocus.setVisibility(INVISIBLE);
        vAudioFocus.setTag(false);
        vAudioFocus.setOnClickListener(this);
        ivAudioFocus = findViewById(R.id.iv_switch_focus);
        tvAudioFocus = findViewById(R.id.tv_switch_focus);

        ivOutputDevice = findViewById(R.id.iv_switch_output_device);
        tvOutputDevice = findViewById(R.id.tv_switch_output_device);
        vOutputDevice = findViewById(R.id.switch_output_device);
        vOutputDevice.setTag(false);
        vOutputDevice.setOnClickListener(this);
        vOutputDevice.setOnLongClickListener(this);

        findViewById(R.id.switch_call_mode).setOnClickListener(this);
        findViewById(R.id.audio_mute).setOnClickListener(this);
        View view = findViewById(R.id.switch_camera);
        view.setTag(true);
        view.setOnClickListener(this);
        tvCamera = findViewById(R.id.tv_switch_camera);
        findViewById(R.id.huang_up).setOnClickListener(this);
        findViewById(R.id.forward_call).setOnClickListener(this);
        findViewById(R.id.add_other).setOnClickListener(this);
        findViewById(R.id.buzzer).setOnClickListener(this);
        findViewById(R.id.mute_video).setOnClickListener(this);
        tvVideoMute = findViewById(R.id.tv_mute_video);
        didChangeCallMode(callMode);
        if (callMode == RtcCallMode.CallMode_Observer) {
            outMuteCount = -1;
            outMute.setTag(false);
        } else {
            outMuteCount = 0;
            outMute.setTag(true); //保证不会被mute
        }
        outMute.performClick();
    }

    private int outMuteCount;

    protected void didChangeCallMode(RtcCallMode callMode) {
        if (RtcCallMode.CallMode_Observer == callMode) {
            findViewById(R.id.switch_call_mode).setVisibility(GONE);
            findViewById(R.id.audio_mute).setVisibility(GONE);
            findViewById(R.id.switch_camera).setVisibility(GONE);
            findViewById(R.id.add_other).setVisibility(GONE);
            findViewById(R.id.recording).setVisibility(GONE);
            findViewById(R.id.buzzer).setVisibility(GONE);

            findViewById(R.id.forward_call).setVisibility(VISIBLE);
        } else if (RtcCallMode.CallMode_AudioOnly == callMode) {
            callModeImg.setImageResource(R.drawable.voice_answer);
            callModeTxt.setText(R.string.voice_model);
        } else {
            callModeImg.setImageResource(R.drawable.video_answer);
            callModeTxt.setText(R.string.video_model);
        }
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.switch_camera) {
            boolean tag = (boolean) v.getTag();
            v.setTag(!tag);
            tvCamera.setText(userActionListener.switchCamera(tag));
        } else if (viewId == R.id.switch_call_mode) {
            RtcCallMode callMode = userActionListener.switchCallMode();
            didChangeCallMode(callMode);
        } else if (viewId == R.id.audio_mute) {
            boolean mute = userActionListener.switchAudioMute();
            if (mute) {
                audioMuteImg.setImageResource(R.drawable.open_voice);
            } else {
                audioMuteImg.setImageResource(R.drawable.audio_mute);
            }
        } else if (viewId == R.id.huang_up) {
            userActionListener.dropCall();
        } else if (viewId == R.id.add_other) {
            userActionListener.onAddConferee();
        } else if (viewId == R.id.buzzer) {
            userActionListener.sendBuzzer();
        } else if (viewId == R.id.recording) {
            boolean tag = (boolean) v.getTag();
            if (tag) {
                recordTxt.setText(R.string.stop_recording);
                recordImg.setImageResource(R.drawable.stop_record);
                userActionListener.startRecording();
            } else {
                userActionListener.stopRecording();
                recordTxt.setText(R.string.recording);
                recordImg.setImageResource(R.drawable.start_record);
            }
            v.setTag(!tag);
        } else if (viewId == R.id.forward_call) {

            findViewById(R.id.switch_call_mode).setVisibility(VISIBLE);
            findViewById(R.id.audio_mute).setVisibility(VISIBLE);
            findViewById(R.id.switch_camera).setVisibility(VISIBLE);
            findViewById(R.id.add_other).setVisibility(VISIBLE);
            findViewById(R.id.recording).setVisibility(VISIBLE);
            findViewById(R.id.buzzer).setVisibility(VISIBLE);

            findViewById(R.id.forward_call).setVisibility(GONE);
            userActionListener.forwardCall();
            if (outMuteCount == 0) {
                findViewById(R.id.switch_out_mute).performClick();
            }
        } else if (viewId == R.id.switch_out_mute) {
            outMuteCount++;
            boolean tag = (Boolean) v.getTag();
            v.setTag(!tag);
            if (tag) {
                ivAudioOutMute.setImageResource(R.drawable.focus_on);
                RtcContextCache.get().getAudioService().enableSpeaker(true);
            } else {
                ivAudioOutMute.setImageResource(R.drawable.focus_off);
                RtcContextCache.get().getAudioService().enableSpeaker(false);
            }
        } else if (viewId == R.id.switch_focus) {
            boolean tag = (Boolean) v.getTag();
            v.setTag(!tag);
            userActionListener.recoverAudio();
        } else if (viewId == R.id.switch_output_device) {
            boolean tag = (Boolean) v.getTag();
            userActionListener.setSpeakerphoneOn(tag);
        } else if (viewId == R.id.mute_video) {
            if (userActionListener.switchVideoMute()) {
                tvVideoMute.setText("VUnMute");
            } else {
                tvVideoMute.setText("VMute");
            }
        }
    }

    void updateOutputDevice(RtcAudioOutputListener.OutputDevice device) {
        if (RtcAudioOutputListener.OutputDevice.SPEAKER.equals(device)) {
            ToastUtil.showText("扬声器模式");
            vOutputDevice.setTag(false);
            ivOutputDevice.setImageResource(R.drawable.focus_on);
            tvOutputDevice.setText(R.string.speaker);
        } else if (RtcAudioOutputListener.OutputDevice.EARPIECE.equals(device)) {
            ToastUtil.showText("听筒模式");
            vOutputDevice.setTag(true);
            ivOutputDevice.setImageResource(R.drawable.focus_off);
            tvOutputDevice.setText(R.string.earpiece);
        } else if (RtcAudioOutputListener.OutputDevice.BLUETOOTH.equals(device)) {
            ToastUtil.showText("蓝牙耳机模式");
            vOutputDevice.setTag(true);
            ivOutputDevice.setImageResource(R.drawable.focus_off);
            tvOutputDevice.setText(R.string.bluetooth);
        } else {
            ToastUtil.showText("耳机模式");
            vOutputDevice.setTag(true);
            ivOutputDevice.setImageResource(R.drawable.focus_off);
            tvOutputDevice.setText(R.string.headset);
        }
    }

    void onAudioFocusChanged(RtcAudioOutputListener.OutputFocus focus) {
        switch (focus) {
            case GAIN:
                vAudioFocus.setVisibility(INVISIBLE);
                break;
            case LOSS_TRANSIENT:
                vAudioFocus.setVisibility(VISIBLE);
                tvAudioFocus.setText(focus.name());
                tvAudioFocus.setTextColor(Color.YELLOW);
                break;
            case LOSS:
                vAudioFocus.setVisibility(VISIBLE);
                tvAudioFocus.setText(focus.name());
                tvAudioFocus.setTextColor(Color.RED);
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.switch_output_device) {
            userActionListener.setSpeakerphoneOn(null);
        }
        return true;
    }

    public void updateAudioMutedBtn(boolean isAudioMute) {
        audioMuteImg.setImageResource(isAudioMute ? R.drawable.open_voice : R.drawable.audio_mute);
    }

    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = focusChange -> {

    };
}
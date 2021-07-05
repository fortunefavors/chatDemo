package com.xiaoyu.rtc.wrapper.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.xiaoyu.open.call.RtcCallIntent;
import com.xiaoyu.open.call.RtcCallMode;
import com.xiaoyu.rtc.wrapper.R;

/**
 * 呼出中功能按钮布局封装
 */
@SuppressLint("ViewConstructor")
public class CallOutgoingView extends RelativeLayout implements View.OnClickListener {
    private RtcCallIntent callIntent;
    private ImageView callModeIcon;
    private TextView callModeText;
    private TextView tvCamera;
    private IUserActionListener userActionListener;

    public CallOutgoingView(Context context, RtcCallIntent intent, IUserActionListener userActionListener) {
        super(context);
        this.callIntent = intent;
        this.userActionListener = userActionListener;
        onCreateView(context);
    }

    protected void onCreateView(Context context) {
        inflate(context, R.layout.call_outgoing_view, this);

        TextView peerName = findViewById(R.id.call_peer_name);
        if (!TextUtils.isEmpty(callIntent.peerName)) {
            peerName.setText(callIntent.peerName);
        } else if (!TextUtils.isEmpty(callIntent.peerNumber)) {
            peerName.setText(callIntent.peerNumber);
        } else {
            peerName.setText(callIntent.peerUri.getUid());
        }

        callModeIcon = findViewById(R.id.call_mode_icon);
        callModeText = findViewById(R.id.call_mode_text);
        findViewById(R.id.pstn_ly).setOnClickListener(this);
        findViewById(R.id.call_mode_ly).setOnClickListener(this);
        findViewById(R.id.hang_up_ly).setOnClickListener(this);
        View view = findViewById(R.id.switch_camera);
        view.setTag(true);
        view.setOnClickListener(this);
        tvCamera = findViewById(R.id.tv_switch_camera);

        didChangeCallMode(callIntent.callMode);
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.call_mode_ly) {
            RtcCallMode callMode = userActionListener.switchCallMode();
            didChangeCallMode(callMode);
        } else if (viewId == R.id.hang_up_ly) {
            userActionListener.dropCall();
        } else if (viewId == R.id.pstn_ly) {
            userActionListener.forwardPSTN();
        } else if (viewId == R.id.switch_camera) {
            boolean tag = (boolean) v.getTag();
            v.setTag(!tag);
            tvCamera.setText(userActionListener.switchCamera(tag));
        }
    }

    protected void didChangeCallMode(RtcCallMode callMode) {
        if (RtcCallMode.CallMode_AudioOnly == callMode) {
            callModeIcon.setImageResource(R.drawable.voice_answer);
            callModeText.setText(R.string.voice_model);
        } else {
            callModeIcon.setImageResource(R.drawable.video_answer);
            callModeText.setText(R.string.video_model);
        }
    }
}

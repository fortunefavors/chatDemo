package com.xiaoyu.rtc.wrapper.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.xiaoyu.open.call.RtcCallIntent;
import com.xiaoyu.open.call.RtcCallMode;
import com.xiaoyu.rtc.wrapper.R;

/**
 * 呼入中功能按钮布局封装
 */
@SuppressLint("ViewConstructor")
public class CallIncomingView extends RelativeLayout implements View.OnClickListener {
    private RtcCallIntent callIntent;
    private IUserActionListener userActionListener;

    public CallIncomingView(Context context, RtcCallIntent intent, IUserActionListener userActionListener) {
        super(context);
        this.callIntent = intent;
        this.userActionListener = userActionListener;

        onCreateView(context);
    }

    protected void onCreateView(Context context) {
        inflate(context, R.layout.call_incoming_view, this);

        TextView peerName = findViewById(R.id.call_income_name_text);
        if (!TextUtils.isEmpty(callIntent.peerName)) {
            peerName.setText(callIntent.peerName);
        } else if (!TextUtils.isEmpty(callIntent.peerNumber)) {
            peerName.setText(callIntent.peerNumber);
        } else {
            peerName.setText(callIntent.peerUri.getUid());
        }

        TextView offerTips = findViewById(R.id.income_invite_text);
        if (RtcCallMode.CallMode_AudioOnly == callIntent.callMode) {
            offerTips.setText(R.string.call_income_invite_voice);
        } else {
            offerTips.setText(R.string.call_income_invite);
        }

        findViewById(R.id.hang_up_ly).setOnClickListener(this);
        findViewById(R.id.voice_accept_ly).setOnClickListener(this);
        findViewById(R.id.accept_ly).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.hang_up_ly) {
            userActionListener.rejectIncomingCall(callIntent.callIndex, "APP_BUSINESS_REASON");
        } else if (viewId == R.id.voice_accept_ly) {
            userActionListener.answerCall(callIntent.callIndex, RtcCallMode.CallMode_AudioOnly, false);
        } else if (viewId == R.id.accept_ly) {
            userActionListener.answerCall(callIntent.callIndex, RtcCallMode.CallMode_AudioVideo, false);
        }
    }
}

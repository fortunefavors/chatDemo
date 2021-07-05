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
 * 通话中收到呼入时功能按钮布局封装
 */
@SuppressLint("ViewConstructor")
public class CallIncomingMoreView extends RelativeLayout implements View.OnClickListener {

    private RtcCallIntent callIntent;
    private IUserActionListener userActionListener;

    public CallIncomingMoreView(Context context, RtcCallIntent intent, IUserActionListener userActionListener) {
        super(context);
        this.callIntent = intent;
        this.userActionListener = userActionListener;

        onCreateView(context);
    }

    protected void onCreateView(Context context) {
        inflate(context, R.layout.call_incoming_more_view, this);

        TextView peerName = findViewById(R.id.more_call_income_name_text);
        if (!TextUtils.isEmpty(callIntent.peerName)) {
            peerName.setText(callIntent.peerName);
        } else if (!TextUtils.isEmpty(callIntent.peerNumber)) {
            peerName.setText(callIntent.peerNumber);
        } else {
            peerName.setText(callIntent.peerUri.getUid());
        }


        findViewById(R.id.more_hang_up_ly).setOnClickListener(this);
        findViewById(R.id.more_accept_ly).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.more_hang_up_ly) {
            userActionListener.rejectIncomingCall(callIntent.callIndex, "APP_BUSINESS_REASON");
        } else if (viewId == R.id.more_accept_ly) {
            userActionListener.answerCall(callIntent.callIndex, RtcCallMode.CallMode_AudioVideo, true);
        }
    }
}
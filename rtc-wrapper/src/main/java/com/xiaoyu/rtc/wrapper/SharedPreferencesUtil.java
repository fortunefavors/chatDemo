package com.xiaoyu.rtc.wrapper;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.text.TextUtils;

public class SharedPreferencesUtil {
    public static SharedPreferencesUtil INSTANCE = new SharedPreferencesUtil();
    private static final String KEY_SHARE_SP = "RTC_WRAPPER";
    private SharedPreferences mSp;

    public void init(@NonNull Context context) {
        mSp = context.getSharedPreferences(KEY_SHARE_SP, Context.MODE_PRIVATE);
    }

    public SharedPreferences.Editor putString(String key, String value) {
        if (mSp == null) throw new RuntimeException("SharedPreferencesUtil has not init");
        return mSp.edit().putString(key, value);
    }

    public String getStringValue(String key, String defaultValue) {
        if (mSp == null) throw new RuntimeException("SharedPreferencesUtil has not init");
        return mSp.getString(key, defaultValue);
    }

    public SharedPreferences.Editor putBoolean(String key, boolean value) {
        if (mSp == null) throw new RuntimeException("SharedPreferencesUtil has not init");
        return mSp.edit().putBoolean(key, value);
    }

    public boolean getBooleanValue(String key, boolean defaultValue) {
        if (mSp == null) throw new RuntimeException("SharedPreferencesUtil has not init");
        return mSp.getBoolean(key, defaultValue);
    }

    public SharedPreferences.Editor putLong(String key, long value) {
        if (mSp == null) throw new RuntimeException("SharedPreferencesUtil has not init");
        return mSp.edit().putLong(key, value);
    }

    public Long getLongValue(String key, Long defaultValue) {
        if (mSp == null) throw new RuntimeException("SharedPreferencesUtil has not init");
        return mSp.getLong(key, defaultValue);
    }

    public SharedPreferences.Editor putInt(String key, int defaultValue) {
        if (mSp == null) throw new RuntimeException("SharedPreferencesUtil has not init");
        return mSp.edit().putInt(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        if (mSp == null) throw new RuntimeException("SharedPreferencesUtil has not init");
        return mSp.getInt(key, defaultValue);
    }

    public SharedPreferences.Editor putFloat(String key, float value) {
        if (mSp == null) throw new RuntimeException("SharedPreferencesUtil has not init");
        return mSp.edit().putFloat(key, value);
    }

    public float getFloat(String key, float defaultValue) {
        if (mSp == null) throw new RuntimeException("SharedPreferencesUtil has not init");
        return mSp.getFloat(key, defaultValue);
    }

    public SharedPreferences.Editor putDouble(String key, double value) {
        if (mSp == null) throw new RuntimeException("SharedPreferencesUtil has not init");
        return mSp.edit().putString(key, String.valueOf(value));
    }

    public double getDouble(String key, double defaultValue) {
        if (mSp == null) throw new RuntimeException("SharedPreferencesUtil has not init");
        String val = mSp.getString(key, "");
        if (TextUtils.isEmpty(val)) {
            return defaultValue;
        }
        return Double.valueOf(val);
    }
}
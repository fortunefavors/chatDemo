package com.example.rtc.sys;

import android.text.TextUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class SystemProperties {

    private SystemProperties() {
    }

    private static String get(String key) {
        try {
            Class<?> clz = SystemProperties.class.getClassLoader().loadClass("android.os.SystemProperties");
            Method method = clz.getMethod("get", String.class);
            Object value = method.invoke(null, key);
            return value == null ? null : value.toString();
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            return null;
        }
    }

    static String getString(String key, String defaultValue) {
        String value = get(key);
        if (TextUtils.isEmpty(value)) {
            return defaultValue;
        }
        return value;
    }
}

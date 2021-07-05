package com.xiaoyu.rtc.wrapper;

/**
 * 小度设备定制媒体能力标记
 */
public enum MediaCustomKey {
    APP, PUFFER, PUFFER1S_1C, PUFFER1L, PUFFERX8, PAD;

    public String getMediaCustomKey() {
        switch (this) {
            case PUFFER:
                return "R58";
            case PUFFER1S_1C:
                return "MTK";
            case PUFFER1L:
                return "RK3326";
            case PUFFERX8:
                return "X8_MTK8167S";
            case PAD:
                return "xp10e_MTK8175";
            default:
                return "APP";
        }
    }
}

package com.xiaoyu.rtc.wrapper.view;

public class ColorUtil {
    public final static int C100_0000000 = argb(1f, 0f, 0f, 0f);
    public final static int C50_000000 = argb(0.5f, 0f, 0f, 0f);
    public final static int C30_FFFFFF = argb(0.3f, 1f, 1f, 1f);

    public static int argb(float alpha, float red, float green, float blue) {
        return ((int) (alpha * 255.0f + 0.5f) << 24) |
                ((int) (red * 255.0f + 0.5f) << 16) |
                ((int) (green * 255.0f + 0.5f) << 8) |
                (int) (blue * 255.0f + 0.5f);
    }
}

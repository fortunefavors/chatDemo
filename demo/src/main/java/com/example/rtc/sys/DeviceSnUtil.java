package com.example.rtc.sys;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

public class DeviceSnUtil {

    private static final Logger LOGGER = Logger.getLogger("DeviceSnUtil");
    private static final String FACTORY_FILE = "/data/log/recovery.flag";
    private static String FACTORY_RANDOM_NUM = "FactoryRandomNumber";
    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    @SuppressLint({"MissingPermission", "HardwareIds"})
    public static String getDeviceSn(Context context) {
        final String serialNum = Build.SERIAL;
        String fingerprint = getFactoryRandom(context);

        if (VersionUtil.getHardwareVersionCode() > 0) {
            return (serialNum + "_" + fingerprint);
        }

        final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        final String tmDevice, androidId;

        tmDevice = "" + tm.getDeviceId();
        androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        UUID deviceUuid = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32) | serialNum.hashCode());

        LOGGER.info("tmDevice " + tmDevice + ", " + serialNum + ", androidId:" + androidId + " , deviceUuid: " + deviceUuid + " , fingerprint: " + fingerprint);
        return (deviceUuid.toString() + "_" + fingerprint);
    }

    private static synchronized String getFactoryRandom(Context context) {
        String mFactoryNumber = null;
        Properties properties = new Properties();
        try {
            File fil = new File(FACTORY_FILE);
            if (!fil.exists()) {
                mFactoryNumber = generateRandomUUID(context);
                saveFactoryRandom(mFactoryNumber);
                if (fil.exists()) {
                    fil.setReadable(true, false);
                    fil.setWritable(true, false);
                }
            } else {
                FileInputStream inputStream = new FileInputStream(FACTORY_FILE);
                properties.load(inputStream);
                inputStream.close();

                mFactoryNumber = (String) properties.get(FACTORY_RANDOM_NUM);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mFactoryNumber;
    }

    private static void saveFactoryRandom(String randomNum) {
        try {
            Properties properties = new Properties();
            properties.setProperty(FACTORY_RANDOM_NUM, randomNum);

            FileOutputStream outputStream = new FileOutputStream(FACTORY_FILE, false);
            properties.store(outputStream, null);
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String generateRandomUUID(Context context) {
        final String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        long androidIdL;
        //protected
        Random random = new Random();
        if (TextUtils.isEmpty(androidId)) {
            androidIdL = random.nextLong();
        } else {
            androidIdL = androidId.hashCode();
        }

        androidIdL = Math.abs(androidIdL);
        long randoml = random.nextLong();
        long randoml2 = random.nextLong();
        randoml2 = Math.abs(randoml2);

        randoml = new Random(((randoml2 << 32) | randoml)).nextLong();
        randoml = Math.abs(randoml);

        //[16 -32)
        int left = random.nextInt(32);
        if (left < 16) {
            left = 32 - left;
        }

        long etime = SystemClock.elapsedRealtime();

        UUID deviceUuid = new UUID((randoml << left) | etime, ((randoml2 << left) | androidIdL));
        LOGGER.info("getRandomUUID androidId:" + androidIdL + " rlong, " + randoml + ", randoml2 " + randoml2 + ", etime " + etime + ",left " + left + " , deviceUuid: " + deviceUuid);

        try {
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            mdInst.update(deviceUuid.toString().getBytes());
            byte[] md = mdInst.digest();

            int len = md.length;
            StringBuilder buf = new StringBuilder(len * 2);
            for (byte b : md) {
                buf.append(HEX_DIGITS[(b >> 4) & 0x0f]);
                buf.append(HEX_DIGITS[b & 0x0f]);
            }
            return buf.substring(0, 8);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return deviceUuid.toString();
    }
}
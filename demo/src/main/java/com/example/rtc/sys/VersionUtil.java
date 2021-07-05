package com.example.rtc.sys;

import java.util.logging.Logger;

class VersionUtil {

    private static final Logger LOGGER = Logger.getLogger("VersionUtil");

    static int getHardwareVersionCode() {
        String version = SystemProperties.getString("ro.hardwareno", "0");
        LOGGER.info("getHardwareVersionCode " + version);
        if (version.equalsIgnoreCase("0")) {
            return 0;
        } else if (version.startsWith("VENUS")) {
            // VENUS MB :1
            String[] iversion = version.split(":");
            if (iversion.length == 2) {
                return Integer.parseInt(iversion[1]);
            } else {
                return 0;
            }
        } else {
            // 60001-00001-001 1.1.0
            String[] iversion = version.split(" ");
            if (iversion.length >= 2) {
                String[] subv = iversion[1].split("\\.");
                if (subv.length == 3) {
                    return Integer.parseInt(subv[0]) * 10000 + Integer.parseInt(subv[1]) * 100 + Integer.parseInt(subv[2]);
                }
                return 0;
            } else {
                String[] subv = version.split("\\.");
                if (subv.length == 3) {
                    return Integer.parseInt(subv[0]) * 10000 + Integer.parseInt(subv[1]) * 100 + Integer.parseInt(subv[2]);
                }
                return 0;
            }
        }
    }
}
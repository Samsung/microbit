package com.samsung.microbit.data.constants;

import com.samsung.microbit.service.BLEService;
import com.samsung.microbit.service.PluginService;

/**
 * Contains registration ids.
 * Used for registering/unregistering device sensors for send statistic.
 * <p/>
 * Used for handling changes inside {@link PluginService PluginService}.<br/>
 * Registering for changes inside {@link BLEService BLEService}
 */
public class RegistrationIds {
    private RegistrationIds() {
    }

    // Registration ID's
    public static final int REG_TELEPHONY = 0x01;    // 0x00000001;
    public static final int REG_MESSAGING = 0x02;    // 0x00000002;
    public static final int REG_DEVICEORIENTATION = 0x04;    // 0x00000004;
    public static final int REG_DEVICEGESTURE = 0x08;    // 0x00000008;
    public static final int REG_DISPLAY = 0x010;    // 0x00000010;
    public static final int REG_SIGNALSTRENGTH = 0x020;    // 0x00000020;
    public static final int REG_BATTERYSTRENGTH = 0x040;    // 0x00000040;
    public static final int REG_TEMPERATURE = 0x080;    // 0x00000080;
}

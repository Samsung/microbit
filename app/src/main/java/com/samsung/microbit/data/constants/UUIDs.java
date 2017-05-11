package com.samsung.microbit.data.constants;

import com.samsung.microbit.utils.Utils;

import java.util.UUID;

/**
 * Contains common universally unique identifiers.
 */
public class UUIDs {
    private UUIDs() {
    }

    /*
     * Base Low energy UUID's for services and characteristics
	 */
    public static final String BASE_UUID_STR = "00000000-0000-1000-8000-00805f9b34fb";
    public static final String MICROBIT_BASE_UUID_STR = "e95d5be9-251d-470a-a062-fa1922dfa9a8";

    public static final UUID CLIENT_DESCRIPTOR = Utils.makeUUID(BASE_UUID_STR, 0x02902);

    public static final UUID BASE_UUID = UUID.fromString(BASE_UUID_STR);
    public static final UUID MICROBIT_BASE_UUID = UUID.fromString(MICROBIT_BASE_UUID_STR);

    // This descriptor is used for requesting notification

    public static final UUID SERVICE_GENERIC_ACCESS = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x01800);
    public static final UUID SGA_DEVICE_NAME = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x02a00);
    public static final UUID SGA_APPEARANCE = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x02a01);
    public static final UUID SGA_PPRIVACY_FLAG = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x02a02);
    public static final UUID SGA_RECONNECTION_ADDRESS = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x02a03);
    public static final UUID SGA_PPCONNECTION_PARAMETERS = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x02a04);

    public static final UUID SERVICE_DEVICE_INFORMATION = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x0180a);
    public static final UUID SDI_SYSTEM_ID = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x02a23);
    public static final UUID SDI_MODEL_NUMBER_STRING = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x02a24);
    public static final UUID SDI_SERIAL_NUMBER_STRING = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x02a25);
    public static final UUID SDI_FIRMWARE_REVISION_STRING = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x02a26);
    public static final UUID SDI_HARDWARE_REVISION_STRING = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x02a27);
    public static final UUID SDI_SOFTWARE_REVISION_STRING = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x02a28);
    public static final UUID SDI_REGULATORY_CERTIFICATION_DATA_LIST = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x02a2a);
    public static final UUID SDI_PNP_ID = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x02a50);

    public static final UUID SERVICE_BATTERY_SERVICE = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x0180f);
    public static final UUID SBS_BATTERY_LEVEL = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x02a19);

    /*
     * Microbit specific UUID's
     */
    public static final UUID ACCELEROMETER_SERVICE = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x00753);
    public static final UUID AS_ACCELEROMETER_DATA = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x0ca4b);
    public static final UUID AS_ACCELEROMETER_PERIOD = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x0fb24);

    public static final UUID MAGNETOMETER_SERVICE = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x0f2d8);
    public static final UUID MS_MAGNETOMETER_DATA = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x0fb11);
    public static final UUID MS_MAGNETOMETER_PERIOD = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x0386c);

    public static final UUID BUTTON_SERVICE = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x09882);
    public static final UUID BS_BUTTON_STATE = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x0da90);

    public static final UUID LED_SERVICE = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x0d91d);
    public static final UUID LS_LED_MATRIX_STATE = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x07b77);
    public static final UUID LS_SYSTEM_LED_STATE = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x0b744);
    public static final UUID LS_LED_TEXT = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x093ee);
    public static final UUID LS_SCROLLING_STATE = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0X08136);
    public static final UUID LS_SCROLLING_SPEED = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x00d2d);

    public static final UUID IO_PIN_SERVICE = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x0127b);
    public static final UUID IOPS_PIN_0 = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x08d00);
    public static final UUID IOPS_PIN_1 = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x0c58c);
    public static final UUID IOPS_PIN_2 = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x004f4);
    public static final UUID IOPS_PIN_3 = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x0bf30);
    public static final UUID IOPS_PIN_4 = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x0e5c1);
    public static final UUID IOPS_PIN_5 = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0X05281);
    public static final UUID IOPS_PIN_6 = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x02c44);
    public static final UUID IOPS_PIN_7 = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x0d205);
    public static final UUID IOPS_PIN_8 = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x055ff);
    public static final UUID IOPS_PIN_9 = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0X00906);
    public static final UUID IOPS_PIN_10 = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x020be);
    public static final UUID IOPS_PIN_11 = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x0e36e);
    public static final UUID IOPS_PIN_12 = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x02c29);
    public static final UUID IOPS_PIN_13 = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x0b67a);
    public static final UUID IOPS_PIN_14 = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x0c2fe);
    public static final UUID IOPS_PIN_15 = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x074b4);
    public static final UUID IOPS_PIN_16 = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x0ab2c);
    public static final UUID IOPS_PIN_17 = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x0100a);
    public static final UUID IOPS_PIN_CONFIGURATION = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x05899);
    public static final UUID IOPS_PARALLEL_PORT = Utils.makeUUID(MICROBIT_BASE_UUID_STR, 0x060cf);
}

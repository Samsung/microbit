package com.samsung.microbit.data.constants;

/**
 * Contains common permission codes.
 * It uses to identify which permission need to grand.
 */
public class PermissionCodes {
    private PermissionCodes() {
    }

    //Can only use lower 8 bits for requestCode
    public static final int APP_STORAGE_PERMISSIONS_REQUESTED = 0x01;
    public static final int BLUETOOTH_PERMISSIONS_REQUESTED = 0x02;
    public static final int CAMERA_PERMISSIONS_REQUESTED = 0x03;
    public static final int INCOMING_CALL_PERMISSIONS_REQUESTED = 0x03;
    public static final int INCOMING_SMS_PERMISSIONS_REQUESTED = 0x04;
}

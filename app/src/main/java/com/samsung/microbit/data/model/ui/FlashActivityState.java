package com.samsung.microbit.data.model.ui;

/**
 * Activity states of flashing process.
 */
public class FlashActivityState extends BaseActivityState {
    public static final int STATE_ENABLE_BT_INTERNAL_FLASH_REQUEST = 5;
    public static final int STATE_ENABLE_BT_EXTERNAL_FLASH_REQUEST = 6;
    public static final int FLASH_STATE_FIND_DEVICE = 7;
    public static final int FLASH_STATE_VERIFY_DEVICE = 8;
    public static final int FLASH_STATE_WAIT_DEVICE_REBOOT = 9;
    public static final int FLASH_STATE_INIT_DEVICE = 10;
    public static final int FLASH_STATE_PROGRESS = 11;
}

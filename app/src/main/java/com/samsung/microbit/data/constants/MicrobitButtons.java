package com.samsung.microbit.data.constants;

//TODO: consider to use somewhere or remove
public class MicrobitButtons {
    private MicrobitButtons() {
    }

    /*
     * this is microbit buttons
     */
    public static final int MICROBIT_BUTTON_A_ID = 1;
    //public static final int MICROBIT_BUTTON_A_IDF = MICROBIT_BUTTON_A_ID << 16;

    public static final int MICROBIT_BUTTON_B_ID = 2;
    //public static final int MICROBIT_BUTTON_B_IDF = MICROBIT_BUTTON_B_ID << 16;

    public static final int MICROBIT_BUTTON_RESET_ID = 3;
    //public static final int MICROBIT_BUTTON_RESET_IDF = MICROBIT_BUTTON_RESET_ID << 16;

    public static final int MICROBIT_BUTTON_EVT_DOWN = 1;
    public static final int MICROBIT_BUTTON_EVT_UP = 2;
    public static final int MICROBIT_BUTTON_EVT_CLICK = 3;
    public static final int MICROBIT_BUTTON_EVT_LONG_CLICK = 4;
    public static final int MICROBIT_BUTTON_EVT_HOLD = 5;
}

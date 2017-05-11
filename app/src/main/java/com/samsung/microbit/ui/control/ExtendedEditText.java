package com.samsung.microbit.ui.control;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;

import static com.samsung.microbit.BuildConfig.DEBUG;

/**
 * Custom edit text view. Uses to enter new project name
 * while renaming a project item.
 */
public class ExtendedEditText extends EditText {
    private static final String TAG = ExtendedEditText.class.getSimpleName();

    /**
     * Simplified method to log informational messages. Uses in debug mode.
     *
     * @param message Message to log.
     */
    protected void logi(final String message) {
        if(DEBUG) {
            Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
        }
    }

    public ExtendedEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

    }

    public ExtendedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public ExtendedEditText(Context context) {
        super(context);

    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if(event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            onEditorAction(-1);
            dispatchKeyEvent(event);

            return false;
        }

        return super.onKeyPreIme(keyCode, event);
    }

}
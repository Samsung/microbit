package com.samsung.microbit.ui.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.GoogleAnalyticsManager;
import com.samsung.microbit.ui.PopUp;
import com.samsung.microbit.utils.Utils;

import pl.droidsonroids.gif.GifImageView;

/**
 * Represents a custom dialog window. Allows to display and manage a fullscreen window
 * that contains a title, a message, a gif image or a simple image, a progress bar and
 * confirmation buttons.
 */
public class PopUpActivity extends Activity implements View.OnClickListener {

    //intent from PopUpActivity to PopUp
    public static final String INTENT_ACTION_OK_PRESSED = "PopUpActivity.OK_PRESSED";
    public static final String INTENT_ACTION_CANCEL_PRESSED = "PopUpActivity.CANCEL_PRESSED";
    public static final String INTENT_ACTION_DESTROYED = "PopUpActivity.DESTROYED";
    public static final String INTENT_ACTION_CREATED = "PopUpActivity.CREATED";

    //intent from PopUp to PopUpActivity
    public static final String INTENT_ACTION_CLOSE = "PopUpActivity.CLOSE";
    public static final String INTENT_ACTION_UPDATE_PROGRESS = "PopUpActivity.UPDATE_PROGRESS";
    public static final String INTENT_ACTION_UPDATE_LAYOUT = "PopUpActivity.UPDATE_LAYOUT";

    public static final String INTENT_EXTRA_TYPE = "type";
    public static final String INTENT_EXTRA_TITLE = "title";
    public static final String INTENT_EXTRA_MESSAGE = "message";
    public static final String INTENT_EXTRA_ICON = "imageIcon";
    public static final String INTENT_EXTRA_ICONBG = "imageIconBg";
    public static final String INTENT_EXTRA_PROGRESS = "progress.xml";
    public static final String INTENT_EXTRA_CANCELABLE = "cancelable";
    public static final String INTENT_GIFF_ANIMATION_CODE = "giffAnimationCode";

    // Animations - Loading Error & Flash states
    private GifImageView gifImageView;

    private ImageView imageIcon;
    private TextView titleTxt;
    private ProgressBar progressBar;
    private ProgressBar spinnerBar;
    private TextView messageTxt;
    private Button okButton;
    private Button cancelButton;
    private Button affirmationOKButton;
    private LinearLayout layoutBottom;

    private boolean isCancelable;

    private Intent mReceiverIntent;

    /**
     * A broadcast receiver that handles amount of actions such as
     * intent to close a popup, update a progress bar or update a layout.
     */
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, final Intent intent) {
            if(intent.getAction().equals(INTENT_ACTION_CLOSE)) {
                Log.d("PopUpActivity", "BroadcastReceiver.INTENT_ACTION_CLOSE");
                finish();
            } else if(intent.getAction().equals(INTENT_ACTION_UPDATE_PROGRESS)) {
                if(progressBar != null)
                    progressBar.setProgress(intent.getIntExtra(INTENT_EXTRA_PROGRESS, 0));
            } else if(intent.getAction().equals(INTENT_ACTION_UPDATE_LAYOUT)) {
                Log.d("PopUpActivity", "BroadcastReceiver.INTENT_ACTION_UPDATE_LAYOUT");
                isCancelable = intent.getBooleanExtra(INTENT_EXTRA_CANCELABLE, true);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        clearLayout();
                        setLayout(intent);
                        mReceiverIntent = intent;
                    }
                });
            }
        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        releaseViews();

        setContentView(R.layout.activity_popup);
        initViews();
        //Check if an intent was passed through a receiver.
        if(mReceiverIntent == null) {
            setLayout(getIntent());
        } else {
            setLayout(mReceiverIntent);
        }
    }

    /**
     * Setups font style by setting an appropriate typeface.
     */
    private void setupFontStyle() {
        affirmationOKButton.setTypeface(MBApp.getApp().getRobotoTypeface());
        cancelButton.setTypeface(MBApp.getApp().getRobotoTypeface());
        okButton.setTypeface(MBApp.getApp().getRobotoTypeface());
        messageTxt.setTypeface(MBApp.getApp().getRobotoTypeface());
        titleTxt.setTypeface(MBApp.getApp().getTypefaceBold());
    }

    private void initViews() {
        imageIcon = (ImageView) findViewById(R.id.image_icon);
        titleTxt = (TextView) findViewById(R.id.flash_projects_title_txt);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        spinnerBar = (ProgressBar) findViewById(R.id.spinnerBar);
        messageTxt = (TextView) findViewById(R.id.messageTxt);
        layoutBottom = (LinearLayout) findViewById(R.id.popup_bottom_layout);
        okButton = (Button) findViewById(R.id.imageButtonOk);
        cancelButton = (Button) findViewById(R.id.imageButtonCancel);
        affirmationOKButton = (Button) findViewById(R.id.affirmationOKBtn);
        // Error / Flash animation
        gifImageView = (GifImageView) findViewById(R.id.pop_up_gif_image_view);

        setupFontStyle();
    }

    private void releaseViews() {
        Utils.unbindDrawables(imageIcon);
        imageIcon = null;

        Utils.unbindDrawables(titleTxt);
        titleTxt = null;

        progressBar = null;
        spinnerBar = null;

        Utils.unbindDrawables(messageTxt);
        messageTxt = null;

        Utils.unbindDrawables(layoutBottom);
        layoutBottom = null;

        Utils.unbindDrawables(okButton);
        okButton = null;

        Utils.unbindDrawables(cancelButton);
        cancelButton = null;

        Utils.unbindDrawables(affirmationOKButton);
        affirmationOKButton = null;

        Utils.unbindDrawables(gifImageView);
        gifImageView = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GoogleAnalyticsManager.getInstance().sendViewEventStats(PopUpActivity.class.getSimpleName());

        Log.d("PopUpActivity", "onCreate() popuptype = " + getIntent().getIntExtra(INTENT_EXTRA_TYPE, PopUp.TYPE_NONE));
        setContentView(R.layout.activity_popup);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        initViews();

        isCancelable = getIntent().getBooleanExtra(INTENT_EXTRA_CANCELABLE, true);

        setLayout(getIntent());

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);

        IntentFilter popupActivityFilter = new IntentFilter();
        popupActivityFilter.addAction(INTENT_ACTION_CLOSE);
        popupActivityFilter.addAction(INTENT_ACTION_UPDATE_PROGRESS);
        popupActivityFilter.addAction(INTENT_ACTION_UPDATE_LAYOUT);

        //listen for close or update progress.xml request
        localBroadcastManager.registerReceiver(broadcastReceiver, popupActivityFilter);

        //notify creation of activity to calling code PopUp class
        localBroadcastManager.sendBroadcast(new Intent(INTENT_ACTION_CREATED));
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleAnalyticsManager.getInstance().activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        GoogleAnalyticsManager.getInstance().activityStop(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure sure animation remains loading
        if(gifImageView != null) {
            gifImageView.animate();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        gifImageView.setFreezesAnimation(true);
    }

    /**
     * Clears layout by setting all its elements visibility to
     * INVISIBLE or GONE.
     */
    private void clearLayout() {
        imageIcon.setImageResource(R.drawable.overwrite_face);
        imageIcon.setBackgroundResource(0);
        titleTxt.setVisibility(View.GONE);
        messageTxt.setVisibility(View.GONE);
        layoutBottom.setVisibility(View.INVISIBLE);
        okButton.setVisibility(View.GONE);
        cancelButton.setVisibility(View.GONE);
        affirmationOKButton.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        spinnerBar.setVisibility(View.GONE);
    }

    /**
     * Sets a popup layout which allows to dynamically create different popup views.
     *
     * @param intent Contains information about how should a popup look like.
     */
    private void setLayout(Intent intent) {
        String title = intent.getStringExtra(INTENT_EXTRA_TITLE);

        if(!title.isEmpty()) {
            titleTxt.setText(title);
            titleTxt.setVisibility(View.VISIBLE);
        }

        String message = intent.getStringExtra(INTENT_EXTRA_MESSAGE);
        if(!message.isEmpty()) {
            messageTxt.setText(message);
            messageTxt.setVisibility(View.VISIBLE);
        }

        int imageResId = intent.getIntExtra(INTENT_EXTRA_ICON, 0);
        int imageBackgroundResId = intent.getIntExtra(INTENT_EXTRA_ICONBG, 0);
        if(imageResId != 0) {
            imageIcon.setImageResource(imageResId);
        }
        if(imageBackgroundResId != 0) {
            imageIcon.setBackgroundResource(imageBackgroundResId);
        }

        /* Loading the Giff only if the animation code isn't default 0
         * Default value is 0 (there is no animation ) Case 1 = flash, Case 2 = Error */
        int imageGiffAnimationCode = intent.getIntExtra(INTENT_GIFF_ANIMATION_CODE, 0);
        if(imageGiffAnimationCode != 0) {
            switch(imageGiffAnimationCode) {
                // Flashing screen
                case 1:
                    // Asset file
                    gifImageView.setBackgroundResource(R.drawable.emoji_flashing_microbit);
                    gifImageView.setVisibility(View.VISIBLE);
                    // Regular image disabled
                    imageIcon.setVisibility(View.GONE);
                    break;

                // Error screen
                case 2:
                    // Asset file
                    gifImageView.setBackgroundResource(R.drawable.emoji_fail_microbit);
                    gifImageView.setVisibility(View.VISIBLE);
                    // Regular image disabled
                    imageIcon.setVisibility(View.GONE);
                    break;
            }
            // Set default plain icon
        } else {
            imageIcon.setVisibility(View.VISIBLE);
            gifImageView.setVisibility(View.GONE);
        }

        switch(intent.getIntExtra(INTENT_EXTRA_TYPE, PopUp.TYPE_NONE)) {
            case PopUp.TYPE_CHOICE:
                layoutBottom.setVisibility(View.VISIBLE);
                okButton.setVisibility(View.VISIBLE);
                cancelButton.setVisibility(View.VISIBLE);
                break;
            case PopUp.TYPE_ALERT:
            case PopUp.TYPE_ALERT_LIGHT:
                layoutBottom.setVisibility(View.VISIBLE);
                affirmationOKButton.setVisibility(View.VISIBLE);
                break;
            case PopUp.TYPE_PROGRESS:
            case PopUp.TYPE_PROGRESS_NOT_CANCELABLE:
                progressBar.setVisibility(View.VISIBLE);
                break;
            case PopUp.TYPE_NOBUTTON:
                break;
            case PopUp.TYPE_SPINNER:
            case PopUp.TYPE_SPINNER_NOT_CANCELABLE:
                spinnerBar.setVisibility(View.VISIBLE);
                break;

            default:
                //TODO: handle Error
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("PopUpActivity", "onDestroy()");

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);

        localBroadcastManager.sendBroadcast(new Intent(INTENT_ACTION_DESTROYED));
        localBroadcastManager.unregisterReceiver(broadcastReceiver);
        releaseViews();
    }

    @Override
    public void onBackPressed() {
        Log.d("PopUpActivity", "onBackPressed IsCancelable " + isCancelable);
        if(!isCancelable)
            return;

        //Do not call super.onBackPressed() because we let the calling PopUp code to issue a "hide" call.
        //PopUp code is the master code which decides when to destroy or create PopUpActivity.
        LocalBroadcastManager.getInstance(this).sendBroadcastSync(new Intent(INTENT_ACTION_CANCEL_PRESSED));
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent();
        //       intent.putExtra(INTENT_EXTRA_INPUTTEXT, inputText.getText().toString());

        switch(v.getId()) {
            case R.id.imageButtonOk:
                intent.setAction(INTENT_ACTION_OK_PRESSED);
                break;

            case R.id.imageButtonCancel:
            case R.id.affirmationOKBtn:
                intent.setAction(INTENT_ACTION_CANCEL_PRESSED);
                break;
        }

        LocalBroadcastManager.getInstance(this).sendBroadcastSync(intent);
    }
}

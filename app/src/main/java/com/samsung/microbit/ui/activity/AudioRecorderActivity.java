package com.samsung.microbit.ui.activity;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;

import com.samsung.microbit.R;
import com.samsung.microbit.core.GoogleAnalyticsManager;
import com.samsung.microbit.plugin.AudioRecordPlugin;
import com.samsung.microbit.ui.PopUp;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;

public class AudioRecorderActivity extends Activity {

    private static final int NOTIFICATION_ID = 1;
    private static final String TAG = AudioRecorderActivity.class.getSimpleName();

    private TextView filenameTxt;
    private Chronometer chronometer;
    private ImageView imageMic;
    private Drawable drawable_mic_off;
    private Drawable drawable_mic_on;
    private Bitmap notificationLargeIconBitmapRecordingOn;
    private Bitmap notificationLargeIconBitmapRecordingOff;
    private NotificationCompat.Builder mBuilder;

    private boolean backPressed;

    private MediaRecorder mRecorder;
    private File mRecordFileOutput;
    private boolean mIsRecording;
    private boolean mLaunchActivity;

    private void create(String action) {
        setContentView(R.layout.activity_audio_recorder);
        filenameTxt = (TextView) findViewById(R.id.filenameTxt);
        chronometer = (Chronometer) findViewById(R.id.chronometer);
        imageMic = (ImageView) findViewById(R.id.imageMic);

        //preallocate to avoid memory leak
        drawable_mic_off = getResources().getDrawable(R.drawable.recording_off);
        drawable_mic_on = getResources().getDrawable(R.drawable.recording);

        backPressed = false;

        notificationLargeIconBitmapRecordingOn = BitmapFactory.decodeResource(getResources(), R.drawable.recording);
        notificationLargeIconBitmapRecordingOff = BitmapFactory.decodeResource(getResources(), R.drawable.recording_off);

        mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(notificationLargeIconBitmapRecordingOff)
                .setTicker(getString(R.string.audio_recorder_notification))
                .setContentTitle(getString(R.string.audio_recorder_notification));

        Drawable d = getResources().getDrawable(R.drawable.bg);
        if(d != null) {
            d.mutate();//prevent affecting all instances of that drawable with color filter
            d.setColorFilter(Color.argb(187, 0, 0, 0), PorterDuff.Mode.SRC_OVER);
            findViewById(R.id.layout).setBackground(d);
        }
        processIntent(action);
    }

    private boolean showPopup(final String action) {
        return PopUp.show("",
                getString(R.string.record_audio),
                R.drawable.record_icon, //image icon res id (pass 0 to use default icon)
                R.drawable.white_btn, //image icon background res id (pass 0 if there is no background)
                PopUp.GIFF_ANIMATION_NONE,
                PopUp.TYPE_CHOICE, //type of popup.
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AudioRecorderActivity.this.create(action);
                        mLaunchActivity = true;
                    }
                },//override click listener for ok button
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AudioRecorderActivity.this.finish();
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GoogleAnalyticsManager.getInstance().sendViewEventStats(AudioRecorderActivity.class.getSimpleName());

        mLaunchActivity = false;

        if(!showPopup(getIntent().getAction())) {//pass null to use default listener
            finish();
        }
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

        //do not create notification if back pressed (or it is not recording?)
        //or if activity was not launched
        if(mLaunchActivity && !backPressed) {
            Intent resultIntent = new Intent(this, AudioRecorderActivity.class);
            resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//bring existing activity to foreground

            PendingIntent resultPendingIntent =
                    PendingIntent.getActivity(
                            this,
                            0,
                            resultIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT //update existing notification instead of creating new one
                    );

            if(mIsRecording)
                mBuilder.setLargeIcon(notificationLargeIconBitmapRecordingOn);
            else
                mBuilder.setLargeIcon(notificationLargeIconBitmapRecordingOff);

            mBuilder.setContentIntent(resultPendingIntent);
            Notification notification = mBuilder.build();
            notification.flags |= Notification.FLAG_AUTO_CANCEL;//the notification should disappear when it is clicked by the user.
            notification.flags |= Notification.FLAG_NO_CLEAR;//the notification should not be removed when the user clicks the Clear all button.

            NotificationManager mNotifyMgr =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mNotifyMgr.notify(NOTIFICATION_ID, notification);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        //process intent only if activity is launched after popup confirmation
        if(mLaunchActivity)
            processIntent(intent.getAction());
        else {
            PopUp.hide();//Needed to fix case when activity is brought to foreground but no showing popup activity anymore
            //this will reset the flag PopUp.current_type so another PopUp can be shown
            showPopup(intent.getAction());
        }
    }

    private void processIntent(String action) {
        if(action == null)
            return;

        switch(action) {
            case AudioRecordPlugin.INTENT_ACTION_START_RECORD:
                startRecording();
                break;
            case AudioRecordPlugin.INTENT_ACTION_STOP_RECORD:
                stopRecording();
                break;
            case AudioRecordPlugin.INTENT_ACTION_STOP:
                if(mIsRecording)
                    stopRecording();
                finish();
                backPressed = true;//prevent notification creation

                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //make sure we remove existing notification if any when activity is destroyed
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(NOTIFICATION_ID);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        //handle orientation change to prevent re-creation of activity.
        //i.e. while recording we need to preserve state of recorder
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        //if recording is active, then stop recording.
        //otherwise exit activity
        if(mIsRecording) {
            stopRecording();
        } else {
            super.onBackPressed();
            backPressed = true;
        }
    }

    public File getAudioFilename() {
        // Get the directory for the user's public pictures directory.
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        if(!dir.exists()) {
            if(!dir.mkdirs()) {
                Log.e(TAG, "Failed to create directory");
            }
        }

        Calendar c = Calendar.getInstance();
        DateFormat sdf = DateFormat.getDateTimeInstance();
        String filename = "voice_" + sdf.format(c.getTime());

        return new File(dir, filename + ".3gp");
    }

    private void startRecording() {
        releaseRecorder();

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

        mRecordFileOutput = getAudioFilename();

        //TODO: check disk space left?
        mRecorder.setOutputFile(mRecordFileOutput.getPath());
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch(IOException e) {
            releaseRecorder();
            //TODO: show popup for failure?
            Log.e(TAG, e.toString());
            return;
        }

        mRecorder.start();
        mIsRecording = true;

        //UI update
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();

        filenameTxt.setText(mRecordFileOutput.getName());
        imageMic.setImageDrawable(drawable_mic_on);
    }

    private void releaseRecorder() {
        if(mRecorder != null) {
            if(mIsRecording) {
                mRecorder.stop();
            } else {
                mRecorder.reset();
            }
            mRecorder.release();

            mRecorder = null;
        }
    }

    private void stopRecording() {
        releaseRecorder();

        mIsRecording = false;

        refreshAudio(mRecordFileOutput);

        //UI update
        chronometer.stop();

        imageMic.setImageDrawable(drawable_mic_off);
    }

    private void refreshAudio(File file) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }
}

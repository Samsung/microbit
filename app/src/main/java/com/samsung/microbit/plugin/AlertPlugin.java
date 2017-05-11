package com.samsung.microbit.plugin;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.annotation.IntDef;
import android.util.Log;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.data.constants.EventSubCodes;
import com.samsung.microbit.data.constants.InternalPaths;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.presentation.PlayAudioPresenter;
import com.samsung.microbit.presentation.PlayRingtonePresenter;
import com.samsung.microbit.presentation.Presenter;
import com.samsung.microbit.presentation.VibratePresenter;
import com.samsung.microbit.ui.PopUp;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.samsung.microbit.plugin.AlertPlugin.AlertType.*;

/**
 * Represents a module that can provide actions to raise a big smoke.
 * It can make your mobile device ring and vibrate so you can figure out
 * where it is if you can't find it.
 */
public class AlertPlugin implements AbstractPlugin {
    private static final String TAG = AlertPlugin.class.getSimpleName();

    private static final int MAX_RINGTONE_DURATION = (int) TimeUnit.SECONDS.toMillis(10);

    private List<Presenter> activePresenters = new ArrayList<>();
    private List<Integer> alertTypes = new ArrayList<>();

    private MediaPlayer mediaPlayer;

    @Override
    public void handleEntry(CmdArg cmd) {
        Context context = MBApp.getApp();

        String message = "";

        final String title;

        int popupAction = PopUp.OK_ACTION_NONE;

        switch(cmd.getCMD()) {
            case EventSubCodes.SAMSUNG_ALERT_EVT_DISPLAY_TOAST:
                message = cmd.getValue();
                title = "Message from Micro:Bit";
                break;

            case EventSubCodes.SAMSUNG_ALERT_EVT_VIBRATE:
                title = context.getString(R.string.vibrating_via_microbit);
                addVibrationForPlaying(Integer.parseInt(cmd.getValue()));
                break;

            case EventSubCodes.SAMSUNG_ALERT_EVT_PLAY_SOUND:
                title = context.getString(R.string.sound_via_microbit);
                Uri ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                int durationNotExceed = getMaxRingtoneDuration(ringtone);

                addRingtoneForPlaying(ringtone, durationNotExceed, false);
                break;

            case EventSubCodes.SAMSUNG_ALERT_EVT_PLAY_RINGTONE:
                title = context.getString(R.string.ringtone_via_microbit);
                ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

                durationNotExceed = getMaxRingtoneDuration(ringtone);

                addRingtoneForPlaying(ringtone, durationNotExceed, false);
                break;

            case EventSubCodes.SAMSUNG_ALERT_EVT_FIND_MY_PHONE:
                title = context.getString(R.string.findphone_via_microbit);
                popupAction = PopUp.OK_ACTION_STOP_SERVICE_PLAYING;

                durationNotExceed = getMaxAudioDuration(InternalPaths.FIND_MY_PHONE_AUDIO);

                addInternalPathForPlaying(InternalPaths.FIND_MY_PHONE_AUDIO, null);
                addVibrationForPlaying(durationNotExceed);
                break;
            case EventSubCodes.SAMSUNG_ALERT_EVT_ALARM1:
            case EventSubCodes.SAMSUNG_ALERT_EVT_ALARM2:
            case EventSubCodes.SAMSUNG_ALERT_EVT_ALARM3:
            case EventSubCodes.SAMSUNG_ALERT_EVT_ALARM4:
            case EventSubCodes.SAMSUNG_ALERT_EVT_ALARM5:
            case EventSubCodes.SAMSUNG_ALERT_EVT_ALARM6:
                title = context.getString(R.string.sound_via_microbit);
                ringtone = searchAlarmUri(cmd.getCMD());

                durationNotExceed = getMaxRingtoneDuration(ringtone);

                addRingtoneForPlaying(ringtone, durationNotExceed, false);
                break;
            case EventSubCodes.SAMSUNG_ALERT_STOP_PLAYING:
                stopPlaying();
                return;
            default:
                Log.e(TAG, "Unknown category");
                return;
        }

        stopPlaying();

        if(popupAction == PopUp.OK_ACTION_NONE) {
            showDialog(message, title);
        } else {
            showDialogWithAction(title, popupAction);
        }

        for(Presenter presenter : activePresenters) {
            presenter.start();
        }
    }

    /**
     * Makes your device to stop ringing.
     */
    private void stopPlaying() {
        for(Presenter presenter : activePresenters) {
            presenter.stop();
        }
    }

    public static int getDuration(MediaPlayer mediaPlayer, AssetFileDescriptor afd) {
        int duration = 500;

        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mediaPlayer.prepare();
            duration = mediaPlayer.getDuration();
        } catch(IOException e) {
            Log.e(TAG, e.toString());
        }

        mediaPlayer.reset();

        return duration;
    }

    public static int getDuration(MediaPlayer mediaPlayer, Uri fileUri) {
        int duration = 500;

        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(MBApp.getApp(), fileUri);
            mediaPlayer.prepare();
            duration = mediaPlayer.getDuration();
        } catch(IOException e) {
            Log.e(TAG, e.toString());
        }

        mediaPlayer.reset();

        return duration;
    }

    private int getMaxRingtoneDuration(Uri ringtoneUri) {
        if(mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }

        int duration = getDuration(mediaPlayer, ringtoneUri);

        if(MAX_RINGTONE_DURATION > 0 && duration > MAX_RINGTONE_DURATION) {
            duration = MAX_RINGTONE_DURATION;
        }

        return duration;
    }

    private int getMaxAudioDuration(String rawNameForPlay) {
        MBApp app = MBApp.getApp();

        Resources resources = app.getResources();
        int resID = resources.getIdentifier(rawNameForPlay, "raw", app.getPackageName());
        AssetFileDescriptor afd = resources.openRawResourceFd(resID);

        if(mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }

        int duration = getDuration(mediaPlayer, afd);

        if(MAX_RINGTONE_DURATION > 0 && duration > MAX_RINGTONE_DURATION) {
            duration = MAX_RINGTONE_DURATION;
        }

        return duration;
    }

    private void addRingtoneForPlaying(Uri ringtoneUri, int maxDuration, boolean isAlarm) {
        int playRingtoneIndex = alertTypes.indexOf(AlertType.TYPE_RINGTONE);

        final PlayRingtonePresenter playRingtonePresenter;

        if(playRingtoneIndex == -1) {
            playRingtonePresenter = new PlayRingtonePresenter();
        } else {
            playRingtonePresenter = (PlayRingtonePresenter) activePresenters.get(playRingtoneIndex);
        }

        playRingtonePresenter.reInit(mediaPlayer, ringtoneUri, maxDuration, isAlarm);

        if(playRingtoneIndex == -1) {
            activePresenters.add(playRingtonePresenter);
            alertTypes.add(AlertType.TYPE_RINGTONE);
        }
    }

    private void addVibrationForPlaying(int maxDuration) {
        int vibrateIndex = alertTypes.indexOf(AlertType.TYPE_VIBRATION);

        final VibratePresenter vibratePresenter;

        if(vibrateIndex == -1) {
            vibratePresenter = new VibratePresenter();
        } else {
            vibratePresenter = (VibratePresenter) activePresenters.get(vibrateIndex);
        }

        vibratePresenter.reInit(maxDuration);

        if(vibrateIndex == -1) {
            activePresenters.add(vibratePresenter);
            alertTypes.add(AlertType.TYPE_VIBRATION);
        }
    }

    private void addInternalPathForPlaying(String rawNameForPlay, MediaPlayer.OnCompletionListener onCompletionListener) {
        int playRawIndex = alertTypes.indexOf(AlertType.TYPE_RAW);

        final PlayAudioPresenter playAudioPresenter;

        if(playRawIndex == -1) {
            playAudioPresenter = new PlayAudioPresenter();
        } else {
            playAudioPresenter = (PlayAudioPresenter) activePresenters.get(playRawIndex);
        }

        playAudioPresenter.setInternalPathForPlay(rawNameForPlay);
        playAudioPresenter.setCallBack(onCompletionListener);

        if(playRawIndex == -1) {
            activePresenters.add(playAudioPresenter);
            alertTypes.add(AlertType.TYPE_RAW);
        }
    }

    private Uri searchAlarmUri(int alarmId) {
        Context context = MBApp.getApp();

        RingtoneManager ringtoneMgr = new RingtoneManager(context);
        ringtoneMgr.setType(RingtoneManager.TYPE_ALARM);
        Cursor alarms = ringtoneMgr.getCursor();
        Log.i(TAG, "playAlarm: total alarms = " + alarms.getCount());

        alarms.moveToPosition(alarmId - 4);
        Uri alarm = ringtoneMgr.getRingtoneUri(alarms.getPosition());

        if(alarm == null) {
            Log.i(TAG, "Cannot play nth Alarm. Playing default");
            alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        }

        return alarm;
    }

    /**
     * Allows to show additional dialog window while alarm is on.
     *
     * @param textMsg     Text message to show.
     * @param popupAction Popup code action.
     */
    private static void showDialogWithAction(String textMsg, int popupAction) {
        PopUp.showFromService(MBApp.getApp(), "",
                textMsg,
                R.drawable.message_face, R.drawable.blue_btn,
                0, /* TODO - nothing needs to be done */
                PopUp.TYPE_ALERT_LIGHT, popupAction);
    }

    /**
     * Simplified version to show additional dialog window while alarm.
     *
     * @param message Text message to show.
     * @param title   Title of dialog
     */
    private static void showDialog(String message, String title) {
        PopUp.showFromService(MBApp.getApp(), message,
                title,
                R.drawable.message_face, R.drawable.blue_btn,
                0, /* TODO - nothing needs to be done */
                PopUp.TYPE_ALERT_LIGHT);
    }

    @Override
    public void destroy() {
        stopPlaying();
        for(Presenter presenter : activePresenters) {
            presenter.destroy();
        }
        activePresenters.clear();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @IntDef(value = {TYPE_VIBRATION, TYPE_RINGTONE, TYPE_RAW})
    @interface AlertType {
        int TYPE_VIBRATION = 0;
        int TYPE_RINGTONE = 1;
        int TYPE_RAW = 2;
    }
}

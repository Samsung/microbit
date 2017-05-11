package com.samsung.microbit.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.ui.activity.PopUpActivity;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * To show a popup from ui thread use
 * {@link PopUp#show(String, String, int, int, int, int, View.OnClickListener, View.OnClickListener)} method.
 * <p/>
 * To show a popup from service use
 * {@link PopUp#showFromService(Context, String, String, int, int, int, int)} method.
 */
public class PopUp {

    private static final String TAG = PopUp.class.getSimpleName();

    public static final int TYPE_CHOICE = 0;//2 buttons type
    public static final int TYPE_ALERT = 1;//1 button type
    public static final int TYPE_PROGRESS = 2;//0 button progress.xml bar type
    public static final int TYPE_NOBUTTON = 3;//No button type
    public static final int TYPE_SPINNER = 4;//0 button type spinner
    public static final int TYPE_PROGRESS_NOT_CANCELABLE = 6;//0 button progress.xml bar type not cancelable
    // (backpress disabled)
    public static final int TYPE_SPINNER_NOT_CANCELABLE = 7;//0 button type spinner not cancelable (backpress disabled)
    public static final int TYPE_ALERT_LIGHT = 8;//Shows only once and do not leaves any history and cannot be recreated
    public static final int TYPE_NONE = 9;


    // Constants for giff animation options
    public static final int GIFF_ANIMATION_NONE = 0;
    public static final int GIFF_ANIMATION_FLASH = 1;
    public static final int GIFF_ANIMATION_ERROR = 2;


    public static final String INTENT_EXTRA_OK_ACTION = "Popup.extra.ok.type";
    public static final int OK_ACTION_NONE = 0;
    public static final int OK_ACTION_STOP_SERVICE_PLAYING = 1;


    //constants that indicate the type of request for which each type involves specific handling
    //see processNextPendingRequest
    private static final short REQUEST_TYPE_SHOW = 0;
    private static final short REQUEST_TYPE_HIDE = 1;
    private static final short REQUEST_TYPE_UPDATE_PROGRESS = 2;
    private static final short REQUEST_TYPE_MAX = 3;

    private static final String ACTION_FROM_SERVICE = "com.samsung.microbit.core.SHOWFROMSERVICE";


    private static int sCurrentType = TYPE_NONE; //current type of displayed popup  (TYPE_CHOICE, ...)

    private static final Context ctx = MBApp.getApp();

    private static boolean isCurrentRequestPending = false;

    private static class PendingRequest {
        private final Intent intent;
        private final View.OnClickListener okListener;
        private final View.OnClickListener cancelListener;
        private final short type;//type of request (REQUEST_TYPE_SHOW, ...)

        public PendingRequest(Intent intent, View.OnClickListener okListener,
                              View.OnClickListener cancelListener,
                              short type) {
            this.intent = intent;
            this.okListener = okListener != null ? okListener : defaultPressListener;
            this.cancelListener = cancelListener != null ? cancelListener : defaultPressListener;
            this.type = type;
        }
    }

    //FIFO queue for pending requests
    //This queue is a solution to handle the asynchronous behaviour
    // of Activity startActivity/onCreate
    private static Deque<PendingRequest> pendingQueue = new ArrayDeque<>();

    /**
     * Broadcast receiver that handles common popup states.
     */
    private static BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(PopUpActivity.INTENT_ACTION_OK_PRESSED)) {
                if(okPressListener != null)
                    okPressListener.onClick(null);
            } else if(intent.getAction().equals(PopUpActivity.INTENT_ACTION_CANCEL_PRESSED)) {
                if(cancelPressListener != null)
                    cancelPressListener.onClick(null);
            } else if(intent.getAction().equals(PopUpActivity.INTENT_ACTION_DESTROYED)) {
                Log.d(TAG, "INTENT_ACTION_DESTROYED size queue = " + pendingQueue.size());
                pendingQueue.poll();
                isCurrentRequestPending = false;
                sCurrentType = TYPE_NONE;
                processNextPendingRequest();
            } else if(intent.getAction().equals(PopUpActivity.INTENT_ACTION_CREATED)) {
                Log.d(TAG, "INTENT_ACTION_CREATED size queue = " + pendingQueue.size());
                PendingRequest request = pendingQueue.poll();
                if(request != null) {
                    sCurrentType = request.intent.getIntExtra(PopUpActivity.INTENT_EXTRA_TYPE, 0);
                    okPressListener = request.okListener;
                    cancelPressListener = request.cancelListener;
                }
                isCurrentRequestPending = false;
                processNextPendingRequest();
            }
        }
    };

    private static boolean registered = false;
    private static View.OnClickListener okPressListener = null;
    private static View.OnClickListener cancelPressListener = null;
    private static View.OnClickListener defaultPressListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            PopUp.hide();
        }
    };

    /**
     * Closes a popup window by creating a pending request
     * with intent to close the window. If current request isn't pending
     * then process next pending request.
     */
    public static void hide() {
        Log.d(TAG, "hide START");
        pendingQueue.add(new PendingRequest(new Intent(PopUpActivity.INTENT_ACTION_CLOSE),
                null, null, REQUEST_TYPE_HIDE));

        if(!isCurrentRequestPending) {
            processNextPendingRequest();
        }
    }

    /**
     * Updates progress of the progress bar, for example, during the
     * flashing process.
     *
     * @param val New progress value.
     */
    public static void updateProgressBar(int val) {
        Intent intent = new Intent(PopUpActivity.INTENT_ACTION_UPDATE_PROGRESS);
        intent.putExtra(PopUpActivity.INTENT_EXTRA_PROGRESS, val);

        pendingQueue.add(new PendingRequest(intent, null, null, REQUEST_TYPE_UPDATE_PROGRESS));
        if(!isCurrentRequestPending) {
            processNextPendingRequest();
        }
    }

    //Interface function for showing a popup inside a service plugin class
    //only supports TYPE_ALERT popup for now.
    public static void showFromService(Context context, String message, String title,
                                       int imageResId, int imageBackgroundResId,
                                       int animationCode, int type) {
        Log.d(TAG, "showFromService");
        Intent intent = new Intent(ACTION_FROM_SERVICE);
        putIntentExtra(intent, message, title, imageResId, imageBackgroundResId, animationCode, type);
        context.sendBroadcast(intent);
    }

    public static void showFromService(Context context, String message, String title,
                                       int imageResId, int imageBackgroundResId,
                                       int animationCode, int type, int okAction) {
        Log.d(TAG, "showFromService");
        Intent intent = new Intent(ACTION_FROM_SERVICE);
        putIntentExtra(intent, message, title, imageResId, imageBackgroundResId, animationCode, type);
        intent.putExtra(INTENT_EXTRA_OK_ACTION, okAction);
        context.sendBroadcast(intent);
    }

    private static void putIntentExtra(Intent intent, String message, String title,
                                       int imageResId, int imageBackgroundResId, int animationCode, int type) {
        intent.putExtra(PopUpActivity.INTENT_EXTRA_TYPE, type);
        intent.putExtra(PopUpActivity.INTENT_EXTRA_TITLE, title);
        intent.putExtra(PopUpActivity.INTENT_EXTRA_MESSAGE, message);
        intent.putExtra(PopUpActivity.INTENT_EXTRA_ICON, imageResId);
        intent.putExtra(PopUpActivity.INTENT_EXTRA_ICONBG, imageBackgroundResId);
        intent.putExtra(PopUpActivity.INTENT_GIFF_ANIMATION_CODE, animationCode);
        switch(type) {
            case TYPE_PROGRESS_NOT_CANCELABLE:
            case TYPE_SPINNER_NOT_CANCELABLE:
                intent.putExtra(PopUpActivity.INTENT_EXTRA_CANCELABLE, false);
                break;
            default:
                intent.putExtra(PopUpActivity.INTENT_EXTRA_CANCELABLE, true);
        }
    }

    private static class PopUpTask extends AsyncTask<Void, Void, Void> {
        private final String message;
        private final String title;
        private final int imageResId;
        private final int imageBackgroundResId;
        private int giffAnimationCode = 2;
        private final int type;
        private final View.OnClickListener okListener;
        private final View.OnClickListener cancelListener;

        @Override
        protected Void doInBackground(Void... voids) {
            Log.d("PopUpTask", "doInBackground");
            PopUp.showInternal(message, title, imageResId, imageBackgroundResId, giffAnimationCode, type,
                    okListener, cancelListener);
            return null;
        }

        public PopUpTask(String message, String title,
                         int imageResId, int imageBackgroundResId, int animationCode, int type,
                         View.OnClickListener okListener, View.OnClickListener cancelListener) {
            this.message = message;
            this.title = title;
            this.imageResId = imageResId;
            this.imageBackgroundResId = imageBackgroundResId;
            this.giffAnimationCode = animationCode;
            this.type = type;
            this.okListener = okListener;
            this.cancelListener = cancelListener;
        }
    }

    /**
     * Method used by activities to display Pop ups
     *
     * @param message              - pop up message
     * @param title,               - pop up title
     * @param imageResId           - pass 0 to imageResId to use default icon
     * @param imageBackgroundResId - pass 0 to imageBackgroundResId if no background is needed for icon
     * @param animationCode        - pass 0 to use default @imageResId icon.
     *                             The animationCode and the @imageResId are shown separately but never together (visibility is toggled)
     * @param type                 - pop up type e.g. spinner, cancellable;
     * @param okListener           - pass null to use default listener (which hides the pops) - you can override for your own purpose
     * @param cancelListener       - pass null to use default listener (which hides the pops)
     */
    //Interface function for showing popup inside an application activity
    public static boolean show(String message, String title,
                               int imageResId, int imageBackgroundResId, int animationCode, int type,
                               View.OnClickListener okListener, View.OnClickListener cancelListener) {
        new PopUpTask(message, title, imageResId, imageBackgroundResId, animationCode, type,
                okListener, cancelListener).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        return true;
    }

    private static synchronized boolean showInternal(String message, String title,
                                                     int imageResId, int imageBackgroundResId,
                                                     int animationCode, int type,
                                                     View.OnClickListener okListener,
                                                     View.OnClickListener cancelListener) {
        Log.d(TAG, "show START popup type " + type);

        if(!registered) {
            IntentFilter popupIntentFilter = new IntentFilter();
            popupIntentFilter.addAction(PopUpActivity.INTENT_ACTION_OK_PRESSED);
            popupIntentFilter.addAction(PopUpActivity.INTENT_ACTION_CANCEL_PRESSED);
            popupIntentFilter.addAction(PopUpActivity.INTENT_ACTION_DESTROYED);
            popupIntentFilter.addAction(PopUpActivity.INTENT_ACTION_CREATED);

            LocalBroadcastManager.getInstance(ctx).registerReceiver(broadcastReceiver, popupIntentFilter);
            registered = true;
        }

        Intent intent = new Intent(ctx, PopUpActivity.class);
        putIntentExtra(intent, message, title, imageResId, imageBackgroundResId, animationCode, type);

        pendingQueue.add(new PendingRequest(intent, okListener, cancelListener, REQUEST_TYPE_SHOW));

        if(!isCurrentRequestPending) {
            processNextPendingRequest();
        }

        return true;
    }

    /**
     * Allows to process a queue of pending requests while queue is not empty
     * and action is not done. Processes show, hide and update progress requests for now.
     */
    private static void processNextPendingRequest() {
        Log.d(TAG, "processNextPendingRequest START size = " + pendingQueue.size());
        boolean done = false;
        //keep iterating until we find async. request (HIDE, SHOW) to process
        while(!pendingQueue.isEmpty() && !done) {
            PendingRequest request = pendingQueue.peek();

            switch(request.type) {
                case REQUEST_TYPE_SHOW: {
                    Log.d(TAG, "processNextPendingRequest REQUEST_TYPE_SHOW");
                    if(sCurrentType != TYPE_NONE) {
                        Log.d(TAG, "processNextPendingRequest Update existing layout instead of hiding");
                        request.intent.setAction(PopUpActivity.INTENT_ACTION_UPDATE_LAYOUT);
                        LocalBroadcastManager.getInstance(ctx).sendBroadcastSync(request.intent);
                        sCurrentType = request.intent.getIntExtra(PopUpActivity.INTENT_EXTRA_TYPE, 0);
                        okPressListener = request.okListener;
                        cancelPressListener = request.cancelListener;
                        pendingQueue.poll();
                        continue;
                    }

                    done = true;
                    isCurrentRequestPending = true;
                    request.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    //Add additional flags to not be able to recreate the window.
                    if(request.intent.getIntExtra(PopUpActivity.INTENT_EXTRA_TYPE, 0) == TYPE_ALERT_LIGHT) {
                        request.intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY
                                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    }

                        ctx.startActivity(request.intent);
                    break;
                }
                case REQUEST_TYPE_HIDE: {
                    if(sCurrentType == TYPE_NONE) {
                        Log.d(TAG, "processNextPendingRequest Nothing to hide");
                        pendingQueue.poll();
                        continue;
                    }

                    done = true;
                    isCurrentRequestPending = true;
                    LocalBroadcastManager.getInstance(ctx).sendBroadcastSync(request.intent);
                    break;
                }
                case REQUEST_TYPE_UPDATE_PROGRESS: {
                    LocalBroadcastManager.getInstance(ctx).sendBroadcastSync(request.intent);
                    pendingQueue.poll();
                    break;
                }
            }
        }
        Log.d(TAG, "processNextPendingRequest END size = " + pendingQueue.size());
    }
}

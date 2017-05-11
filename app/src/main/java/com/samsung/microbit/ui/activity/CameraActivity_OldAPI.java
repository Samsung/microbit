package com.samsung.microbit.ui.activity;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.SensorManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import com.samsung.microbit.BuildConfig;
import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.GoogleAnalyticsManager;
import com.samsung.microbit.data.constants.Constants;
import com.samsung.microbit.data.constants.FileConstants;
import com.samsung.microbit.data.constants.InternalPaths;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.plugin.CameraPlugin;
import com.samsung.microbit.presentation.PlayAudioPresenter;
import com.samsung.microbit.service.PluginService;
import com.samsung.microbit.ui.view.CameraPreview;
import com.samsung.microbit.utils.ServiceUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Provides actions to interact with a camera.
 */
public class CameraActivity_OldAPI extends Activity {
    private static final String TAG = CameraActivity_OldAPI.class.getSimpleName();

    private CameraPreview mPreview;
    private ImageButton mButtonClick, mButtonBack_portrait, mButtonBack_landscape;

    private BroadcastReceiver mMessageReceiver;

    private Camera mCamera;
    private int mCameraIdx;
    private boolean mFrontCamera;

    private boolean mVideo = false;
    private boolean mIsRecording;
    private MediaRecorder mMediaRecorder;
    private File mVideoFile;

    private OrientationEventListener myOrientationEventListener;
    private int mCurrentRotation = -1;
    private int mStoredRotation = -1;
    private int mOrientationOffset = 0;
    private int mCurrentIconIndex = 0;
    private ArrayList<Drawable> mTakePhoto, mStartRecord, mStopRecord, mCurrentIconList;
    private Camera.Parameters mParameters;

    private boolean isActivityInBackground;
    private boolean isMakingPicOnResume;
    private boolean isRecordingVideoOnResume;

    private PlayAudioPresenter playAudioPresenter;

    private boolean debug = BuildConfig.DEBUG;

    /**
     * Media information listener to handle the media recorder events.
     */
    private MediaRecorder.OnInfoListener m_MediaInfoListener = new MediaRecorder.OnInfoListener() {
        @Override
        public void onInfo(MediaRecorder mr, int what, int extra) {
            switch(what) {
                case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
                    playAudioPresenter.setInternalPathForPlay(InternalPaths.MAX_VIDEO_RECORDED);
                    playAudioPresenter.start();
                    stopRecording();
                    break;
                case MediaRecorder.MEDIA_RECORDER_INFO_UNKNOWN:
                    logi("Error in media recorder - What = " + what + " extra = " + extra);
                    stopRecording();
                    break;
            }
        }
    };

    /**
     * Simplified method to log informational messages.
     *
     * @param message Message to log.
     */
    void logi(String message) {
        if(debug) {
            Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
        }
    }

    /**
     * Returns current camera identifier.
     *
     * @return Camera id.
     */
    private int getCurrentCamera() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();
        for(int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if(mFrontCamera && cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                logi("returning front camera");
                return camIdx;
            } else if(!mFrontCamera && cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                logi("returning back camera");
                return camIdx;
            }

        }
        return -1;
    }

    /**
     * Rotates icon for a given rotation angle.
     *
     * @param icon     Icon to rotate.
     * @param rotation Rotation angle.
     * @return Rotated icon.
     */
    private Drawable rotateIcon(Drawable icon, int rotation) {
        Bitmap existingBitmap = ((BitmapDrawable) icon).getBitmap();
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation - mOrientationOffset);
        Bitmap rotated = Bitmap.createBitmap(existingBitmap, 0, 0, existingBitmap.getWidth(), existingBitmap.getHeight(), matrix, true);
        return new BitmapDrawable(rotated);
    }

    /**
     * Create presets of rotated action icons.
     */
    private void createRotatedIcons() {
        Drawable icon = getResources().getDrawable(R.drawable.take_photo);
        mTakePhoto = new ArrayList<>();
        mTakePhoto.add(rotateIcon(icon, 0));
        mTakePhoto.add(rotateIcon(icon, -90));
        mTakePhoto.add(rotateIcon(icon, 180));
        mTakePhoto.add(rotateIcon(icon, -270));

        icon = getResources().getDrawable(R.drawable.start_record_icon);
        mStartRecord = new ArrayList<>();
        mStartRecord.add(rotateIcon(icon, 0));
        mStartRecord.add(rotateIcon(icon, -90));
        mStartRecord.add(rotateIcon(icon, 180));
        mStartRecord.add(rotateIcon(icon, -270));

        icon = getResources().getDrawable(R.drawable.stop_record_icon);
        mStopRecord = new ArrayList<>();
        mStopRecord.add(rotateIcon(icon, 0));
        mStopRecord.add(rotateIcon(icon, -90));
        mStopRecord.add(rotateIcon(icon, 180));
        mStopRecord.add(rotateIcon(icon, -270));
    }

    /**
     * Sets back buttons (portrait and landscape) on click listener.
     */
    private void setButtonForBackAction() {
        mButtonBack_portrait.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                startHomeActivity();
            }
        });

        mButtonBack_landscape.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                startHomeActivity();
            }
        });
    }

    /**
     * Action that occurs when a back button is pressed.
     */
    private void startHomeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Changes button's background to current icon index.
     */
    private void updateButtonClickIcon() {
        mButtonClick.setBackground(mCurrentIconList.get(mCurrentIconIndex));
        mButtonClick.invalidate();
    }

    /**
     * Changes camera rotation to a current rotation.
     */
    private void updateCameraRotation() {
        if(mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setRotation(getRotationCameraCorrection(mCurrentRotation)); //set rotation to save the picture
            mCamera.setParameters(parameters);
        }
    }

    /**
     * Changes button orientation and updates button icon according to
     * passed orientation value.
     *
     * @param rotation New button rotation.
     */
    private void updateButtonOrientation(int rotation) {
        rotation = (rotation + mOrientationOffset) % 360;
        int quant_rotation = 0;
        boolean buttonPortraitVisible = true;
        if(rotation == OrientationEventListener.ORIENTATION_UNKNOWN)
            return;
        if(rotation < 45 || rotation >= 315) {
            buttonPortraitVisible = true;
            quant_rotation = 0;
            mCurrentIconIndex = 0;
        } else if((rotation >= 135 && rotation < 225)) {
            buttonPortraitVisible = true;
            quant_rotation = 180;
            //mCurrentIconIndex = 2;
            mCurrentIconIndex = 0; //This way only 2 configurations are allowed
        } else if((rotation >= 45 && rotation < 135)) {
            buttonPortraitVisible = false;
            quant_rotation = 270;
            //mCurrentIconIndex = 1;
            mCurrentIconIndex = 3; //This way only 2 configurations are allowed
        } else if((rotation >= 225 && rotation < 315)) {
            buttonPortraitVisible = false;
            quant_rotation = 90;
            mCurrentIconIndex = 3;
        }
        if(quant_rotation != mCurrentRotation) {
            mCurrentRotation = quant_rotation;

            if(buttonPortraitVisible) {
                mButtonBack_landscape.setVisibility(View.INVISIBLE);
                mButtonBack_portrait.setVisibility(View.VISIBLE);
                mButtonBack_portrait.bringToFront();
            } else {
                mButtonBack_landscape.setVisibility(View.VISIBLE);
                mButtonBack_landscape.bringToFront();
                mButtonBack_portrait.setVisibility(View.INVISIBLE);
            }
            mButtonBack_portrait.invalidate();
            mButtonBack_landscape.invalidate();

            updateButtonClickIcon();
            updateCameraRotation();
        }
    }

    /**
     * Setups the button to take a picture.
     */
    private void setButtonForPicture() {
        mCurrentIconList = mTakePhoto;

        updateButtonClickIcon();
        mButtonClick.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);
                } catch(final Exception ex) {
                    sendCameraError();
                    Log.e(TAG, "Error during take picture", ex);
                }
            }
        });

        mButtonClick.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                mCamera.autoFocus(new AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean arg0, Camera arg1) {
                        //TODO Is there anything we have to do after autofocus?
                    }
                });
                return true;
            }
        });

    }

    /**
     * Setups preview button to take a picture state.
     */
    private void setPreviewForPicture() {
        mPreview.setSoundEffectsEnabled(false);
        mPreview.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mCamera.autoFocus(new AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean arg0, Camera arg1) {
                        //TODO Is there anything we have to do after autofocus?
                    }
                });
            }
        });
    }

    /**
     * Stops the media recorder and releases it.
     */
    private void stopRecording() {
        logi("Stop recording");
        mMediaRecorder.stop(); // stop the recording
        refreshGallery(mVideoFile);
        releaseMediaRecorder(); // release the MediaRecorder object
        mIsRecording = false;
        mCurrentIconList = mStartRecord;
        updateButtonClickIcon();
        releaseMediaRecorder();
        resetCam();
    }

    /**
     * Setups the button to record a video.
     */
    private void setButtonForVideo() {

        mCurrentIconList = mStartRecord;
        updateButtonClickIcon();
        mButtonClick.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if(mIsRecording) {
                    // stop recording and release mCamera
                    stopRecording();
                } else {
                    if(!prepareMediaRecorder()) {
                        sendCameraError();
                        Log.e(TAG, "Error preparing mediaRecorder");
                        finish();
                    }

                    mCurrentIconList = mStopRecord;
                    updateButtonClickIcon();
                    //TODO Video recording crashing. Check #112 for details. Temporary fix for the BETT
                    //indicateVideoRecording();
                    //TODO Check that is true
                    // work on UiThread for better performance
                    runOnUiThread(new Runnable() {
                        public void run() {
                            try {
                                mMediaRecorder.start();
                            } catch(final Exception ex) {
                                sendCameraError();
                                Log.e(TAG, "Error during video recording", ex);
                                finish();
                            }
                        }
                    });
                    mIsRecording = true;
                }
            }
        });
    }

    private void indicateVideoRecording() {
        logi("indicateVideoRecording");
        if(mParameters == null && mCamera != null) {
            setParameters();

        }
        new CountDownTimer(Constants.MAX_VIDEO_RECORDING_TIME_MILLIS, Constants.VIDEO_FLASH_PICK_INTERVAL) {
            boolean flashON = false;

            public void onTick(long millisUntilFinished) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if(mIsRecording && mCamera != null) {
                            if(flashON) {
                                logi("turning flash ON");
                                mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);

                            } else {
                                logi("turning flash OFF");
                                mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                            }
                            mCamera.setParameters(mParameters);
                            flashON = !flashON;
                        }
                    }
                });
            }

            public void onFinish() {
                //Do nothing
            }
        }.start();
    }

    /**
     * Logs supported flash modes, resets parameters and
     * enables shutter sound.
     */
    private void setParameters() {
        mParameters = mCamera.getParameters();

        List<String> flashModes = mParameters.getSupportedFlashModes();
        for(String flashmode : flashModes) {
            logi("Supported flash mode : " + flashmode);
        }
        mCamera.setParameters(mParameters);
        mCamera.enableShutterSound(true);
    }

    private void setPreviewForVideo() {
        //TODO: add implementation
    }

    private int getRotationCameraCorrection(int current_rotation) {
        int degree = (current_rotation + 270) % 360;

        int result;
        String model = Build.MODEL;

        if(model.contains("Nexus 5X")) {
            //Workaround for Nexus 5X camera issue
            //TODO: Use Camera API 2 to fix this correctly
            result = (mOrientationOffset + degree) % 360;

            if(!mFrontCamera) {
                if(result == 0)
                    result += 180;
                else if(result == 180)
                    result = 0;
            }

        } else {
            if(mFrontCamera) {
                result = (mOrientationOffset + degree) % 360;
            } else { // back-facing
                result = (mOrientationOffset - degree + 360) % 360;
            }
        }

        return result;
    }

    /**
     * Sends camera error message using IPC message manager.
     */
    private void sendCameraError() {
        CmdArg cmd = new CmdArg(0, "Camera Error");
        ServiceUtils.sendReplyCommand(PluginService.CAMERA, cmd);
    }

    private void setOrientationOffset() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();

        Point screenSize = new Point(0, 0);
        getWindowManager().getDefaultDisplay().getSize(screenSize);

        logi("Display size " + screenSize.x + "x" + screenSize.y);

        //Checking if it's a tablet
        if(rotation == 0 || rotation == 2) {
            if(screenSize.x > screenSize.y) {
                //Tablet
                mOrientationOffset = 270;
                logi("Tablet");
            } else {
                //Phone
                mOrientationOffset = 0;
                logi("Phone");
            }
        } else {
            if(screenSize.x > screenSize.y) {
                //Phone
                mOrientationOffset = 0;
                logi("Phone");
            } else {
                //Tablet
                mOrientationOffset = 270;
                logi("Tablet");
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        logi("onCreate() :: Start");
        super.onCreate(savedInstanceState);

        GoogleAnalyticsManager.getInstance().sendViewEventStats(CameraActivity_OldAPI.class.getSimpleName());

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if(savedInstanceState == null) {
            mFrontCamera = true;
        }

        playAudioPresenter = new PlayAudioPresenter();

        createRotatedIcons();

        setOrientationOffset();

        myOrientationEventListener
                = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {

            @Override
            public void onOrientationChanged(int arg0) {
                updateButtonOrientation(arg0);
            }
        };

        mCamera = null;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_camera_old_api);
        String action = getIntent().getAction();

        //When user finished with camera, so there is nothing to do here.
        if (action != null) {
            if (action.equals(CameraPlugin.OPEN_FOR_PIC_ACTION)) {
                mVideo = false;
            } else if (action.equals(CameraPlugin.OPEN_FOR_VIDEO_ACTION)) {
                mVideo = true;
            }
        } else {
            //Return to Home screen
            startHomeActivity();
        }

        SurfaceView mSurfaceView = new SurfaceView(this);
        mPreview = new CameraPreview(this, mSurfaceView);
        mPreview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        ((FrameLayout) findViewById(R.id.camera_preview_container)).addView(mPreview);
        mPreview.setKeepScreenOn(true);

        mButtonClick = (ImageButton) findViewById(R.id.picture);
        mButtonBack_portrait = (ImageButton) findViewById(R.id.back_portrait);
        mButtonBack_landscape = (ImageButton) findViewById(R.id.back_landscape);

        setButtonForBackAction();

        if(mVideo) {
            //Setup specific to OPEN_FOR_VIDEO
            setButtonForVideo();
            setPreviewForVideo();
        } else {
            //Setup specific to OPEN_FOR_PIC
            setButtonForPicture();
            setPreviewForPicture();
        }

        updateButtonOrientation(0);

        mMessageReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals("CLOSE")) {
                    finish();
                } else if(!mVideo && intent.getAction().equals("TAKE_PIC")) {
                    mFrontCamera = true;
                    takePic();
                } else if(intent.getAction().equals("TOGGLE_CAMERA")) {
                    toggleCamera();
                } else if(mVideo && !mIsRecording && intent.getAction().equals("START_VIDEO")) {
                    mFrontCamera = true;
                    if(isActivityInBackground) {
                        bringActivityToFront();
                        isRecordingVideoOnResume = true;
                    } else {
                        recordVideo();
                    }
                } else if(mVideo && mIsRecording && intent.getAction().equals("STOP_VIDEO")) {
                    mButtonClick.callOnClick();
                } else {
                    //Wrong sequence of commands
                    CmdArg cmd = new CmdArg(0, "Wrong Camera Command Sequence");
                    ServiceUtils.sendReplyCommand(PluginService.CAMERA, cmd);
                    Log.e(TAG, "Wrong command sequence");
                }
            }
        };

        this.registerReceiver(mMessageReceiver, new IntentFilter("CLOSE"));

        if(mVideo) {
            this.registerReceiver(mMessageReceiver, new IntentFilter("START_VIDEO"));
            this.registerReceiver(mMessageReceiver, new IntentFilter("STOP_VIDEO"));
            this.registerReceiver(mMessageReceiver, new IntentFilter("TOGGLE_CAMERA"));
        } else {
            this.registerReceiver(mMessageReceiver, new IntentFilter("TAKE_PIC"));
            this.registerReceiver(mMessageReceiver, new IntentFilter("TOGGLE_CAMERA"));
        }

        logi("onCreate() :: Done");
    }

    private void bringActivityToFront() {
        Intent intent = new Intent(getApplicationContext(), CameraActivity_OldAPI.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    /**
     * Brings camera activity to the front or recreates it.
     */
    private void toggleCamera() {
        mFrontCamera = !mFrontCamera;
        if(isActivityInBackground) {
            bringActivityToFront();
        } else {
            recreate();
        }
    }

    /**
     * If the activity is in background then bring it to the front,
     * else start taking a picture.
     */
    private void takePic() {
        if(isActivityInBackground) {
            bringActivityToFront();
            isMakingPicOnResume = true;
        } else {
            startTakePicCounter();
        }
    }

    /**
     * Starts taking a picture countdown with playing an audio
     * and showing a text countdown for defined interval, and then takes a picture.
     */
    private void startTakePicCounter() {

        playAudioPresenter.setInternalPathForPlay(InternalPaths.TAKING_PHOTO_AUDIO);
        playAudioPresenter.start();

        @SuppressLint("ShowToast") final Toast toast = Toast.makeText(MBApp.getApp().getApplicationContext(), "bbb", Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);

        //Toast.LENGTH_SHORT will keep the toast for 2s, our interval is 1s and calling toast.show()
        //after 1s will cause some count to be missed. Only call toast.show() just before 2s interval.
        //Also add delay to show the "Ready" toast.
        new CountDownTimer(Constants.PIC_COUNTER_DURATION_MILLIS, Constants.PIC_COUNTER_INTERVAL_MILLIS) {

            public void onTick(long millisUntilFinished) {
                int count = (int) millisUntilFinished / Constants.PIC_COUNTER_INTERVAL_MILLIS;
                toast.setText("Ready in... " + count);
                if(count % 2 != 0)
                    toast.show();
            }

            public void onFinish() {
                toast.setText("Ready");
                toast.show();
                mButtonClick.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mButtonClick.callOnClick();
                    }

                }, 200);
            }
        }.start();
    }

    @Override
    protected void onResume() {
        logi("onCreate() :: onResume");

        super.onResume();

        isActivityInBackground = false;
        //This intent filter has to be set even if no camera is found otherwise the unregisterReceiver()
        //fails during the onPause()
        if(myOrientationEventListener.canDetectOrientation()) {
            logi("DetectOrientation Enabled");
            myOrientationEventListener.enable();
        } else {
            logi("DetectOrientation Disabled");
        }

        mCameraIdx = getCurrentCamera();
        logi("mCameraIdx = " + mCameraIdx);
        try {
            mCamera = Camera.open(mCameraIdx);
            if(mCamera == null) {
                logi("Couldn't open the camera");
            }
            logi("Step 2");
            mPreview.setCamera(mCamera, mCameraIdx);
            logi("Step 3");

            logi("Step 3");
            mPreview.restartCameraPreview();
            logi("Step 4");
            updateCameraRotation();
            logi("onCreate() :: onResume # ");

            if(isMakingPicOnResume) {
                isMakingPicOnResume = false;
                startTakePicCounter();
            } else if(isRecordingVideoOnResume) {
                isRecordingVideoOnResume = false;
                recordVideo();
            }

        } catch(RuntimeException ex) {
            Log.e(TAG, ex.toString());
            Toast.makeText(this, getString(R.string.camera_not_found), Toast.LENGTH_LONG).show();
            sendCameraError();
            finish();
        }
    }

    /**
     * Plays notification sound and starts recording a video.
     */
    private void recordVideo() {
        playAudioPresenter.setInternalPathForPlay(InternalPaths.RECORDING_VIDEO_AUDIO);
        playAudioPresenter.start();
        mButtonClick.postDelayed(new Runnable() {
            @Override
            public void run() {
                mButtonClick.callOnClick();
            }

        }, 200);
    }

    @Override
    protected void onPause() {

        logi("onCreate() :: onPause");

        if(mIsRecording) {
            stopRecording();
        }

        if(myOrientationEventListener.canDetectOrientation()) {
            logi("DetectOrientation Disabled");
            myOrientationEventListener.disable();
        }

        if(mCamera != null) {
            mCamera.stopPreview();
            mPreview.setCamera(null, -1);
            mCamera.release();
            mCamera = null;
        }
        super.onPause();

        isActivityInBackground = true;
    }

    /**
     * Resets camera parameters and starts camera preview.
     */
    private void resetCam() {
        try {
            Camera.Parameters parameters = mCamera.getParameters();
            logi("Set Flash mode ON");
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(parameters);
            mCamera.startPreview();
            mCamera.autoFocus(new AutoFocusCallback() {
                public void onAutoFocus(boolean success, Camera camera) {
                    //TODO: add implementation or leave a comment if it's nothing to do here
                }
            });
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            mCamera.setParameters(parameters);
        } catch(RuntimeException e) {
            Log.e(TAG, e.toString());
        }
    }

    private void refreshGallery(File file) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }

    //TODO Add Sound here
    //Currently if the device is on silent mode no sound is going to be heard
    ShutterCallback shutterCallback = new ShutterCallback() {
        public void onShutter() {
            ImageView blinkRect = (ImageView) findViewById(R.id.blink_rectangle);
            blinkRect.setVisibility(View.VISIBLE);
            blinkRect.bringToFront();
            blinkRect.invalidate();
        }
    };

    /**
     * Shows message and plays notification sound that indicate that a photo has been taken.
     */
    PictureCallback rawCallback = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            // Display toast here and play audio
            Toast toast = Toast.makeText(MBApp.getApp().getApplicationContext(), "Photo taken", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();

            playAudioPresenter.setInternalPathForPlay(InternalPaths.PICTURE_TAKEN_AUDIO);
            playAudioPresenter.start();

        }
    };

    /**
     * Saves a picture and resets a camera.
     */
    PictureCallback jpegCallback = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            new SaveImageTask(CameraActivity_OldAPI.this).execute(data);
            DrawBlink();
            resetCam();
        }
    };

    void DrawBlink() {
        SystemClock.sleep(500);
        ImageView blinkRect = (ImageView) findViewById(R.id.blink_rectangle);
        blinkRect.setVisibility(View.INVISIBLE);
        blinkRect.invalidate();
    }

    @Override
    protected void onStart() {
        logi("onCreate() :: onStart");
        //Informing microbit that the mCamera is active now
        CmdArg cmd = new CmdArg(0, "Camera on");
        ServiceUtils.sendReplyCommand(PluginService.CAMERA, cmd);
        super.onStart();
        GoogleAnalyticsManager.getInstance().activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        GoogleAnalyticsManager.getInstance().activityStop(this);
    }

    @Override
    protected void onDestroy() {
        logi("onCreate() :: onDestroy");
        //Informing microbit that the mCamera is active now
        CmdArg cmd = new CmdArg(0, "Camera off");
        ServiceUtils.sendReplyCommand(PluginService.CAMERA, cmd);

        this.unregisterReceiver(mMessageReceiver);

        playAudioPresenter.destroy();

        super.onDestroy();
    }

    /**
     * Provides an asynchronous task to save a picture on a device.
     */
    private static class SaveImageTask extends AsyncTask<byte[], Void, File> {

        private final WeakReference<CameraActivity_OldAPI> cameraActivity_oldAPIWeakReference;

        public SaveImageTask(CameraActivity_OldAPI cameraActivity_oldAPI) {
            this.cameraActivity_oldAPIWeakReference = new WeakReference<>(cameraActivity_oldAPI);
        }

        @Override
        protected void onPostExecute(File savedFile) {
            super.onPostExecute(savedFile);
            if(cameraActivity_oldAPIWeakReference.get() != null && savedFile != null) {
                cameraActivity_oldAPIWeakReference.get().refreshGallery(savedFile);
                CmdArg cmd = new CmdArg(0, "Camera picture saved");
                ServiceUtils.sendReplyCommand(PluginService.CAMERA, cmd);
            }
        }

        @Override
        protected File doInBackground(byte[]... data) {

            // Write to SD Card
            try {
                File dir = FileConstants.MEDIA_OUTPUT_FOLDER;

                if(!dir.exists()) {
                    dir.mkdirs();
                }

                //TODO defining the file name
                String fileName = String.format(Locale.getDefault(), "%d.jpg", System.currentTimeMillis());
                File outFile = new File(dir, fileName);

                FileOutputStream outStream = new FileOutputStream(outFile);
                outStream.write(data[0]);

                outStream.flush();
                outStream.close();

                return outFile;
            } catch(IOException e) {
                Log.e(TAG, e.toString());
            }
            return null;
        }
    }

    private void releaseMediaRecorder() {
        if(mMediaRecorder != null) {
            mMediaRecorder.reset(); // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mVideoFile = null;
            mCamera.lock(); // lock camera for later use
        }
    }

    private boolean prepareMediaRecorder() {

        if(mCameraIdx < 0 || mCamera == null)
            return false;

        mMediaRecorder = new MediaRecorder();

        mMediaRecorder.setOnInfoListener(m_MediaInfoListener);

        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        //Rohit - Removed the audio source
        //mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        //TODO Check because depending on the quality on some devices the MediaRecorder doesn't work
        if(CamcorderProfile.hasProfile(mCameraIdx, CamcorderProfile.QUALITY_TIME_LAPSE_HIGH))
            mMediaRecorder.setProfile(CamcorderProfile.get(mCameraIdx, CamcorderProfile.QUALITY_TIME_LAPSE_HIGH));
        else if(CamcorderProfile.hasProfile(mCameraIdx, CamcorderProfile.QUALITY_TIME_LAPSE_LOW))
            mMediaRecorder.setProfile(CamcorderProfile.get(mCameraIdx, CamcorderProfile.QUALITY_TIME_LAPSE_LOW));
        else {
            releaseMediaRecorder();
            Log.e(TAG, "Error preparing media Recorder: no CamcorderProfile available");
            return false;
        }

        //Setting output file
        File dir = FileConstants.MEDIA_OUTPUT_FOLDER;
        if(!dir.exists()) {
            dir.mkdirs();
        }
        String fileName = String.format(Locale.getDefault(), "%d.mp4", System.currentTimeMillis());
        mVideoFile = new File(dir, fileName);
        mMediaRecorder.setOutputFile(mVideoFile.getAbsolutePath());

        //Setting limits
        mMediaRecorder.setMaxDuration(Constants.MAX_VIDEO_RECORDING_TIME_MILLIS);
        mMediaRecorder.setMaxFileSize(Constants.MAX_VIDEO_FILE_SIZE_BYTES);

        int rotation = getRotationCameraCorrection(mCurrentRotation);
        mMediaRecorder.setOrientationHint(rotation);

        try {
            mMediaRecorder.prepare();
        } catch(IOException e) {
            releaseMediaRecorder();
            Log.e(TAG, e.toString());
            return false;
        }
        return true;
    }
}

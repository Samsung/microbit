package com.samsung.microbit.ui.view;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import java.util.List;

import static com.samsung.microbit.BuildConfig.DEBUG;

public class CameraPreview extends ViewGroup implements SurfaceHolder.Callback {
    private static final String TAG = CameraPreview.class.getSimpleName();

    private void logi(String message) {
        if(DEBUG) {
            Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
        }
    }

    SurfaceView mSurfaceView;
    SurfaceHolder mHolder;
    Size mPreviewSize;
    List<Size> mSupportedPreviewSizes;
    Camera mCamera;
    int mCameraIdx;

    public SurfaceHolder getHolder() {
        return mHolder;
    }

    public void restartCameraPreview() {
        logi("restartCameraPreview");
        if(mCamera != null && mPreviewSize != null) {
            try {
                mCamera.stopPreview();
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
                mCamera.setParameters(parameters);
                mCamera.setPreviewDisplay(mHolder);
                logi("Set Flash mode ON");
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                mCamera.setParameters(parameters);
                mCamera.startPreview();
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                    }
                });
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                mCamera.setParameters(parameters);

            } catch(Exception e) {
                Log.e(TAG, "IOException caused by setPreviewDisplay()", e);
            }
        }
    }

    public int getCameraDisplayOrientation(int cameraId, android.hardware.Camera mCamera) {
        Activity mParentActivity = (Activity) getContext();

        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = mParentActivity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch(rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        logi("info.orientation " + info.orientation + " degrees " + degrees);

        int result;
        if(info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;
    }

    public CameraPreview(Context context) {
        super(context);
    }

    public CameraPreview(Context context, SurfaceView sv) {
        this(context);

        mCameraIdx = -1;
        mCamera = null;

        mSurfaceView = sv;
        addView(mSurfaceView);

        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void setCamera(Camera camera, int idx) {
        logi("setCamera");
        mCamera = camera;
        mCameraIdx = idx;
        if(mCamera != null) {
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();

            // get Camera parameters
            Camera.Parameters params = mCamera.getParameters();
            int mRotation = getCameraDisplayOrientation(mCameraIdx, mCamera);
            mCamera.setDisplayOrientation(mRotation);
            logi("Camera rotation " + mRotation);

            List<String> focusModes = params.getSupportedFocusModes();
            if(focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                // set the focus mode
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                // set Camera parameters
                mCamera.setParameters(params);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        logi("onMeasure()");

        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        setMeasuredDimension(width, height);

        logi("setMeasuredDimension " + width + " " + height);

        if(mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        logi("onLayout()");

        if(changed) {
            final int width = r - l;
            final int height = b - t;

            int previewWidth = width;
            int previewHeight = height;

            if(mPreviewSize != null) {
                //The previewSize is always in landscape mode,
                // so we have to change the dimensions accordingly
                boolean landscapeMode = mPreviewSize.width > mPreviewSize.height && width > height;
                previewWidth = (landscapeMode) ? mPreviewSize.width : mPreviewSize.height;
                previewHeight = (landscapeMode) ? mPreviewSize.height : mPreviewSize.width;
            }

            // Center the child SurfaceView within the parent.
            //Code to have a smaller image if the camera preview ratio doesn't match the screen
//            if (width * previewHeight > height * previewWidth) {
//                final int scaledChildWidth = previewWidth * height / previewHeight;
//                mSurfaceView.layout((width - scaledChildWidth) / 2, 0,
//                        (width + scaledChildWidth) / 2, height);
//            } else {
//                final int scaledChildHeight = previewHeight * width / previewWidth;
//                mSurfaceView.layout(0, (height - scaledChildHeight) / 2,
//                        width, (height + scaledChildHeight) / 2);
//            }
            //Code to have a preview that cover all the screen. If the camera preview ratio doesn't
            //match the screen size the image is cropped
            if(width * previewHeight > height * previewWidth) {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                mSurfaceView.layout(0, (height - scaledChildHeight) / 2,
                        width, (height + scaledChildHeight) / 2);
            } else {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                mSurfaceView.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        logi("surfaceCreated()");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        if(mCamera != null) {
            mCamera.stopPreview();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        logi("surfaceChanged()");
        if(mHolder.getSurface() == null) {
            return;
        }

        restartCameraPreview();
    }

    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;

        //The list of camera preview sizes inside sizes is always with w>h (landscape)
        //When in portrait mode w and h has to be inverted to have a proper comparison
        if(w < h) {
            int temp = w;
            w = h;
            h = temp;
        }

        double targetRatio = (double) w / h;
        if(sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for(Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if(Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if(Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if(optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for(Size size : sizes) {
                if(Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        logi("optimalSize = " + optimalSize.width + " x " + optimalSize.height);

        //Even in portrait case we provide the optimal size in landscape mode
        //because it's going to be used to set the camera parameter
        return optimalSize;
    }
}

package com.samsung.microbit.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.samsung.microbit.R;
import com.samsung.microbit.core.GoogleAnalyticsManager;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;
import pl.droidsonroids.gif.AnimationListener;

/**
 * Represents a loading screen activity.
 * Provides methods to create and manage splash screen animation.
 */
public class SplashScreenActivity extends Activity implements View.OnClickListener {
    private static final String EXTRA_ANIMATION_STARTED = "animation_started";
    private static final int ANIM_STEP_ONE_DURATION = 1000;
    private static final int ANIM_STEP_TWO_DURATION = 800;
    private static final int ANIM_STEP_TWO_DELAY = 600;
    private static final int ANIM_STEP_THREE_DELAY = 600;
    private static final int ANIM_STEP_THREE_DURATION = 800;
    private static final int ANIM_FADE_OUT_GIF_FIRST_FRAME_DURATION = 400;

    private ViewGroup mLogoLayout;
    private TextView mDevByText;
    private ImageView mLogo;
    private ViewGroup mGifImageLayout;
    private ImageView mGifImageFirstFrame;
    private GifImageView mGifImage;
    private GifDrawable mGifDrawable;
    private Handler mLastAnimStepHandler;

    private boolean mAnimationStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash_screen);
        initViews();

        GoogleAnalyticsManager.getInstance().sendAppStats(SplashScreenActivity.class.getSimpleName());
    }

    private void initViews() {
        mLogoLayout = (RelativeLayout) findViewById(R.id.splash_screen_logo_layout);
        mDevByText = (TextView) findViewById(R.id.splash_screen_devby_text);
        mLogo = (ImageView) findViewById(R.id.splash_screen_logo);
        mGifImageLayout = (RelativeLayout) findViewById(R.id.splash_screen_gif_image_layout);
        mGifImageFirstFrame = (ImageView) findViewById(R.id.splash_screen_gif_image_first_frame);
        mGifImage = (GifImageView) findViewById(R.id.splash_screen_gif_image);
        mLastAnimStepHandler = new Handler();

        findViewById(R.id.splash_screen_layout).setOnClickListener(this);
    }

    /**
     * Prepares views by setting presents before animation started.
     */
    private void prepareViews() {
        mDevByText.setAlpha(0f);
        mLogo.setAlpha(0f);
        mLogo.setScaleX(0.5f);
        mLogo.setScaleY(0.5f);
    }

    private void releaseViews() {
        mLogoLayout = null;
        mDevByText = null;
        mLogo = null;
        mGifImageLayout = null;
        mGifImageFirstFrame = null;
        mGifImage = null;
    }

    /**
     * Runs a splash screen animation.
     */
    private void startAnimation() {
        mAnimationStarted = true;

        final boolean isPortrait = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT;

        //Setup views presets.
        prepareGif(isPortrait);
        prepareViews();

        //Step 1. Show logo and text.
        showLogoAnim();

        //Step 2. Move logo layout and gif image up.
        moveLogoAndGifUpAnim(isPortrait);

        //Step 3. Move logo layout out.
        mLastAnimStepHandler.postDelayed(mLastAnimStepCallback,
                ANIM_STEP_ONE_DURATION + ANIM_STEP_TWO_DURATION
                        + ANIM_STEP_TWO_DELAY + ANIM_STEP_THREE_DELAY);
    }

    /**
     * Start animation of the first step of splash screen animation that shows a logo.
     */
    private void showLogoAnim() {
        mLogo.animate().alpha(1f).scaleY(1f).scaleX(1f).setDuration(ANIM_STEP_ONE_DURATION).start();
        mDevByText.animate().alpha(1f).setDuration(ANIM_STEP_ONE_DURATION).start();
    }

    /**
     * Prepares gif image view before it's started.
     *
     * @param isPortrait Checks if device is in portrait view.
     */
    private void prepareGif(final boolean isPortrait) {
        mGifDrawable = (GifDrawable) mGifImage.getDrawable();
        mGifDrawable.stop();
        mGifDrawable.addAnimationListener(mGifAnimListener);

        RelativeLayout.LayoutParams params =
                (RelativeLayout.LayoutParams) mGifImageLayout.getLayoutParams();

        int halfOfScreen;

        //Change size of the gif image.
        if(isPortrait) {
            final int screenHeight = getResources().getDisplayMetrics().heightPixels;
            halfOfScreen = screenHeight / 2;
            params.height = halfOfScreen;
            mGifImageLayout.setTranslationY(screenHeight);
        } else {
            final int screenWidth = getResources().getDisplayMetrics().widthPixels;
            halfOfScreen = screenWidth / 2;
            params.width = halfOfScreen;
            mGifImageLayout.setTranslationX(screenWidth);
            mGifImageLayout.setPadding(0, 0, 0, 0);
        }

        mGifImageLayout.setLayoutParams(params);
    }

    /**
     * Step 2 splash screen animation that moves a logo and a gif image upside.
     *
     * @param isPortrait Checks if device is in portrait view.
     */
    private void moveLogoAndGifUpAnim(final boolean isPortrait) {
        int halfOfScreen;
        int quarterOfScreen;

        final int delay = ANIM_STEP_ONE_DURATION + ANIM_STEP_TWO_DELAY;

        if(isPortrait) {
            final int screenHeight = getResources().getDisplayMetrics().heightPixels;
            halfOfScreen = screenHeight / 2;
            quarterOfScreen = screenHeight / 4;

            //Move gif image to the half of the screen up.
            mGifImageLayout.animate().translationY(halfOfScreen).setDuration(ANIM_STEP_TWO_DURATION)
                    .setStartDelay(delay).start();

            //Move logo layout up by the half of the screen up.
            mLogoLayout.animate().translationY(-halfOfScreen).setDuration(ANIM_STEP_TWO_DURATION)
                    .setStartDelay(delay).start();

            //Logo and the text moves along with the layout, so move them to the quarter
            //of the screen beneath to stay on the half of the screen.
            mDevByText.animate().translationY(quarterOfScreen).setDuration(ANIM_STEP_TWO_DURATION)
                    .setStartDelay(delay).start();
            mLogo.animate().translationY(quarterOfScreen).setDuration(ANIM_STEP_TWO_DURATION)
                    .setStartDelay(delay).start();
        } else {
            final int screenWidth = getResources().getDisplayMetrics().widthPixels;
            halfOfScreen = screenWidth / 2;
            quarterOfScreen = screenWidth / 4;

            //Move gif image to the half of the screen from right to left.
            mGifImageLayout.animate().translationX(halfOfScreen).setDuration(ANIM_STEP_TWO_DURATION)
                    .setStartDelay(delay).start();

            //Move logo layout up by the half of the screen up from right to left.
            mLogoLayout.animate().translationX(-halfOfScreen).setDuration(ANIM_STEP_TWO_DURATION)
                    .setStartDelay(delay).start();

            //Move logo and text from left to right to stay on the half of the screen.
            mDevByText.animate().translationX(quarterOfScreen).setDuration(ANIM_STEP_TWO_DURATION)
                    .setStartDelay(delay).start();
            mLogo.animate().translationX(quarterOfScreen).setDuration(ANIM_STEP_TWO_DURATION)
                    .setStartDelay(delay).start();
        }
    }

    /**
     * Callback for the last step of animation progress that follows with the gif animation.
     */
    private final Runnable mLastAnimStepCallback = new Runnable() {
        @Override
        public void run() {
            final boolean isPortrait = getResources().getConfiguration().orientation
                    == Configuration.ORIENTATION_PORTRAIT;

            //Position of the screen to center gif image.
            int positionOnScreen;

            if(isPortrait) {
                final int screenHeight = getResources().getDisplayMetrics().heightPixels;

                //Move logo layout out of the screen.
                mLogoLayout.animate().translationY(-screenHeight).alpha(0f)
                        .setStartDelay(0)
                        .setDuration(ANIM_STEP_THREE_DURATION).start();

                positionOnScreen = screenHeight / 8;

                //Move gif image to center of the screen.
                mGifImageLayout.animate().translationY(positionOnScreen)
                        .setStartDelay(0)
                        .setDuration(ANIM_STEP_THREE_DURATION).start();
            } else {
                final int screenWidth = getResources().getDisplayMetrics().widthPixels;

                mLogoLayout.animate().translationX(-screenWidth).alpha(0f)
                        .setStartDelay(0)
                        .setDuration(ANIM_STEP_THREE_DURATION).start();

                positionOnScreen = screenWidth / 4;

                mGifImageLayout.animate().translationX(positionOnScreen)
                        .setStartDelay(0)
                        .setDuration(ANIM_STEP_THREE_DURATION).start();
            }

            //Fade out the first frame of the gif animation.
            mGifImageFirstFrame.animate().alpha(0f)
                    .setDuration(ANIM_FADE_OUT_GIF_FIRST_FRAME_DURATION)
                    .setStartDelay(ANIM_STEP_THREE_DURATION).withEndAction(new Runnable() {
                @Override
                public void run() {
                    mGifDrawable.start();
                }
            }).start();

        }
    };

    /**
     * Listener of a gif image to start home activity after animation is completed.
     */
    private AnimationListener mGifAnimListener = new AnimationListener() {
        @Override
        public void onAnimationCompleted(int i) {
            startHomeActivity();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if(!mAnimationStarted) {
            startAnimation();
        } else {
            startHomeActivity();
        }
    }

    /**
     * Ends a splash screen and starts Home activity.
     */
    private void startHomeActivity() {
        startActivity(new Intent(SplashScreenActivity.this, HomeActivity.class));
        finish();
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
    protected void onPause() {
        super.onPause();
        removeAnimation();
    }

    private void removeAnimation() {
        if(mAnimationStarted) {
            mLastAnimStepHandler.removeCallbacks(mLastAnimStepCallback);
            mGifDrawable.removeAnimationListener(mGifAnimListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeAnimation();
        releaseViews();
    }

    @Override
    public void onClick(View v) {
        startHomeActivity();
    }
}

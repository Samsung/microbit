package com.samsung.microbit.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.GoogleAnalyticsManager;
import com.samsung.microbit.data.constants.PermissionCodes;
import com.samsung.microbit.service.IPCService;
import com.samsung.microbit.ui.PopUp;
import com.samsung.microbit.utils.FileUtils;
import com.samsung.microbit.utils.Utils;

import pl.droidsonroids.gif.GifImageView;

import static com.samsung.microbit.BuildConfig.DEBUG;

/**
 * Represents a home screen. Allows to navigate to all functionality
 * that the app provides.
 */
public class HomeActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = HomeActivity.class.getSimpleName();

    public static final String FIRST_RUN = "firstrun";

    // share stats checkbox
    private CheckBox mShareStatsCheckBox;

    SharedPreferences mPrefs = null;

    // Hello animation
    private GifImageView gifAnimationHelloEmoji;

    private DrawerLayout mDrawer;

    /* Debug code*/
    private String urlToOpen;
    /* Debug code ends*/

    private String emailBodyString;

    /**
     * Provides simplified way to log informational messages.
     *
     * @param message Message to log.
     */
    private void logi(String message) {
        if(DEBUG) {
            Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        //handle orientation change to prevent re-creation of activity.
        super.onConfigurationChanged(newConfig);

        unbindDrawables();

        setContentView(R.layout.activity_home);
        setupDrawer();
        setupButtonsFontStyle();
        initGifImage();
    }

    /**
     * Initializes the gif image and sets a resource.
     */
    private void initGifImage() {
        gifAnimationHelloEmoji = (GifImageView) findViewById(R.id.homeHelloAnimationGifView);
        gifAnimationHelloEmoji.setImageResource(R.drawable.hello_emoji_animation);
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logi("onCreate() :: ");

        setContentView(R.layout.activity_home);

        if(savedInstanceState == null) {
            startService(new Intent(this, IPCService.class));
        }

        setupDrawer();
        setupButtonsFontStyle();

        checkMinimumPermissionsForThisScreen();

        GoogleAnalyticsManager.getInstance().sendViewEventStats(HomeActivity.class.getSimpleName());

        /* Debug code*/
        MenuItem item = (MenuItem) findViewById(R.id.live);
        if(item != null) {
            item.setChecked(true);
        }

        initGifImage();
    }

    /**
     * Sets buttons font style by setting an appropriate typeface.
     */
    private void setupButtonsFontStyle() {
        Typeface typeface = MBApp.getApp().getTypeface();

        Button connectButton = (Button) findViewById(R.id.connect_device_btn);
        connectButton.setTypeface(typeface);
        Button flashButton = (Button) findViewById(R.id.flash_microbit_btn);
        flashButton.setTypeface(typeface);
        Button createCodeButton = (Button) findViewById(R.id.create_code_btn);
        createCodeButton.setTypeface(typeface);
        Button discoverButton = (Button) findViewById(R.id.discover_btn);
        discoverButton.setTypeface(typeface);
    }

    /**
     * Creates and setups side navigation menu.
     */
    private void setupDrawer() {

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationContentDescription(R.string.content_description_toolbar_home);
        ImageView imgToolbarLogo = (ImageView) findViewById(R.id.img_toolbar_logo);
        imgToolbarLogo.setContentDescription("Micro:bit");
        setSupportActionBar(toolbar);

        final boolean previousDrawerState = mDrawer != null && mDrawer.isDrawerOpen(GravityCompat.START);

        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawer.setDrawerTitle(GravityCompat.START, "Menu"); // TODO - Accessibility for touching the drawer

        if(previousDrawerState) {
            mDrawer.openDrawer(GravityCompat.START);
        }

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        boolean shareStats = false;
        mPrefs = getSharedPreferences("com.samsung.microbit", MODE_PRIVATE);
        if(mPrefs != null) {
            shareStats = mPrefs.getBoolean(getString(R.string.prefs_share_stats_status), true);
            GoogleAnalyticsManager.getInstance().setShareStatistic(shareStats);
        }
        //TODO focusable view
        mDrawer.setDrawerListener(toggle);

        toggle.syncState();

        /* Todo [Hack]:
        * NavigationView items for selection by user using
        * onClick listener instead of overriding onNavigationItemSelected*/
        Button menuNavBtn = (Button) findViewById(R.id.btn_nav_menu);
        menuNavBtn.setTypeface(MBApp.getApp().getTypeface());
        findViewById(R.id.btn_nav_menu).setOnClickListener(this);

        Button aboutNavBtn = (Button) findViewById(R.id.btn_about);
        aboutNavBtn.setTypeface(MBApp.getApp().getTypeface());
        findViewById(R.id.btn_about).setOnClickListener(this);

        Button helpNavBtn = (Button) findViewById(R.id.btn_help);
        helpNavBtn.setTypeface(MBApp.getApp().getTypeface());
        findViewById(R.id.btn_help).setOnClickListener(this);

        Button privacyNavBtn = (Button) findViewById(R.id.btn_privacy_cookies);
        privacyNavBtn.setTypeface(MBApp.getApp().getTypeface());
        findViewById(R.id.btn_privacy_cookies).setOnClickListener(this);

        Button termsNavBtn = (Button) findViewById(R.id.btn_terms_conditions);
        termsNavBtn.setTypeface(MBApp.getApp().getTypeface());
        findViewById(R.id.btn_terms_conditions).setOnClickListener(this);

        Button sendFeedbackNavBtn = (Button) findViewById(R.id.btn_send_feedback);
        sendFeedbackNavBtn.setTypeface(MBApp.getApp().getTypeface());
        findViewById(R.id.btn_send_feedback).setOnClickListener(this);

        // Share stats checkbox
        TextView shareStatsCheckTitle = (TextView) findViewById(R.id.share_statistics_title);
        shareStatsCheckTitle.setTypeface(MBApp.getApp().getTypeface());
        TextView shareStatsDescription = (TextView) findViewById(R.id.share_statistics_description);
        shareStatsDescription.setTypeface(MBApp.getApp().getRobotoTypeface());
        mShareStatsCheckBox = (CheckBox) findViewById(R.id.share_statistics_status);
        mShareStatsCheckBox.setOnClickListener(this);
        mShareStatsCheckBox.setChecked(shareStats);
    }

    /**
     * Creates email body to send statistics. Adds information about a device.
     *
     * @return Email body with device information.
     */
    private String prepareEmailBody() {
        if(emailBodyString != null) {
            return emailBodyString;
        }
        String emailBody = getString(R.string.email_body);
        String version = "0.1.0";
        try {
            version = MBApp.getApp().getPackageManager()
                    .getPackageInfo(MBApp.getApp().getPackageName(), 0).versionName;
        } catch(PackageManager.NameNotFoundException e) {
            Log.e(TAG, e.toString());
        }
        emailBodyString = String.format(emailBody,
                version,
                Build.MODEL,
                Build.VERSION.RELEASE,
                getString(R.string.privacy_policy_url));
        return emailBodyString;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if(drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindDrawables();
    }

    private void unbindDrawables() {
        Utils.unbindDrawables(gifAnimationHelloEmoji);
        Utils.unbindDrawables(findViewById(R.id.connect_device_btn));
        Utils.unbindDrawables(findViewById(R.id.flash_microbit_btn));
        Utils.unbindDrawables(findViewById(R.id.create_code_btn));
        Utils.unbindDrawables(findViewById(R.id.discover_btn));

        Utils.unbindDrawables(findViewById(R.id.img_toolbar_logo));
        Utils.unbindDrawables(findViewById(R.id.toolbar));
        Utils.unbindDrawables(findViewById(R.id.nav_view));
        Utils.unbindDrawables(findViewById(R.id.drawer_layout));
        Utils.unbindDrawables(findViewById(R.id.btn_nav_menu));
        Utils.unbindDrawables(findViewById(R.id.btn_about));
        Utils.unbindDrawables(findViewById(R.id.btn_help));
        Utils.unbindDrawables(findViewById(R.id.btn_privacy_cookies));
        Utils.unbindDrawables(findViewById(R.id.btn_terms_conditions));
        Utils.unbindDrawables(findViewById(R.id.btn_send_feedback));
        Utils.unbindDrawables(findViewById(R.id.share_statistics_title));
        Utils.unbindDrawables(findViewById(R.id.share_statistics_description));
        Utils.unbindDrawables(findViewById(R.id.share_statistics_status));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        urlToOpen = getString(R.string.create_code_url);
        switch(id) {
            case R.id.live:
                item.setChecked(true);
                break;
            case R.id.stage:
                item.setChecked(true);
                urlToOpen = urlToOpen.replace("www", "stage");
                break;
            case R.id.test:
                item.setChecked(true);
                urlToOpen = urlToOpen.replace("www", "test");
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause animation
        gifAnimationHelloEmoji.setFreezesAnimation(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /* function may be needed */
    }

    @Override
    public void onClick(final View v) {
        if(DEBUG) logi("onBtnClicked() :: ");

        // Drawer closes only after certain items are selected from the Navigation View
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        switch(v.getId()) {
//            case R.id.addDevice:
            case R.id.connect_device_btn: {
                Intent intent = new Intent(this, PairingActivity.class);
                startActivity(intent);
            }
            break;
            case R.id.create_code_btn: {
                //Update Stats
                GoogleAnalyticsManager.getInstance()
                        .sendNavigationStats(HomeActivity.class.getSimpleName(), "create-code");
                if(urlToOpen == null) {
                    urlToOpen = getString(R.string.create_code_url);
                }

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(urlToOpen));

                startActivity(intent);
            }
            break;
            case R.id.flash_microbit_btn:
                GoogleAnalyticsManager.getInstance()
                        .sendNavigationStats(HomeActivity.class.getSimpleName(), "flash");
                Intent i = new Intent(this, ProjectActivity.class);
                startActivity(i);
                break;
            case R.id.discover_btn:
                GoogleAnalyticsManager.getInstance()
                        .sendNavigationStats(HomeActivity.class.getSimpleName(), "discover");
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(getString(R.string.discover_url)));
                startActivity(intent);
                break;

            // TODO: HACK - Navigation View items from drawer here instead of [onNavigationItemSelected]
            // NavigationView items
            case R.id.btn_nav_menu: {
                // Close drawer
                drawer.closeDrawer(GravityCompat.START);
            }
            break;
            case R.id.btn_about: {
                Intent aboutIntent = new Intent(Intent.ACTION_VIEW);
                aboutIntent.setData(Uri.parse(getString(R.string.about_url)));
                startActivity(aboutIntent);
                // Close drawer
                drawer.closeDrawer(GravityCompat.START);
            }
            break;
            case R.id.btn_help: {
                GoogleAnalyticsManager.getInstance()
                        .sendNavigationStats(HomeActivity.class.getSimpleName() + ", overflow-menu", "help");
                Intent launchHelpIntent = new Intent(this, HelpWebView.class);
                launchHelpIntent.putExtra("url", "file:///android_asset/help.html");
                startActivity(launchHelpIntent);
                // Close drawer
                drawer.closeDrawer(GravityCompat.START);
            }
            break;
            case R.id.btn_privacy_cookies: {
                GoogleAnalyticsManager.getInstance()
                        .sendNavigationStats(HomeActivity.class.getSimpleName() + ", overflow-menu", "privacy-policy");

                Intent privacyIntent = new Intent(Intent.ACTION_VIEW);
                privacyIntent.setData(Uri.parse(getString(R.string.privacy_policy_url)));
                startActivity(privacyIntent);
                // Close drawer
                drawer.closeDrawer(GravityCompat.START);
            }
            break;
            case R.id.btn_terms_conditions: {
                GoogleAnalyticsManager.getInstance()
                        .sendNavigationStats(HomeActivity.class.getSimpleName() + ", overflow-menu", "ts-and-cs");

                Intent termsIntent = new Intent(Intent.ACTION_VIEW);
                termsIntent.setData(Uri.parse(getString(R.string.terms_of_use_url)));
                startActivity(termsIntent);
                // Close drawer
                drawer.closeDrawer(GravityCompat.START);
            }
            break;

            case R.id.btn_send_feedback: {
                Intent feedbackIntent = new Intent(Intent.ACTION_SEND);
                feedbackIntent.setType("message/rfc822");
                feedbackIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.feedback_email_address)});
                feedbackIntent.putExtra(Intent.EXTRA_SUBJECT, "[User feedback] ");
                //Prepare the body of email
                String body = prepareEmailBody();
                feedbackIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(body));
                Intent mailer = Intent.createChooser(feedbackIntent, null);
                startActivity(mailer);
                // Close drawer
                if(drawer != null) {
                    drawer.closeDrawer(GravityCompat.START);
                }
            }
            break;
            case R.id.share_statistics_status: {
                toggleShareStatistics();
            }
            break;

            default:
                break;

        }//Switch Ends
    }

    /**
     * Allows to turn on/off sharing statistics ability.
     */
    private void toggleShareStatistics() {
        if(mShareStatsCheckBox == null) {
            return;
        }
        boolean shareStatistics = mShareStatsCheckBox.isChecked();

        if(shareStatistics) {
            GoogleAnalytics.getInstance(this).reportActivityStart(this);
        } else {
            GoogleAnalytics.getInstance(this).reportActivityStop(this);
        }

        mPrefs.edit().putBoolean(getString(R.string.prefs_share_stats_status), shareStatistics).apply();
        logi("shareStatistics = " + shareStatistics);
        GoogleAnalyticsManager.getInstance().setShareStatistic(shareStatistics);
        GoogleAnalyticsManager.getInstance().
                sendStatSharing(HomeActivity.class.getSimpleName(), shareStatistics);
    }

    /**
     * Loads standard samples provided by Samsung. The samples can be used to
     * flash on a micro:bit board.
     */
    private void installSamples() {
        if(mPrefs.getBoolean(FIRST_RUN, true)) {
            mPrefs.edit().putBoolean(FIRST_RUN, false).apply();
            //First Run. Install the Sample applications
            new Thread(new Runnable() {
                @Override
                public void run() {
                    PopUp.show(getString(R.string.samples_are_about_to_be_copied),
                            "Thank you",
                            R.drawable.message_face, R.drawable.blue_btn,
                            PopUp.GIFF_ANIMATION_NONE,
                            PopUp.TYPE_ALERT,
                            null, null);
                    FileUtils.installSamples();

                }
            }).start();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch(requestCode) {
            case PermissionCodes.APP_STORAGE_PERMISSIONS_REQUESTED: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    installSamples();
                } else {
                    if(mPrefs != null) mPrefs.edit().putBoolean(FIRST_RUN, false).apply();
                    PopUp.show(getString(R.string.storage_permission_for_samples_error),
                            "",
                            R.drawable.error_face, R.drawable.red_btn,
                            PopUp.GIFF_ANIMATION_ERROR,
                            PopUp.TYPE_ALERT,
                            null, null);
                }
            }
            break;

        }
    }

    private void requestPermission(String[] permissions, final int requestCode) {
        ActivityCompat.requestPermissions(this, permissions, requestCode);
    }

    /**
     * Requests required external storage permissions.
     */
    View.OnClickListener diskStoragePermissionOKHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            logi("diskStoragePermissionOKHandler");
            PopUp.hide();
            String[] permissionsNeeded = {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
            requestPermission(permissionsNeeded, PermissionCodes.APP_STORAGE_PERMISSIONS_REQUESTED);
        }
    };

    /**
     * Provides action if a user canceled storage permission granting.
     */
    View.OnClickListener diskStoragePermissionCancelHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            logi("diskStoragePermissionCancelHandler");
            PopUp.hide();
            PopUp.show(getString(R.string.storage_permission_for_samples_error),
                    "",
                    R.drawable.error_face, R.drawable.red_btn,
                    PopUp.GIFF_ANIMATION_ERROR,
                    PopUp.TYPE_ALERT,
                    null, null);
            if(mPrefs != null) mPrefs.edit().putBoolean(FIRST_RUN, false).apply();
        }
    };

    /**
     * Checks and requests for external storage permissions
     * if the app is started at the first time.
     */
    private void checkMinimumPermissionsForThisScreen() {
        //Check reading permissions & writing permission to populate the HEX files & show program list
        if(mPrefs.getBoolean(FIRST_RUN, true)) {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PermissionChecker.PERMISSION_GRANTED ||
                    (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PermissionChecker.PERMISSION_GRANTED)) {
                PopUp.show(getString(R.string.storage_permission_for_samples),
                        getString(R.string.permissions_needed_title),
                        R.drawable.message_face, R.drawable.blue_btn, PopUp.GIFF_ANIMATION_NONE,
                        PopUp.TYPE_CHOICE,
                        diskStoragePermissionOKHandler,
                        diskStoragePermissionCancelHandler);
            } else {
                if(mPrefs.getBoolean(FIRST_RUN, true)) {
                    mPrefs.edit().putBoolean(FIRST_RUN, false).apply();
                    //First Run. Install the Sample applications
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            FileUtils.installSamples();
                        }
                    }).start();
                }
            }
        }
    }

    @Override
    public void onResume() {
        if(DEBUG) logi("onResume() :: ");
        super.onResume();
        if(gifAnimationHelloEmoji != null) {
            gifAnimationHelloEmoji.animate();
        }
    }
}

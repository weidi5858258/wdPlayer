package com.weidi.media.wdplayer.video_player;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.media.wdplayer.MainActivity;
import com.weidi.media.wdplayer.R;
import com.weidi.utils.PermissionsUtils;

import static com.weidi.media.wdplayer.MainActivity.PLAYERSERVICE;

/***

 */
public class JniPlayerActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG)
            Log.d(TAG, "onCreate()"
                    + " savedInstanceState: " + savedInstanceState);
        internalCreate();
    }

    @Override
    public void onRestart() {
        super.onRestart();
        if (DEBUG)
            Log.d(TAG, "onRestart()");
    }

    @Override
    public void onStart() {
        super.onStart();
        if (DEBUG)
            Log.d(TAG, "onStart()");
        internalStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG)
            Log.d(TAG, "onResume()");
        internalResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (DEBUG)
            Log.d(TAG, "onPause()");
        internalPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (DEBUG)
            Log.d(TAG, "onStop()");
        internalStop();
    }

    @Override
    public void onDestroy() {
        if (DEBUG)
            Log.d(TAG, "onDestroy()");
        internalDestroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (DEBUG)
            Log.d(TAG, "onActivityResult()" +
                    " requestCode: " + requestCode +
                    " resultCode: " + resultCode +
                    " data: " + data);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (DEBUG)
            Log.d(TAG, "onSaveInstanceState()" +
                    " outState: " + outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (DEBUG)
            Log.d(TAG, "onRestoreInstanceState()" +
                    " savedInstanceState: " + savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (DEBUG)
            Log.d(TAG, "onConfigurationChanged()" +
                    " newConfig: " + newConfig.toString());

        if (noFinish) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // 横屏的时候隐藏刘海屏的刘海部分
                if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    WindowManager.LayoutParams lp = getWindow().getAttributes();
                    lp.layoutInDisplayCutoutMode =
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
                    getWindow().setAttributes(lp);
                } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    // 竖屏的时候展示刘海屏的刘海部分
                    WindowManager.LayoutParams lp = getWindow().getAttributes();
                    lp.layoutInDisplayCutoutMode =
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                    getWindow().setAttributes(lp);
                }
            }

            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                EventBusUtils.post(
                        PlayerService.class,
                        PlayerService.COMMAND_HANDLE_LANDSCAPE_SCREEN,
                        new Object[]{0});
            } else {
                EventBusUtils.post(
                        PlayerService.class,
                        PlayerService.COMMAND_HANDLE_PORTRAIT_SCREEN,
                        null);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        PermissionsUtils.onRequestPermissionsResult(
                this,
                permissions,
                grantResults);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (DEBUG)
            Log.d(TAG, "onBackPressed()");
        finish();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (DEBUG)
            Log.d(TAG, "onWindowFocusChanged()" +
                    " hasFocus: " + hasFocus);

        /*if (hasFocus) {
            if (getResources().getConfiguration().orientation ==
                    Configuration.ORIENTATION_LANDSCAPE) {
                // 需要无状态栏的横屏
                EventBusUtils.post(
                        PlayerService.class,
                        PlayerService.COMMAND_HANDLE_LANDSCAPE_SCREEN,
                        new Object[]{0});
            } else {
                EventBusUtils.post(
                        PlayerService.class,
                        PlayerService.COMMAND_HANDLE_PORTRAIT_SCREEN,
                        null);
            }
        }*/
    }

    ///////////////////////////////////////////////////////////////////////

    private static final String TAG = "player_alexander";
    private static final boolean DEBUG = true;

    public static final String CONTENT_PATH = "content_path";
    public static final String COMMAND_NO_FINISH = "command_no_finish";

    public static boolean isAliveJniPlayerActivity = false;

    private String mPath;
    private String mType;
    private boolean noFinish;
    private OrientationListener myOrientationListener;

    private static final String[] REQUIRED_PERMISSIONS = {
            android.Manifest.permission.READ_PHONE_STATE,

            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static boolean isRunService(Context context, String serviceName) {
        ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo
                service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceName.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void internalCreate() {
        // 还没有测试
        /*myOrientationListener = new OrientationListener(this);
        boolean autoRotateOn = android.provider.Settings.System.getInt(getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, 0) == 1;
        // 检查系统是否开启自动旋转
        if (autoRotateOn) {
            myOrientationListener.enable();
        }*/

        final Intent intent = getIntent();
        // 为flase时表示从外部打开一个视频进行播放.为true时只是使用Activity的全屏特性(在本应用打开).
        noFinish = intent.getBooleanExtra(COMMAND_NO_FINISH, false);

        if (noFinish) {
            isAliveJniPlayerActivity = true;
            if (getResources().getConfiguration().orientation ==
                    Configuration.ORIENTATION_LANDSCAPE) {
                getWindow().setFlags(
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
                setFullscreen(false, false);
            }
        }
        setContentView(R.layout.transparent_layout);

        if (noFinish) {
            return;
        }

        // 在本应用开启当前Activity时能得到这个路径,从其他应用打开时为null
        mPath = intent.getStringExtra(CONTENT_PATH);
        if (TextUtils.isEmpty(mPath)) {
            Log.d(TAG, "internalCreate()  mPath: null");
            /***
             1.
             uri : content://media/external/video/media/272775
             path: /external/video/media/272775
             2.
             uri : content://com.huawei.hidisk.fileprovider
             /root/storage/1532-48AD/Videos/download/25068919/1/32/audio.m4s
             // 这个路径是不对的
             path: /root/storage/1532-48AD/Videos/download/25068919/1/32/audio.m4s
             */
            Uri uri = intent.getData();
            if (uri != null) {
                // content://com.huawei.hidisk.fileprovider/root/storage/1532-48AD/Android/data/
                // tv.danmaku.bili/download/92647556/1/64/video.m4s
                Log.d(TAG, "internalCreate()    uri: " + uri.toString());
                mPath = uri.getPath();
                if (!mPath.substring(mPath.lastIndexOf("/")).contains(".")) {
                    // 如: /external/video/media/272775
                    String[] proj = {MediaStore.Images.Media.DATA};
                    Cursor actualimagecursor = this.managedQuery(
                            uri, proj, null, null, null);
                    int actual_image_column_index =
                            actualimagecursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    actualimagecursor.moveToFirst();
                    mPath = actualimagecursor.getString(actual_image_column_index);
                }
                Log.d(TAG, "internalCreate() mPath1: " + mPath);
                if (mPath.startsWith("/root/")) {
                    // /root/storage/1532-48AD/Android/data/
                    // tv.danmaku.bili/download/92647556/1/64/video.m4s
                    mPath = mPath.substring(5);
                } else if (mPath.startsWith("/document/")) {
                    // /document/37C8-3904:myfiles/video/[2K]Clarity_Demo_2016.mp4
                    mPath = mPath.replace("/document/", "/storage/");
                    mPath = mPath.replace(":", "/");
                }
                // /storage/37C8-3904/myfiles/video/
                Log.d(TAG, "internalCreate() mPath2: " + mPath);
            }
            if (TextUtils.isEmpty(mPath)) {
                finish();
                return;
            }
            // video/mp4
            // audio/mpeg audio/quicktime(flac) audio/x-ms-wma(wma) audio/x-wav(wav)
            // audio/amr(amr) audio/mp3
            mType = intent.getType();
            Log.d(TAG, "internalCreate()   type: " + mType);
            if (TextUtils.isEmpty(mType)
                    || (!mType.startsWith("video/")
                    && !mType.startsWith("audio/"))) {
                finish();
                return;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (mPath.startsWith("/storage/")) {
                // 申请存储权限
                PermissionsUtils.checkAndRequestPermission(
                        new PermissionsUtils.IRequestPermissionsResult() {
                            @Override
                            public Activity getRequiredActivity() {
                                return JniPlayerActivity.this;
                            }

                            @Override
                            public String[] getRequiredPermissions() {
                                return REQUIRED_PERMISSIONS;
                            }

                            @Override
                            public void onRequestPermissionsResult() {
                                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                                        == PackageManager.PERMISSION_GRANTED) {
                                    doSomething(intent);
                                }
                                finish();
                            }
                        });
            }

            return;
        }

        doSomething(intent);

        finish();
    }

    private void internalStart() {

    }

    private void internalResume() {

    }

    private void internalPause() {

    }

    private void internalStop() {

    }

    private void internalDestroy() {
        if (noFinish) {
            isAliveJniPlayerActivity = false;
            if (getResources().getConfiguration().orientation ==
                    Configuration.ORIENTATION_LANDSCAPE) {
                // 需要有状态栏的横屏
                EventBusUtils.post(
                        PlayerService.class,
                        PlayerService.COMMAND_HANDLE_LANDSCAPE_SCREEN,
                        new Object[]{1});
            } else {
                EventBusUtils.post(
                        PlayerService.class,
                        PlayerService.COMMAND_HANDLE_PORTRAIT_SCREEN,
                        null);
            }
            setFullscreen(true, true);
        }

        if (myOrientationListener != null) {
            myOrientationListener.disable();
        }
    }

    private void doSomething(Intent intent) {
        if (!isRunService(this, PLAYERSERVICE)) {
            Log.d(TAG, "internalCreate() PlayerService is not alive");
            intent = new Intent();
            intent.setClass(this, PlayerService.class);
            intent.setAction(PlayerService.COMMAND_ACTION);
            intent.putExtra(PlayerService.COMMAND_PATH, mPath);
            intent.putExtra(PlayerService.COMMAND_TYPE, mType);
            intent.putExtra(PlayerService.COMMAND_NAME, PlayerService.COMMAND_SHOW_WINDOW);
            startService(intent);
        } else {
            Log.d(TAG, "internalCreate() PlayerService is alive");
            EventBusUtils.post(
                    PlayerService.class,
                    PlayerService.COMMAND_SHOW_WINDOW,
                    new Object[]{mPath, intent.getType()});
        }
    }

    private void setFullscreen(boolean isShowStatusBar, boolean isShowNavigationBar) {
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        if (!isShowStatusBar) {
            uiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        }
        if (!isShowNavigationBar) {
            uiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }
        getWindow().getDecorView().setSystemUiVisibility(uiOptions);
    }

    private class OrientationListener extends OrientationEventListener {

        public OrientationListener(Context context) {
            super(context);
        }

        public OrientationListener(Context context, int rate) {
            super(context, rate);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            //Log.d(TAG, "orention" + orientation);
            int screenOrientation = getResources().getConfiguration().orientation;
            if (((orientation >= 0) && (orientation < 45))
                    || (orientation > 315)) {
                // 设置竖屏
                if (screenOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        && orientation != ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
                    Log.d(TAG, "设置竖屏");
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            } else if (orientation > 225 && orientation < 315) {
                // 设置横屏
                Log.d(TAG, "设置横屏");
                if (screenOrientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
            } else if (orientation > 45 && orientation < 135) {
                // 设置反向横屏
                Log.d(TAG, "反向横屏");
                if (screenOrientation != ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                }
            } else if (orientation > 135 && orientation < 225) {
                Log.d(TAG, "反向竖屏");
                if (screenOrientation != ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                }
            }
        }
    }

}
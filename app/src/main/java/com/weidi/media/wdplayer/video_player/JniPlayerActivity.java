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
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;

import com.weidi.eventbus.Phone;
import com.weidi.media.wdplayer.R;
import com.weidi.utils.PermissionsUtils;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (DEBUG)
            Log.d(TAG, "onNewIntent() intent: " + intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (DEBUG)
            Log.d(TAG, "onActivityResult()" +
                    " requestCode: " + requestCode +
                    " resultCode: " + resultCode +
                    " data: " + data);
        if (requestCode == REQUEST_CODE
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                doSomething();
                finish();
            } else {
                Log.e(TAG, "onActivityResult() 没有得到外部读写的存储权限");
            }
        }
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
                Phone.call(
                        PlayerService.class.getName(),
                        PlayerService.COMMAND_HANDLE_LANDSCAPE_SCREEN,
                        new Object[]{0});
            } else {
                Phone.call(
                        PlayerService.class.getName(),
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
                        PlayerService.class.getName(),
                        PlayerService.COMMAND_HANDLE_LANDSCAPE_SCREEN,
                        new Object[]{0});
            } else {
                EventBusUtils.post(
                        PlayerService.class.getName(),
                        PlayerService.COMMAND_HANDLE_PORTRAIT_SCREEN,
                        null);
            }
        }*/
    }

    ///////////////////////////////////////////////////////////////////////

    private static final String TAG = "JniPlayerActivity";
    private static final boolean DEBUG = true;

    public static final String CONTENT_PATH = "content_path";
    public static final String CONTENT_TYPE = "content_type";
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
            if (TextUtils.equals(serviceName, service.service.getClassName())) {
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

        // 在本应用内开启当前Activity时能得到这个路径,从其他应用打开时为null
        mPath = intent.getStringExtra(CONTENT_PATH);
        mType = intent.getStringExtra(CONTENT_TYPE);
        Log.d(TAG, "internalCreate()  mPath: " + mPath);
        Log.d(TAG, "internalCreate()  mType: " + mType);
        if (TextUtils.isEmpty(mPath) && TextUtils.isEmpty(mType)) {
            /***
             1.
             uri : content://media/external/video/media/272775
             path = uri.getPath();
             path:                /external/video/media/272775
             path: /storage/emulated/0/Pictures/WeiXin/wx_camera_1610676456161.mp4
             2.
             uri : content://com.huawei.hidisk.fileprovider/root/storage/1532-48AD/.../audio.m4s
             path = uri.getPath();
             path:                                         /root/storage/1532-48AD/.../audio.m4s
             path:                                              /storage/1532-48AD/.../audio.m4s
             3.
             uri : content://com.mobi.shtp.provider/external/Pictures/WeiXin/***.mp4
             path = uri.getPath();
             path:                                 /external/Pictures/WeiXin/***.mp4
             path:                       /storage/emulated/0/Pictures/WeiXin/***.mp4
             4.
             uri : file:///storage/1532-48AD/Movies/Movies/AQUAMAN_Trailer_3840_2160_4K.webm
             path = uri.getPath();
             path:        /storage/1532-48AD/Movies/Movies/AQUAMAN_Trailer_3840_2160_4K.webm
             5.
             uri : content://com.speedsoftware.rootexplorer.fileprovider/external_storage_root/Movies/jlbhd5.mp4
             path = uri.getPath();
             path:        /storage/emulated/0/Movies/jlbhd5.mp4

             // 加密文件
             content://cn.oneplus.filemanager.Safebox/file/2
             content://com.huawei.hidisk.fileprovider
             /root/storage/1532-48AD/.File_SafeBox/.../temp/***.mp4
             */
            Uri uri = intent.getData();
            if (uri != null) {
                mPath = uri.toString().trim();
                Log.d(TAG, "internalCreate()    uri: " + mPath);
                if (!mPath.toLowerCase().startsWith("http://")
                        && !mPath.toLowerCase().startsWith("https://")
                        && !mPath.toLowerCase().startsWith("rtmp://")) {
                    mPath = uri.getPath();
                    Log.d(TAG, "internalCreate() mPath1: " + mPath);
                    if (!mPath.substring(mPath.lastIndexOf("/")).contains(".")) {
                        try {
                            String[] proj = {MediaStore.Images.Media.DATA};
                            Cursor actualimagecursor =
                                    managedQuery(uri, proj, null, null, null);
                            // Caused by: java.lang.IllegalArgumentException:
                            // column '_data' does not exist. Available columns: []
                            int actual_image_column_index =
                                    actualimagecursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                            actualimagecursor.moveToFirst();
                            mPath = actualimagecursor.getString(actual_image_column_index);
                        } catch (Exception e) {
                            e.printStackTrace();
                            finish();
                            return;
                        }
                    }
                    Log.d(TAG, "internalCreate() mPath2: " + mPath);
                    if (mPath.startsWith("/root/")) {
                        // /root/storage/1532-48AD/download/***.mp4
                        // --->
                        // /storage/1532-48AD/download/***.mp4
                        mPath = mPath.substring(5);
                    } else if (mPath.startsWith("/document/")) {
                        if (mPath.startsWith("/document/primary:")) {
                            // /document/primary:Movies/Camera/少年包青天.mp4
                            // --->
                            // /storage/emulated/0/Movies/Camera/少年包青天.mp4
                            mPath = mPath.replace("/document/primary", "/storage/emulated/0");
                        } else {
                            // /document/37C8-3904:myfiles/video/***.mp4
                            // --->
                            // /storage/37C8-3904/myfiles/video/***.mp4
                            mPath = mPath.replace("/document/", "/storage/");
                        }
                        mPath = mPath.replace(":", "/");
                    } else if (mPath.startsWith("/external/")) {
                        // /external/Pictures/WeiXin/wx_camera_1610676456161.mp4
                        // --->
                        // /storage/emulated/0/Pictures/WeiXin/wx_camera_1610676456161.mp4
                        mPath = mPath.replace("/external/", "/storage/emulated/0/");
                    } else if (mPath.startsWith("/external_storage_root/")) {
                        // /external_storage_root/Movies/jlbhd5.mp4
                        // --->
                        // /storage/emulated/0/Movies/jlbhd5.mp4
                        mPath = mPath.replace("/external_storage_root/", "/storage/emulated/0/");
                    }
                    Log.d(TAG, "internalCreate() mPath3: " + mPath);
                }
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (mPath.startsWith("/storage/")) {
                // 先判断有没有权限
                if (Environment.isExternalStorageManager()) {
                } else {
                    Intent intent_ =
                            new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent_.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent_, REQUEST_CODE);
                    return;
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
                                    doSomething();
                                }
                                finish();
                            }
                        });
                return;
            }
        }

        doSomething();

        finish();
    }

    private void internalStart() {

    }

    private void internalResume() {
        if (PlayerWrapper.IS_TV && noFinish) {
            Phone.call(
                    PlayerService.class.getName(),
                    PlayerService.COMMAND_HANDLE_LANDSCAPE_SCREEN,
                    new Object[]{0});
        }
    }

    private void internalPause() {
        if (PlayerWrapper.IS_TV && noFinish) {
            Phone.call(
                    PlayerService.class.getName(),
                    PlayerService.COMMAND_HANDLE_LANDSCAPE_SCREEN,
                    new Object[]{2});
        }
    }

    private void internalStop() {

    }

    private void internalDestroy() {
        if (noFinish) {
            isAliveJniPlayerActivity = false;
            if (getResources().getConfiguration().orientation ==
                    Configuration.ORIENTATION_LANDSCAPE) {
                // 需要有状态栏的横屏
                Phone.call(
                        PlayerService.class.getName(),
                        PlayerService.COMMAND_HANDLE_LANDSCAPE_SCREEN,
                        new Object[]{2});
            } else {
                Phone.call(
                        PlayerService.class.getName(),
                        PlayerService.COMMAND_HANDLE_PORTRAIT_SCREEN,
                        null);
            }
            setFullscreen(true, true);
        }

        if (myOrientationListener != null) {
            myOrientationListener.disable();
        }
    }

    private void doSomething() {
        if (!isRunService(this, PLAYERSERVICE)) {
            Log.d(TAG, "internalCreate() PlayerService is not alive");
            Intent intent = new Intent();
            intent.setClass(this, PlayerService.class);
            intent.setAction(PlayerService.COMMAND_ACTION);
            intent.putExtra(PlayerService.COMMAND_PATH, mPath);
            intent.putExtra(PlayerService.COMMAND_TYPE, mType);
            intent.putExtra(PlayerService.COMMAND_NAME, PlayerService.COMMAND_SHOW_WINDOW);
            startService(intent);
        } else {
            Log.d(TAG, "internalCreate() PlayerService is alive");
            Phone.call(
                    PlayerService.class.getName(),
                    PlayerService.COMMAND_SHOW_WINDOW,
                    new Object[]{mPath, mType});
        }
    }

    private static final int REQUEST_CODE = 100;
    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 先判断有没有权限
            if (Environment.isExternalStorageManager()) {
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 先判断有没有权限
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE);
            }
        } else {
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
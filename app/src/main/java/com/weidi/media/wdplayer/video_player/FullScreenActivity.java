package com.weidi.media.wdplayer.video_player;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.weidi.eventbus.Phone;
import com.weidi.media.wdplayer.Constants;

import static com.weidi.media.wdplayer.Constants.BUTTON_CLICK_FF;
import static com.weidi.media.wdplayer.Constants.BUTTON_CLICK_FR;
import static com.weidi.media.wdplayer.Constants.BUTTON_CLICK_PAUSE;
import static com.weidi.media.wdplayer.Constants.BUTTON_CLICK_PLAY;

/***

 */
public class FullScreenActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // requestWindowFeature(Window.FEATURE_NO_TITLE);

        Window window = getWindow();
        window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // http://events.jianshu.io/p/91808e9f3b38
            WindowManager.LayoutParams params = window.getAttributes();
            params.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(params);
        }

        int flags = window.getDecorView().getSystemUiVisibility();
        // 有些机型上,不能达到全面屏
        int immersiveModeFlags = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
        // 可以达到全面屏
        // https://blog.csdn.net/qq_24642353/article/details/89179144
        immersiveModeFlags = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        flags = flags | immersiveModeFlags;
        window.getDecorView().setSystemUiVisibility(flags);

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

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
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
                    PlayerService.class.getName(),
                    PlayerService.COMMAND_HANDLE_LANDSCAPE_SCREEN,
                    new Object[]{0});
        } else {
            EventBusUtils.post(
                    PlayerService.class.getName(),
                    PlayerService.COMMAND_HANDLE_PORTRAIT_SCREEN,
                    null);
        }*/
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

        if (hasFocus) {
            hideBottomUIMenu();
            /*getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);*/
        }

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

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    Phone.call(
                            PlayerWrapper.class.getName(),
                            BUTTON_CLICK_FR,
                            null);
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    Phone.call(
                            PlayerWrapper.class.getName(),
                            BUTTON_CLICK_FF,
                            null);
                    break;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    // 暂停/播放
                    if (Boolean.parseBoolean(
                            FFMPEG.getDefault().onTransact(
                                    FFMPEG.DO_SOMETHING_CODE_isRunning, null))) {
                        if (Boolean.parseBoolean(
                                FFMPEG.getDefault().onTransact(
                                        FFMPEG.DO_SOMETHING_CODE_isPlaying, null))) {
                            Phone.call(
                                    PlayerWrapper.class.getName(),
                                    BUTTON_CLICK_PAUSE,
                                    null);
                        } else {
                            Phone.call(
                                    PlayerWrapper.class.getName(),
                                    BUTTON_CLICK_PLAY,
                                    null);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    ///////////////////////////////////////////////////////////////////////

    private static final String TAG = "FullScreenActivity";
    private static final boolean DEBUG = true;
    public static boolean SCREEN_ORIENTATION_LANDSCAPE = true;

    private void internalCreate() {
        JniPlayerActivity.isAliveJniPlayerActivity = true;
        Phone.register(this);


        if (PlayerWrapper.IS_TV) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            if (SCREEN_ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            }
        }
    }

    private void internalStart() {

    }

    private void internalResume() {
        Phone.removeUiMessages(1);
        Phone.callUiDelayed(FullScreenActivity.class.getName(), 1, 1000, null);
    }

    private void internalPause() {
        Phone.removeUiMessages(2);
        Phone.callUiDelayed(FullScreenActivity.class.getName(), 2, 1000, null);
    }

    private void internalStop() {

    }

    private void internalDestroy() {
        JniPlayerActivity.isAliveJniPlayerActivity = false;
        Phone.removeUiMessages(1);
        Phone.removeUiMessages(2);
        Phone.removeUiMessages(3);
        Phone.call(FullScreenActivity.class.getName(), 3, null);
        Phone.unregister(this);
    }

    private void hideBottomUIMenu() {
        // 隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    private Object onEvent(int what, Object[] objArray) {
        Object result = null;
        switch (what) {
            case 1: {
                Phone.call(
                        PlayerService.class.getName(),
                        PlayerService.COMMAND_HANDLE_LANDSCAPE_SCREEN,
                        new Object[]{0});
                break;
            }
            case 2: {
                Phone.call(
                        PlayerService.class.getName(),
                        PlayerService.COMMAND_HANDLE_LANDSCAPE_SCREEN,
                        new Object[]{2});
                break;
            }
            case 3: {
                if (PlayerWrapper.IS_PHONE) {
                    int orientation = getResources().getConfiguration().orientation;
                    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                        Phone.call(
                                PlayerService.class.getName(),
                                PlayerService.COMMAND_HANDLE_PORTRAIT_SCREEN,
                                new Object[]{1000});
                    } else {
                        Phone.call(
                                PlayerService.class.getName(),
                                PlayerService.COMMAND_HANDLE_LANDSCAPE_SCREEN,
                                new Object[]{2});
                    }
                } else {
                    Phone.call(
                            PlayerService.class.getName(),
                            PlayerService.COMMAND_HANDLE_LANDSCAPE_SCREEN,
                            new Object[]{2});
                }
                break;
            }
            case 4: {
                SCREEN_ORIENTATION_LANDSCAPE = !SCREEN_ORIENTATION_LANDSCAPE;
                if (SCREEN_ORIENTATION_LANDSCAPE) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                }
                break;
            }
            case Constants.FINISH_FULL_SCREEN_ACTIVITY: {
                finish();
                break;
            }
            default:
                break;
        }
        return result;
    }

}
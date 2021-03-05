package com.weidi.media.wdplayer.video_player;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager;

import com.weidi.eventbus.EventBusUtils;

import androidx.annotation.NonNull;

/***

 */
public class FullScreenActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
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
                    PlayerService.class,
                    PlayerService.COMMAND_HANDLE_LANDSCAPE_SCREEN,
                    new Object[]{0});
        } else {
            EventBusUtils.post(
                    PlayerService.class,
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

    private static final String TAG = "FullScreenActivity";
    private static final boolean DEBUG = true;

    private void internalCreate() {

    }

    private void internalStart() {

    }

    private void internalResume() {
        JniPlayerActivity.isAliveJniPlayerActivity = true;
        mUiHandler.removeMessages(1);
        mUiHandler.sendEmptyMessageDelayed(1, 1000);
    }

    private void internalPause() {
        JniPlayerActivity.isAliveJniPlayerActivity = false;
        mUiHandler.removeMessages(2);
        mUiHandler.sendEmptyMessageDelayed(2, 1000);
    }

    private void internalStop() {

    }

    private void internalDestroy() {
        mUiHandler.removeMessages(2);
        mUiHandler.removeMessages(3);
        mUiHandler.sendEmptyMessage(3);
    }

    private Handler mUiHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            //super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    EventBusUtils.post(
                            PlayerService.class,
                            PlayerService.COMMAND_HANDLE_LANDSCAPE_SCREEN,
                            new Object[]{0});
                    break;
                case 2:
                    EventBusUtils.post(
                            PlayerService.class,
                            PlayerService.COMMAND_HANDLE_LANDSCAPE_SCREEN,
                            new Object[]{2});
                    break;
                case 3:
                    if (PlayerWrapper.IS_PHONE) {
                        EventBusUtils.post(
                                PlayerService.class,
                                PlayerService.COMMAND_HANDLE_PORTRAIT_SCREEN,
                                new Object[]{1000});
                    } else {
                        EventBusUtils.post(
                                PlayerService.class,
                                PlayerService.COMMAND_HANDLE_LANDSCAPE_SCREEN,
                                new Object[]{2});
                    }
                    break;
                default:
                    break;
            }
        }
    };

}
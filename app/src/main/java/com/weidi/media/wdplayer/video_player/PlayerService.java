package com.weidi.media.wdplayer.video_player;

import android.app.Service;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.media.wdplayer.MainActivity;
import com.weidi.media.wdplayer.R;
import com.weidi.media.wdplayer.WearMainActivity;

import static com.weidi.media.wdplayer.Constants.PLAYBACK_ADDRESS;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_MEDIA_TYPE;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_NORMAL_FINISH;
import static com.weidi.media.wdplayer.Constants.PREFERENCES_NAME;

/***
 Created by root on 19-8-5.
 */

public class PlayerService extends Service {

    /*private static final String TAG =
            FloatingService.class.getSimpleName();*/
    private static final String TAG = "player_alexander";
    private static final boolean DEBUG = true;


    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind() intent: " + intent);
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind() intent: " + intent);
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate()");
        internalCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand() intent: " + intent);
        internalStartCommand(intent, flags, startId);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        internalDestroy();
        super.onDestroy();
    }

    ////////////////////////////////////////////////////////////////////////

    private Handler mUiHandler;
    private PlayerWrapper mPlayerWrapper;
    private String mPath = null;

    private WindowManager mWindowManager;
    private View mView;

    private void internalCreate() {
        EventBusUtils.register(this);
        registerHeadsetPlugReceiver();
        registerTelephonyReceiver();

        initViewForService();

        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                PlayerService.this.uiHandleMessage(msg);
            }
        };
    }

    /***
     播放视频
     adb shell am startservice \
     -n com.weidi.usefragments/com.weidi.usefragments.business.video_player.PlayerService \
     -a com.weidi.usefragments.business.video_player.PlayerService \
     --ei HandlePlayerService 1 \
     --es HandlePlayerServicePath "video/" \
     --es HandlePlayerServicePath "http://wttv-lh.akamaihd
     .net:80/i/WTTVBreaking_1@333494/index_3000_av-b.m3u8"
     停止播放
     adb shell am startservice \
     -n com.weidi.usefragments/com.weidi.usefragments.business.video_player.PlayerService \
     -a com.weidi.usefragments.business.video_player.PlayerService \
     --ei HandlePlayerService 2
     停止服务
     adb shell am startservice \
     -n com.weidi.usefragments/com.weidi.usefragments.business.video_player.PlayerService \
     -a com.weidi.usefragments.business.video_player.PlayerService \
     --ei HandlePlayerService 3
     重新加载内容
     adb shell am startservice \
     -n com.weidi.usefragments/com.weidi.usefragments.business.video_player.PlayerService \
     -a com.weidi.usefragments.business.video_player.PlayerService \
     --ei HandlePlayerService 4
     */

    public static final String COMMAND_ACTION =
            "com.weidi.media.wdplayer.video_player.PlayerService";
    public static final String COMMAND_NAME = "HandlePlayerService";
    public static final String COMMAND_PATH = "HandlePlayerServicePath";
    public static final String COMMAND_TYPE = "HandlePlayerServiceType";
    public static final int COMMAND_SHOW_WINDOW = 1;
    public static final int COMMAND_HIDE_WINDOW = 2;
    public static final int COMMAND_STOP_SERVICE = 3;
    public static final int COMMAND_RESTART_LOAD_CONTENTS = 4;
    public static final int COMMAND_HANDLE_LANDSCAPE_SCREEN = 5;
    public static final int COMMAND_HANDLE_PORTRAIT_SCREEN = 6;
    public static final int COMMAND_TEST = 7;

    // 测试时使用
    private void internalStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            // app crash后的操作
            handleAppCrash();
            return;
        }

        String action = intent.getAction();
        Log.d(TAG, "internalStartCommand()   action: " + action);
        /*if (TextUtils.isEmpty(action)) {
            handleAppCrash();
            return;
        }*/

        if (!TextUtils.equals(COMMAND_ACTION, action)) {
            return;
        }

        int commandName = intent.getIntExtra(COMMAND_NAME, 0);
        switch (commandName) {
            case COMMAND_SHOW_WINDOW:
                Uri uri = intent.getData();
                if (uri != null) {
                    mPath = uri.getPath();
                } else {
                    mPath = intent.getStringExtra(COMMAND_PATH);
                }
                String type = intent.getStringExtra(COMMAND_TYPE);
                Log.d(TAG, "internalStartCommand()    mType: " + type);
                Log.d(TAG, "internalStartCommand() mCurPath: " + mPath);
                if (mPlayerWrapper != null) {
                    mPlayerWrapper.setType(type);
                }
                mUiHandler.removeMessages(COMMAND_SHOW_WINDOW);
                mUiHandler.sendEmptyMessage(COMMAND_SHOW_WINDOW);
                break;
            case COMMAND_HIDE_WINDOW:
                mUiHandler.removeMessages(COMMAND_HIDE_WINDOW);
                mUiHandler.sendEmptyMessage(COMMAND_HIDE_WINDOW);
                break;
            case COMMAND_STOP_SERVICE:
                mUiHandler.removeMessages(COMMAND_STOP_SERVICE);
                mUiHandler.sendEmptyMessage(COMMAND_STOP_SERVICE);
                break;
            case COMMAND_RESTART_LOAD_CONTENTS:
                if (mPlayerWrapper != null)
                    mPlayerWrapper.sendMessageForLoadContents();
                break;
            default:
                break;
        }
    }

    private void internalDestroy() {
        if (mPlayerWrapper != null)
            mPlayerWrapper.onDestroy();
        mWindowManager.removeView(mView);
        unRegisterHeadsetPlugReceiver();
        EventBusUtils.unregister(this);
    }

    private void handleAppCrash() {
        SharedPreferences sp = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        boolean isNormalFinish = sp.getBoolean(PLAYBACK_NORMAL_FINISH, true);
        mPath = sp.getString(PLAYBACK_ADDRESS, null);
        if (!isNormalFinish && !TextUtils.isEmpty(mPath)) {
            String type = sp.getString(PLAYBACK_MEDIA_TYPE, null);
            if (TextUtils.isEmpty(type)
                    || type.startsWith("video/")) {
                UiModeManager uiModeManager =
                        (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
                int whatIsDevice = uiModeManager.getCurrentModeType();
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (whatIsDevice != Configuration.UI_MODE_TYPE_WATCH) {
                    intent.setClass(getApplicationContext(), MainActivity.class);
                } else {
                    intent.setClass(getApplicationContext(), WearMainActivity.class);
                }
                startActivity(intent);

                if (mPlayerWrapper != null) {
                    mPlayerWrapper.setType(type);
                }
                mUiHandler.removeMessages(COMMAND_SHOW_WINDOW);
                mUiHandler.sendEmptyMessageDelayed(COMMAND_SHOW_WINDOW, 3000);
            }
        }
    }

    private Object onEvent(int what, Object[] objArray) {
        Object result = null;
        switch (what) {
            case COMMAND_SHOW_WINDOW:
                if (objArray != null && objArray.length >= 2) {
                    mPath = (String) objArray[0];
                    if (mPlayerWrapper != null) {
                        mPlayerWrapper.setType((String) objArray[1]);
                    }
                }
                mUiHandler.removeMessages(COMMAND_SHOW_WINDOW);
                mUiHandler.sendEmptyMessage(COMMAND_SHOW_WINDOW);
                break;
            case COMMAND_HIDE_WINDOW:
                mUiHandler.removeMessages(COMMAND_HIDE_WINDOW);
                mUiHandler.sendEmptyMessage(COMMAND_HIDE_WINDOW);
                break;
            case COMMAND_STOP_SERVICE:
                mUiHandler.removeMessages(COMMAND_STOP_SERVICE);
                mUiHandler.sendEmptyMessage(COMMAND_STOP_SERVICE);
                break;
            case COMMAND_HANDLE_LANDSCAPE_SCREEN:
                if (objArray != null && objArray.length > 0) {
                    int statusBarHeight = (Integer) objArray[0];
                    Message msg = mUiHandler.obtainMessage();
                    msg.what = COMMAND_HANDLE_LANDSCAPE_SCREEN;
                    msg.arg1 = statusBarHeight;
                    mUiHandler.removeMessages(COMMAND_HANDLE_LANDSCAPE_SCREEN);
                    mUiHandler.sendMessage(msg);
                }
                break;
            case COMMAND_HANDLE_PORTRAIT_SCREEN:
                mUiHandler.removeMessages(COMMAND_HANDLE_PORTRAIT_SCREEN);
                mUiHandler.sendEmptyMessage(COMMAND_HANDLE_PORTRAIT_SCREEN);
                break;
            case KeyEvent.KEYCODE_HEADSETHOOK:// 79
                if (mPlayerWrapper != null)
                    mPlayerWrapper.onEvent(KeyEvent.KEYCODE_HEADSETHOOK, null);
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:// 85
                if (mPlayerWrapper != null)
                    mPlayerWrapper.onEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, null);
                break;
            case KeyEvent.KEYCODE_MEDIA_STOP:// 86
                if (mPlayerWrapper != null)
                    mPlayerWrapper.onEvent(KeyEvent.KEYCODE_MEDIA_STOP, null);
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:// 88
                // 三击
                if (mPlayerWrapper != null)
                    mPlayerWrapper.onEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS, null);
                break;
            case KeyEvent.KEYCODE_MEDIA_NEXT:// 87
                // 双击
                if (mPlayerWrapper != null)
                    mPlayerWrapper.onEvent(KeyEvent.KEYCODE_MEDIA_NEXT, null);
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY:// 126
                // 单击
                if (mPlayerWrapper != null)
                    mPlayerWrapper.onEvent(KeyEvent.KEYCODE_MEDIA_PLAY, null);
                break;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:// 127
                // 单击
                if (mPlayerWrapper != null)
                    mPlayerWrapper.onEvent(KeyEvent.KEYCODE_MEDIA_PAUSE, null);
                break;
            default:
                break;
        }
        return result;
    }

    private void uiHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        switch (msg.what) {
            case COMMAND_SHOW_WINDOW:
                if (mPlayerWrapper != null) {
                    mPlayerWrapper.setDataSource(mPath);
                }
                break;
            case COMMAND_HIDE_WINDOW:
                if (mPlayerWrapper != null) {
                    mPlayerWrapper.removeView();
                }
                break;
            case COMMAND_STOP_SERVICE:
                if (mPlayerWrapper != null) {
                    mPlayerWrapper.removeView();
                }
                stopSelf();
                break;
            case COMMAND_HANDLE_LANDSCAPE_SCREEN:
                if (mPlayerWrapper != null) {
                    if (msg.arg1 == 0) {
                        mPlayerWrapper.handleLandscapeScreen(0);
                    } else {
                        mPlayerWrapper.handleLandscapeScreen(1);
                    }
                }
                break;
            case COMMAND_HANDLE_PORTRAIT_SCREEN:
                if (mPlayerWrapper != null) {
                    mPlayerWrapper.handlePortraitScreen();
                }
                break;
            default:
                break;
        }
    }

    private void initViewForService() {
        // 为了进程保活
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        LayoutInflater inflater =
                (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = inflater.inflate(R.layout.transparent_layout, null);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParams.gravity = Gravity.TOP + Gravity.LEFT;
        layoutParams.width = 1;
        layoutParams.height = 1;
        layoutParams.x = 0;
        layoutParams.y = 0;
        mWindowManager.addView(mView, layoutParams);

        if (mPlayerWrapper == null) {
            mPlayerWrapper = new PlayerWrapper();
        }
        mPlayerWrapper.setService(this);
    }

    /////////////////////////////////////////////////////////////////

    private TelephonyManager mTelephonyManager;

    private void registerTelephonyReceiver() {
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String phoneNumber) {
            super.onCallStateChanged(state, phoneNumber);
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:
                    // 挂断或启动监听时
                    Log.i(TAG, "onCallStateChanged() TelephonyManager.CALL_STATE_IDLE");
                    if (mPlayerWrapper != null)
                        mPlayerWrapper.playPlayerWithTelephonyCall();
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    // 来电
                    Log.i(TAG, "onCallStateChanged() TelephonyManager.CALL_STATE_RINGING");
                    if (mPlayerWrapper != null)
                        mPlayerWrapper.pausePlayerWithTelephonyCall();
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    // 接听
                    Log.i(TAG, "onCallStateChanged() TelephonyManager.CALL_STATE_OFFHOOK");
                    break;
                default:
                    break;
            }
        }
    };

    /////////////////////////////////////////////////////////////////

    /***
     耳机操作
     */

    // Android监听耳机的插拔事件(只能动态注册,经过测试可行)
    private HeadsetPlugReceiver mHeadsetPlugReceiver;
    private AudioManager mAudioManager;
    private ComponentName mMediaButtonReceiver;

    private void registerHeadsetPlugReceiver() {
        if (mHeadsetPlugReceiver == null) {
            mHeadsetPlugReceiver = new HeadsetPlugReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.HEADSET_PLUG");
            filter.setPriority(2147483647);
            registerReceiver(mHeadsetPlugReceiver, filter);
        }

        if (mMediaButtonReceiver == null) {
            mAudioManager =
                    (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            mMediaButtonReceiver = new ComponentName(
                    getPackageName(),
                    com.weidi.media.wdplayer.business.receiver.MediaButtonReceiver.class.getName());
            mAudioManager.registerMediaButtonEventReceiver(mMediaButtonReceiver);
        }
    }

    private void unRegisterHeadsetPlugReceiver() {
        if (mHeadsetPlugReceiver != null) {
            unregisterReceiver(mHeadsetPlugReceiver);
        }

        if (mMediaButtonReceiver != null) {
            mAudioManager.unregisterMediaButtonEventReceiver(mMediaButtonReceiver);
        }
    }

    private class HeadsetPlugReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            if (intent.hasExtra("state")) {
                switch (intent.getIntExtra("state", 0)) {
                    case 0:
                        if (DEBUG)
                            Log.d(TAG, "HeadsetPlugReceiver headset not connected");
                        break;
                    case 1:
                        if (DEBUG)
                            Log.d(TAG, "HeadsetPlugReceiver headset has connected");
                        break;
                    default:
                }
            }
        }
    }

}

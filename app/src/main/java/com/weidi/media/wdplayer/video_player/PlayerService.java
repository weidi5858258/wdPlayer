package com.weidi.media.wdplayer.video_player;

import android.app.Service;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.sonyericsson.dlna.dmr.player.IDmrPlayerApp;
import com.sonyericsson.dlna.dmr.player.IDmrPlayerAppCallback;
import com.weidi.eventbus.EventBusUtils;
import com.weidi.media.wdplayer.MainActivity;
import com.weidi.media.wdplayer.R;
import com.weidi.media.wdplayer.WearMainActivity;
import com.weidi.media.wdplayer.util.JniObject;
import com.weidi.utils.MyToast;

import java.util.Map;

import static com.weidi.media.wdplayer.Constants.NEED_TWO_PLAYER;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_ADDRESS;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_MEDIA_TYPE;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_NORMAL_FINISH;
import static com.weidi.media.wdplayer.Constants.PREFERENCES_NAME;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_setRemainingTime;

/***
 Created by root on 19-8-5.
 */

public class PlayerService extends Service {

    /*private static final String TAG =
            FloatingService.class.getSimpleName();*/
    private static final String TAG = "LocalPlayerService";
    private static final boolean DEBUG = true;

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind() intent: " + intent);
        // 被dlna绑定
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind() intent: " + intent);
        // 不解绑
        /*if (mRemoteVideoPlayer != null) {
            unbindService(mRemoteVideoConnection);
        }*/
        // 被dlna解绑
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
    private String mType = null;
    private String mPath = null;
    public static boolean mUseLocalPlayer = true;

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

     Intent intent = new Intent();
     // 从当前应用开启
     intent.setClass(this, PlayerService.class);
     // 从其他应用开启
     intent.setComponent(
     new ComponentName(
     "com.weidi.media.wdplayer",
     "com.weidi.media.wdplayer.video_player.PlayerService"));
     intent.setAction(PlayerService.COMMAND_ACTION);
     intent.putExtra(PlayerService.COMMAND_PATH, mediaPath);
     intent.putExtra(PlayerService.COMMAND_TYPE, mediaType);
     intent.putExtra(PlayerService.COMMAND_NAME, PlayerService.COMMAND_SHOW_WINDOW);
     startService(intent);
     */

    public static final String COMMAND_ACTION =
            "com.weidi.media.wdplayer.video_player.PlayerService";
    public static final String COMMAND_NAME = "HandlePlayerService";
    // 需要播放的媒体路径
    public static final String COMMAND_PATH = "HandlePlayerServicePath";
    // 需要播放的媒体是视频("video/")还是音频("audio/")
    public static final String COMMAND_TYPE = "HandlePlayerServiceType";
    public static final String COMMAND_ON_EVENT_TYPE = "command_on_event_type";
    public static final String COMMAND_SET_REMAINING_TIME_STR = "setRemainingTime";

    // 对COMMAND_NAME进行设置的值
    public static final int COMMAND_SHOW_WINDOW = 1;
    public static final int COMMAND_HIDE_WINDOW = 2;
    public static final int COMMAND_STOP_SERVICE = 3;
    public static final int COMMAND_RESTART_LOAD_CONTENTS = 4;
    public static final int COMMAND_HANDLE_LANDSCAPE_SCREEN = 5;
    public static final int COMMAND_HANDLE_PORTRAIT_SCREEN = 6;
    public static final int COMMAND_ON_EVENT = 7;
    // window a fm / window b ijk A窗口选择什么播放器
    // window a time_out 16000    A窗口设置超时时间
    // window a audio 0/1
    // window a media 0/1
    public static final int COMMAND_WHICH_WINDOW = 8;
    public static final int COMMAND_SET_REMAINING_TIME = 9;

    public static final int COMMAND_START_SECOND_PLAYERSERVICE = 10;
    public static final int COMMAND_STOP_SECOND_PLAYERSERVICE = 11;
    private static boolean sBindRemotePlayerService = false;

    // 测试时使用
    private void internalStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            // app crash后的操作
            Log.e(TAG, "handleAppCrash()");
            //handleAppCrash();
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
                mType = intent.getStringExtra(COMMAND_TYPE);
                Log.d(TAG, "internalStartCommand() mCurPath: " + mPath);
                Log.d(TAG, "internalStartCommand()    mType: " + mType);
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
            case COMMAND_ON_EVENT:
                int eventType = intent.getIntExtra(COMMAND_ON_EVENT_TYPE, -1);
                if (eventType != -1 && mPlayerWrapper != null) {
                    Bundle bundle = intent.getExtras();
                    if (bundle != null) {
                        mPlayerWrapper.onEvent(eventType, new Object[]{bundle});
                    } else {
                        mPlayerWrapper.onEvent(eventType, null);
                    }
                }
                break;
            case COMMAND_WHICH_WINDOW:
                if (mUseLocalPlayer) {
                    MyToast.show("使用远程窗口");
                    mUseLocalPlayer = false;
                } else {
                    MyToast.show("使用本地窗口");
                    mUseLocalPlayer = true;
                }
                break;
            case COMMAND_SET_REMAINING_TIME: {
                try {
                    String remaining_time_str =
                            intent.getStringExtra(COMMAND_SET_REMAINING_TIME_STR);
                    if (TextUtils.isEmpty(remaining_time_str)) {
                        remaining_time_str = "-0.01";
                    }
                    double remaining_time = Double.parseDouble(remaining_time_str);
                    FFMPEG.getDefault().onTransact(
                            DO_SOMETHING_CODE_setRemainingTime,
                            JniObject.obtain().writeDouble(remaining_time));
                } catch (Exception e) {

                }
                break;
            }
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
        unbindRemotePlayerService();
    }

    private void handleAppCrash() {
        SharedPreferences sp = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        boolean isNormalFinish = sp.getBoolean(PLAYBACK_NORMAL_FINISH, true);
        mPath = sp.getString(PLAYBACK_ADDRESS, null);
        if (!isNormalFinish && !TextUtils.isEmpty(mPath)) {
            mType = sp.getString(PLAYBACK_MEDIA_TYPE, null);
            if (TextUtils.isEmpty(mType)
                    || mType.startsWith("video/")) {
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

                mUiHandler.removeMessages(COMMAND_SHOW_WINDOW);
                mUiHandler.sendEmptyMessageDelayed(COMMAND_SHOW_WINDOW, 3000);
            }
        }
    }

    // 当前Service活着的时候,由其他地方发送事件到这里进行处理
    private Object onEvent(int what, Object[] objArray) {
        Object result = null;
        switch (what) {
            case COMMAND_SHOW_WINDOW:
                if (objArray != null && objArray.length >= 2) {
                    mPath = (String) objArray[0];
                    mType = (String) objArray[1];
                    mUiHandler.removeMessages(COMMAND_SHOW_WINDOW);
                    mUiHandler.sendEmptyMessage(COMMAND_SHOW_WINDOW);
                }
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
                int delay = 0;
                if (objArray != null && objArray.length > 0) {
                    delay = (Integer) objArray[0];
                }
                mUiHandler.removeMessages(COMMAND_HANDLE_PORTRAIT_SCREEN);
                if (delay > 0) {
                    mUiHandler.sendEmptyMessageDelayed(COMMAND_HANDLE_PORTRAIT_SCREEN, delay);
                    return null;
                }
                mUiHandler.sendEmptyMessage(COMMAND_HANDLE_PORTRAIT_SCREEN);
                break;
            case COMMAND_WHICH_WINDOW:
                break;
            case COMMAND_START_SECOND_PLAYERSERVICE: {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        bindRemotePlayerService();
                        return null;
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                break;
            }
            case COMMAND_STOP_SECOND_PLAYERSERVICE: {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        unbindRemotePlayerService();
                        return null;
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                break;
            }
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
                SharedPreferences sp = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
                boolean needTwoPlayer = sp.getBoolean(NEED_TWO_PLAYER, false);
                if (mRemoteVideoPlayer == null || !needTwoPlayer || mUseLocalPlayer) {
                    if (mPlayerWrapper != null) {
                        mPlayerWrapper.setType(mType);
                        mPlayerWrapper.setDataSource(mPath);
                    }
                    return;
                }

                try {
                    mRemoteVideoPlayer.setDataSource(-1, mType);
                    mRemoteVideoPlayer.setDataSourceMetadata(-1, mPath, null);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case COMMAND_HIDE_WINDOW:
                if (mPlayerWrapper != null) {
                    mPlayerWrapper.removeView(false);
                }
                break;
            case COMMAND_STOP_SERVICE:
                if (mPlayerWrapper != null) {
                    mPlayerWrapper.removeView(false);
                }
                stopSelf();
                break;
            case COMMAND_HANDLE_LANDSCAPE_SCREEN:
                if (mPlayerWrapper != null) {
                    if (msg.arg1 == 0) {
                        if (JniPlayerActivity.isAliveJniPlayerActivity) {
                            mPlayerWrapper.handleLandscapeScreen(0);
                        } else {
                            mPlayerWrapper.handlePortraitScreenWithTV();
                        }
                    } else if (msg.arg1 == 1) {
                        mPlayerWrapper.handleLandscapeScreen(1);
                    } else if (msg.arg1 == 2) {
                        mPlayerWrapper.handlePortraitScreenWithTV();
                    }
                }
                if (mRemoteVideoPlayer != null) {
                    try {
                        mRemoteVideoPlayer.setDataSource(-1, "LandscapeScreen");
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case COMMAND_HANDLE_PORTRAIT_SCREEN:
                if (mPlayerWrapper != null) {
                    mPlayerWrapper.handlePortraitScreen();
                }
                if (mRemoteVideoPlayer != null) {
                    try {
                        mRemoteVideoPlayer.setDataSource(-1, "PortraitScreen");
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
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
        // 创建非模态,不可碰触
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        //layoutParams.gravity = Gravity.TOP + Gravity.LEFT;
        layoutParams.gravity = Gravity.TOP + Gravity.START;
        layoutParams.width = 1;
        layoutParams.height = 1;
        layoutParams.x = 0;
        layoutParams.y = 0;
        mWindowManager.addView(mView, layoutParams);

        if (mPlayerWrapper == null) {
            mPlayerWrapper = new PlayerWrapper();
        }
        mPlayerWrapper.setService(this);

        SharedPreferences sp = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        boolean needTwoPlayer = sp.getBoolean(NEED_TWO_PLAYER, false);
        if (needTwoPlayer) {
            UiModeManager uiModeManager =
                    (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
            int whatIsDevice = uiModeManager.getCurrentModeType();
            if (whatIsDevice != Configuration.UI_MODE_TYPE_WATCH) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        bindRemotePlayerService();
                        return null;
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
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

    /////////////////////////////////////////////////////////////////

    private static final int SUCCESS = 0;

    private IDmrPlayerApp mRemoteVideoPlayer;

    private final IDmrPlayerApp.Stub mBinder = new IDmrPlayerApp.Stub() {

        private boolean needTwoPlayer = false;

        @Override
        public void registerCallback(int iid, IDmrPlayerAppCallback cb) throws RemoteException {
            Log.i(TAG, "registerCallback() iid:" + iid + " cb: " + cb);
            if (iid == 0) {
                mPlayerWrapper.registerCallback(iid, cb);
            } else {
                if (mRemoteVideoPlayer != null) {
                    mRemoteVideoPlayer.registerCallback(iid, cb);
                }
            }
        }

        @Override
        public void unregisterCallback(int iid, IDmrPlayerAppCallback cb) throws RemoteException {
            Log.i(TAG, "unregisterCallback() iid:" + iid + " cb: " + cb);
            if (iid == 0) {
                mPlayerWrapper.unregisterCallback(iid, cb);
            } else {
                if (mRemoteVideoPlayer != null) {
                    mRemoteVideoPlayer.unregisterCallback(iid, cb);
                }
            }
        }

        @Override
        public int setDataSource(int iid, String uri) throws RemoteException {
            return SUCCESS;
        }

        @Override
        public int setDataSourceMetadata(final int iid, String uri, Map metadata) throws RemoteException {
            Log.i(TAG, "setDataSourceMetadata()" +
                    "\niid: " + iid +
                    "\nuri: " + uri +
                    "\nmetadata: " + metadata);
            if (iid == 0) {
                mPlayerWrapper.setType("video/");
                mPlayerWrapper.setDataSource(uri);
            } else {
                if (mRemoteVideoPlayer != null) {
                    mRemoteVideoPlayer.setDataSourceMetadata(iid, uri, metadata);
                }
            }

            return SUCCESS;
        }

        @SuppressWarnings("unchecked")
        private int setDataSourceCommon(int iid, String uri, Map metadata) {
            Log.i(TAG, "setDataSourceCommon() iid: " + iid);
            return SUCCESS;
        }

        @Override
        public int stop(int iid) throws RemoteException {
            Log.i(TAG, "stop() iid: " + iid);
            if (iid == 0) {
                mPlayerWrapper.stopForDlna(iid);
            } else {
                if (mRemoteVideoPlayer != null) {
                    mRemoteVideoPlayer.stop(iid);
                }
            }
            return SUCCESS;
        }

        @Override
        public int start(int iid) throws RemoteException {
            Log.i(TAG, "start() iid: " + iid);
            if (iid == 0) {
                mPlayerWrapper.playForDlna(iid);
            } else {
                if (mRemoteVideoPlayer != null) {
                    mRemoteVideoPlayer.start(iid);
                }
            }
            return SUCCESS;
        }

        @Override
        public int pause(int iid) throws RemoteException {
            Log.i(TAG, "pause() iid: " + iid);
            if (iid == 0) {
                mPlayerWrapper.pauseForDlna(iid);
            } else {
                if (mRemoteVideoPlayer != null) {
                    mRemoteVideoPlayer.pause(iid);
                }
            }
            return SUCCESS;
        }

        @Override
        public int seekTo(int iid, int msec) throws RemoteException {
            Log.i(TAG, "seekTo() iid: " + iid + " msec: " + msec);
            if (iid == 0) {
                mPlayerWrapper.seekToForDlna(iid, msec);
            } else {
                if (mRemoteVideoPlayer != null) {
                    mRemoteVideoPlayer.seekTo(iid, msec);
                }
            }
            return SUCCESS;
        }

        @Override
        public int getCurrentPosition(int iid) throws RemoteException {
            if (iid == 0) {
                return mPlayerWrapper.getCurrentPositionForDlna(iid);
            } else {
                if (mRemoteVideoPlayer != null) {
                    return mRemoteVideoPlayer.getCurrentPosition(iid);
                }
            }
            return 0;
        }

        @Override
        public int getDuration(int iid) throws RemoteException {
            if (iid == 0) {
                return mPlayerWrapper.getDurationForDlna(iid);
            } else {
                if (mRemoteVideoPlayer != null) {
                    return mRemoteVideoPlayer.getDuration(iid);
                }
            }
            return 0;
        }

        @Override
        public int setPlaySpeed(int iid, String speedSpec) throws RemoteException {
            Log.i(TAG, "setPlaySpeed() iid: " + iid + " speedSpec: " + speedSpec);
            if (iid == 0) {
            } else {
                if (mRemoteVideoPlayer != null) {
                }
            }
            return SUCCESS;
        }

        @Override
        public String availablePlaySpeed(int iid) throws RemoteException {
            Log.i(TAG, "availablePlaySpeed() iid: " + iid);
            if (iid == 0) {
            } else {
                if (mRemoteVideoPlayer != null) {
                }
            }
            return "1";
        }

        @Override
        public int onTransact(int iid, int code, Bundle data) throws RemoteException {
            return 0;
        }
    };

    private void bindRemotePlayerService() {
        if (!sBindRemotePlayerService) {
            Log.v(TAG, "######## bindService(RemotePlayerService) ########");
            Intent intent = new Intent();
            ComponentName cn = new ComponentName(
                    "com.weidi.media.wdplayer",
                    "com.weidi.media.wdplayer.video_player.RemotePlayerService");
            intent.setComponent(cn);
            bindService(intent, mRemoteVideoConnection, Context.BIND_AUTO_CREATE);
            sBindRemotePlayerService = true;
        }
    }

    private void unbindRemotePlayerService() {
        if (sBindRemotePlayerService) {
            Log.v(TAG, "######## unbindService(RemotePlayerService) ########");
            unbindService(mRemoteVideoConnection);
            sBindRemotePlayerService = false;
        }
    }

    private ServiceConnection mRemoteVideoConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TAG, "onServiceConnected() " + className.getClassName());
            mRemoteVideoPlayer = IDmrPlayerApp.Stub.asInterface(service);
            Log.i(TAG, "onServiceConnected() mRemoteVideoPlayer: " + mRemoteVideoPlayer);
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.i(TAG, "onServiceDisconnected() " + className.getClassName());
            mRemoteVideoPlayer = null;
        }
    };

}

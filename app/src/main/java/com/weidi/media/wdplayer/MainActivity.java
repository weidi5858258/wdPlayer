package com.weidi.media.wdplayer;

import android.app.Activity;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.media.wdplayer.business.contents.LiveActivity;
import com.weidi.media.wdplayer.business.contents.LiveActivityForIptv;
import com.weidi.media.wdplayer.business.contents.LiveActivityForMenFavorite;
import com.weidi.media.wdplayer.business.contents.LocalAudioActivity;
import com.weidi.media.wdplayer.socket.SocketClient;
import com.weidi.media.wdplayer.socket.SocketServer;
import com.weidi.media.wdplayer.util.MediaUtils;
import com.weidi.media.wdplayer.video_player.FullScreenActivity;
import com.weidi.media.wdplayer.video_player.JniPlayerActivity;
import com.weidi.media.wdplayer.video_player.PlayerService;
import com.weidi.media.wdplayer.video_player.PlayerWrapper;
import com.weidi.utils.MyToast;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import static com.weidi.media.wdplayer.Constants.BUTTON_CLICK_EXIT;
import static com.weidi.media.wdplayer.Constants.BUTTON_CLICK_FF;
import static com.weidi.media.wdplayer.Constants.BUTTON_CLICK_FR;
import static com.weidi.media.wdplayer.Constants.BUTTON_CLICK_NEXT;
import static com.weidi.media.wdplayer.Constants.BUTTON_CLICK_PAUSE;
import static com.weidi.media.wdplayer.Constants.BUTTON_CLICK_PLAY;
import static com.weidi.media.wdplayer.Constants.BUTTON_CLICK_PREV;
import static com.weidi.media.wdplayer.Constants.BUTTON_CLICK_REPEAT_ALL;
import static com.weidi.media.wdplayer.Constants.BUTTON_CLICK_REPEAT_OFF;
import static com.weidi.media.wdplayer.Constants.BUTTON_CLICK_REPEAT_ONE;
import static com.weidi.media.wdplayer.Constants.BUTTON_CLICK_SHUFFLE_OFF;
import static com.weidi.media.wdplayer.Constants.BUTTON_CLICK_SHUFFLE_ON;
import static com.weidi.media.wdplayer.Constants.BUTTON_CLICK_VOLUME_MUTE;
import static com.weidi.media.wdplayer.Constants.BUTTON_CLICK_VOLUME_NORMAL;
import static com.weidi.media.wdplayer.Constants.DO_SOMETHING_EVENT_GET_MEDIA_DURATION;
import static com.weidi.media.wdplayer.Constants.DO_SOMETHING_EVENT_GET_REPEAT;
import static com.weidi.media.wdplayer.Constants.DO_SOMETHING_EVENT_GET_SHUFFLE;
import static com.weidi.media.wdplayer.Constants.DO_SOMETHING_EVENT_IS_RUNNING;
import static com.weidi.media.wdplayer.Constants.HARD_SOLUTION;
import static com.weidi.media.wdplayer.Constants.NEED_SHOW_CACHE_PROGRESS;
import static com.weidi.media.wdplayer.Constants.NEED_SHOW_MEDIA_INFO;
import static com.weidi.media.wdplayer.Constants.NEED_TWO_PLAYER;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_ADDRESS;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_MEDIA_TYPE;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_USE_PLAYER;
import static com.weidi.media.wdplayer.Constants.PLAYER_FFMPEG_MEDIACODEC;
import static com.weidi.media.wdplayer.Constants.PLAYER_FFPLAY;
import static com.weidi.media.wdplayer.Constants.PLAYER_IJKPLAYER;
import static com.weidi.media.wdplayer.Constants.PREFERENCES_NAME;
import static com.weidi.media.wdplayer.Constants.PREFERENCES_NAME_REMOTE;
import static com.weidi.media.wdplayer.video_player.JniPlayerActivity.isRunService;
import static com.weidi.media.wdplayer.video_player.PlayerService.COMMAND_START_SECOND_PLAYERSERVICE;
import static com.weidi.media.wdplayer.video_player.PlayerService.COMMAND_STOP_SECOND_PLAYERSERVICE;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final String PLAYERSERVICE =
            "com.weidi.media.wdplayer.video_player.PlayerService";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(
                com.weidi.library.R.anim.push_left_in,
                com.weidi.library.R.anim.push_left_out);
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");
        // Volume change should always affect media volume_normal
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        setContentView(R.layout.activity_main);

        internalCreate(savedInstanceState);

        /*new Thread(new Runnable() {
            @Override
            public void run() {
                test();
            }
        }).start();*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");
        internalResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop()");
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy()");
        super.onDestroy();
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
                    PlayerService.mUseLocalPlayer = true;
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    PlayerService.mUseLocalPlayer = false;
                    break;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    break;
                default:
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "onConfigurationChanged()" +
                " newConfig: " + newConfig.toString());

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

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(
                com.weidi.library.R.anim.push_right_in,
                com.weidi.library.R.anim.push_right_out);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult() requestCode: " + requestCode +
                " resultCode: " + resultCode +
                " data: " + data);
        if (requestCode == 0
                && resultCode == Activity.RESULT_CANCELED
                && null == data) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // onActivityResult() requestCode: 0 resultCode: 0 data: null
                if (Settings.canDrawOverlays(this)) {
                    startService(new Intent(this, PlayerService.class));
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && IS_PHONE) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            boolean hasIgnored = powerManager.isIgnoringBatteryOptimizations(getPackageName());
            // 判断当前APP是否有加入电池优化的白名单，如果没有，弹出加入电池优化的白名单的设置对话框。
            if (!hasIgnored) {
                Intent intent =
                        new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////

    private Handler mUiHandler;
    private SharedPreferences mPreferences;
    private int clickCounts = 0;
    private boolean IS_PHONE = false;
    private boolean IS_WATCH = false;
    private boolean IS_TV = false;

    // 快退
    private ImageButton mFrIB;
    // 快进
    private ImageButton mFfIB;
    // 歌曲上一首
    private ImageButton mPrevIB;
    // 歌曲下一首
    private ImageButton mNextIB;
    private ImageButton mPlayIB;
    private ImageButton mPauseIB;
    private ImageButton mExitIB;
    // 声音
    private ImageButton mVolumeNormal;
    private ImageButton mVolumeMute;
    private ImageButton mRepeatOff;
    private ImageButton mRepeatAll;
    private ImageButton mRepeatOne;
    private ImageButton mShuffleOff;
    private ImageButton mShuffleOn;

    private EditText mAddressET;

    // 关闭重复播放
    private PlayerWrapper.Repeat mRepeat = PlayerWrapper.Repeat.Repeat_Off;
    // 关闭随机播放
    private PlayerWrapper.Shuffle mShuffle = PlayerWrapper.Shuffle.Shuffle_Off;

    private void internalCreate(Bundle savedInstanceState) {
        UiModeManager uiModeManager =
                (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
        int whatIsDevice = uiModeManager.getCurrentModeType();
        IS_PHONE = false;
        IS_WATCH = false;
        IS_TV = false;
        switch (whatIsDevice) {
            case Configuration.UI_MODE_TYPE_WATCH:
                IS_WATCH = true;
                break;
            case Configuration.UI_MODE_TYPE_TELEVISION:
                IS_TV = true;
                break;
            case Configuration.UI_MODE_TYPE_NORMAL:
            default:
                IS_PHONE = true;
                break;
        }

        mPreferences = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                //super.handleMessage(msg);

                SharedPreferences sp = null;
                /*if (clickCounts > 5) {
                    clickCounts = 5;
                }*/
                switch (clickCounts) {
                    case 1:
                        break;
                    case 2:
                        startActivity(new Intent(MainActivity.this, LiveActivity.class));
                        break;
                    case 3:
                        startActivity(new Intent(MainActivity.this, LiveActivityForIptv.class));
                        break;
                    case 4:
                        /*Intent intent = new Intent();
                        intent.putExtra(JniPlayerActivity.COMMAND_NO_FINISH, true);
                        intent.setClass(MainActivity.this, JniPlayerActivity.class);
                        startActivity(intent);*/
                        startActivity(new Intent(MainActivity.this, FullScreenActivity.class));
                        break;
                    case 5:
                        startActivity(new Intent(MainActivity.this, LocalAudioActivity.class));
                        break;
                    case 6:
                        sp = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
                        sp.edit().putString(PLAYBACK_USE_PLAYER, PLAYER_FFPLAY).commit();
                        MyToast.show(PLAYER_FFPLAY);
                        break;
                    case 7:
                        sp = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
                        sp.edit().putString(PLAYBACK_USE_PLAYER, PLAYER_FFMPEG_MEDIACODEC).commit();
                        MyToast.show(PLAYER_FFMPEG_MEDIACODEC);
                        break;
                    case 8:
                        sp = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
                        sp.edit().putString(PLAYBACK_USE_PLAYER, PLAYER_IJKPLAYER).commit();
                        MyToast.show(PLAYER_IJKPLAYER);
                        break;
                    case 9:
                        sp = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
                        boolean needTwoPlayer = sp.getBoolean(NEED_TWO_PLAYER, false);
                        if (needTwoPlayer) {
                            MyToast.show("禁用同时使用两个播放器");
                            sp.edit().putBoolean(NEED_TWO_PLAYER, false).commit();
                            EventBusUtils.post(
                                    PlayerService.class,
                                    COMMAND_STOP_SECOND_PLAYERSERVICE,
                                    null);
                        } else {
                            MyToast.show("启用同时使用两个播放器");
                            sp.edit().putBoolean(NEED_TWO_PLAYER, true).commit();
                            EventBusUtils.post(
                                    PlayerService.class,
                                    COMMAND_START_SECOND_PLAYERSERVICE,
                                    null);
                        }
                        break;
                    case 10:
                        sp = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
                        boolean needShowMediaInfo = sp.getBoolean(NEED_SHOW_MEDIA_INFO, false);
                        if (needShowMediaInfo) {
                            MyToast.show("隐藏媒体信息");
                            sp.edit().putBoolean(NEED_SHOW_MEDIA_INFO, false).commit();
                        } else {
                            MyToast.show("显示媒体信息");
                            sp.edit().putBoolean(NEED_SHOW_MEDIA_INFO, true).commit();
                        }
                        break;
                    case 11:
                        sp = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
                        boolean needShowCacheProgress = sp.getBoolean(
                                NEED_SHOW_CACHE_PROGRESS, false);
                        if (needShowCacheProgress) {
                            MyToast.show("隐藏顶部缓冲进度条");
                            sp.edit().putBoolean(NEED_SHOW_CACHE_PROGRESS, false).commit();
                        } else {
                            MyToast.show("显示顶部缓冲进度条");
                            sp.edit().putBoolean(NEED_SHOW_CACHE_PROGRESS, true).commit();
                        }
                        break;
                    case 12: {
                        FullScreenActivity.SCREEN_ORIENTATION_LANDSCAPE = true;
                        MyToast.show("横屏向左");
                        break;
                    }
                    case 13: {
                        FullScreenActivity.SCREEN_ORIENTATION_LANDSCAPE = false;
                        MyToast.show("横屏向右");
                        break;
                    }
                    case 14: {
                        //finish();
                        break;
                    }
                    case 20:
                        if (IS_PHONE) {
                            startActivity(
                                    new Intent(MainActivity.this,
                                            LiveActivityForMenFavorite.class));
                        }
                        break;
                    default:
                        break;
                }
                clickCounts = 0;
            }
        };

        View view = findViewById(R.id.text);
        view.setClickable(true);
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnClickListener(mOnClickListener);
        findViewById(R.id.playback_btn).setOnClickListener(mOnClickListener);
        findViewById(R.id.use_local_player_btn).setOnClickListener(mOnClickListener);
        findViewById(R.id.use_remote_player_btn).setOnClickListener(mOnClickListener);
        findViewById(R.id.mc_switch).setOnClickListener(mOnClickListener);

        if (IS_TV) {
            setTvView();
        } else if (IS_PHONE) {
            mAddressET = findViewById(R.id.address_et);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 申请浮窗权限
            if (!isRunService(this, PLAYERSERVICE)) {
                if (!Settings.canDrawOverlays(this)) {
                    Intent intent = new Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, 0);
                } else {
                    startService(new Intent(this, PlayerService.class));
                }
            }

            test2();
        }
    }

    private void internalResume() {
        if (IS_TV) {
            findViewById(R.id.address_et).setVisibility(View.GONE);
            Object result = EventBusUtils.post(
                    PlayerWrapper.class, DO_SOMETHING_EVENT_IS_RUNNING, null);
            boolean isRunning = false;
            if (result != null) {
                isRunning = (boolean) result;
            }
            if (isRunning) {
                findViewById(R.id.controller_panel_framelayout).setVisibility(View.VISIBLE);

                long mediaDuration = (long) EventBusUtils.post(
                        PlayerWrapper.class, DO_SOMETHING_EVENT_GET_MEDIA_DURATION, null);
                mRepeat = (PlayerWrapper.Repeat) EventBusUtils.post(
                        PlayerWrapper.class, DO_SOMETHING_EVENT_GET_REPEAT, null);
                mShuffle = (PlayerWrapper.Shuffle) EventBusUtils.post(
                        PlayerWrapper.class, DO_SOMETHING_EVENT_GET_SHUFFLE, null);

                SeekBar progress_bar = findViewById(R.id.progress_bar);
                RelativeLayout show_time_rl = findViewById(R.id.show_time_rl);
                ImageButton button_fr = findViewById(R.id.button_fr);
                ImageButton button_ff = findViewById(R.id.button_ff);
                ImageButton button_prev = findViewById(R.id.button_prev);
                ImageButton button_next = findViewById(R.id.button_next);
                ImageButton button_repeat_off = findViewById(R.id.button_repeat_off);
                ImageButton button_repeat_all = findViewById(R.id.button_repeat_all);
                ImageButton button_repeat_one = findViewById(R.id.button_repeat_one);
                ImageButton button_shuffle_off = findViewById(R.id.button_shuffle_off);
                ImageButton button_shuffle_on = findViewById(R.id.button_shuffle_on);
                if (mediaDuration <= 0) {
                    progress_bar.setVisibility(View.GONE);
                    show_time_rl.setVisibility(View.GONE);
                    button_fr.setVisibility(View.INVISIBLE);
                    button_ff.setVisibility(View.INVISIBLE);
                    button_prev.setVisibility(View.INVISIBLE);
                    button_next.setVisibility(View.INVISIBLE);
                    button_repeat_off.setVisibility(View.INVISIBLE);
                    button_repeat_all.setVisibility(View.INVISIBLE);
                    button_repeat_one.setVisibility(View.INVISIBLE);
                    button_shuffle_off.setVisibility(View.INVISIBLE);
                    button_shuffle_on.setVisibility(View.INVISIBLE);
                } else {
                    progress_bar.setVisibility(View.VISIBLE);
                    show_time_rl.setVisibility(View.VISIBLE);
                    button_fr.setVisibility(View.VISIBLE);
                    button_ff.setVisibility(View.VISIBLE);
                    button_prev.setVisibility(View.VISIBLE);
                    button_next.setVisibility(View.VISIBLE);
                    setRepeatView();
                    setShuffleView();
                }

                if (mPlayIB.getVisibility() == View.VISIBLE) {
                    mPlayIB.requestFocus();
                } else {
                    mPauseIB.requestFocus();
                }
            } else {
                findViewById(R.id.controller_panel_framelayout).setVisibility(View.GONE);
            }
        } else if (IS_PHONE) {
            findViewById(R.id.controller_panel_framelayout).setVisibility(View.GONE);
            String path = mPreferences.getString(PLAYBACK_ADDRESS, null);
            if (!TextUtils.isEmpty(path)) {
                mAddressET.setText(path);
            }
        }

        SharedPreferences sp = getSharedPreferences(
                PREFERENCES_NAME, Context.MODE_PRIVATE);
        int softSolution = sp.getInt(HARD_SOLUTION, 1);
        Switch mc_switch = findViewById(R.id.mc_switch);
        if (softSolution == 0) {
            mc_switch.setChecked(false);
        } else {
            mc_switch.setChecked(true);
        }
    }

    private void setTvView() {
        SeekBar mProgressBar = findViewById(R.id.progress_bar);
        TextView mFileNameTV = findViewById(R.id.file_name_tv);
        TextView mProgressTimeTV = findViewById(R.id.progress_time_tv);
        TextView mSeekTimeTV = findViewById(R.id.seek_time_tv);
        TextView mDurationTimeTV = findViewById(R.id.duration_time_tv);

        mFrIB = findViewById(R.id.button_fr);
        mFfIB = findViewById(R.id.button_ff);
        mPrevIB = findViewById(R.id.button_prev);
        mNextIB = findViewById(R.id.button_next);
        mPlayIB = findViewById(R.id.button_play);
        mPauseIB = findViewById(R.id.button_pause);
        mExitIB = findViewById(R.id.button_exit);
        mVolumeNormal = findViewById(R.id.volume_normal);
        mVolumeMute = findViewById(R.id.volume_mute);
        mRepeatOff = findViewById(R.id.button_repeat_off);
        mRepeatAll = findViewById(R.id.button_repeat_all);
        mRepeatOne = findViewById(R.id.button_repeat_one);
        mShuffleOff = findViewById(R.id.button_shuffle_off);
        mShuffleOn = findViewById(R.id.button_shuffle_on);

        mFrIB.setOnClickListener(mOnClickListener);
        mFfIB.setOnClickListener(mOnClickListener);
        mPrevIB.setOnClickListener(mOnClickListener);
        mNextIB.setOnClickListener(mOnClickListener);
        mPlayIB.setOnClickListener(mOnClickListener);
        mPauseIB.setOnClickListener(mOnClickListener);
        mExitIB.setOnClickListener(mOnClickListener);
        mVolumeNormal.setOnClickListener(mOnClickListener);
        mVolumeMute.setOnClickListener(mOnClickListener);
        mRepeatOff.setOnClickListener(mOnClickListener);
        mRepeatAll.setOnClickListener(mOnClickListener);
        mRepeatOne.setOnClickListener(mOnClickListener);
        mShuffleOff.setOnClickListener(mOnClickListener);
        mShuffleOn.setOnClickListener(mOnClickListener);

        /*mFrIB.setOnFocusChangeListener(mOnFocusChangeListener);
        mFfIB.setOnFocusChangeListener(mOnFocusChangeListener);
        mPrevIB.setOnFocusChangeListener(mOnFocusChangeListener);
        mNextIB.setOnFocusChangeListener(mOnFocusChangeListener);
        mPlayIB.setOnFocusChangeListener(mOnFocusChangeListener);
        mPauseIB.setOnFocusChangeListener(mOnFocusChangeListener);
        mExitIB.setOnFocusChangeListener(mOnFocusChangeListener);
        mVolumeNormal.setOnFocusChangeListener(mOnFocusChangeListener);
        mVolumeMute.setOnFocusChangeListener(mOnFocusChangeListener);
        mRepeatOff.setOnFocusChangeListener(mOnFocusChangeListener);
        mRepeatAll.setOnFocusChangeListener(mOnFocusChangeListener);
        mRepeatOne.setOnFocusChangeListener(mOnFocusChangeListener);
        mShuffleOff.setOnFocusChangeListener(mOnFocusChangeListener);
        mShuffleOn.setOnFocusChangeListener(mOnFocusChangeListener);*/
    }

    private void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_fr:
                EventBusUtils.post(
                        PlayerWrapper.class,
                        BUTTON_CLICK_FR,
                        null);
                mFrIB.requestFocus();
                break;
            case R.id.button_ff:
                EventBusUtils.post(
                        PlayerWrapper.class,
                        BUTTON_CLICK_FF,
                        null);
                mFfIB.requestFocus();
                break;
            case R.id.button_prev:
                EventBusUtils.post(
                        PlayerWrapper.class,
                        BUTTON_CLICK_PREV,
                        null);
                mPrevIB.requestFocus();
                break;
            case R.id.button_next:
                EventBusUtils.post(
                        PlayerWrapper.class,
                        BUTTON_CLICK_NEXT,
                        null);
                mNextIB.requestFocus();
                break;
            case R.id.button_play:
                mPlayIB.setVisibility(View.INVISIBLE);
                mPauseIB.setVisibility(View.VISIBLE);
                EventBusUtils.post(
                        PlayerWrapper.class,
                        BUTTON_CLICK_PLAY,
                        null);
                mPauseIB.requestFocus();
                break;
            case R.id.button_pause:
                mPlayIB.setVisibility(View.VISIBLE);
                mPauseIB.setVisibility(View.INVISIBLE);
                EventBusUtils.post(
                        PlayerWrapper.class,
                        BUTTON_CLICK_PAUSE,
                        null);
                mPlayIB.requestFocus();
                break;
            case R.id.button_exit:
                EventBusUtils.post(
                        PlayerWrapper.class,
                        BUTTON_CLICK_EXIT,
                        null);
                findViewById(R.id.text).requestFocus();
                findViewById(R.id.controller_panel_framelayout).setVisibility(View.GONE);
                break;
            case R.id.volume_normal:
                mVolumeNormal.setVisibility(View.INVISIBLE);
                mVolumeMute.setVisibility(View.VISIBLE);
                EventBusUtils.post(
                        PlayerWrapper.class,
                        BUTTON_CLICK_VOLUME_NORMAL,
                        null);
                mVolumeMute.requestFocus();
                break;
            case R.id.volume_mute:
                mVolumeNormal.setVisibility(View.VISIBLE);
                mVolumeMute.setVisibility(View.INVISIBLE);
                EventBusUtils.post(
                        PlayerWrapper.class,
                        BUTTON_CLICK_VOLUME_MUTE,
                        null);
                mVolumeNormal.requestFocus();
                break;
            case R.id.button_repeat_off:
                mRepeat = PlayerWrapper.Repeat.Repeat_All;
                setRepeatView();
                EventBusUtils.post(
                        PlayerWrapper.class,
                        BUTTON_CLICK_REPEAT_OFF,
                        null);
                mRepeatAll.requestFocus();
                break;
            case R.id.button_repeat_all:
                mRepeat = PlayerWrapper.Repeat.Repeat_One;
                setRepeatView();
                EventBusUtils.post(
                        PlayerWrapper.class,
                        BUTTON_CLICK_REPEAT_ALL,
                        null);
                mRepeatOne.requestFocus();
                break;
            case R.id.button_repeat_one:
                mRepeat = PlayerWrapper.Repeat.Repeat_Off;
                setRepeatView();
                EventBusUtils.post(
                        PlayerWrapper.class,
                        BUTTON_CLICK_REPEAT_ONE,
                        null);
                mRepeatOff.requestFocus();
                break;
            case R.id.button_shuffle_off:
                mShuffle = PlayerWrapper.Shuffle.Shuffle_On;
                setShuffleView();
                EventBusUtils.post(
                        PlayerWrapper.class,
                        BUTTON_CLICK_SHUFFLE_OFF,
                        null);
                mShuffleOn.requestFocus();
                break;
            case R.id.button_shuffle_on:
                mShuffle = PlayerWrapper.Shuffle.Shuffle_Off;
                setShuffleView();
                EventBusUtils.post(
                        PlayerWrapper.class,
                        BUTTON_CLICK_SHUFFLE_ON,
                        null);
                mShuffleOff.requestFocus();
                break;
            case R.id.text:
                clickCounts++;
                MyToast.show(String.valueOf(clickCounts));
                mUiHandler.removeMessages(1);
                mUiHandler.sendEmptyMessageDelayed(1, 1000);
                break;
            case R.id.playback_btn:
                String mediaPath = null;
                String mediaType = null;
                if (mAddressET != null && mAddressET.getVisibility() == View.VISIBLE) {
                    mediaPath = mAddressET.getText().toString().trim();
                    mediaType = "video/";
                    if (TextUtils.isEmpty(mediaPath)) {
                        mediaPath = mPreferences.getString(PLAYBACK_ADDRESS, null);
                        mediaType = mPreferences.getString(PLAYBACK_MEDIA_TYPE, null);
                    }
                } else {
                    mediaPath = mPreferences.getString(PLAYBACK_ADDRESS, null);
                    mediaType = mPreferences.getString(PLAYBACK_MEDIA_TYPE, null);
                }
                if (!TextUtils.isEmpty(mediaPath) && !TextUtils.isEmpty(mediaType)) {
                    if (!isRunService(this, PLAYERSERVICE)) {
                        Intent intent = new Intent();
                        intent.setClass(this, PlayerService.class);
                        intent.setAction(PlayerService.COMMAND_ACTION);
                        intent.putExtra(PlayerService.COMMAND_PATH, mediaPath);
                        intent.putExtra(PlayerService.COMMAND_TYPE, mediaType);
                        intent.putExtra(PlayerService.COMMAND_NAME,
                                PlayerService.COMMAND_SHOW_WINDOW);
                        startService(intent);
                    } else {
                        EventBusUtils.post(
                                PlayerService.class,
                                PlayerService.COMMAND_SHOW_WINDOW,
                                new Object[]{mediaPath, mediaType});
                    }
                }
                break;
            case R.id.use_local_player_btn:
                PlayerService.mUseLocalPlayer = true;
                break;
            case R.id.use_remote_player_btn:
                PlayerService.mUseLocalPlayer = false;
                break;
            case R.id.mc_switch:
                SharedPreferences sp = getSharedPreferences(
                        PREFERENCES_NAME, Context.MODE_PRIVATE);
                boolean isChecked = ((Switch) v).isChecked();
                if (isChecked) {
                    sp.edit().putInt(HARD_SOLUTION, 1).commit();
                } else {
                    sp.edit().putInt(HARD_SOLUTION, 0).commit();
                }
                break;
            default:
                break;
        }
    }

    private void setRepeatView() {
        switch (mRepeat) {
            case Repeat_Off:
                mRepeatOff.setVisibility(View.VISIBLE);
                mRepeatAll.setVisibility(View.INVISIBLE);
                mRepeatOne.setVisibility(View.INVISIBLE);
                break;
            case Repeat_All:
                mRepeatOff.setVisibility(View.INVISIBLE);
                mRepeatAll.setVisibility(View.VISIBLE);
                mRepeatOne.setVisibility(View.INVISIBLE);
                break;
            case Repeat_One:
                mRepeatOff.setVisibility(View.INVISIBLE);
                mRepeatAll.setVisibility(View.INVISIBLE);
                mRepeatOne.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    private void setShuffleView() {
        switch (mShuffle) {
            case Shuffle_Off:
                mShuffleOff.setVisibility(View.VISIBLE);
                mShuffleOn.setVisibility(View.INVISIBLE);
                break;
            case Shuffle_On:
                mShuffleOff.setVisibility(View.INVISIBLE);
                mShuffleOn.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    /*private void onFocusChange(View v, boolean hasFocus) {
        switch (v.getId()) {
            case R.id.button_fr:
                break;
            case R.id.button_ff:
                break;
            case R.id.button_prev:
                break;
            case R.id.button_next:
                break;
            case R.id.button_play:
                break;
            case R.id.button_pause:
                break;
            case R.id.button_exit:
                break;
            case R.id.volume_normal:
                break;
            case R.id.volume_mute:
                break;
            case R.id.button_repeat_off:
                break;
            case R.id.button_repeat_all:
                break;
            case R.id.button_repeat_one:
                break;
            case R.id.button_shuffle_off:
                break;
            case R.id.button_shuffle_on:
                break;
            default:
                break;
        }
    }*/

    private void test2() {
        /***
         {@link Environment#DIRECTORY_MUSIC}
         {@link Environment#DIRECTORY_MOVIES}
         {@link Environment#DIRECTORY_PICTURES}
         {@link Environment#DIRECTORY_PODCASTS}
         {@link Environment#DIRECTORY_RINGTONES}
         {@link Environment#DIRECTORY_ALARMS}
         {@link Environment#DIRECTORY_NOTIFICATIONS}

         访问的还是手机本身的存储卡,不是外置的SD卡
         getFilesDir            :
         /data/user/0/com.weidi.usefragments/files

         getCacheDir            :
         /data/user/0/com.weidi.usefragments/cache

         getExternalCacheDir    :
         /storage/emulated/0/Android/data/com.weidi.usefragments/cache

         getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC)
         DIRECTORY_MUSIC        :
         /storage/emulated/0/Android/data/com.weidi.usefragments/files/Music

         DIRECTORY_MOVIES       :
         /storage/emulated/0/Android/data/com.weidi.usefragments/files/Movies

         DIRECTORY_PICTURES     :
         /storage/emulated/0/Android/data/com.weidi.usefragments/files/Pictures

         DIRECTORY_PODCASTS     :
         /storage/emulated/0/Android/data/com.weidi.usefragments/files/Podcasts

         DIRECTORY_RINGTONES    :
         /storage/emulated/0/Android/data/com.weidi.usefragments/files/Ringtones

         DIRECTORY_ALARMS       :
         /storage/emulated/0/Android/data/com.weidi.usefragments/files/Alarms

         DIRECTORY_NOTIFICATIONS:
         /storage/emulated/0/Android/data/com.weidi.usefragments/files/Notifications

         getExternalMediaDirs();
         /storage/emulated/0/Android/media/com.weidi.media.wdplayer
         */

        File file = null;
        file = new File(getFilesDir().getAbsolutePath());
        file = new File(getCacheDir().getAbsolutePath());
        file = new File(getExternalCacheDir().getAbsolutePath());
        file = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath());
        file = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES).getAbsolutePath());
        file = new File(getExternalFilesDir(Environment.DIRECTORY_ALARMS).getAbsolutePath());
        file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath());
        file = new File(getExternalFilesDir(Environment.DIRECTORY_PODCASTS).getAbsolutePath());
        file = new File(getExternalFilesDir(Environment.DIRECTORY_RINGTONES).getAbsolutePath());
        file = new File(getExternalFilesDir(Environment.DIRECTORY_RINGTONES).getAbsolutePath());
        file = new File(getExternalFilesDir(Environment.DIRECTORY_NOTIFICATIONS).getAbsolutePath());
        file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES).getAbsolutePath());
        Log.i(TAG, "canWrite: " + file.canWrite());// false

        /***
         getExternalFilesDirs(Environment.DIRECTORY_MOVIES)
         Environment.DIRECTORY_MOVIES:
         /storage/emulated/0/Android/data/com.weidi.usefragments/files/Movies
         Environment.DIRECTORY_MOVIES:
         /storage/2430-1702/Android/data/com.weidi.usefragments/files/Movies

         Environment.MEDIA_MOUNTED   :
         /storage/emulated/0/Android/data/com.weidi.usefragments/files/mounted
         Environment.MEDIA_MOUNTED   :
         /storage/2430-1702/Android/data/com.weidi.usefragments/files/mounted

         Environment.MEDIA_SHARED    :
         /storage/emulated/0/Android/data/com.weidi.usefragments/files/shared
         /storage/2430-1702/Android/data/com.weidi.usefragments/files/shared
         */
        File[] files = null;
        files = getExternalFilesDirs(Environment.DIRECTORY_MOVIES);
        for (File f : files) {
            Log.i(TAG, "Environment.DIRECTORY_MOVIES: " + f.getAbsolutePath());
        }
        files = getExternalFilesDirs(Environment.MEDIA_MOUNTED);
        for (File f : files) {
            Log.i(TAG, "Environment.MEDIA_MOUNTED   : " + f.getAbsolutePath());
        }
        files = getExternalFilesDirs(Environment.MEDIA_SHARED);
        for (File f : files) {
            Log.i(TAG, "Environment.MEDIA_SHARED    : " + f.getAbsolutePath());
        }

        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_VIDEO_RAW);
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_VIDEO_HEVC);
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_VIDEO_AVC);
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_VIDEO_H263);
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_VIDEO_MPEG4);
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_VIDEO_MPEG2);
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_VIDEO_VP8);
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_VIDEO_VP9);
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION);
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_VIDEO_SCRAMBLED);
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_VIDEO_AV1);
        // 没有遇到过
        /*MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_VIDEO_AV1);
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION);
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_VIDEO_SCRAMBLED);*/

        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_AUDIO_RAW);
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_AUDIO_FLAC);
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_AUDIO_MPEG);
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_AUDIO_AC3);
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_AUDIO_EAC3);
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_AUDIO_AC4);
        MediaUtils.findAllDecodersByMime("audio/mpeg-L2");

        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_AUDIO_AAC);
        MediaUtils.findAllDecodersByMime("audio/x-ms-wma");

        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_AUDIO_VORBIS);
        // 没有遇到过
        /*MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_AUDIO_OPUS);
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_AUDIO_QCELP);*/
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MainActivity.this.onClick(v);
        }
    };

    /*private View.OnFocusChangeListener mOnFocusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            MainActivity.this.onFocusChange(v, hasFocus);
        }
    };*/

    private void test() {
        SocketServer.getInstance().bind();
        SocketServer.getInstance().accept();

        //        SocketClient.getInstance().connect();
    }

}

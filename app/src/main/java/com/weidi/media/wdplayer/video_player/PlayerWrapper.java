package com.weidi.media.wdplayer.video_player;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.sonyericsson.dlna.dmr.player.IDmrPlayerAppCallback;
import com.weidi.eventbus.Phone;
import com.weidi.media.wdplayer.Constants;
import com.weidi.media.wdplayer.R;
import com.weidi.media.wdplayer.util.Callback;
import com.weidi.media.wdplayer.util.JniObject;
import com.weidi.media.wdplayer.util.NetworkUtils;
import com.weidi.media.wdplayer.util.NotificationUtil;
import com.weidi.utils.MyToast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import androidx.core.content.ContextCompat;

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
import static com.weidi.media.wdplayer.Constants.BUTTON_CLICK_TEST_START;
import static com.weidi.media.wdplayer.Constants.BUTTON_CLICK_TEST_STOP;
import static com.weidi.media.wdplayer.Constants.BUTTON_CLICK_VOLUME_MUTE;
import static com.weidi.media.wdplayer.Constants.BUTTON_CLICK_VOLUME_NORMAL;
import static com.weidi.media.wdplayer.Constants.DO_SOMETHING_EVENT_GET_MEDIA_DURATION;
import static com.weidi.media.wdplayer.Constants.DO_SOMETHING_EVENT_GET_REPEAT;
import static com.weidi.media.wdplayer.Constants.DO_SOMETHING_EVENT_GET_SHUFFLE;
import static com.weidi.media.wdplayer.Constants.DO_SOMETHING_EVENT_HANDLE_BUTTON_SIZE;
import static com.weidi.media.wdplayer.Constants.DO_SOMETHING_EVENT_IS_RUNNING;
import static com.weidi.media.wdplayer.Constants.DO_SOMETHING_EVENT_MIN_SCREEN;
import static com.weidi.media.wdplayer.Constants.DO_SOMETHING_EVENT_REPLAY;
import static com.weidi.media.wdplayer.Constants.DO_SOMETHING_EVENT_WIDTH_SCREEN;
import static com.weidi.media.wdplayer.Constants.HARD_SOLUTION;
import static com.weidi.media.wdplayer.Constants.HARD_SOLUTION_AUDIO;
import static com.weidi.media.wdplayer.Constants.NEED_SHOW_CACHE_PROGRESS;
import static com.weidi.media.wdplayer.Constants.NEED_SHOW_MEDIA_INFO;
import static com.weidi.media.wdplayer.Constants.NEED_TWO_PLAYER;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_ADDRESS;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_IS_MUTE;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_MEDIA_TYPE;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_NORMAL_FINISH;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_SHOW_CONTROLLERPANELLAYOUT;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_USE_PLAYER;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_WIDTH_PROPORTION;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_WINDOW_POSITION;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_WINDOW_POSITION_TAG;
import static com.weidi.media.wdplayer.Constants.PLAYER_FFMPEG_MEDIACODEC;
import static com.weidi.media.wdplayer.Constants.PLAYER_FFPLAY;
import static com.weidi.media.wdplayer.Constants.PLAYER_IJKPLAYER;
import static com.weidi.media.wdplayer.Constants.PLAYER_MEDIACODEC;
import static com.weidi.media.wdplayer.Constants.PREFERENCES_NAME;
import static com.weidi.media.wdplayer.Constants.PREFERENCES_NAME_REMOTE;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_allow_exit;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_download;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_frameByFrame;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_frameByFrameForFinish;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_frameByFrameForReady;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_init;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_isWatchForCloseAudio;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_isWatchForCloseVideo;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_stepAdd;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_stepSubtract;
import static com.weidi.media.wdplayer.video_player.FFMPEG.VOLUME_MUTE;
import static com.weidi.media.wdplayer.video_player.FFMPEG.VOLUME_NORMAL;
import static com.weidi.media.wdplayer.video_player.JniPlayerActivity.CONTENT_PATH;
import static com.weidi.media.wdplayer.video_player.JniPlayerActivity.CONTENT_TYPE;

/***

 */
public class PlayerWrapper {

    private static final String TAG = "player_alexander";

    public static boolean IS_PHONE = false;
    public static boolean IS_WATCH = false;
    public static boolean IS_TV = false;
    public static boolean IS_HIKEY970 = false;

    private static final int MSG_CHANGE_COLOR = 10;
    private static final int MSG_PREPARE = 11;
    private static final int MSG_START_PLAYBACK = 12;
    private static final int MSG_SEEK_TO_ADD = 13;
    private static final int MSG_SEEK_TO_SUBTRACT = 14;
    private static final int MSG_VOLUME_SEEK_TO_ADD = 15;
    private static final int MSG_VOLUME_SEEK_TO_SUBTRACT = 16;
    private static final int MSG_VOLUME_HIDE_LAYOUT = 17;
    private static final int MSG_DOWNLOAD = 18;
    private static final int MSG_LOAD_CONTENTS = 19;
    private static final int MSG_ADD_VIEW = 20;
    private static final int MSG_SCREEN_BRIGHT_WAKE_LOCK = 21;
    private static final int MSG_RELEASE = 22;
    private static final int MSG_TEST_SIGNAL = 23;

    private HashMap<String, Long> mPathTimeMap = new HashMap<>();
    private ArrayList<String> mCouldPlaybackPathList = new ArrayList<>();

    private SharedPreferences mSP;
    private PowerManager.WakeLock mPowerWakeLock;
    private SurfaceHolder mSurfaceHolder;
    private WdPlayer mWdPlayer;
    private FFMPEG mFFMPEGPlayer;
    private IjkPlayer mIjkPlayer;
    private SimpleVideoPlayer mSimpleVideoPlayer;
    //private SimplePlayer mSimpleVideoPlayer;
    private FfmpegUseMediaCodecDecode mFfmpegUseMediaCodecDecode;
    private boolean mIsAddedView = false;
    private String mPrePath;
    private String mCurPath;
    private String md5Path;
    // 有些mp3文件含有video,因此播放失败
    private String mType;
    // 单位秒
    private long mProgress;
    // 单位秒
    private long mPresentationTime;
    private int mDownloadProgress = -1;
    private long contentLength = -1;
    private boolean mNeedToSyncProgressBar = true;
    private boolean mIsScreenPress = false;
    private boolean mHasError = false;
    private int mErrorCode;
    // 单位秒
    private long mMediaDuration;
    private boolean mIsLocal = true;
    private boolean mIsLive = false;
    private boolean mIsH264 = false;
    private boolean mIsVideo = false;
    private boolean mIsAudio = false;
    private boolean mIsLocalPlayer = true;
    private SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;
    private View mRootView;
    private View mMediaPlayerRootLayout;

    private AlarmManager mAlarmManager;
    private AudioManager mAudioManager;
    private AudioFocusRequest mAudioFocusRequest;
    private int minVolume;
    private int maxVolume;
    // 查看剩余电量
    private BatteryManager mBatteryManager;
    private ScreenBroadcastReceiver mScreenReceiver;

    private SurfaceView mSurfaceView;
    private LinearLayout mControllerPanelLayout;
    private ProgressBar mLoadingLayout;
    private SeekBar mPositionSeekBar;
    private TextView mFileNameTV;
    private TextView mPositionTimeTV;
    private TextView mSeekTimeTV;
    private TextView mDurationTimeTV;
    // 快退
    private ImageButton mFrIB;
    // 快进
    private ImageButton mFfIB;
    // 歌曲上一首
    private ImageButton mPrevIB;
    // 歌曲下一首
    private ImageButton mNextIB;
    // 播放暂停
    private ImageButton mPlayIB;
    private ImageButton mPauseIB;
    private ImageButton mExitIB;
    // 声音
    private ImageButton mVolumeNormal;
    private ImageButton mVolumeMute;
    private ImageButton mScreenMax;
    private ImageButton mRepeatOff;
    private ImageButton mRepeatAll;
    private ImageButton mRepeatOne;
    private ImageButton mShuffleOff;
    private ImageButton mShuffleOn;
    // 声音控制条
    private View mVolumeLayout;
    private ImageButton mVolumeMin;
    private ImageButton mVolumeMax;
    private SeekBar mVolumeSeekBar;
    private int mVolumeProgress;
    // 下载
    private TextView mDownloadTV;
    private boolean mIsDownloading = false;
    /***
     1(停止下载)
     2(下载音视频,边下边播)
     3(只下载,不播放.不调用seekTo)
     4(只提取音视频,不播放.调用seekTo到0)
     */
    private int mDownloadClickCounts = 0;
    // 跟视频有关的提示信息
    private ScrollView textInfoScrollView;
    private TextView textInfoTV;
    private String textInfo;

    private Handler mUiHandler;
    private Handler mThreadHandler;
    private HandlerThread mHandlerThread;
    // 屏幕的宽高
    // 竖屏:width = 1080 height = 2244
    // 横屏:width = 2244 height = 1080
    private int mScreenWidth;
    private int mScreenHeight;
    // 视频源的宽高
    private int mVideoWidth;
    private int mVideoHeight;
    // 想要的宽高
    private int mNeedVideoWidth;
    private int mNeedVideoHeight;
    // 控制面板的高度
    private int mControllerPanelLayoutHeight;
    private int bit_rate_total;
    private int bit_rate_video;
    private int frame_rate;
    //  用于触摸滑动判断距离
    private float mX;
    private float mY;
    private float moveX;
    private float moveY;

    // 音视频的加载进度
    private LinearLayout mDataCacheLayout;
    private ProgressBar mVideoProgressBar;
    private ProgressBar mAudioProgressBar;
    private int mDataCacheLayoutHeight;

    private Context mContext;
    private PlayerService mPlayerService;
    private RemotePlayerService mRemotePlayerService;

    /***
     Configuration.UI_MODE_TYPE_NORMAL     手机
     Configuration.UI_MODE_TYPE_WATCH      手表
     Configuration.UI_MODE_TYPE_TELEVISION 电视机
     */
    private int whatIsDevice = -1;
    // 是否是竖屏 true为竖屏
    private boolean mIsPortraitScreen;

    private String whatPlayer = PLAYER_FFPLAY;

    // 第一个存储视频地址,第二个存储标题
    public static final LinkedHashMap<String, String> mContentsMap = new LinkedHashMap();
    public static final LinkedHashMap<String, String> mIptvContentsMap = new LinkedHashMap();
    public static final LinkedHashMap<String, String> mMenFavoriteContentsMap = new LinkedHashMap();
    public static LinkedHashMap<String, String> mLocalVideoContentsMap;
    public static LinkedHashMap<String, String> mLocalAudioContentsMap;
    // 当mShuffle = Shuffle.Shuffle_On;时,保存已经播放过的文件
    private ArrayList<Integer> mLocalContentsHasPlayedVideoIndexList = new ArrayList<>();
    private ArrayList<Integer> mLocalContentsHasPlayedAudioIndexList = new ArrayList<>();
    private ArrayList<String> mLocalContentsHasPlayedVideoPathList = new ArrayList<>();
    private ArrayList<String> mLocalContentsHasPlayedAudioPathList = new ArrayList<>();
    private String mLocalVideoPath;
    private String mLocalAudioPath;
    private Random mRandom = new Random();

    public enum Repeat {
        Repeat_Off, Repeat_All, Repeat_One
    }

    public enum Shuffle {
        Shuffle_Off, Shuffle_On
    }

    public enum Window {
        Full_Screen, Max_Screen, Min_Screen
    }

    // 关闭重复播放
    private Repeat mRepeat = Repeat.Repeat_Off;
    // 关闭随机播放
    private Shuffle mShuffle = Shuffle.Shuffle_Off;
    // 当mShuffle == Shuffle.Shuffle_Off时,上一首,下一首才有效
    private boolean mPlayPrevFile = false;
    private boolean mPlayNextFile = false;
    private Window mWindow = Window.Max_Screen;

    // 必须首先被调用
    public void setService(Service service) {
        if (service == null) {
            throw new NullPointerException("setService() service is null");
        }
        if (!(service instanceof PlayerService)
                && !(service instanceof RemotePlayerService)) {
            throw new IllegalArgumentException(
                    "!(service instanceof PlayerService) or " +
                            "!(service instanceof RemotePlayerService)");
        }
        mPlayerService = null;
        mRemotePlayerService = null;
        whatIsDevice = -1;

        mContext = service.getApplicationContext();
        if (service instanceof PlayerService) {
            PlayerService playerService = (PlayerService) service;
            mPlayerService = playerService;
            mIsLocalPlayer = true;
        } else if (service instanceof RemotePlayerService) {
            RemotePlayerService remotePlayerService = (RemotePlayerService) service;
            mRemotePlayerService = remotePlayerService;
            mIsLocalPlayer = false;
        }

        if (mIsLocalPlayer) {
            mSP = mContext.getSharedPreferences(
                    PREFERENCES_NAME, Context.MODE_PRIVATE);
        } else {
            mSP = mContext.getSharedPreferences(
                    PREFERENCES_NAME_REMOTE, Context.MODE_PRIVATE);
        }

        mBatteryManager = (BatteryManager) mContext.getSystemService(Context.BATTERY_SERVICE);
        mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);

        // 得到屏幕分辨率
        DisplayMetrics displayMetrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;

        UiModeManager uiModeManager =
                (UiModeManager) mContext.getSystemService(Context.UI_MODE_SERVICE);
        whatIsDevice = uiModeManager.getCurrentModeType();
        Log.i(TAG, "setService() whatIsDevice: " + whatIsDevice);
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

        if (IS_WATCH) {
            registerReceiver();
        }

        LayoutInflater inflater =
                (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (IS_WATCH) {
            mRootView = inflater.inflate(R.layout.media_player_wear, null);
        } else {
            mRootView = inflater.inflate(R.layout.media_player, null);
        }
        mLayoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            /*mLayoutParams.type = WindowManager.LayoutParams.TYPE_TOAST;
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_PRIORITY_PHONE;
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;*/
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mLayoutParams.gravity = Gravity.TOP + Gravity.LEFT;
        // 背景透明
        // mLayoutParams.format = PixelFormat.RGBA_8888;
        mLayoutParams.width = mScreenWidth;
        mLayoutParams.height = 400;
        mLayoutParams.x = 0;
        mLayoutParams.y = 0;

        mMediaPlayerRootLayout = mRootView.findViewById(R.id.mediaplayer_root_layout);
        mSurfaceView = mRootView.findViewById(R.id.surfaceView);
        mControllerPanelLayout = mRootView.findViewById(R.id.controller_panel_layout);
        mLoadingLayout = mRootView.findViewById(R.id.loading_view);
        mPositionSeekBar = mRootView.findViewById(R.id.progress_bar);
        mFileNameTV = mRootView.findViewById(R.id.file_name_tv);
        mPositionTimeTV = mRootView.findViewById(R.id.progress_time_tv);
        mSeekTimeTV = mRootView.findViewById(R.id.seek_time_tv);
        mDurationTimeTV = mRootView.findViewById(R.id.duration_time_tv);

        mFrIB = mRootView.findViewById(R.id.button_fr);
        mFfIB = mRootView.findViewById(R.id.button_ff);
        mPrevIB = mRootView.findViewById(R.id.button_prev);
        mNextIB = mRootView.findViewById(R.id.button_next);
        mPlayIB = mRootView.findViewById(R.id.button_play);
        mPauseIB = mRootView.findViewById(R.id.button_pause);
        mExitIB = mRootView.findViewById(R.id.button_exit);
        mVolumeNormal = mRootView.findViewById(R.id.volume_normal);
        mVolumeMute = mRootView.findViewById(R.id.volume_mute);
        mScreenMax = mRootView.findViewById(R.id.screen_max);
        mRepeatOff = mRootView.findViewById(R.id.button_repeat_off);
        mRepeatAll = mRootView.findViewById(R.id.button_repeat_all);
        mRepeatOne = mRootView.findViewById(R.id.button_repeat_one);
        mShuffleOff = mRootView.findViewById(R.id.button_shuffle_off);
        mShuffleOn = mRootView.findViewById(R.id.button_shuffle_on);
        mDownloadTV = mRootView.findViewById(R.id.download_tv);

        mVolumeLayout = mRootView.findViewById(R.id.volume_layout);
        mVolumeMin = mRootView.findViewById(R.id.button_volume_min);
        mVolumeMax = mRootView.findViewById(R.id.button_volume_max);
        mVolumeSeekBar = mRootView.findViewById(R.id.volume_progress_bar);

        mDataCacheLayout = mRootView.findViewById(R.id.progress_bar_layout);
        mVideoProgressBar = mRootView.findViewById(R.id.video_progress_bar);
        mAudioProgressBar = mRootView.findViewById(R.id.audio_progress_bar);

        textInfoScrollView = mRootView.findViewById(R.id.text_scrollview);
        textInfoTV = mRootView.findViewById(R.id.text_info_tv);

        View usedToMoveView = mRootView.findViewById(R.id.used_to_move_view);
        PlayerOnTouchListener playerOnTouchListener = new PlayerOnTouchListener();
        usedToMoveView.setOnTouchListener(playerOnTouchListener);
        mControllerPanelLayout.setOnTouchListener(playerOnTouchListener);
        // mRootView.setOnTouchListener(new PlayerOnTouchListener());

        mSurfaceView.setOnClickListener(mOnClickListener);
        mFrIB.setOnClickListener(mOnClickListener);
        mFfIB.setOnClickListener(mOnClickListener);
        mPrevIB.setOnClickListener(mOnClickListener);
        mNextIB.setOnClickListener(mOnClickListener);
        mPlayIB.setOnClickListener(mOnClickListener);
        mPauseIB.setOnClickListener(mOnClickListener);
        mExitIB.setOnClickListener(mOnClickListener);
        mDownloadTV.setOnClickListener(mOnClickListener);
        mVolumeNormal.setOnClickListener(mOnClickListener);
        mVolumeMute.setOnClickListener(mOnClickListener);
        mScreenMax.setOnClickListener(mOnClickListener);
        mRepeatOff.setOnClickListener(mOnClickListener);
        mRepeatAll.setOnClickListener(mOnClickListener);
        mRepeatOne.setOnClickListener(mOnClickListener);
        mShuffleOff.setOnClickListener(mOnClickListener);
        mShuffleOn.setOnClickListener(mOnClickListener);
        mVolumeMin.setOnClickListener(mOnClickListener);
        mVolumeMax.setOnClickListener(mOnClickListener);

        mExitIB.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Phone.call(PlayerWrapper.class.getName(), DO_SOMETHING_EVENT_REPLAY, null);
                return true;
            }
        });

        mVolumeNormal.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                buttonLongClickForVolumeNormal();
                return true;
            }
        });

        mVolumeMute.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                buttonLongClickForVolumeMute();
                return true;
            }
        });

        mScreenMax.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mWindow == Window.Full_Screen) {
                    Phone.call(FullScreenActivity.class.getName(), 4, null);
                    Phone.removeUiMessages(DO_SOMETHING_EVENT_HANDLE_BUTTON_SIZE);
                    Phone.callUiDelayed(PlayerWrapper.class.getName(),
                            DO_SOMETHING_EVENT_HANDLE_BUTTON_SIZE, 1000, null);
                    return true;
                }

                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setClass(mContext, FullScreenActivity.class);
                mContext.startActivity(intent);
                return true;
            }
        });

        if (!IS_WATCH) {
            mSurfaceView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (mPlayerService != null) {
                        if (PlayerService.mUseLocalPlayer) {
                            MyToast.show("使用远程窗口");
                            PlayerService.mUseLocalPlayer = false;
                        } else {
                            MyToast.show("使用本地窗口");
                            PlayerService.mUseLocalPlayer = true;
                        }
                        return true;
                    }

                    Intent intent = new Intent();
                    ComponentName cn = new ComponentName(
                            "com.weidi.media.wdplayer",
                            "com.weidi.media.wdplayer.video_player.PlayerService");
                    intent.setComponent(cn);
                    intent.setAction(PlayerService.COMMAND_ACTION);
                    intent.putExtra(PlayerService.COMMAND_NAME, PlayerService.COMMAND_WHICH_WINDOW);
                    mContext.startService(intent);

                    /*if (isFrameByFrameMode) {
                        return true;
                    }
                    int curVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    mAudioManager.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            curVolume,
                            AudioManager.FLAG_SHOW_UI);*/
                    return true;
                }
            });
            /*mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent motionEvent) {
                    switch (motionEvent.getAction()) {
                        case MotionEvent.ACTION_DOWN: {
                            mX = motionEvent.getX();
                            mY = motionEvent.getY();
                            break;
                        }
                        case MotionEvent.ACTION_MOVE: {
                            moveX = motionEvent.getX() - mX;
                            moveY = motionEvent.getY() - mY;

                            break;
                        }
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL: {
                            Log.i(TAG, "onTouch() moveX: " + moveX + " moveY: " + moveY);
                            break;
                        }
                        default:
                            break;
                    }
                    return false;
                }
            });*/
        }

        onCreate();
    }

    /*if (newPath.startsWith("http://")
            || newPath.startsWith("https://")
            || newPath.startsWith("rtmp://")
            || newPath.startsWith("rtsp://")
            || newPath.startsWith("ftp://")) {
        mIsLocal = false;
    }*/
    public void setDataSource(String path) {
        if (TextUtils.isEmpty(path)) {
            Log.i(TAG, "setDataSource() path is null");
            return;
        }
        if (TextUtils.equals(path, mCurPath) && mPrePath != null && mIsAddedView) {
            Log.i(TAG, "setDataSource() path:\n" + path + "\n正在播放中......");
            return;
        }
        if (!allowExit()) {
            Log.i(TAG, "setDataSource() 稍等片刻");
            MyToast.show("稍候再试");
            return;
        }

        mPrePath = mCurPath;
        mCurPath = path;
        Log.i(TAG, "setDataSource() mPrePath:\n" + mPrePath);
        Log.i(TAG, "setDataSource() mCurPath:\n" + mCurPath);

        mUiHandler.removeMessages(MSG_ADD_VIEW);
        mUiHandler.sendEmptyMessageDelayed(MSG_ADD_VIEW, 888);
        mUiHandler.removeMessages(MSG_RELEASE);
    }

    public void setType(String type) {
        Log.i(TAG, "setType()          mType: " + type);
        mType = type;
        mIsVideo = false;
        mIsAudio = false;
        if (TextUtils.isEmpty(mType)
                || mType.startsWith("video/")) {
            mIsVideo = true;
        } else if (mType.startsWith("audio/")) {
            mIsAudio = true;
        }
    }

    private void onCreate() {
        Phone.register(this);

        mPathTimeMap.clear();
        mContentsMap.clear();
        mCouldPlaybackPathList.clear();

        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                PlayerWrapper.this.uiHandleMessage(msg);
            }
        };

        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mThreadHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                PlayerWrapper.this.threadHandleMessage(msg);
            }
        };

        mFfmpegUseMediaCodecDecode = new FfmpegUseMediaCodecDecode();
        mFFMPEGPlayer = FFMPEG.getDefault();
        mFfmpegUseMediaCodecDecode.setContext(mContext);
        mFfmpegUseMediaCodecDecode.mIsLocalPlayer = mIsLocalPlayer;
        mFFMPEGPlayer.setContext(mContext);
        mFFMPEGPlayer.setHandler(mUiHandler);
        mFFMPEGPlayer.mIsLocalPlayer = mIsLocalPlayer;
        mFFMPEGPlayer.setFfmpegUseMediaCodecDecode(mFfmpegUseMediaCodecDecode);
        mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_init, null);
        /*if (IS_WATCH) {
            mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_isWatchForCloseAudio, null);
        }*/

        /*if (mGetMediaFormat == null) {
            mGetMediaFormat = new GetMediaFormat();
        }
        mGetMediaFormat.setContext(mContext);
        mGetMediaFormat.setPlayerWrapper(this);*/
        //mFfmpegUseMediaCodecDecode.setGetMediaFormat(mGetMediaFormat);

        /*int duration = (int) mMediaDuration;
        int currentPosition = (int) mPresentationTime;
        float pos = (float) currentPosition / duration;
        int target = Math.round(pos * mPositionSeekBar.getMax());
        mPositionSeekBar.setProgress(target);
        mPositionSeekBar.setSecondaryProgress(0);*/
        mPositionSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.d(TAG, "onStartTrackingTouch");
                if (isFrameByFrameMode) {
                    return;
                }
                mNeedToSyncProgressBar = false;
                mSeekTimeTV.setVisibility(View.VISIBLE);
                mFileNameTV.setVisibility(View.GONE);
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
                if (fromTouch && !isFrameByFrameMode) {
                    // 得到的是秒
                    long tempProgress =
                            (long) ((progress / 100.00) * mMediaDuration);
                    //(long) ((progress / 3840.00) * mMediaDuration);
                    mProgress = tempProgress;
                    if (!mIsH264) {
                        String elapsedTime =
                                DateUtils.formatElapsedTime(tempProgress);
                        mSeekTimeTV.setText(elapsedTime);
                    } else {
                        mSeekTimeTV.setText(String.valueOf(tempProgress));
                    }
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d(TAG, "onStopTrackingTouch");
                if (isFrameByFrameMode) {
                    return;
                }
                mNeedToSyncProgressBar = true;
                mSeekTimeTV.setVisibility(View.GONE);
                mFileNameTV.setVisibility(View.VISIBLE);
                mFileNameTV.requestFocus();
                if (mIsH264) {
                    Log.d(TAG, "onStopTrackingTouch mProgress: " + mProgress);
                } else {
                    Log.d(TAG, "onStopTrackingTouch mProgress: " + mProgress +
                            " " + DateUtils.formatElapsedTime(mProgress));
                }
                if (mProgress >= 0 && mProgress <= mMediaDuration) {
                    if (mWdPlayer != null) {
                        mWdPlayer.seekTo(mProgress);
                    }
                }
            }
        });
        /*mPositionSeekBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (isFrameByFrameMode) {
                            return false;
                        }
                        mNeedToSyncProgressBar = false;
                        mSeekTimeTV.setVisibility(View.VISIBLE);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (isFrameByFrameMode) {
                            return false;
                        }
                        mNeedToSyncProgressBar = true;
                        mSeekTimeTV.setVisibility(View.GONE);
                        if (mIsH264) {
                            Log.d(TAG, "MotionEvent.ACTION_UP mProgress: " + mProgress);
                        } else {
                            Log.d(TAG, "MotionEvent.ACTION_UP mProgress: " + mProgress +
                                    " " + DateUtils.formatElapsedTime(mProgress));
                        }
                        if (mProgress >= 0 && mProgress <= mMediaDuration) {
                            if (mWdPlayer != null) {
                                mWdPlayer.seekTo(mProgress);
                            }
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        break;
                    default:
                        break;
                }
                return false;
            }
        });*/

        mVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mVolumeProgress = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
                if (fromTouch) {
                    mVolumeProgress = progress;
                    MyToast.show(String.valueOf(progress));
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mAudioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        mVolumeProgress,
                        AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                mVolumeLayout.setVisibility(View.INVISIBLE);
            }
        });

        minVolume = 0;
        if (IS_TV) {
            maxVolume = 100;
        } else {
            maxVolume = 15;
        }
        int curVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            minVolume = mAudioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC);
        }
        maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mVolumeSeekBar.setMin(minVolume);
        }
        mVolumeSeekBar.setMax(maxVolume);
        Log.i(TAG, "onCreate() minVolume: " + minVolume);// 0
        Log.i(TAG, "onCreate() maxVolume: " + maxVolume);// 15(Phone) 100(TV)
        Log.i(TAG, "onCreate() curVolume: " + curVolume);

        sendMessageForLoadContents();

        // createAlarmTask();
        // Test
        /*new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    saveLog();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SystemClock.sleep(5000);
                    readLog();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();*/
    }

    // 调用之前,视频路径先设置好
    @SuppressLint("InvalidWakeLockTag")
    private void onResume() {
        Log.i(TAG, "onResume()");
        if (mPowerWakeLock == null) {
            // When player view started,wake the lock.
            PowerManager powerManager =
                    (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            mPowerWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
            mPowerWakeLock.acquire();
        }

        mSurfaceHolder = mSurfaceView.getHolder();
        // 没有图像出来,就是由于没有设置PixelFormat.RGBA_8888
        mSurfaceHolder.setFormat(PixelFormat.RGBA_8888);
        mSurfaceHolder.addCallback(mSurfaceCallback);
    }

    private void onPause() {
        if (mPowerWakeLock != null && mPowerWakeLock.isHeld()) {
            mPowerWakeLock.release();
            mPowerWakeLock = null;
        }
    }

    private void onStop() {

    }

    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        onRelease();
        unregisterReceiver();
        mFFMPEGPlayer = null;
        mIjkPlayer = null;
        mSimpleVideoPlayer = null;

        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }
        Phone.unregister(this);
    }

    private void onRelease() {
        Log.i(TAG, "onRelease() whatPlayer: " + whatPlayer);
        if (TextUtils.equals(whatPlayer, PLAYER_FFPLAY)) {
            mUiHandler.removeMessages(MSG_RELEASE);
            mUiHandler.sendEmptyMessage(MSG_RELEASE);
        } else {
            if (mWdPlayer != null/* && mWdPlayer.isRunning()*/) {
                Log.i(TAG, "onRelease()");
                mWdPlayer.release();
            }
        }
    }

    private void addView() {
        Log.i(TAG, "addView()    mIsAddedView: " + mIsAddedView);
        if (mIsAddedView) {
            mIsAddedView = false;
            onPause();
            // addView() ---> removeView(...) ---> onRelease() ---> onFinished() --->
            // needToPlaybackOtherVideo() ---> addView()
            mWindowManager.removeView(mRootView);
            return;
        }

        mIsAddedView = true;
        onResume();
        getMD5ForPath();
        mWindowManager.addView(mRootView, mLayoutParams);
    }

    public void removeView(boolean needToRemoveCallback) {
        Log.i(TAG, "removeView() mIsAddedView: " + mIsAddedView);
        Log.d(TAG, "removeView() mPrePath = null");
        mPrePath = null;
        if (mIsAddedView) {
            mIsAddedView = false;
            // removeView(false) ---> removeView(...) ---> onRelease() ---> onFinished() --->
            // removeView(true)
            Log.i(TAG, "removeView()");
            mWindowManager.removeView(mRootView);
        }
        if (needToRemoveCallback) {
            onPause();
            removeCallback();
            abandonAudioFocusRequest();
            stopForeground();
            System.gc();
            Log.i(TAG, "removeView() System.gc()");
        }
    }

    public void createAlarmTask() {
        long anHour = 30 * 1000;
        long triggerAtTime = SystemClock.elapsedRealtime() + anHour;
        Intent intent = new Intent(mContext, JniPlayerActivity.class);
        //intent.setData(Uri.parse("/storage/37C8-3904/myfiles/music/冷漠、云菲菲\\ -\\ 伤心城市.mp3"));
        //intent.setType("audio/");
        intent.putExtra(CONTENT_PATH, "/storage/37C8-3904/myfiles/music/冷漠、云菲菲 - 伤心城市.mp3");
        intent.putExtra(CONTENT_PATH, "/storage/emulated/0/Music/谭咏麟 - 水中花.mp3");
        intent.putExtra(CONTENT_TYPE, "audio/");
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
        mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pendingIntent);
    }

    private ArrayList<Integer> mColorsHasUsedList;
    private static final int[] COLORS = new int[]{
            R.color.lightgray,// 原来的颜色
            R.color.aqua,
            R.color.deepskyblue,
            R.color.maroon,
            R.color.olivedrab,
            R.color.lightgreen,
            R.color.saddlebrown,
            R.color.lightsalmon,
            R.color.gold,
            R.color.lightpink,
            R.color.result_view,
            R.color.salmon,
            R.color.violet,
            R.color.pink,
            R.color.hotpink,
            R.color.palevioletred,
            R.color.navajowhite,
            R.color.wheat,
            R.color.lavender,
            R.color.darkseagreen,
            R.color.darkslateblue,
            R.color.palegoldenrod,
            R.color.khaki,
            R.color.darkkhaki,
            R.color.indianred,
            R.color.powderblue,
            R.color.palegreen,
            R.color.olive,

            R.color.holo_green_light,
            R.color.holo_red_light,
            R.color.holo_blue_dark,
            R.color.holo_green_dark,
            R.color.holo_red_dark,
            R.color.holo_orange_light,
            R.color.holo_orange_dark,
            R.color.holo_blue_bright,
            R.color.holo_gray_bright,
            R.color.group_button_dialog_focused_holo_light,
            R.color.group_button_dialog_pressed_holo_dark,
            R.color.highlighted_text_holo_light,
            R.color.link_text_holo_dark,
            R.color.material_deep_teal_100,
            R.color.material_deep_teal_200,
            R.color.material_deep_teal_300,
            R.color.material_deep_teal_500,
            R.color.car_purple_50,
            R.color.car_purple_100,
            R.color.car_purple_200,
            R.color.car_purple_300,
            R.color.car_purple_400,
            R.color.car_purple_500,
            R.color.car_purple_600,
            R.color.car_purple_700,
            R.color.car_purple_800,
            R.color.car_purple_900,
    };

    private void setControllerPanelBackgroundColor() {
        // Log.i(TAG, "setControllerPanelBackgroundColor()");
        if (!mIsAddedView) {
            Log.i(TAG, "setControllerPanelBackgroundColor() return");
            return;
        }

        if (mColorsHasUsedList == null) {
            mColorsHasUsedList = new ArrayList<>();
        }

        int length = COLORS.length;
        int targetColor = -1;
        for (; ; ) {
            int randomNumber = mRandom.nextInt(length);
            if (!mColorsHasUsedList.contains(randomNumber)) {
                mColorsHasUsedList.add(randomNumber);
                int index = -1;
                for (int color : COLORS) {
                    if (++index == randomNumber) {
                        targetColor = color;
                        break;
                    }
                }
                break;
            } else {
                if (mColorsHasUsedList.size() >= length) {
                    mColorsHasUsedList.clear();
                }
            }
        }

        if (targetColor < 0) {
            return;
        }

        int orientation = mContext.getResources().getConfiguration().orientation;
        /*int height = getStatusBarHeight() + getNavigationBarHeight();
        if ((orientation == Configuration.ORIENTATION_PORTRAIT
                && (mNeedVideoHeight + mControllerPanelLayoutHeight) <= (mScreenHeight - height))
                || (orientation == Configuration.ORIENTATION_LANDSCAPE && handleScreenFlag == 1
                && !IS_TV)
                || mIsAudio) {
            mControllerPanelLayout.setBackgroundColor(
                    ContextCompat.getColor(mContext, targetColor));
        } else {
            mControllerPanelLayout.setBackgroundColor(
                    ContextCompat.getColor(mContext, android.R.color.transparent));
        }*/
        if (mIsAudio) {
            mControllerPanelLayout.setBackgroundColor(
                    ContextCompat.getColor(mContext, targetColor));
        }
        if (mIsVideo) {
            if (textInfoTV.getVisibility() == View.VISIBLE) {
                textInfoTV.setTextColor(
                        ContextCompat.getColor(mContext, targetColor));
                StringBuilder sb = new StringBuilder();
                if (!TextUtils.isEmpty(textInfo)) {
                    sb.append(textInfo);
                    sb.append("\n");
                } else {
                    if (IS_WATCH && orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        sb.append("    ");
                    }
                }

                if (!IS_WATCH) {
                    if (mIsLocalPlayer) {
                        sb.append("[1] "); // 第1个播放窗口
                    } else {
                        sb.append("[2] "); // 第2个播放窗口
                    }
                }

                sb.append("[");
                sb.append(mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY));
                sb.append("]");
                if (mIsLive) {
                    sb.append(" [");
                    sb.append(DateUtils.formatElapsedTime(mPresentationTime));
                    sb.append("]");
                }
                textInfoTV.setText(sb.toString());
            }
        }
        if (!IS_WATCH) {
            ObjectAnimator controllerPanelAnimator =
                    ObjectAnimator.ofFloat(mControllerPanelLayout, "alpha", 0.5f, 1.0f);
            ObjectAnimator textInfoAnimator =
                    ObjectAnimator.ofFloat(textInfoTV, "alpha", 0.5f, 1.0f);
            /*if (mIsLocal) {
                controllerPanelAnimator.setDuration(5000);
                textInfoAnimator.setDuration(5000);
            } else {
                controllerPanelAnimator.setDuration(8000);
                textInfoAnimator.setDuration(8000);
            }*/
            if (mIsAudio) {
                controllerPanelAnimator.setDuration(5000);
                textInfoAnimator.setDuration(5000);
            }
            controllerPanelAnimator.setDuration(1000);
            textInfoAnimator.setDuration(1000);
            controllerPanelAnimator.start();
            textInfoAnimator.start();
        }

        mUiHandler.removeMessages(MSG_CHANGE_COLOR);
        mUiHandler.sendEmptyMessageDelayed(MSG_CHANGE_COLOR, 60 * 1000);
    }

    private void getMD5ForPath() {
        md5Path = md5(mCurPath);
        String newPath = mCurPath.toLowerCase();
        if (newPath.startsWith("/storage/")) {
            mIsLocal = true;
            try {
                if (!TextUtils.isEmpty(mLocalVideoPath) && mCurPath.startsWith(mLocalVideoPath)) {
                    if (!mLocalContentsHasPlayedVideoPathList.contains(mCurPath)) {
                        mLocalContentsHasPlayedVideoPathList.add(mCurPath);
                    }
                } else if (!TextUtils.isEmpty(mLocalAudioPath) && mCurPath.startsWith(mLocalAudioPath)) {
                    if (!mLocalContentsHasPlayedAudioPathList.contains(mCurPath)) {
                        mLocalContentsHasPlayedAudioPathList.add(mCurPath);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            mIsLocal = false;
        }
        if (newPath.endsWith(".h264")) {
            mIsH264 = true;
        } else {
            mIsH264 = false;
        }
        Log.i(TAG, "getMD5ForPath()  mIsLocal: " + mIsLocal);
        Log.i(TAG, "getMD5ForPath()   mIsH264: " + mIsH264);
    }

    private boolean allowToPlayback() {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatusIntent = mContext.registerReceiver(null, intentFilter);

        int status = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging =
                (status == BatteryManager.BATTERY_STATUS_CHARGING)
                        || (status == BatteryManager.BATTERY_STATUS_FULL);
        Log.i(TAG, "allowToPlayback() 设备是否正在充电: " + (isCharging ? "是" : "否"));

        // 手机正在充电
        if (isCharging) {
            int plugged = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            boolean usbPlugged = (plugged == BatteryManager.BATTERY_PLUGGED_USB);
            boolean acPlugged = (plugged == BatteryManager.BATTERY_PLUGGED_AC);
            Log.i(TAG, "allowToPlayback() usbPlugged: " + usbPlugged);
            Log.i(TAG, "allowToPlayback()  acPlugged: " + acPlugged);
            if (usbPlugged) {
                // 手机正处于USB连接
            } else if (acPlugged) {
                // 手机通过电源充电中
            }
            return true;
        }

        // 手机没在充电
        try {
            int battery = mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            Log.i(TAG, "allowToPlayback()    battery: " + battery);
            if (battery <= 15) {
                Log.e(TAG, "allowToPlayback() 电量过低,不允许再循环播放!!!");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    @SuppressLint("MissingPermission")
    private boolean needToPlaybackOtherVideo() {
        Log.i(TAG, "needToPlaybackOtherVideo()");
        if (!mIsLocal && !NetworkUtils.isConnected(mContext)) {
            Log.i(TAG, "needToPlaybackOtherVideo() return false" +
                    " for network doesn't connect");
            return false;
        }

        if (mPrePath != null) {
            Log.i(TAG, "needToPlaybackOtherVideo() return true" +
                    " for mPrePath != null");
            removeCallback();
            mUiHandler.removeMessages(MSG_ADD_VIEW);
            mUiHandler.sendEmptyMessage(MSG_ADD_VIEW);
            return true;
        }

        if (!mIsAddedView || !mRootView.isShown()) {
            Log.i(TAG, "needToPlaybackOtherVideo() return false" +
                    " for mRootView isn't showed");
            mUiHandler.removeMessages(MSG_CHANGE_COLOR);
            return false;
        }

        if (!allowToPlayback()) {
            Log.i(TAG, "needToPlaybackOtherVideo() return false" +
                    " for doesn't allowToPlayback");
            return false;
        }

        if (mRepeat == Repeat.Repeat_Off) {
            Log.i(TAG, "needToPlaybackOtherVideo() return false" +
                    " for mRepeat == Repeat.Repeat_Off");
            return false;
        }

        if (mRepeat == Repeat.Repeat_One) {
            startForGetMediaFormat();
            return true;
        }

        LinkedHashMap<String, String> map = null;
        ArrayList<Integer> indexList = null;
        ArrayList<String> pathList = null;
        if (mIsVideo) {
            map = mLocalVideoContentsMap;
            indexList = mLocalContentsHasPlayedVideoIndexList;
            pathList = mLocalContentsHasPlayedVideoPathList;
        } else if (mIsAudio) {
            map = mLocalAudioContentsMap;
            indexList = mLocalContentsHasPlayedAudioIndexList;
            pathList = mLocalContentsHasPlayedAudioPathList;
        }
        if (map == null) {
            return false;
        }

        // region mShuffle == Shuffle.Shuffle_Off(顺序播放)
        if (mShuffle == Shuffle.Shuffle_Off) {
            if (!mPlayPrevFile && !mPlayNextFile) {
                mPlayNextFile = true;
            }
            int index = -1;
            int prevPathIndex = -2;
            int nextPathIndex = -2;
            String prevPath = null;
            for (Map.Entry<String, String> tempMap : map.entrySet()) {
                index++;
                if (TextUtils.equals(tempMap.getKey(), mCurPath)) {
                    if (mPlayPrevFile) {
                        if (index == 0) {
                            prevPathIndex = map.size() - 1;
                        }
                        break;
                    } else if (mPlayNextFile) {
                        nextPathIndex = index;
                        if (nextPathIndex == map.size() - 1) {
                            // 刚刚播放完的是最后一个文件,接下去就是播放Map中的第一个文件
                            index = 0;
                            break;
                        }
                    }
                } else {
                    prevPath = tempMap.getKey();
                }
                if (index == nextPathIndex + 1) {
                    mPrePath = mCurPath;
                    mCurPath = tempMap.getKey();
                    getMD5ForPath();
                    break;
                }
            }
            if (mPlayPrevFile) {
                if (index == 0) {
                    // 播放最后一个文件
                    for (Map.Entry<String, String> tempMap : map.entrySet()) {
                        mPrePath = mCurPath;
                        mCurPath = tempMap.getKey();
                        if (index++ == prevPathIndex) {
                            break;
                        }
                    }
                } else {
                    mPrePath = mCurPath;
                    mCurPath = prevPath;
                }
                getMD5ForPath();
            } else if (mPlayNextFile) {
                if (index == 0) {
                    // 播放第一个文件
                    for (Map.Entry<String, String> tempMap : map.entrySet()) {
                        mPrePath = mCurPath;
                        mCurPath = tempMap.getKey();
                        getMD5ForPath();
                        if (++index == 1) {
                            break;
                        }
                    }
                }
            }
            mPlayPrevFile = false;
            mPlayNextFile = false;
            startForGetMediaFormat();
            return true;
        }
        // endregion

        // region mShuffle == Shuffle.Shuffle_On (随机播放)
        int size = map.size();
        for (; ; ) {
            int randomNumber = mRandom.nextInt(size);
            if (!indexList.contains(randomNumber)) {
                indexList.add(randomNumber);
                int index = -1;
                boolean needBreak = false;
                for (Map.Entry<String, String> tempMap : map.entrySet()) {
                    if (++index == randomNumber) {
                        mPrePath = mCurPath;
                        mCurPath = tempMap.getKey();
                        if (!pathList.contains(mCurPath)) {
                            pathList.add(mCurPath);
                            getMD5ForPath();
                            needBreak = true;
                        }
                        break;
                    }
                }
                if (needBreak) {
                    break;
                }
            } else {
                if (indexList.size() >= size) {
                    indexList.clear();
                    pathList.clear();
                }
            }
        }
        startForGetMediaFormat();
        return true;
        // endregion

        /*Log.i(TAG, "needToPlaybackOtherVideo() return false");
        // 不需要播放另一个视频
        return false;*/
    }

    private boolean allowExit() {
        if (TextUtils.equals(whatPlayer, PLAYER_IJKPLAYER)) {
        } else if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
        } else {
            String allowExitStr = sendEmptyMessage(DO_SOMETHING_CODE_allow_exit);
            if (!TextUtils.isEmpty(allowExitStr) && !Boolean.parseBoolean(allowExitStr)) {
                return false;
            }
        }
        return true;
    }

    private void uiHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        switch (msg.what) {
            case Callback.MSG_ON_TRANSACT_VIDEO_PRODUCER:// 生产者
                mVideoProgressBar.setSecondaryProgress(((JniObject) msg.obj).valueInt);
                break;
            case Callback.MSG_ON_TRANSACT_VIDEO_CONSUMER:// 消费者
                mVideoProgressBar.setProgress(((JniObject) msg.obj).valueInt);
                break;
            case Callback.MSG_ON_TRANSACT_AUDIO_PRODUCER:// 生产者
                mAudioProgressBar.setSecondaryProgress(((JniObject) msg.obj).valueInt);
                break;
            case Callback.MSG_ON_TRANSACT_AUDIO_CONSUMER:// 消费者
                mAudioProgressBar.setProgress(((JniObject) msg.obj).valueInt);
                break;
            case Callback.MSG_ON_TRANSACT_READY:
                Log.d(TAG, "Callback.MSG_ON_TRANSACT_READY");
                onReady();
                break;
            case Callback.MSG_ON_TRANSACT_CHANGE_WINDOW:
                Log.d(TAG, "Callback.MSG_ON_TRANSACT_CHANGE_WINDOW");
                onChangeWindow(msg);
                break;
            case Callback.MSG_ON_TRANSACT_PLAYED:
                Log.d(TAG, "Callback.MSG_ON_TRANSACT_PLAYED");
                onPlayed();
                break;
            case Callback.MSG_ON_TRANSACT_PAUSED:
                Log.d(TAG, "Callback.MSG_ON_TRANSACT_PAUSED");
                onPaused();
                break;
            case Callback.MSG_ON_TRANSACT_FINISHED:
                Log.d(TAG, "Callback.MSG_ON_TRANSACT_FINISHED");
                onFinished();
                break;
            case Callback.MSG_ON_TRANSACT_INFO:
                //Log.d(TAG, "Callback.MSG_ON_TRANSACT_INFO");
                inInfo(msg);
                break;
            case Callback.MSG_ON_TRANSACT_ERROR:
                Log.d(TAG, "Callback.MSG_ON_TRANSACT_ERROR");
                onError(msg);
                break;
            case Callback.MSG_ON_TRANSACT_PROGRESS_UPDATED:
                onUpdated(msg);
                break;
            case KeyEvent.KEYCODE_HEADSETHOOK:// 79 单击事件
                if (clickCounts > NEED_CLICK_COUNTS) {
                    clickCounts = NEED_CLICK_COUNTS;
                }

                switch (clickCounts) {
                    case 1:
                        Log.d(TAG, "clickOne()");
                        clickOne();
                        break;
                    case 2:
                        Log.d(TAG, "clickTwo()");
                        clickTwo();
                        break;
                    case 3:
                        Log.d(TAG, "clickThree()");
                        clickThree();
                        break;
                    case 4:
                        Log.d(TAG, "clickFour()");
                        clickFour();
                        break;
                    case 5:
                        Log.d(TAG, "clickFive()");
                        clickFive();
                        break;
                    case 6:
                        Log.d(TAG, "clickSix()");
                        clickSix();
                        break;
                    case 7:
                        Log.d(TAG, "clickSeven()");
                        clickSeven();
                        break;
                    case 8:
                        Log.d(TAG, "clickEight()");
                        clickEight();
                        break;
                    case 9:
                        Log.d(TAG, "clickNine()");
                        clickNine();
                        break;
                    case 10:
                        Log.d(TAG, "clickTen()");
                        clickTen();
                        break;
                    case 11:
                        Log.d(TAG, "clickEleven()");
                        clickEleven();
                        break;
                    default:
                        break;
                }

                clickCounts = 0;
                break;
            case MSG_CHANGE_COLOR:
                setControllerPanelBackgroundColor();
                break;
            case MSG_START_PLAYBACK:
                break;
            case MSG_SEEK_TO_ADD:
                if (mWdPlayer != null) {
                    if (TextUtils.equals(whatPlayer, PLAYER_FFPLAY)) {
                        long count = 0;
                        if (mMediaDuration > 300) {
                            count = addStep / 30;
                        } else {
                            count = addStep / 10;
                        }
                        mWdPlayer.onTransact(
                                DO_SOMETHING_CODE_stepAdd,
                                JniObject.obtain().writeLong(count));
                    } else {
                        mWdPlayer.seekTo(mPresentationTime + addStep);
                    }
                }
                addStep = 0;
                break;
            case MSG_SEEK_TO_SUBTRACT:
                if (mWdPlayer != null) {
                    if (TextUtils.equals(whatPlayer, PLAYER_FFPLAY)) {
                        long count = 0;
                        if (mMediaDuration > 300) {
                            count = subtractStep / 30;
                        } else {
                            count = subtractStep / 10;
                        }
                        mWdPlayer.onTransact(
                                DO_SOMETHING_CODE_stepSubtract,
                                JniObject.obtain().writeLong(count));
                    } else {
                        mWdPlayer.seekTo(mPresentationTime - subtractStep);
                    }
                }
                subtractStep = 0;
                break;
            case MSG_VOLUME_SEEK_TO_ADD:
            case MSG_VOLUME_SEEK_TO_SUBTRACT:
                mAudioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        volumeStep,
                        AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                volumeStep = 0;
                mVolumeLayout.setVisibility(View.INVISIBLE);
                break;
            case MSG_VOLUME_HIDE_LAYOUT:
                mVolumeLayout.setVisibility(View.INVISIBLE);
                break;
            case MSG_ADD_VIEW:
                addView();
                break;
            case MSG_SCREEN_BRIGHT_WAKE_LOCK:
                wakeUpAndUnlock();
                break;
            case MSG_RELEASE: {
                if (mWdPlayer != null) {
                    Log.i(TAG, "onRelease()");
                    mWdPlayer.release();
                }
                break;
            }
            default:
                break;
        }
    }

    private void threadHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        switch (msg.what) {
            case MSG_PREPARE:
                if (IS_WATCH) {
                    whatPlayer = mSP.getString(PLAYBACK_USE_PLAYER, PLAYER_IJKPLAYER);
                } else {
                    whatPlayer = mSP.getString(PLAYBACK_USE_PLAYER, PLAYER_FFPLAY);
                }
                /*if (TextUtils.equals(whatPlayer, PLAYER_FFMPEG_MEDIACODEC)
                        && !mPath.endsWith(".m4s")
                        && !mPath.endsWith(".h264")
                        && !mPath.endsWith(".aac")
                        //&& mIsLocal
                        && mIsVideo) {
                    onReady();
                    mGetMediaFormat.start(mPath);
                    return;
                }*/
                startPlayback();
                break;
            case MSG_DOWNLOAD:
                if (mDownloadClickCounts > 5) {
                    mDownloadClickCounts = 5;
                }
                Log.d(TAG, "threadHandleMessage() mDownloadClickCounts: " +
                        mDownloadClickCounts);

                // 点击次数
                switch (mDownloadClickCounts) {
                    case 1:
                        if (!mIsDownloading) {
                            break;
                        }
                        mIsDownloading = false;
                        mUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                // 这个操作在ThreadHandler中没有发生问题
                                mDownloadTV.setText("1");
                            }
                        });
                        // 停止下载
                        mWdPlayer.onTransact(
                                DO_SOMETHING_CODE_download,
                                JniObject.obtain()
                                        .writeInt(1).writeStringArray(new String[]{"", ""}));
                        break;
                    case 2:
                    case 3:
                    case 4:
                        if (mIsDownloading) {
                            break;
                        }
                        mIsDownloading = true;
                        String path =
                                "/storage/1532-48AD/Android/data/" +
                                        "com.weidi.usefragments/files/Movies/";
                        String title;
                        if (mIsLocal) {
                            title = mCurPath.substring(
                                    mCurPath.lastIndexOf("/") + 1, mCurPath.lastIndexOf("."));
                        } else {
                            title = mContentsMap.get(mCurPath);
                        }
                        StringBuilder sb = new StringBuilder();
                        if (TextUtils.isEmpty(title)) {
                            sb.append("media-");
                        } else {
                            sb.append(title);
                            sb.append("-");
                        }
                        sb.append(mSimpleDateFormat.format(new Date()));
                        // 保存路径 文件名
                        if (mDownloadClickCounts == 2) {
                            mUiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mDownloadTV.setText("2");
                                }
                            });
                            // 开始下载,边下边播
                            mWdPlayer.onTransact(
                                    DO_SOMETHING_CODE_download,
                                    JniObject.obtain()
                                            .writeInt(0)
                                            .writeStringArray(new String[]{path, sb.toString()}));
                        } else {
                            mUiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (mDownloadClickCounts == 3) {
                                        mDownloadTV.setText("3");
                                    } else {
                                        mDownloadTV.setText("4");
                                    }
                                    if (mIsVideo) {
                                        mVideoWidth = 0;
                                        mVideoHeight = 0;
                                        handlePortraitScreen();
                                    }
                                }
                            });
                            if (mDownloadClickCounts == 3) {
                                // 只下载,不播放.不调用seekTo
                                mWdPlayer.onTransact(
                                        DO_SOMETHING_CODE_download,
                                        JniObject.obtain()
                                                .writeInt(4)
                                                .writeStringArray(
                                                        new String[]{path, sb.toString()}));
                            } else {
                                // 只提取音视频,不播放.调用seekTo到0
                                mWdPlayer.onTransact(
                                        DO_SOMETHING_CODE_download,
                                        JniObject.obtain()
                                                .writeInt(5)
                                                .writeStringArray(
                                                        new String[]{path, sb.toString()}));
                            }
                        }
                        break;
                    case 5:
                        mUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mDownloadTV.setText("");
                                mDownloadTV.setBackgroundColor(
                                        mContext.getResources().getColor(R.color.transparent));
                            }
                        });
                        break;
                    default:
                        break;
                }

                mDownloadClickCounts = 0;
                break;
            case MSG_LOAD_CONTENTS:
                if (mIsLocalPlayer) {
                    loadContents();
                }
                break;
            /*case MSG_RELEASE: {
                Log.i(TAG, "onRelease()");
                if (mWdPlayer != null) {
                    mWdPlayer.release();
                }
                break;
            }*/
            case MSG_TEST_SIGNAL: {
                testSignal();
                break;
            }
            case BUTTON_CLICK_TEST_STOP: {
                stopTest();
                break;
            }
            default:
                break;
        }
    }

    private void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_fr:
                buttonClickForFr();
                break;
            case R.id.button_ff:
                buttonClickForFf();
                break;
            case R.id.button_prev:
                buttonClickForPrev();
                break;
            case R.id.button_next:
                buttonClickForNext();
                break;
            case R.id.button_play:
                buttonClickForPause();
                break;
            case R.id.button_pause:
                buttonClickForPlay();
                break;
            case R.id.surfaceView:
                mIsScreenPress = true;
                onEvent(KeyEvent.KEYCODE_HEADSETHOOK, null);
                break;
            case R.id.button_exit:
                buttonClickForExit();
                break;
            case R.id.volume_normal:
            case R.id.volume_mute:
                buttonClickForVolume();
                break;
            case R.id.screen_max: {
                if (mWindow == Window.Full_Screen) {
                    Phone.call(FullScreenActivity.class.getName(),
                            Constants.FINISH_FULL_SCREEN_ACTIVITY, null);
                    break;
                }
                switch (mWindow) {
                    case Max_Screen: {
                        handlePortraitScreenWithTV();
                        break;
                    }
                    case Min_Screen: {
                        handlePortraitScreen();
                        break;
                    }
                    default:
                        break;
                }
                break;
            }
            case R.id.button_repeat_off:
                buttonClickForRepeatOff();
                break;
            case R.id.button_repeat_all:
                buttonClickForRepeatAll();
                break;
            case R.id.button_repeat_one:
                buttonClickForRepeatOne();
                break;
            case R.id.button_shuffle_off:
                buttonClickForShuffleOff();
                break;
            case R.id.button_shuffle_on:
                buttonClickForShuffleOn();
                break;
            case R.id.download_tv:
                buttonClickForDownload();
                break;
            case R.id.button_volume_min:
                buttonClickForVolumeMin();
                break;
            case R.id.button_volume_max:
                buttonClickForVolumeMax();
                break;
            default:
                break;
        }
    }

    // @@@
    private void startForGetMediaFormat() {
        mThreadHandler.removeMessages(MSG_PREPARE);
        mThreadHandler.sendEmptyMessage(MSG_PREPARE);
        //mThreadHandler.removeMessages(MSG_PREPARE);
        //mThreadHandler.sendEmptyMessageDelayed(MSG_PREPARE, 3000);
    }

    private void startPlayback() {
        Log.d(TAG, "startPlayback() start");
        Log.d(TAG, "startPlayback()                  mPath: " + mCurPath);
        if (!mIsAddedView
                || !mRootView.isShown()
                || TextUtils.isEmpty(mCurPath)) {
            Log.e(TAG, "startPlayback() The condition is not satisfied");
            return;
        }

        textInfo = null;

        if (TextUtils.equals(whatPlayer, PLAYER_IJKPLAYER)) {
            if (mIjkPlayer == null) {
                mIjkPlayer = new IjkPlayer();
            }
            mIjkPlayer.setContext(mContext);
            mIjkPlayer.setCallback(mFFMPEGPlayer.mCallback);
            mIjkPlayer.mIsLocalPlayer = mIsLocalPlayer;
            mIjkPlayer.mIsLocal = mIsLocal;
            mWdPlayer = mIjkPlayer;
        } else if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
            //mSimpleVideoPlayer = new SimpleVideoPlayer();
            //mWdPlayer = mSimpleVideoPlayer;
        } else {
            mWdPlayer = mFFMPEGPlayer;
            mFFMPEGPlayer.whatPlayer = whatPlayer;
            mFFMPEGPlayer.setType(mType);
        }

        long position = 0;
        if (mPathTimeMap.containsKey(md5Path)) {
            // seekTo
            position = mPathTimeMap.get(md5Path);
            Log.d(TAG, "startPlayback()               position: " + position);
        }

        mWdPlayer.seekTo(position);
        mWdPlayer.setDataSource(mCurPath);
        mWdPlayer.setSurface(mSurfaceHolder.getSurface());
        if (!mWdPlayer.prepareSync()) {
            Log.e(TAG, "startPlayback() prepareSync failed");
            if (mIsTesting) {
                mHasTestError = true;
                mThreadHandler.removeMessages(MSG_TEST_SIGNAL);
                mThreadHandler.sendEmptyMessageDelayed(MSG_TEST_SIGNAL, 5000);
            }
            return;
        }

        boolean needTwoPlayer = mSP.getBoolean(NEED_TWO_PLAYER, false);
        if (mPlayerService != null && !needTwoPlayer) {
            requestAudioFocus();
        }
        // startForeground();

        mWdPlayer.start();

        if (mIsTesting) {
            mThreadHandler.removeMessages(MSG_TEST_SIGNAL);
            mThreadHandler.sendEmptyMessageDelayed(MSG_TEST_SIGNAL, 5000);
        }
        Log.d(TAG, "startPlayback() end");
    }

    private void requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (mAudioFocusRequest != null) {
                mAudioManager.abandonAudioFocusRequest(mAudioFocusRequest);
                mAudioFocusRequest = null;
            }
            /***
             AUDIOFOCUS_LOSS：当本程序正在播放音频时有另一播放器请求获得音频焦点播放音频，那么就会回调该方法并传入此参数
             AUDIOFOCUS_LOSS_TRANSIENT：当另一个播放器请求“短暂”获得音频焦点，传入此参数
             AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK：当另一播放器请求“短暂”获得音频焦点且不用完全暂停可以让对方降低音量时传入此参数
             AUDIOFOCUS_GAIN：当其他播放器正在播放音频时，本程序请求获得音频焦点播放音频，传入此参数
             AUDIOFOCUS_GAIN_TRANSIENT：本程序请求“短暂”获取音频焦点时传入此参数
             AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK：本程序请求“短暂”音频焦点且可能会将低对方音量时传入此参数
             */
            AudioAttributes audioAttributes =
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build();
            mAudioFocusRequest =
                    new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                            // 有短暂的音频（如短信）来了，这时音乐的声音就会降低（但不会暂停）以此凸显出短信提示音，这是由系统自己自动处理的
                            .setWillPauseWhenDucked(false)
                            .setAcceptsDelayedFocusGain(true)// ---> AUDIOFOCUS_REQUEST_DELAYED
                            .setAudioAttributes(audioAttributes)
                            .setOnAudioFocusChangeListener(AudioFocusChangeListener)
                            .build();
            int focusRequest = mAudioManager.requestAudioFocus(mAudioFocusRequest);
            switch (focusRequest) {
                case AudioManager.AUDIOFOCUS_REQUEST_FAILED:
                    // 不允许播放
                    Log.i(TAG, "requestAudioFocus() AudioManager.AUDIOFOCUS_REQUEST_FAILED");
                    break;
                case AudioManager.AUDIOFOCUS_REQUEST_GRANTED:
                    // 开始播放
                    Log.i(TAG, "requestAudioFocus() AudioManager.AUDIOFOCUS_REQUEST_GRANTED");
                    break;
                case AudioManager.AUDIOFOCUS_REQUEST_DELAYED:
                    Log.i(TAG, "requestAudioFocus() AudioManager.AUDIOFOCUS_REQUEST_DELAYED");
                    break;
                default:
                    break;
            }
        }
    }

    private void abandonAudioFocusRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && mAudioFocusRequest != null) {
            Log.i(TAG, "abandonAudioFocusRequest()");
            mAudioManager.abandonAudioFocusRequest(mAudioFocusRequest);
            mAudioFocusRequest = null;
        }
    }

    private Notification mNotification;

    private void startForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (mNotification == null) {
                mNotification = NotificationUtil.createNotification(
                        mContext,
                        R.drawable.appicon,
                        mContext.getString(R.string.app_name),
                        "多媒体播放",
                        "com.weidi.media.wdplayer",
                        "com.weidi.media.wdplayer.video_player.PlayerWrapper");
                NotificationUtil.showNotification(mContext, mNotification);
            }
        }
    }

    private void stopForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (mNotification != null) {
                NotificationUtil.cancelNotification(mContext);
                mNotification = null;
            }
        }
    }

    private void removeCallback() {
        // 视频在播放过程中如果removeCallback(...)的话,会发生异常.所以只有在结束时removeCallback(...)
        // drainOutputBuffer() Video Output occur exception: java.lang.IllegalStateException
        if (mSurfaceHolder != null) {
            Log.i(TAG, "removeCallback()");
            mSurfaceHolder.removeCallback(mSurfaceCallback);
            mSurfaceHolder = null;
        }
    }

    // 执行全屏和取消全屏的方法
    private void setFullscreen(Activity context, boolean fullscreen) {
        android.view.Window window = context.getWindow();
        WindowManager.LayoutParams winParams = window.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        if (fullscreen) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        window.setAttributes(winParams);
    }

    // 状态栏高度
    private int getStatusBarHeight() {
        int height = 0;
        Resources resources = mContext.getResources();
        int resourceId = resources.getIdentifier(
                "status_bar_height",
                "dimen",
                "android");
        if (resourceId > 0) {
            height = resources.getDimensionPixelSize(resourceId);
        }
        // getStatusBarHeight() height: 48 63 95
        // Log.d(TAG, "getStatusBarHeight() height: " + height);
        return height;
    }

    // 虚拟导航栏高度
    private int getNavigationBarHeight() {
        int height = 0;
        Resources resources = mContext.getResources();
        int resourceId = resources.getIdentifier(
                "navigation_bar_height",
                "dimen",
                "android");
        if (resourceId > 0) {
            height = resources.getDimensionPixelSize(resourceId);
        }
        // Log.d(TAG, "getNavigationBarHeight() height: " + height);
        return height;
    }

    private void test() {
        /*RotateAnimation rotate = new RotateAnimation(
                0, 90f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setInterpolator(new LinearInterpolator());
        rotate.setDuration(5 * 1000);
        rotate.setFillAfter(true);
        mMediaPlayerRootLayout.setAnimation(rotate);
        rotate.start();*/

        ValueAnimator animator = ValueAnimator.ofFloat(0, 90);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Float value = (Float) animation.getAnimatedValue();
                mMediaPlayerRootLayout.setRotation(value);
            }
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mMediaPlayerRootLayout.layout(0, 0, 2244, 1080);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animator.setInterpolator(new LinearInterpolator());
        animator.setDuration(5000);
        animator.start();

        //mMediaPlayerRootLayout.animate().rotation(90).setDuration(5000).start();

        /*ObjectAnimator.ofFloat(mMediaPlayerRootLayout, "rotation", 0f, 180f)
                .setDuration(5000)
                .start();*/

        // 能够旋转,但不是原来的宽高了,点击事件还在原来位置上
        /*ObjectAnimator anim = ObjectAnimator.ofFloat(mMediaPlayerRootLayout, "rotation", 0, 90);
        anim.setInterpolator(new LinearInterpolator());
        anim.setDuration(5000);
        anim.start();*/
    }

    // 处理横屏
    @SuppressLint("SourceLockedOrientationActivity")
    public void handleLandscapeScreen(int statusBarHeight) {
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }*/
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW handleLandscapeScreen");
        if (!mIsAddedView) {
            return;
        }

        if (statusBarHeight == 0) {
            handleScreenFlag = 2;
        } else if (statusBarHeight == 1) {
            handleScreenFlag = 3;
        }

        mWindow = Window.Full_Screen;
        mIsPortraitScreen = false;
        mRootView.setBackgroundColor(
                mContext.getResources().getColor(R.color.black));
        mControllerPanelLayout.setBackgroundColor(
                ContextCompat.getColor(mContext, android.R.color.transparent));

        // 暂停按钮高度
        getPauseRlHeight2();

        if (statusBarHeight != 0) {
            statusBarHeight = getStatusBarHeight();
        }
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW              statusBarHeight: " +
                statusBarHeight);

        // mScreenWidth: 2149 mScreenHeight: 1080
        // 屏幕宽高
        DisplayMetrics displayMetrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW                 mScreenWidth: " +
                mScreenWidth + " mScreenHeight: " + mScreenHeight);

        if (mVideoWidth <= mScreenWidth && mVideoHeight <= mScreenHeight) {
            mNeedVideoWidth = (mScreenHeight * mVideoWidth) / mVideoHeight;
            mNeedVideoHeight = mScreenHeight;
        } else if (mVideoWidth > mScreenWidth && mVideoHeight <= mScreenHeight) {
            mNeedVideoWidth = mScreenWidth;
            mNeedVideoHeight = (mScreenWidth * mVideoHeight) / mVideoWidth;
        } else if (mVideoWidth <= mScreenWidth && mVideoHeight > mScreenHeight) {
            mNeedVideoWidth = (mScreenHeight * mVideoWidth) / mVideoHeight;
            mNeedVideoHeight = mScreenHeight;
        } else if (mVideoWidth > mScreenWidth && mVideoHeight > mScreenHeight) {
            mNeedVideoWidth = (mScreenHeight * mVideoWidth) / mVideoHeight;
            mNeedVideoHeight = mScreenHeight;
        }
        if (mNeedVideoWidth > mScreenWidth) {
            mNeedVideoWidth = mScreenWidth;
        }
        if (mNeedVideoHeight > mScreenHeight) {
            mNeedVideoHeight = mScreenHeight;
        }
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW              mNeedVideoWidth: " +
                mNeedVideoWidth + " mNeedVideoHeight: " + mNeedVideoHeight);

        // 控制面板高度
        mControllerPanelLayoutHeight = mControllerPanelLayout.getHeight();
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW mControllerPanelLayoutHeight: " +
                mControllerPanelLayoutHeight);

        boolean needShowCacheProgress = mSP.getBoolean(NEED_SHOW_CACHE_PROGRESS, true);
        if (needShowCacheProgress && !mIsLocal) {
            mDataCacheLayout.setVisibility(View.VISIBLE);
            // 生产,消耗进度条高度
            mDataCacheLayoutHeight = mDataCacheLayout.getHeight();
            Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW     mProgressBarLayoutHeight: " +
                    mDataCacheLayoutHeight);
            if (mDataCacheLayoutHeight > 0) {
                RelativeLayout.LayoutParams relativeParams =
                        (RelativeLayout.LayoutParams) mDataCacheLayout.getLayoutParams();
                relativeParams.setMargins(
                        (mScreenWidth - mNeedVideoWidth) / 2,
                        (mScreenHeight - mNeedVideoHeight) / 2,
                        0, 0);
                relativeParams.width = mNeedVideoWidth;
                relativeParams.height = mDataCacheLayoutHeight;
                mDataCacheLayout.setLayoutParams(relativeParams);
            }
        } else {
            mDataCacheLayout.setVisibility(View.GONE);
        }

        boolean needShowInfo = mSP.getBoolean(NEED_SHOW_MEDIA_INFO, false);
        textInfoTV.setVisibility(needShowInfo ? View.VISIBLE : View.GONE);

        // 改变SurfaceView宽高度
        RelativeLayout.LayoutParams relativeParams =
                (RelativeLayout.LayoutParams) mSurfaceView.getLayoutParams();
        //relativeParams.setMargins(0, 0, 0, 0);
        relativeParams.setMargins(
                (mScreenWidth - mNeedVideoWidth) / 2, (mScreenHeight - mNeedVideoHeight) / 2,
                0, 0);
        relativeParams.width = mNeedVideoWidth;
        if (statusBarHeight != 0) {
            relativeParams.height = mNeedVideoHeight - statusBarHeight;
        } else {
            relativeParams.height = mNeedVideoHeight;
        }
        mSurfaceView.setLayoutParams(relativeParams);
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW         relativeParams.width: " +
                relativeParams.width + " relativeParams.height: " + relativeParams.height);

        relativeParams =
                (RelativeLayout.LayoutParams) textInfoScrollView.getLayoutParams();
        relativeParams.setMargins(
                (mScreenWidth - mNeedVideoWidth) / 2, 6, 0, 0);
        textInfoScrollView.setLayoutParams(relativeParams);

        // 改变ControllerPanelLayout高度
        FrameLayout.LayoutParams frameParams =
                (FrameLayout.LayoutParams) mControllerPanelLayout.getLayoutParams();
        if ((mScreenWidth / 3) < mNeedVideoWidth) {// top = 120
            //frameParams.setMargins((mScreenWidth - mNeedVideoWidth) / 2, 40, 0, 0);
            frameParams.setMargins((mScreenWidth - mNeedVideoWidth) / 2,
                    mNeedVideoHeight - mControllerPanelLayoutHeight, 0, 0);
            frameParams.width = mNeedVideoWidth;
        } else {
            //frameParams.setMargins((mScreenWidth - mScreenHeight) / 2, 40, 0, 0);
            frameParams.setMargins((mScreenWidth - mScreenHeight) / 2,
                    mNeedVideoHeight - mControllerPanelLayoutHeight, 0, 0);
            frameParams.width = mScreenHeight;
        }
        frameParams.height = mControllerPanelLayoutHeight;
        mControllerPanelLayout.setLayoutParams(frameParams);

        if (mPlayerService != null || mRemotePlayerService != null) {
            if (statusBarHeight != 0) {
                updateRootViewLayout(mScreenWidth, mScreenHeight - statusBarHeight);
            } else {
                updateRootViewLayout(mScreenWidth, mScreenHeight);
            }
        }

        mUiHandler.removeMessages(MSG_CHANGE_COLOR);
        mUiHandler.sendEmptyMessageDelayed(MSG_CHANGE_COLOR, 1000);

        Phone.removeUiMessages(DO_SOMETHING_EVENT_HANDLE_BUTTON_SIZE);
        Phone.callUiDelayed(PlayerWrapper.class.getName(),
                DO_SOMETHING_EVENT_HANDLE_BUTTON_SIZE, 500, null);
    }

    // 处理竖屏
    public void handlePortraitScreen() {
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW handlePortraitScreen");
        if (!mIsAddedView) {
            return;
        }

        mWindow = Window.Max_Screen;
        mIsPortraitScreen = true;
        mRootView.setBackgroundColor(
                mContext.getResources().getColor(android.R.color.transparent));

        int x = 0;
        int y = 0;
        String position = mSP.getString(PLAYBACK_WINDOW_POSITION, null);
        if (position != null && position.contains(PLAYBACK_WINDOW_POSITION_TAG)) {
            String[] positions = position.split(PLAYBACK_WINDOW_POSITION_TAG);
            y = Integer.parseInt(positions[1]);
            /*if (IS_WATCH) {
                y = 65;
            }*/
        }
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW x: " + x + " y: " + y);

        // 控制面板高度
        mControllerPanelLayoutHeight = mControllerPanelLayout.getHeight();
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW mControllerPanelLayoutHeight: " +
                mControllerPanelLayoutHeight);

        // 暂停按钮高度
        int pauseRlHeight = getPauseRlHeight2();

        // mScreenWidth: 1080 mScreenHeight: 2244
        // 屏幕宽高
        DisplayMetrics displayMetrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW                 mScreenWidth: " +
                mScreenWidth + " mScreenHeight: " + mScreenHeight);

        int height = getStatusBarHeight() + getNavigationBarHeight();

        // 改变SurfaceView高度
        RelativeLayout.LayoutParams relativeParams =
                (RelativeLayout.LayoutParams) mSurfaceView.getLayoutParams();
        if (mVideoWidth != 0 && mVideoHeight != 0) {
            mNeedVideoWidth = mScreenWidth;
            mNeedVideoHeight = (mScreenWidth * mVideoHeight) / mVideoWidth;
            if (mNeedVideoHeight > (mScreenHeight - height)) {
                mNeedVideoHeight = mScreenHeight - height;
                mNeedVideoWidth = (mNeedVideoHeight * mVideoWidth) / mVideoHeight;
                /*if (mNeedVideoWidth < mScreenWidth) {
                    relativeParams.setMargins((mScreenWidth - mNeedVideoWidth), 0, 0, 0);
                }*/
            }
        } else {
            mNeedVideoWidth = mScreenWidth;
            mNeedVideoHeight = mControllerPanelLayoutHeight;
            // mNeedVideoHeight = 1;
        }
        relativeParams.setMargins(0, 0, 0, 0);
        relativeParams.width = mNeedVideoWidth;
        relativeParams.height = mNeedVideoHeight;
        mSurfaceView.setLayoutParams(relativeParams);
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW              mNeedVideoWidth: " +
                mNeedVideoWidth + " mNeedVideoHeight: " + mNeedVideoHeight);

        relativeParams =
                (RelativeLayout.LayoutParams) textInfoScrollView.getLayoutParams();
        relativeParams.setMargins(0, 6, 0, 0);
        textInfoScrollView.setLayoutParams(relativeParams);

        // 改变ControllerPanelLayout高度
        FrameLayout.LayoutParams frameParams =
                (FrameLayout.LayoutParams) mControllerPanelLayout.getLayoutParams();
        /*if ((mNeedVideoHeight + mControllerPanelLayoutHeight) > (mScreenHeight - height)) {
            frameParams.setMargins(
                    0,
                    // mScreenHeight - mControllerPanelLayoutHeight - height,
                    mNeedVideoHeight - mControllerPanelLayoutHeight,
                    0, 0);
            mControllerPanelLayout.setBackgroundColor(
                    mContext.getResources().getColor(android.R.color.transparent));
        } else {
            frameParams.setMargins(
                    0, mNeedVideoHeight, 0, 0);
        }*/
        if (mIsLive) {
            frameParams.setMargins(
                    0,
                    mNeedVideoHeight - pauseRlHeight,
                    0, 0);
        } else {
            frameParams.setMargins(
                    0,
                    mNeedVideoHeight - mControllerPanelLayoutHeight,
                    0, 0);
        }
        if (mIsVideo) {
            mControllerPanelLayout.setBackgroundColor(
                    ContextCompat.getColor(mContext, android.R.color.transparent));
        } else {
            mControllerPanelLayout.setBackgroundColor(
                    ContextCompat.getColor(mContext, android.R.color.holo_purple));
        }
        frameParams.width = mNeedVideoWidth;
        frameParams.height = mControllerPanelLayoutHeight;
        mControllerPanelLayout.setLayoutParams(frameParams);

        boolean needShowCacheProgress = mSP.getBoolean(NEED_SHOW_CACHE_PROGRESS, true);
        if (needShowCacheProgress && !mIsLocal) {
            mDataCacheLayout.setVisibility(View.VISIBLE);
            // 生产,消耗进度条高度
            mDataCacheLayoutHeight = mDataCacheLayout.getHeight();
            Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW     mProgressBarLayoutHeight: " +
                    mDataCacheLayoutHeight);
            if (mDataCacheLayoutHeight > 0) {
                relativeParams = (RelativeLayout.LayoutParams) mDataCacheLayout.getLayoutParams();
                relativeParams.setMargins(0, 0, 0, 0);
                relativeParams.width = mNeedVideoWidth;
                relativeParams.height = mDataCacheLayoutHeight;
                mDataCacheLayout.setLayoutParams(relativeParams);
            }
        } else {
            mDataCacheLayout.setVisibility(View.GONE);
        }

        boolean needShowInfo = mSP.getBoolean(NEED_SHOW_MEDIA_INFO, false);
        textInfoTV.setVisibility(needShowInfo ? View.VISIBLE : View.GONE);

        if (mPlayerService != null || mRemotePlayerService != null) {
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                //if (mNeedVideoHeight > (int) (mScreenHeight * 2 / 3)) {
                /*if ((mNeedVideoHeight + mControllerPanelLayoutHeight) > (mScreenHeight -
                height)) {
                    updateRootViewLayout(
                            mNeedVideoWidth, mNeedVideoHeight, x, y);
                } else {
                    if (mIsLive) {
                        // 直播节目
                        updateRootViewLayout(
                                mNeedVideoWidth,
                                mNeedVideoHeight + pauseRlHeight, x, y);
                    } else {
                        updateRootViewLayout(
                                mNeedVideoWidth,
                                mNeedVideoHeight + mControllerPanelLayoutHeight, x, y);
                    }
                }*/
                updateRootViewLayout(mNeedVideoWidth, mNeedVideoHeight, x, y);
            } else {
                if (mIsVideo) {
                    if (mIsLive) {
                        // 是视频并且只下载不播放的情况下
                        updateRootViewLayout(mNeedVideoWidth, pauseRlHeight, x, y);
                        return;
                    }
                }
                if (!mIsLive) {
                    // 音乐 或者 mMediaDuration > 0
                    updateRootViewLayout(mNeedVideoWidth, mControllerPanelLayoutHeight + 1, x, y);
                } else {
                    updateRootViewLayout(mNeedVideoWidth, pauseRlHeight + 1, x, y);
                }
            }
        }

        mUiHandler.removeMessages(MSG_CHANGE_COLOR);
        mUiHandler.sendEmptyMessageDelayed(MSG_CHANGE_COLOR, 1000);

        Phone.removeUiMessages(DO_SOMETHING_EVENT_HANDLE_BUTTON_SIZE);
        Phone.callUiDelayed(PlayerWrapper.class.getName(),
                DO_SOMETHING_EVENT_HANDLE_BUTTON_SIZE, 500, null);
    }

    // 电视机专用
    public void handlePortraitScreenWithTV() {
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW handlePortraitScreenWithTV");
        if (/*mVideoWidth == 0 || mVideoHeight == 0 || */!mIsAddedView) {
            return;
        }

        handleScreenFlag = 1;

        mWindow = Window.Min_Screen;
        mIsPortraitScreen = true;
        mRootView.setBackgroundColor(
                mContext.getResources().getColor(android.R.color.transparent));

        int x = 0;
        int y = 0;
        String position = mSP.getString(PLAYBACK_WINDOW_POSITION, null);
        if (position != null && position.contains(PLAYBACK_WINDOW_POSITION_TAG)) {
            String[] positions = position.split(PLAYBACK_WINDOW_POSITION_TAG);
            x = Integer.parseInt(positions[0]);
            y = Integer.parseInt(positions[1]);
        }
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW x: " + x + " y: " + y);

        int pauseRlHeight = getPauseRlHeight2();

        // mScreenWidth: 1080 mScreenHeight: 2244
        // 屏幕宽高
        DisplayMetrics displayMetrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW                 mScreenWidth: " +
                mScreenWidth + " mScreenHeight: " + mScreenHeight);

        // 宽度比例
        String widthProportion = mSP.getString(PLAYBACK_WIDTH_PROPORTION, null);
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW              widthProportion: " +
                widthProportion);
        if (!TextUtils.isEmpty(widthProportion) && widthProportion.endsWith("/11")) {
            int proportion = 6; // 理想值
            try {
                proportion = Integer.parseInt(widthProportion.split("/")[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mScreenWidth = (mScreenWidth * proportion) / 11;
        } else {
            mScreenWidth = mScreenWidth / 3 + 180;
        }

        // 控制面板高度
        mControllerPanelLayoutHeight = mControllerPanelLayout.getHeight();
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW mControllerPanelLayoutHeight: " +
                mControllerPanelLayoutHeight);

        // 改变SurfaceView高度
        RelativeLayout.LayoutParams relativeParams =
                (RelativeLayout.LayoutParams) mSurfaceView.getLayoutParams();
        relativeParams.setMargins(0, 0, 0, 0);
        if (mVideoWidth != 0 && mVideoHeight != 0) {
            mNeedVideoWidth = mScreenWidth;
            mNeedVideoHeight = (mScreenWidth * mVideoHeight) / mVideoWidth;
            if (mNeedVideoHeight > mScreenHeight) {
                mNeedVideoHeight = mScreenHeight;
                mNeedVideoWidth = (mNeedVideoHeight * mVideoWidth) / mVideoHeight;
            }
        } else {
            mNeedVideoWidth = mScreenWidth;
            mNeedVideoHeight = mControllerPanelLayoutHeight;
            // mNeedVideoHeight = 1;
        }
        relativeParams.width = mNeedVideoWidth;
        relativeParams.height = mNeedVideoHeight;
        mSurfaceView.setLayoutParams(relativeParams);
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW              mNeedVideoWidth: " +
                mNeedVideoWidth + " mNeedVideoHeight: " + mNeedVideoHeight);

        relativeParams =
                (RelativeLayout.LayoutParams) textInfoScrollView.getLayoutParams();
        relativeParams.setMargins(0, 4, 0, 0);
        textInfoScrollView.setLayoutParams(relativeParams);

        // 改变ControllerPanelLayout高度
        FrameLayout.LayoutParams frameParams =
                (FrameLayout.LayoutParams) mControllerPanelLayout.getLayoutParams();
        /*if ((mNeedVideoHeight + mControllerPanelLayoutHeight) > (mScreenHeight -
        getStatusBarHeight())) {
            frameParams.setMargins(
                    0,
                    mScreenHeight - mControllerPanelLayoutHeight - getStatusBarHeight(),
                    0, 0);
            mControllerPanelLayout.setBackgroundColor(
                    mContext.getResources().getColor(android.R.color.transparent));
        } else {
            frameParams.setMargins(
                    0, mNeedVideoHeight, 0, 0);
        }*/
        if (mIsLive) {
            frameParams.setMargins(
                    0,
                    mNeedVideoHeight - pauseRlHeight,
                    0, 0);
        } else {
            frameParams.setMargins(
                    0,
                    mNeedVideoHeight - mControllerPanelLayoutHeight,
                    0, 0);
        }
        if (mIsVideo) {
            mControllerPanelLayout.setBackgroundColor(
                    ContextCompat.getColor(mContext, android.R.color.transparent));
        } else {
            mControllerPanelLayout.setBackgroundColor(
                    ContextCompat.getColor(mContext, android.R.color.holo_purple));
        }
        frameParams.width = mNeedVideoWidth;
        frameParams.height = mControllerPanelLayoutHeight;
        mControllerPanelLayout.setLayoutParams(frameParams);

        boolean needShowCacheProgress = mSP.getBoolean(NEED_SHOW_CACHE_PROGRESS, true);
        if (needShowCacheProgress && !mIsLocal) {
            mDataCacheLayout.setVisibility(View.VISIBLE);
            mDataCacheLayoutHeight = mDataCacheLayout.getHeight();
            Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW     mProgressBarLayoutHeight: " +
                    mDataCacheLayoutHeight);
            if (mDataCacheLayoutHeight > 0) {
                relativeParams =
                        (RelativeLayout.LayoutParams) mDataCacheLayout.getLayoutParams();
                relativeParams.setMargins(0, 0, 0, 0);
                relativeParams.width = mNeedVideoWidth;
                relativeParams.height = mDataCacheLayoutHeight;
                mDataCacheLayout.setLayoutParams(relativeParams);
            }
        } else {
            mDataCacheLayout.setVisibility(View.GONE);
        }

        boolean needShowInfo = mSP.getBoolean(NEED_SHOW_MEDIA_INFO, false);
        textInfoTV.setVisibility(needShowInfo ? View.VISIBLE : View.GONE);

        if (mPlayerService != null || mRemotePlayerService != null) {
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                //if (mNeedVideoHeight > (int) (mScreenHeight * 2 / 3)) {
                /*if ((mNeedVideoHeight + mControllerPanelLayoutHeight) > (mScreenHeight -
                getStatusBarHeight())) {
                    updateRootViewLayout(mNeedVideoWidth, mNeedVideoHeight, x, y);
                } else {
                    if (mIsLive) {
                        updateRootViewLayout(mNeedVideoWidth,
                                mNeedVideoHeight + pauseRlHeight, x, y);
                    } else {
                        updateRootViewLayout(mNeedVideoWidth,
                                mNeedVideoHeight + mControllerPanelLayoutHeight, x, y);
                    }
                }*/
                updateRootViewLayout(mNeedVideoWidth, mNeedVideoHeight, x, y);
            } else {
                updateRootViewLayout(mNeedVideoWidth, mControllerPanelLayoutHeight + 1, x, y);
            }
        }

        mFileNameTV.requestFocus();
        mUiHandler.removeMessages(MSG_CHANGE_COLOR);
        mUiHandler.sendEmptyMessageDelayed(MSG_CHANGE_COLOR, 1000);

        Phone.removeUiMessages(DO_SOMETHING_EVENT_HANDLE_BUTTON_SIZE);
        Phone.callUiDelayed(PlayerWrapper.class.getName(),
                DO_SOMETHING_EVENT_HANDLE_BUTTON_SIZE, 500, null);
    }

    // Hikey970开发板专用
    @SuppressLint("SourceLockedOrientationActivity")
    public void handleLandscapeScreenWithHikey970() {
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW handleLandscapeScreenWithHikey970");

        getPauseRlHeight();

        // 状态样高度
        int statusBarHeight = getStatusBarHeight();
        // 系统控制面板高度
        int navigationBarHeight = getNavigationBarHeight();

        // mScreenWidth: 2149 mScreenHeight: 1080
        // 屏幕宽高
        DisplayMetrics displayMetrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW mScreenWidth: " +
                mScreenWidth + " mScreenHeight: " + mScreenHeight);

        // 控制面板高度
        mControllerPanelLayoutHeight = mControllerPanelLayout.getHeight();
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW mControllerPanelLayoutHeight: " +
                mControllerPanelLayoutHeight);
        if (mIsVideo) {
            mControllerPanelLayout.setBackgroundColor(
                    ContextCompat.getColor(mContext, android.R.color.transparent));
        } else {
            mControllerPanelLayout.setBackgroundColor(
                    ContextCompat.getColor(mContext, android.R.color.holo_purple));
        }

        // 改变SurfaceView高度
        RelativeLayout.LayoutParams relativeParams =
                (RelativeLayout.LayoutParams) mSurfaceView.getLayoutParams();
        relativeParams.setMargins(0, 0, 0, 0);
        relativeParams.width = mScreenWidth;
        relativeParams.height = mScreenHeight - statusBarHeight - navigationBarHeight;
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW relativeParams.width: " +
                relativeParams.width + " relativeParams.height: " + relativeParams.height);
        mSurfaceView.setLayoutParams(relativeParams);

        // 改变ControllerPanelLayout高度
        FrameLayout.LayoutParams frameParams =
                (FrameLayout.LayoutParams) mControllerPanelLayout.getLayoutParams();
        frameParams.setMargins(
                0, getStatusBarHeight(), 0, 0);
        frameParams.width = mScreenWidth;
        frameParams.height = mControllerPanelLayoutHeight;
        mControllerPanelLayout.setLayoutParams(frameParams);

        if (mPlayerService != null || mRemotePlayerService != null) {
            updateRootViewLayout(mScreenWidth, relativeParams.height);
        }
    }

    // Hikey970开发板专用
    public void handlePortraitScreenWithHikey970() {
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW handlePortraitScreenWithHikey970");

        int pauseRlHeight = getPauseRlHeight();

        // mScreenWidth: 1080 mScreenHeight: 2244
        // 屏幕宽高
        DisplayMetrics displayMetrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW mScreenWidth: " +
                mScreenWidth + " mScreenHeight: " + mScreenHeight);

        mScreenWidth = mScreenWidth / 3;

        // 控制面板高度
        mControllerPanelLayoutHeight = mControllerPanelLayout.getHeight();
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW mControllerPanelLayoutHeight: " +
                mControllerPanelLayoutHeight);

        // 改变SurfaceView高度
        RelativeLayout.LayoutParams relativeParams =
                (RelativeLayout.LayoutParams) mSurfaceView.getLayoutParams();
        relativeParams.setMargins(0, 0, 0, 0);
        if (mVideoWidth != 0 && mVideoHeight != 0) {
            mNeedVideoWidth = mScreenWidth;
            mNeedVideoHeight = (mScreenWidth * mVideoHeight) / mVideoWidth;
            if (mNeedVideoHeight > mScreenHeight) {
                mNeedVideoHeight = mScreenHeight;
            }
            mControllerPanelLayout.setBackgroundColor(
                    mContext.getResources().getColor(android.R.color.transparent));
        } else {
            mNeedVideoWidth = mScreenWidth;
            mNeedVideoHeight = 1;
            mControllerPanelLayout.setBackgroundColor(
                    mContext.getResources().getColor(R.color.lightgray));
        }
        relativeParams.width = mNeedVideoWidth;
        relativeParams.height = mNeedVideoHeight;
        mSurfaceView.setLayoutParams(relativeParams);
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW mNeedVideoWidth: " +
                mNeedVideoWidth + " mNeedVideoHeight: " + mNeedVideoHeight);

        relativeParams =
                (RelativeLayout.LayoutParams) textInfoScrollView.getLayoutParams();
        relativeParams.setMargins(0, 4, 0, 0);
        textInfoScrollView.setLayoutParams(relativeParams);

        // 改变ControllerPanelLayout高度
        FrameLayout.LayoutParams frameParams =
                (FrameLayout.LayoutParams) mControllerPanelLayout.getLayoutParams();
        if (mPlayerService != null || mRemotePlayerService != null) {
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                frameParams.setMargins(0, getStatusBarHeight(), 0, 0);
            } else {
                frameParams.setMargins(0, 0, 0, 0);
            }
        }
        frameParams.width = mScreenWidth;
        frameParams.height = mControllerPanelLayoutHeight;
        mControllerPanelLayout.setLayoutParams(frameParams);

        if (mPlayerService != null || mRemotePlayerService != null) {
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                updateRootViewLayout(mScreenWidth, mNeedVideoHeight + pauseRlHeight);
            } else {
                updateRootViewLayout(mScreenWidth, mControllerPanelLayoutHeight + 1);
            }
        }
    }

    public void sendMessageForLoadContents() {
        mThreadHandler.removeMessages(MSG_LOAD_CONTENTS);
        mThreadHandler.sendEmptyMessage(MSG_LOAD_CONTENTS);
    }

    private void buttonClickForFr() {
        if (!isFrameByFrameMode) {
            if (!mIsH264) {
                if (mMediaDuration > 300) {
                    subtractStep += 30;
                } else {
                    subtractStep += 10;
                }
            } else {
                if (mMediaDuration > 52428800) {// 50MB
                    subtractStep += 1048576;// 1MB
                } else {
                    subtractStep += 524288;// 514KB
                }
            }
            Log.d(TAG, "onClick() subtractStep: " + subtractStep);
            mUiHandler.removeMessages(MSG_SEEK_TO_SUBTRACT);
            mUiHandler.sendEmptyMessageDelayed(MSG_SEEK_TO_SUBTRACT, 1000);
        } else {
            sendEmptyMessage(DO_SOMETHING_CODE_frameByFrame);
        }
    }

    private void buttonClickForFf() {
        if (!isFrameByFrameMode) {
            if (!mIsH264) {
                if (mMediaDuration > 300) {
                    addStep += 30;
                } else {
                    addStep += 10;
                }
            } else {
                if (mMediaDuration > 52428800) {// 50MB
                    addStep += 1048576;// 1MB
                } else {
                    addStep += 524288;// 514KB
                }
            }
            Log.d(TAG, "onClick() addStep: " + addStep);
            mUiHandler.removeMessages(MSG_SEEK_TO_ADD);
            mUiHandler.sendEmptyMessageDelayed(MSG_SEEK_TO_ADD, 1000);
        } else {
            sendEmptyMessage(DO_SOMETHING_CODE_frameByFrame);
        }
    }

    private void buttonClickForPrev() {
        if (isFrameByFrameMode) {
            return;
        }
        mPlayPrevFile = true;
        mPlayNextFile = false;
        mPrePath = null;
        onRelease();
    }

    private void buttonClickForNext() {
        if (isFrameByFrameMode) {
            return;
        }
        mPlayPrevFile = false;
        mPlayNextFile = true;
        mPrePath = null;
        onRelease();
    }

    private void buttonClickForPause() {
        if (mWdPlayer != null && mWdPlayer.isRunning()) {
            mPlayIB.setVisibility(View.INVISIBLE);
            mPauseIB.setVisibility(View.VISIBLE);
            mWdPlayer.pause();
            mControllerPanelLayout.requestLayout();
            mControllerPanelLayout.invalidate();
        }
    }

    private void buttonClickForPlay() {
        if (mWdPlayer != null && mWdPlayer.isRunning()) {
            if (isFrameByFrameMode) {
                isFrameByFrameMode = false;
                sendEmptyMessage(DO_SOMETHING_CODE_frameByFrameForFinish);
                mVolumeNormal.setVisibility(View.VISIBLE);
                mVolumeMute.setVisibility(View.INVISIBLE);
                mWdPlayer.setVolume(VOLUME_NORMAL);
                mFfmpegUseMediaCodecDecode.setVolume(VOLUME_NORMAL);
                mSP.edit().putBoolean(PLAYBACK_IS_MUTE, false).commit();
                MyToast.show("帧模式已关闭");
            }
            mPlayIB.setVisibility(View.VISIBLE);
            mPauseIB.setVisibility(View.INVISIBLE);
            if (!IS_WATCH) {
                mLoadingLayout.setVisibility(View.GONE);
            }
            mWdPlayer.play();
            mControllerPanelLayout.requestLayout();
            mControllerPanelLayout.invalidate();
        }
    }

    private void buttonClickForExit() {
        if (!allowExit()) {
            Log.i(TAG, "buttonClickForExit() 稍等片刻");
            MyToast.show("稍候再试");
            return;
        }
        mDownloadClickCounts = 0;
        mIsDownloading = false;
        isFrameByFrameMode = false;
        Log.i(TAG, "buttonClickForExit()");
        // 表示用户主动关闭,不需要再继续播放
        removeView(false);
    }

    private void buttonClickForVolume() {
        if (isFrameByFrameMode) {
            return;
        }
        int curVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        MyToast.show(String.valueOf(curVolume));
        mVolumeSeekBar.setProgress(curVolume);
        mVolumeLayout.setVisibility(View.VISIBLE);
        mUiHandler.removeMessages(MSG_VOLUME_HIDE_LAYOUT);
        mUiHandler.sendEmptyMessageDelayed(MSG_VOLUME_HIDE_LAYOUT, 3000);
    }

    private void buttonClickForVolumeMin() {
        if (volumeStep == 0) {
            int curVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            volumeStep = curVolume - 1;
        } else {
            volumeStep -= 1;
        }
        if (volumeStep < minVolume) {
            volumeStep = minVolume;
        }
        MyToast.show(String.valueOf(volumeStep));
        mUiHandler.removeMessages(MSG_VOLUME_SEEK_TO_ADD);
        mUiHandler.removeMessages(MSG_VOLUME_SEEK_TO_SUBTRACT);
        mUiHandler.removeMessages(MSG_VOLUME_HIDE_LAYOUT);
        mUiHandler.sendEmptyMessageDelayed(MSG_VOLUME_SEEK_TO_SUBTRACT, 2000);
    }

    private void buttonClickForVolumeMax() {
        if (volumeStep == 0 || volumeStep == maxVolume) {
            int curVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            volumeStep = curVolume + 1;
        } else {
            volumeStep += 1;
        }
        if (volumeStep > maxVolume) {
            volumeStep = maxVolume;
        }
        MyToast.show(String.valueOf(volumeStep));
        mUiHandler.removeMessages(MSG_VOLUME_SEEK_TO_ADD);
        mUiHandler.removeMessages(MSG_VOLUME_SEEK_TO_SUBTRACT);
        mUiHandler.removeMessages(MSG_VOLUME_HIDE_LAYOUT);
        mUiHandler.sendEmptyMessageDelayed(MSG_VOLUME_SEEK_TO_ADD, 2000);
    }

    private void buttonLongClickForVolumeNormal() {
        if (isFrameByFrameMode) {
            return;
        }
        mVolumeNormal.setVisibility(View.INVISIBLE);
        mVolumeMute.setVisibility(View.VISIBLE);
        if (mWdPlayer != null) {
            mWdPlayer.setVolume(VOLUME_MUTE);
        }
        mFfmpegUseMediaCodecDecode.setVolume(VOLUME_MUTE);
        mSP.edit().putBoolean(PLAYBACK_IS_MUTE, true).commit();
    }

    private void buttonLongClickForVolumeMute() {
        if (isFrameByFrameMode) {
            return;
        }
        mVolumeNormal.setVisibility(View.VISIBLE);
        mVolumeMute.setVisibility(View.INVISIBLE);
        if (mWdPlayer != null) {
            mWdPlayer.setVolume(VOLUME_NORMAL);
        }
        mFfmpegUseMediaCodecDecode.setVolume(VOLUME_NORMAL);
        mSP.edit().putBoolean(PLAYBACK_IS_MUTE, false).commit();
    }

    private void buttonClickForRepeatOff() {
        MyToast.show("Repeat All");
        mRepeat = Repeat.Repeat_All;
        setRepeatView();
    }

    private void buttonClickForRepeatAll() {
        MyToast.show("Repeat One");
        mRepeat = Repeat.Repeat_One;
        setRepeatView();
    }

    private void buttonClickForRepeatOne() {
        MyToast.show("Repeat Off");
        mRepeat = Repeat.Repeat_Off;
        setRepeatView();
    }

    private void buttonClickForShuffleOff() {
        MyToast.show("Shuffle On");
        mShuffle = Shuffle.Shuffle_On;
        setShuffleView();
    }

    private void buttonClickForShuffleOn() {
        MyToast.show("Shuffle Off");
        mShuffle = Shuffle.Shuffle_Off;
        setShuffleView();
    }

    private void buttonClickForDownload() {
        if (TextUtils.isEmpty(mDownloadTV.getText())) {
            mDownloadTV.setText("0");
            mDownloadTV.setBackgroundColor(mContext.getResources().getColor(R.color.burlywood));
            return;
        }
        mDownloadClickCounts++;
        mThreadHandler.removeMessages(MSG_DOWNLOAD);
        mThreadHandler.sendEmptyMessageDelayed(MSG_DOWNLOAD, 2000);
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

    private void getSomeInfo(String textInfo) throws Exception {
        String[] infos = textInfo.split("] \\[");
        if (infos.length >= 4) {
            String bit_rate_total_str = infos[0];
            String bit_rate_video_str = infos[1];
            String frame_rate_str = infos[3];
            bit_rate_total_str = bit_rate_video_str.substring(bit_rate_total_str.indexOf("["));
            bit_rate_total = Integer.parseInt(bit_rate_total_str);
            bit_rate_video = Integer.parseInt(bit_rate_video_str);
            frame_rate = Integer.parseInt(frame_rate_str);
            Log.i(TAG, "getSomeInfo()" +
                    " bit_rate_total: " + bit_rate_total +
                    " bit_rate_video: " + bit_rate_video +
                    " frame_rate: " + frame_rate);
        }
    }

    private void onReady() {
        if (mIDmrPlayerAppCallback != null) {
            try {
                Log.i(TAG, "onReady() onState STATE_PREPARING");
                mIDmrPlayerAppCallback.onState(mIid, STATE_PREPARING);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        // 是否显示控制面板
        if (mIsVideo) {
            if (!mIsLocal) {
                if (!IS_WATCH) {
                    mLoadingLayout.setVisibility(View.VISIBLE);
                }
                mControllerPanelLayout.setVisibility(View.VISIBLE);
            } else {
                boolean show = mSP.getBoolean(PLAYBACK_SHOW_CONTROLLERPANELLAYOUT, true);
                if (show) {
                    mControllerPanelLayout.setVisibility(View.VISIBLE);
                } else {
                    mControllerPanelLayout.setVisibility(View.INVISIBLE);
                }
            }
        } else if (mIsAudio) {
            mControllerPanelLayout.setVisibility(View.VISIBLE);
        }
        mPresentationTime = 0;
        mMediaDuration = 0;
        setControllerPanelBackgroundColor();
        //mPositionTimeTV.setText("00:00:00");
        mPositionSeekBar.setProgress(0);
        mPositionTimeTV.setText("");
        mDurationTimeTV.setText("");

        //mPositionSeekBar.setProgress(0);
        //mPositionSeekBar.setPadding(0, 0, 0, 0);
        //mPositionSeekBar.setThumbOffset(0);

        mDataCacheLayout.setVisibility(View.GONE);
        // 左边进度值
        mVideoProgressBar.setProgress(0);
        // 右边进度值
        mVideoProgressBar.setSecondaryProgress(0);
        mAudioProgressBar.setProgress(0);
        mAudioProgressBar.setSecondaryProgress(0);

        mPlayIB.setVisibility(View.VISIBLE);
        mPauseIB.setVisibility(View.INVISIBLE);
        mVolumeLayout.setVisibility(View.INVISIBLE);
        textInfoScrollView.setVisibility(View.GONE);
        textInfoTV.setText("");
        mDownloadTV.setText("");
        // R.color.lightgray
        mDownloadTV.setBackgroundColor(
                ContextCompat.getColor(mContext, android.R.color.transparent));
        // 声音图标
        boolean isMute = mSP.getBoolean(PLAYBACK_IS_MUTE, false);
        if (!isMute) {
            mVolumeNormal.setVisibility(View.VISIBLE);
            mVolumeMute.setVisibility(View.INVISIBLE);
        } else {
            mVolumeNormal.setVisibility(View.INVISIBLE);
            mVolumeMute.setVisibility(View.VISIBLE);
        }
        // 标题
        String title;
        if (mIsLocal) {
            title = mCurPath.substring(mCurPath.lastIndexOf("/") + 1);
        } else {
            title = mContentsMap.get(mCurPath);
        }
        mFileNameTV.setText("");
        if (!TextUtils.isEmpty(title)) {
            if (!IS_WATCH || (IS_WATCH && mIsAudio)) {
                mFileNameTV.setText(title);
            }
        }
    }

    private void onChangeWindow(Message msg) {
        // 此时才能得到视频或音频的时间Duration
        // 视频宽高
        mVideoWidth = msg.arg1;
        mVideoHeight = msg.arg2;
        mMediaDuration = mWdPlayer.getDuration();
        if (mMediaDuration <= 0) {
            mIsLive = true;
        } else {
            mIsLive = false;
        }
        if (TextUtils.equals(whatPlayer, PLAYER_IJKPLAYER)) {
            mIjkPlayer.mIsLive = mIsLive;
        }
        if (mFfmpegUseMediaCodecDecode != null) {
            mFfmpegUseMediaCodecDecode.mIsLive = mIsLive;
        }
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW               mMediaDuration: " +
                mMediaDuration);
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW                   videoWidth: " +
                mVideoWidth + " videoHeight: " + mVideoHeight);

        if (mIsH264) {
            mDurationTimeTV.setText(String.valueOf(mMediaDuration));
        } else {
            mDurationTimeTV.setText(DateUtils.formatElapsedTime(mMediaDuration));
        }

        if (mIsVideo) {
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                boolean show = mSP.getBoolean(PLAYBACK_SHOW_CONTROLLERPANELLAYOUT, true);
                if (show) {
                    mControllerPanelLayout.setVisibility(View.VISIBLE);
                    textInfoScrollView.setVisibility(View.VISIBLE);
                } else {
                    mControllerPanelLayout.setVisibility(View.INVISIBLE);
                    textInfoScrollView.setVisibility(View.GONE);
                }
                if (!mIsLocal/* && !TextUtils.equals(whatPlayer, PLAYER_IJKPLAYER)*/) {
                    boolean needShowCacheProgress = mSP.getBoolean(
                            NEED_SHOW_CACHE_PROGRESS, true);
                    if (needShowCacheProgress) {
                        mDataCacheLayout.setVisibility(View.VISIBLE);
                    }
                }
                if (!mCouldPlaybackPathList.contains(mCurPath)) {
                    mCouldPlaybackPathList.add(mCurPath);
                }
            }

            if (mVideoWidth == 0 && mVideoHeight == 0) {
                setType("audio/");
                mControllerPanelLayout.setVisibility(View.VISIBLE);
                textInfoScrollView.setVisibility(View.GONE);
                mDataCacheLayout.setVisibility(View.GONE);
            }
        }

        int orientation = mContext.getResources().getConfiguration().orientation;
        switch (orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                // 横屏
                if (IS_WATCH) {
                    if (mIsVideo) {
                        handleLandscapeScreen(0);
                    } else {
                        handlePortraitScreen();
                    }
                } else {
                    if (JniPlayerActivity.isAliveJniPlayerActivity) {
                        handleLandscapeScreen(0);
                    } else {
                        handlePortraitScreenWithTV();
                    }
                }
                break;
            case Configuration.ORIENTATION_PORTRAIT:
            default:
                if (mWindow == Window.Min_Screen) {
                    handlePortraitScreenWithTV();
                } else {
                    // 竖屏
                    handlePortraitScreen();
                }
                break;
        }

        SharedPreferences.Editor edit = mSP.edit();
        // 保存播放地址
        edit.putString(PLAYBACK_ADDRESS, mCurPath);
        edit.putString(PLAYBACK_MEDIA_TYPE, mType);
        // 开始播放设置为false,表示初始化状态
        edit.putBoolean(PLAYBACK_NORMAL_FINISH, false);
        edit.commit();

        if (mIDmrPlayerAppCallback != null) {
            try {
                Log.i(TAG, "onReady() onState STATE_PREPARED");
                mIDmrPlayerAppCallback.onState(mIid, STATE_PREPARED);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        if (mHasError) {
            mHasError = false;
            mFfmpegUseMediaCodecDecode.mUseMediaCodecForVideo = true;
        }
    }

    private void onPlayed() {
        mPlayIB.setVisibility(View.VISIBLE);
        mPauseIB.setVisibility(View.INVISIBLE);
        if (!IS_WATCH) {
            mLoadingLayout.setVisibility(View.GONE);
        } else {
            if (mIsVideo) {
                MyToast.show("Play");
            }
        }
        if (mIDmrPlayerAppCallback != null) {
            try {
                Log.i(TAG, "onPlayed() onState STATE_PLAYING");
                mIDmrPlayerAppCallback.onState(mIid, STATE_PLAYING);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void onPaused() {
        //mPlayIB.setVisibility(View.INVISIBLE);
        //mPauseIB.setVisibility(View.VISIBLE);
        if (!mIsLocal) {
            if (!IS_WATCH) {
                mLoadingLayout.setVisibility(View.VISIBLE);
            } else {
                if (mIsVideo) {
                    MyToast.show("Pause");
                }
            }
        }
        if (mIDmrPlayerAppCallback != null) {
            try {
                Log.i(TAG, "onPaused() onState STATE_PAUSED");
                mIDmrPlayerAppCallback.onState(mIid, STATE_PAUSED);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void onFinished() {
        Log.i(TAG, "onFinished()");
        mPositionSeekBar.setProgress(0);
        mPositionTimeTV.setText("");
        mDurationTimeTV.setText("");
        if (mIDmrPlayerAppCallback != null) {
            try {
                Log.i(TAG, "onFinished() onState STATE_STOPPED");
                mIDmrPlayerAppCallback.onState(mIid, STATE_STOPPED);
                //Log.i(TAG, "onFinished() onCompletion");
                //mIDmrPlayerAppCallback.onCompletion(mIid);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        if (mFfmpegUseMediaCodecDecode.VIDEO_NEED_TO_ASYNC) {
            mFfmpegUseMediaCodecDecode.releaseMediaCodec();
        }
        // mFfmpegUseMediaCodecDecode.releaseMediaCodec();
        mFFMPEGPlayer.releaseAudioTrack();

        abandonAudioFocusRequest();
        stopForeground();

        if (mHasError) {
            switch (mErrorCode) {
                case Callback.ERROR_TIME_OUT:
                case Callback.ERROR_MEDIA_CODEC:
                case Callback.ERROR_TIME_DIFFERENCE:
                    Log.i(TAG, "onFinished() restart playback");
                    if (mErrorCode == Callback.ERROR_TIME_OUT) {
                        mHasError = false;
                    }
                    removeCallback();
                    mUiHandler.removeMessages(MSG_ADD_VIEW);
                    mUiHandler.sendEmptyMessage(MSG_ADD_VIEW);
                    break;
                case Callback.ERROR_FFMPEG_INIT:
                    if (mIsTesting) {
                        mHasTestError = true;
                        mThreadHandler.removeMessages(MSG_TEST_SIGNAL);
                        mThreadHandler.sendEmptyMessageDelayed(MSG_TEST_SIGNAL, 5000);
                    }
                    break;
                default:
                    break;
            }
            return;
        }

        Log.i(TAG, "Safe Exit");
        MyToast.show("(*^_^*)");

        // 播放结束
        if (!needToPlaybackOtherVideo()) {
            removeView(true);
            mSP.edit().putBoolean(PLAYBACK_NORMAL_FINISH, true).commit();
        }
    }

    private void inInfo(Message msg) {
        if (msg.obj != null && msg.obj instanceof String) {
            String toastInfo = ((String) msg.obj).trim();
            //Log.d(TAG, "Callback.MSG_ON_TRANSACT_INFO\n" + toastInfo);
            if (toastInfo.contains("[") && toastInfo.contains("]")) {
                textInfo = toastInfo;
                try {
                    getSomeInfo(textInfo);
                } catch (Exception e) {
                }
                textInfoTV.setText(toastInfo);
                mUiHandler.removeMessages(MSG_CHANGE_COLOR);
                mUiHandler.sendEmptyMessage(MSG_CHANGE_COLOR);
            } else if (toastInfo.contains("AVERROR_EOF")) {
                Log.d(TAG, "inInfo() mPrePath = null");
                mPrePath = null;
            } else {
                MyToast.show(toastInfo);
            }
        }
    }

    private void onError(Message msg) {
        mHasError = false;
        String errorInfo = "error";
        if (msg.obj != null) {
            errorInfo = (String) msg.obj;
        }
        MyToast.show(errorInfo);
        mErrorCode = msg.arg1;
        switch (mErrorCode) {
            case Callback.ERROR_TIME_DIFFERENCE:
                Log.e(TAG, "PlayerWrapper Callback.ERROR_TIME_DIFFERENCE errorInfo: " + errorInfo);
                // 需要重新播放
                mHasError = true;
                removeView(false);
                break;
            case Callback.ERROR_MEDIA_CODEC:
                // 音频或视频硬解码失败(会调到onFinished())
                Log.e(TAG, "PlayerWrapper Callback.ERROR_MEDIA_CODEC errorInfo: " + errorInfo);
                mFfmpegUseMediaCodecDecode.mUseMediaCodecForVideo = false;
                mHasError = true;
                removeView(false);
                break;
            case Callback.ERROR_TIME_OUT:
                // 读取数据超时(会调到onFinished())
                Log.e(TAG, "PlayerWrapper Callback.ERROR_TIME_OUT errorInfo: " + errorInfo);
                // 需要重新播放
                mHasError = true;
                removeView(false);
                break;
            case Callback.ERROR_FFMPEG_INIT:
                // 音视频初始化失败(不会调到onFinished())
                Log.e(TAG, "PlayerWrapper Callback.ERROR_FFMPEG_INIT errorInfo: " + errorInfo);
                /*if (mIsVideo) {
                    if (mCouldPlaybackPathList.contains(mCurPath)
                            && !mCurPath.startsWith("http://cache.m.iqiyi.com/")) {
                        //startForGetMediaFormat();
                        mThreadHandler.removeMessages(MSG_PREPARE);
                        mThreadHandler.sendEmptyMessageDelayed(MSG_PREPARE, 3000);
                        break;
                    } else {
                        String path = mSP.getString(PLAYBACK_ADDRESS, null);
                        if (TextUtils.equals(path, mCurPath)
                                && !mCurPath.startsWith("http://cache.m.iqiyi.com/")) {
                            //startForGetMediaFormat();
                            mThreadHandler.removeMessages(MSG_PREPARE);
                            mThreadHandler.sendEmptyMessageDelayed(MSG_PREPARE, 3000);
                            break;
                        }
                    }
                }*/

                if (!needToPlaybackOtherVideo()) {
                    mHasError = true;
                    removeView(true);
                }
                break;
            default:
                break;
        }
        if (mIDmrPlayerAppCallback != null) {
            try {
                mIDmrPlayerAppCallback.onError(mIid, mErrorCode, errorInfo);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void onUpdated(Message msg) {
        // 底层判断过了,如果是live节目,就不会回调到这里
        /*if (mIsLive) {
            return;
        }*/

        if (mIsLive) {
            mPresentationTime++;
            return;
        }

        // 秒
        mPresentationTime = (Long) msg.obj;

        if (!mIsH264) {
            mPositionTimeTV.setText(DateUtils.formatElapsedTime(mPresentationTime));
        } else {
            mPositionTimeTV.setText(String.valueOf(mPresentationTime));
        }

        if (mNeedToSyncProgressBar) {
            int currentPosition = (int) (mPresentationTime);
            float pos = (float) currentPosition / mMediaDuration;
            int target = Math.round(pos * mPositionSeekBar.getMax());
            mPositionSeekBar.setProgress(target);
            mPositionSeekBar.invalidate();
            mControllerPanelLayout.requestLayout();
            mControllerPanelLayout.invalidate();
        }

        if (mPresentationTime < (mMediaDuration - 5)) {
            mPathTimeMap.put(md5Path, mPresentationTime);
            if (mIsH264 && (mMediaDuration - mPresentationTime <= 1000000)) {
                mPathTimeMap.remove(md5Path);
            }
        } else {
            // 正常结束就不需要播放了
            if (!mIsTesting) {
                Log.d(TAG, "onUpdated() mPrePath = null");
                mPrePath = null;
                if (mPresentationTime == (mMediaDuration - 5)
                        && TextUtils.equals(whatPlayer, PLAYER_FFPLAY)) {
                    Log.d(TAG, "onUpdated() MSG_RELEASE");
                    if (mPathTimeMap.containsKey(md5Path)) {
                        mPathTimeMap.remove(md5Path);
                    }
                    mUiHandler.removeMessages(MSG_RELEASE);
                    if (mVideoWidth >= 3840 && mVideoHeight >= 2160) {
                        mUiHandler.sendEmptyMessageDelayed(MSG_RELEASE, 7000);
                    } else {
                        mUiHandler.sendEmptyMessageDelayed(MSG_RELEASE, 5000);
                    }
                }
            }
        }
    }

    private void updateRootViewLayout(int width, int height) {
        updateRootViewLayout(width, height, 0, 0);
    }

    private void updateRootViewLayout(int width, int height, int x, int y) {
        if (mIsAddedView) {
            // mLayoutParams.gravity = Gravity.DISPLAY_CLIP_VERTICAL;
            mLayoutParams.width = width;
            mLayoutParams.height = height;
            mLayoutParams.x = x;
            mLayoutParams.y = y;
            mWindowManager.updateViewLayout(mRootView, mLayoutParams);
        }
    }

    private int getPauseRlHeight() {
        int pauseRlHeight = 0;
        if (mPlayerService != null || mRemotePlayerService != null) {
            RelativeLayout pause_rl = mRootView.findViewById(R.id.button_control_layout);
            pauseRlHeight = pause_rl.getHeight();
            SeekBar progress_bar = mRootView.findViewById(R.id.progress_bar);
            LinearLayout show_time_rl = mRootView.findViewById(R.id.show_time_rl);
            ImageButton button_fr = mRootView.findViewById(R.id.button_fr);
            ImageButton button_ff = mRootView.findViewById(R.id.button_ff);
            ImageButton button_prev = mRootView.findViewById(R.id.button_prev);
            ImageButton button_next = mRootView.findViewById(R.id.button_next);
            ImageButton button_repeat_off = mRootView.findViewById(R.id.button_repeat_off);
            ImageButton button_repeat_all = mRootView.findViewById(R.id.button_repeat_all);
            ImageButton button_repeat_one = mRootView.findViewById(R.id.button_repeat_one);
            ImageButton button_shuffle_off = mRootView.findViewById(R.id.button_shuffle_off);
            ImageButton button_shuffle_on = mRootView.findViewById(R.id.button_shuffle_on);
            if (mIsLive && !mIsH264) {
                progress_bar.setVisibility(View.GONE);
                show_time_rl.setVisibility(View.GONE);
                if (!IS_WATCH) {
                    button_fr.setVisibility(View.INVISIBLE);
                    button_ff.setVisibility(View.INVISIBLE);
                }
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
                if (!IS_WATCH) {
                    button_fr.setVisibility(View.VISIBLE);
                    button_ff.setVisibility(View.VISIBLE);
                }
                button_prev.setVisibility(View.VISIBLE);
                button_next.setVisibility(View.VISIBLE);
                setRepeatView();
                setShuffleView();
            }
        }
        return pauseRlHeight;
    }

    private int mCount = 0;

    private int getPauseRlHeight2() {
        Log.i(TAG, "getPauseRlHeight2()");
        if (mPlayerService == null && mRemotePlayerService == null) {
            return 0;
        }

        // region
        // 按钮的Layout,不包括SeekBar
        RelativeLayout controlLaout = mRootView.findViewById(R.id.button_control_layout);
        int controlLayoutWidth = controlLaout.getWidth();
        int controlLayoutHeight = controlLaout.getHeight();
        Log.i(TAG, "getPauseRlHeight2()    controlLayoutWidth: " + controlLayoutWidth);
        Log.i(TAG, "getPauseRlHeight2()   controlLayoutHeight: " + controlLayoutHeight);
        if (controlLayoutWidth == 0 || controlLayoutHeight == 0) {
            return 0;
        }
        SeekBar progress_bar = mRootView.findViewById(R.id.progress_bar);
        // 显示时间的Layout
        LinearLayout show_time_rl = mRootView.findViewById(R.id.show_time_rl);
        // 播放/暂停,退出,音量
        ImageButton button_play = mRootView.findViewById(R.id.button_play);
        ImageButton button_pause = mRootView.findViewById(R.id.button_pause);
        ImageButton button_exit = mRootView.findViewById(R.id.button_exit);
        ImageButton button_volume = mRootView.findViewById(R.id.volume_normal);
        ImageButton volume_mute = mRootView.findViewById(R.id.volume_mute);
        ImageButton screen_max = mRootView.findViewById(R.id.screen_max);
        // 快退快进
        ImageButton button_fr = mRootView.findViewById(R.id.button_fr);
        ImageButton button_ff = mRootView.findViewById(R.id.button_ff);
        // 上一首下一首
        ImageButton button_prev = mRootView.findViewById(R.id.button_prev);
        ImageButton button_next = mRootView.findViewById(R.id.button_next);
        // 关闭重复/重复全部/重复单个
        ImageButton button_repeat_off = mRootView.findViewById(R.id.button_repeat_off);
        ImageButton button_repeat_all = mRootView.findViewById(R.id.button_repeat_all);
        ImageButton button_repeat_one = mRootView.findViewById(R.id.button_repeat_one);
        // 顺序/乱序
        ImageButton button_shuffle_off = mRootView.findViewById(R.id.button_shuffle_off);
        ImageButton button_shuffle_on = mRootView.findViewById(R.id.button_shuffle_on);
        // endregion

        // region
        if (mIsLive && !mIsH264) {
            progress_bar.setVisibility(View.GONE);
            show_time_rl.setVisibility(View.GONE);
            if (!IS_WATCH) {
                button_fr.setVisibility(View.INVISIBLE);
                button_ff.setVisibility(View.INVISIBLE);
            }
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
            if (!IS_WATCH) {
                button_fr.setVisibility(View.VISIBLE);
                button_ff.setVisibility(View.VISIBLE);
            }
            button_prev.setVisibility(View.VISIBLE);
            button_next.setVisibility(View.VISIBLE);
            setRepeatView();
            setShuffleView();
        }
        // endregion

        // 播放/暂停按钮的宽高
        int maxButtonWantedWidth = 88; // 72
        int maxButtonWantedHeight = 88;
        // 其他按钮的宽高
        int minButtonWantedWidth = 72; // 48
        int minButtonWantedHeight = 72;
        // 间隙
        int space = 40;
        if (mWindow == Window.Full_Screen) {
            maxButtonWantedWidth = 104;
            maxButtonWantedHeight = 104;
            minButtonWantedWidth = 88;
            minButtonWantedHeight = 88;
            space = 48;
        } else if (mWindow == Window.Max_Screen) {
            maxButtonWantedWidth = 100;
            maxButtonWantedHeight = 100;
            minButtonWantedWidth = 80;
            minButtonWantedHeight = 80;
            if (mIsLive && !mIsH264) {
            } else {
                space = 8;
            }
        } else if (mWindow == Window.Min_Screen) {
            if (mIsLive && !mIsH264) {
            } else {
                space = 0;
            }
        }
        if (mIsLive && !mIsH264) {
            // region
            while (maxButtonWantedWidth + minButtonWantedWidth * 2 + space * 2 > controlLayoutWidth) {
                minButtonWantedWidth -= 2;
                minButtonWantedHeight = minButtonWantedWidth;
                maxButtonWantedWidth = (minButtonWantedWidth * 3) / 2;
                maxButtonWantedHeight = maxButtonWantedWidth;
            }
            // endregion
        } else {
            // region
            while (maxButtonWantedWidth + minButtonWantedWidth * 10 + space * 10 > controlLayoutWidth) {
                minButtonWantedWidth -= 2;
                minButtonWantedHeight = minButtonWantedWidth;
                maxButtonWantedWidth = (minButtonWantedWidth * 3) / 2;
                maxButtonWantedHeight = maxButtonWantedWidth;
            }
            // endregion
        }
        Log.i(TAG, "getPauseRlHeight2()  maxButtonWantedWidth: " + maxButtonWantedWidth);
        Log.i(TAG, "getPauseRlHeight2() maxButtonWantedHeight: " + maxButtonWantedHeight);
        Log.i(TAG, "getPauseRlHeight2()  minButtonWantedWidth: " + minButtonWantedWidth);
        Log.i(TAG, "getPauseRlHeight2() minButtonWantedHeight: " + minButtonWantedHeight);
        Log.i(TAG, "getPauseRlHeight2()                 space: " + space);

        RelativeLayout.LayoutParams params = null;
        if (mIsLive && !mIsH264) {
            // region 3个按钮
            params = (RelativeLayout.LayoutParams) button_exit.getLayoutParams();
            int start =
                    (controlLayoutWidth - minButtonWantedWidth * 2 - space * 2 - maxButtonWantedWidth) / 2;
            params.setMarginStart(start);
            params.width = minButtonWantedWidth;
            params.height = minButtonWantedHeight;
            button_exit.setLayoutParams(params);

            params = (RelativeLayout.LayoutParams) button_play.getLayoutParams();
            params.setMarginStart(space);
            params.width = maxButtonWantedWidth;
            params.height = maxButtonWantedHeight;
            button_play.setLayoutParams(params);
            button_pause.setLayoutParams(params);

            params = (RelativeLayout.LayoutParams) button_volume.getLayoutParams();
            params.setMarginStart(space);
            params.width = minButtonWantedWidth;
            params.height = minButtonWantedHeight;
            button_volume.setLayoutParams(params);
            volume_mute.setLayoutParams(params);

            params = (RelativeLayout.LayoutParams) screen_max.getLayoutParams();
            params.setMarginStart(space);
            params.width = minButtonWantedWidth;
            params.height = minButtonWantedHeight;
            screen_max.setLayoutParams(params);
            // endregion
        } else {
            // region 9个按钮
            params = (RelativeLayout.LayoutParams) button_fr.getLayoutParams();
            params.setMarginStart(0);
            params.width = minButtonWantedWidth;
            params.height = minButtonWantedHeight;
            button_fr.setLayoutParams(params);

            params = (RelativeLayout.LayoutParams) button_ff.getLayoutParams();
            params.setMarginStart(space);
            params.width = minButtonWantedWidth;
            params.height = minButtonWantedHeight;
            button_ff.setLayoutParams(params);

            params = (RelativeLayout.LayoutParams) button_prev.getLayoutParams();
            params.setMarginStart(space);
            params.width = minButtonWantedWidth;
            params.height = minButtonWantedHeight;
            button_prev.setLayoutParams(params);

            params = (RelativeLayout.LayoutParams) button_next.getLayoutParams();
            params.setMarginStart(space);
            params.width = minButtonWantedWidth;
            params.height = minButtonWantedHeight;
            button_next.setLayoutParams(params);

            params = (RelativeLayout.LayoutParams) button_exit.getLayoutParams();
            params.setMarginStart(space);
            params.width = minButtonWantedWidth;
            params.height = minButtonWantedHeight;
            button_exit.setLayoutParams(params);

            params = (RelativeLayout.LayoutParams) button_play.getLayoutParams();
            params.setMarginStart(space);
            params.width = maxButtonWantedWidth;
            params.height = maxButtonWantedHeight;
            button_play.setLayoutParams(params);
            button_pause.setLayoutParams(params);

            params = (RelativeLayout.LayoutParams) button_volume.getLayoutParams();
            params.setMarginStart(space);
            params.width = minButtonWantedWidth;
            params.height = minButtonWantedHeight;
            button_volume.setLayoutParams(params);
            volume_mute.setLayoutParams(params);

            params = (RelativeLayout.LayoutParams) screen_max.getLayoutParams();
            params.setMarginStart(space);
            params.width = minButtonWantedWidth;
            params.height = minButtonWantedHeight;
            screen_max.setLayoutParams(params);

            params = (RelativeLayout.LayoutParams) button_repeat_off.getLayoutParams();
            params.setMarginStart(minButtonWantedWidth + space * 2);
            params.width = minButtonWantedWidth;
            params.height = minButtonWantedHeight;
            button_repeat_off.setLayoutParams(params);
            button_repeat_all.setLayoutParams(params);
            button_repeat_one.setLayoutParams(params);

            params = (RelativeLayout.LayoutParams) button_shuffle_off.getLayoutParams();
            params.setMarginStart(space);
            params.width = minButtonWantedWidth;
            params.height = minButtonWantedHeight;
            button_shuffle_off.setLayoutParams(params);
            button_shuffle_on.setLayoutParams(params);
            // endregion
        }

        return controlLayoutHeight;
    }

    private SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "surfaceCreated()");

            // startPlayback();
            startForGetMediaFormat();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder,
                                   int format, int width, int height) {
            Log.d(TAG, "surfaceChanged() width: " + width + " height: " + height);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "surfaceDestroyed()");
            onRelease();
        }
    };

    private int volumeStep = 0;
    private long addStep = 0;
    private long subtractStep = 0;
    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            PlayerWrapper.this.onClick(v);
        }
    };

    private void clickOne() {
        mUiHandler.removeMessages(MSG_CHANGE_COLOR);
        if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE// 横屏
                || Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (textInfoScrollView.getVisibility() == View.VISIBLE) {
                mControllerPanelLayout.setVisibility(View.GONE);
                textInfoScrollView.setVisibility(View.GONE);
                mSP.edit().putBoolean(PLAYBACK_SHOW_CONTROLLERPANELLAYOUT, false).commit();
            } else {
                mControllerPanelLayout.setVisibility(View.VISIBLE);
                textInfoScrollView.setVisibility(View.VISIBLE);
                mSP.edit().putBoolean(PLAYBACK_SHOW_CONTROLLERPANELLAYOUT, true).commit();
                mUiHandler.sendEmptyMessage(MSG_CHANGE_COLOR);
            }
            return;
        }

        if (textInfoScrollView.getVisibility() == View.VISIBLE) {
            textInfoScrollView.setVisibility(View.GONE);
        } else {
            textInfoScrollView.setVisibility(View.VISIBLE);
            mUiHandler.sendEmptyMessageDelayed(MSG_CHANGE_COLOR, 10);
        }
    }

    private void clickTwo() {
        /*test();
        if (true) return;*/

        if (mWdPlayer == null) {
            return;
        }
        if (!mWdPlayer.isRunning()) {
            return;
        }

        // 播放与暂停
        if (mWdPlayer.isPlaying()) {
            mPlayIB.setVisibility(View.INVISIBLE);
            mPauseIB.setVisibility(View.VISIBLE);
            mWdPlayer.pause();
        } else {
            mPlayIB.setVisibility(View.VISIBLE);
            mPauseIB.setVisibility(View.INVISIBLE);
            if (!IS_WATCH) {
                mLoadingLayout.setVisibility(View.GONE);
            }
            mWdPlayer.play();
        }
    }

    private void clickThree() {
        if (mWdPlayer == null) {
            return;
        }
        if (!mWdPlayer.isRunning()) {
            // 蓝牙的操作
            if (mIsVideo) {
                setType("video/");
            } else if (mIsAudio) {
                setType("audio/");
            }
            Phone.call(
                    PlayerService.class.getName(),
                    PlayerService.COMMAND_SHOW_WINDOW,
                    new Object[]{mCurPath, mType});
            return;
        }

        /***
         缓冲过程中,按三下进行暂停,继续缓冲.
         */
        mPlayIB.setVisibility(View.INVISIBLE);
        mPauseIB.setVisibility(View.VISIBLE);
        if (!mIsLocal) {
            if (!IS_WATCH) {
                mLoadingLayout.setVisibility(View.VISIBLE);
            }
        }
        mWdPlayer.pause();
        MyToast.show("Pause");
    }

    private int handleScreenFlag = 1;

    @SuppressLint("SourceLockedOrientationActivity")
    private void clickFour() {
        if (!IS_WATCH) {
            if (!JniPlayerActivity.isAliveJniPlayerActivity) {
                Log.i(TAG, "clickFour() startActivity");
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setClass(mContext, FullScreenActivity.class);
                mContext.startActivity(intent);
                return;
            }
            Log.i(TAG, "clickFour() DO_SOMETHING_EVENT_HANDLE_BUTTON_SIZE");
            Phone.call(FullScreenActivity.class.getName(), 4, null);
            Phone.removeUiMessages(DO_SOMETHING_EVENT_HANDLE_BUTTON_SIZE);
            Phone.callUiDelayed(PlayerWrapper.class.getName(),
                    DO_SOMETHING_EVENT_HANDLE_BUTTON_SIZE, 1000, null);
        } else {
            clickTen();
        }
    }

    private void clickFive() {
        if (TextUtils.equals(whatPlayer, PLAYER_IJKPLAYER)) {
        } else if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
        } else {
            if (mWdPlayer == null) {
                return;
            }
            if (!mWdPlayer.isRunning()) {
                return;
            }

            isFrameByFrameMode = Boolean.parseBoolean(
                    sendEmptyMessage(DO_SOMETHING_CODE_frameByFrameForReady));
            if (isFrameByFrameMode) {
                // 静音
                mVolumeNormal.setVisibility(View.INVISIBLE);
                mVolumeMute.setVisibility(View.VISIBLE);
                mWdPlayer.setVolume(VOLUME_MUTE);
                mFfmpegUseMediaCodecDecode.setVolume(VOLUME_MUTE);
                mSP.edit().putBoolean(PLAYBACK_IS_MUTE, true).commit();
                // 显示控制面板
                mControllerPanelLayout.setVisibility(View.VISIBLE);
                textInfoScrollView.setVisibility(View.VISIBLE);
                mSP.edit().putBoolean(PLAYBACK_SHOW_CONTROLLERPANELLAYOUT, true).commit();
                // 显示"暂停"按钮
                mPlayIB.setVisibility(View.INVISIBLE);
                mPauseIB.setVisibility(View.VISIBLE);
                MyToast.show("帧模式已开启");
            }
        }
    }

    private void clickSix() {
        int softSolutionForAudio = mSP.getInt(HARD_SOLUTION_AUDIO, 1);
        if (softSolutionForAudio == 1) {
            MyToast.show("使用音频软解码");
            mSP.edit().putInt(HARD_SOLUTION_AUDIO, 0).commit();
        } else if (softSolutionForAudio == 0) {
            MyToast.show("使用音频硬解码");
            mSP.edit().putInt(HARD_SOLUTION_AUDIO, 1).commit();
        }
    }

    private void clickSeven() {
        int softSolution = mSP.getInt(HARD_SOLUTION, 1);
        if (softSolution == 1) {
            MyToast.show("使用音视频软解码");
            mSP.edit().putInt(HARD_SOLUTION, 0).commit();
        } else if (softSolution == 0) {
            MyToast.show("使用音视频硬解码");
            mSP.edit().putInt(HARD_SOLUTION, 1).commit();
        }
    }

    private void clickEight() {
        // 关闭音频部分
        if (TextUtils.equals(whatPlayer, PLAYER_IJKPLAYER)) {
        } else if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
        } else {
            sendEmptyMessage(DO_SOMETHING_CODE_isWatchForCloseAudio);
        }
    }

    private void clickNine() {
        // 关闭视频部分
        if (TextUtils.equals(whatPlayer, PLAYER_IJKPLAYER)) {
        } else if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
        } else {
            sendEmptyMessage(DO_SOMETHING_CODE_isWatchForCloseVideo);
        }
    }

    private void clickTen() {
        if (mWdPlayer == null) {
            return;
        }
        if (!mWdPlayer.isRunning()) {
            return;
        }

        if (mContext.getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_PORTRAIT) {
            Log.d(TAG, "onKeyDown() 4 竖屏");
            handlePortraitScreen();
        } else {
            Log.d(TAG, "onKeyDown() 4 横屏");
            switch (handleScreenFlag) {
                case 1:
                    handleLandscapeScreen(0);
                    break;
                case 2:
                    handleLandscapeScreen(1);
                    break;
                case 3:
                    handlePortraitScreenWithTV();
                    break;
                default:
                    break;
            }
        }
    }

    private void clickEleven() {
        onRelease();
    }

    private String sendEmptyMessage(int code) {
        if (mWdPlayer != null) {
            return mWdPlayer.onTransact(code, null);
        }
        return null;
    }

    private void loadContents() {
        Log.i(TAG, "loadContents() start");
        mContentsMap.clear();
        mIptvContentsMap.clear();
        mMenFavoriteContentsMap.clear();

        /***
         mContext.getExternalFilesDirs(Environment.DIRECTORY_PICTURES);
         /storage/emulated/0/Android/data/com.sony.dtv.smartmediaapp/files/Pictures
         /storage/3670-C58C/Android/data/com.sony.dtv.smartmediaapp/files/Pictures
         /storage/37C8-3904/Android/data/com.sony.dtv.smartmediaapp/files/Pictures
         */
        File[] files = mContext.getExternalFilesDirs(Environment.MEDIA_SHARED);
        if (files == null) {
            Log.e(TAG, "loadContents() files is null");
            return;
        }

        File file = null;
        String rootDir = null;
        if (files.length == 1) {
            file = files[0];
            rootDir = file.getAbsolutePath();
        } else {
            for (File f : files) {
                if (f == null) {
                    continue;
                }
                if (f.getAbsolutePath().startsWith("/storage/emulated/0/")) {
                    file = f;
                } else {
                    rootDir = f.getAbsolutePath();
                }
            }
        }
        if (file == null) {
            return;
        }

        // /storage/emulated/0/Android/data/com.weidi.media.wdplayer/files/shared
        Log.i(TAG, "Environment.MEDIA_SHARED: " + file.getAbsolutePath());
        testTargetPath = file.getAbsolutePath();

        // /storage/emulated/0/Android/data/com.weidi.media.wdplayer/files/shared/contents.txt
        StringBuilder sb = new StringBuilder();
        sb.append(file.getAbsolutePath());
        sb.append("/");
        sb.append("contents.txt");
        File contentsFile = new File(sb.toString());
        if (contentsFile.exists()) {
            readContents(contentsFile, mContentsMap);
        } else {
            if (copyFile("contents.", contentsFile)) {
                readContents(contentsFile, mContentsMap);
            }
        }

        // /storage/emulated/0/Android/data/com.weidi.media.wdplayer/files/shared/iptv.url
        sb.delete(0, sb.length());
        sb.append(file.getAbsolutePath());
        sb.append("/");
        sb.append("iptv.url");
        contentsFile = new File(sb.toString());
        testTargetPath2 = contentsFile.getAbsolutePath();
        if (contentsFile.exists()) {
            readContents(contentsFile, mIptvContentsMap);
        } else {
            if (copyFile("iptv.url", contentsFile)) {
                readContents(contentsFile, mIptvContentsMap);
            }
        }

        /*sb.delete(0, sb.length());
        sb.append(file.getAbsolutePath());
        sb.append("/");
        sb.append("test.url");
        testTargetPath1 = sb.toString();*/

        if (IS_PHONE || IS_WATCH) {
            sb.delete(0, sb.length());
            sb.append(file.getAbsolutePath());
            sb.append("/");
            sb.append("men_favorite.txt");
            File menFavoriteFile = new File(sb.toString());
            if (menFavoriteFile.exists()) {
                readContents(menFavoriteFile, mMenFavoriteContentsMap);
            } else {
                if (copyFile("men_favorite.", menFavoriteFile)) {
                    readContents(menFavoriteFile, mMenFavoriteContentsMap);
                }
            }

            // 最好把文件放在下面这些目录中
            // Alarms  DCIM      Download Music         Pictures Ringtones
            // Android Documents Movies   Notifications Podcasts
            PackageManager packageManager = mContext.getPackageManager();
            if (PackageManager.PERMISSION_GRANTED == packageManager.checkPermission(
                    Manifest.permission.READ_EXTERNAL_STORAGE, mContext.getPackageName())) {
                if (mLocalVideoContentsMap == null)
                    mLocalVideoContentsMap = new LinkedHashMap();
                if (mLocalAudioContentsMap == null)
                    mLocalAudioContentsMap = new LinkedHashMap();
                rootDir = rootDir.substring(0, rootDir.indexOf("/Android/"));
                Log.i(TAG, "loadContents()   rootDir: " + rootDir);
                sb.delete(0, sb.length());
                sb.append(rootDir);
                // 华为手机 /Movies/Camera
                // 一加手机 /Movies
                sb.append("/Movies/Camera");
                // /storage/emulated/0/Movies/
                // Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
                file = new File(sb.toString());
                mLocalVideoPath = file.getAbsolutePath();
                Log.i(TAG, "loadContents()      file: " + file.getAbsolutePath());
                saveLocalFile("video", file);

                sb.delete(0, sb.length());
                sb.append(rootDir);
                sb.append("/Music");
                // /storage/emulated/0/Music/
                // Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
                file = new File(sb.toString());
                mLocalAudioPath = file.getAbsolutePath();
                Log.i(TAG, "loadContents()      file: " + file.getAbsolutePath());
                saveLocalFile("audio", file);
            }
        }

        Log.i(TAG, "loadContents() end");
    }

    private void saveLocalFile(String type, File file) {
        // path: /storage/emulated/0/Movies/SONY_CM_EXTRA_BASS_dance.mp4
        // name: SONY_CM_EXTRA_BASS_dance.mp4
        if (file != null && file.isDirectory() && file.exists()) {
            File[] files = file.listFiles();
            if (files != null && files.length > 0) {
                String path;
                String name;
                for (File f : files) {
                    if (f == null) {
                        continue;
                    }
                    if (f.isFile()) {
                        path = f.getAbsolutePath();
                        name = path.substring(path.lastIndexOf("/") + 1);
                        //Log.i(TAG, "path: " + path + " name: " + name);
                        if (TextUtils.equals("video", type)) {
                            mLocalVideoContentsMap.put(path, name);
                        } else if (TextUtils.equals("audio", type)) {
                            mLocalAudioContentsMap.put(path, name);
                        }
                    }
                }
            }
        }
    }

    private boolean copyFile(String flag, File targetFile) {
        if (!targetFile.exists()) {
            try {
                targetFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        try {
            // 遍历该目录下的文件和文件夹
            String[] listFiles = mContext.getAssets().list("");
            if (listFiles == null) {
                return false;
            }
            boolean isSuccess = false;
            // 判断目录是文件还是文件夹，这里只好用.做区分了
            for (String path : listFiles) {
                if (TextUtils.isEmpty(path)) {
                    continue;
                }
                File file = new File(path);
                String fileName = file.getName();
                // /contents.txt
                // /hw_pc_white_apps.xml
                // /wifipro_regexlist.xml
                Log.i(TAG, "getAssets               : " + fileName);
                //if (file.getAbsolutePath().contains("contents.")) {
                if (fileName.contains(flag)) {
                    InputStream is = mContext.getAssets().open(fileName);
                    FileOutputStream fos = new FileOutputStream(targetFile);
                    byte[] buffer = new byte[2048];
                    int byteCount = 0;
                    while ((byteCount = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, byteCount);
                    }
                    fos.flush();
                    is.close();
                    fos.close();
                    isSuccess = true;
                    Log.i(TAG, "copyFile() success!!!");
                    break;
                }
            }
            if (isSuccess) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void readContents(File file, LinkedHashMap<String, String> map) {
        final String TAG1 = "@@@@@@@@@@";
        final String TAG2 = "#EXTINF:-1 ,";

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String aLineContent = null;
            String[] contents = null;
            String key = null;
            String value = null;
            StringBuilder sb = new StringBuilder();
            int i = 0;
            boolean toDoIt = false;
            //一次读一行，读入null时文件结束
            while ((aLineContent = reader.readLine()) != null) {
                toDoIt = false;
                if (aLineContent.length() == 0) {
                    continue;
                }

                aLineContent = aLineContent.trim();

                if (aLineContent.contains(TAG1)
                        && !aLineContent.startsWith("#")
                        && !aLineContent.startsWith("//")) {
                    contents = aLineContent.split(TAG1);
                    key = contents[0];
                    value = contents[1];
                    if (contents.length > 1) {
                        toDoIt = true;
                    }
                } else {
                    if (aLineContent.startsWith(TAG2)) {
                        value = aLineContent.substring(TAG2.length(), aLineContent.length());
                        key = null;
                    } else if (aLineContent.startsWith("http://")
                            || aLineContent.startsWith("https://")
                            || aLineContent.startsWith("rtmp://")
                            || aLineContent.startsWith("rtsp://")) {
                        key = aLineContent;
                    } else {
                        value = null;
                        key = null;
                    }
                    if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
                        toDoIt = true;
                    }
                }

                if (toDoIt) {
                    ++i;
                    sb.append(i);
                    if (i < 10) {
                        sb.append("____");
                    } else if (i >= 10 && i < 100) {
                        sb.append("___");
                    } else if (i >= 100 && i < 1000) {
                        sb.append("__");
                    } else if (i >= 1000 && i < 10000) {
                        sb.append("_");
                    }
                    sb.append("_");
                    sb.append(value);

                    if (!map.containsKey(key)) {
                        map.put(key, sb.toString());
                    } else {
                        --i;
                    }

                    /*Log.i("player_alexander", "readContents() sb.toString(): " + sb.toString());*/
                    sb.delete(0, sb.length());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
    }

    private boolean isFrameByFrameMode = false;
    private static final int NEED_CLICK_COUNTS = 10;
    private int clickCounts = 0;

    public Object onEvent(int what, Object[] objArray) {
        Object result = null;
        switch (what) {
            // 有线耳机发出的事件
            case KeyEvent.KEYCODE_HEADSETHOOK:// 79
                if (!isFrameByFrameMode) {
                    ++clickCounts;
                    MyToast.show(String.valueOf(clickCounts));

                    // 单位时间内按1次,2次,3次分别实现单击,双击,三击
                    mUiHandler.removeMessages(KeyEvent.KEYCODE_HEADSETHOOK);
                    mUiHandler.sendEmptyMessageDelayed(KeyEvent.KEYCODE_HEADSETHOOK, 1000);
                } else {
                    sendEmptyMessage(DO_SOMETHING_CODE_frameByFrame);
                }
                break;
            // 85 86 87 88 126 127 这些都是蓝牙耳机发出的事件
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:// 85
                // 由LocalAudioActivityForWear发过来的
                // objArray = new Object[]{Shuffle.Shuffle_On, Repeat.Repeat_All};
                if (objArray == null || objArray.length <= 0) {
                    return null;
                }
                mShuffle = (Shuffle) objArray[0];
                mRepeat = (Repeat) objArray[1];
                break;
            case KeyEvent.KEYCODE_MEDIA_STOP:// 86
                break;
            case KeyEvent.KEYCODE_MEDIA_NEXT:// 87
                // 双击
                Log.d(TAG, "clickTwo()");
                clickTwo();
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:// 88
                // 三击
                Log.d(TAG, "clickThree()");
                clickThree();
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY:// 126
            case KeyEvent.KEYCODE_MEDIA_PAUSE:// 127
                // 单击
                //Log.d(TAG, "clickOne()");
                //clickOne();
                break;

            case BUTTON_CLICK_FR:
                buttonClickForFr();
                break;
            case BUTTON_CLICK_FF:
                buttonClickForFf();
                break;
            case BUTTON_CLICK_PREV:
                buttonClickForPrev();
                break;
            case BUTTON_CLICK_NEXT:
                buttonClickForNext();
                break;
            case BUTTON_CLICK_PLAY:
                buttonClickForPlay();
                break;
            case BUTTON_CLICK_PAUSE:
                buttonClickForPause();
                break;
            case BUTTON_CLICK_EXIT:
                buttonClickForExit();
                break;
            case BUTTON_CLICK_VOLUME_NORMAL:
                buttonLongClickForVolumeNormal();
                break;
            case BUTTON_CLICK_VOLUME_MUTE:
                buttonLongClickForVolumeMute();
                break;
            case BUTTON_CLICK_REPEAT_OFF:
                buttonClickForRepeatOff();
                break;
            case BUTTON_CLICK_REPEAT_ALL:
                buttonClickForRepeatAll();
                break;
            case BUTTON_CLICK_REPEAT_ONE:
                buttonClickForRepeatOne();
                break;
            case BUTTON_CLICK_SHUFFLE_OFF:
                buttonClickForShuffleOff();
                break;
            case BUTTON_CLICK_SHUFFLE_ON:
                buttonClickForShuffleOn();
                break;

            case BUTTON_CLICK_TEST_START: {
                if (!mIsTesting) {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            startTest();
                            return null;
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
                break;
            }
            case BUTTON_CLICK_TEST_STOP: {
                mThreadHandler.removeMessages(BUTTON_CLICK_TEST_STOP);
                mThreadHandler.sendEmptyMessageDelayed(BUTTON_CLICK_TEST_STOP, 3000);
                break;
            }

            case DO_SOMETHING_EVENT_IS_RUNNING:
                if (mWdPlayer != null && mWdPlayer.isRunning()) {
                    return true;
                } else {
                    return false;
                }
            case DO_SOMETHING_EVENT_GET_MEDIA_DURATION:
                return mMediaDuration;
            case DO_SOMETHING_EVENT_GET_REPEAT:
                return mRepeat;
            case DO_SOMETHING_EVENT_GET_SHUFFLE:
                return mShuffle;
            case DO_SOMETHING_EVENT_WIDTH_SCREEN: {
                handlePortraitScreen();
                break;
            }
            case DO_SOMETHING_EVENT_MIN_SCREEN: {
                handlePortraitScreenWithTV();
                break;
            }

            case DO_SOMETHING_EVENT_HANDLE_BUTTON_SIZE: {
                if (mCount >= 1) {
                    mCount = 0;
                    break;
                }
                mCount++;
                switch (mWindow) {
                    case Full_Screen: {
                        handleLandscapeScreen(0);
                        break;
                    }
                    case Max_Screen: {
                        handlePortraitScreen();
                        break;
                    }
                    case Min_Screen: {
                        handlePortraitScreenWithTV();
                        break;
                    }
                    default:
                        break;
                }
                break;
            }

            case DO_SOMETHING_EVENT_REPLAY: { // 在UI线程中调用
                buttonClickForExit();
                Phone.callThreadDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setDataSource(mCurPath);
                    }
                }, 3000);
                break;
            }

            default:
                break;
        }
        return result;
    }

    public void playPlayerWithTelephonyCall() {
        if (mWdPlayer != null && mWdPlayer.isRunning()) {
            boolean isMute = mSP.getBoolean(PLAYBACK_IS_MUTE, false);
            if (!isMute) {
                mWdPlayer.setVolume(VOLUME_NORMAL);
                mVolumeNormal.setVisibility(View.VISIBLE);
                mVolumeMute.setVisibility(View.INVISIBLE);
            } else {
                mWdPlayer.setVolume(VOLUME_MUTE);
                mVolumeNormal.setVisibility(View.INVISIBLE);
                mVolumeMute.setVisibility(View.VISIBLE);
            }

            if (!mWdPlayer.isPlaying()) {
                mPlayIB.setVisibility(View.VISIBLE);
                mPauseIB.setVisibility(View.INVISIBLE);
                mWdPlayer.play();
            }
        }
    }

    public void pausePlayerWithTelephonyCall() {
        if (mWdPlayer != null && mWdPlayer.isRunning()) {
            if (mWdPlayer.isPlaying()) {
                mPlayIB.setVisibility(View.INVISIBLE);
                mPauseIB.setVisibility(View.VISIBLE);
                mWdPlayer.pause();
            }
        }
    }

    AudioManager.OnAudioFocusChangeListener AudioFocusChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                public void onAudioFocusChange(int focusChange) {
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_GAIN:
                            // Resume playback or Raise it back to normal
                            Log.i(TAG, "onAudioFocusChange() AudioManager.AUDIOFOCUS_GAIN");
                            playPlayerWithTelephonyCall();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS:
                            //am.unregisterMediaButtonEventReceiver(RemoteControlReceiver);
                            //am.abandonAudioFocus(afChangeListener);
                            // Stop playback
                            Log.i(TAG, "onAudioFocusChange() AudioManager.AUDIOFOCUS_LOSS");
                            pausePlayerWithTelephonyCall();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            // Pause playback
                            Log.i(TAG, "onAudioFocusChange()" +
                                    " AudioManager.AUDIOFOCUS_LOSS_TRANSIENT");
                            pausePlayerWithTelephonyCall();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            // Pause playback
                            // Lower the volume
                            Log.i(TAG, "onAudioFocusChange()" +
                                    " AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                            pausePlayerWithTelephonyCall();
                            break;
                        default:
                            break;
                    }
                }
            };

    private class PlayerOnTouchListener implements View.OnTouchListener {
        private StringBuffer sb = new StringBuffer();
        private int x;
        private int y;

        private int tempX;
        private int tempY;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            /*if (!mIsPortraitScreen) {
                return true;
            }*/
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = (int) event.getRawX();
                    y = (int) event.getRawY();
                    tempX = x;
                    tempY = y;
                    break;
                case MotionEvent.ACTION_MOVE:
                    int nowX = (int) event.getRawX();
                    int nowY = (int) event.getRawY();
                    int movedX = nowX - x;
                    int movedY = nowY - y;
                    x = nowX;
                    y = nowY;
                    mLayoutParams.x = mLayoutParams.x + movedX;
                    mLayoutParams.y = mLayoutParams.y + movedY;

                    tempX = mLayoutParams.x;
                    tempY = mLayoutParams.y;

                    // 更新悬浮窗控件布局
                    // mWindowManager.updateViewLayout(view, mLayoutParams);
                    mWindowManager.updateViewLayout(mRootView, mLayoutParams);
                    break;
                case MotionEvent.ACTION_UP:
                    sb.delete(0, sb.length());
                    sb.append(tempX);
                    sb.append(PLAYBACK_WINDOW_POSITION_TAG);
                    sb.append(tempY);
                    mSP.edit().putString(PLAYBACK_WINDOW_POSITION, sb.toString()).commit();
                    Log.i(TAG,
                            "Callback.MSG_ON_CHANGE_WINDOW PlayerService: " + sb.toString());
                    break;
                default:
                    break;
            }
            return true;
        }
    }

    private void registerReceiver() {
        if (mScreenReceiver == null) {
            mScreenReceiver = new ScreenBroadcastReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_USER_PRESENT);
            mContext.registerReceiver(mScreenReceiver, filter);
        }
    }

    private void unregisterReceiver() {
        if (mScreenReceiver != null) {
            mContext.unregisterReceiver(mScreenReceiver);
            mScreenReceiver = null;
        }
    }

    @SuppressLint("InvalidWakeLockTag")
    private void wakeUpAndUnlock() {
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        boolean isInteractive = pm.isInteractive();
        if (!isInteractive) {
            // 屏幕是关闭状态
            PowerManager.WakeLock wl = pm.newWakeLock(
                    PowerManager.ACQUIRE_CAUSES_WAKEUP
                            | PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
                    //| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    "WakeLock");
            // 点亮屏幕
            wl.acquire(10000);
            // 释放
            wl.release();
        }
        /*// 屏幕解锁
        KeyguardManager keyguardManager =
                (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("unLock");
        // 屏幕锁定
        keyguardLock.reenableKeyguard();
        // 解锁
        keyguardLock.disableKeyguard();*/
    }

    private class ScreenBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                Log.i(TAG, "onReceive() 亮屏");
                mUiHandler.removeMessages(MSG_SCREEN_BRIGHT_WAKE_LOCK);
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                Log.i(TAG, "onReceive() 锁屏");
                // 在手表上,delayMillis的时间间隔是严重不同的
                mUiHandler.removeMessages(MSG_SCREEN_BRIGHT_WAKE_LOCK);
                mUiHandler.sendEmptyMessageDelayed(MSG_SCREEN_BRIGHT_WAKE_LOCK, 30 * 1000);
            } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                Log.i(TAG, "onReceive() 解锁");
            }
        }
    }

    private boolean mIsReading = false;

    private void saveLog() throws Exception {
        File[] files = mContext.getExternalFilesDirs(Environment.MEDIA_SHARED);
        File file = null;
        for (File f : files) {
            Log.i(TAG, "Environment.MEDIA_SHARED    : " + f.getAbsolutePath());
            file = f;
        }
        if (file == null) {
            return;
        }
        file = new File(file.getAbsolutePath(), "/Log.txt");
        if (file.exists()) {
            file.delete();
        }

        // adb logcat -G 20m &&
        String[] running = new String[]{"logcat", "-s", "adb logcat *:V"};
        Process exec = Runtime.getRuntime().exec(running);
        InputStream inputStream = exec.getInputStream();

        FileOutputStream os = new FileOutputStream(file, false);
        int readLength = 0;
        byte[] buffer = new byte[1024];
        Log.i(TAG, "saveLog() start");
        while (-1 != (readLength = inputStream.read(buffer))) {
            os.write(buffer, 0, readLength);
            os.flush();
        }
        Log.i(TAG, "saveLog() end");
    }

    private void readLog() throws Exception {
        File[] files = mContext.getExternalFilesDirs(Environment.MEDIA_SHARED);
        File file = null;
        for (File f : files) {
            Log.i(TAG, "Environment.MEDIA_SHARED    : " + f.getAbsolutePath());
            file = f;
        }
        if (file == null) {
            return;
        }

        file = new File(file.getAbsolutePath(), "/Log.txt");
        BufferedReader reader = new BufferedReader(new FileReader(file));

        mIsReading = true;
        String aLineLog = null;
        Log.i(TAG, "readLog() start");
        while (mIsReading) {
            aLineLog = reader.readLine();
            if (TextUtils.isEmpty(aLineLog)) {
                SystemClock.sleep(10);
                continue;
            }

            if (aLineLog.contains("DmrPlayerService")) {
                Log.d(TAG, "readLog(): \n" + aLineLog);
            }
        }
        Log.i(TAG, "readLog() end");
    }

    public static String md5(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("md5");
            byte[] digest = md.digest(str.getBytes());
            return bytes2hex02(digest);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String bytes2hex02(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        String tmp = null;
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            // 将每个字节与0xFF进行与运算，然后转化为10进制，然后借助于Integer再转化为16进制
            tmp = Integer.toHexString(0xFF & b);
            // 每个字节8为，转为16进制标志，2个16进制位
            if (tmp.length() == 1) {
                tmp = "0" + tmp;
            }
            sb.append(tmp);
        }
        return sb.toString();
    }

    ////////////////////////////////////////////////////////////////////////////////////

    // 目的: 把有用的地址挑出来
    private final Lock testLock = new ReentrantLock();
    private final Condition testCondition = testLock.newCondition();
    private ArrayList<String> availablePathList = new ArrayList<>();
    private boolean mIsTesting = false;
    private boolean mHasTestError = false;

    // /storage/emulated/0/Android/data/com.weidi.media.wdplayer/files/shared
    private String testTargetPath;

    // /storage/emulated/0/Android/data/com.weidi.media.wdplayer/files/shared/test.url
    //private String testTargetPath1;

    // /storage/emulated/0/Android/data/com.weidi.media.wdplayer/files/shared/iptv.url
    private String testTargetPath2;

    private void testAwait() {
        testLock.lock();
        try {
            testCondition.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        testLock.unlock();
    }

    private void testSignal() {
        testLock.lock();
        testCondition.signal();
        testLock.unlock();
    }

    /***
     /storage/emulated/0/Android/data/com.weidi.media.wdplayer/files/shared/test.url
     --->
     availablePathList
     --->
     /storage/emulated/0/Android/data/com.weidi.media.wdplayer/files/shared/temp_test.url
     */
    private void startTest() {
        if (mIsTesting
                //|| TextUtils.isEmpty(testTargetPath1)
                || TextUtils.isEmpty(testTargetPath2)) {
            Log.i(TAG, "startTest() return");
            return;
        }

        Log.i(TAG, "startTest() start");
        availablePathList.clear();
        mIsTesting = true;
        final String TAG1 = "@@@@@@@@@@";
        final String TAG2 = "#EXTINF:-1 ,";
        ArrayList<String> tempList = new ArrayList<>();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(testTargetPath2));
            String aLineContent = null;
            String[] contents = null;
            String key = null;// 视频地址
            String value = null;
            StringBuilder sb = new StringBuilder();
            boolean toDoIt = false;
            //一次读一行，读入null时文件结束
            while (mIsTesting && ((aLineContent = reader.readLine()) != null)) {
                toDoIt = false;
                if (aLineContent.length() == 0) {
                    continue;
                }

                aLineContent = aLineContent.trim();

                if (aLineContent.contains(TAG1)
                        && !aLineContent.startsWith("#")
                        && !aLineContent.startsWith("//")) {
                    contents = aLineContent.split(TAG1);
                    key = contents[0];
                    //value = contents[1];
                    value = aLineContent;
                    if (contents.length > 1) {
                        toDoIt = true;
                        tempList.add(value);
                    }
                } else {
                    if (aLineContent.startsWith(TAG2)) {
                        key = null;
                        //value = aLineContent.substring(TAG2.length(), aLineContent.length());
                        value = aLineContent;
                    } else if (aLineContent.startsWith("http://")
                            || aLineContent.startsWith("https://")
                            || aLineContent.startsWith("rtmp://")
                            || aLineContent.startsWith("rtsp://")) {
                        key = aLineContent;
                    } else {
                        key = null;
                        value = null;
                    }
                    if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
                        toDoIt = true;
                        tempList.add(value);
                        tempList.add(key);
                    }
                }

                if (toDoIt) {
                    setDataSource(key);

                    testAwait();

                    if (value.contains("[")) {
                        value = value.substring(0, value.indexOf("["));
                    }
                    int size = tempList.size();
                    sb.delete(0, sb.length());
                    sb.append(value);
                    sb.append("[");
                    sb.append(mVideoWidth);
                    sb.append("*");
                    sb.append(mVideoHeight);
                    sb.append("-");
                    sb.append(bit_rate_total);
                    sb.append("-");
                    sb.append(bit_rate_video);
                    sb.append("-");
                    sb.append(frame_rate);
                    sb.append("]");
                    tempList.clear();
                    tempList.add(sb.toString());
                    if (size > 1) {
                        tempList.add(key);
                    }

                    if (!mHasTestError) {
                        for (String path : tempList) {
                            availablePathList.add(path);
                            Log.i(TAG, "startTest() : " + path);
                        }
                    }
                    mHasTestError = false;
                    tempList.clear();
                }
            }
            aLineContent = null;
            contents = null;
            key = null;
            value = null;
            sb = null;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
            reader = null;
        }
        tempList = null;
        Log.i(TAG, "startTest() end");

        stopTest();

        mUiHandler.removeMessages(MSG_RELEASE);
        mUiHandler.sendEmptyMessageDelayed(MSG_RELEASE, 60000);
    }

    private void stopTest() {
        Log.i(TAG, "stopTest() start");
        mIsTesting = false;
        mHasTestError = false;
        testSignal();

        if (availablePathList.isEmpty()) {
            Log.i(TAG, "stopTest() return");
            return;
        }

        BufferedWriter writer = null;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(testTargetPath);
            sb.append("/");
            sb.append("temp_iptv.url");
            writer = new BufferedWriter(new FileWriter(sb.toString(), true));
            for (String path : availablePathList) {
                writer.write(path);
                writer.newLine();
            }
            sb = null;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.flush();
                } catch (IOException e) {
                }
                try {
                    writer.close();
                } catch (IOException e) {
                }
            }
            writer = null;
        }

        availablePathList.clear();
        Log.i(TAG, "stopTest() end");
    }

    ////////////////////////////////////////////////////////////////////////////////////

    public static final int STATE_IDLE = 0;
    public static final int STATE_PREPARING = 1;
    public static final int STATE_PLAY_START = 3;

    public static final int STATE_PREPARED = 2;
    public static final int STATE_PLAYING = 4;
    public static final int STATE_PAUSED = 5;
    public static final int STATE_STOPPED = 6;
    public static final int STATE_SEEKING = 7;

    private int mIid = -1;
    private IDmrPlayerAppCallback mIDmrPlayerAppCallback = null;

    public void registerCallback(int iid, IDmrPlayerAppCallback cb) {
        mIid = iid;
        mIDmrPlayerAppCallback = cb;
        /*if (mIDmrPlayerAppCallback != null) {
            try {
                mIDmrPlayerAppCallback.onError(0, 1, "which_window");
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }*/
    }

    public void unregisterCallback(int iid, IDmrPlayerAppCallback cb) {
        mIid = -1;
        mIDmrPlayerAppCallback = null;
    }

    public void playForDlna(int iid) {
        if (mWdPlayer != null && mIDmrPlayerAppCallback != null) {
            if (mWdPlayer.isRunning() && !mWdPlayer.isPlaying()) {
                mWdPlayer.play();
                try {
                    Log.i(TAG, "playForDlna() onState STATE_PLAYING");
                    mIDmrPlayerAppCallback.onState(iid, STATE_PLAYING);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void pauseForDlna(int iid) {
        if (mWdPlayer != null && mIDmrPlayerAppCallback != null) {
            if (mWdPlayer.isRunning() && mWdPlayer.isPlaying()) {
                mWdPlayer.pause();
                try {
                    Log.i(TAG, "pauseForDlna() onState STATE_PAUSED");
                    mIDmrPlayerAppCallback.onState(iid, STATE_PAUSED);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void stopForDlna(int iid) {
        if (mWdPlayer != null && mIDmrPlayerAppCallback != null) {
            if (mWdPlayer.isRunning()) {
                mPrePath = null;
                mWdPlayer.release();
                try {
                    Log.i(TAG, "stopForDlna() onState STATE_STOPPED");
                    mIDmrPlayerAppCallback.onState(iid, STATE_STOPPED);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void seekToForDlna(int iid, int msec) {
        if (mWdPlayer != null && mIDmrPlayerAppCallback != null) {
            mWdPlayer.seekTo(msec / 1000);
            try {
                Log.i(TAG, "seekToForDlna() onState STATE_SEEKING");
                mIDmrPlayerAppCallback.onState(iid, STATE_SEEKING);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public int getCurrentPositionForDlna(int iid) {
        return (int) mPresentationTime * 1000;
    }

    public int getDurationForDlna(int iid) {
        return (int) mMediaDuration * 1000;
    }

}

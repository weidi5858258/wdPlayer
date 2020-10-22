package com.weidi.media.wdplayer.video_player;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.app.UiModeManager;
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
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
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
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.media.wdplayer.R;
import com.weidi.media.wdplayer.util.Callback;
import com.weidi.media.wdplayer.util.JniObject;
import com.weidi.threadpool.ThreadPool;
import com.weidi.utils.MyToast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
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

import androidx.core.content.ContextCompat;

import static com.weidi.media.wdplayer.Constants.PLAYBACK_ADDRESS;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_IS_MUTE;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_MEDIA_TYPE;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_NORMAL_FINISH;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_SHOW_CONTROLLERPANELLAYOUT;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_USE_PLAYER;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_WINDOW_POSITION;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_WINDOW_POSITION_TAG;
import static com.weidi.media.wdplayer.Constants.PLAYER_FFMPEG;
import static com.weidi.media.wdplayer.Constants.PLAYER_FFMPEG_MEDIACODEC;
import static com.weidi.media.wdplayer.Constants.PLAYER_MEDIACODEC;
import static com.weidi.media.wdplayer.Constants.PREFERENCES_NAME;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_audioHandleData;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_download;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_frameByFrame;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_frameByFrameForFinish;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_frameByFrameForReady;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_getDuration;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_init;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_initPlayer;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_isPlaying;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_isRunning;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_isWatch;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_isWatchForCloseAudio;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_isWatchForCloseVideo;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_pause;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_play;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_readData;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_seekTo;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_setMode;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_setSurface;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_stepAdd;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_stepSubtract;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_videoHandleData;
import static com.weidi.media.wdplayer.video_player.FFMPEG.USE_MODE_AAC_H264;
import static com.weidi.media.wdplayer.video_player.FFMPEG.USE_MODE_AUDIO_VIDEO;
import static com.weidi.media.wdplayer.video_player.FFMPEG.VOLUME_MUTE;
import static com.weidi.media.wdplayer.video_player.FFMPEG.VOLUME_NORMAL;

/***
 /Users/alexander/mydev/workspace_android/wdPlayer/gradle/wrapper/gradle-wrapper.properties
 https://www.kagura.me/dev/20200828131600.html
 distributionUrl=https://code.aliyun.com/kar/gradle-bin-zip/raw/master/gradle-6.5-bin.zip
 distributionUrl=https\://services.gradle.org/distributions/gradle-6.5-bin.zip
 */
public class PlayerWrapper {

    private static final String TAG = "player_alexander";

    public static boolean IS_PHONE = false;
    public static boolean IS_WATCH = false;
    public static boolean IS_TV = false;
    public static boolean IS_HIKEY970 = false;

    private static final int MSG_CHANGE_COLOR = 10;
    private static final int MSG_START_PLAYBACK = 11;
    private static final int MSG_SEEK_TO_ADD = 12;
    private static final int MSG_SEEK_TO_SUBTRACT = 13;
    private static final int MSG_DOWNLOAD = 14;
    private static final int MSG_LOAD_CONTENTS = 15;

    private HashMap<String, Long> mPathTimeMap = new HashMap<>();
    private ArrayList<String> mCouldPlaybackPathList = new ArrayList<>();

    private SharedPreferences mSP;
    private PowerManager.WakeLock mPowerWakeLock;
    private SurfaceHolder mSurfaceHolder;
    private FFMPEG mFFMPEGPlayer;
    private SimpleVideoPlayer mSimpleVideoPlayer;
    //private SimplePlayer mSimpleVideoPlayer;
    private FfmpegUseMediaCodecDecode mFfmpegUseMediaCodecDecode;
    private boolean mIsAddedView = false;
    private String mPrePath;
    private String mCurPath;
    private String md5Path;
    // 有些mp3文件含有video,因此播放失败
    private String mType;
    private long mProgress;
    // 单位秒
    private long mPresentationTime;
    private int mDownloadProgress = -1;
    private long contentLength = -1;
    private boolean mNeedToSyncProgressBar = true;
    private boolean mIsScreenPress = false;
    private boolean mHasError = false;
    private boolean mIsSeparatedAudioVideo = false;
    private long mMediaDuration;
    private boolean mIsLocal = true;
    private boolean mIsH264 = false;
    private boolean mIsVideo = false;
    private boolean mIsAudio = false;
    private SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;
    private View mRootView;

    private AudioManager mAudioManager;
    private AudioFocusRequest mAudioFocusRequest;
    private int minVolume;
    private int maxVolume;

    private SurfaceView mSurfaceView;
    private LinearLayout mControllerPanelLayout;
    private ProgressBar mLoadingView;
    private SeekBar mProgressBar;
    private TextView mFileNameTV;
    private TextView mProgressTimeTV;
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
    // 音视频的加载进度
    private LinearLayout mProgressBarLayout;
    private ProgressBar mVideoProgressBar;
    private ProgressBar mAudioProgressBar;
    private int mProgressBarLayoutHeight;

    private Context mContext;
    private PlayerService mService;

    /***
     Configuration.UI_MODE_TYPE_NORMAL     手机
     Configuration.UI_MODE_TYPE_WATCH      手表
     Configuration.UI_MODE_TYPE_TELEVISION 电视机
     */
    private int whatIsDevice;
    private boolean mIsPhoneDevice;
    // 是否是竖屏 true为竖屏
    private boolean mIsPortraitScreen;

    private String whatPlayer = PLAYER_FFMPEG_MEDIACODEC;

    // 第一个存储视频地址,第二个存储标题
    public static final LinkedHashMap<String, String> mContentsMap = new LinkedHashMap();
    public static LinkedHashMap<String, String> mLocalVideoContentsMap;
    public static LinkedHashMap<String, String> mLocalAudioContentsMap;
    // 当mShuffle = Shuffle.Shuffle_On;时,保存已经播放过的文件
    private ArrayList<Integer> mLocalContentsHasPlayedList;
    private Random mRandom;

    public enum Repeat {
        Repeat_Off, Repeat_All, Repeat_One
    }

    public enum Shuffle {
        Shuffle_Off, Shuffle_On
    }

    // 关闭重复播放
    private Repeat mRepeat = Repeat.Repeat_Off;
    // 关闭随机播放
    private Shuffle mShuffle = Shuffle.Shuffle_Off;
    // 当mShuffle == Shuffle.Shuffle_Off时,上一首,下一首才有效
    private boolean mPlayPrevFile = false;
    private boolean mPlayNextFile = false;

    // 必须首先被调用
    public void setService(Service service) {
        mService = null;
        if (!(service instanceof PlayerService)) {
            throw new IllegalArgumentException("!(service instanceof PlayerService)");
        }

        PlayerService playerService = (PlayerService) service;
        mService = playerService;
        mContext = playerService.getApplicationContext();

        if (mSP == null) {
            mSP = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        }

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
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
            case Configuration.UI_MODE_TYPE_NORMAL:
                IS_PHONE = true;
                break;
            case Configuration.UI_MODE_TYPE_WATCH:
                IS_WATCH = true;
                break;
            case Configuration.UI_MODE_TYPE_TELEVISION:
                IS_TV = true;
                break;
            default:
                IS_PHONE = true;
                break;
        }

        LayoutInflater inflater =
                (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (!IS_WATCH) {
            mRootView = inflater.inflate(R.layout.media_player, null);
        } else {
            mRootView = inflater.inflate(R.layout.media_player_wear, null);
        }
        mLayoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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

        mRootView.setOnTouchListener(new PlayerOnTouchListener());

        mSurfaceView = mRootView.findViewById(R.id.surfaceView);
        mControllerPanelLayout = mRootView.findViewById(R.id.controller_panel_layout);
        mLoadingView = mRootView.findViewById(R.id.loading_view);
        mProgressBar = mRootView.findViewById(R.id.progress_bar);
        mFileNameTV = mRootView.findViewById(R.id.file_name_tv);
        mProgressTimeTV = mRootView.findViewById(R.id.progress_time_tv);
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
        mRepeatOff = mRootView.findViewById(R.id.button_repeat_off);
        mRepeatAll = mRootView.findViewById(R.id.button_repeat_all);
        mRepeatOne = mRootView.findViewById(R.id.button_repeat_one);
        mShuffleOff = mRootView.findViewById(R.id.button_shuffle_off);
        mShuffleOn = mRootView.findViewById(R.id.button_shuffle_on);
        mDownloadTV = mRootView.findViewById(R.id.download_tv);

        mProgressBarLayout = mRootView.findViewById(R.id.progress_bar_layout);
        mVideoProgressBar = mRootView.findViewById(R.id.video_progress_bar);
        mAudioProgressBar = mRootView.findViewById(R.id.audio_progress_bar);

        textInfoScrollView = mRootView.findViewById(R.id.text_scrollview);
        textInfoTV = mRootView.findViewById(R.id.text_info_tv);

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
        mRepeatOff.setOnClickListener(mOnClickListener);
        mRepeatAll.setOnClickListener(mOnClickListener);
        mRepeatOne.setOnClickListener(mOnClickListener);
        mShuffleOff.setOnClickListener(mOnClickListener);
        mShuffleOn.setOnClickListener(mOnClickListener);

        mPlayIB.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int curVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                mAudioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        curVolume,
                        AudioManager.FLAG_SHOW_UI);
                return true;
            }
        });

        mPauseIB.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int curVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                mAudioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        curVolume,
                        AudioManager.FLAG_SHOW_UI);
                return true;
            }
        });

        if (!IS_WATCH) {
            mSurfaceView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int curVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    mAudioManager.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            curVolume,
                            AudioManager.FLAG_SHOW_UI);
                    return true;
                }
            });
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
    public synchronized void setDataSource(String path) {
        if (TextUtils.isEmpty(path)) {
            Log.i(TAG, "setDataSource() path is null");
            return;
        }
        if (TextUtils.equals(path, mCurPath) && mPrePath != null) {
            Log.i(TAG, "setDataSource() path:\n" + path + "\n正在播放中......");
            return;
        }

        mCurPath = path;
        mPrePath = path;
        Log.i(TAG, "setDataSource() mPrePath:\n" + mPrePath);
        Log.i(TAG, "setDataSource() mCurPath:\n" + mCurPath);

        addView();
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
        EventBusUtils.register(this);

        mPathTimeMap.clear();
        mContentsMap.clear();
        mCouldPlaybackPathList.clear();

        mIsPhoneDevice = isPhoneDevice();
        if (mFFMPEGPlayer == null) {
            mFFMPEGPlayer = FFMPEG.getDefault();
        }
        if (mFfmpegUseMediaCodecDecode == null) {
            mFfmpegUseMediaCodecDecode = new FfmpegUseMediaCodecDecode();
        }
        mFFMPEGPlayer.setContext(mContext);
        mFFMPEGPlayer.setFfmpegUseMediaCodecDecode(mFfmpegUseMediaCodecDecode);
        mFfmpegUseMediaCodecDecode.setContext(mContext);
        if (IS_WATCH) {
            mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_isWatchForCloseAudio, null);
        }

        /*if (mGetMediaFormat == null) {
            mGetMediaFormat = new GetMediaFormat();
        }
        mGetMediaFormat.setContext(mContext);
        mGetMediaFormat.setPlayerWrapper(this);*/
        //mFfmpegUseMediaCodecDecode.setGetMediaFormat(mGetMediaFormat);

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

        sendMessageForLoadContents();

        int duration = (int) mMediaDuration;
        int currentPosition = (int) mPresentationTime;
        float pos = (float) currentPosition / duration;
        int target = Math.round(pos * mProgressBar.getMax());
        mProgressBar.setProgress(target);
        mProgressBar.setSecondaryProgress(0);
        mProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
                if (fromTouch && !isFrameByFrameMode) {
                    // 得到的是秒
                    long tempProgress =
                            (long) ((progress / 3840.00) * mMediaDuration);
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
            }
        });
        mProgressBar.setOnTouchListener(new View.OnTouchListener() {
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
                        if (!mIsH264) {
                            Log.d(TAG, "MotionEvent.ACTION_UP mProgress: " + mProgress +
                                    " " + DateUtils.formatElapsedTime(mProgress));
                        } else {
                            Log.d(TAG, "MotionEvent.ACTION_UP mProgress: " + mProgress);
                        }
                        if (mProgress >= 0 && mProgress <= mMediaDuration) {
                            if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
                                mSimpleVideoPlayer.setProgressUs(mProgress * 1000000);
                            } else {
                                mFFMPEGPlayer.onTransact(
                                        DO_SOMETHING_CODE_seekTo,
                                        JniObject.obtain().writeLong(mProgress));
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
        });

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            minVolume = mAudioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC);
            maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int curVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            Log.i(TAG, "onCreate() minVolume: " + minVolume);// 0
            Log.i(TAG, "onCreate() maxVolume: " + maxVolume);// 15
            Log.i(TAG, "onCreate() curVolume: " + curVolume);
        }

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

        if (mSurfaceHolder == null) {
            mSurfaceHolder = mSurfaceView.getHolder();
            // 没有图像出来,就是由于没有设置PixelFormat.RGBA_8888
            // 这里要写
            mSurfaceHolder.setFormat(PixelFormat.RGBA_8888);
            mSurfaceHolder.addCallback(mSurfaceCallback);
        }
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
        onRelease();
        mSimpleVideoPlayer = null;
        mFFMPEGPlayer = null;

        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }
        EventBusUtils.unregister(this);
    }

    private void onRelease() {
        if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
            if (mSimpleVideoPlayer != null) {
                mSimpleVideoPlayer.release();
            }
        } else {
            if (mFFMPEGPlayer != null) {
                mFFMPEGPlayer.releaseAll();
            }
        }
    }

    private void addView() {
        Log.i(TAG, "addView()    mIsAddedView: " + mIsAddedView);
        if (mIsAddedView) {
            mIsAddedView = false;
            onPause();
            mWindowManager.removeView(mRootView);
            return;
        }

        mIsAddedView = true;
        onResume();
        mWindowManager.addView(mRootView, mLayoutParams);

        getMD5ForPath();
    }

    public void removeView() {
        Log.i(TAG, "removeView() mIsAddedView: " + mIsAddedView);
        mPrePath = null;
        if (mIsAddedView) {
            onPause();
            mWindowManager.removeView(mRootView);
            mIsAddedView = false;
        }
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
        Log.i(TAG, "setControllerPanelBackgroundColor()");
        if (//mContext.getResources().getConfiguration().orientation
            // != Configuration.ORIENTATION_PORTRAIT
            //|| mControllerPanelLayout.getVisibility() != View.VISIBLE
            //||
                !mIsAddedView) {
            Log.i(TAG, "setControllerPanelBackgroundColor() return");
            return;
        }

        if (mColorsHasUsedList == null)
            mColorsHasUsedList = new ArrayList<>();
        if (mRandom == null)
            mRandom = new Random();
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

        if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT
                || mIsAudio) {
            // 竖屏
            mControllerPanelLayout.setBackgroundColor(
                    ContextCompat.getColor(mContext, targetColor));
        }
        if (mIsVideo) {
            textInfoTV.setTextColor(
                    ContextCompat.getColor(mContext, targetColor));
        }
        if (!IS_WATCH) {
            ObjectAnimator controllerPanelAnimator =
                    ObjectAnimator.ofFloat(mControllerPanelLayout, "alpha", 0f, 1f);
            ObjectAnimator textInfoAnimator =
                    ObjectAnimator.ofFloat(textInfoTV, "alpha", 0f, 1f);
            if (mIsLocal) {
                controllerPanelAnimator.setDuration(5000);
                textInfoAnimator.setDuration(5000);
            } else {
                controllerPanelAnimator.setDuration(8000);
                textInfoAnimator.setDuration(8000);
            }
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
            BatteryManager batteryManager =
                    (BatteryManager) mContext.getSystemService(Context.BATTERY_SERVICE);
            int battery =
                    batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
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

    private boolean needToPlaybackOtherVideo() {
        if (mPrePath != null) {
            addView();
            Log.i(TAG, "needToPlaybackOtherVideo() return true for mPrePath != null");
            return true;
        } else if (IS_PHONE || IS_WATCH) {
            if (!mIsAddedView || !mRootView.isShown()) {
                Log.i(TAG, "needToPlaybackOtherVideo() return false" +
                        " for mRootView isn't showed");
                mUiHandler.removeMessages(MSG_CHANGE_COLOR);
                return false;
            }

            if (mRepeat == Repeat.Repeat_Off) {
                Log.i(TAG, "needToPlaybackOtherVideo() return false" +
                        " for mRepeat == Repeat.Repeat_Off");
                return false;
            }

            if (!allowToPlayback()) {
                Log.i(TAG, "needToPlaybackOtherVideo() return false" +
                        " for doesn't allowToPlayback");
                return false;
            }

            if (mRepeat == Repeat.Repeat_One
                    || mLocalAudioContentsMap.size() <= 1
                    || mIsVideo) {
                startForGetMediaFormat();
                return true;
            }

            // region mRepeat == Repeat.Repeat_All
            // 按mLocalAudioContentsMap顺序播放
            if (mShuffle == Shuffle.Shuffle_Off) {
                if (!mPlayPrevFile && !mPlayNextFile) {
                    mPlayNextFile = true;
                }
                int index = -1;
                int prevPathIndex = -2;
                int nextPathIndex = -2;
                String prevPath = null;
                for (Map.Entry<String, String> tempMap : mLocalAudioContentsMap.entrySet()) {
                    index++;
                    if (TextUtils.equals(tempMap.getKey(), mCurPath)) {
                        if (mPlayPrevFile) {
                            if (index == 0) {
                                prevPathIndex = mLocalAudioContentsMap.size() - 1;
                            }
                            break;
                        } else if (mPlayNextFile) {
                            nextPathIndex = index;
                            if (nextPathIndex == mLocalAudioContentsMap.size() - 1) {
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
                        for (Map.Entry<String, String> tempMap :
                                mLocalAudioContentsMap.entrySet()) {
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
                        for (Map.Entry<String, String> tempMap :
                                mLocalAudioContentsMap.entrySet()) {
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

            // region mShuffle == Shuffle.Shuffle_On
            // 按mLocalAudioContentsMap随机播放
            if (mLocalContentsHasPlayedList == null)
                mLocalContentsHasPlayedList = new ArrayList<>();
            if (mRandom == null)
                mRandom = new Random();
            int size = mLocalAudioContentsMap.size();
            for (; ; ) {
                int randomNumber = mRandom.nextInt(size);
                if (!mLocalContentsHasPlayedList.contains(randomNumber)) {
                    mLocalContentsHasPlayedList.add(randomNumber);
                    int index = -1;
                    for (Map.Entry<String, String> tempMap : mLocalAudioContentsMap.entrySet()) {
                        if (++index == randomNumber) {
                            mPrePath = mCurPath;
                            mCurPath = tempMap.getKey();
                            getMD5ForPath();
                            break;
                        }
                    }
                    break;
                } else {
                    if (mLocalContentsHasPlayedList.size() >= size) {
                        mLocalContentsHasPlayedList.clear();
                    }
                }
            }
            startForGetMediaFormat();
            return true;
            // endregion
        }

        Log.i(TAG, "needToPlaybackOtherVideo() return false");
        // 不需要播放另一个视频
        return false;
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
                Log.d(TAG, "Callback.MSG_ON_TRANSACT_INFO");
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
                    default:
                        break;
                }

                clickCounts = 0;
                break;
            case MSG_CHANGE_COLOR:
                setControllerPanelBackgroundColor();
                break;
            case MSG_START_PLAYBACK:
                if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
                    /*ThreadPool.getFixedThreadPool().execute(new Runnable() {
                        @Override
                        public void run() {
                            mSimpleVideoPlayer.handleData(mSimpleVideoPlayer.mAudioWrapper);
                        }
                    });
                    ThreadPool.getFixedThreadPool().execute(new Runnable() {
                        @Override
                        public void run() {
                            mSimpleVideoPlayer.handleData(mSimpleVideoPlayer.mVideoWrapper);
                        }
                    });*/
                } else {
                    ThreadPool.getFixedThreadPool().execute(new Runnable() {
                        @Override
                        public void run() {
                            sendEmptyMessage(DO_SOMETHING_CODE_audioHandleData);
                        }
                    });
                    ThreadPool.getFixedThreadPool().execute(new Runnable() {
                        @Override
                        public void run() {
                            sendEmptyMessage(DO_SOMETHING_CODE_videoHandleData);
                        }
                    });

                    ThreadPool.getFixedThreadPool().execute(new Runnable() {
                        @Override
                        public void run() {
                            SystemClock.sleep(500);
                            sendEmptyMessage(DO_SOMETHING_CODE_readData);
                        }
                    });
                    if (mIsSeparatedAudioVideo) {
                        ThreadPool.getFixedThreadPool().execute(new Runnable() {
                            @Override
                            public void run() {
                                SystemClock.sleep(1000);
                                sendEmptyMessage(DO_SOMETHING_CODE_readData);
                            }
                        });
                    }
                }
                break;
            case MSG_SEEK_TO_ADD:
                if (IS_WATCH) {
                    mAudioManager.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            (int) addStep,
                            AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                    addStep = 0;
                    break;
                }
                if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
                    mSimpleVideoPlayer.setProgressUs(
                            mSimpleVideoPlayer.getCurrentPosition() + addStep * 1000000);
                } else {
                    if (mFFMPEGPlayer != null) {
                        mFFMPEGPlayer.onTransact(
                                DO_SOMETHING_CODE_stepAdd,
                                JniObject.obtain().writeLong(addStep));
                    }
                }
                addStep = 0;
                break;
            case MSG_SEEK_TO_SUBTRACT:
                if (IS_WATCH) {
                    mAudioManager.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            (int) subtractStep,
                            AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                    subtractStep = 0;
                    break;
                }
                if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
                    mSimpleVideoPlayer.setProgressUs(
                            mSimpleVideoPlayer.getCurrentPosition() - subtractStep * 1000000);
                } else {
                    if (mFFMPEGPlayer != null) {
                        mFFMPEGPlayer.onTransact(
                                DO_SOMETHING_CODE_stepSubtract,
                                JniObject.obtain().writeLong(subtractStep));
                    }
                }
                subtractStep = 0;
                break;
            default:
                break;
        }
    }

    private void threadHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        switch (msg.what) {
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
                        mFFMPEGPlayer.onTransact(
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
                            mFFMPEGPlayer.onTransact(
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
                                mFFMPEGPlayer.onTransact(
                                        DO_SOMETHING_CODE_download,
                                        JniObject.obtain()
                                                .writeInt(4)
                                                .writeStringArray(
                                                        new String[]{path, sb.toString()}));
                            } else {
                                // 只提取音视频,不播放.调用seekTo到0
                                mFFMPEGPlayer.onTransact(
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
                loadContents();
                break;
            default:
                break;
        }
    }

    // @@@
    private void startForGetMediaFormat() {
        whatPlayer = mSP.getString(PLAYBACK_USE_PLAYER, PLAYER_FFMPEG_MEDIACODEC);
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
    }

    private void startPlayback() {
        Log.d(TAG, "startPlayback() start");
        if (!mIsAddedView || !mRootView.isShown() || TextUtils.isEmpty(mCurPath)) {
            Log.e(TAG, "startPlayback() The condition is not satisfied");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
                    Log.i(TAG, "startPlayback() AudioManager.AUDIOFOCUS_REQUEST_FAILED");
                    break;
                case AudioManager.AUDIOFOCUS_REQUEST_GRANTED:
                    // 开始播放
                    Log.i(TAG, "startPlayback() AudioManager.AUDIOFOCUS_REQUEST_GRANTED");
                    break;
                case AudioManager.AUDIOFOCUS_REQUEST_DELAYED:
                    Log.i(TAG, "startPlayback() AudioManager.AUDIOFOCUS_REQUEST_DELAYED");
                    break;
                default:
                    break;
            }
        }

        mSurfaceHolder = mSurfaceView.getHolder();
        // 这里也要写
        mSurfaceHolder.setFormat(PixelFormat.RGBA_8888);

        // 底层有关参数的设置
        mFFMPEGPlayer.setHandler(mUiHandler);
        if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
            mSimpleVideoPlayer = new SimpleVideoPlayer();
            //mSimpleVideoPlayer = new SimplePlayer();
            mSimpleVideoPlayer.setContext(mContext);
            mSimpleVideoPlayer.setHandler(mUiHandler);
            mSimpleVideoPlayer.setCallback(mFFMPEGPlayer.mCallback);
        }

        /*new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                return null;
            }
            @Override
            protected void onPostExecute(Void result) {
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);*/

        // 开启线程初始化ffmpeg
        ThreadPool.getFixedThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "startPlayback()                  mPath: " + mCurPath);

                // region 判断是什么样的文件(一般用于本地文件)

                String tempPath = "";
                mIsSeparatedAudioVideo = false;
                if (mCurPath.endsWith(".m4s")) {
                    tempPath = mCurPath.substring(0, mCurPath.lastIndexOf("/"));
                    File audioFile = new File(tempPath + "/audio.m4s");
                    File videoFile = new File(tempPath + "/video.m4s");
                    Log.d(TAG,
                            "startPlayback()                  audio: " + audioFile.getAbsolutePath());
                    Log.d(TAG,
                            "startPlayback()                  video: " + videoFile.getAbsolutePath());
                    if (audioFile.exists() && videoFile.exists()) {
                        mIsSeparatedAudioVideo = true;
                    }
                } else if (mCurPath.endsWith(".h264") || mCurPath.endsWith(".aac")) {
                    tempPath = mCurPath.substring(0, mCurPath.lastIndexOf("/"));
                    String fileName = mCurPath.substring(
                            mCurPath.lastIndexOf("/") + 1, mCurPath.lastIndexOf("."));
                    Log.d(TAG, "startPlayback()               fileName: " + fileName);
                    StringBuilder sb = new StringBuilder(tempPath);
                    sb.append("/");
                    sb.append(fileName);
                    sb.append(".aac");
                    File audioFile = new File(sb.toString());
                    sb = new StringBuilder(tempPath);
                    sb.append("/");
                    sb.append(fileName);
                    sb.append(".h264");
                    File videoFile = new File(sb.toString());
                    Log.d(TAG,
                            "startPlayback()                  audio: " + audioFile.getAbsolutePath());
                    Log.d(TAG,
                            "startPlayback()                  video: " + videoFile.getAbsolutePath());
                    if (audioFile.exists() && videoFile.exists()) {
                        mIsSeparatedAudioVideo = true;
                    }
                }
                Log.d(TAG, "startPlayback() mIsSeparatedAudioVideo: " + mIsSeparatedAudioVideo);

                // endregion

                if (mIsSeparatedAudioVideo
                        && TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
                    MyToast.show(PLAYER_MEDIACODEC + "模式下不能播放");
                    if (mService != null) {
                        removeView();
                        if (mSurfaceHolder != null) {
                            mSurfaceHolder.removeCallback(mSurfaceCallback);
                            mSurfaceHolder = null;
                        }
                    }
                    return;
                }

                if (!mIsSeparatedAudioVideo) {
                    if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
                        mSimpleVideoPlayer.setSurface(mSurfaceHolder.getSurface());
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            mSimpleVideoPlayer.setDataSource(mCurPath);
                        }
                        /*if (!mSimpleVideoPlayer.initPlayer()) {
                            return;
                        }*/
                    } else {
                        sendEmptyMessage(DO_SOMETHING_CODE_init);
                        mFfmpegUseMediaCodecDecode.mType = mType;
                        if (mIsVideo) {
                            if (!mCurPath.endsWith(".h264")) {
                                mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_setMode,
                                        JniObject.obtain().writeInt(
                                                FFMPEG.USE_MODE_MEDIA_MEDIACODEC));
                            } else {
                                mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_setMode,
                                        JniObject.obtain().writeInt(
                                                FFMPEG.USE_MODE_ONLY_VIDEO));
                            }
                        } else if (mIsAudio) {
                            mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_setMode,
                                    JniObject.obtain().writeInt(FFMPEG.USE_MODE_ONLY_AUDIO));
                        }
                    }

                    if (mPathTimeMap.containsKey(md5Path)) {
                        long position = mPathTimeMap.get(md5Path);
                        Log.d(TAG, "startPlayback()               position: " + position);

                        if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
                            //mSimpleVideoPlayer.setProgressUs(position * 1000000);
                        } else {
                            mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_seekTo,
                                    JniObject.obtain().writeLong(position));
                        }
                    }
                } else {
                    sendEmptyMessage(DO_SOMETHING_CODE_init);
                    // [.m4s] or [.h264 and .aac](达不到同步效果)
                    if (mCurPath.endsWith(".m4s")) {
                        mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_setMode,
                                JniObject.obtain().writeInt(USE_MODE_AUDIO_VIDEO));
                    } else {
                        mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_setMode,
                                JniObject.obtain().writeInt(USE_MODE_AAC_H264));
                    }
                }

                if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
                } else {
                    mFfmpegUseMediaCodecDecode.setSurface(mSurfaceHolder.getSurface());
                    mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_isWatch,
                            JniObject.obtain()
                                    .writeBoolean(whatIsDevice != Configuration.UI_MODE_TYPE_WATCH ? false : true));
                    mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_setSurface,
                            JniObject.obtain()
                                    .writeString(mCurPath)
                                    .writeObject(mSurfaceHolder.getSurface()));

                    if (Integer.parseInt(mFFMPEGPlayer.onTransact(
                            DO_SOMETHING_CODE_initPlayer, null)) != 0) {
                        // 不在这里做事了.遇到error会从底层回调到java端的
                        //MyToast.show("音视频初始化失败");
                        //mUiHandler.removeMessages(Callback.MSG_ON_ERROR);
                        //mUiHandler.sendEmptyMessage(Callback.MSG_ON_ERROR);
                        return;
                    }
                }

                mUiHandler.removeMessages(MSG_START_PLAYBACK);
                mUiHandler.sendEmptyMessage(MSG_START_PLAYBACK);
                Log.d(TAG, "startPlayback() end");
            }
        });
    }

    // 执行全屏和取消全屏的方法
    private void setFullscreen(Activity context, boolean fullscreen) {
        Window window = context.getWindow();
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
        // getStatusBarHeight() height: 48 95
        Log.d(TAG, "getStatusBarHeight() height: " + height);
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
        Log.d(TAG, "getNavigationBarHeight() height: " + height);
        return height;
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

        mIsPortraitScreen = false;
        mRootView.setBackgroundColor(
                mContext.getResources().getColor(R.color.black));
        mControllerPanelLayout.setBackgroundColor(
                mContext.getResources().getColor(android.R.color.transparent));

        // 暂停按钮高度
        getPauseRlHeight();

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

        // 生产,消耗进度条高度
        mProgressBarLayoutHeight = mProgressBarLayout.getHeight();
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW     mProgressBarLayoutHeight: " +
                mProgressBarLayoutHeight);
        if (mProgressBarLayoutHeight > 0) {
            RelativeLayout.LayoutParams relativeParams =
                    (RelativeLayout.LayoutParams) mProgressBarLayout.getLayoutParams();
            relativeParams.setMargins(
                    (mScreenWidth - mNeedVideoWidth) / 2, (mScreenHeight - mNeedVideoHeight) / 2,
                    0, 0);
            relativeParams.width = mNeedVideoWidth;
            relativeParams.height = mProgressBarLayoutHeight;
            mProgressBarLayout.setLayoutParams(relativeParams);
        }

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
            frameParams.setMargins((mScreenWidth - mNeedVideoWidth) / 2, 40, 0, 0);
            frameParams.width = mNeedVideoWidth;
        } else {
            frameParams.setMargins((mScreenWidth - mScreenHeight) / 2, 40, 0, 0);
            frameParams.width = mScreenHeight;
        }
        frameParams.height = mControllerPanelLayoutHeight;
        mControllerPanelLayout.setLayoutParams(frameParams);

        if (mService != null) {
            if (statusBarHeight != 0) {
                updateRootViewLayout(mScreenWidth, mScreenHeight - statusBarHeight);
            } else {
                updateRootViewLayout(mScreenWidth, mScreenHeight);
            }
        }
    }

    // 处理竖屏
    public void handlePortraitScreen() {
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW handlePortraitScreen");
        if (!mIsAddedView) {
            return;
        }

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
        int pauseRlHeight = getPauseRlHeight();

        // mScreenWidth: 1080 mScreenHeight: 2244
        // 屏幕宽高
        DisplayMetrics displayMetrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW                 mScreenWidth: " +
                mScreenWidth + " mScreenHeight: " + mScreenHeight);

        // 生产,消耗进度条高度
        mProgressBarLayoutHeight = mProgressBarLayout.getHeight();
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW     mProgressBarLayoutHeight: " +
                mProgressBarLayoutHeight);
        if (mProgressBarLayoutHeight > 0) {
            RelativeLayout.LayoutParams relativeParams =
                    (RelativeLayout.LayoutParams) mProgressBarLayout.getLayoutParams();
            relativeParams.setMargins(0, 0, 0, 0);
            relativeParams.width = mScreenWidth;
            relativeParams.height = mProgressBarLayoutHeight;
            mProgressBarLayout.setLayoutParams(relativeParams);
        }

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
        } else {
            mNeedVideoWidth = mScreenWidth;
            mNeedVideoHeight = 1;
        }
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
        if (mNeedVideoHeight > (int) (mScreenHeight * 2 / 3)) {
            if (mNeedVideoHeight < mScreenHeight) {
                frameParams.setMargins(
                        0, mNeedVideoHeight - mControllerPanelLayoutHeight - 10, 0, 0);
            } else {
                frameParams.setMargins(
                        0, getStatusBarHeight(), 0, 0);
            }
            mControllerPanelLayout.setBackgroundColor(
                    mContext.getResources().getColor(android.R.color.transparent));
        } else {
            frameParams.setMargins(
                    0, mNeedVideoHeight, 0, 0);
            /*mControllerPanelLayout.setBackgroundColor(
                    mContext.getResources().getColor(R.color.lightgray));*/
        }
        frameParams.width = mScreenWidth;
        frameParams.height = mControllerPanelLayoutHeight;
        mControllerPanelLayout.setLayoutParams(frameParams);

        if (mService != null) {
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                if (mNeedVideoHeight > (int) (mScreenHeight * 2 / 3)) {
                    updateRootViewLayout(
                            mScreenWidth,
                            mNeedVideoHeight, x, y);
                } else {
                    if (mMediaDuration <= 0) {
                        // 直播节目
                        updateRootViewLayout(
                                mScreenWidth,
                                mNeedVideoHeight + pauseRlHeight, x, y);
                    } else {
                        updateRootViewLayout(
                                mScreenWidth,
                                mNeedVideoHeight + mControllerPanelLayoutHeight, x, y);
                    }
                }
            } else {
                if (mIsVideo) {
                    if (mMediaDuration <= 0) {
                        // 是视频并且只下载不播放的情况下
                        updateRootViewLayout(mScreenWidth, pauseRlHeight, x, y);
                        return;
                    }
                }
                if (mMediaDuration > 0) {
                    // 音乐 或者 mMediaDuration > 0
                    updateRootViewLayout(mScreenWidth, mControllerPanelLayoutHeight + 1, x, y);
                } else {
                    updateRootViewLayout(mScreenWidth, pauseRlHeight + 1, x, y);
                }
            }
        }
    }

    // 电视机专用
    public void handlePortraitScreenWithTV() {
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW handlePortraitScreenWithTV");
        if (mVideoWidth == 0 || mVideoHeight == 0 || !mIsAddedView) {
            return;
        }

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

        int pauseRlHeight = getPauseRlHeight();

        // mScreenWidth: 1080 mScreenHeight: 2244
        // 屏幕宽高
        DisplayMetrics displayMetrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW                 mScreenWidth: " +
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
        } else {
            mNeedVideoWidth = mScreenWidth;
            mNeedVideoHeight = 1;
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
        if (mNeedVideoHeight > (int) (mScreenHeight * 2 / 3)) {
            frameParams.setMargins(0, getStatusBarHeight(), 0, 0);
            mControllerPanelLayout.setBackgroundColor(
                    mContext.getResources().getColor(android.R.color.transparent));
        } else {
            frameParams.setMargins(0, mNeedVideoHeight, 0, 0);
            /*mControllerPanelLayout.setBackgroundColor(
                    mContext.getResources().getColor(R.color.lightgray));*/
        }
        frameParams.width = mScreenWidth;
        frameParams.height = mControllerPanelLayoutHeight;
        mControllerPanelLayout.setLayoutParams(frameParams);

        if (mService != null) {
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                if (mNeedVideoHeight > (int) (mScreenHeight * 2 / 3)) {
                    updateRootViewLayout(mScreenWidth, mNeedVideoHeight, x, y);
                } else {
                    if (mMediaDuration <= 0) {
                        updateRootViewLayout(mScreenWidth,
                                mNeedVideoHeight + pauseRlHeight, x, y);
                    } else {
                        updateRootViewLayout(mScreenWidth,
                                mNeedVideoHeight + mControllerPanelLayoutHeight, x, y);
                    }
                }
            } else {
                updateRootViewLayout(mScreenWidth, mControllerPanelLayoutHeight + 1, x, y);
            }
        }
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
        mControllerPanelLayout.setBackgroundColor(
                mContext.getResources().getColor(android.R.color.transparent));

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

        if (mService != null) {
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
        if (mService != null) {
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                frameParams.setMargins(0, getStatusBarHeight(), 0, 0);
            } else {
                frameParams.setMargins(0, 0, 0, 0);
            }
        }
        frameParams.width = mScreenWidth;
        frameParams.height = mControllerPanelLayoutHeight;
        mControllerPanelLayout.setLayoutParams(frameParams);

        if (mService != null) {
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

    private void onReady() {
        // 是否显示控制面板
        if (mIsVideo) {
            if (!mIsLocal) {
                if (!IS_WATCH) {
                    mLoadingView.setVisibility(View.VISIBLE);
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
        setControllerPanelBackgroundColor();
        mProgressTimeTV.setText("");
        mDurationTimeTV.setText("");
        mProgressBar.setProgress(0);
        mProgressBar.setPadding(0, 0, 0, 0);
        mProgressBar.setThumbOffset(0);
        // 左边进度值
        mVideoProgressBar.setProgress(0);
        // 右边进度值
        mVideoProgressBar.setSecondaryProgress(0);
        mAudioProgressBar.setProgress(0);
        mAudioProgressBar.setSecondaryProgress(0);
        mProgressBarLayout.setVisibility(View.GONE);
        mPlayIB.setVisibility(View.VISIBLE);
        mPauseIB.setVisibility(View.INVISIBLE);
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
        if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
            mMediaDuration = mSimpleVideoPlayer.getDurationUs() / 1000000;
        } else {
            mMediaDuration = Long.parseLong(
                    mFFMPEGPlayer.onTransact(
                            DO_SOMETHING_CODE_getDuration, null));
        }
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW               mMediaDuration: " +
                mMediaDuration);
        Log.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW                   videoWidth: " +
                mVideoWidth + " videoHeight: " + mVideoHeight);

        if (!mIsH264) {
            mDurationTimeTV.setText(DateUtils.formatElapsedTime(mMediaDuration));
        } else {
            mDurationTimeTV.setText(String.valueOf(mMediaDuration));
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
                if (!mIsLocal) {
                    mProgressBarLayout.setVisibility(View.VISIBLE);
                }
                if (!mCouldPlaybackPathList.contains(mCurPath)) {
                    mCouldPlaybackPathList.add(mCurPath);
                }
            }

            if (mVideoWidth == 0 && mVideoHeight == 0) {
                setType("audio/");
                mControllerPanelLayout.setVisibility(View.VISIBLE);
                textInfoScrollView.setVisibility(View.GONE);
                mProgressBarLayout.setVisibility(View.GONE);
            }
        }

        if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT
                && IS_PHONE) {
            // Log.i(TAG, "Callback.MSG_ON_CHANGE_WINDOW 手机");
            // 手机并且竖屏
            handlePortraitScreen();
        } else {
            // Log.i(TAG, "Callback.MSG_ON_CHANGE_WINDOW 电视机");
            if (IS_WATCH) {
                if (mIsVideo) {
                    // 全屏
                    handleLandscapeScreen(0);
                } else {
                    handlePortraitScreen();
                }
            } else {
                // 手机并且横屏,电视机
                handleScreenFlag = 1;
                handlePortraitScreenWithTV();
            }
        }

        SharedPreferences.Editor edit = mSP.edit();
        // 保存播放地址
        edit.putString(PLAYBACK_ADDRESS, mCurPath);
        edit.putString(PLAYBACK_MEDIA_TYPE, mType);
        // 开始播放设置为false,表示初始化状态
        edit.putBoolean(PLAYBACK_NORMAL_FINISH, false);
        edit.commit();
    }

    private void onPlayed() {
        mPlayIB.setVisibility(View.VISIBLE);
        mPauseIB.setVisibility(View.INVISIBLE);
        if (!IS_WATCH) {
            mLoadingView.setVisibility(View.GONE);
        } else {
            if (mIsVideo) {
                MyToast.show("Play");
            }
        }
    }

    private void onPaused() {
        //mPlayIB.setVisibility(View.INVISIBLE);
        //mPauseIB.setVisibility(View.VISIBLE);
        if (!mIsLocal) {
            if (!IS_WATCH) {
                mLoadingView.setVisibility(View.VISIBLE);
            } else {
                if (mIsVideo) {
                    MyToast.show("Pause");
                }
            }
        }
    }

    private void onFinished() {
        if (mFfmpegUseMediaCodecDecode != null)
            mFfmpegUseMediaCodecDecode.releaseMediaCodec();

        if (mHasError) {
            mHasError = false;
            Log.d(TAG, "onFinished() restart playback");
            // 重新开始播放
            startForGetMediaFormat();
        } else {
            MyToast.show("Safe Exit");

            // 播放结束
            if (!needToPlaybackOtherVideo()) {
                removeView();
                if (mSurfaceHolder != null) {
                    mSurfaceHolder.removeCallback(mSurfaceCallback);
                    mSurfaceHolder = null;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mAudioManager.abandonAudioFocusRequest(mAudioFocusRequest);
                }
                System.gc();
            }
        }

        mSP.edit().putBoolean(PLAYBACK_NORMAL_FINISH, true).commit();
    }

    private void inInfo(Message msg) {
        if (msg.obj != null && msg.obj instanceof String) {
            String toastInfo = ((String) msg.obj).trim();
            //Log.d(TAG, "Callback.MSG_ON_TRANSACT_INFO\n" + toastInfo);
            if (toastInfo.contains("[")
                    && toastInfo.contains("]")) {
                textInfoTV.setText(toastInfo);
            } else if (toastInfo.contains("AVERROR_EOF")) {
                mPrePath = null;
            } else {
                MyToast.show(toastInfo);
            }
        }
    }

    private void onError(Message msg) {
        mHasError = false;
        String errorInfo = null;
        if (msg.obj != null) {
            errorInfo = (String) msg.obj;
        }
        int error = msg.arg1;
        switch (error) {
            case Callback.ERROR_TIME_OUT:
                //case Callback.ERROR_DATA_EXCEPTION:
                Log.e(TAG, "PlayerWrapper Callback.ERROR_TIME_OUT errorInfo: " + errorInfo);
                // 需要重新播放
                mHasError = true;
                break;
            case Callback.ERROR_FFMPEG_INIT:
                Log.e(TAG, "PlayerWrapper Callback.ERROR_FFMPEG_INIT errorInfo: " + errorInfo);
                if (mIsVideo) {
                    if (mCouldPlaybackPathList.contains(mCurPath)
                            && !mCurPath.startsWith("http://cache.m.iqiyi.com/")) {
                        // startPlayback();
                        startForGetMediaFormat();
                        break;
                    } else {
                        String path = mSP.getString(PLAYBACK_ADDRESS, null);
                        if (TextUtils.equals(path, mCurPath)
                                && !mCurPath.startsWith("http://cache.m.iqiyi.com/")) {
                            // startPlayback();
                            startForGetMediaFormat();
                            break;
                        }
                    }
                }

                MyToast.show("音视频初始化失败");
                // 不需要重新播放
                /*Log.i(TAG, "PlayerWrapper Callback.ERROR_FFMPEG_INIT " +
                        "mService.removeView()");*/
                removeView();
                if (mSurfaceHolder != null) {
                    mSurfaceHolder.removeCallback(mSurfaceCallback);
                    mSurfaceHolder = null;
                }
                break;
            default:
                break;
        }
    }

    private void onUpdated(Message msg) {
        // 秒
        mPresentationTime = (Long) msg.obj;
        if (!mIsH264) {
            mProgressTimeTV.setText(DateUtils.formatElapsedTime(mPresentationTime));
        } else {
            mProgressTimeTV.setText(String.valueOf(mPresentationTime));
        }

        if (mMediaDuration > 0) {
            if (mNeedToSyncProgressBar) {
                int currentPosition = (int) (mPresentationTime);
                float pos = (float) currentPosition / mMediaDuration;
                int target = Math.round(pos * mProgressBar.getMax());
                mProgressBar.setProgress(target);
            }

            if (mPresentationTime < (mMediaDuration - 5)) {
                mPathTimeMap.put(md5Path, mPresentationTime);
                if (mIsH264 && (mMediaDuration - mPresentationTime <= 1000000)) {
                    mPathTimeMap.remove(md5Path);
                }
            } else {
                // 正常结束就不需要播放了
                mPrePath = null;
                if (mPathTimeMap.containsKey(md5Path)) {
                    mPathTimeMap.remove(md5Path);
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
        if (mService != null) {
            RelativeLayout pause_rl = mRootView.findViewById(R.id.pause_rl);
            pauseRlHeight = pause_rl.getHeight();
            SeekBar progress_bar = mRootView.findViewById(R.id.progress_bar);
            RelativeLayout show_time_rl = mRootView.findViewById(R.id.show_time_rl);
            ImageButton button_fr = mRootView.findViewById(R.id.button_fr);
            ImageButton button_ff = mRootView.findViewById(R.id.button_ff);
            ImageButton button_prev = mRootView.findViewById(R.id.button_prev);
            ImageButton button_next = mRootView.findViewById(R.id.button_next);
            ImageButton button_repeat_off = mRootView.findViewById(R.id.button_repeat_off);
            ImageButton button_repeat_all = mRootView.findViewById(R.id.button_repeat_all);
            ImageButton button_repeat_one = mRootView.findViewById(R.id.button_repeat_one);
            ImageButton button_shuffle_off = mRootView.findViewById(R.id.button_shuffle_off);
            ImageButton button_shuffle_on = mRootView.findViewById(R.id.button_shuffle_on);
            if (mMediaDuration <= 0 && !mIsH264) {
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

    private long addStep = 0;
    private long subtractStep = 0;
    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_fr:
                    if (!isFrameByFrameMode) {
                        if (IS_WATCH) {
                            if (subtractStep == 0) {
                                int curVolume =
                                        mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                                subtractStep = curVolume - 1;
                            } else {
                                subtractStep -= 1;
                            }
                            if (subtractStep < minVolume) {
                                subtractStep = minVolume;
                            }
                            MyToast.show(String.valueOf(subtractStep));
                            mUiHandler.removeMessages(MSG_SEEK_TO_SUBTRACT);
                            mUiHandler.sendEmptyMessageDelayed(MSG_SEEK_TO_SUBTRACT, 1000);
                            return;
                        }
                        if (mFFMPEGPlayer != null) {
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
                        }
                    } else {
                        if (mFFMPEGPlayer != null) {
                            mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_frameByFrame, null);
                        }
                    }
                    break;
                case R.id.button_ff:
                    if (!isFrameByFrameMode) {
                        if (IS_WATCH) {
                            if (addStep == 0) {
                                int curVolume =
                                        mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                                addStep = curVolume + 1;
                            } else {
                                addStep += 1;
                            }
                            if (addStep > maxVolume) {
                                addStep = maxVolume;
                            }
                            MyToast.show(String.valueOf(addStep));
                            mUiHandler.removeMessages(MSG_SEEK_TO_ADD);
                            mUiHandler.sendEmptyMessageDelayed(MSG_SEEK_TO_ADD, 1000);
                            return;
                        }
                        if (mFFMPEGPlayer != null) {
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
                        }
                    } else {
                        if (mFFMPEGPlayer != null) {
                            mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_frameByFrame, null);
                        }
                    }
                    break;
                case R.id.button_prev:
                    mPlayPrevFile = true;
                    mPlayNextFile = false;
                    mPrePath = null;
                    onRelease();
                    break;
                case R.id.button_next:
                    mPlayPrevFile = false;
                    mPlayNextFile = true;
                    mPrePath = null;
                    onRelease();
                    break;
                case R.id.button_play:
                    if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
                        if (mSimpleVideoPlayer.isRunning()) {
                            if (mSimpleVideoPlayer.isPlaying()) {
                                mPlayIB.setVisibility(View.INVISIBLE);
                                mPauseIB.setVisibility(View.VISIBLE);
                                mSimpleVideoPlayer.pause();
                            }
                        }
                    } else {
                        if (mFFMPEGPlayer != null) {
                            if (Boolean.parseBoolean(mFFMPEGPlayer.onTransact(
                                    DO_SOMETHING_CODE_isRunning, null))) {
                                mPlayIB.setVisibility(View.INVISIBLE);
                                mPauseIB.setVisibility(View.VISIBLE);
                                sendEmptyMessage(DO_SOMETHING_CODE_pause);
                            }
                        }
                    }
                    break;
                case R.id.button_pause:
                    if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
                        if (mSimpleVideoPlayer.isRunning()) {
                            if (!mSimpleVideoPlayer.isPlaying()) {
                                mPlayIB.setVisibility(View.VISIBLE);
                                mPauseIB.setVisibility(View.INVISIBLE);
                                mSimpleVideoPlayer.play();
                            }
                        }
                    } else {
                        if (mFFMPEGPlayer != null) {
                            if (isFrameByFrameMode) {
                                isFrameByFrameMode = false;
                                mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_frameByFrameForFinish,
                                        null);
                                mVolumeNormal.setVisibility(View.VISIBLE);
                                mVolumeMute.setVisibility(View.INVISIBLE);
                                mFFMPEGPlayer.setVolume(VOLUME_NORMAL);
                                mFfmpegUseMediaCodecDecode.setVolume(VOLUME_NORMAL);
                                mSP.edit().putBoolean(PLAYBACK_IS_MUTE, false).commit();
                                MyToast.show("帧模式已关闭");
                            }
                            mPlayIB.setVisibility(View.VISIBLE);
                            mPauseIB.setVisibility(View.INVISIBLE);
                            if (!IS_WATCH) {
                                mLoadingView.setVisibility(View.GONE);
                            }
                            sendEmptyMessage(DO_SOMETHING_CODE_play);
                        }
                    }
                    break;
                case R.id.surfaceView:
                    mIsScreenPress = true;
                    onEvent(KeyEvent.KEYCODE_HEADSETHOOK, null);
                    break;
                case R.id.button_exit:
                    mDownloadClickCounts = 0;
                    mIsDownloading = false;
                    isFrameByFrameMode = false;
                    // 表示用户主动关闭,不需要再继续播放
                    removeView();
                    break;
                case R.id.volume_normal:
                    mVolumeNormal.setVisibility(View.INVISIBLE);
                    mVolumeMute.setVisibility(View.VISIBLE);
                    if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
                        mSimpleVideoPlayer.setVolume(VOLUME_MUTE);
                    } else {
                        mFFMPEGPlayer.setVolume(VOLUME_MUTE);
                        mFfmpegUseMediaCodecDecode.setVolume(VOLUME_MUTE);
                    }
                    mSP.edit().putBoolean(PLAYBACK_IS_MUTE, true).commit();
                    break;
                case R.id.volume_mute:
                    if (isFrameByFrameMode) {
                        return;
                    }
                    mVolumeNormal.setVisibility(View.VISIBLE);
                    mVolumeMute.setVisibility(View.INVISIBLE);
                    if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
                        mSimpleVideoPlayer.setVolume(VOLUME_NORMAL);
                    } else {
                        mFFMPEGPlayer.setVolume(VOLUME_NORMAL);
                        mFfmpegUseMediaCodecDecode.setVolume(VOLUME_NORMAL);
                    }
                    mSP.edit().putBoolean(PLAYBACK_IS_MUTE, false).commit();
                    break;
                case R.id.button_repeat_off:
                    MyToast.show("Repeat All");
                    mRepeat = Repeat.Repeat_All;
                    setRepeatView();
                    break;
                case R.id.button_repeat_all:
                    MyToast.show("Repeat One");
                    mRepeat = Repeat.Repeat_One;
                    setRepeatView();
                    break;
                case R.id.button_repeat_one:
                    MyToast.show("Repeat Off");
                    mRepeat = Repeat.Repeat_Off;
                    setRepeatView();
                    break;
                case R.id.button_shuffle_off:
                    MyToast.show("Shuffle On");
                    mShuffle = Shuffle.Shuffle_On;
                    setShuffleView();
                    break;
                case R.id.button_shuffle_on:
                    MyToast.show("Shuffle Off");
                    mShuffle = Shuffle.Shuffle_Off;
                    setShuffleView();
                    break;
                case R.id.download_tv:
                    if (TextUtils.isEmpty(mDownloadTV.getText())) {
                        mDownloadTV.setText("0");
                        mDownloadTV.setBackgroundColor(
                                mContext.getResources().getColor(R.color.burlywood));
                        return;
                    }
                    mDownloadClickCounts++;
                    mThreadHandler.removeMessages(MSG_DOWNLOAD);
                    mThreadHandler.sendEmptyMessageDelayed(MSG_DOWNLOAD, 1000);
                    break;
                default:
                    break;
            }
        }
    };

    private void clickOne() {
        if (mFFMPEGPlayer == null) {
            return;
        }
        if (!Boolean.parseBoolean(
                mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_isRunning, null))) {
            return;
        }

        mUiHandler.removeMessages(MSG_CHANGE_COLOR);
        if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE// 横屏
                || Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (mControllerPanelLayout.getVisibility() == View.VISIBLE) {
                mControllerPanelLayout.setVisibility(View.GONE);
                textInfoScrollView.setVisibility(View.GONE);
                mSP.edit().putBoolean(PLAYBACK_SHOW_CONTROLLERPANELLAYOUT, false).commit();
            } else {
                mControllerPanelLayout.setVisibility(View.VISIBLE);
                textInfoScrollView.setVisibility(View.VISIBLE);
                mSP.edit().putBoolean(PLAYBACK_SHOW_CONTROLLERPANELLAYOUT, true).commit();
                mUiHandler.sendEmptyMessageDelayed(MSG_CHANGE_COLOR, 60 * 1000);
            }
            return;
        }

        if (textInfoScrollView.getVisibility() == View.VISIBLE) {
            textInfoScrollView.setVisibility(View.GONE);
        } else {
            textInfoScrollView.setVisibility(View.VISIBLE);
            mUiHandler.sendEmptyMessageDelayed(MSG_CHANGE_COLOR, 60 * 1000);
        }
    }

    private void clickTwo() {
        if (mFFMPEGPlayer == null) {
            return;
        }
        if (!Boolean.parseBoolean(
                mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_isRunning, null))) {
            return;
        }

        // 播放与暂停
        if (Boolean.parseBoolean(mFFMPEGPlayer.onTransact(
                DO_SOMETHING_CODE_isPlaying, null))) {
            mPlayIB.setVisibility(View.INVISIBLE);
            mPauseIB.setVisibility(View.VISIBLE);
            sendEmptyMessage(DO_SOMETHING_CODE_pause);
        } else {
            mPlayIB.setVisibility(View.VISIBLE);
            mPauseIB.setVisibility(View.INVISIBLE);
            if (!IS_WATCH) {
                mLoadingView.setVisibility(View.GONE);
            }
            sendEmptyMessage(DO_SOMETHING_CODE_play);
        }
    }

    private void clickThree() {
        if (mFFMPEGPlayer == null) {
            return;
        }
        if (!Boolean.parseBoolean(
                mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_isRunning, null))) {
            if (mIsVideo) {
                setType("video/");
            } else if (mIsAudio) {
                setType("audio/");
            }
            EventBusUtils.post(
                    PlayerService.class,
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
                mLoadingView.setVisibility(View.VISIBLE);
            }
        }
        sendEmptyMessage(DO_SOMETHING_CODE_pause);
        MyToast.show("Pause");
    }

    private int handleScreenFlag = 1;

    @SuppressLint("SourceLockedOrientationActivity")
    private void clickFour() {
        if (mFFMPEGPlayer == null) {
            return;
        }
        if (!Boolean.parseBoolean(
                mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_isRunning, null))) {
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
                    handleScreenFlag = 2;
                    handleLandscapeScreen(0);
                    break;
                case 2:
                    handleScreenFlag = 3;
                    handleLandscapeScreen(1);
                    break;
                case 3:
                    handleScreenFlag = 1;
                    handlePortraitScreenWithTV();
                    break;
                default:
                    break;
            }
        }
    }

    private void clickFive() {
        if (mFFMPEGPlayer == null) {
            return;
        }
        if (!Boolean.parseBoolean(
                mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_isRunning, null))) {
            return;
        }

        isFrameByFrameMode = Boolean.parseBoolean(mFFMPEGPlayer.onTransact(
                DO_SOMETHING_CODE_frameByFrameForReady, null));
        if (isFrameByFrameMode) {
            // 静音
            mVolumeNormal.setVisibility(View.INVISIBLE);
            mVolumeMute.setVisibility(View.VISIBLE);
            mFFMPEGPlayer.setVolume(VOLUME_MUTE);
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

    private void clickSix() {
        String whatPlayer = mSP.getString(PLAYBACK_USE_PLAYER, PLAYER_FFMPEG_MEDIACODEC);
        if (TextUtils.equals(whatPlayer, PLAYER_FFMPEG_MEDIACODEC)) {
            MyToast.show(PLAYER_FFMPEG);
            mSP.edit().putString(PLAYBACK_USE_PLAYER, PLAYER_FFMPEG).commit();
        } else {
            MyToast.show(PLAYER_FFMPEG_MEDIACODEC);
            mSP.edit().putString(PLAYBACK_USE_PLAYER, PLAYER_FFMPEG_MEDIACODEC).commit();
        }
    }

    private void clickSeven() {
        if (mFfmpegUseMediaCodecDecode != null) {
            if (mFfmpegUseMediaCodecDecode.mUseMediaCodecForAudio) {
                mFfmpegUseMediaCodecDecode.mUseMediaCodecForAudio = false;
                MyToast.show("不使用音频硬解码");
            } else {
                mFfmpegUseMediaCodecDecode.mUseMediaCodecForAudio = true;
                MyToast.show("使用音频硬解码");
            }
        }
    }

    // 关闭音频部分
    private void clickEight() {
        if (mFFMPEGPlayer == null) {
            return;
        }

        mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_isWatchForCloseAudio, null);
    }

    // 关闭视频部分
    private void clickNine() {
        if (mFFMPEGPlayer == null) {
            return;
        }

        mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_isWatchForCloseVideo, null);
        /*Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(JniPlayerActivity.COMMAND_NO_FINISH, true);
        intent.setClass(mContext, JniPlayerActivity.class);
        mContext.startActivity(intent);*/
    }

    private void clickTen() {
        if (mFFMPEGPlayer == null) {
            return;
        }
        if (!Boolean.parseBoolean(
                mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_isRunning, null))) {
            return;
        }

        onRelease();
    }

    private boolean isPhoneDevice() {
        boolean isPhoneDevice = true;
        UiModeManager uiModeManager =
                (UiModeManager) mContext.getSystemService(Context.UI_MODE_SERVICE);
        if (uiModeManager.getCurrentModeType() != Configuration.UI_MODE_TYPE_TELEVISION) {
            isPhoneDevice = true;
        } else {
            isPhoneDevice = false;
        }
        return isPhoneDevice;
    }

    private void sendEmptyMessage(int code) {
        if (mFFMPEGPlayer != null) {
            mFFMPEGPlayer.onTransact(code, null);
        }
    }

    private void loadContents() {
        Log.i(TAG, "loadContents() start");
        mContentsMap.clear();
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
                if (!f.getAbsolutePath().startsWith("/storage/emulated/0")) {
                    file = f;
                    rootDir = f.getAbsolutePath();
                    break;
                }
            }
        }
        if (file == null) {
            return;
        }

        Log.i(TAG, "Environment.MEDIA_SHARED: " + file.getAbsolutePath());
        StringBuilder sb = new StringBuilder();
        sb.append(file.getAbsolutePath());
        sb.append("/");
        sb.append("contents.txt");
        file = new File(sb.toString());
        if (file.exists()) {
            readContents(file);
            //return;
        } else {
            if (copyFile(file)) {
                loadContents();
                return;
            }
        }

        if (IS_PHONE || IS_WATCH) {
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
                sb.append("/Movies");
                // /storage/emulated/0/Movies/
                // Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
                file = new File(sb.toString());
                Log.i(TAG, "loadContents()      file: " + file.getAbsolutePath());
                saveLocalFile("video", file);

                sb.delete(0, sb.length());
                sb.append(rootDir);
                sb.append("/Music");
                // /storage/emulated/0/Music/
                // Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
                file = new File(sb.toString());
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

    private boolean copyFile(File targetFile) {
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
                String filePath = file.getAbsolutePath();
                // /contents.txt
                // /hw_pc_white_apps.xml
                // /wifipro_regexlist.xml
                Log.i(TAG, "getAssets               : " + filePath);
                if (file.getAbsolutePath().contains("contents.")) {
                    InputStream is = mContext.getAssets().open(filePath);
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

    private void readContents(File file) {
        final String TAG = "@@@@@@@@@@";
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String aLineContent = null;
            String[] contents = null;
            String key = null;
            String value = null;
            StringBuilder sb = new StringBuilder();
            int i = 0;
            //一次读一行，读入null时文件结束
            while ((aLineContent = reader.readLine()) != null) {
                if (aLineContent.length() == 0) {
                    continue;
                }

                aLineContent = aLineContent.trim();

                if (aLineContent.contains(TAG) && !aLineContent.startsWith("#")) {
                    ++i;
                    contents = aLineContent.split(TAG);
                    key = contents[0];
                    value = contents[1];

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

                    if (contents.length > 1) {
                        if (!mContentsMap.containsKey(key)) {
                            mContentsMap.put(key, sb.toString());
                        } else {
                            --i;
                        }
                    }

                    /*Log.i("player_alexander", "readContents() sb.toString(): " +
                            sb.toString());*/
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

    private static int length(String s) {
        if (s == null) {
            return 0;
        }
        char[] c = s.toCharArray();
        int len = 0;
        for (int i = 0; i < c.length; i++) {
            len++;
            if (!isLetter(c[i])) {
                len++;
            }
        }
        return len;
    }

    private static boolean isLetter(char c) {
        int k = 0x80;
        return c / k == 0 ? true : false;
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
                    if (mFFMPEGPlayer != null) {
                        mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_frameByFrame, null);
                    }
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
            default:
                break;
        }
        return result;
    }

    public void playPlayerWithTelephonyCall() {
        if (mFFMPEGPlayer != null) {
            if (Boolean.parseBoolean(mFFMPEGPlayer.onTransact(
                    DO_SOMETHING_CODE_isRunning, null))) {
                boolean isMute = mSP.getBoolean(PLAYBACK_IS_MUTE, false);
                if (!isMute) {
                    mFFMPEGPlayer.setVolume(VOLUME_NORMAL);
                    mVolumeNormal.setVisibility(View.VISIBLE);
                    mVolumeMute.setVisibility(View.INVISIBLE);
                } else {
                    mFFMPEGPlayer.setVolume(VOLUME_MUTE);
                    mVolumeNormal.setVisibility(View.INVISIBLE);
                    mVolumeMute.setVisibility(View.VISIBLE);
                }

                if (!Boolean.parseBoolean(mFFMPEGPlayer.onTransact(
                        DO_SOMETHING_CODE_isPlaying, null))) {
                    mPlayIB.setVisibility(View.VISIBLE);
                    mPauseIB.setVisibility(View.INVISIBLE);
                    sendEmptyMessage(DO_SOMETHING_CODE_play);
                }
            }
        }
    }

    public void pausePlayerWithTelephonyCall() {
        if (mFFMPEGPlayer != null) {
            if (Boolean.parseBoolean(mFFMPEGPlayer.onTransact(
                    DO_SOMETHING_CODE_isRunning, null))) {
                mPlayIB.setVisibility(View.INVISIBLE);
                mPauseIB.setVisibility(View.VISIBLE);
                sendEmptyMessage(DO_SOMETHING_CODE_pause);
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
            if (!mIsPortraitScreen) {
                return true;
            }
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
                    mWindowManager.updateViewLayout(view, mLayoutParams);
                    break;
                case MotionEvent.ACTION_UP:
                    sb.delete(0, sb.length());
                    sb.append(tempX);
                    sb.append(PLAYBACK_WINDOW_POSITION_TAG);
                    sb.append(tempY);
                    mSP.edit().putString(PLAYBACK_WINDOW_POSITION, sb.toString()).commit();
                    Log.i(TAG, "Callback.MSG_ON_CHANGE_WINDOW : " + sb.toString());
                    break;
                default:
                    break;
            }
            return true;
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

}

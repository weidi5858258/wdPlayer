package com.weidi.media.wdplayer.video_player;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;


import com.weidi.threadpool.ThreadPool;
import com.weidi.media.wdplayer.util.AVPacket;
import com.weidi.media.wdplayer.util.Callback;
import com.weidi.media.wdplayer.util.MediaUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static com.weidi.media.wdplayer.Constants.PLAYBACK_IS_MUTE;
import static com.weidi.media.wdplayer.Constants.PREFERENCES_NAME;
import static com.weidi.media.wdplayer.video_player.FFMPEG.USE_MODE_ONLY_VIDEO;

/***
 Created by weidi on 2019/8/6.

 亲手写过这个程序后,最最最关键的点
 我认为是时间戳问题.
 如果播放本地视频,几乎不需要时间缓存,从头手播到尾,那么几乎没什么问题,音视频几乎达到同步.
 但是经过seek,pause后,音视频就会发生不同步的现象.

 现在有的问题:
 视频:
 1.花屏(原因:在某些视频帧上多读或者少读几个字节,就会造成花屏.已解决)
 2.播放速度过快(queueInputBuffer()方法中的presentationTimeUs参数设置不正确,也就是时间戳不正确.已解决)
 3.一方pause时,另一方没有pause
 4.seek
 5.pause or play
 6.prev or nextHandle
 7.sound控制
 8.屏幕明亮控制

 总体流程:
 音频视频各有2个缓存用于存数据,各有1个缓存用于处理数据

 缓存数据:
 程序开始时称填满缓存1.
 然后
 缓存1满缓存2未满则缓存2;
 缓存1未满缓存2满则缓存1;
 缓存1满缓存2满则等待,用一个标志标记哪个缓存先满.

 处理数据:
 从缓存中copy过来的数据都是一帧帧的音视频帧.
 因此只要截取每一帧然后进行解码就行了.

 mp4,flv,rtmp不需要adts头
 hls,rtp,ts需要adts头

 android的无法分离高profile的h264文件,分离的是Baseline（profile）

 “video/x-vnd.on2.vp8” - VP8 video (i.e. video in .webm)
 “video/x-vnd.on2.vp9” - VP9 video (i.e. video in .webm)
 “video/mp4v-es”       - MPEG4 video (mp4)
 “video/3gpp”          - H.263 video
 “video/avc”           - H.264/AVC video
 “audio/mp4a-latm”     - AAC audio (note, this is raw AAC packets, not packaged in LATM!)
 “audio/3gpp”          - AMR narrowband audio
 “audio/amr-wb”        - AMR wideband audio
 “audio/mpeg”          - MPEG1/2 audio layer III (mp3)
 “audio/vorbis”        - vorbis audio
 “audio/g711-alaw”     - G.711 alaw audio
 “audio/g711-mlaw”     - G.711 ulaw audio
 “text/vtt”
 “text/cea-608”
 “text/cea-708”
 */

public class SimpleVideoPlayer {

    private static final String TAG =
            "player_alexander";
    //SimpleVideoPlayer.class.getSimpleName();
    private static final boolean DEBUG = true;

    public static final int MAX_CACHE_AUDIO_COUNT = 600;// 5000
    public static final int MAX_CACHE_VIDEO_COUNT = 60;

    private static final int START_CACHE_COUNT_HTTP = 30;// 3000
    private static final int START_CACHE_COUNT_LOCAL = 30;

    // 只是设置为默认值
    // 音频一帧的大小不能超过这个值,不然出错(如果设成1024 * 1024会有杂音,不能过大,调查了好久才发现跟这个有关)
    private static final int AUDIO_FRAME_MAX_LENGTH = 1024 * 100;// 102400
    // 视频一帧的大小不能超过这个值,不然出错
    private static final int VIDEO_FRAME_MAX_LENGTH = 1024 * 1024 * 5;// 5242880

    public static final int TYPE_AUDIO = 0x0001;
    public static final int TYPE_VIDEO = 0x0002;
    private static final int TIME_OUT = 10000;

    /*private static final int PREPARE = 0x0001;
    private static final int PLAY = 0x0002;
    private static final int PAUSE = 0x0003;
    private static final int STOP = 0x0004;
    private static final int RELEASE = 0x0005;
    private static final int PREV = 0x0006;
    private static final int NEXT = 0x0007;*/
    // Internal messages
    private static final int MSG_PREPARE = 0;
    private static final int MSG_SET_PLAY_WHEN_READY = 1;
    private static final int MSG_DO_SOME_WORK = 2;
    private static final int MSG_SEEK_TO = 3;
    private static final int MSG_SET_PLAYBACK_PARAMETERS = 4;
    private static final int MSG_SET_SEEK_PARAMETERS = 5;
    private static final int MSG_PLAY = 20;
    private static final int MSG_PAUSE = 21;
    private static final int MSG_STOP = 6;
    private static final int MSG_RELEASE = 7;
    private static final int MSG_REFRESH_SOURCE_INFO = 8;
    private static final int MSG_PERIOD_PREPARED = 9;
    private static final int MSG_SOURCE_CONTINUE_LOADING_REQUESTED = 10;
    private static final int MSG_TRACK_SELECTION_INVALIDATED = 11;
    private static final int MSG_SET_REPEAT_MODE = 12;
    private static final int MSG_SET_SHUFFLE_ENABLED = 13;
    private static final int MSG_SET_FOREGROUND_MODE = 14;
    private static final int MSG_SEND_MESSAGE = 15;
    private static final int MSG_SEND_MESSAGE_TO_TARGET_THREAD = 16;
    private static final int MSG_PLAYBACK_PARAMETERS_CHANGED_INTERNAL = 17;
    private static final int MSG_HANDLE_DATA_LOCK_WAIT = 18;
    private static final int MSG_SEEK_TO_NOTIFY = 19;
    private static final int MSG_GAME_OVER = 22;

    private static final int STATUS_READ_DATA1_STARTED = 0x0001;
    private static final int STATUS_READ_DATA2_STARTED = 0x0002;
    private static final int STATUS_READ_DATA_PAUSED = 0x0003;
    private static final int STATUS_READ_FINISHED = 0x0004;
    private static final int STATUS_READ_ERROR = 0x0005;

    // 为了注册广播
    private Context mContext = null;
    private String mPath = null;
    //private float mVolume = 1.0f;
    private boolean mIsLocal = false;
    private MediaExtractor extractor = null;

    private Handler mUiHandler = null;
    private Handler mThreadHandler = null;
    private HandlerThread mHandlerThread = null;

    private Callback mCallback = null;
    private Surface mSurface = null;

    private boolean isReading = false;
    private Object readDataLock = new Object();
    //private Object handleDataLock = new Object();
    public AudioWrapper mAudioWrapper;
    public VideoWrapper mVideoWrapper;
    private int use_mode = FFMPEG.USE_MODE_MEDIA;

    public static class SimpleWrapper {
        public String mime = null;
        public MediaCodec decoderMediaCodec = null;
        public MediaFormat decoderMediaFormat = null;
        // 是否需要渲染图像(播放音频为false,播放视频为true)
        public boolean render = false;
        public int trackIndex = -1;
        // 使用于while条件判断
        public boolean isHandling = false;
        // readData用于缓存数据,handleData用于处理数据
        public ArrayList<AVPacket> readData = null;

        public int CACHE;
        public int CACHE_START;
        // 用于标识音频还是视频
        public int type;
        // 总时长
        public long durationUs = 0;
        // 播放的时长(下面两个参数一起做的事是每隔一秒发一次回调函数)
        public long sampleTime = 0;
        public long startTimeUs = 0;

        // 使用于时间戳
        public long presentationTimeUs = 0;

        //        // 下面4个变量在没有seek过的情况下统计是正确的,seek过后就不正确了
        //        // "块"的概念,一"块"有N个字节.
        //        // 编码时接收的数据就是一"块"的大小,少一个字节或者多一个字节都会出现异常
        //        public long readFrameCounts = 0;
        //        // 处理了多少"块"
        //        public long handleFrameCounts = 0;
        //        // 使用readSampleData(...)方法总共读了多少字节
        //        public long readFrameLengthTotal = 0;
        //        // 处理了多少字节
        //        public long handleFrameLengthTotal = 0;


        public boolean isStarted = false;
        // 因为user所以pause
        public boolean isPausedForUser = false;
        // 因为cache所以pause
        public boolean isPausedForCache = false;
        // 因为seek所以pause
        public boolean isPausedForSeek = false;
        public Object readDataLock = new Object();
        public Object handleDataLock = new Object();

        // 一帧音频或者视频的最大值
        public int frameMaxLength = 0;
        // 音频或者视频一帧的实际大小
        public int size = 0;
        // 放一帧音频或者视频的容器
        public byte[] data = null;

        // seekTo
        public long progressUs = -1;
        public boolean needToSeek = false;

        private SimpleWrapper() {
        }

        public SimpleWrapper(int type) {
            switch (type) {
                case TYPE_AUDIO:
                    this.type = TYPE_AUDIO;
                    CACHE = MAX_CACHE_AUDIO_COUNT;
                    frameMaxLength = AUDIO_FRAME_MAX_LENGTH;
                    break;
                case TYPE_VIDEO:
                    this.type = TYPE_VIDEO;
                    CACHE = MAX_CACHE_VIDEO_COUNT;
                    frameMaxLength = VIDEO_FRAME_MAX_LENGTH;
                    break;
                default:
                    break;
            }
        }

        public void clear() {
            mime = null;
            decoderMediaCodec = null;
            decoderMediaFormat = null;
            render = false;
            trackIndex = -1;
            //isReading = false;
            isStarted = false;
            isHandling = false;
            isPausedForUser = false;
            isPausedForCache = false;
            isPausedForSeek = false;
            //readData.clear();
            //handleData.clear();
            durationUs = 0;
            sampleTime = 0;
            startTimeUs = 0;
            presentationTimeUs = 0;
            //frameMaxLength = 0;
            size = 0;

            /*progressUs = -1;
            needToSeek = false;*/
        }
    }

    private static class AudioWrapper extends SimpleWrapper {
        public AudioTrack mAudioTrack = null;

        private AudioWrapper() {
        }

        public AudioWrapper(int type) {
            super(type);
        }

        public void clear() {
            MediaUtils.releaseAudioTrack(mAudioTrack);
            MediaUtils.releaseMediaCodec(decoderMediaCodec);
            super.clear();
        }
    }

    public static class VideoWrapper extends SimpleWrapper {
        public Surface mSurface = null;
        public int width = 0;
        public int height = 0;

        private VideoWrapper() {
        }

        public VideoWrapper(int type) {
            super(type);
        }

        public void clear() {
            MediaUtils.releaseMediaCodec(decoderMediaCodec);
            super.clear();
        }
    }

    public SimpleVideoPlayer() {
        // init();
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public void setHandler(Handler handler) {
        mUiHandler = handler;
    }

    public void setDataSource(String path) {
        mPath = path;
    }

    public void setSurface(Surface surface) {
        mSurface = surface;
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public void setVolume(float volume) {
        if (mAudioWrapper.mAudioTrack == null
                || mAudioWrapper.mAudioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
            return;
        }
        if (volume < 0 || volume > 1.0f) {
            volume = FFMPEG.VOLUME_NORMAL;
        }
        if (Build.VERSION.SDK_INT >= 21) {
            mAudioWrapper.mAudioTrack.setVolume(volume);
        } else {
            mAudioWrapper.mAudioTrack.setStereoVolume(volume, volume);
        }
    }

    public long mProgressUs = -1;

    // 返回"微秒"
    public long getCurrentPosition() {
        return mAudioWrapper.presentationTimeUs;
    }

    // 95160000
    public void setProgressUs(long progressUs) {
        Log.i(TAG, "setProgressUs() start");
        Log.i(TAG, "----------------------------------------------------------");
        String elapsedTime = DateUtils.formatElapsedTime(progressUs / 1000000);
        Log.i(TAG, "setProgressUs() progressUs: " + progressUs + " " + elapsedTime);
        Log.i(TAG, "----------------------------------------------------------");
        switch (use_mode) {
            case FFMPEG.USE_MODE_MEDIA:
                if (progressUs < 0
                        || progressUs > mAudioWrapper.durationUs
                        || progressUs > mVideoWrapper.durationUs
                        || mAudioWrapper.isPausedForSeek
                        || mVideoWrapper.isPausedForSeek) {
                    Log.i(TAG, "setProgressUs() return");
                    return;
                }
                mAudioWrapper.isPausedForSeek = true;
                mVideoWrapper.isPausedForSeek = true;
                notifyToHandle(mAudioWrapper);
                notifyToHandle(mVideoWrapper);
                mProgressUs = progressUs;
                mAudioWrapper.progressUs = progressUs;
                mVideoWrapper.progressUs = progressUs;
                notifyToRead();
                notifyToRead(mAudioWrapper);
                notifyToRead(mVideoWrapper);
                break;
            case FFMPEG.USE_MODE_ONLY_AUDIO:
                if (progressUs < 0
                        || progressUs > mAudioWrapper.durationUs
                        || mAudioWrapper.isPausedForSeek) {
                    Log.i(TAG, "setProgressUs() return");
                    return;
                }
                mAudioWrapper.isPausedForSeek = true;
                notifyToHandle(mAudioWrapper);
                mProgressUs = progressUs;
                mAudioWrapper.progressUs = progressUs;
                notifyToRead();
                notifyToRead(mAudioWrapper);
                break;
            case USE_MODE_ONLY_VIDEO:
                if (progressUs < 0
                        || progressUs > mVideoWrapper.durationUs
                        || mVideoWrapper.isPausedForSeek) {
                    Log.i(TAG, "setProgressUs() return");
                    return;
                }
                mVideoWrapper.isPausedForSeek = true;
                notifyToHandle(mVideoWrapper);
                mProgressUs = progressUs;
                mVideoWrapper.progressUs = progressUs;
                notifyToRead();
                notifyToRead(mVideoWrapper);
                break;
            default:
                break;
        }
        Log.i(TAG, "setProgressUs() end");
    }

    private void seekTo() {
        Log.i(TAG, "seekTo() start");
        switch (use_mode) {
            case FFMPEG.USE_MODE_MEDIA:
                if (mAudioWrapper.isStarted && mVideoWrapper.isStarted) {
                    while (!mAudioWrapper.needToSeek || !mVideoWrapper.needToSeek) {
                        SystemClock.sleep(1);
                    }
                }
                synchronized (mAudioWrapper.readDataLock) {
                    mAudioWrapper.readData.clear();
                }
                synchronized (mVideoWrapper.readDataLock) {
                    mVideoWrapper.readData.clear();
                }
                Log.i(TAG, "seekTo()");
                // SEEK_TO_PREVIOUS_SYNC SEEK_TO_CLOSEST_SYNC SEEK_TO_NEXT_SYNC
                extractor.seekTo(mProgressUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                mAudioWrapper.decoderMediaCodec.flush();
                mVideoWrapper.decoderMediaCodec.flush();
                mAudioWrapper.needToSeek = false;
                mVideoWrapper.needToSeek = false;
                mAudioWrapper.isPausedForSeek = false;
                mVideoWrapper.isPausedForSeek = false;
                mAudioWrapper.presentationTimeUs = 0;
                mVideoWrapper.presentationTimeUs = 0;
                preElapsedTime = null;
                break;
            case FFMPEG.USE_MODE_ONLY_AUDIO:
                if (mAudioWrapper.isStarted) {
                    while (!mAudioWrapper.needToSeek) {
                        SystemClock.sleep(1);
                    }
                }
                synchronized (mAudioWrapper.readDataLock) {
                    mAudioWrapper.readData.clear();
                }
                Log.i(TAG, "seekTo()");
                extractor.seekTo(mProgressUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                mAudioWrapper.decoderMediaCodec.flush();
                mAudioWrapper.needToSeek = false;
                mAudioWrapper.isPausedForSeek = false;
                mAudioWrapper.presentationTimeUs = 0;
                preElapsedTime = null;
                break;
            case USE_MODE_ONLY_VIDEO:
                if (mVideoWrapper.isStarted) {
                    while (!mVideoWrapper.needToSeek) {
                        SystemClock.sleep(1);
                    }
                }
                synchronized (mVideoWrapper.readDataLock) {
                    mVideoWrapper.readData.clear();
                }
                Log.i(TAG, "seekTo()");
                // SEEK_TO_PREVIOUS_SYNC SEEK_TO_CLOSEST_SYNC SEEK_TO_NEXT_SYNC
                extractor.seekTo(mProgressUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                mVideoWrapper.decoderMediaCodec.flush();
                mVideoWrapper.needToSeek = false;
                mVideoWrapper.isPausedForSeek = false;
                mVideoWrapper.presentationTimeUs = 0;
                preElapsedTime = null;
                break;
            default:
                break;
        }
        Log.i(TAG, "seekTo() end");
    }

    public long getDurationUs() {
        switch (use_mode) {
            case FFMPEG.USE_MODE_MEDIA:
                return mAudioWrapper.durationUs;
            case FFMPEG.USE_MODE_ONLY_AUDIO:
                return mAudioWrapper.durationUs;
            case USE_MODE_ONLY_VIDEO:
                return mVideoWrapper.durationUs;
            default:
                break;
        }
        return -1;
    }

    public void play() {
        internalPlay();
        /*if (!isRunning()) {
            mThreadHandler.removeMessages(MSG_PREPARE);
            mThreadHandler.sendEmptyMessage(MSG_PREPARE);
        } else {
            mThreadHandler.removeMessages(MSG_PLAY);
            mThreadHandler.sendEmptyMessage(MSG_PLAY);
        }*/
    }

    public void pause() {
        internalPause();
        //mThreadHandler.removeMessages(MSG_PAUSE);
        //mThreadHandler.sendEmptyMessage(MSG_PAUSE);
    }

    public void release() {
        internalRelease();
        //mThreadHandler.removeMessages(MSG_RELEASE);
        //mThreadHandler.sendEmptyMessage(MSG_RELEASE);
    }

    public boolean isPlaying() {
        switch (use_mode) {
            case FFMPEG.USE_MODE_MEDIA:
                return mAudioWrapper.isStarted
                        && !mAudioWrapper.isPausedForUser
                        && !mAudioWrapper.isPausedForCache
                        && mVideoWrapper.isStarted
                        && !mVideoWrapper.isPausedForUser
                        && !mVideoWrapper.isPausedForCache;
            case FFMPEG.USE_MODE_ONLY_AUDIO:
                return mAudioWrapper.isStarted
                        && !mAudioWrapper.isPausedForUser
                        && !mAudioWrapper.isPausedForCache;
            case USE_MODE_ONLY_VIDEO:
                return mVideoWrapper.isStarted
                        && !mVideoWrapper.isPausedForUser
                        && !mVideoWrapper.isPausedForCache;
            default:
                break;
        }
        return false;
    }

    public boolean isRunning() {
        switch (use_mode) {
            case FFMPEG.USE_MODE_MEDIA:
                return mAudioWrapper.isHandling
                        || mVideoWrapper.isHandling;
            case FFMPEG.USE_MODE_ONLY_AUDIO:
                return mAudioWrapper.isHandling;
            case USE_MODE_ONLY_VIDEO:
                return mVideoWrapper.isHandling;
            default:
                break;
        }
        return false;
    }

    public boolean initPlayer() {
        return internalPrepare() && prepareAudio() && prepareVideo();
    }

    private boolean firstFlag = false;
    private boolean secondFlag = false;
    private boolean threeFlag = false;

    /***
     action=ACTION_DOWN, keyCode=KEYCODE_HEADSETHOOK,
     scanCode=226, metaState=0, flags=0x8, repeatCount=0,
     eventTime=301864016, downTime=301864016, deviceId=9, source=0x101
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        /*if (DEBUG)
            Log.d(TAG, "onKeyDown() event: " + event);*/
        switch (keyCode) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
                if (!firstFlag) {
                    firstFlag = true;
                } else if (firstFlag && !secondFlag) {
                    secondFlag = true;
                } else if (firstFlag && secondFlag && !threeFlag) {
                    threeFlag = true;
                }
                // 单位时间内按1次,2次,3次分别实现单击,双击,三击
                mUiHandler.removeMessages(KeyEvent.KEYCODE_HEADSETHOOK);
                mUiHandler.sendEmptyMessageDelayed(KeyEvent.KEYCODE_HEADSETHOOK, 300);
                return true;
            default:
                break;
        }

        return false;
    }

    private void threadHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        switch (msg.what) {
            case MSG_PREPARE:
                if (mCallback != null) {
                    Log.i(TAG, "onReady");
                    mCallback.onReady();
                }

                if (internalPrepare()) {
                    if (prepareAudio() && prepareVideo()) {
                        ThreadPool.getFixedThreadPool().execute(new Runnable() {
                            @Override
                            public void run() {
                                handleData(mAudioWrapper);
                            }
                        });
                        ThreadPool.getFixedThreadPool().execute(new Runnable() {
                            @Override
                            public void run() {
                                handleData(mVideoWrapper);
                            }
                        });
                        SystemClock.sleep(500);
                        ThreadPool.getFixedThreadPool().execute(new Runnable() {
                            @Override
                            public void run() {
                                readData();
                            }
                        });
                    } else {
                        if (mCallback != null) {
                            Log.e(TAG, "onError prepareAudio() || prepareVideo()");
                            mCallback.onError(0, null);
                        }
                    }
                } else {
                    if (mCallback != null) {
                        Log.e(TAG, "onError internalPrepare()");
                        mCallback.onError(0, null);
                    }
                }
                break;
            case MSG_PLAY:
                internalPlay();
                break;
            case MSG_PAUSE:
                internalPause();
                break;
            case MSG_DO_SOME_WORK:
                break;
            case MSG_RELEASE:
                internalRelease();
                //internalDestroy();
                break;
            case MSG_HANDLE_DATA_LOCK_WAIT:
                Log.e(TAG, "卧槽!卧槽!卧槽!等了10分钟了还不能播放,爷不等了");
                internalRelease();
                /*if (internalPrepare()) {
                    new Thread(mAudioReadData).start();
                    new Thread(mVideoReadData).start();
                }*/
                break;
            case MSG_SEEK_TO_NOTIFY:
                //                notifyToHandleDataLock();
                break;
            case MSG_GAME_OVER:
                if (msg.obj == null) {
                    return;
                }
                if (msg.obj instanceof SimpleWrapper) {
                    SimpleWrapper wrapper = (SimpleWrapper) msg.obj;

                    if (wrapper instanceof AudioWrapper
                            && mAudioWrapper.mAudioTrack != null) {
                        mAudioWrapper.mAudioTrack.release();
                    }

                    if (wrapper.decoderMediaCodec != null) {
                        wrapper.decoderMediaCodec.release();
                    }

                    wrapper.clear();

                    if (!mAudioWrapper.isHandling
                            && !mVideoWrapper.isHandling) {
                        if (mCallback != null) {
                            Log.w(TAG, "handleData() onFinished");
                            mCallback.onFinished();
                        }
                    }
                }
                break;
            /*case PLAY:
                internalPlay();
                break;
            case PAUSE:
                internalPause();
                break;
            case STOP:
            case RELEASE:
                internalStop();
                break;
            case PREV:
                internalPrev();
                break;
            case NEXT:
                internalNext();
                break;*/
            default:
                break;
        }
    }

    // Test
    //long step = 300000000;
    long step = 0;

    private void uiHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        switch (msg.what) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
                if (firstFlag && secondFlag && threeFlag) {
                    if (DEBUG)
                        Log.d(TAG, "onKeyDown() 3");
                    MediaUtils.SLEEP_TIME--;
                    if (DEBUG)
                        Log.d(TAG, "onKeyDown() MediaUtils.SLEEP_TIME: " +
                                MediaUtils.SLEEP_TIME);
                    /*if (mCallback != null) {
                        mCallback.onPlaybackFinished();
                    }*/
                } else if (firstFlag && secondFlag) {
                    if (DEBUG)
                        Log.d(TAG, "onKeyDown() 2");
                    /*MediaUtils.SLEEP_TIME++;
                    if (DEBUG)
                        Log.d(TAG, "onKeyDown() MediaUtils.SLEEP_TIME: " +
                                MediaUtils.SLEEP_TIME);*/
                    /*if (mCallback != null) {
                        mCallback.onPlaybackFinished();
                    }*/
                    if (mProgressUs == -1) {
                        step = (int) ((mVideoWrapper.presentationTimeUs
                                / (mVideoWrapper.durationUs * 1.00)) * 3840.00);
                        Log.d(TAG, "onKeuiHandleMessageyDown() step: " + step);
                    }
                    step += 100;
                    long progress = (long) (((step / 3840.00) * mVideoWrapper.durationUs));
                    setProgressUs(progress);
                } else {
                    if (DEBUG)
                        Log.d(TAG, "onKeyDown() 1");
                    if (!mAudioWrapper.isPausedForCache
                            && !mVideoWrapper.isPausedForCache) {
                        if (!mAudioWrapper.isPausedForUser
                                && !mAudioWrapper.isPausedForUser) {
                            internalPause();
                        } else {
                            internalPlay();
                        }
                    }
                }
                firstFlag = false;
                secondFlag = false;
                threeFlag = false;
                break;
            case MSG_RELEASE:
                internalRelease();
                break;
            /*case PLAY:
                internalPlay();
                break;*/
            default:
                break;
        }
    }

    // 读到的都是压缩数据
    public void readData() {
        if (mAudioWrapper.trackIndex != -1 && mVideoWrapper.trackIndex != -1) {
            use_mode = FFMPEG.USE_MODE_MEDIA;
            mAudioWrapper.readData = new ArrayList<AVPacket>();
            mVideoWrapper.readData = new ArrayList<AVPacket>(mVideoWrapper.CACHE);
            mVideoWrapper.width =
                    mVideoWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_WIDTH);
            mVideoWrapper.height =
                    mVideoWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
            if (mCallback != null) {
                mCallback.onChangeWindow(mVideoWrapper.width, mVideoWrapper.height);
            }
        } else if (mAudioWrapper.trackIndex != -1 && mVideoWrapper.trackIndex == -1) {
            use_mode = FFMPEG.USE_MODE_ONLY_AUDIO;
            mAudioWrapper.readData = new ArrayList<AVPacket>(mAudioWrapper.CACHE);
            if (mCallback != null) {
                mCallback.onChangeWindow(0, 0);
            }
        } else if (mAudioWrapper.trackIndex == -1 && mVideoWrapper.trackIndex != -1) {
            use_mode = USE_MODE_ONLY_VIDEO;
            mVideoWrapper.readData = new ArrayList<AVPacket>(mVideoWrapper.CACHE);
            mVideoWrapper.width =
                    mVideoWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_WIDTH);
            mVideoWrapper.height =
                    mVideoWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
            if (mCallback != null) {
                mCallback.onChangeWindow(mVideoWrapper.width, mVideoWrapper.height);
            }
        }
        Log.i(TAG, "readData()" +
                " mVideoWrapper.trackIndex: " + mVideoWrapper.trackIndex +
                " mAudioWrapper.trackIndex: " + mAudioWrapper.trackIndex);

        Log.i(TAG, "readData() start");
        //setProgressUs(721057837);
        int readSize = -1;
        int trackIndex = -1;
        ByteBuffer room = null;
        switch (use_mode) {
            case FFMPEG.USE_MODE_MEDIA:
                mAudioWrapper.decoderMediaCodec.flush();
                mVideoWrapper.decoderMediaCodec.flush();
                room = ByteBuffer.allocate(mVideoWrapper.frameMaxLength);
                break;
            case FFMPEG.USE_MODE_ONLY_AUDIO:
                mAudioWrapper.decoderMediaCodec.flush();
                room = ByteBuffer.allocate(mAudioWrapper.frameMaxLength);
                break;
            case USE_MODE_ONLY_VIDEO:
                mVideoWrapper.decoderMediaCodec.flush();
                room = ByteBuffer.allocate(mVideoWrapper.frameMaxLength);
                break;
            default:
                break;
        }

        boolean audioStarted = false;
        boolean videoStarted = false;
        int audioSize = 0;
        int videoSize = 0;
        AVPacket data = null;
        while (isReading) {
            try {
                switch (use_mode) {
                    case FFMPEG.USE_MODE_MEDIA:
                        // seekTo
                        if (mAudioWrapper.isPausedForSeek
                                || mVideoWrapper.isPausedForSeek) {
                            seekTo();
                            audioStarted = false;
                            videoStarted = false;
                        }
                        break;
                    case FFMPEG.USE_MODE_ONLY_AUDIO:
                        if (mAudioWrapper.isPausedForSeek) {
                            seekTo();
                            audioStarted = false;
                        }
                        break;
                    case USE_MODE_ONLY_VIDEO:
                        if (mVideoWrapper.isPausedForSeek) {
                            seekTo();
                            videoStarted = false;
                        }
                        break;
                    default:
                        break;
                }

                room.clear();
                readSize = extractor.readSampleData(room, 0);
                trackIndex = extractor.getSampleTrackIndex();

                // region readSize < 0

                if (readSize < 0) {
                    // 没有数据可读了,结束
                    // 也有可能是底层异常引起
                    // MediaHTTPConnection:
                    // readAt 1033700796 / 32768 => java.net.ProtocolException:
                    // unexpected end of stream
                    Log.e(TAG, "readData() end      readSize: " + readSize);
                    switch (use_mode) {
                        case FFMPEG.USE_MODE_MEDIA:
                            Log.e(TAG,
                                    "readData() end audioDataSize: " + mAudioWrapper.readData.size());
                            Log.e(TAG,
                                    "readData() end videoDataSize: " + mVideoWrapper.readData.size());
                            break;
                        case FFMPEG.USE_MODE_ONLY_AUDIO:
                            Log.e(TAG,
                                    "readData() end audioDataSize: " + mAudioWrapper.readData.size());
                            break;
                        case USE_MODE_ONLY_VIDEO:
                            Log.e(TAG,
                                    "readData() end videoDataSize: " + mVideoWrapper.readData.size());
                            break;
                        default:
                            break;
                    }

                    isReading = false;
                    // 开启任务处理数据(如果还没有开启任务过的话)
                    // 如果任务是在这里被开启的,那么说明网络文件长度小于等于CACHE
                    switch (use_mode) {
                        case FFMPEG.USE_MODE_MEDIA:
                            if (!mAudioWrapper.isPausedForUser
                                    && !mVideoWrapper.isPausedForUser) {
                                notifyToHandle(mAudioWrapper);
                                notifyToHandle(mVideoWrapper);
                            }
                            break;
                        case FFMPEG.USE_MODE_ONLY_AUDIO:
                            if (!mAudioWrapper.isPausedForUser) {
                                notifyToHandle(mAudioWrapper);
                            }
                            break;
                        case USE_MODE_ONLY_VIDEO:
                            if (!mVideoWrapper.isPausedForUser) {
                                notifyToHandle(mVideoWrapper);
                            }
                            break;
                        default:
                            break;
                    }

                    Log.e(TAG, "readData() notifyToReadWait start");
                    // 需要seek,不能退出
                    notifyToReadWait();
                    Log.e(TAG, "readData() notifyToReadWait end");
                    if (use_mode == FFMPEG.USE_MODE_MEDIA) {
                        if (mAudioWrapper.isPausedForSeek
                                || mVideoWrapper.isPausedForSeek) {
                            isReading = true;
                            continue;
                        } else {
                            break;
                        }
                    } else if (use_mode == FFMPEG.USE_MODE_ONLY_AUDIO) {
                        if (mAudioWrapper.isPausedForSeek) {
                            isReading = true;
                            continue;
                        } else {
                            break;
                        }
                    } else if (use_mode == USE_MODE_ONLY_VIDEO) {
                        if (mVideoWrapper.isPausedForSeek) {
                            isReading = true;
                            continue;
                        } else {
                            break;
                        }
                    }
                }

                // endregion

                data = new AVPacket(readSize, extractor.getSampleTime());
                room.get(data.data, 0, readSize);

                switch (use_mode) {
                    case FFMPEG.USE_MODE_MEDIA:
                        if (trackIndex == mAudioWrapper.trackIndex) {
                            synchronized (mAudioWrapper.readDataLock) {
                                mAudioWrapper.readData.add(data);
                                audioSize = mAudioWrapper.readData.size();
                            }

                            if (!audioStarted && audioSize == mAudioWrapper.CACHE_START) {
                                Log.i(TAG, "readData() audio notifyToHandle");
                                audioStarted = true;
                                notifyToHandle(mAudioWrapper);
                            }
                        } else if (trackIndex == mVideoWrapper.trackIndex) {
                            synchronized (mVideoWrapper.readDataLock) {
                                mVideoWrapper.readData.add(data);
                                videoSize = mVideoWrapper.readData.size();
                            }

                            if (videoStarted && videoSize >= mVideoWrapper.CACHE) {
                                /*Log.i(TAG, "readData() notifyToReadWait start");
                                Log.i(TAG,
                                        "readData() audio size: " + mAudioWrapper.readData.size());
                                Log.i(TAG,
                                        "readData() video size: " + mVideoWrapper.readData.size()
                                        );*/
                                System.gc();
                                notifyToReadWait(mVideoWrapper);
                                if (mAudioWrapper.isPausedForSeek
                                        || mVideoWrapper.isPausedForSeek) {
                                    Log.i(TAG, "readData() notifyToReadWait end for Seek");
                                    continue;
                                }
                                //Log.i(TAG, "readData() notifyToReadWait end");
                            } else if (!videoStarted && videoSize == mVideoWrapper.CACHE_START) {
                                Log.i(TAG, "readData() video notifyToHandle");
                                videoStarted = true;
                                notifyToHandle(mVideoWrapper);
                            }
                        }
                        break;
                    case FFMPEG.USE_MODE_ONLY_AUDIO:
                        if (trackIndex == mAudioWrapper.trackIndex) {
                            synchronized (mAudioWrapper.readDataLock) {
                                mAudioWrapper.readData.add(data);
                                audioSize = mAudioWrapper.readData.size();
                            }

                            if (audioStarted && audioSize >= mAudioWrapper.CACHE) {
                                System.gc();
                                notifyToReadWait(mAudioWrapper);
                                if (mAudioWrapper.isPausedForSeek) {
                                    continue;
                                }
                            } else if (!audioStarted && audioSize == mAudioWrapper.CACHE_START) {
                                Log.i(TAG, "readData() audio notifyToHandle");
                                audioStarted = true;
                                notifyToHandle(mAudioWrapper);
                            }
                        }
                        break;
                    case USE_MODE_ONLY_VIDEO:
                        if (trackIndex == mVideoWrapper.trackIndex) {
                            synchronized (mVideoWrapper.readDataLock) {
                                mVideoWrapper.readData.add(data);
                                videoSize = mVideoWrapper.readData.size();
                            }

                            if (videoStarted && videoSize >= mVideoWrapper.CACHE) {
                                System.gc();
                                notifyToReadWait(mVideoWrapper);
                                if (mVideoWrapper.isPausedForSeek) {
                                    continue;
                                }
                            } else if (!videoStarted && videoSize == mVideoWrapper.CACHE_START) {
                                Log.i(TAG, "readData() video notifyToHandle");
                                videoStarted = true;
                                notifyToHandle(mVideoWrapper);
                            }
                        }
                        break;
                    default:
                        break;
                }


                // Test
                /*if (trackIndex == mAudioWrapper.trackIndex) {
                    mAudioWrapper.data = data.data;
                    mAudioWrapper.size = data.size;
                    mAudioWrapper.sampleTime = data.sampleTime;
                    if (!feedInputBufferAndDrainOutputBuffer(mAudioWrapper)) {
                        mAudioWrapper.isHandling = false;
                        break;
                    }
                } else if (trackIndex == mVideoWrapper.trackIndex) {
                    mVideoWrapper.data = data.data;
                    mVideoWrapper.size = data.size;
                    mVideoWrapper.sampleTime = data.sampleTime;
                    if (!feedInputBufferAndDrainOutputBuffer(mVideoWrapper)) {
                        mVideoWrapper.isHandling = false;
                        break;
                    }
                }*/

                // 跳到下一帧
                extractor.advance();
            } catch (Exception e) {
                e.printStackTrace();
                isReading = false;
                break;
            }
        }// while(...) end

        if (extractor != null) {
            extractor.release();
            extractor = null;
        }

        Log.i(TAG, "readData() end");
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void handleData(SimpleWrapper wrapper) {
        switch (use_mode) {
            case FFMPEG.USE_MODE_MEDIA:
                if (wrapper.type == TYPE_AUDIO) {
                    mAudioWrapper.isHandling = true;
                } else {
                    mVideoWrapper.isHandling = true;
                }
                break;
            case FFMPEG.USE_MODE_ONLY_AUDIO:
                if (wrapper.type == TYPE_AUDIO) {
                    mAudioWrapper.isHandling = true;
                } else {
                    mVideoWrapper.isHandling = false;
                }
                break;
            case USE_MODE_ONLY_VIDEO:
                if (wrapper.type == TYPE_AUDIO) {
                    mAudioWrapper.isHandling = false;
                } else {
                    mVideoWrapper.isHandling = true;
                }
                break;
            default:
                break;
        }

        if (wrapper.isHandling) {
            if (wrapper.type == TYPE_AUDIO) {
                Log.d(TAG, "handleData() audio notifyToHandleWait start");
            } else {
                Log.w(TAG, "handleData() video notifyToHandleWait start");
            }
            notifyToHandleWait(wrapper);
            if (wrapper.type == TYPE_AUDIO) {
                Log.d(TAG, "handleData() audio notifyToHandleWait end");
            } else {
                Log.w(TAG, "handleData() video notifyToHandleWait end");
            }
            if (!wrapper.isHandling) {
                return;
            }
        }

        AVPacket data = null;
        int readDataSize = 0;
        if (wrapper.type == TYPE_AUDIO) {
            Log.d(TAG, "handleData() audio start");
        } else {
            Log.w(TAG, "handleData() video start");
        }
        while (wrapper.isHandling) {
            // region 暂停装置

            if (wrapper.isPausedForUser
                    || wrapper.isPausedForCache
                    || wrapper.isPausedForSeek) {
                boolean isPausedForUser = wrapper.isPausedForUser;
                boolean isPausedForSeek = wrapper.isPausedForSeek;

                if (isPausedForSeek) {
                    wrapper.needToSeek = true;
                    if (wrapper.type == TYPE_AUDIO) {
                        Log.d(TAG, "handleData() audio Seek notifyToHandleWait start");
                    } else {
                        Log.w(TAG, "handleData() video Seek notifyToHandleWait start");
                    }
                } else if (isPausedForUser) {
                    if (wrapper.type == TYPE_AUDIO) {
                        Log.d(TAG, "handleData() audio User notifyToHandleWait start");
                    } else {
                        Log.w(TAG, "handleData() video User notifyToHandleWait start");
                    }
                } else {
                    if (wrapper.type == TYPE_AUDIO) {
                        Log.d(TAG, "handleData() audio Cache notifyToHandleWait start");
                    } else {
                        Log.w(TAG, "handleData() video Cache notifyToHandleWait start");
                    }
                }

                notifyToHandleWait(wrapper);

                if (wrapper.isPausedForSeek || wrapper.isPausedForUser) {
                    continue;
                }

                if (isPausedForSeek) {
                    if (wrapper.type == TYPE_AUDIO) {
                        Log.d(TAG, "handleData() audio Seek notifyToHandleWait end");
                        if (mCallback != null) {
                            mCallback.onPlayed();
                        }
                    } else {
                        Log.w(TAG, "handleData() video Seek notifyToHandleWait end");
                    }
                } else if (isPausedForUser) {
                    if (wrapper.type == TYPE_AUDIO) {
                        Log.d(TAG, "handleData() audio User notifyToHandleWait end");
                    } else {
                        Log.w(TAG, "handleData() video User notifyToHandleWait end");
                    }
                } else {
                    if (wrapper.type == TYPE_AUDIO) {
                        Log.d(TAG, "handleData() audio Cache notifyToHandleWait end");
                    } else {
                        Log.w(TAG, "handleData() video Cache notifyToHandleWait end");
                    }
                }
            }

            // endregion

            // 取得一个AVPacket
            synchronized (wrapper.readDataLock) {
                if (!wrapper.readData.isEmpty()) {
                    data = wrapper.readData.get(0);
                    wrapper.readData.remove(0);
                }
                readDataSize = wrapper.readData.size();
            }

            if (wrapper.type == TYPE_AUDIO) {
                //Log.d(TAG, "handleData() audio readDataSize: " + readDataSize);
                if (use_mode == FFMPEG.USE_MODE_ONLY_AUDIO) {
                    if ((mIsLocal && (readDataSize % 30 == 0))
                            || (!mIsLocal && readDataSize < mAudioWrapper.CACHE)) {
                        notifyToRead(wrapper);
                    }
                }
            } else {
                //Log.w(TAG, "handleData() video readDataSize: " + readDataSize);
                if (use_mode == FFMPEG.USE_MODE_MEDIA || use_mode == USE_MODE_ONLY_VIDEO) {
                    if ((mIsLocal && (readDataSize % 30 == 0))
                            || (!mIsLocal && readDataSize < mVideoWrapper.CACHE)) {
                        notifyToRead(wrapper);
                    }
                }
            }

            if (readDataSize > 0) {
                wrapper.data = data.data;
                wrapper.size = data.size;
                wrapper.sampleTime = data.pts;

                // 向MediaCodec喂数据
                if (!feedInputBufferAndDrainOutputBuffer(wrapper)) {
                    wrapper.isHandling = false;
                    break;
                }

                data.clear();

                if (wrapper.type == TYPE_AUDIO) {
                    curElapsedTime = DateUtils.formatElapsedTime(
                            (wrapper.sampleTime / 1000) / 1000);
                    if (mCallback != null
                            // 防止重复更新
                            && !TextUtils.equals(curElapsedTime, preElapsedTime)) {
                        mCallback.onProgressUpdated(wrapper.sampleTime / 1000000);
                        preElapsedTime = curElapsedTime;
                        //Log.d(TAG, "handleData() curElapsedTime: " + curElapsedTime);
                    }
                } else {
                }
            } else {
                if (isReading) {
                    notifyToRead(wrapper);
                    if (mCallback != null) {
                        if (wrapper.type == TYPE_AUDIO) {
                            Log.d(TAG, "handleData() audio onPaused");
                        } else {
                            Log.w(TAG, "handleData() video onPaused");
                        }
                        mCallback.onPaused();
                    }

                    if (wrapper.type == TYPE_AUDIO) {
                        if (mIsLocal && mUiHandler != null) {
                            mUiHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    setProgressUs(mAudioWrapper.presentationTimeUs);
                                }
                            }, 200);
                        }
                        Log.e(TAG, "卧槽!卧槽!卧槽! audio太不给力了");
                    } else {
                        Log.e(TAG, "卧槽!卧槽!卧槽! video太不给力了");
                    }
                    mAudioWrapper.isPausedForCache = true;
                    mVideoWrapper.isPausedForCache = true;

                    notifyToHandleWait(wrapper);

                    mAudioWrapper.isPausedForCache = false;
                    mVideoWrapper.isPausedForCache = false;

                    if (wrapper.isPausedForSeek) {
                        continue;
                    }

                    if (wrapper.type == TYPE_AUDIO) {
                        notifyToHandle(mVideoWrapper);
                        Log.d(TAG, "handleData() audio onPlayed");
                    } else {
                        notifyToHandle(mAudioWrapper);
                        Log.w(TAG, "handleData() video onPlayed");
                    }
                    if (mCallback != null) {
                        mCallback.onPlayed();
                    }
                } else {
                    wrapper.isHandling = false;
                    break;
                }
            }
        }// while(true) end
        if (wrapper.type == TYPE_AUDIO) {
            Log.d(TAG, "handleData() audio end");
        } else {
            Log.w(TAG, "handleData() video end");
        }

        switch (use_mode) {
            case FFMPEG.USE_MODE_MEDIA:
                if (wrapper != null) {
                    if (wrapper.type == TYPE_AUDIO) {
                        mAudioWrapper.decoderMediaCodec.flush();
                    } else {
                        mVideoWrapper.decoderMediaCodec.flush();
                    }
                    wrapper.clear();
                    if (wrapper.type == TYPE_AUDIO) {
                        mAudioWrapper = null;
                    } else {
                        mVideoWrapper = null;
                    }
                    wrapper = null;
                }
                break;
            case FFMPEG.USE_MODE_ONLY_AUDIO:
                if (wrapper != null) {
                    if (wrapper.type == TYPE_AUDIO) {
                        mAudioWrapper.decoderMediaCodec.flush();
                    } else {
                    }
                    wrapper.clear();
                    if (wrapper.type == TYPE_AUDIO) {
                        mAudioWrapper = null;
                        mVideoWrapper = null;
                        wrapper = null;
                    }
                }
                break;
            case USE_MODE_ONLY_VIDEO:
                if (wrapper != null) {
                    if (wrapper.type == TYPE_AUDIO) {
                    } else {
                        mVideoWrapper.decoderMediaCodec.flush();
                    }
                    wrapper.clear();
                    if (wrapper.type == TYPE_AUDIO) {
                    } else {
                        mAudioWrapper = null;
                        mVideoWrapper = null;
                        wrapper = null;
                    }
                }
                break;
            default:
                break;
        }


        if (mAudioWrapper == null
                && mVideoWrapper == null) {
            //SystemClock.sleep(500);
            internalDestroy();
            if (mCallback != null) {
                Log.w(TAG, "handleData() onFinished");
                mCallback.onFinished();
            }

            System.gc();
            Log.i(TAG, "handleData() audio and video are finished");
        }
    }

    private void init() {
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mThreadHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                SimpleVideoPlayer.this.threadHandleMessage(msg);
            }
        };
        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                SimpleVideoPlayer.this.uiHandleMessage(msg);
            }
        };
    }

    private boolean prepareAudio() {
        if (TextUtils.isEmpty(mAudioWrapper.mime)
                && mAudioWrapper.trackIndex == -1
                && mAudioWrapper.decoderMediaFormat == null) {
            Log.d(TAG, "prepareAudio() return");
            return true;
        }
        // Audio
        Log.d(TAG, "prepareAudio() start");
        Log.d(TAG, "prepareAudio() audio decoderMediaFormat: " +
                mAudioWrapper.decoderMediaFormat.toString());

        /***
         解码前
         {
         csd-0=java.nio.HeapByteBuffer[pos=0 lim=2 cap=2],
         mime=audio/mp4a-latm, aac-profile=2, channel-count=2, track-id=2, bitrate=96000,
         max-input-size=444, durationUs=10871488000,
         sample-rate=48000, max-bitrate=96000
         }
         解码后
         {pcm-encoding=2, mime=audio/raw, channel-count=2, sample-rate=48000}
         */
        // durationUs
        if (mAudioWrapper.decoderMediaFormat.containsKey(MediaFormat.KEY_DURATION)) {
            mAudioWrapper.durationUs =
                    mAudioWrapper.decoderMediaFormat.getLong(MediaFormat.KEY_DURATION);
            String durationTime = DateUtils.formatElapsedTime(
                    (mAudioWrapper.durationUs / 1000) / 1000);
            Log.d(TAG, "prepareAudio()          audioDurationUs: " +
                    mAudioWrapper.durationUs + " " + durationTime);
            if (mAudioWrapper.durationUs > 0 || mVideoWrapper.durationUs > 0) {
                mAudioWrapper.durationUs =
                        (mAudioWrapper.durationUs > mVideoWrapper.durationUs)
                                ?
                                mAudioWrapper.durationUs
                                :
                                mVideoWrapper.durationUs;
                mVideoWrapper.durationUs = mAudioWrapper.durationUs;
            } else {
                mVideoWrapper.durationUs = mAudioWrapper.durationUs = -1;
            }
        }
        // max-input-size
        if (!mAudioWrapper.decoderMediaFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            // 4K 8192
            mAudioWrapper.decoderMediaFormat.setInteger(
                    MediaFormat.KEY_MAX_INPUT_SIZE, AUDIO_FRAME_MAX_LENGTH);
        }
        mAudioWrapper.frameMaxLength =
                mAudioWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        Log.d(TAG, "prepareAudio() audio     frameMaxLength: " +
                mAudioWrapper.frameMaxLength);
        String errorInfo = null;
        // 创建音频解码器
        try {
            extractor.selectTrack(mAudioWrapper.trackIndex);
            // audio/vorbis
            Log.d(TAG, "prepareAudio() audio mAudioWrapper.mime: " + mAudioWrapper.mime);
            switch (mAudioWrapper.mime) {
                case "audio/ac4":
                    MediaCodecInfo mediaCodecInfo =
                            MediaUtils.getDecoderMediaCodecInfo(mAudioWrapper.mime);
                    String codecName = null;
                    if (mediaCodecInfo != null) {
                        codecName = mediaCodecInfo.getName();
                    } else {
                        codecName = "OMX.google.raw.decoder";
                        mAudioWrapper.decoderMediaFormat.setString(
                                MediaFormat.KEY_MIME, "audio/raw");
                    }
                    if (!TextUtils.isEmpty(codecName)) {
                        Log.d(TAG, "prepareAudio() audio          codecName: " + codecName);
                        mAudioWrapper.decoderMediaCodec = MediaCodec.createByCodecName(codecName);
                        mAudioWrapper.decoderMediaCodec.configure(
                                mAudioWrapper.decoderMediaFormat, null, null, 0);
                        mAudioWrapper.decoderMediaCodec.start();
                    }
                    break;
                default:
                    mAudioWrapper.decoderMediaCodec = MediaUtils.getAudioDecoderMediaCodec(
                            mAudioWrapper.mime, mAudioWrapper.decoderMediaFormat);
                    break;
            }

            mAudioWrapper.render = false;
        } catch (MediaCodec.CryptoException
                | IllegalStateException
                | IllegalArgumentException
                | IOException e) {
            e.printStackTrace();
            errorInfo = e.toString();
            if (mAudioWrapper.decoderMediaCodec != null) {
                mAudioWrapper.decoderMediaCodec.release();
                mAudioWrapper.decoderMediaCodec = null;
            }
        }
        if (mAudioWrapper.decoderMediaCodec == null) {
            if (extractor != null) {
                extractor.release();
                extractor = null;
            }
            internalRelease();
            if (mCallback != null) {
                mCallback.onError(Callback.ERROR_FFMPEG_INIT, errorInfo);
            }
            return false;
        }

        if (mAudioWrapper.decoderMediaFormat.containsKey("csd-0")) {
            ByteBuffer buffer = mAudioWrapper.decoderMediaFormat.getByteBuffer("csd-0");
            byte[] csd_0 = new byte[buffer.limit()];
            buffer.get(csd_0);
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            int count = csd_0.length;
            for (int i = 0; i < count; i++) {
                sb.append(csd_0[i]);
                if (i <= count - 2) {
                    sb.append(", ");
                }
            }
            sb.append("}");
            Log.d(TAG, "prepareAudio() audio              csd-0: " + sb.toString());
        }
        if (mAudioWrapper.decoderMediaFormat.containsKey("csd-1")) {
            ByteBuffer buffer = mAudioWrapper.decoderMediaFormat.getByteBuffer("csd-1");
            byte[] csd_1 = new byte[buffer.limit()];
            buffer.get(csd_1);
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            int count = csd_1.length;
            for (int i = 0; i < count; i++) {
                sb.append(csd_1[i]);
                if (i <= count - 2) {
                    sb.append(", ");
                }
            }
            sb.append("}");
            Log.d(TAG, "prepareAudio() audio              csd-1: " + sb.toString());
        }
        Log.d(TAG, "prepareAudio() end");

        return true;
    }

    private boolean prepareVideo() {
        if (TextUtils.isEmpty(mVideoWrapper.mime)
                && mVideoWrapper.trackIndex == -1
                && mVideoWrapper.decoderMediaFormat == null) {
            Log.d(TAG, "prepareVideo() return");
            return true;
        }

        // Video
        Log.w(TAG, "prepareVideo() start");
        if (mSurface != null) {
            mVideoWrapper.mSurface = mSurface;
        } else {
            Log.e(TAG, "prepareVideo() mSurface is null");
            if (mCallback != null) {
                mCallback.onError(Callback.ERROR_FFMPEG_INIT, "Surface is null");
            }
            return false;
        }
        Log.w(TAG, "prepareVideo() video decoderMediaFormat: " +
                mVideoWrapper.decoderMediaFormat.toString());
        /***
         BITRATE_MODE_CQ:  表示完全不控制码率，尽最大可能保证图像质量
         BITRATE_MODE_CBR: 表示编码器会尽量把输出码率控制为设定值
         BITRATE_MODE_VBR: 表示编码器会根据图像内容的复杂度（实际上是帧间变化量的大小）来动态调整输出码率，
         图像复杂则码率高，图像简单则码率低
         */
        mVideoWrapper.decoderMediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE,
                MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ);
        // https://www.polarxiong.com/archives/Android-MediaCodec%E8%A7%86%E9%A2%91%E6%96%87%E4
        // %BB%B6%E7%A1%AC%E4%BB%B6%E8%A7%A3%E7%A0%81-%E9%AB%98%E6%95%88%E7%8E%87%E5%BE%97%E5%88
        // %B0YUV%E6%A0%BC%E5%BC%8F%E5%B8%A7-%E5%BF%AB%E9%80%9F%E4%BF%9D%E5%AD%98JPEG%E5%9B%BE%E7
        // %89%87-%E4%B8%8D%E4%BD%BF%E7%94%A8OpenGL.html
        // 指定解码后的帧格式(解码器将编码的帧解码为这种指定的格式)
        // COLOR_FormatYUV420Flexible是几乎所有解码器都支持的一种帧格式
        // 经过这种格式的指定,解码视频后的帧格式可以很方便的转换成YUV420SemiPlanar(NV21)或YUV420Planar(I420)格式
        mVideoWrapper.decoderMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);

        /***
         解码前
         {
         csd-0=java.nio.HeapByteBuffer[pos=0 lim=28 cap=28],
         csd-1=java.nio.HeapByteBuffer[pos=0 lim=9 cap=9],
         mime=video/avc, frame-rate=24, track-id=1, profile=8,
         width=1280, height=720, max-input-size=243905, durationUs=10871402208,
         bitrate-mode=0, level=512
         }
         解码后
         {crop-top=0, crop-right=1279, color-format=19, height=720,
         color-standard=1, crop-left=0, color-transfer=3, stride=1280,
         mime=video/raw, slice-height=720, width=1280, color-range=2, crop-bottom=719}
         */
        // durationUs
        if (mVideoWrapper.decoderMediaFormat.containsKey(MediaFormat.KEY_DURATION)) {
            mVideoWrapper.durationUs =
                    mVideoWrapper.decoderMediaFormat.getLong(MediaFormat.KEY_DURATION);
            String durationTime = DateUtils.formatElapsedTime(
                    (mVideoWrapper.durationUs / 1000) / 1000);
            Log.w(TAG, "prepareVideo()          videoDurationUs: " +
                    mVideoWrapper.durationUs + " " + durationTime);
            if (mAudioWrapper.durationUs > 0 || mVideoWrapper.durationUs > 0) {
                mAudioWrapper.durationUs =
                        (mAudioWrapper.durationUs > mVideoWrapper.durationUs)
                                ?
                                mAudioWrapper.durationUs
                                :
                                mVideoWrapper.durationUs;
                mVideoWrapper.durationUs = mAudioWrapper.durationUs;
            } else {
                mVideoWrapper.durationUs = mAudioWrapper.durationUs = -1;
            }
        }
        // max-input-size
        if (!mVideoWrapper.decoderMediaFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            // 4K 3110400
            mVideoWrapper.decoderMediaFormat.setInteger(
                    MediaFormat.KEY_MAX_INPUT_SIZE, VIDEO_FRAME_MAX_LENGTH);
        }
        mVideoWrapper.frameMaxLength =
                mVideoWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        Log.w(TAG, "prepareVideo() video     frameMaxLength: " +
                mVideoWrapper.frameMaxLength);
        String errorInfo = null;
        // 创建视频解码器
        try {
            extractor.selectTrack(mVideoWrapper.trackIndex);
            // video/x-vnd.on2.vp9
            Log.w(TAG, "prepareVideo() video mVideoWrapper.mime: " + mVideoWrapper.mime);
            mVideoWrapper.decoderMediaCodec =
                    MediaUtils.getVideoDecoderMediaCodec(
                            mVideoWrapper.mime,
                            mVideoWrapper.decoderMediaFormat,
                            mVideoWrapper.mSurface);
            mVideoWrapper.render = true;
        } catch (MediaCodec.CryptoException
                | IllegalStateException
                | IllegalArgumentException e) {
            e.printStackTrace();
            errorInfo = e.toString();
            if (mVideoWrapper.decoderMediaCodec != null) {
                mVideoWrapper.decoderMediaCodec.release();
                mVideoWrapper.decoderMediaCodec = null;
            }
        }
        if (mVideoWrapper.decoderMediaCodec == null) {
            if (extractor != null) {
                extractor.release();
                extractor = null;
            }
            internalRelease();
            if (mCallback != null) {
                mCallback.onError(Callback.ERROR_FFMPEG_INIT, errorInfo);
            }
            return false;
        }

        if (mVideoWrapper.decoderMediaFormat.containsKey("csd-0")) {
            ByteBuffer buffer = mVideoWrapper.decoderMediaFormat.getByteBuffer("csd-0");
            byte[] csd_0 = new byte[buffer.limit()];
            buffer.get(csd_0);
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            int count = csd_0.length;
            for (int i = 0; i < count; i++) {
                sb.append(csd_0[i]);
                if (i <= count - 2) {
                    sb.append(", ");
                }
            }
            sb.append("}");
            Log.w(TAG, "prepareVideo() video              csd-0: " + sb.toString());
        }
        if (mVideoWrapper.decoderMediaFormat.containsKey("csd-1")) {
            ByteBuffer buffer = mVideoWrapper.decoderMediaFormat.getByteBuffer("csd-1");
            byte[] csd_1 = new byte[buffer.limit()];
            buffer.get(csd_1);
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            int count = csd_1.length;
            for (int i = 0; i < count; i++) {
                sb.append(csd_1[i]);
                if (i <= count - 2) {
                    sb.append(", ");
                }
            }
            sb.append("}");
            Log.w(TAG, "prepareVideo() video              csd-1: " + sb.toString());
        }
        Log.w(TAG, "prepareVideo() end");

        return true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean internalPrepare() {
        if (TextUtils.isEmpty(mPath)) {
            Log.e(TAG, "internalPrepare() mPath is empty");
            return false;
        }
        Log.i(TAG, "internalPrepare() start");

        if (mCallback != null) {
            mCallback.onReady();
        }

        mAudioWrapper = new AudioWrapper(TYPE_AUDIO);
        mVideoWrapper = new VideoWrapper(TYPE_VIDEO);
        mAudioWrapper.clear();
        mVideoWrapper.clear();
        preElapsedTime = null;
        curElapsedTime = null;
        runCounts = 0;
        Arrays.fill(timeDiff, 0);
        isReading = true;
        mAudioWrapper.isHandling = true;
        mVideoWrapper.isHandling = true;

        if (mPath.startsWith("/storage/")) {
            File file = new File(mPath);
            if (!file.exists()
                    || !file.canRead()
                    || file.isDirectory()) {
                Log.e(TAG, "internalPrepare() 不能读取此文件: " + mPath);
                internalRelease();
                if (mCallback != null) {
                    mCallback.onError(Callback.ERROR_FFMPEG_INIT, "文件有问题");
                }
                return false;
            }
            long fileSize = file.length();
            Log.i(TAG, "internalPrepare() fileSize: " + fileSize);
            mIsLocal = true;
            mAudioWrapper.CACHE_START = START_CACHE_COUNT_LOCAL;
            mVideoWrapper.CACHE_START = START_CACHE_COUNT_LOCAL;
        } else {
            mIsLocal = false;
            mAudioWrapper.CACHE_START = START_CACHE_COUNT_HTTP;
            mVideoWrapper.CACHE_START = START_CACHE_COUNT_HTTP;
        }

        extractor = new MediaExtractor();
        try {
            extractor.setDataSource(mPath);
        } catch (IOException e) {
            e.printStackTrace();
            if (extractor != null) {
                extractor.release();
                extractor = null;
            }
            internalRelease();
            if (mCallback != null) {
                mCallback.onError(
                        Callback.ERROR_FFMPEG_INIT, "setDataSource() occurs error");
            }
            return false;
        }

        int trackCount = extractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                mAudioWrapper.mime = mime;
                mAudioWrapper.trackIndex = i;
                mAudioWrapper.decoderMediaFormat = format;
            } else if (mime.startsWith("video/")) {
                mVideoWrapper.mime = mime;
                mVideoWrapper.trackIndex = i;
                mVideoWrapper.decoderMediaFormat = format;
            }
        }
        if ((TextUtils.isEmpty(mAudioWrapper.mime)
                && TextUtils.isEmpty(mVideoWrapper.mime))
                || (mAudioWrapper.trackIndex == -1
                && mVideoWrapper.trackIndex == -1)
                || (mAudioWrapper.decoderMediaFormat == null
                && mVideoWrapper.decoderMediaFormat == null)) {
            if (extractor != null) {
                extractor.release();
                extractor = null;
            }
            internalRelease();
            if (mCallback != null) {
                mCallback.onError(
                        Callback.ERROR_FFMPEG_INIT, "audio and video aren't find");
            }
            return false;
        }

        Log.i(TAG, "internalPrepare() end");

        return true;
    }

    private void printInfo() {
        // 保存最小的那个时间
        if (mAudioWrapper.decoderMediaFormat.containsKey(MediaFormat.KEY_DURATION)
                && mVideoWrapper.decoderMediaFormat.containsKey(MediaFormat.KEY_DURATION)) {
            long audioDurationUs =
                    mAudioWrapper.decoderMediaFormat.getLong(MediaFormat.KEY_DURATION);
            long videoDurationUs =
                    mVideoWrapper.decoderMediaFormat.getLong(MediaFormat.KEY_DURATION);
            mAudioWrapper.durationUs =
                    audioDurationUs > videoDurationUs ? videoDurationUs : audioDurationUs;
            mVideoWrapper.durationUs = mAudioWrapper.durationUs;
            Log.i(TAG, "internalPrepare()      audioDurationUs: " + audioDurationUs);
            Log.i(TAG, "internalPrepare()      videoDurationUs: " + videoDurationUs);
            Log.i(TAG, "internalPrepare()           durationUs: " + mVideoWrapper.durationUs);
        }

        mThreadHandler.removeMessages(MSG_HANDLE_DATA_LOCK_WAIT);
    }

    private void internalPlay() {
        switch (use_mode) {
            case FFMPEG.USE_MODE_MEDIA:
                if (!mAudioWrapper.isPausedForCache
                        && !mVideoWrapper.isPausedForCache) {
                    mAudioWrapper.isPausedForUser = false;
                    mVideoWrapper.isPausedForUser = false;
                    notifyToHandle(mAudioWrapper);
                    notifyToHandle(mVideoWrapper);
                }
                break;
            case FFMPEG.USE_MODE_ONLY_AUDIO:
                if (!mAudioWrapper.isPausedForCache) {
                    mAudioWrapper.isPausedForUser = false;
                    notifyToHandle(mAudioWrapper);
                }
                break;
            case USE_MODE_ONLY_VIDEO:
                if (!mVideoWrapper.isPausedForCache) {
                    mVideoWrapper.isPausedForUser = false;
                    notifyToHandle(mVideoWrapper);
                }
                break;
            default:
                break;
        }
    }

    private void internalPause() {
        switch (use_mode) {
            case FFMPEG.USE_MODE_MEDIA:
                if (!mAudioWrapper.isPausedForCache
                        && !mVideoWrapper.isPausedForCache) {
                    mAudioWrapper.isPausedForUser = true;
                    mVideoWrapper.isPausedForUser = true;
                }
                break;
            case FFMPEG.USE_MODE_ONLY_AUDIO:
                if (!mAudioWrapper.isPausedForCache) {
                    mAudioWrapper.isPausedForUser = true;
                }
                break;
            case USE_MODE_ONLY_VIDEO:
                if (!mVideoWrapper.isPausedForCache) {
                    mVideoWrapper.isPausedForUser = true;
                }
                break;
            default:
                break;
        }
    }

    private void internalRelease() {
        Log.i(TAG, "internalRelease() start");
        //notifyAudioEndOfStream();
        //notifyVideoEndOfStream();
        switch (use_mode) {
            case FFMPEG.USE_MODE_MEDIA:
                isReading = false;
                mAudioWrapper.isPausedForSeek = false;
                mVideoWrapper.isPausedForSeek = false;
                notifyToRead();
                mAudioWrapper.isHandling = false;
                mAudioWrapper.isPausedForUser = false;
                mAudioWrapper.isPausedForCache = false;
                mVideoWrapper.isHandling = false;
                mVideoWrapper.isPausedForUser = false;
                mVideoWrapper.isPausedForCache = false;
                notifyToRead(mAudioWrapper);
                notifyToRead(mVideoWrapper);
                notifyToHandle(mAudioWrapper);
                notifyToHandle(mVideoWrapper);
                break;
            case FFMPEG.USE_MODE_ONLY_AUDIO:
                isReading = false;
                mAudioWrapper.isPausedForSeek = false;
                notifyToRead();
                mAudioWrapper.isHandling = false;
                mAudioWrapper.isPausedForUser = false;
                mAudioWrapper.isPausedForCache = false;
                notifyToRead(mAudioWrapper);
                notifyToHandle(mAudioWrapper);
                break;
            case USE_MODE_ONLY_VIDEO:
                isReading = false;
                mVideoWrapper.isPausedForSeek = false;
                notifyToRead();
                mVideoWrapper.isHandling = false;
                mVideoWrapper.isPausedForUser = false;
                mVideoWrapper.isPausedForCache = false;
                notifyToRead(mVideoWrapper);
                notifyToHandle(mVideoWrapper);
                break;
            default:
                break;
        }
        Log.i(TAG, "internalRelease() end");
    }

    private void internalDestroy() {
        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }
    }

    private String preElapsedTime = null;
    private String curElapsedTime = null;
    //private boolean onlyOneStart = true;

    /***
     * 填充数据送到底层进行编解码
     * @param codec
     * @param data
     * @param offset
     * @param size
     * @param presentationTimeUs
     * @return
     */
    private static boolean feedInputBuffer(
            MediaCodec codec,
            byte[] data,
            int offset,
            int size,
            long presentationTimeUs) {
        try {
            // 拿到房间号
            int roomIndex = codec.dequeueInputBuffer(TIME_OUT);
            if (roomIndex >= 0) {
                ByteBuffer room = null;
                // 根据房间号找到房间
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    room = codec.getInputBuffer(roomIndex);
                } else {
                    room = codec.getInputBuffers()[roomIndex];
                }
                if (room == null) {
                    return false;
                }
                // 入住之前打扫一下房间
                room.clear();
                // 入住
                room.put(data, offset, size);
                int flags = 0;
                if (size <= 0) {
                    presentationTimeUs = 0L;
                    flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                }
                // 通知已经"入住"了,可以进行"编解码"的操作了
                codec.queueInputBuffer(
                        roomIndex,
                        offset,
                        size,
                        presentationTimeUs,
                        flags);
                // reset
                roomIndex = -1;
                room = null;
            }
        } catch (MediaCodec.CryptoException
                | IllegalStateException
                | NullPointerException e) {
            Log.e(TAG, "feedInputBuffer() Input occur exception: " + e);
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /***
     * 拿出数据(在底层已经经过编解码了)进行处理(如视频数据进行渲染,音频数据进行播放)
     * @param wrapper
     * @return
     */
    private boolean drainOutputBuffer(SimpleWrapper wrapper) {
        // 房间信息
        MediaCodec.BufferInfo roomInfo = new MediaCodec.BufferInfo();
        for (; ; ) {
            if (!wrapper.isHandling) {
                return false;
            }
            try {
                // 房间号
                int roomIndex = wrapper.decoderMediaCodec.dequeueOutputBuffer(roomInfo, TIME_OUT);
                /*if (wrapper.type == TYPE_AUDIO) {
                    Log.d(TAG, "drainOutputBuffer() Audio roomIndex: " + roomIndex);
                } else {
                    Log.w(TAG, "drainOutputBuffer() Video roomIndex: " + roomIndex);
                }*/
                switch (roomIndex) {
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        // 像音频,第一个输出日志
                        /*if (wrapper.type == TYPE_AUDIO) {
                            Log.d(TAG, "drainOutputBuffer() " +
                                    "Audio Output MediaCodec.INFO_TRY_AGAIN_LATER");
                        } else {
                            Log.w(TAG, "drainOutputBuffer() " +
                                    "Video Output MediaCodec.INFO_TRY_AGAIN_LATER");
                        }*/
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        // 像音频,第三个输出日志
                        // 一般一个视频各自调用一次
                        if (wrapper.type == TYPE_AUDIO) {
                            Log.d(TAG, "drainOutputBuffer() " +
                                    "Audio Output MediaCodec.INFO_OUTPUT_FORMAT_CHANGED");
                            handleAudioOutputFormat();
                        } else {
                            Log.w(TAG, "drainOutputBuffer() " +
                                    "Video Output MediaCodec.INFO_OUTPUT_FORMAT_CHANGED");
                            handleVideoOutputFormat();
                        }
                        break;
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        // 像音频,第二个输出日志.视频好像没有这个输出日志
                        if (wrapper.type == TYPE_AUDIO) {
                            Log.d(TAG, "drainOutputBuffer() " +
                                    "Audio Output MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                        } else {
                            Log.w(TAG, "drainOutputBuffer() " +
                                    "Video Output MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                        }
                        break;
                    default:
                        break;
                }
                if (roomIndex < 0) {
                    break;
                }

                /*if (wrapper.type == TYPE_AUDIO) {
                } else {
                    // https://www.polarxiong
                    .com/archives/Android-YUV_420_888%E7%BC%96%E7%A0%81Image%E8%BD%AC%E6%8D%A2%E4
                    %B8%BAI420%E5%92%8CNV21%E6%A0%BC%E5%BC%8Fbyte%E6%95%B0%E7%BB%84.html
                    Image image = wrapper.decoderMediaCodec.getOutputImage(roomIndex);
                    Log.w(TAG, "drainOutputBuffer() " +
                            "image format: " + image.getFormat());
                    if (outputImageFileType != -1) {
                        String fileName;
                        switch (outputImageFileType) {
                            case FILE_TypeI420:
                                fileName = OUTPUT_DIR +
                                        String.format("frame_%05d_I420_%dx%d.yuv",
                                                outputFrameCount, width, height);
                                dumpFile(fileName, getDataFromImage(image, COLOR_FormatI420));
                                break;
                            case FILE_TypeNV21:
                                fileName = OUTPUT_DIR +
                                        String.format("frame_%05d_NV21_%dx%d.yuv",
                                                outputFrameCount, width, height);
                                dumpFile(fileName, getDataFromImage(image, COLOR_FormatNV21));
                                break;
                            case FILE_TypeJPEG:
                                fileName = OUTPUT_DIR +
                                        String.format("frame_%05d.jpg", outputFrameCount);
                                compressToJpeg(fileName, image);
                                break;
                        }
                    }
                }*/

                ByteBuffer room = null;
                // 根据房间号找到房间
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    room = wrapper.decoderMediaCodec.getOutputBuffer(roomIndex);
                } else {
                    room = wrapper.decoderMediaCodec.getOutputBuffers()[roomIndex];
                }
                // 房间大小
                int roomSize = roomInfo.size;
                // 不能根据room是否为null来判断是audio还是video(但我的三星Note2手机上是可以的)
                if (room != null) {
                    // audio
                    room.position(roomInfo.offset);
                    room.limit(roomInfo.offset + roomSize);
                    if (wrapper.type == TYPE_AUDIO) {
                        handleAudioOutputBuffer(roomIndex, room, roomInfo, roomSize);
                    } else {
                        handleVideoOutputBuffer(roomIndex, room, roomInfo, roomSize);
                    }
                    room.clear();
                } else {
                    // video
                    handleVideoOutputBuffer(roomIndex, null, roomInfo, roomSize);
                }

                if ((roomInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (wrapper.type == TYPE_AUDIO) {
                        Log.d(TAG, "drainOutputBuffer() " +
                                "Audio Output MediaCodec.BUFFER_FLAG_END_OF_STREAM");
                    } else {
                        Log.w(TAG, "drainOutputBuffer() " +
                                "Video Output MediaCodec.BUFFER_FLAG_END_OF_STREAM");
                    }
                    // 结束
                    return false;
                }
                if ((roomInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    if (wrapper.type == TYPE_AUDIO) {
                        Log.d(TAG, "drainOutputBuffer() " +
                                "Audio Output MediaCodec.BUFFER_FLAG_CODEC_CONFIG");
                    } else {
                        Log.w(TAG, "drainOutputBuffer() " +
                                "Video Output MediaCodec.BUFFER_FLAG_CODEC_CONFIG");
                    }
                }

                wrapper.decoderMediaCodec.releaseOutputBuffer(roomIndex, wrapper.render);
            } catch (IllegalStateException
                    | IllegalArgumentException
                    | NullPointerException e) {
                if (wrapper.type == TYPE_AUDIO) {
                    Log.e(TAG, "drainOutputBuffer() Audio Output occur exception: " + e);
                } else {
                    Log.e(TAG, "drainOutputBuffer() Video Output occur exception: " + e);
                }
                e.printStackTrace();
                return false;
            }
        }// for(;;) end

        return true;
    }

    public boolean feedInputBufferAndDrainOutputBuffer(SimpleWrapper wrapper) {
        if (!wrapper.isHandling) {
            return false;
        }

        // seek的时候就不要往下走了
        if (wrapper.needToSeek) {
            return true;
        }

        /*// 互相等待,一起start
        if (wrapper.type == TYPE_AUDIO) {
            // 如果是音频先走到这里,那么等待视频的开始
            while (!mVideoWrapper.isStarted) {
                if (!wrapper.isHandling) {
                    return false;
                }
                SystemClock.sleep(10);
            }
        } else {
            wrapper.isStarted = true;
            // 如果是视频先走到这里,那么等待音频的开始
            while (!mAudioWrapper.isStarted) {
                if (!wrapper.isHandling) {
                    return false;
                }
                SystemClock.sleep(10);
            }
        }*/

        boolean feedInputBufferResult = false;
        boolean drainOutputBufferResult = false;
        try {
            // Input Data
            feedInputBufferResult = feedInputBuffer(
                    wrapper.decoderMediaCodec,
                    wrapper.data,
                    0,
                    wrapper.size,
                    wrapper.sampleTime);

            // Output Data
            drainOutputBufferResult = drainOutputBuffer(wrapper);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return feedInputBufferResult && drainOutputBufferResult;
    }

    private void handleAudioOutputFormat() {
        /***
         解码前
         {mime=audio/mp4a-latm, aac-profile=2, channel-count=2, track-id=2, bitrate=96000,
         max-input-size=444, durationUs=10871488000,
         csd-0=java.nio.HeapByteBuffer[pos=0 lim=2 cap=2],
         sample-rate=48000, max-bitrate=96000}
         解码后
         {pcm-encoding=2, mime=audio/raw, channel-count=2, sample-rate=48000}
         */
        try {
            MediaFormat newMediaFormat = mAudioWrapper.decoderMediaCodec.getOutputFormat();
            Class clazz = Class.forName("android.media.MediaFormat");
            Method method = clazz.getDeclaredMethod("getMap");
            method.setAccessible(true);
            Object newObject = method.invoke(newMediaFormat);
            Object oldObject = method.invoke(mAudioWrapper.decoderMediaFormat);
            if (newObject != null
                    && newObject instanceof Map
                    && oldObject != null
                    && oldObject instanceof Map) {
                Map<String, Object> newMap = (Map) newObject;
                Map<String, Object> oldMap = (Map) oldObject;
                String mime = (String) oldMap.get("mime");
                for (Map.Entry<String, Object> entry : newMap.entrySet()) {
                    oldMap.put(entry.getKey(), entry.getValue());
                }
                oldMap.put("mime-old", mime);
            }
            Log.d(TAG, "handleAudioOutputFormat() newMediaFormat: " +
                    mAudioWrapper.decoderMediaFormat);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mAudioWrapper.mAudioTrack != null) {
            mAudioWrapper.mAudioTrack.release();
        }

        // 创建AudioTrack
        // 1.
        int sampleRateInHz =
                mAudioWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        // 2.
        int channelCount =
                mAudioWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        // 3.
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        if (mAudioWrapper.decoderMediaFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
            // 关键参数(需要解码后才能知道)
            audioFormat =
                    mAudioWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_PCM_ENCODING);
        }

        // sampleRateInHz: 48000 channelCount: 2 audioFormat: 2
        Log.d(TAG, "handleAudioOutputFormat()" +
                " sampleRateInHz: " + sampleRateInHz +
                " channelCount: " + channelCount +
                " audioFormat: " + audioFormat);

        // create AudioTrack
        mAudioWrapper.mAudioTrack = MediaUtils.createAudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRateInHz, channelCount, audioFormat,
                AudioTrack.MODE_STREAM);
        if (mAudioWrapper.mAudioTrack != null) {
            if (mContext != null) {
                SharedPreferences sp =
                        mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
                boolean isMute = sp.getBoolean(PLAYBACK_IS_MUTE, false);
                if (!isMute) {
                    setVolume(FFMPEG.VOLUME_NORMAL);
                } else {
                    setVolume(FFMPEG.VOLUME_MUTE);
                }
            } else {
                setVolume(FFMPEG.VOLUME_NORMAL);
            }
            mAudioWrapper.mAudioTrack.play();
            mAudioWrapper.isStarted = true;
            Log.d(TAG, "handleAudioOutputFormat() 声音马上输出......");
        } else {
            Log.e(TAG, "handleAudioOutputFormat() AudioTrack is null");
            mAudioWrapper.isHandling = false;
            notifyToRead(mAudioWrapper);
            notifyToHandle(mAudioWrapper);
        }

        // 音视频都已经开始的话,就可以播放了
        if (mCallback != null) {
            Log.i(TAG, "onPlayed");
            mCallback.onPlayed();
        }
    }

    private void handleVideoOutputFormat() {
        /***
         解码前
         {csd-1=java.nio.HeapByteBuffer[pos=0 lim=9 cap=9],
         mime=video/avc, frame-rate=24, track-id=1, profile=8,
         width=1280, height=720, max-input-size=243905, durationUs=10871402208,
         csd-0=java.nio.HeapByteBuffer[pos=0 lim=28 cap=28],
         bitrate-mode=0, level=512}
         解码后
         {crop-top=0, crop-right=1279, color-format=19, height=720,
         color-standard=1, crop-left=0, color-transfer=3, stride=1280,
         mime=video/raw, slice-height=720, width=1280, color-range=2, crop-bottom=719}
         */
        try {
            MediaFormat newMediaFormat = mVideoWrapper.decoderMediaCodec.getOutputFormat();
            Class clazz = Class.forName("android.media.MediaFormat");
            Method method = clazz.getDeclaredMethod("getMap");
            method.setAccessible(true);
            Object newObject = method.invoke(newMediaFormat);
            Object oldObject = method.invoke(mVideoWrapper.decoderMediaFormat);
            if (newObject != null
                    && newObject instanceof Map
                    && oldObject != null
                    && oldObject instanceof Map) {
                Map<String, Object> newMap = (Map) newObject;
                Map<String, Object> oldMap = (Map) oldObject;
                for (Map.Entry<String, Object> entry : newMap.entrySet()) {
                    oldMap.put(entry.getKey(), entry.getValue());
                }
            }
            Log.w(TAG, "handleVideoOutputFormat() newMediaFormat: " +
                    mVideoWrapper.decoderMediaFormat);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mVideoWrapper.isStarted = true;
        // 如果是视频先走到这里,那么等待音频的开始
        /*while (!mAudioWrapper.isStarted) {
            if (!mVideoWrapper.isHandling) {
                return;
            }
            SystemClock.sleep(10);
        }*/

        Log.w(TAG, "handleVideoOutputFormat() 画面马上输出......");
    }

    private int handleAudioOutputBuffer(int roomIndex, ByteBuffer room,
                                        MediaCodec.BufferInfo roomInfo, int roomSize) {
        mAudioWrapper.presentationTimeUs = roomInfo.presentationTimeUs;
        // 输出音频
        if (mAudioWrapper.isHandling
                && mAudioWrapper.mAudioTrack != null
                && mAudioWrapper.mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
            byte[] audioData = new byte[roomSize];
            room.get(audioData, 0, audioData.length);
            mAudioWrapper.mAudioTrack.write(audioData, roomInfo.offset, audioData.length);
            //mAudioWrapper.mAudioTrack.write(audioData, 0, audioData.length);
        }

        return 0;
    }

    //private boolean mIsSeeked = false;
    //private int mCountWithSeek = 0;
    private long prePresentationTimeUs;
    private static final int RUN_COUNTS = 100;
    private int[] timeDiff = new int[RUN_COUNTS];
    private int TIME_DIFFERENCE = 150;
    private int runCounts = 0;
    private int averageTimeDiff = 0;

    // 最最最关键的一步
    private int handleVideoOutputBuffer(int roomIndex, ByteBuffer room,
                                        MediaCodec.BufferInfo roomInfo, int roomSize) {
        mVideoWrapper.presentationTimeUs = roomInfo.presentationTimeUs;

        if (mVideoWrapper.presentationTimeUs > 0
                && mAudioWrapper.presentationTimeUs > 0) {
            int diffTime =
                    (int) ((mVideoWrapper.presentationTimeUs - mAudioWrapper.presentationTimeUs) / 1000);
            if (runCounts < RUN_COUNTS) {
                if (diffTime > 0) {
                    timeDiff[runCounts++] = diffTime;
                }
            } else if (runCounts == RUN_COUNTS) {
                runCounts++;
                int totleTimeDiff = 0;
                for (int i = 0; i < RUN_COUNTS; i++) {
                    totleTimeDiff += timeDiff[i];
                }
                averageTimeDiff = totleTimeDiff / RUN_COUNTS;
                // 希望得到一个好的TIME_DIFFERENCE值
                hope_to_get_a_good_result();

            }
            if (diffTime < 0) {
                return 0;
            }
            /*if (diffTime > averageTimeDiff) {
                mVideoWrapper.presentationTimeUs =
                        mAudioWrapper.presentationTimeUs + averageTimeDiff * 1000;
            }*/
            while (diffTime > TIME_DIFFERENCE) {
                if (!mVideoWrapper.isHandling
                        || !mAudioWrapper.isHandling
                        || mVideoWrapper.isPausedForUser
                        || mAudioWrapper.isPausedForUser
                        || mVideoWrapper.isPausedForCache
                        || mAudioWrapper.isPausedForCache
                        || mVideoWrapper.isPausedForSeek
                        || mAudioWrapper.isPausedForSeek) {
                    return 0;
                }

                SystemClock.sleep(1);
                diffTime =
                        (int) ((mVideoWrapper.presentationTimeUs - mAudioWrapper.presentationTimeUs) / 1000);
            }
        }

        SystemClock.sleep(11);

        return 0;
    }

    private void hope_to_get_a_good_result() {
        Log.w(TAG, "handleVideoOutputBuffer() averageTimeDiff: " + averageTimeDiff);
        if (averageTimeDiff > 1000) {
            TIME_DIFFERENCE = 1000;
        } else if (averageTimeDiff > 900 && averageTimeDiff <= 1000) {
            TIME_DIFFERENCE = 900;
        } else if (averageTimeDiff > 800 && averageTimeDiff <= 900) {
            TIME_DIFFERENCE = 800;
        } else if (averageTimeDiff > 700 && averageTimeDiff <= 800) {
            TIME_DIFFERENCE = 700;
        } else if (averageTimeDiff > 600 && averageTimeDiff <= 700) {
            TIME_DIFFERENCE = 600;
        } else if (averageTimeDiff > 500 && averageTimeDiff <= 600) {
            TIME_DIFFERENCE = 500;
        } else if (averageTimeDiff > 400 && averageTimeDiff <= 500) {
            TIME_DIFFERENCE = 400;
        } else if (averageTimeDiff > 300 && averageTimeDiff <= 400) {
            TIME_DIFFERENCE = 300;
        } else if (averageTimeDiff > 200 && averageTimeDiff <= 300) {
            TIME_DIFFERENCE = 200;
        } else if (averageTimeDiff > 100 && averageTimeDiff <= 200) {
            TIME_DIFFERENCE = 100;
        } else if (averageTimeDiff <= 100) {
            TIME_DIFFERENCE = 50;
        }

        Log.w(TAG, "handleVideoOutputBuffer() TIME_DIFFERENCE: " + TIME_DIFFERENCE);
        if (mCallback != null) {
            mCallback.onInfo(String.valueOf(TIME_DIFFERENCE));
        }
    }

    private void notifyAudioEndOfStream() {
        Log.d(TAG, "notifyAudioEndOfStream() start");
        // audio end notify
        int inputAudioBufferIndex = mAudioWrapper.decoderMediaCodec.dequeueInputBuffer(0);
        while (inputAudioBufferIndex < 0) {
            inputAudioBufferIndex = mAudioWrapper.decoderMediaCodec.dequeueInputBuffer(0);
        }

        long presentationTime = System.nanoTime() / 1000;
        mAudioWrapper.decoderMediaCodec.queueInputBuffer(
                inputAudioBufferIndex,
                0,
                0,
                presentationTime,
                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
        Log.d(TAG, "notifyAudioEndOfStream() end");
    }

    private void notifyVideoEndOfStream() {
        Log.w(TAG, "notifyVideoEndOfStream() start");
        // video end notify
        int inputBufferIndex = mVideoWrapper.decoderMediaCodec.dequeueInputBuffer(0);
        while (inputBufferIndex < 0) {
            inputBufferIndex = mVideoWrapper.decoderMediaCodec.dequeueInputBuffer(0);
        }

        long presentationTime = System.nanoTime() / 1000;
        mVideoWrapper.decoderMediaCodec.queueInputBuffer(
                inputBufferIndex,
                0,
                0,
                presentationTime,
                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
        Log.w(TAG, "notifyVideoEndOfStream() end");
    }

    private void notifyToRead() {
        synchronized (readDataLock) {
            readDataLock.notify();
        }
    }

    private void notifyToReadWait() {
        try {
            synchronized (readDataLock) {
                readDataLock.wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void notifyToRead(SimpleWrapper wrapper) {
        if (wrapper == null) {
            return;
        }
        synchronized (wrapper.readDataLock) {
            wrapper.readDataLock.notify();
        }
    }

    private void notifyToReadWait(SimpleWrapper wrapper) {
        if (wrapper == null) {
            return;
        }
        try {
            synchronized (wrapper.readDataLock) {
                wrapper.readDataLock.wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void notifyToHandle(SimpleWrapper wrapper) {
        if (wrapper == null) {
            return;
        }
        synchronized (wrapper.handleDataLock) {
            wrapper.handleDataLock.notify();
        }
    }

    private void notifyToHandleWait(SimpleWrapper wrapper) {
        if (wrapper == null) {
            return;
        }
        try {
            synchronized (wrapper.handleDataLock) {
                wrapper.handleDataLock.wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}

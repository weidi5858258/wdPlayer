package com.weidi.media.wdplayer.video_player;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.weidi.media.wdplayer.util.Callback;
import com.weidi.media.wdplayer.util.JniObject;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

import static com.weidi.media.wdplayer.Constants.HARD_SOLUTION;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_IS_MUTE;
import static com.weidi.media.wdplayer.Constants.PREFERENCES_NAME;
import static com.weidi.media.wdplayer.Constants.PREFERENCES_NAME_REMOTE;

/***
 ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0);//关闭mediacodec硬解，使用软解
 ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);//开启mediacodec硬解

 ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 5);   //丢帧
 是在视频帧处理不过来的时候丢弃一些帧达到同步的效果
 ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 0);
 //设置是否开启环路过滤: 0开启，画面质量高，解码开销大，48关闭，画面质量差点，解码开销小
 //播放延时的解决方案
 ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", 1);//设置播放前的探测时间
 1,达到首屏秒开效果
 //如果是rtsp协议，可以优先用tcp(默认是用udp)
 ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "rtsp_transport", "tcp");
 ijkMediaPlayer.setOption(1, "analyzemaxduration", 100L);
 ijkMediaPlayer.setOption(1, "flush_packets", 1L);
 ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 1);
 //需要准备好后自动播放
 ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "fast", 1);//不额外优化
 ijkMediaPlayer.setOption(4, "packet-buffering",  0);  //是否开启预缓冲，一般直播项目会开启，达到秒开的效果，不过带来了播放丢帧卡顿的体验
 ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 0);  //自动旋屏
 ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,
 "mediacodec-handle-resolution-change", 0);   //处理分辨率变化
 ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "max-buffer-size", 0);//最大缓冲大小,单位kb
 ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "min-frames", 2);   //默认最小帧数2
 ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,  "max_cached_duration", 3);   //最大缓存时长
 ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,  "infbuf", 1);   //是否限制输入缓存数
 ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "fflags", "nobuffer");
 ijkMediaPlayer.setOption(1, "probesize", 200);  //播放前的探测Size，默认是1M, 改小一点会出画面更快
 ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,"reconnect",5);  //播放重连次数


 ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_clear", 1);
 //因为项目中多次调用播放器，有网络视频，resp，本地视频，还有wifi上http视频，所以得清空DNS才能播放WIFI上的视频
 如果项目无法播放远程视频,可以试试这句话 Server returned 4XX Client Error, but not one of 40{0,1,3,4}报这个错误也可以试试
 */
public class IjkPlayer implements WdPlayer {

    private static final String TAG = "player_alexander";

    // all possible internal states
    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARING = 1;
    private static final int STATE_PREPARED = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;
    private int mCurrentState = STATE_IDLE;
    //    private int mTargetState = STATE_IDLE;

    private IjkMediaPlayer mIjkMediaPlayer = null;
    private Context mContext = null;
    private String mPath = null;
    private Handler mUiHandler = null;
    private Callback mCallback = null;
    private Surface mSurface = null;
    private long mPositionMs;
    public boolean mIsLocal = true;
    public boolean mIsLive = false;
    public boolean mIsLocalPlayer = true;
    private boolean mHasPlayed = true;


    //    private IMediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener;
    //    private IMediaPlayer.OnSeekCompleteListener mOnSeekCompleteListener;
    //    private IMediaPlayer.OnCompletionListener mOnCompletionListener;
    //    private IMediaPlayer.OnPreparedListener mOnPreparedListener;
    //    private IMediaPlayer.OnErrorListener mOnErrorListener;
    //    private IMediaPlayer.OnInfoListener mOnInfoListener;

    public IjkPlayer() {
        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                IjkPlayer.this.uiHandleMessage(msg);
            }
        };
    }

    @Override
    public void setContext(Context context) {
        mContext = context;
    }

    @Override
    public void setHandler(Handler handler) {
        //mUiHandler = handler;
    }

    @Override
    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    @Override
    public void setDataSource(String path) {
        mPath = path;
    }

    @Override
    public void setSurface(Surface surface) {
        mSurface = surface;
    }

    @Override
    public void setVolume(float volume) {
        if (mIjkMediaPlayer == null) {
            return;
        }

        mIjkMediaPlayer.setVolume(volume, volume);
    }

    @Override
    public void seekTo(long second) {
        mPositionMs = second * 1000;

        if (mIjkMediaPlayer == null) {
            return;
        }
        if (mCurrentState == STATE_PREPARED && mPositionMs >= 0) {
            try {
                mIjkMediaPlayer.seekTo(mPositionMs);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean prepareSync() {
        return true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public boolean prepareAsync() {
        return true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void start() {
        if (mCallback != null) {
            mCallback.onReady();
        }
        // we shouldn't clear the target state,
        // because somebody might have called start() previously
        release(false);

        mHasPlayed = true;
        mCurrentState = STATE_IDLE;

        // init player
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");

        try {
            createPlayer();
            mIjkMediaPlayer.setOnPreparedListener(mPreparedListener);
            mIjkMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            mIjkMediaPlayer.setOnSeekCompleteListener(mSeekCompleteListener);
            mIjkMediaPlayer.setOnCompletionListener(mCompletionListener);
            mIjkMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
            mIjkMediaPlayer.setOnErrorListener(mErrorListener);
            mIjkMediaPlayer.setOnInfoListener(mInfoListener);

            mIjkMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mIjkMediaPlayer.setScreenOnWhilePlaying(true);

            mIjkMediaPlayer.setSurface(mSurface);
            mIjkMediaPlayer.setDataSource(mPath);

            mIjkMediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
            //mCurrentState = STATE_ERROR;
            //mTargetState = STATE_ERROR;
            mErrorListener.onError(mIjkMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        } finally {

        }

        mUiHandler.removeMessages(Callback.MSG_ON_TRANSACT_PROGRESS_UPDATED);
        mUiHandler.sendEmptyMessage(Callback.MSG_ON_TRANSACT_PROGRESS_UPDATED);
    }

    @Override
    public void play() {
        if (isInPlaybackState()) {
            mIjkMediaPlayer.start();
            //mCurrentState = STATE_PLAYING;
        }
        //mTargetState = STATE_PLAYING;
    }

    @Override
    public void pause() {
        if (isInPlaybackState()) {
            mIjkMediaPlayer.pause();
            //mCurrentState = STATE_PAUSED;
        }
        //mTargetState = STATE_PAUSED;
    }

    @Override
    public void release() {
        mUiHandler.removeMessages(Callback.MSG_ON_TRANSACT_PROGRESS_UPDATED);
        if (mIjkMediaPlayer == null) {
            if (mCallback != null) {
                mCallback.onFinished();
            }
            return;
        }

        mIjkMediaPlayer.stop();
        mIjkMediaPlayer.release();
        mIjkMediaPlayer = null;
        //mCurrentState = STATE_IDLE;
        //mTargetState = STATE_IDLE;
        release(true);

        IjkMediaPlayer.native_profileEnd();

        if (mUiHandler != null) {
            mUiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mCallback != null) {
                        mCallback.onFinished();
                    }
                }
            }, 1000);
        } else {
            if (mCallback != null) {
                mCallback.onFinished();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return mIjkMediaPlayer != null;
    }

    public boolean isPlaying() {
        if (mIjkMediaPlayer == null) {
            return false;
        }

        return mIjkMediaPlayer.isPlaying();
    }

    @Override
    public long getDuration() {
        if (mIjkMediaPlayer == null) {
            return 0;
        }

        return mIjkMediaPlayer.getDuration() / 1000;
    }

    @Override
    public String onTransact(int code, JniObject jniObject) {
        return null;
    }

    private void release(boolean cleartargetstate) {
        if (mIjkMediaPlayer == null) {
            return;
        }

        mIjkMediaPlayer.reset();
        mIjkMediaPlayer.release();
        mIjkMediaPlayer = null;
    }

    private void createPlayer() {
        mIjkMediaPlayer = new IjkMediaPlayer();
        mIjkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG);

        boolean useHardSolution = true;
        if (mContext != null) {
            SharedPreferences sp =
                    mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
            int softSolution = sp.getInt(HARD_SOLUTION, 1);
            if (softSolution == 0) {
                useHardSolution = false;
            }
        }

        if (useHardSolution) {
            mIjkMediaPlayer.setOption(
                    IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
            mIjkMediaPlayer.setOption(
                    IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
            mIjkMediaPlayer.setOption(
                    IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1);
            mIjkMediaPlayer.setOption(
                    IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 1);
        } else {
            mIjkMediaPlayer.setOption(
                    IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0);
            mIjkMediaPlayer.setOption(
                    IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 0);
            mIjkMediaPlayer.setOption(
                    IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 0);
            mIjkMediaPlayer.setOption(
                    IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 0);
        }

        mIjkMediaPlayer.setOption(
                IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", IjkMediaPlayer.SDL_FCC_RV32);
        // 跳帧处理,放CPU处理较慢时，进行跳帧处理，保证播放流程，画面和声音同步
        mIjkMediaPlayer.setOption(
                IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 5);// 原来为1
        mIjkMediaPlayer.setOption(
                IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);
        mIjkMediaPlayer.setOption(
                IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);
        mIjkMediaPlayer.setOption(
                IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
        // 某些视频在SeekTo的时候，会跳回到拖动前的位置，这是因为视频的关键帧的问题，
        // 通俗一点就是FFMPEG不兼容，视频压缩过于厉害，seek只支持关键帧，
        // 出现这个情况就是原始的视频文件中i 帧比较少
        mIjkMediaPlayer.setOption(
                IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", 1);
        // 设置seekTo能够快速seek到指定位置并播放
        mIjkMediaPlayer.setOption(
                IjkMediaPlayer.OPT_CATEGORY_FORMAT, "fflags", "fastseek");

        if (mIsLocal) {
            // 设置播放前的探测时间 1,达到首屏秒开效果
            mIjkMediaPlayer.setOption(
                    IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", 1);
            mIjkMediaPlayer.setOption(
                    IjkMediaPlayer.OPT_CATEGORY_PLAYER, "min-frames", 100);

            mIjkMediaPlayer.setCacheShare(100);
        } else {
            // 清空DNS,有时因为在APP里面要播放多种类型的视频(如:MP4,直播,直播平台保存的视频,和其他http视频),
            // 有时会造成因为DNS的问题而报10000问题
            mIjkMediaPlayer.setOption(
                    IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_clear", 1);
            // 重连模式，如果中途服务器断开了连接，让它重新连接,
            // 参考 https://github.com/Bilibili/ijkplayer/issues/445
            mIjkMediaPlayer.setOption(
                    IjkMediaPlayer.OPT_CATEGORY_FORMAT, "reconnect", 1);

            mIjkMediaPlayer.setCacheShare(10000);
        }
    }

    private boolean isInPlaybackState() {
        return (mIjkMediaPlayer != null/* &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING*/);
    }

    private void uiHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        switch (msg.what) {
            case Callback.MSG_ON_TRANSACT_PROGRESS_UPDATED:
                if (mCallback != null && mIjkMediaPlayer != null) {
                    long position = mIjkMediaPlayer.getCurrentPosition();
                    if (!mIsLive) {
                        if (position > 0) {
                            mCallback.onProgressUpdated(position / 1000);
                        } else {
                            mUiHandler.removeMessages(Callback.MSG_ON_TRANSACT_PROGRESS_UPDATED);
                            mUiHandler.sendEmptyMessageDelayed(
                                    Callback.MSG_ON_TRANSACT_PROGRESS_UPDATED, 10);
                            return;
                        }
                    }

                    /*Log.i(TAG, "videoBytes: " + mIjkMediaPlayer.getVideoCachedBytes() +
                            " audioBytes: " + mIjkMediaPlayer.getAudioCachedBytes() +
                            " videopackets: " + mIjkMediaPlayer.getVideoCachedPackets() +
                            " audiopackets: " + mIjkMediaPlayer.getAudioCachedPackets());*/

                    if (!mIsLocal) {
                        int videoPackets = (int) mIjkMediaPlayer.getVideoCachedPackets();
                        int audioPackets = (int) mIjkMediaPlayer.getAudioCachedPackets();
                        if (mHasPlayed && (videoPackets <= 10 || audioPackets <= 10)) {
                            mHasPlayed = false;
                            mCallback.onPaused();
                        } else if (!mHasPlayed && videoPackets > 10 && audioPackets > 10) {
                            mHasPlayed = true;
                            mCallback.onPlayed();
                        }
                        mCallback.onTransact(Callback.MSG_ON_TRANSACT_VIDEO_PRODUCER,
                                FFMPEG.videoProducer.writeInt(videoPackets));
                        mCallback.onTransact(Callback.MSG_ON_TRANSACT_AUDIO_PRODUCER,
                                FFMPEG.audioProducer.writeInt(audioPackets));
                    }

                    mUiHandler.removeMessages(Callback.MSG_ON_TRANSACT_PROGRESS_UPDATED);
                    mUiHandler.sendEmptyMessageDelayed(
                            Callback.MSG_ON_TRANSACT_PROGRESS_UPDATED, 1000);
                }
                break;
            default:
                break;
        }
    }

    private final IMediaPlayer.OnPreparedListener mPreparedListener =
            new IMediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(IMediaPlayer mp) {
                    Log.i(TAG, "onPrepared()");
                    mCurrentState = STATE_PREPARED;
                    int videoWidth = mp.getVideoWidth();
                    int videoHeight = mp.getVideoHeight();
                    if (mCallback != null) {
                        mCallback.onChangeWindow(videoWidth, videoHeight);
                    }
                    if (mPositionMs >= 0) {
                        try {
                            mp.seekTo(mPositionMs);
                        } catch (IllegalStateException e) {
                            e.printStackTrace();
                        }
                    }

                    play();

                    if (mContext != null) {
                        SharedPreferences sp;
                        if (mIsLocalPlayer) {
                            sp = mContext.getSharedPreferences(
                                    PREFERENCES_NAME, Context.MODE_PRIVATE);
                        } else {
                            sp = mContext.getSharedPreferences(
                                    PREFERENCES_NAME_REMOTE, Context.MODE_PRIVATE);
                        }
                        boolean isMute = sp.getBoolean(PLAYBACK_IS_MUTE, false);
                        if (!isMute) {
                            setVolume(FFMPEG.VOLUME_NORMAL);
                        } else {
                            setVolume(FFMPEG.VOLUME_MUTE);
                        }
                    }
                }
            };
    private final IMediaPlayer.OnVideoSizeChangedListener mSizeChangedListener =
            new IMediaPlayer.OnVideoSizeChangedListener() {
                @Override
                public void onVideoSizeChanged(IMediaPlayer mp,
                                               int width, int height,
                                               int sar_num, int sar_den) {
                    Log.i(TAG, "onVideoSizeChanged() width: " + width + " height: " + height);
                }
            };
    private final IMediaPlayer.OnSeekCompleteListener mSeekCompleteListener =
            new IMediaPlayer.OnSeekCompleteListener() {
                @Override
                public void onSeekComplete(IMediaPlayer mp) {
                    Log.i(TAG, "onSeekComplete()");
                    if (mCallback != null) {
                        mCallback.onPlayed();
                    }
                }
            };
    private final IMediaPlayer.OnCompletionListener mCompletionListener =
            new IMediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(IMediaPlayer mp) {
                    Log.i(TAG, "onCompletion()");
                    mCurrentState = STATE_IDLE;
                    if (mCallback != null) {
                        mCallback.onFinished();
                    }
                }
            };
    private final IMediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener =
            new IMediaPlayer.OnBufferingUpdateListener() {
                @Override
                public void onBufferingUpdate(IMediaPlayer mp, int percent) {
                    //Log.i(TAG, "onBufferingUpdate() percent: " + percent);
                }
            };
    private final IMediaPlayer.OnInfoListener mInfoListener =
            new IMediaPlayer.OnInfoListener() {
                @Override
                public boolean onInfo(IMediaPlayer mp, int what, int extra) {
                    //Log.i(TAG, "onInfo() what: " + what + " extra: " + extra);
                    /*if (mCallback != null) {
                        mCallback.onInfo("what: " + what + " extra: " + extra);
                    }*/
                    return false;
                }
            };
    private final IMediaPlayer.OnErrorListener mErrorListener =
            new IMediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(IMediaPlayer mp, int what, int extra) {
                    Log.e(TAG, "onError() what: " + what + " extra: " + extra);
                    if (mCallback != null) {
                        mCallback.onError(what, "what: " + what + " extra: " + extra);
                    }
                    return false;
                }
            };

}

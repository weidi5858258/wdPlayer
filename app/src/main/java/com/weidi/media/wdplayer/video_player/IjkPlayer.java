package com.weidi.media.wdplayer.video_player;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.weidi.media.wdplayer.util.Callback;

import java.io.IOException;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class IjkPlayer {

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

    public void setContext(Context context) {
        mContext = context;
    }

    public void setHandler(Handler handler) {
        //mUiHandler = handler;
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
        if (mIjkMediaPlayer == null) {
            return;
        }

        mIjkMediaPlayer.setVolume(volume, volume);
    }

    public void seekTo(long msec) {
        mPositionMs = msec;

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

    public boolean isPlaying() {
        if (mIjkMediaPlayer == null) {
            return false;
        }

        return mIjkMediaPlayer.isPlaying();
    }

    public long getDuration() {
        if (mIjkMediaPlayer == null) {
            return 0;
        }

        return mIjkMediaPlayer.getDuration();
    }

    public void start() {
        if (isInPlaybackState()) {
            mIjkMediaPlayer.start();
            //mCurrentState = STATE_PLAYING;
        }
        //mTargetState = STATE_PLAYING;
    }

    public void pause() {
        if (isInPlaybackState()) {
            mIjkMediaPlayer.pause();
            //mCurrentState = STATE_PAUSED;
        }
        //mTargetState = STATE_PAUSED;
    }

    public void stop() {
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

    private void release(boolean cleartargetstate) {
        if (mIjkMediaPlayer == null) {
            return;
        }

        mIjkMediaPlayer.reset();
        mIjkMediaPlayer.release();
        mIjkMediaPlayer = null;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void prepareAsync() {
        if (mCallback != null) {
            mCallback.onReady();
        }
        // we shouldn't clear the target state,
        // because somebody might have called start() previously
        release(false);

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
    }

    private void createPlayer() {
        mIjkMediaPlayer = new IjkMediaPlayer();
        mIjkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG);

        /*mIjkMediaPlayer.setOption(
                IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0);
        mIjkMediaPlayer.setOption(
                IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 0);
        mIjkMediaPlayer.setOption(
                IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 0);
        mIjkMediaPlayer.setOption(
                IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 0);*/

        mIjkMediaPlayer.setOption(
                IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
        mIjkMediaPlayer.setOption(
                IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
        mIjkMediaPlayer.setOption(
                IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1);
        mIjkMediaPlayer.setOption(
                IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 1);
        mIjkMediaPlayer.setOption(
                IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", IjkMediaPlayer.SDL_FCC_RV32);
        mIjkMediaPlayer.setOption(
                IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1);
        mIjkMediaPlayer.setOption(
                IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);
        mIjkMediaPlayer.setOption(
                IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);
        mIjkMediaPlayer.setOption(
                IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
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
                    mCallback.onProgressUpdated(position / 1000);
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

                    start();
                    mUiHandler.removeMessages(Callback.MSG_ON_TRANSACT_PROGRESS_UPDATED);
                    mUiHandler.sendEmptyMessage(Callback.MSG_ON_TRANSACT_PROGRESS_UPDATED);
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

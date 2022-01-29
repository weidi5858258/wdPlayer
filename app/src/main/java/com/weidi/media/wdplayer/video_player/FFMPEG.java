package com.weidi.media.wdplayer.video_player;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import com.weidi.eventbus.Phone;
import com.weidi.media.wdplayer.util.Callback;
import com.weidi.media.wdplayer.util.EDMediaCodec;
import com.weidi.media.wdplayer.util.JniObject;
import com.weidi.media.wdplayer.util.MediaUtils;
import com.weidi.utils.MyToast;

import java.io.File;

import static com.weidi.media.wdplayer.Constants.BUTTON_CLICK_EXIT;
import static com.weidi.media.wdplayer.Constants.DO_SOMETHING_EVENT_REPLAY;
import static com.weidi.media.wdplayer.Constants.MEDIACODEC_TIME_OUT;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_IS_MUTE;
import static com.weidi.media.wdplayer.Constants.PLAYER_FFMPEG_MEDIACODEC;
import static com.weidi.media.wdplayer.Constants.PLAYER_FFPLAY;
import static com.weidi.media.wdplayer.Constants.PREFERENCES_NAME;
import static com.weidi.media.wdplayer.Constants.PREFERENCES_NAME_REMOTE;


/***
 Created by root on 19-8-8.
 */

public class FFMPEG implements WdPlayer {

    private static final String TAG = "player_alexander";// player_alexander
    // FFMPEG.class.getSimpleName();

    // status_t onTransact(uint32_t code, const Parcel& data, Parcel* reply, uint32_t flags = 0)
    // public native String onTransact(int code, Parcel data, Parcel reply);
    // 从上面调到下面只定义这一个方法
    @Override
    public native String onTransact(int code, JniObject jniObject);

    static {
        // 需要在CMakeLists.txt文件中的target_link_libraries(...)中加上下面3个库才能加载成功
        /*try {
            System.loadLibrary("ijkffmpeg");
        } catch (UnsatisfiedLinkError error) {
            Log.e(TAG, "卧槽, ijkffmpeg库加载失败了!!!");
            error.printStackTrace();
        }
        try {
            System.loadLibrary("ijkplayer");
        } catch (UnsatisfiedLinkError error) {
            Log.e(TAG, "卧槽, ijkplayer库加载失败了!!!");
            error.printStackTrace();
        }
        try {
            System.loadLibrary("ijksdl");
        } catch (UnsatisfiedLinkError error) {
            Log.e(TAG, "卧槽, ijksdl库加载失败了!!!");
            error.printStackTrace();
        }*/

        try {
            System.loadLibrary("ffmpeg");
        } catch (UnsatisfiedLinkError error) {
            Log.e(TAG, "卧槽, ffmpeg库加载失败了!!!");
            error.printStackTrace();
        }
    }

    private volatile static FFMPEG sFFMPEG;

    private FFMPEG() {
        Phone.register(this);
    }

    public static FFMPEG getDefault() {
        if (sFFMPEG == null) {
            synchronized (FFMPEG.class) {
                if (sFFMPEG == null) {
                    sFFMPEG = new FFMPEG();
                }
            }
        }
        return sFFMPEG;
    }

    public String whatPlayer = PLAYER_FFPLAY;
    private AudioTrack mAudioTrack;
    public static final float VOLUME_NORMAL = 1.0f;
    public static final float VOLUME_MUTE = 0.0f;

    // 更新视频流下载量
    public static final JniObject videoProducer = JniObject.obtain();
    // 更新视频流消耗量
    public static final JniObject videoConsumer = JniObject.obtain();
    public static final JniObject audioProducer = JniObject.obtain();
    public static final JniObject audioConsumer = JniObject.obtain();
    // 更新进度
    public static final JniObject processUpdate = JniObject.obtain();

    public static final int USE_MODE_MEDIA = 1;
    public static final int USE_MODE_ONLY_VIDEO = 2;
    public static final int USE_MODE_ONLY_AUDIO = 3;
    public static final int USE_MODE_AUDIO_VIDEO = 4;
    public static final int USE_MODE_AAC_H264 = 5;
    public static final int USE_MODE_MEDIA_4K = 6;
    public static final int USE_MODE_MEDIA_MEDIACODEC = 7;
    public static final int USE_MODE_MEDIA_FFPLAY = 8;

    // 0(开始下载,边播放边下) 1(停止下载) 2(只下载音频,暂时不用) 3(只下载视频,暂时不用)
    // 4(只下载,不播放.不调用seekTo) 5(只提取音视频,不播放.调用seekTo到0)
    public static final int DO_SOMETHING_CODE_init = 1099;
    public static final int DO_SOMETHING_CODE_setMode = 1100;
    //public static final int DO_SOMETHING_CODE_setCallback = 1101;
    public static final int DO_SOMETHING_CODE_setSurface = 1102;
    public static final int DO_SOMETHING_CODE_initPlayer = 1103;
    public static final int DO_SOMETHING_CODE_readData = 1104;
    public static final int DO_SOMETHING_CODE_audioHandleData = 1105;
    public static final int DO_SOMETHING_CODE_videoHandleData = 1106;
    public static final int DO_SOMETHING_CODE_play = 1107;
    public static final int DO_SOMETHING_CODE_pause = 1108;
    public static final int DO_SOMETHING_CODE_stop = 1109;
    public static final int DO_SOMETHING_CODE_release = 1110;
    public static final int DO_SOMETHING_CODE_isRunning = 1111;
    public static final int DO_SOMETHING_CODE_isPlaying = 1112;
    public static final int DO_SOMETHING_CODE_isPausedForUser = 1113;
    public static final int DO_SOMETHING_CODE_stepAdd = 1114;
    public static final int DO_SOMETHING_CODE_stepSubtract = 1115;
    // 单位: 秒
    public static final int DO_SOMETHING_CODE_seekTo = 1116;
    // 单位: 秒
    public static final int DO_SOMETHING_CODE_getDuration = 1117;
    public static final int DO_SOMETHING_CODE_download = 1118;
    public static final int DO_SOMETHING_CODE_closeJni = 1119;
    public static final int DO_SOMETHING_CODE_videoHandleRender = 1120;
    public static final int DO_SOMETHING_CODE_handleAudioOutputBuffer = 1121;
    public static final int DO_SOMETHING_CODE_handleVideoOutputBuffer = 1122;
    public static final int DO_SOMETHING_CODE_frameByFrameForReady = 1123;
    public static final int DO_SOMETHING_CODE_frameByFrameForFinish = 1124;
    public static final int DO_SOMETHING_CODE_frameByFrame = 1125;
    public static final int DO_SOMETHING_CODE_isWatch = 1126;
    public static final int DO_SOMETHING_CODE_isWatchForCloseVideo = 1127;
    public static final int DO_SOMETHING_CODE_isWatchForCloseAudio = 1128;
    public static final int DO_SOMETHING_CODE_setTimeDifference = 1129;
    public static final int DO_SOMETHING_CODE_setRemainingTime = 1130;
    public static final int DO_SOMETHING_CODE_clearQueue = 1131;
    public static final int DO_SOMETHING_CODE_postDelayed = 1132;
    public static final int DO_SOMETHING_CODE_allow_exit = 1133;
    public static final int DO_SOMETHING_CODE_replay = 1134;

    private byte[] eof = new byte[]{-1, -1, -1, -1, -1};

    private FfmpegUseMediaCodecDecode mFfmpegUseMediaCodecDecode;

    public void setFfmpegUseMediaCodecDecode(FfmpegUseMediaCodecDecode decode) {
        mFfmpegUseMediaCodecDecode = decode;
    }

    // 供jni层调用
    private boolean initMediaCodec(int type, JniObject jniObject) {
        if (mFfmpegUseMediaCodecDecode != null) {
            switch (type) {
                case FfmpegUseMediaCodecDecode.TYPE_AUDIO:
                    return mFfmpegUseMediaCodecDecode.initAudioMediaCodec(jniObject);
                case FfmpegUseMediaCodecDecode.TYPE_VIDEO:
                    return mFfmpegUseMediaCodecDecode.initVideoMediaCodec(jniObject);
                default:
                    break;
            }
        }
        return false;
    }

    // 供jni层调用
    private boolean feedInputBufferAndDrainOutputBuffer(
            int type, byte[] data, int size, long presentationTimeUs) {
        if (mFfmpegUseMediaCodecDecode != null) {
            switch (type) {
                case FfmpegUseMediaCodecDecode.TYPE_AUDIO:
                    if (mFfmpegUseMediaCodecDecode.mAudioWrapper != null) {
                        mFfmpegUseMediaCodecDecode.mAudioWrapper.data = data;
                        mFfmpegUseMediaCodecDecode.mAudioWrapper.size = size;
                        mFfmpegUseMediaCodecDecode.mAudioWrapper.pts = presentationTimeUs;
                        return mFfmpegUseMediaCodecDecode.feedInputBufferAndDrainOutputBuffer(
                                mFfmpegUseMediaCodecDecode.mAudioWrapper);
                    }
                    break;
                case FfmpegUseMediaCodecDecode.TYPE_VIDEO:
                    if (mFfmpegUseMediaCodecDecode.mVideoWrapper != null) {
                        mFfmpegUseMediaCodecDecode.mVideoWrapper.data = data;
                        mFfmpegUseMediaCodecDecode.mVideoWrapper.size = size;
                        mFfmpegUseMediaCodecDecode.mVideoWrapper.pts = presentationTimeUs;
                        return mFfmpegUseMediaCodecDecode.feedInputBufferAndDrainOutputBuffer(
                                mFfmpegUseMediaCodecDecode.mVideoWrapper);
                    }
                    break;
                default:
                    break;

            }
        }
        return false;
    }

    // presentationTimeUs = pts
    private boolean feedInputBufferAndDrainOutputBuffer2(
            int type, int serial, int flags, byte[] data, int size,
            long pts, long dts, long pos, long duration) {
        if (mFfmpegUseMediaCodecDecode != null) {
            switch (type) {
                case FfmpegUseMediaCodecDecode.TYPE_AUDIO:
                    if (mFfmpegUseMediaCodecDecode.mAudioWrapper != null) {
                        mFfmpegUseMediaCodecDecode.mAudioWrapper.serial = serial;
                        mFfmpegUseMediaCodecDecode.mAudioWrapper.flags = flags;
                        mFfmpegUseMediaCodecDecode.mAudioWrapper.data = data;
                        mFfmpegUseMediaCodecDecode.mAudioWrapper.size = size;
                        mFfmpegUseMediaCodecDecode.mAudioWrapper.pts = pts;
                        mFfmpegUseMediaCodecDecode.mAudioWrapper.dts = dts;
                        mFfmpegUseMediaCodecDecode.mAudioWrapper.pos = pos;
                        mFfmpegUseMediaCodecDecode.mAudioWrapper.duration = duration;
                        return mFfmpegUseMediaCodecDecode.feedInputBufferAndDrainOutputBuffer(
                                mFfmpegUseMediaCodecDecode.mAudioWrapper);
                    }
                    break;
                case FfmpegUseMediaCodecDecode.TYPE_VIDEO:
                    if (mFfmpegUseMediaCodecDecode.mVideoWrapper != null) {
                        mFfmpegUseMediaCodecDecode.mVideoWrapper.serial = serial;
                        mFfmpegUseMediaCodecDecode.mVideoWrapper.flags = flags;
                        mFfmpegUseMediaCodecDecode.mVideoWrapper.data = data;
                        mFfmpegUseMediaCodecDecode.mVideoWrapper.size = size;
                        mFfmpegUseMediaCodecDecode.mVideoWrapper.pts = pts;
                        mFfmpegUseMediaCodecDecode.mVideoWrapper.dts = dts;
                        mFfmpegUseMediaCodecDecode.mVideoWrapper.pos = pos;
                        mFfmpegUseMediaCodecDecode.mVideoWrapper.duration = duration;
                        boolean ret =
                                mFfmpegUseMediaCodecDecode.feedInputBufferAndDrainOutputBuffer(
                                        mFfmpegUseMediaCodecDecode.mVideoWrapper);
                        mFfmpegUseMediaCodecDecode.mVideoWrapper.data = null;
                        return ret;
                    }
                    break;
                default:
                    break;

            }
        }
        return false;
    }

    // 供jni层调用(不要改动方法名称,如改动了,jni层也要改动)
    private void createAudioTrack(int sampleRateInHz,
                                  int channelCount,
                                  int audioFormat) {
        // sampleRateInHz: 44100 channelCount: 2 audioFormat: 2
        Log.i(TAG, "createAudioTrack()" +
                " sampleRateInHz: " + sampleRateInHz +
                " channelCount: " + channelCount +
                " audioFormat: " + audioFormat);

        MediaUtils.releaseAudioTrack(mAudioTrack);
        mAudioTrack = null;
        Log.i(TAG, "createAudioTrack() start");

        // AudioTrack: releaseBuffer() track 0xe55f0a00
        // disabled due to previous underrun,

        mAudioTrack = MediaUtils.createAudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRateInHz, channelCount, audioFormat,
                AudioTrack.MODE_STREAM);
        //AudioTrack.MODE_STREAM, mIsLocalPlayer);

        if (mAudioTrack != null) {
            if (mContext != null) {
                /*AudioManager mAudioManager =
                        (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                AudioDeviceInfo[] devices =
                        mAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
                for (AudioDeviceInfo info : devices) {
                    Log.i(TAG, "---------------start");
                    Log.i(TAG, "Device productName: " + info.getProductName());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        Log.i(TAG, "Device     address: " + info.getAddress());
                    }
                    int type = info.getType();
                    Log.i(TAG, "Device        type: " + type);
                    switch (type) {
                        case TYPE_BUILTIN_SPEAKER:
                            if (mIsLocalPlayer) {
                                Log.i(TAG, "TYPE_BUILTIN_SPEAKER");
                                mAudioTrack.setPreferredDevice(info);
                            }
                            break;
                        case TYPE_WIRED_HEADPHONES:// 耳机
                            //audioDeviceInfo = info;
                            break;
                        case TYPE_USB_HEADSET:
                            if (!mIsLocalPlayer) {
                                Log.i(TAG, "TYPE_USB_HEADSET");
                                mAudioTrack.setPreferredDevice(info);
                            }
                            break;
                        default:
                            break;
                    }
                    Log.i(TAG, "---------------end");
                }*/

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
                    setVolume(VOLUME_NORMAL);
                } else {
                    setVolume(VOLUME_MUTE);
                }
            } else {
                setVolume(VOLUME_NORMAL);
            }

            mAudioTrack.play();
            Log.i(TAG, "createAudioTrack() end");

            return;
        }

        Log.e(TAG, "createAudioTrack() mAudioTrack is null");
    }

    /***
     No pending exception expected:
     java.lang.IllegalStateException:
     Unable to retrieve AudioTrack pointer for write()
     JNI DETECTED ERROR IN APPLICATION: JNI CallVoidMethodV called with pending exception
     java.lang.IllegalStateException:
     Unable to retrieve AudioTrack pointer for write()
     */
    // 供jni层调用
    private void write(byte[] audioData, int offsetInBytes, int sizeInBytes) {
        //Log.i(TAG, "audioData.length: " + audioData.length);
        /*for (int i = 0; i < audioData.length; i++) {
            Log.i(TAG, "" + audioData[i]);
        }
        Log.i(TAG, "write()" +
                " offsetInBytes: " + offsetInBytes +
                " sizeInBytes: " + sizeInBytes);*/
        if (mAudioTrack != null
                && audioData != null
                && sizeInBytes > 0) {
            mAudioTrack.write(audioData, offsetInBytes, sizeInBytes);
        }
    }

    // 供jni层调用
    private void sleep(long ms) {
        //SystemClock.sleep(ms);
        if (mFfmpegUseMediaCodecDecode != null) {
            if (ms == 0) {
                mFfmpegUseMediaCodecDecode.releaseOutputBuffer(true);
            } else {
                mFfmpegUseMediaCodecDecode.releaseOutputBuffer(false);
            }
        }
    }

    // 供native层调用,主要是使用Handler的延时功能
    private void postDelayed(final int what, final long delayMillis) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            Phone.removeUiMessages(DO_SOMETHING_CODE_postDelayed);
        } else {
            Phone.removeThreadMessages(DO_SOMETHING_CODE_postDelayed);
        }
        if (what == DO_SOMETHING_CODE_replay && delayMillis < 0) {
            return;
        }
        Phone.callDelayed(
                FFMPEG.class.getName(),
                DO_SOMETHING_CODE_postDelayed,
                delayMillis,
                new Object[]{what});
    }

    private Object onEvent(int what, Object[] objArray) {
        Object result = null;
        switch (what) {
            case DO_SOMETHING_CODE_postDelayed: {
                if (objArray != null && objArray.length > 0) {
                    int native_what = (int) objArray[0];
                    if (native_what != DO_SOMETHING_CODE_replay) {
                        JniObject jniObject = JniObject.obtain();
                        jniObject.valueInt = native_what;
                        onTransact(DO_SOMETHING_CODE_postDelayed, jniObject);
                        objArray = null;
                        objArray[0] = null;
                        jniObject = null;
                        break;
                    }

                    // replay
                    Log.w(TAG, "onEvent() 网络是不是不好啊!");
                    String allowReplay = onTransact(DO_SOMETHING_CODE_replay, null);
                    if (!TextUtils.isEmpty(allowReplay) && Boolean.parseBoolean(allowReplay)) {
                        Log.e(TAG, "onEvent() 重新播放!");
                        Phone.callUi(new Runnable() {
                            @Override
                            public void run() {
                                MyToast.show("网络差,重新播放!");
                                Phone.call(PlayerWrapper.class.getName(),
                                        DO_SOMETHING_EVENT_REPLAY,
                                        null);
                            }
                        });
                    }
                }
                break;
            }
            default:
                break;
        }
        return result;
    }

    private Context mContext;
    private Handler mUiHandler;
    private Surface mSurface;
    private String mPath;
    private String mType;
    private boolean mIsInit = false;
    private boolean mIsVideo = false;
    private boolean mIsAudio = false;
    private boolean mIsSeparatedAudioVideo = false;
    public boolean mIsLocalPlayer = true;

    // 在结束时调用,不然调用时机不对,会产生IllegalStateException异常的
    public void releaseAudioTrack() {
        MediaUtils.releaseAudioTrack(mAudioTrack);
        mAudioTrack = null;
    }

    @Override
    public void setContext(Context context) {
        mContext = context;
    }

    @Override
    public void setHandler(Handler handler) {
        mUiHandler = handler;
    }

    @Override
    public void setCallback(Callback callback) {
        // do nothing
    }

    @Override
    public void setSurface(Surface surface) {
        mSurface = surface;
    }

    @Override
    public void setDataSource(String path) {
        mPath = path;
    }

    public void setType(String type) {
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

    @Override
    public void setVolume(float volume) {
        if (mAudioTrack == null) {
            return;
        }
        if (volume < 0 || volume > 1.0f) {
            volume = VOLUME_NORMAL;
        }
        if (Build.VERSION.SDK_INT >= 21) {
            mAudioTrack.setVolume(volume);
        } else {
            mAudioTrack.setStereoVolume(volume, volume);
        }
    }

    @Override
    public void seekTo(long second) {
        onTransact(DO_SOMETHING_CODE_seekTo, JniObject.obtain().writeLong(second));
        if (TextUtils.equals(whatPlayer, PLAYER_FFPLAY)) {
        } else if (TextUtils.equals(whatPlayer, PLAYER_FFMPEG_MEDIACODEC)) {
            if (mFfmpegUseMediaCodecDecode != null) {
                mFfmpegUseMediaCodecDecode.clearQueue();
                mFfmpegUseMediaCodecDecode.signalQueue();
            }
        }
    }

    @Override
    public boolean prepareSync() {
        if (TextUtils.isEmpty(mPath) || mSurface == null) {
            Log.e(TAG, "prepareSync() TextUtils.isEmpty(mPath) || mSurface == null");
            return false;
        }

        if (mFfmpegUseMediaCodecDecode != null) {
            mFfmpegUseMediaCodecDecode.mType = mType;
            mFfmpegUseMediaCodecDecode.setSurface(mSurface);
            if (TextUtils.equals(whatPlayer, PLAYER_FFPLAY)) {
                mFfmpegUseMediaCodecDecode.useFFplay = true;
            } else if (TextUtils.equals(whatPlayer, PLAYER_FFMPEG_MEDIACODEC)) {
                mFfmpegUseMediaCodecDecode.useFFplay = false;
            }
        }

        // region 判断是什么样的文件(一般用于本地文件)

        mIsSeparatedAudioVideo = false;
        String tempPath = "";
        if (mPath.endsWith(".m4s")) {
            tempPath = mPath.substring(0, mPath.lastIndexOf("/"));
            File audioFile = new File(tempPath + "/audio.m4s");
            File videoFile = new File(tempPath + "/video.m4s");
            Log.d(TAG,
                    "prepareSync()                  audio: " + audioFile.getAbsolutePath());
            Log.d(TAG,
                    "prepareSync()                  video: " + videoFile.getAbsolutePath());
            if (audioFile.exists() && videoFile.exists()) {
                mIsSeparatedAudioVideo = true;
            }
        } else if (mPath.endsWith(".h264") || mPath.endsWith(".aac")) {
            tempPath = mPath.substring(0, mPath.lastIndexOf("/"));
            String fileName = mPath.substring(
                    mPath.lastIndexOf("/") + 1, mPath.lastIndexOf("."));
            Log.d(TAG, "prepareSync()               fileName: " + fileName);
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
                    "prepareSync()                  audio: " + audioFile.getAbsolutePath());
            Log.d(TAG,
                    "prepareSync()                  video: " + videoFile.getAbsolutePath());
            if (audioFile.exists() && videoFile.exists()) {
                mIsSeparatedAudioVideo = true;
            }
        }
        Log.d(TAG, "prepareSync()  isSeparatedAudioVideo: " + mIsSeparatedAudioVideo);

        // endregion

        // region setMode

        JniObject jniObject = JniObject.obtain();
        if (mIsSeparatedAudioVideo) {
            // [.m4s] or [.h264 and .aac](达不到同步效果)
            if (mPath.endsWith(".m4s")) {
                jniObject.writeInt(USE_MODE_AUDIO_VIDEO);
            } else {
                jniObject.writeInt(USE_MODE_AAC_H264);
            }
        } else {
            /*if (mIsVideo) {
            } else if (mIsAudio) {
                jniObject.writeInt(USE_MODE_ONLY_AUDIO);
            }*/
            if (mPath.endsWith(".h264")) {
                if (TextUtils.equals(whatPlayer, PLAYER_FFPLAY)) {
                    jniObject.writeInt(USE_MODE_MEDIA_FFPLAY);
                } else if (TextUtils.equals(whatPlayer, PLAYER_FFMPEG_MEDIACODEC)) {
                    jniObject.writeInt(USE_MODE_ONLY_VIDEO);
                }
            } else {
                jniObject.writeBoolean(PlayerWrapper.IS_WATCH ? true : false);
                onTransact(DO_SOMETHING_CODE_isWatch, jniObject);
                if (TextUtils.equals(whatPlayer, PLAYER_FFPLAY)) {
                    jniObject.writeInt(USE_MODE_MEDIA_FFPLAY);
                } else if (TextUtils.equals(whatPlayer, PLAYER_FFMPEG_MEDIACODEC)) {
                    jniObject.writeInt(USE_MODE_MEDIA_MEDIACODEC);
                }
            }
        }
        onTransact(DO_SOMETHING_CODE_setMode, jniObject);

        // endregion

        if (PlayerWrapper.IS_PHONE) {
            SharedPreferences sp;
            if (mIsLocalPlayer) {
                sp = mContext.getSharedPreferences(
                        PREFERENCES_NAME, Context.MODE_PRIVATE);
            } else {
                sp = mContext.getSharedPreferences(
                        PREFERENCES_NAME_REMOTE, Context.MODE_PRIVATE);
            }
            EDMediaCodec.TIME_OUT = sp.getInt(MEDIACODEC_TIME_OUT, 10000);
        }
        // MyToast.show(String.valueOf(EDMediaCodec.TIME_OUT));
        Log.d(TAG, "prepareSync() time_out: " + EDMediaCodec.TIME_OUT);

        onTransact(DO_SOMETHING_CODE_setSurface,
                JniObject.obtain()
                        .writeString(mPath)
                        .writeObject(mSurface));

        if (Integer.parseInt(onTransact(DO_SOMETHING_CODE_initPlayer, null)) != 0) {
            return false;
        }

        return true;
    }

    @Override
    public boolean prepareAsync() {
        return true;
    }

    @Override
    public void start() {
        if (TextUtils.equals(whatPlayer, PLAYER_FFPLAY) && !mIsSeparatedAudioVideo) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    onTransact(DO_SOMETHING_CODE_readData, null);
                    return null;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else if (TextUtils.equals(whatPlayer, PLAYER_FFMPEG_MEDIACODEC)) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    onTransact(DO_SOMETHING_CODE_audioHandleData, null);
                    return null;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    onTransact(DO_SOMETHING_CODE_videoHandleData, null);
                    return null;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    SystemClock.sleep(500);
                    onTransact(DO_SOMETHING_CODE_readData, null);
                    return null;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            if (mIsSeparatedAudioVideo) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        SystemClock.sleep(1000);
                        onTransact(DO_SOMETHING_CODE_readData, null);
                        return null;
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }

        /*ThreadPool.getFixedThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                onTransact(DO_SOMETHING_CODE_audioHandleData, null);
            }
        });
        ThreadPool.getFixedThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                onTransact(DO_SOMETHING_CODE_videoHandleData, null);
            }
        });
        ThreadPool.getFixedThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                SystemClock.sleep(500);
                onTransact(DO_SOMETHING_CODE_readData, null);
            }
        });
        if (mIsSeparatedAudioVideo) {
            ThreadPool.getFixedThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    SystemClock.sleep(1000);
                    onTransact(DO_SOMETHING_CODE_readData, null);
                }
            });
        }*/
    }

    @Override
    public void play() {
        onTransact(DO_SOMETHING_CODE_play, null);
    }

    @Override
    public void pause() {
        onTransact(DO_SOMETHING_CODE_pause, null);
    }

    @Override
    public void release() {
        if (mFfmpegUseMediaCodecDecode != null) {
            mFfmpegUseMediaCodecDecode.destroy();
        }
        onTransact(DO_SOMETHING_CODE_release, null);
    }

    @Override
    public boolean isRunning() {
        return Boolean.parseBoolean(onTransact(DO_SOMETHING_CODE_isRunning, null));
    }

    @Override
    public boolean isPlaying() {
        return Boolean.parseBoolean(onTransact(DO_SOMETHING_CODE_isPlaying, null));
    }

    @Override
    public long getDuration() {
        return Long.parseLong(onTransact(DO_SOMETHING_CODE_getDuration, null));
    }

    // 供jni层调用(底层信息才是通过这个接口反映到java层的)
    public Callback mCallback = new Callback() {

        @Override
        public int onTransact(int code, JniObject jniObject) {
            //Log.i(TAG, "onTransact() code: " + code + " " + jniObject.toString());
            if (mUiHandler != null) {
                Message msg = mUiHandler.obtainMessage();
                msg.what = code;
                msg.obj = jniObject;
                mUiHandler.removeMessages(code);
                mUiHandler.sendMessage(msg);
            }
            return 0;
        }

        @Override
        public void onReady() {
            Log.i(TAG, "FFMPEG onReady()");
            if (mUiHandler != null) {
                mUiHandler.removeMessages(Callback.MSG_ON_TRANSACT_READY);
                mUiHandler.sendEmptyMessage(Callback.MSG_ON_TRANSACT_READY);
            }
        }

        @Override
        public void onChangeWindow(int width, int height) {
            Log.i(TAG, "FFMPEG onChangeWindow() width: " + width + " height: " + height);
            if (mUiHandler != null) {
                Message msg = mUiHandler.obtainMessage();
                msg.what = Callback.MSG_ON_TRANSACT_CHANGE_WINDOW;
                msg.arg1 = width;
                msg.arg2 = height;
                mUiHandler.removeMessages(Callback.MSG_ON_TRANSACT_CHANGE_WINDOW);
                mUiHandler.sendMessage(msg);
            }
        }

        @Override
        public void onPlayed() {
            Log.i(TAG, "FFMPEG onPlayed()");
            if (mUiHandler != null) {
                mUiHandler.removeMessages(Callback.MSG_ON_TRANSACT_PLAYED);
                mUiHandler.sendEmptyMessage(Callback.MSG_ON_TRANSACT_PLAYED);
            }
        }

        @Override
        public void onPaused() {
            Log.i(TAG, "FFMPEG onPaused()");
            if (mUiHandler != null) {
                mUiHandler.removeMessages(Callback.MSG_ON_TRANSACT_PAUSED);
                mUiHandler.sendEmptyMessage(Callback.MSG_ON_TRANSACT_PAUSED);
            }
        }

        @Override
        public void onFinished() {
            Log.i(TAG, "FFMPEG onFinished()");
            if (mUiHandler != null) {
                mUiHandler.removeMessages(Callback.MSG_ON_TRANSACT_FINISHED);
                mUiHandler.sendEmptyMessage(Callback.MSG_ON_TRANSACT_FINISHED);
            }
        }

        @Override
        public void onProgressUpdated(long presentationTime) {
            // 视频时长小于0时(如直播节目),不回调
            if (mUiHandler != null) {
                Message msg = mUiHandler.obtainMessage();
                msg.what = Callback.MSG_ON_TRANSACT_PROGRESS_UPDATED;
                msg.obj = presentationTime;
                mUiHandler.removeMessages(Callback.MSG_ON_TRANSACT_PROGRESS_UPDATED);
                mUiHandler.sendMessage(msg);
            }
        }

        @Override
        public void onError(int error, String errorInfo) {
            Log.e(TAG, "FFMPEG onError() error: " + error + " errorInfo: " + errorInfo);
            if (mUiHandler != null) {
                Message msg = mUiHandler.obtainMessage();
                msg.what = Callback.MSG_ON_TRANSACT_ERROR;
                msg.arg1 = error;
                msg.obj = errorInfo;
                mUiHandler.removeMessages(Callback.MSG_ON_TRANSACT_ERROR);
                mUiHandler.sendMessage(msg);
            }
        }

        @Override
        public void onInfo(String info) {
            Log.i(TAG, "FFMPEG onInfo() info:\n" + info);
            if (mUiHandler != null) {
                Message msg = mUiHandler.obtainMessage();
                msg.what = Callback.MSG_ON_TRANSACT_INFO;
                msg.obj = info;
                mUiHandler.removeMessages(Callback.MSG_ON_TRANSACT_INFO);
                mUiHandler.sendMessage(msg);
            }
        }
    };

}

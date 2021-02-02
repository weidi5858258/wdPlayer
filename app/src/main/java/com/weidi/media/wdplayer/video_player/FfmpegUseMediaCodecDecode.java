package com.weidi.media.wdplayer.video_player;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import com.weidi.media.wdplayer.exo.ExoAudioTrack;
import com.weidi.media.wdplayer.util.AVPacket;
import com.weidi.media.wdplayer.util.ArrayBlockingQueue;
import com.weidi.media.wdplayer.util.EDMediaCodec;
import com.weidi.media.wdplayer.util.JniObject;
import com.weidi.media.wdplayer.util.MediaUtils;
import com.weidi.utils.MyToast;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;

import androidx.annotation.NonNull;

import static com.weidi.media.wdplayer.Constants.HARD_SOLUTION;
import static com.weidi.media.wdplayer.Constants.HARD_SOLUTION_AUDIO;
import static com.weidi.media.wdplayer.Constants.PREFERENCES_NAME;
import static com.weidi.media.wdplayer.Constants.PREFERENCES_NAME_REMOTE;

/***
 Created by weidi on 2020/07/11.


 */

public class FfmpegUseMediaCodecDecode {

    private static final String TAG =
            "player_alexander";

    private static final int AUDIO_FRAME_MAX_LENGTH = 1024 * 100;// 102400
    private static final int VIDEO_FRAME_MAX_LENGTH = 1024 * 1024 * 5;// 5242880

    // 不要修改值,如果要修改,MediaPlayerForMediaCodec.cpp中也要修改
    public static final int TYPE_AUDIO = 0x0001;
    public static final int TYPE_VIDEO = 0x0002;

    // 为了注册广播
    private Context mContext = null;
    private Surface mSurface = null;
    private Handler mVideoThreadHandler;
    private Handler mAudioThreadHandler;

    public boolean mIsLocalPlayer = true;
    public boolean mUseMediaCodecForVideo = true;
    public boolean mUseMediaCodecForAudio = true;
    public AudioWrapper mAudioWrapper = null;
    public VideoWrapper mVideoWrapper = null;
    private ExoAudioTrack mExoAudioTrack = null;
    private FFMPEG mFFMPEG = null;
    public String mType = null;

    public static class SimpleWrapper {
        public String mime = null;
        public MediaCodec decoderMediaCodec = null;
        public MediaFormat decoderMediaFormat = null;
        // 是否需要渲染图像(播放音频为false,播放视频为true)
        public boolean render = false;
        public int trackIndex = -1;
        // 使用于while条件判断
        public boolean isHandling = false;

        // 用于标识音频还是视频
        public int type;
        // 总时长
        public long durationUs = 0;
        // 播放的时长(下面两个参数一起做的事是每隔一秒发一次回调函数)
        public long sampleTime = 0;
        public long startTimeUs = 0;

        // 使用于时间戳
        public long presentationTimeUs = 0;

        public boolean isStarted = false;
        // 因为user所以pause
        public boolean isPausedForUser = false;
        // 因为cache所以pause
        public boolean isPausedForCache = false;
        // 因为seek所以pause
        public boolean isPausedForSeek = false;

        // 一帧音频或者视频的最大值
        public int frameMaxLength = 0;
        // 音频或者视频一帧的实际大小
        public int size = 0;
        // 放一帧音频或者视频的容器
        public byte[] data = null;

        private SimpleWrapper() {
        }

        public SimpleWrapper(int type) {
            switch (type) {
                case TYPE_AUDIO:
                    this.type = TYPE_AUDIO;
                    frameMaxLength = AUDIO_FRAME_MAX_LENGTH;
                    break;
                case TYPE_VIDEO:
                    this.type = TYPE_VIDEO;
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
            isStarted = false;
            isHandling = false;
            isPausedForUser = false;
            isPausedForCache = false;
            isPausedForSeek = false;
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

    public static class AudioWrapper extends SimpleWrapper {
        //public AudioTrack mAudioTrack = null;

        private AudioWrapper() {
        }

        public AudioWrapper(int type) {
            super(type);
        }

        public void clear() {
            //MediaUtils.releaseAudioTrack(mAudioTrack);
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

    public FfmpegUseMediaCodecDecode() {
        mFFMPEG = FFMPEG.getDefault();
        HandlerThread videoHandlerThread = new HandlerThread(TAG);
        videoHandlerThread.start();
        mVideoThreadHandler = new Handler(videoHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
            }
        };
        HandlerThread audioHandlerThread = new HandlerThread(TAG);
        audioHandlerThread.start();
        mAudioThreadHandler = new Handler(audioHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
            }
        };
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public void setSurface(Surface surface) {
        mSurface = surface;
    }

    public void setVolume(float volume) {
        if (mExoAudioTrack == null) {
            return;
        }

        mExoAudioTrack.setVolume(volume);
    }

    public void releaseMediaCodec() {
        Log.i(TAG, "releaseMediaCodec() start");
        mType = null;
        /*if (mVideoWrapper != null && mVideoWrapper.decoderMediaCodec != null) {
            mVideoWrapper.decoderMediaCodec.release();
            mVideoWrapper.decoderMediaCodec = null;
            mVideoWrapper = null;
        }
        Log.i(TAG, "releaseMediaCodec() 1");
        if (mExoAudioTrack != null) {
            MediaUtils.releaseAudioTrack(mExoAudioTrack.mAudioTrack);
            mExoAudioTrack.mAudioTrack = null;
            mExoAudioTrack = null;
        }
        Log.i(TAG, "releaseMediaCodec() 2");
        if (mAudioWrapper != null && mAudioWrapper.decoderMediaCodec != null) {
            mAudioWrapper.decoderMediaCodec.release();
            mAudioWrapper.decoderMediaCodec = null;
            mAudioWrapper = null;
        }*/
        if (mVideoWrapper != null) {
            //mVideoWrapper.decoderMediaCodec.flush();
            MediaUtils.releaseMediaCodec(mVideoWrapper.decoderMediaCodec);
            mVideoWrapper.decoderMediaCodec = null;
            mVideoWrapper = null;
        }
        if (mAudioWrapper != null) {
            //mAudioWrapper.decoderMediaCodec.flush();
            MediaUtils.releaseMediaCodec(mAudioWrapper.decoderMediaCodec);
            mAudioWrapper.decoderMediaCodec = null;
            mAudioWrapper = null;
        }
        if (mExoAudioTrack != null) {
            MediaUtils.releaseAudioTrack(mExoAudioTrack.mAudioTrack);
            mExoAudioTrack.mAudioTrack = null;
            mExoAudioTrack = null;
        }
        clearQueue();
        signalQueue();
        Log.i(TAG, "releaseMediaCodec() end");
    }

    public void destroy() {
        Log.i(TAG, "release() start");
        //notifyVideoEndOfStream();
        //notifyAudioEndOfStream();
        if (mVideoWrapper != null) {
            mVideoWrapper.isHandling = false;
            mVideoWrapper.isPausedForUser = false;
            mVideoWrapper.isPausedForCache = false;
            mVideoWrapper.isPausedForSeek = false;
        }

        if (mAudioWrapper != null) {
            mAudioWrapper.isHandling = false;
            mAudioWrapper.isPausedForUser = false;
            mAudioWrapper.isPausedForCache = false;
            mAudioWrapper.isPausedForSeek = false;
        }

        clearQueue();
        signalQueue();
        Log.i(TAG, "release() end");
    }

    public void clearQueue() {
        try {
            int size = mVideoInputDatasQueue.size();
            for (int i = 0; i < size; i++) {
                AVPacket avPacket = mVideoInputDatasQueue.poll();
                avPacket = null;
            }
            size = mAudioInputDatasQueue.size();
            for (int i = 0; i < size; i++) {
                AVPacket avPacket = mAudioInputDatasQueue.poll();
                avPacket = null;
            }
            mVideoInputDatasQueue.clear();
            mAudioInputDatasQueue.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void signalQueue() {
        mVideoInputDatasQueue.signalTake();
        mVideoInputDatasQueue.signalPut();
        mAudioInputDatasQueue.signalTake();
        mAudioInputDatasQueue.signalPut();
    }

    // video
    private static final int AV_CODEC_ID_HEVC = 173;
    private static final int AV_CODEC_ID_H264 = 27;
    private static final int AV_CODEC_ID_MPEG4 = 12;
    private static final int AV_CODEC_ID_VP8 = 139;
    private static final int AV_CODEC_ID_VP9 = 167;
    private static final int AV_CODEC_ID_MPEG2VIDEO = 2;
    private static final int AV_CODEC_ID_H263 = 4;
    // 上面除AV_CODEC_ID_H263外,都已验证过
    private static final int AV_CODEC_ID_MPEG1VIDEO = 1;
    private static final int AV_CODEC_ID_MJPEG = 7;
    private static final int AV_CODEC_ID_RV40 = 69;
    private static final int AV_CODEC_ID_VC1 = 70;
    private static final int AV_CODEC_ID_WMV3 = 71;
    private static final int AV_CODEC_ID_VP6 = 91;
    private static final int AV_CODEC_ID_VP6F = 92;

    // audio
    private static final int AV_CODEC_ID_PCM_S16LE = 65536;
    private static final int AV_CODEC_ID_PCM_S16BE = 65537;
    private static final int AV_CODEC_ID_MP2 = 86016;
    private static final int AV_CODEC_ID_MP3 = 86017;
    private static final int AV_CODEC_ID_AAC = 86018;
    private static final int AV_CODEC_ID_AC3 = 86019;
    private static final int AV_CODEC_ID_VORBIS = 86021;
    private static final int AV_CODEC_ID_WMAV2 = 86024;
    private static final int AV_CODEC_ID_FLAC = 86028;
    private static final int AV_CODEC_ID_QCELP = 86040;
    private static final int AV_CODEC_ID_EAC3 = 86056;
    private static final int AV_CODEC_ID_AAC_LATM = 86065;
    private static final int AV_CODEC_ID_OPUS = 86076;

    /***
     video
     只有csd-0
     MediaFormat.MIMETYPE_VIDEO_HEVC
     video/hevc
     {track-id=1, level=65536, mime=video/hevc, profile=1, language=und, display-width=3840,
     durationUs=20000400, display-height=2160, width=3840, max-input-size=1086672, frame-rate=60,
     height=2160, csd-0=java.nio.HeapByteBuffer[pos=0 lim=88 cap=88]}
     csd-0: {0, 0, 0, 1, 64, 1, 12, 1, -1, -1, 1, 96, 0, 0, 3, 0, -112, 0, 0, 3, 0, 0, 3, 0,
     -103, -107, -104, 9, 0, 0, 0, 1, 66, 1, 1, 1, 96, 0, 0, 3, 0, -112, 0, 0, 3, 0, 0, 3, 0,
     -103, -96, 1, -32, 32, 2, 28, 89, 101, 102, -110, 76, -82, 106, 4, 36, 4, 8, 0, 0, 31, 64,
     0, 7, 83, 0, 64, 0, 0, 0, 1, 68, 1, -63, 114, -76, 98, 64, 0}

     MediaFormat.MIMETYPE_VIDEO_MPEG4
     video/mp4v-es
     {max-bitrate=800000, track-id=1, level=4, mime=video/mp4v-es, profile=1, language=und,
     display-width=640, durationUs=69200000, display-height=480, width=640, max-input-size=25046,
     frame-rate=25, height=480, csd-0=java.nio.HeapByteBuffer[pos=0 lim=45 cap=45]}
     csd-0: {0, 0, 1, -80, 1, 0, 0, 1, -75, -119, 19, 0, 0, 1, 0, 0, 0, 1, 32, 0, -60, -115,
     -120, 0, -51, 20, 4, 60, 20, 99, 0, 0, 1, -78, 76, 97, 118, 99, 53, 50, 46, 49, 53, 46, 48}

     MediaFormat.MIMETYPE_VIDEO_MPEG2
     video/mpeg2
     {track-id=1, mime=video/mpeg2, width=1920, height=1080, csd-0=java.nio.HeapByteBuffer[pos=0
     lim=22 cap=22]}
     csd-0: {0, 0, 1, -77, 120, 4, 56, 53, -1, -1, -32, 24, 0, 0, 1, -75, 20, 74, 0, 1, 0, 0}

     有csd-0, csd-1
     MediaFormat.MIMETYPE_VIDEO_AVC
     video/avc
     {track-id=1, level=2048, mime=video/avc, profile=8, language=und, display-width=1920,
     csd-1=java.nio.HeapByteBuffer[pos=0 lim=8 cap=8], durationUs=585966666, display-height=1080,
     width=1920, max-input-size=173638, frame-rate=30, height=1080, csd-0=java.nio
     .HeapByteBuffer[pos=0 lim=34 cap=34]}
     csd-0: {0, 0, 0, 1, 103, 100, 0, 40, -84, -47, 0, 120, 2, 39, -27, -64, 90, -128, -128,
     -125, 32, 0, 0, 3, 0, 32, 0, 0, 7, -127, -29, 6, 34, 64}
     csd-1: {0, 0, 0, 1, 104, -21, -113, 44}

     没有csd-0, csd-1
     MediaFormat.MIMETYPE_VIDEO_VP8
     video/x-vnd.on2.vp8
     {track-id=1, durationUs=52069000, display-height=1080, mime=video/x-vnd.on2.vp8, width=1920,
     frame-rate=30, height=1080, display-width=1920}

     MediaFormat.MIMETYPE_VIDEO_VP9
     video/x-vnd.on2.vp9
     {color-transfer=3, track-id=1, durationUs=328983000, display-height=2160, mime=video/x-vnd
     .on2.vp9, width=3840, color-range=2, color-standard=1, frame-rate=24, height=2160,
     display-width=3840}

     audio
     MediaFormat.MIMETYPE_AUDIO_MPEG
     audio/mpeg
     {encoder-delay=576, sample-rate=44100, pcm-encoding=2, track-id=1, durationUs=207856326,
     mime=audio/raw, channel-count=2, bitrate=320000, mime-old=audio/mpeg, encoder-padding=698,
     max-input-size=102400}

     MediaFormat.MIMETYPE_AUDIO_RAW
     MediaFormat.MIMETYPE_AUDIO_FLAC
     audio/raw
     {sample-rate=96000, pcm-encoding=2, track-id=1, durationUs=170533333, mime=audio/raw,
     channel-count=2, mime-old=audio/raw, max-input-size=102400}

     MediaFormat.MIMETYPE_AUDIO_EAC3
     audio/eac3
     {sample-rate=48000, track-id=2, durationUs=38112000, mime=audio/eac3, channel-count=2,
     language=eng, max-input-size=788}

     MediaFormat.MIMETYPE_AUDIO_AAC
     audio/mp4a-latm
     {sample-rate=44100, pcm-encoding=2, track-id=1, durationUs=5534069040, mime=audio/raw,
     profile=2, channel-count=2, mime-old=audio/mp4a-latm, max-input-size=102400,
     csd-0=java.nio.HeapByteBuffer[pos=2 lim=2 cap=2]}
     {max-bitrate=82376, sample-rate=16000, track-id=1, mime=audio/raw, profile=2, language=und,
     aac-profile=2, bitrate=64000, encoder-delay=2112, pcm-encoding=2, durationUs=788096000,
     channel-count=2, mime-old=audio/mp4a-latm, encoder-padding=491, max-input-size=1257,
     csd-0=java.nio.HeapByteBuffer[pos=2 lim=2 cap=2]}
     csd-0: {18, 16}
     csd-0: {19, 16, 86, -27, -99, 72, 0}
     csd-0: {20, 16}
     csd-0: {17, -112}
     csd-0: {17, -112, 86, -27, 0}

     MediaFormat.MIMETYPE_AUDIO_VORBIS
     audio/vorbis
     {csd-1=java.nio.HeapByteBuffer[pos=3861 lim=3861 cap=3861], sample-rate=48000, pcm-encoding=2,
     track-id=1, durationUs=1070812, mime=audio/raw, channel-count=2, mime-old=audio/vorbis,
     max-input-size=102400, csd-0=java.nio.HeapByteBuffer[pos=30 lim=30 cap=30]}
     csd-0: {...}
     csd-1: {...}

     MediaFormat.MIMETYPE_AUDIO_AC3
     audio/ac3
     {sample-rate=48000, pcm-encoding=2, track-id=2, durationUs=128160000, mime=audio/raw,
     channel-count=2, language=und, mime-old=audio/ac3, max-input-size=1812}
     csd-0: {0, 0, 0, 1, 103, 100, 0, 41, -84, 27, 26, 80, 30, 1, 19, -9, -128, -75, 1, 1, 1, 64,
     0, 0, -6, 64, 0, 58, -104, 56, -104, 0, 1, 48, -33, 0, 0, 28, -100, 62, 49, 46, 49, 48, 0, 2,
     97, -66, 0, 0, 57, 56, 124, 98, 92, 62, 56, 97, 75}
     csd-1: {0, 0, 0, 1, 104, -6, -116, -14, 60}
     {sample-rate=48000, pcm-encoding=2, track-id=2, durationUs=5617728000, mime=audio/raw,
     channel-count=2, language=new, mime-old=audio/ac3, max-input-size=1556}

     audio/raw
     OMX.google.raw.decoder
     c2.android.raw.decoder
     audio/flac
     OMX.qti.audio.decoder.flac
     OMX.google.flac.decoder
     c2.android.flac.decoder
     audio/mpeg
     OMX.google.mp3.decoder
     c2.android.mp3.decoder
     audio/ac3
     OMX.MTK.AUDIO.DECODER.DSPAC3
     audio/eac3
     OMX.MTK.AUDIO.DECODER.DSPEAC3
     audio/ac4
     audio/mpeg-L2
     OMX.MTK.AUDIO.DECODER.DSPMP2

     audio/mp4a-latm
     OMX.google.aac.decoder
     c2.android.aac.decoder
     audio/x-ms-wma
     OMX.MTK.AUDIO.DECODER.DSPWMA

     audio/vorbis
     OMX.google.vorbis.decoder
     c2.android.vorbis.decoder
     */
    public boolean initAudioMediaCodec(JniObject jniObject) {
        Log.d(TAG, "initAudioMediaCodec() start");
        if (jniObject == null
                || jniObject.valueObjectArray == null
                || jniObject.valueObjectArray.length < 2) {
            Log.e(TAG, "initAudioMediaCodec() jniObject failure");
            return false;
        }

        if (!mUseMediaCodecForAudio) {
            return false;
        }

        if (mContext != null) {
            SharedPreferences sp;
            if (mIsLocalPlayer) {
                sp = mContext.getSharedPreferences(
                        PREFERENCES_NAME, Context.MODE_PRIVATE);
            } else {
                sp = mContext.getSharedPreferences(
                        PREFERENCES_NAME_REMOTE, Context.MODE_PRIVATE);
            }
            int softSolution = sp.getInt(HARD_SOLUTION, 1);
            int softSolutionForAudio = sp.getInt(HARD_SOLUTION_AUDIO, 1);
            if (softSolution == 0 || softSolutionForAudio == 0) {
                return false;
            }
        }

        if (mAudioWrapper != null
                && mAudioWrapper.decoderMediaCodec != null) {
            // mAudioWrapper.decoderMediaCodec.flush();
            Log.e(TAG, "initAudioMediaCodec() audio clear");
            MediaUtils.releaseMediaCodec(mAudioWrapper.decoderMediaCodec);
            mAudioWrapper.clear();
            mAudioWrapper.decoderMediaCodec = null;
            mAudioWrapper = null;
        }

        MediaFormat mediaFormat = null;
        String audioMime = null;
        String codecName = null;
        Log.d(TAG, "initAudioMediaCodec() audio       mimeType: " + jniObject.valueInt);
        switch (jniObject.valueInt) {
            // 65536 pcm_s16le ---> audio/raw
            case AV_CODEC_ID_PCM_S16LE:
                audioMime = MediaFormat.MIMETYPE_AUDIO_RAW;
                codecName = "OMX.google.raw.decoder";
                break;
            // 65537 pcm_s16be ---> audio/raw
            case AV_CODEC_ID_PCM_S16BE:
                audioMime = MediaFormat.MIMETYPE_AUDIO_RAW;
                codecName = "OMX.google.raw.decoder";
                break;
            // 86017 mp3 ---> audio/mpeg
            case AV_CODEC_ID_MP3:
                audioMime = MediaFormat.MIMETYPE_AUDIO_MPEG;
                codecName = "OMX.google.mp3.decoder";
                break;
            // 86018 aac ---> audio/mp4a-latm
            case AV_CODEC_ID_AAC:
                audioMime = MediaFormat.MIMETYPE_AUDIO_AAC;
                codecName = "OMX.google.aac.decoder";
                break;
            // 86019 ac3 ---> audio/ac3
            case AV_CODEC_ID_AC3:
                audioMime = MediaFormat.MIMETYPE_AUDIO_AC3;
                codecName = "OMX.google.raw.decoder";
                codecName = "OMX.dolby.ac3.decoder";
                break;
            // 86021 vorbis ---> audio/vorbis
            case AV_CODEC_ID_VORBIS:
                // 因为sps_pps还不知道怎么分离出来,所以先注释掉
                //audioMime = MediaFormat.MIMETYPE_AUDIO_VORBIS;
                //codecName = "OMX.google.vorbis.decoder";
                break;
            // 86028 flac ---> audio/raw
            case AV_CODEC_ID_FLAC:
                audioMime = MediaFormat.MIMETYPE_AUDIO_RAW;
                codecName = "OMX.google.raw.decoder";
                break;
            // 86056 eac3 ---> audio/eac3
            case AV_CODEC_ID_EAC3:
                audioMime = MediaFormat.MIMETYPE_AUDIO_EAC3;
                codecName = "OMX.google.raw.decoder";
                codecName = "OMX.dolby.eac3.decoder";
                break;
            // 86076
            case AV_CODEC_ID_OPUS:
                //audioMime = MediaFormat.MIMETYPE_AUDIO_OPUS;
                //codecName = "OMX.google.opus.decoder";
                break;

            // 下面几种在我的手机上不支持
            // 86016 mp2 ---> audio/mpeg-L2
            case AV_CODEC_ID_MP2:
                audioMime = "audio/mpeg-L2";
                //codecName = "OMX.MTK.AUDIO.DECODER.DSPMP2";
                break;
            // 86024 wmav2 ---> audio/x-ms-wma
            case AV_CODEC_ID_WMAV2:
                audioMime = "audio/x-ms-wma";
                //codecName = "OMX.MTK.AUDIO.DECODER.DSPWMA";
                break;
            // 86040
            case AV_CODEC_ID_QCELP:
                //audioMime = MediaFormat.MIMETYPE_AUDIO_QCELP;
                //codecName = "";
                break;
            // 86065
            case AV_CODEC_ID_AAC_LATM:
                //audioMime = "";
                //codecName = "";
                break;
            default:
                break;
        }

        Log.d(TAG, "initAudioMediaCodec() audio           mime: " + audioMime);
        if (TextUtils.isEmpty(audioMime)) {
            Log.e(TAG, "initAudioMediaCodec() audioMime is empty");
            // MyToast.show("audio mime is empty");
            return false;
        }

        // region

        Object[] valueObjectArray = jniObject.valueObjectArray;
        long[] parameters = (long[]) valueObjectArray[0];
        int sampleRateInHz = (int) parameters[0];
        int channelCount = (int) parameters[1];
        int audioFormat = (int) parameters[2];
        // 单位: 秒
        long duration = parameters[3];
        // 码率
        int bitrate = (int) parameters[4];

        mediaFormat = MediaFormat.createAudioFormat(audioMime, sampleRateInHz, channelCount);
        if (duration > 0) {
            mediaFormat.setLong(MediaFormat.KEY_DURATION, duration * 1000000L);
        } else {
            mediaFormat.setLong(MediaFormat.KEY_DURATION, duration);
        }
        if (bitrate > 0) {
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        }
        /*if (TextUtils.equals(audioMime, MediaFormat.MIMETYPE_AUDIO_AAC)) {
            mediaFormat.setInteger(android.media.MediaFormat.KEY_AAC_PROFILE, 2);
        }*/

        Object object = valueObjectArray[1];
        byte[] sps_pps = null;
        if (object != null) {
            sps_pps = (byte[]) object;
            Log.d(TAG, "initAudioMediaCodec() audio sps_pps.length: " + sps_pps.length +
                    " \nsps_pps: " + Arrays.toString(sps_pps));
        } else {
            Log.d(TAG, "initAudioMediaCodec() audio sps_pps is null");
        }
        if (TextUtils.equals(audioMime, MediaFormat.MIMETYPE_AUDIO_RAW)
                || TextUtils.equals(audioMime, MediaFormat.MIMETYPE_AUDIO_MPEG)
                || TextUtils.equals(audioMime, MediaFormat.MIMETYPE_AUDIO_AC3)
                || TextUtils.equals(audioMime, MediaFormat.MIMETYPE_AUDIO_EAC3)
                || TextUtils.equals(audioMime, MediaFormat.MIMETYPE_AUDIO_FLAC)
                || TextUtils.equals(audioMime, "audio/ac4")
                || TextUtils.equals(audioMime, "audio/mpeg-L2")) {
            // 没有csd-0和csd-1
        } else if (TextUtils.equals(audioMime, MediaFormat.MIMETYPE_AUDIO_AAC)
                || TextUtils.equals(audioMime, "audio/x-ms-wma")) {
            // 只有csd-0
            if (sps_pps != null) {
                mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(sps_pps));
            } else {
                // [17, -112]
                // [17, -112, 86, -27, 0]
                // [18, 16]
                // [18,   16, 86, -27, 0]
                sps_pps = new byte[5];
                sps_pps[0] = 17;
                sps_pps[1] = -112;
                sps_pps[2] = 86;
                sps_pps[3] = -27;
                sps_pps[4] = 0;
                mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(sps_pps));
            }
        } else if (TextUtils.equals(audioMime, MediaFormat.MIMETYPE_AUDIO_VORBIS)) {
            if (sps_pps != null) {

            }
        }

        // endregion

        Log.d(TAG, "initAudioMediaCodec() audio    mediaFormat: \n" + mediaFormat);

        mAudioWrapper = new AudioWrapper(TYPE_AUDIO);
        mAudioWrapper.isHandling = true;
        mAudioWrapper.render = false;
        mAudioWrapper.mime = audioMime;
        mAudioWrapper.decoderMediaFormat = mediaFormat;

        mExoAudioTrack = new ExoAudioTrack();
        mExoAudioTrack.mContext = mContext;
        mExoAudioTrack.mime = audioMime;
        mExoAudioTrack.mMediaFormat = mediaFormat;

        //////////////////////////////////////////////////////////////////////////////
        if (AUDIO_NEED_TO_ASYNC) {
            MediaCodecInfo[] mediaCodecInfos =
                    MediaUtils.findAllDecodersByMime(mAudioWrapper.mime);
            for (MediaCodecInfo mediaCodecInfo : mediaCodecInfos) {
                if (mediaCodecInfo == null) {
                    continue;
                }
                codecName = mediaCodecInfo.getName();
                if (TextUtils.isEmpty(codecName)) {
                    continue;
                }
                String tempCodecName = codecName.toLowerCase();
                if (tempCodecName.startsWith("omx.google.")
                        || tempCodecName.startsWith("c2.android.")
                        || tempCodecName.endsWith(".secure")
                        || (!tempCodecName.startsWith("omx.") && !tempCodecName.startsWith("c2."))) {
                    codecName = null;
                    continue;
                }
                break;
            }
            if (TextUtils.isEmpty(codecName)) {
                return false;
            }

            try {
                Log.d(TAG, "initAudioMediaCodec() audio CodecName: " + codecName);
                mAudioWrapper.decoderMediaCodec = MediaCodec.createByCodecName(codecName);
            } catch (NullPointerException
                    | IllegalArgumentException
                    | MediaCodec.CodecException
                    | IOException e) {
                e.printStackTrace();
                MediaUtils.releaseMediaCodec(mAudioWrapper.decoderMediaCodec);
                mAudioWrapper.decoderMediaCodec = null;
            }
            if (mAudioWrapper.decoderMediaCodec == null) {
                Log.e(TAG, "initAudioMediaCodec() create Audio MediaCodec failure");
                // MyToast.show("create Audio MediaCodec failure");
                return false;
            }
            mAudioWrapper.decoderMediaCodec.setCallback(
                    mAudioAsyncDecoderCallback, mAudioThreadHandler);
            mAudioWrapper.decoderMediaCodec.configure(
                    mAudioWrapper.decoderMediaFormat, null, null, 0);
            mAudioWrapper.decoderMediaCodec.start();
            Log.d(TAG, "initAudioMediaCodec() audio MediaCodec start");
        } else {
            mAudioWrapper.decoderMediaCodec =
                    MediaUtils.getAudioDecoderMediaCodec(
                            mAudioWrapper.mime,
                            mAudioWrapper.decoderMediaFormat);
            if (mAudioWrapper.decoderMediaCodec == null) {
                Log.e(TAG, "initAudioMediaCodec() create Audio MediaCodec failure");
                // MyToast.show("create Audio MediaCodec failure");
                return false;
            }
        }
        //////////////////////////////////////////////////////////////////////////////

        if (mAudioWrapper.decoderMediaFormat.containsKey("csd-0")) {
            ByteBuffer buffer = mAudioWrapper.decoderMediaFormat.getByteBuffer("csd-0");
            byte[] csd_0 = new byte[buffer.limit()];
            buffer.get(csd_0);
            Log.d(TAG, "initAudioMediaCodec() audio \n  csd-0: " + Arrays.toString(csd_0));
        }
        if (mAudioWrapper.decoderMediaFormat.containsKey("csd-1")) {
            ByteBuffer buffer = mAudioWrapper.decoderMediaFormat.getByteBuffer("csd-1");
            byte[] csd_1 = new byte[buffer.limit()];
            buffer.get(csd_1);
            Log.d(TAG, "initAudioMediaCodec() audio \n  csd-1: " + Arrays.toString(csd_1));
        }

        Log.d(TAG, "initAudioMediaCodec() end");
        /*if (!TextUtils.isEmpty(mType) && mType.startsWith("audio/")) {
            MyToast.show("音频硬解码");
        }*/
        return true;
    }

    /***
     .rmvb
     AVCodecID = 69
     Video: rv40 (RV40 / 0x30345652), yuv420p, 1920x1080, 1483 kb/s, 30 fps, 30 tbr, 1k tbn, 1k tbc
     Video: rv40 (RV40 / 0x30345652), yuv420p, 1376x768 [SAR 128:129 DAR 16:9], 833 kb/s,
     29.97 fps, 29.97 tbr, 1k tbn, 1k tbc
     .wmv
     Video: vc1 (Advanced) (WVC1 / 0x31435657), yuv420p, 1920x1080 [SAR 1:1 DAR 16:9], 4000 kb/s,
     59.94 fps, 59.94 tbr, 1k tbn, 119.88 tbc
     AVCodecID = 71
     Video: wmv3 (Main) (WMV3 / 0x33564D57), yuv420p, 1920x1080, 7000 kb/s, SAR 1:1 DAR 16:9,
     30 fps, 30 tbr, 1k tbn, 1k tbc
     .flv
     AVCodecID = 92
     Video: vp6f, yuv420p, 1920x1088, 305 kb/s, 29.97 fps, 29.97 tbr, 1k tbn, 1k tbc
     .mpg
     AVCodecID = 1
     Video: mpeg1video, yuv420p(tv), 1920x1080 [SAR 1:1 DAR 16:9], 1150 kb/s,
     29.97 fps, 29.97 tbr, 90k tbn, 29.97 tbc
     .avi
     AVCodecID = 7
     Video: mjpeg (MJPG / 0x47504A4D), yuvj422p(pc, bt470bg/unknown/unknown), 640x480,
     9210 kb/s, 30 fps, 30 tbr, 30 tbn, 30 tbc

     AVCodecID = 69 // rv40
     AVCodecID = 71 // wmv3
     AVCodecID = 92 // vp6f
     AVCodecID = 1 // mpeg1video
     AVCodecID = 7 // mjpeg

     audio/raw
     OMX.google.raw.decoder
     c2.android.raw.decoder
     audio/flac
     OMX.qti.audio.decoder.flac
     OMX.google.flac.decoder
     c2.android.flac.decoder
     audio/mpeg
     OMX.google.mp3.decoder
     c2.android.mp3.decoder
     audio/ac3
     OMX.MTK.AUDIO.DECODER.DSPAC3
     audio/eac3
     OMX.MTK.AUDIO.DECODER.DSPEAC3
     audio/ac4
     audio/mpeg-L2
     OMX.MTK.AUDIO.DECODER.DSPMP2

     audio/mp4a-latm
     OMX.google.aac.decoder
     c2.android.aac.decoder
     audio/x-ms-wma
     OMX.MTK.AUDIO.DECODER.DSPWMA

     video/raw
     无解码器
     video/hevc
     OMX.MTK.VIDEO.DECODER.HEVC
     OMX.MTK.VIDEO.DECODER.HEVC.secure
     OMX.qcom.video.decoder.hevc(硬解)
     OMX.qcom.video.decoder.hevc.secure(硬解)
     OMX.google.hevc.decoder(软解)
     c2.android.hevc.decoder(软解)

     video/avc
     OMX.MTK.VIDEO.DECODER.AVC
     OMX.MTK.VIDEO.DECODER.AVC.secure
     OMX.qcom.video.decoder.avc
     OMX.qcom.video.decoder.avc.secure
     OMX.google.h264.decoder
     c2.android.avc.decoder

     video/3gpp
     OMX.MTK.VIDEO.DECODER.H263
     OMX.qcom.video.decoder.h263
     OMX.google.h263.decoder
     c2.android.h263.decoder

     video/mp4v-es
     OMX.MTK.VIDEO.DECODER.MPEG4
     OMX.qcom.video.decoder.mpeg4
     OMX.google.mpeg4.decoder
     c2.android.mpeg4.decoder

     video/mpeg2
     OMX.MTK.VIDEO.DECODER.MPEG2
     OMX.MTK.VIDEO.DECODER.MPEG2.secure
     OMX.qcom.video.decoder.mpeg2
     OMX.qcom.video.decoder.mpeg2.secure
     OMX.google.mpeg2.decoder

     video/x-vnd.on2.vp8
     OMX.MTK.VIDEO.DECODER.VP8
     OMX.qcom.video.decoder.vp8
     OMX.google.vp8.decoder
     c2.android.vp8.decoder

     video/x-vnd.on2.vp9
     OMX.MTK.VIDEO.DECODER.VP9
     OMX.MTK.VIDEO.DECODER.VP9.secure
     OMX.qcom.video.decoder.vp9
     OMX.qcom.video.decoder.vp9.secure
     OMX.google.vp9.decoder
     c2.android.vp9.decoder

     video/dolby-vision
     OMX.dolby.vision.dvhe.stn.decoder
     OMX.dolby.vision.dvhe.stn.decoder.secure
     OMX.dolby.vision.dvhe.dtr.decoder
     OMX.dolby.vision.dvhe.dtr.decoder.secure
     OMX.dolby.vision.dvhe.st.decoder
     OMX.dolby.vision.dvhe.st.decoder.secure
     OMX.dolby.vision.dvav.se.decoder
     OMX.dolby.vision.dvav.se.decoder.secure

     video/scrambled
     无解码器

     video/av01
     无解码器

     if (codecName.startsWithIgnoreCase("c2.")) {
     return new CCodec;
     } else if (codecName.startsWithIgnoreCase("omx.")) {
     // at this time only ACodec specifies a mime type.
     return new ACodec;
     } else if (codecName.startsWithIgnoreCase("android.filter.")) {
     return new MediaFilter;
     } else {
     return NULL;
     }
     */
    public boolean initVideoMediaCodec(JniObject jniObject) {
        Log.w(TAG, "initVideoMediaCodec() start");
        if (jniObject == null
                || jniObject.valueObjectArray == null
                || jniObject.valueObjectArray.length < 2) {
            Log.e(TAG, "initVideoMediaCodec() jniObject failure");
            return false;
        }
        if (mSurface == null) {
            Log.e(TAG, "initVideoMediaCodec() mSurface is null");
            return false;
        }

        if (!mUseMediaCodecForVideo) {
            return false;
        }

        if (mContext != null) {
            SharedPreferences sp;
            if (mIsLocalPlayer) {
                sp = mContext.getSharedPreferences(
                        PREFERENCES_NAME, Context.MODE_PRIVATE);
            } else {
                sp = mContext.getSharedPreferences(
                        PREFERENCES_NAME_REMOTE, Context.MODE_PRIVATE);
            }
            int softSolution = sp.getInt(HARD_SOLUTION, 1);
            if (softSolution == 0) {
                return false;
            }
        }

        if (mVideoWrapper != null
                && mVideoWrapper.decoderMediaCodec != null) {
            // mVideoWrapper.decoderMediaCodec.flush();
            Log.e(TAG, "initVideoMediaCodec() video clear");
            MediaUtils.releaseMediaCodec(mVideoWrapper.decoderMediaCodec);
            mVideoWrapper.clear();
            mVideoWrapper.decoderMediaCodec = null;
            mVideoWrapper = null;
        }

        MediaFormat mediaFormat = null;
        String videoMime = null;
        Log.w(TAG, "initVideoMediaCodec() video       mimeType: " + jniObject.valueInt);
        // 现在只支持下面这几种硬解码
        switch (jniObject.valueInt) {
            // video/hevc
            case AV_CODEC_ID_HEVC:
                videoMime = MediaFormat.MIMETYPE_VIDEO_HEVC;
                break;
            // video/avc
            case AV_CODEC_ID_H264:
                videoMime = MediaFormat.MIMETYPE_VIDEO_AVC;
                break;
            // video/3gpp
            case AV_CODEC_ID_H263:// 还没有遇到这种视频
                videoMime = MediaFormat.MIMETYPE_VIDEO_H263;
                break;
            // video/mp4v-es
            case AV_CODEC_ID_MPEG4:
                videoMime = MediaFormat.MIMETYPE_VIDEO_MPEG4;
                break;
            // video/x-vnd.on2.vp8
            case AV_CODEC_ID_VP8:
                videoMime = MediaFormat.MIMETYPE_VIDEO_VP8;
                break;
            // video/x-vnd.on2.vp9
            case AV_CODEC_ID_VP9:
                videoMime = MediaFormat.MIMETYPE_VIDEO_VP9;
                break;
            // video/mpeg2
            case AV_CODEC_ID_MPEG2VIDEO:
                videoMime = MediaFormat.MIMETYPE_VIDEO_MPEG2;
                break;
            // 上面几种是能解码成功的

            case AV_CODEC_ID_MPEG1VIDEO:// crash
                //videoMime = MediaFormat.MIMETYPE_VIDEO_MPEG2;
                break;
            case AV_CODEC_ID_MJPEG:// ok
                //videoMime = "video/mjpeg";
                break;
            case AV_CODEC_ID_RV40:
                //videoMime = "";
                break;
            case AV_CODEC_ID_VC1:// error
            case AV_CODEC_ID_WMV3:// crash
                //videoMime = "video/x-ms-wmv";
                break;
            case AV_CODEC_ID_VP6:
                //videoMime = "";
                break;
            case AV_CODEC_ID_VP6F:// ok
                //videoMime = "video/x-vp6";
                break;
            default:
                break;
        }

        Log.w(TAG, "initVideoMediaCodec() video           mime: " + videoMime);
        if (TextUtils.isEmpty(videoMime)) {
            Log.e(TAG, "initVideoMediaCodec() videoMime is empty");
            MyToast.show("video mime is empty");
            return false;
        }

        Object[] valueObjectArray = jniObject.valueObjectArray;
        long[] parameters = (long[]) valueObjectArray[0];
        // 视频宽
        int width = (int) parameters[0];
        // 视频高
        int height = (int) parameters[1];
        // 单位: 秒
        long duration = parameters[2];
        // 帧率
        int frame_rate = (int) parameters[3];
        // 码率
        int bitrate = (int) parameters[4];
        int max_input_size = (int) parameters[5];

        // region

        // MediaFormat的参数并不是设置的值越多越好,有些值设置了视频反而不能播放
        // mediaFormat = MediaUtils.getVideoDecoderMediaFormat(width, height);
        mediaFormat = MediaFormat.createVideoFormat(videoMime, width, height);
        mediaFormat.setInteger(MediaFormat.KEY_MAX_WIDTH, width);
        mediaFormat.setInteger(MediaFormat.KEY_MAX_HEIGHT, height);
        if (max_input_size > 0) {
            mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, max_input_size);
        } else {
            mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, width * height * 4);
        }
        // 视频需要旋转的角度(90时为顺时针旋转90度)
        // mediaFormat.setInteger(MediaFormat.KEY_ROTATION, 0);
        // 下面这个值设置后,用华为手机拍摄的4K视频就不能硬解码
        // mediaFormat.setInteger(MediaFormat.KEY_PRIORITY, 0);
        if (duration > 0) {
            mediaFormat.setLong(MediaFormat.KEY_DURATION, duration * 1000000L);
        } else {
            // -   2077252342L(C++ 时间)
            // -9223372036854L(java时间)
            mediaFormat.setLong(MediaFormat.KEY_DURATION, duration);
        }
        if (frame_rate > 0) {
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frame_rate);
        }
        if (bitrate > 0) {
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
            // 表示完全不控制码率，尽最大可能保证图像质量
            mediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE,
                    MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ);
        }
        /*mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);*/

        Object object = valueObjectArray[1];
        byte[] sps_pps = null;
        if (object != null) {
            sps_pps = (byte[]) object;
            Log.w(TAG, "initVideoMediaCodec() video sps_pps.length: " + sps_pps.length +
                    " \nsps_pps: " + Arrays.toString(sps_pps));
        } else {
            Log.w(TAG, "initVideoMediaCodec() video sps_pps is null");
        }
        if (TextUtils.equals(videoMime, MediaFormat.MIMETYPE_VIDEO_VP8)
                || TextUtils.equals(videoMime, MediaFormat.MIMETYPE_VIDEO_VP9)
                // MTK
                || TextUtils.equals(videoMime, "video/mjpeg")
                || TextUtils.equals(videoMime, "video/x-vp6")) {
            // 没有csd-0和csd-1
        } else if (TextUtils.equals(videoMime, MediaFormat.MIMETYPE_VIDEO_HEVC)
                || TextUtils.equals(videoMime, MediaFormat.MIMETYPE_VIDEO_MPEG4)
                || TextUtils.equals(videoMime, MediaFormat.MIMETYPE_VIDEO_MPEG2)
                // MTK
                || TextUtils.equals(videoMime, "video/x-ms-wmv")) {
            // 只有csd-0
            if (sps_pps != null) {
                mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(sps_pps));
            }
        } else if (TextUtils.equals(videoMime, MediaFormat.MIMETYPE_VIDEO_AVC)) {
            // 同时有csd-0和csd-1
            // csd-0: 0, 0, 0, 1, 103,... csd-1: 0, 0, 0, 1, 104,...
            // csd-0:    0, 0, 1, 103,... csd-1:    0, 0, 1, 104,...
            // csd-0:    0, 0, 1,  39,... csd-1:    0, 0, 1,  40,...
            if (sps_pps != null) {
                try {
                    ByteBuffer byteBuffer = ByteBuffer.wrap(sps_pps);
                    byteBuffer.position(0);
                    byteBuffer.limit(sps_pps.length);
                    int index = -1;
                    if (sps_pps[0] == 0
                            && sps_pps[1] == 0
                            && sps_pps[2] == 0
                            && sps_pps[3] == 1) {
                        for (int i = 1; i < sps_pps.length; i++) {
                            if (sps_pps[i] == 0
                                    && sps_pps[i + 1] == 0
                                    && sps_pps[i + 2] == 0
                                    && sps_pps[i + 3] == 1) {
                                index = i;
                                break;
                            }
                        }
                    } else if (sps_pps[0] == 0
                            && sps_pps[1] == 0
                            && sps_pps[2] == 1) {
                        for (int i = 1; i < sps_pps.length; i++) {
                            if (sps_pps[i] == 0
                                    && sps_pps[i + 1] == 0
                                    && sps_pps[i + 2] == 1) {
                                index = i;
                                break;
                            }
                        }
                    }

                    byte[] sps = null;
                    byte[] pps = null;
                    if (index != -1) {
                        sps = new byte[index];
                        pps = new byte[sps_pps.length - index];
                        byteBuffer.get(sps, 0, sps.length);
                        byteBuffer.get(pps, 0, pps.length);
                        mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(sps));
                        mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(pps));
                    } else {
                        /***
                         /storage/2430-1702/BaiduNetdisk/video/流浪的地球.mp4
                         */
                        Log.w(TAG, "initVideoMediaCodec() doesn't find index value");
                        int spsIndex = -1;
                        int spsLength = 0;
                        int ppsIndex = -1;
                        int ppsLength = 0;
                        for (int i = 0; i < sps_pps.length; i++) {
                            if (sps_pps[i] == 103) {
                                // 0x67 = 103
                                if (spsIndex == -1) {
                                    spsIndex = i;
                                    spsLength = sps_pps[i - 1];
                                    if (spsLength <= 0) {
                                        spsIndex = -1;
                                    }
                                }
                            } else if (sps_pps[i] == 104) {
                                // 103后面可能有2个或多个104
                                // 0x68 = 104
                                ppsIndex = i;
                                ppsLength = sps_pps[i - 1];
                            }
                        }
                        if (spsIndex == -1 || ppsIndex == -1) {
                            spsIndex = -1;
                            spsLength = 0;
                            ppsIndex = -1;
                            ppsLength = 0;
                            for (int i = 0; i < sps_pps.length; i++) {
                                if (sps_pps[i] == 39) {
                                    if (spsIndex == -1) {
                                        spsIndex = i;
                                        spsLength = sps_pps[i - 1];
                                        if (spsLength <= 0) {
                                            spsIndex = -1;
                                        }
                                    }
                                } else if (sps_pps[i] == 40) {
                                    ppsIndex = i;
                                    ppsLength = sps_pps[i - 1];
                                }
                            }
                        }
                        if (spsIndex != -1 && ppsIndex != -1) {
                            byte[] tempSpsPps = new byte[sps_pps.length];
                            byteBuffer.get(tempSpsPps, 0, sps_pps.length);
                            sps = new byte[spsLength + 4];
                            pps = new byte[ppsLength + 4];
                            // 0x00, 0x00, 0x00, 0x01
                            sps[0] = pps[0] = 0;
                            sps[1] = pps[1] = 0;
                            sps[2] = pps[2] = 0;
                            sps[3] = pps[3] = 1;
                            System.arraycopy(tempSpsPps, spsIndex, sps, 4, spsLength);
                            System.arraycopy(tempSpsPps, ppsIndex, pps, 4, ppsLength);
                            mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(sps));
                            mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(pps));
                        }
                    }

                    if (sps != null && pps != null) {
                        Log.i(TAG, "initVideoMediaCodec() video \n  csd-0: " +
                                Arrays.toString(sps));
                        Log.i(TAG, "initVideoMediaCodec() video \n  csd-1: " +
                                Arrays.toString(pps));
                    } else {
                        Log.e(TAG, "initVideoMediaCodec() sps/pps is null");
                        MyToast.show("sps/pps is null");
                        return false;
                    }
                } catch (Exception e) {
                    // java.lang.NegativeArraySizeException: -70
                    Log.e(TAG, "initVideoMediaCodec() Exception: \n" + e);
                    MyToast.show(e.toString());
                    return false;
                }
            }
        }

        // endregion

        Log.w(TAG, "initVideoMediaCodec() video    mediaFormat: \n" + mediaFormat);

        mVideoWrapper = new VideoWrapper(TYPE_VIDEO);
        mVideoWrapper.isHandling = true;
        mVideoWrapper.render = true;
        mVideoWrapper.mime = videoMime;
        mVideoWrapper.decoderMediaFormat = mediaFormat;
        mVideoWrapper.mSurface = mSurface;

        //////////////////////////////////////////////////////////////////////////////
        if (VIDEO_NEED_TO_ASYNC) {
            MediaCodecInfo[] mediaCodecInfos =
                    MediaUtils.findAllDecodersByMime(mVideoWrapper.mime);
            String codecName = null;
            for (MediaCodecInfo mediaCodecInfo : mediaCodecInfos) {
                if (mediaCodecInfo == null) {
                    continue;
                }
                codecName = mediaCodecInfo.getName();
                if (TextUtils.isEmpty(codecName)) {
                    continue;
                }
                String tempCodecName = codecName.toLowerCase();
                if (tempCodecName.startsWith("omx.google.")
                        || tempCodecName.startsWith("c2.android.")
                        // 用于加密的视频
                        || tempCodecName.endsWith(".secure")
                        || (!tempCodecName.startsWith("omx.") && !tempCodecName.startsWith("c2."))) {
                    codecName = null;
                    continue;
                }
                // 保证是硬解的解码器 如: OMX.qcom.video.decoder.avc OMX.MTK.VIDEO.DECODER.HEVC
                break;
            }
            if (TextUtils.isEmpty(codecName)) {
                return false;
            }

            try {
                Log.w(TAG, "initVideoMediaCodec() audio CodecName: " + codecName);
                mVideoWrapper.decoderMediaCodec = MediaCodec.createByCodecName(codecName);
            } catch (NullPointerException
                    | IllegalArgumentException
                    | MediaCodec.CodecException
                    | IOException e) {
                e.printStackTrace();
                MediaUtils.releaseMediaCodec(mVideoWrapper.decoderMediaCodec);
                mVideoWrapper.decoderMediaCodec = null;
            }

            try {
                if (mVideoWrapper.decoderMediaCodec != null) {
                    mVideoWrapper.decoderMediaCodec.setCallback(
                            mVideoAsyncDecoderCallback, mVideoThreadHandler);
                    mVideoWrapper.decoderMediaCodec.configure(
                            mVideoWrapper.decoderMediaFormat, mVideoWrapper.mSurface, null, 0);
                    mVideoWrapper.decoderMediaCodec.start();
                    Log.w(TAG, "initVideoMediaCodec() video MediaCodec start");
                }
            } catch (NullPointerException
                    | IllegalArgumentException
                    | MediaCodec.CodecException e) {
                e.printStackTrace();
                MediaUtils.releaseMediaCodec(mVideoWrapper.decoderMediaCodec);
                mVideoWrapper.decoderMediaCodec = null;
            }

            if (mVideoWrapper.decoderMediaCodec == null) {
                Log.e(TAG, "initVideoMediaCodec() create Video MediaCodec failure");
                MyToast.show("create Video MediaCodec failure");
                return false;
            }
        } else {
            mVideoWrapper.decoderMediaCodec =
                    MediaUtils.getVideoDecoderMediaCodec(
                            mVideoWrapper.mime,
                            mVideoWrapper.decoderMediaFormat,
                            mVideoWrapper.mSurface);
            if (mVideoWrapper.decoderMediaCodec == null) {
                Log.e(TAG, "initVideoMediaCodec() create Video MediaCodec failure");
                MyToast.show("create Video MediaCodec failure");
                return false;
            }
        }
        //////////////////////////////////////////////////////////////////////////////

        if (mVideoWrapper.decoderMediaFormat.containsKey("csd-0")) {
            ByteBuffer buffer = mVideoWrapper.decoderMediaFormat.getByteBuffer("csd-0");
            byte[] csd_0 = new byte[buffer.limit()];
            buffer.get(csd_0);
            Log.w(TAG, "initVideoMediaCodec() video \n  csd-0: " + Arrays.toString(csd_0));
        }
        if (mVideoWrapper.decoderMediaFormat.containsKey("csd-1")) {
            ByteBuffer buffer = mVideoWrapper.decoderMediaFormat.getByteBuffer("csd-1");
            byte[] csd_1 = new byte[buffer.limit()];
            buffer.get(csd_1);
            Log.w(TAG, "initVideoMediaCodec() video \n  csd-1: " + Arrays.toString(csd_1));
        }

        Log.w(TAG, "initVideoMediaCodec() end");
        return true;
    }

    public boolean feedInputBufferAndDrainOutputBuffer(SimpleWrapper wrapper) {
        if (wrapper.type == TYPE_AUDIO) {
            if (AUDIO_NEED_TO_ASYNC) {
                if (mAudioWrapper != null
                        && mAudioWrapper.isHandling) {
                    AVPacket avPacket = new AVPacket(wrapper.size);
                    avPacket.data = wrapper.data;
                    avPacket.presentationTimeUs = wrapper.sampleTime;
                    avPacket.flags = 0;
                    try {
                        // 超出限制就会阻塞
                        mAudioInputDatasQueue.put(avPacket);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            }
        } else {
            if (VIDEO_NEED_TO_ASYNC) {
                if (mVideoWrapper != null
                        && mVideoWrapper.isHandling) {
                    AVPacket avPacket = new AVPacket(wrapper.size);
                    avPacket.data = wrapper.data;
                    avPacket.presentationTimeUs = wrapper.sampleTime;
                    avPacket.flags = 0;
                    /*Log.w(TAG, "feedInputBufferAndDrainOutputBuffer() data:\n" +
                            Arrays.toString(wrapper.data));
                    SystemClock.sleep(1000);*/
                    try {
                        // 超出限制就会阻塞
                        mVideoInputDatasQueue.put(avPacket);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            }
        }

        return EDMediaCodec.feedInputBufferAndDrainOutputBuffer(
                mCallback,
                (wrapper.type == TYPE_AUDIO
                        ? EDMediaCodec.TYPE.TYPE_AUDIO
                        : EDMediaCodec.TYPE.TYPE_VIDEO),
                wrapper.decoderMediaCodec,
                null,
                wrapper.data,
                0,
                wrapper.size,
                wrapper.sampleTime,
                0,
                wrapper.render,
                true);
    }

    // video
    private JniObject mVideoJniObject = new JniObject();
    private int[] videoValueIntArray = new int[2];
    private Object[] videoValueObjectArray = new Object[2];
    // audio
    private JniObject mAudioJniObject = new JniObject();
    private int[] audioValueIntArray = new int[2];
    private Object[] audioValueObjectArray = new Object[2];

    private EDMediaCodec.Callback mCallback = new EDMediaCodec.Callback() {

        private int width;
        private int height;
        // 对齐宽高
        private int alignWidth;
        private int alignHeight;
        private boolean needToDoIt;

        @Override
        public boolean isVideoFinished() {
            return !mVideoWrapper.isHandling;
        }

        @Override
        public boolean isAudioFinished() {
            return !mAudioWrapper.isHandling;
        }

        @Override
        public void handleVideoOutputFormat(MediaFormat mediaFormat) {
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

             durationUs: 从Us到秒, 1 s = 1000 * 1000 Us
             */
            try {
                MediaFormat newMediaFormat = mediaFormat;
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
                    if (oldMap.containsKey("mime-old")) {
                        return;
                    }
                    String mime = (String) oldMap.get("mime");
                    for (Map.Entry<String, Object> entry : newMap.entrySet()) {
                        oldMap.put(entry.getKey(), entry.getValue());
                    }
                    oldMap.put("mime-old", mime);
                }
                Log.w(TAG, "handleVideoOutputFormat() newMediaFormat: \n" +
                        mVideoWrapper.decoderMediaFormat);

                /*width = mVideoWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_WIDTH);
                if (mVideoWrapper.decoderMediaFormat.containsKey("crop-left")
                        && mVideoWrapper.decoderMediaFormat.containsKey("crop-right")) {
                    width = mVideoWrapper.decoderMediaFormat.getInteger("crop-right") + 1 -
                            mVideoWrapper.decoderMediaFormat.getInteger("crop-left");
                }
                height = mVideoWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
                if (mVideoWrapper.decoderMediaFormat.containsKey("crop-top")
                        && mVideoWrapper.decoderMediaFormat.containsKey("crop-bottom")) {
                    height = mVideoWrapper.decoderMediaFormat.getInteger("crop-bottom") + 1 -
                            mVideoWrapper.decoderMediaFormat.getInteger("crop-top");
                }

                int keyStride = mVideoWrapper.decoderMediaFormat.getInteger(
                        MediaFormat.KEY_STRIDE);
                int keyCliceHeight = mVideoWrapper.decoderMediaFormat.getInteger(
                        MediaFormat.KEY_SLICE_HEIGHT);
                alignWidth = keyStride;
                alignHeight = keyCliceHeight;
                needToDoIt = false;
                if (alignWidth != 0 && alignHeight != 0 && alignHeight != height) {
                    needToDoIt = true;
                }*/
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void handleAudioOutputFormat(MediaFormat mediaFormat) {
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
                MediaFormat newMediaFormat = mediaFormat;
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
                    if (oldMap.containsKey("mime-old")) {
                        return;
                    }
                    String mime = (String) oldMap.get("mime");
                    for (Map.Entry<String, Object> entry : newMap.entrySet()) {
                        oldMap.put(entry.getKey(), entry.getValue());
                    }
                    oldMap.put("mime-old", mime);
                }
                Log.d(TAG, "handleAudioOutputFormat() newMediaFormat: \n" +
                        mAudioWrapper.decoderMediaFormat);
            } catch (Exception e) {
                e.printStackTrace();
            }

            mExoAudioTrack.mMediaFormat = mAudioWrapper.decoderMediaFormat;
            mExoAudioTrack.createAudioTrack();

            if (mExoAudioTrack.mAudioTrack == null) {
                Log.e(TAG, "handleAudioOutputFormat() AudioTrack is null");
                mAudioWrapper.isHandling = false;
                handleAudioOutputBuffer(-1, null, null, -1);
            }

            /*// 创建AudioTrack
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
            } else {
                Log.e(TAG, "handleAudioOutputFormat() AudioTrack is null");
                mAudioWrapper.isHandling = false;
                handleAudioOutputBuffer(-1, null, null, -1);
                return;
            }

            mAudioWrapper.decoderMediaFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
            mAudioWrapper.decoderMediaFormat.setInteger(MediaFormat.KEY_PRIORITY, 0);
            if (AACPlayer.sampleRateIndexMap.containsKey(sampleRateInHz)
                    && AACPlayer.channelConfigIndexMap.containsKey(channelCount)) {
                List<byte[]> list = new ArrayList<>();
                list.add(MediaUtils.buildAacAudioSpecificConfig(
                        AACPlayer.sampleRateIndexMap.get(sampleRateInHz),
                        AACPlayer.channelConfigIndexMap.get(channelCount)));
                MediaUtils.setCsdBuffers(mAudioWrapper.decoderMediaFormat, list);
            }*/
        }

        @Override
        public int handleVideoOutputBuffer(int roomIndex, ByteBuffer room,
                                           MediaCodec.BufferInfo roomInfo, int roomSize) {
            /*if(needToDoIt){
                byte[] outData = new byte[roomSize];
                room.get(outData);

                byte[] yData = new byte[width * height];
                byte[] uData = new byte[width * height / 4];
                byte[] vData = new byte[width * height / 4];

                yuvCopy(outData, 0, alignWidth, alignHeight, yData, width, height);
                yuvCopy(outData, alignWidth * alignHeight, alignWidth / 2, alignHeight / 2,
                uData, width / 2, height / 2);
                yuvCopy(outData, alignWidth * alignHeight * 5 / 4, alignWidth / 2,
                alignHeight / 2, vData, width / 2, height / 2);
            }*/
            if (roomSize <= 0) {
                return -1;
            }

            videoValueIntArray[0] = roomIndex;
            videoValueIntArray[1] = roomSize;
            videoValueObjectArray[0] = room;
            videoValueObjectArray[1] = roomInfo;
            mVideoJniObject.valueIntArray = videoValueIntArray;
            mVideoJniObject.valueObjectArray = videoValueObjectArray;
            return Integer.parseInt(
                    mFFMPEG.onTransact(
                            FFMPEG.DO_SOMETHING_CODE_handleVideoOutputBuffer, mVideoJniObject));
        }

        @Override
        public int handleAudioOutputBuffer(int roomIndex, ByteBuffer room,
                                           MediaCodec.BufferInfo roomInfo, int roomSize) {
            if (roomSize <= 0) {
                return -1;
            }

            audioValueIntArray[0] = roomIndex;
            audioValueIntArray[1] = roomSize;
            audioValueObjectArray[0] = room;
            audioValueObjectArray[1] = roomInfo;
            mAudioJniObject.valueIntArray = audioValueIntArray;
            mAudioJniObject.valueObjectArray = audioValueObjectArray;
            Integer.parseInt(
                    mFFMPEG.onTransact(
                            FFMPEG.DO_SOMETHING_CODE_handleAudioOutputBuffer, mAudioJniObject));

            if (mAudioWrapper.isHandling
                    && mExoAudioTrack.mAudioTrack != null
                    && room != null) {
                byte[] audioData = new byte[roomSize];
                room.get(audioData, 0, audioData.length);
                mExoAudioTrack.mAudioTrack.write(audioData, roomInfo.offset, audioData.length);
            }

            return 0;
        }
    };

    /////////////////////////////////////////////////////////////////////////////////////

    private final static boolean VIDEO_NEED_TO_ASYNC = true;
    private final static boolean AUDIO_NEED_TO_ASYNC = true;

    private final static ArrayBlockingQueue<AVPacket> mVideoInputDatasQueue =
            new ArrayBlockingQueue<AVPacket>(5);
    private final static ArrayBlockingQueue<AVPacket> mAudioInputDatasQueue =
            new ArrayBlockingQueue<AVPacket>(5);

    private MediaCodec.Callback mVideoAsyncDecoderCallback = new MediaCodec.Callback() {

        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec codec, int roomIndex) {
            AVPacket avPacket = null;
            if (mVideoWrapper != null && mVideoWrapper.isHandling) {
                try {
                    // avPacket = mInputDatasQueue.poll();
                    // 没有元素就会阻塞
                    avPacket = mVideoInputDatasQueue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (avPacket == null
                    || mVideoWrapper == null
                    || !mVideoWrapper.isHandling
                    || roomIndex < 0) {
                return;
            }

            ByteBuffer room = null;
            // 根据房间号找到房间
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    room = codec.getInputBuffer(roomIndex);
                } else {
                    room = codec.getInputBuffers()[roomIndex];
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
                room = null;
            }
            if (room == null) {
                avPacket = null;
                return;
            }
            byte[] data = avPacket.data;
            int size = avPacket.size;
            long presentationTimeUs = avPacket.presentationTimeUs;
            int flags = avPacket.flags;
            if (size <= 0) {
                flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
            }
            // Log.w(TAG, "onInputBufferAvailable() data:\n" + Arrays.toString(data));
            // 入住之前打扫一下房间
            room.clear();
            // 入住
            room.put(data, 0, size);
            try {
                codec.queueInputBuffer(roomIndex, 0, size, presentationTimeUs, flags);
            } catch (IllegalStateException
                    | NullPointerException
                    | MediaCodec.CryptoException e) {
                e.printStackTrace();
            }
            avPacket.clear();
            avPacket = null;
        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec codec, int roomIndex,
                                            @NonNull MediaCodec.BufferInfo roomInfo) {
            if (mVideoWrapper == null || !mVideoWrapper.isHandling || roomIndex < 0) {
                return;
            }

            ByteBuffer room = null;
            // 根据房间号找到房间
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    room = codec.getOutputBuffer(roomIndex);
                } else {
                    room = codec.getOutputBuffers()[roomIndex];
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
                room = null;
            }
            // 房间大小
            int roomSize = roomInfo.size;
            // 不能根据room是否为null来判断是audio还是video(但我的三星Note2手机上是可以的)
            if (room != null) {
                room.position(roomInfo.offset);
                room.limit(roomInfo.offset + roomSize);
                mCallback.handleVideoOutputBuffer(roomIndex, room, roomInfo, roomSize);
                room.clear();
            } else {
                mCallback.handleVideoOutputBuffer(roomIndex, null, null, -1);
            }

            if (mVideoWrapper != null && mVideoWrapper.isHandling) {
                try {
                    codec.releaseOutputBuffer(roomIndex, true);
                } catch (IllegalStateException
                        | NullPointerException
                        | MediaCodec.CryptoException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
            if (mVideoWrapper == null || !mVideoWrapper.isHandling) {
                return;
            }

            mCallback.handleVideoOutputBuffer(-1, null, null, -1);
        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec,
                                          @NonNull MediaFormat mediaFormat) {
            if (mVideoWrapper == null || !mVideoWrapper.isHandling || mediaFormat == null) {
                return;
            }

            mCallback.handleVideoOutputFormat(mediaFormat);
        }
    };

    private MediaCodec.Callback mAudioAsyncDecoderCallback = new MediaCodec.Callback() {

        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec codec, int roomIndex) {
            AVPacket avPacket = null;
            if (mAudioWrapper != null && mAudioWrapper.isHandling) {
                try {
                    // avPacket = mInputDatasQueue.poll();
                    // 没有元素就会阻塞
                    // audio会被一直block住,原因不详
                    avPacket = mAudioInputDatasQueue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (avPacket == null
                    || mAudioWrapper == null
                    || !mAudioWrapper.isHandling
                    || roomIndex < 0) {
                return;
            }

            ByteBuffer room = null;
            // 根据房间号找到房间
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    room = codec.getInputBuffer(roomIndex);
                } else {
                    room = codec.getInputBuffers()[roomIndex];
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
                room = null;
            }
            if (room == null) {
                avPacket = null;
                return;
            }
            byte[] data = avPacket.data;
            int size = avPacket.size;
            long presentationTimeUs = avPacket.presentationTimeUs;
            int flags = avPacket.flags;
            if (size <= 0) {
                flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
            }
            // 入住之前打扫一下房间
            room.clear();
            // 入住
            room.put(data, 0, size);
            try {
                codec.queueInputBuffer(roomIndex, 0, size, presentationTimeUs, flags);
            } catch (IllegalStateException
                    | NullPointerException
                    | MediaCodec.CryptoException e) {
                e.printStackTrace();
            }
            avPacket.clear();
            avPacket = null;
        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec codec, int roomIndex,
                                            @NonNull MediaCodec.BufferInfo roomInfo) {
            if (mAudioWrapper == null || !mAudioWrapper.isHandling || roomIndex < 0) {
                return;
            }

            ByteBuffer room = null;
            // 根据房间号找到房间
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    room = codec.getOutputBuffer(roomIndex);
                } else {
                    room = codec.getOutputBuffers()[roomIndex];
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
                room = null;
            }
            // 房间大小
            int roomSize = roomInfo.size;
            // 不能根据room是否为null来判断是audio还是video(但我的三星Note2手机上是可以的)
            if (room != null) {
                room.position(roomInfo.offset);
                room.limit(roomInfo.offset + roomSize);
                mCallback.handleAudioOutputBuffer(roomIndex, room, roomInfo, roomSize);
                room.clear();
            } else {
                mCallback.handleAudioOutputBuffer(roomIndex, null, null, -1);
            }

            if (mAudioWrapper != null && mAudioWrapper.isHandling) {
                try {
                    codec.releaseOutputBuffer(roomIndex, false);
                } catch (IllegalStateException
                        | NullPointerException
                        | MediaCodec.CryptoException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
            if (mAudioWrapper == null || !mAudioWrapper.isHandling) {
                return;
            }

            mCallback.handleAudioOutputBuffer(-1, null, null, -1);
        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec,
                                          @NonNull MediaFormat mediaFormat) {
            if (mAudioWrapper == null || !mAudioWrapper.isHandling || mediaFormat == null) {
                return;
            }

            mCallback.handleAudioOutputFormat(mediaFormat);
        }
    };

}

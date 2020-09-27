package com.weidi.media.wdplayer.exo;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodecInfo;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.weidi.media.wdplayer.util.MediaUtils;
import com.weidi.media.wdplayer.video_player.FFMPEG;

import static com.weidi.media.wdplayer.Constants.PLAYBACK_IS_MUTE;
import static com.weidi.media.wdplayer.Constants.PREFERENCES_NAME;

public class ExoAudioTrack {

    private static final String TAG =
            "player_alexander";
    //ExoAudioTrack.class.getSimpleName();

    // additional keys in android.media.MediaFormat for dualMono functionality.
    static final String KEY_IS_DUAL_MONO = "is-dual-mono";
    public static final String EXO_KEY_DUAL_MONO_TYPE = "dual-mono-type";
    // values gotten by EY_DUAL_MONO_TYPE.
    public static final String EXO_DUAL_MONO_TYPE_MAIN = "Main";
    public static final String EXO_DUAL_MONO_TYPE_SUB = "Sub";
    public static final String EXO_DUAL_MONO_TYPE_MAIN_SUB = "Main+Sub";

    /**
     * The minimum audio capabilities supported by all devices.
     */

    private static final AudioCapabilities DEFAULT_AUDIO_CAPABILITIES =
            new AudioCapabilities(new int[]{AudioFormat.ENCODING_PCM_16BIT,
                    AudioFormat.ENCODING_AC3,
                    AudioFormat.ENCODING_E_AC3,
                    AudioFormat.ENCODING_E_AC3_JOC,
                    AudioFormat.ENCODING_DTS,
                    AudioFormat.ENCODING_DTS_HD,
                    C.ENCODING_AAC_LC,
                    C.ENCODING_AAC_HE_V1,
                    C.ENCODING_AAC_HE_V2,
                    C.ENCODING_AC4}, 8);

    private static final DecoderInfo PASSTHROUGH_DECODER_INFO =
            new DecoderInfo("OMX.google.raw.decoder", null);
    private final static int SUPPORT_CHANNEL_MAX_COUNT = 8;
    /**
     * The length for passthrough {@link AudioTrack} buffers, in microseconds.
     */
    private static final long PASSTHROUGH_BUFFER_DURATION_US = 250000;
    /**
     * A multiplication factor to apply to the minimum buffer size requested by the underlying
     * {@link AudioTrack}.
     */
    private static final int BUFFER_MULTIPLICATION_FACTOR = 4;
    /**
     * A minimum length for the {@link AudioTrack} buffer, in microseconds.
     */
    private static final long MIN_BUFFER_DURATION_US = 250000;
    /**
     * A maximum length for the {@link AudioTrack} buffer, in microseconds.
     */
    private static final long MAX_BUFFER_DURATION_US = 750000;

    public String mime;
    public AudioTrack mAudioTrack;
    public android.media.MediaFormat mMediaFormat;
    public ExoMediaFormat mFormat;
    public Context mContext;
    public boolean mPassthroughEnabled = false;

    public void setVolume(float volume) {
        if (mAudioTrack == null
                || mAudioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
            return;
        }
        if (volume < 0 || volume > 1.0f) {
            volume = FFMPEG.VOLUME_NORMAL;
        }
        if (Build.VERSION.SDK_INT >= 21) {
            mAudioTrack.setVolume(volume);
        } else {
            mAudioTrack.setStereoVolume(volume, volume);
        }
    }

    public void createAudioTrack() {
        if (mMediaFormat == null || TextUtils.isEmpty(mime)) {
            return;
        }

        MediaUtils.releaseAudioTrack(mAudioTrack);
        mAudioTrack = null;

        int sampleRateInHz =
                mMediaFormat.getInteger(android.media.MediaFormat.KEY_SAMPLE_RATE);
        int channelCount =
                mMediaFormat.getInteger(android.media.MediaFormat.KEY_CHANNEL_COUNT);
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        if (mMediaFormat.containsKey(android.media.MediaFormat.KEY_PCM_ENCODING)) {
            audioFormat = mMediaFormat.getInteger(android.media.MediaFormat.KEY_PCM_ENCODING);
        }

        mAudioTrack = MediaUtils.createAudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRateInHz, channelCount, audioFormat,
                AudioTrack.MODE_STREAM);

        if (mAudioTrack != null) {
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
            mAudioTrack.play();
        }

        if (true) {
            return;
        }

        //////////////////////////////////////////////////////////////////////////////////////

        if (mFormat == null) {
            isPassthroughEnabled();
        }

        if (mPassthroughEnabled) {
        } else {
            mime = MimeTypes.AUDIO_RAW;
        }
        sampleRateInHz =
                mMediaFormat.getInteger(android.media.MediaFormat.KEY_SAMPLE_RATE);
        channelCount =
                mMediaFormat.getInteger(android.media.MediaFormat.KEY_CHANNEL_COUNT);
        if (channelCount > SUPPORT_CHANNEL_MAX_COUNT) {
            channelCount = SUPPORT_CHANNEL_MAX_COUNT;
        }
        // pcmEncoding
        audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        if (mMediaFormat.containsKey(android.media.MediaFormat.KEY_PCM_ENCODING)) {
            audioFormat = mMediaFormat.getInteger(android.media.MediaFormat.KEY_PCM_ENCODING);
        }
        int aacProfile = 0;
        if (TextUtils.equals(mime, MimeTypes.AUDIO_AAC)) {
            if (mMediaFormat.containsKey(
                    android.media.MediaFormat.KEY_AAC_PROFILE)) {
                aacProfile = mMediaFormat.getInteger(
                        android.media.MediaFormat.KEY_AAC_PROFILE);
            }
            if (aacProfile == MediaCodecInfo.CodecProfileLevel.AACObjectLC
                    || aacProfile == MediaCodecInfo.CodecProfileLevel.AACObjectHE
                    || aacProfile == MediaCodecInfo.CodecProfileLevel.AACObjectHE_PS) {
                RendererConfiguration.getInstance().setAacAudioDisablePassthrough(false);
            } else {
                RendererConfiguration.getInstance().setAacAudioDisablePassthrough(true);
            }
        }
        if (aacProfile == MediaCodecInfo.CodecProfileLevel.AACObjectHE
                || aacProfile == MediaCodecInfo.CodecProfileLevel.AACObjectHE_PS) {
            sampleRateInHz *= 2;
        }
        int audioBitrate = 0;
        if (mMediaFormat.containsKey(android.media.MediaFormat.KEY_BIT_RATE)) {
            audioBitrate = mMediaFormat.getInteger(android.media.MediaFormat.KEY_BIT_RATE);
        }
        if (audioBitrate != 0) {
            RendererConfiguration.getInstance().setAudioBitrate(audioBitrate);
        } else {
            audioBitrate = RendererConfiguration.getInstance().getAudioBitrate();
        }

        String dualMonoType = null;
        if (mFormat != null) {
            android.media.MediaFormat enabledSourceFormat = mFormat.getFrameworkMediaFormatV16();
            dualMonoType = enabledSourceFormat.getString(EXO_KEY_DUAL_MONO_TYPE);
        }
        if (TextUtils.equals(mime, MimeTypes.AUDIO_AAC)) {
            mPassthroughEnabled = dualMonoType == null && isValidAacProfile(aacProfile);
        }

        int sourceEncoding;
        if (mPassthroughEnabled) {
            sourceEncoding = getEncodingForMimeType(mime, aacProfile);
        } else if (audioFormat == C.ENCODING_PCM_8BIT
                || audioFormat == C.ENCODING_PCM_16BIT
                || audioFormat == C.ENCODING_PCM_24BIT
                || audioFormat == C.ENCODING_PCM_32BIT) {
            sourceEncoding = audioFormat;
        } else {
            throw new IllegalArgumentException("Unsupported PCM encoding: " + audioFormat);
        }
        /*boolean directAacMode = passthroughEnabled &&
                (sourceEncoding == C.ENCODING_AAC_LC
                        || sourceEncoding == C.ENCODING_AAC_HE_V1
                        || sourceEncoding == C.ENCODING_AAC_HE_V2);*/

        int channelConfig;
        /*dualMonoResampling = new DualMonoResampling(
                channelCount, dualMonoType, sourceEncoding);
        if (dualMonoResampling.shouldResampling()) {
            channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        } else {
            channelConfig = decideChannelConfig(channelCount, passthrough, mAudioWrapper.mime);
        }*/
        channelConfig = decideChannelConfig(channelCount, mPassthroughEnabled, mime);

        // 2 bytes per 16-bit sample * number of channels.
        int bufferSize = 0;
        int targetEncoding = mPassthroughEnabled ? sourceEncoding : C.ENCODING_PCM_16BIT;
        int specifiedBufferSize = 0;
        if (TextUtils.equals(mime, MimeTypes.AUDIO_AAC) && audioBitrate != 0) {
            specifiedBufferSize =
                    (int) (PASSTHROUGH_BUFFER_DURATION_US * (audioBitrate / 8) / C.MICROS_PER_SECOND);
        }
        if (specifiedBufferSize != 0) {
            bufferSize = specifiedBufferSize;
        } else if (mPassthroughEnabled) {
            // account. [Internal: b/25181305]
            if (targetEncoding == C.ENCODING_AC3
                    || targetEncoding == C.ENCODING_E_AC3
                    || targetEncoding == C.ENCODING_E_AC3_JOC) {
                // AC-3 allows bitrates up to 1024 kbit/s.
                bufferSize =
                        (int) (PASSTHROUGH_BUFFER_DURATION_US * 128 * 1024 / C.MICROS_PER_SECOND);
            } else /* (targetEncoding == C.ENCODING_DTS
                            || targetEncoding == C.ENCODING_DTS_HD) */ {
                // DTS allows an 'open' bitrate,
                // but we assume the maximum listed value: 1536 kbit/s.
                bufferSize =
                        (int) (PASSTHROUGH_BUFFER_DURATION_US * 192 * 1024 / C.MICROS_PER_SECOND);
            }
        } else {
            int pcmFrameSize = 2 * channelCount;
            int minBufferSize =
                    AudioTrack.getMinBufferSize(
                            sampleRateInHz, channelConfig, targetEncoding);
            Assertions.checkState(minBufferSize != AudioTrack.ERROR_BAD_VALUE);
            int multipliedBufferSize = minBufferSize * BUFFER_MULTIPLICATION_FACTOR;
            int minAppBufferSize =
                    (int) ((MIN_BUFFER_DURATION_US * sampleRateInHz) / C.MICROS_PER_SECOND) * pcmFrameSize;
            int maxAppBufferSize =
                    (int) ((MAX_BUFFER_DURATION_US * sampleRateInHz) / C.MICROS_PER_SECOND) * pcmFrameSize;
            maxAppBufferSize = Math.max(minBufferSize, maxAppBufferSize);
            bufferSize = multipliedBufferSize < minAppBufferSize
                    ? minAppBufferSize
                    : multipliedBufferSize > maxAppBufferSize
                    ? maxAppBufferSize : multipliedBufferSize;
        }
        /*long framesToDurationUs =
                ((bufferSize / pcmFrameSize) * C.MICROS_PER_SECOND) / sampleRateInHz;
        long bufferSizeUs = passthrough ? C.UNKNOWN_TIME_US : framesToDurationUs;*/

        // sampleRateInHz: 48000 channelConfig: 4 targetEncoding: 10 bufferSize: 2254
        // channelCount: 1 audioFormat: 2
        Log.d(TAG, "createAudioTrack()" +
                " sampleRateInHz: " + sampleRateInHz +
                " channelConfig: " + channelConfig +
                " targetEncoding: " + targetEncoding +
                " bufferSize: " + bufferSize +
                " channelCount: " + channelCount +
                " audioFormat: " + audioFormat);

        // create AudioTrack
        mAudioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRateInHz, channelConfig, targetEncoding, bufferSize,
                AudioTrack.MODE_STREAM);

        if (mAudioTrack != null) {
            if (mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
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
                mAudioTrack.play();
            } else {
                MediaUtils.releaseAudioTrack(mAudioTrack);
                mAudioTrack = null;
            }
        }
    }

    // first invoked
    public boolean isPassthroughEnabled() {
        mPassthroughEnabled = false;
        if (mMediaFormat == null) {
            return false;
        }

        mime = mMediaFormat.getString(android.media.MediaFormat.KEY_MIME);
        mFormat = MediaFormatUtil.createMediaFormat(mMediaFormat);
        if (allowPassthrough(mime, mFormat, mMediaFormat)) {
            mPassthroughEnabled = true;
            return true;
        }

        return false;
    }

    private boolean allowPassthrough(
            String mime, ExoMediaFormat format, android.media.MediaFormat mediaFormat) {
        int aacProfile = 0;
        if (TextUtils.equals(mime, MimeTypes.AUDIO_AAC)) {
            if (mediaFormat.containsKey(
                    android.media.MediaFormat.KEY_AAC_PROFILE)) {
                aacProfile = mediaFormat.getInteger(
                        android.media.MediaFormat.KEY_AAC_PROFILE);
            }
        }

        AudioCapabilities audioCapabilities = DEFAULT_AUDIO_CAPABILITIES;
        boolean isPassthroughSupported = audioCapabilities != null
                && audioCapabilities.supportsEncoding(getEncodingForMimeType(mime, aacProfile));
        String dualMonoType = null;
        if (format != null) {
            dualMonoType = format.getFrameworkMediaFormatV16().getString(EXO_KEY_DUAL_MONO_TYPE);
        }
        boolean contentSupportPassthrough =
                isPassthroughSupported
                        && dualMonoType == null
                        && !RendererConfiguration.getInstance().isSpeedShiftPlay();
        RendererConfiguration conf = RendererConfiguration.getInstance();
        conf.setAudioContentSupportPassthrough(contentSupportPassthrough);
        return contentSupportPassthrough
                /*&& !BluetoothConfiguration.getInstance().isBluetoothConnect()*/
                && !conf.isAacAudioDisablePassthrough()
                && !conf.isSpeedConvDisablePassthrough();
    }

    private static int getEncodingForMimeType(String mimeType, int aacProfile) {
        switch (mimeType) {
            case MimeTypes.AUDIO_AC3:
                return C.ENCODING_AC3;
            case MimeTypes.AUDIO_E_AC3:
                return C.ENCODING_E_AC3;
            case MimeTypes.AUDIO_E_AC3_JOC:
                return C.ENCODING_E_AC3_JOC;
            case MimeTypes.AUDIO_AC4:
            case MimeTypes.AUDIO_AC4_JOC:
                return C.ENCODING_AC4;
            case MimeTypes.AUDIO_DTS:
                return C.ENCODING_DTS;
            case MimeTypes.AUDIO_DTS_HD:
                return C.ENCODING_DTS_HD;
            case MimeTypes.AUDIO_AAC:
                if (aacProfile == MediaCodecInfo.CodecProfileLevel.AACObjectHE) {
                    Log.d(TAG, "EncodingForMimeType= ENCODING_AAC_HE_V1");
                    return C.ENCODING_AAC_HE_V1;
                } else if (aacProfile == MediaCodecInfo.CodecProfileLevel.AACObjectHE_PS) {
                    Log.d(TAG, "EncodingForMimeType= ENCODING_AAC_HE_V2");
                    return C.ENCODING_AAC_HE_V2;
                } else if (aacProfile == MediaCodecInfo.CodecProfileLevel.AACObjectLC) {
                    Log.d(TAG, "EncodingForMimeType= ENCODING_AAC_LC");
                    return C.ENCODING_AAC_LC;
                }
                Log.d(TAG, "AAC Profile invalid EncodingForMimeType= ENCODING_AAC_LC");
                return C.ENCODING_AAC_LC;
            default:
                return C.ENCODING_INVALID;
        }
    }

    private boolean isValidAacProfile(int aacProfile) {
        return aacProfile == MediaCodecInfo.CodecProfileLevel.AACObjectHE
                || aacProfile == MediaCodecInfo.CodecProfileLevel.AACObjectHE_PS
                || aacProfile == MediaCodecInfo.CodecProfileLevel.AACObjectLC;
    }

    private int decideChannelConfig(
            final int channelCount, final boolean passthrough, String mimeType) {
        int channelConfig;
        switch (channelCount) {
            case 1:
                channelConfig = AudioFormat.CHANNEL_OUT_MONO;
                break;
            case 2:
                channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
                break;
            case 3:
                channelConfig = AudioFormat.CHANNEL_OUT_STEREO
                        | AudioFormat.CHANNEL_OUT_FRONT_CENTER;
                break;
            case 4:
                channelConfig = AudioFormat.CHANNEL_OUT_QUAD;
                break;
            case 5:
                if (mimeType.equals(MimeTypes.AUDIO_AC3)
                        || mimeType.equals(MimeTypes.AUDIO_E_AC3)
                        || mimeType.equals(MimeTypes.AUDIO_AC4)
                        || mimeType.equals(MimeTypes.AUDIO_E_AC3_JOC)
                        || mimeType.equals(MimeTypes.AUDIO_AC4_JOC)) {
                    channelConfig = AudioFormat.CHANNEL_OUT_5POINT1;
                } else {
                    channelConfig = AudioFormat.CHANNEL_OUT_QUAD
                            | AudioFormat.CHANNEL_OUT_FRONT_CENTER;
                }
                break;
            case 6:
                channelConfig = AudioFormat.CHANNEL_OUT_5POINT1;
                break;
            case 7:
                channelConfig = AudioFormat.CHANNEL_OUT_5POINT1
                        | AudioFormat.CHANNEL_OUT_BACK_CENTER;
                break;
            case 8:
                channelConfig = C.CHANNEL_OUT_7POINT1_SURROUND;
                break;
            default:
                throw new IllegalArgumentException("Unsupported channel count: " + channelCount);
        }
        // Workaround for overly strict channel configuration checks on nVidia Shield.
        if (Util.SDK_INT <= 23 && "foster".equals(Util.DEVICE)
                && "NVIDIA".equals(Util.MANUFACTURER)) {
            switch (channelCount) {
                case 7:
                    channelConfig = C.CHANNEL_OUT_7POINT1_SURROUND;
                    break;
                case 3:
                case 5:
                    channelConfig = AudioFormat.CHANNEL_OUT_5POINT1;
                    break;
                default:
                    break;
            }
        }

        // Workaround for Nexus Player not reporting support for mono passthrough.
        // (See [Internal: b/34268671].)
        if (Util.SDK_INT <= 25 && "fugu".equals(Util.DEVICE)
                && passthrough && channelCount == 1) {
            channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
        }
        return channelConfig;
    }

}

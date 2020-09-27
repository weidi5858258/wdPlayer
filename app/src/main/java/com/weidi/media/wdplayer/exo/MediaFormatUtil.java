/*
 * Copyright 2016 - 2017 Sony Corporation
 */
package com.weidi.media.wdplayer.exo;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class MediaFormatUtil {

    @SuppressLint("InlinedApi")
    public static ExoMediaFormat createMediaFormat(android.media.MediaFormat format) {
        Assertions.checkNotNull(format);
        String mimeType = format.getString(android.media.MediaFormat.KEY_MIME);
        String language = getOptionalStringV16(format, android.media.MediaFormat.KEY_LANGUAGE);
        int maxInputSize = getOptionalIntegerV16(format,
                android.media.MediaFormat.KEY_MAX_INPUT_SIZE);
        int width = getOptionalIntegerV16(format, android.media.MediaFormat.KEY_WIDTH);
        int height = getOptionalIntegerV16(format, android.media.MediaFormat.KEY_HEIGHT);
        int rotationDegrees = getOptionalIntegerV16(format, "rotation-degrees");
        int channelCount = getOptionalIntegerV16(format,
                android.media.MediaFormat.KEY_CHANNEL_COUNT);
        int sampleRate = getOptionalIntegerV16(format, android.media.MediaFormat.KEY_SAMPLE_RATE);
        int encoderDelay = getOptionalIntegerV16(format, "encoder-delay");
        int encoderPadding = getOptionalIntegerV16(format, "encoder-padding");
        int framerate = getOptionalIntegerV16(format, android.media.MediaFormat.KEY_FRAME_RATE);
        ArrayList<byte[]> initializationData = new ArrayList<>();
        for (int i = 0; format.containsKey("csd-" + i); i++) {
            ByteBuffer buffer = format.getByteBuffer("csd-" + i);
            byte[] data = new byte[buffer.limit()];
            buffer.get(data);
            initializationData.add(data);
            buffer.flip();
        }
        long formatDurationUs = format.containsKey(android.media.MediaFormat.KEY_DURATION)
                ? format.getLong(android.media.MediaFormat.KEY_DURATION)
                : C.UNKNOWN_TIME_US;
        int pcmEncoding = MimeTypes.AUDIO_RAW.equals(mimeType) ? C.ENCODING_PCM_16BIT
                : ExoMediaFormat.NO_VALUE;
        ExoMediaFormat exoMediaFormat = new ExoMediaFormat(null, mimeType, ExoMediaFormat.NO_VALUE,
                maxInputSize,
                formatDurationUs, width, height, rotationDegrees, ExoMediaFormat.NO_VALUE, channelCount,
                sampleRate,
                language, ExoMediaFormat.OFFSET_SAMPLE_RELATIVE, initializationData, false,
                ExoMediaFormat.NO_VALUE, ExoMediaFormat.NO_VALUE, pcmEncoding, encoderDelay,
                encoderPadding,
                null, ExoMediaFormat.NO_VALUE, framerate);
        exoMediaFormat.setFrameworkFormatV16(format);
        return exoMediaFormat;
    }

    @TargetApi(16)
    private static final String getOptionalStringV16(android.media.MediaFormat format, String key) {
        return format.containsKey(key) ? format.getString(key) : null;
    }

    @TargetApi(16)
    private static final int getOptionalIntegerV16(android.media.MediaFormat format, String key) {
        return format.containsKey(key) ? format.getInteger(key) : ExoMediaFormat.NO_VALUE;
    }

}

/*
 * Copyright 2018 - 2019 Sony Corporation
 */
package com.weidi.media.wdplayer.exo;

import static com.weidi.media.wdplayer.exo.C.MT5891;
import static com.weidi.media.wdplayer.exo.C.MT5895;
import static com.weidi.media.wdplayer.exo.C.getChipsetNumber;

public final class SpeedRangeCapabilities {
  private static final int CHIPSET_TRINITY = MT5891;
  private static final int DONTCARE = Integer.MAX_VALUE;
  private enum MatchedCode {
    NOT_FOUND,
    MATCHED_COMPLETELY,
    OVER_SPECIFIED,
  }

  private interface SpeedRangeInfo {
    default String getName() { return null; }
    default String getMime() { return null; }
    int getMaxSampleRate();
    int getChannelCount();
    int getMaxFrameRate();
    int getMaxBitrate();
    int getMaxPixels();
    float getLower();
    float getUpper();
    float getLimited();

    default MatchedCode findDecoder(String name, String mime
            , final int sampleRate, final int channel, final int frameRate, final int bitrate
            , final int pixels) {
      if (name.equalsIgnoreCase(getName()) && mime.equalsIgnoreCase(getMime())) {
        return
          (sampleRate <= getMaxSampleRate() && channel <= getChannelCount() &&
          frameRate <= getMaxFrameRate() && bitrate <= getMaxBitrate() && pixels <= getMaxPixels())?
            MatchedCode.MATCHED_COMPLETELY: MatchedCode.OVER_SPECIFIED;
      }
      return MatchedCode.NOT_FOUND;
    }
  }

  private enum SpeedRangeInfoVideo implements SpeedRangeInfo {
//    MTK_AVC("OMX.MTK.VIDEO.DECODER.AVC",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    MTK_MPEG4("OMX.MTK.VIDEO.DECODER.MPEG4",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    MTK_H263("OMX.MTK.VIDEO.DECODER.H263",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    MTK_VP8("OMX.MTK.VIDEO.DECODER.VP8",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    MTK_VP9("OMX.MTK.VIDEO.DECODER.VP9",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    MTK_HEVC("OMX.MTK.VIDEO.DECODER.HEVC",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    DOLBY_VISION_DVHE_DTR("OMX.dolby.vision.dvhe.dtr.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    DOLBY_VISION_DVHE_STN("OMX.dolby.vision.dvhe.stn.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    DOLBY_VISION_DVHE_ST("OMX.dolby.vision.dvhe.st.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    DOLBY_VISION_DVAV_SE("OMX.dolby.vision.dvav.se.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    MTK_MPEG2("OMX.MTK.VIDEO.DECODER.MPEG2",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    MTK_WMV("OMX.MTK.VIDEO.DECODER.WMV",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    MTK_MJPEG("OMX.MTK.VIDEO.DECODER.MJPEG",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    MTK_VP6("OMX.MTK.VIDEO.DECODER.VP6",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    GOOGLE_MPEG2("OMX.google.mpeg2.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    GOOGLE_MPEG4("OMX.google.mpeg4.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    GOOGLE_H263("OMX.google.h263.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    GOOGLE_H264("OMX.google.h264.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    GOOGLE_HEVC("OMX.google.hevc.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    GOOGLE_VP8("OMX.google.vp8.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    GOOGLE_VP9("OMX.google.vp9.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
    VP8("OMX.MTK.VIDEO.DECODER.VP8", "video/x-vnd.on2.vp8",
      DONTCARE, DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 1.5f, 1.2f),
    SECURE_MPEG2("OMX.MTK.VIDEO.DECODER.MPEG2.secure", "video/mpeg2",
      DONTCARE, DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 1.25f, 1.2f),
    SECURE_AVC("OMX.MTK.VIDEO.DECODER.AVC.secure", "video/avc",
      DONTCARE, DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 1.25f, 1.2f);
    private final String mName;
    private final String mMime;
    private final int mMaxSampleRate;
    private final int mChannelCount;
    private final int mMaxFrameRate;
    private final int mMaxBitrate;
    private final int mMaxPixels;  // width * height
    private final float mLower;
    private final float mUpper;
    private final float mLimited;

    SpeedRangeInfoVideo(String name, String mime,
                        final int sampleRate, final int channel, final int frameRate,
                        final int bitrate, final int pixels,
                        final float lower, final float upper, final float limited) {
      mName = name;
      mMime = mime;
      mMaxSampleRate = sampleRate;
      mChannelCount = channel;
      mMaxFrameRate = frameRate;
      mMaxBitrate = bitrate;
      mMaxPixels = pixels;
      mLower = lower;
      mUpper = upper;
      mLimited = limited;
    }

    @Override
    public String getName() {
      return mName;
    }
    @Override
    public String getMime() {
      return mMime;
    }
    @Override
    public int getMaxSampleRate() {
      return mMaxSampleRate;
    }
    @Override
    public int getChannelCount() {
      return mChannelCount;
    }
    @Override
    public int getMaxFrameRate() {
      return mMaxFrameRate;
    }
    @Override
    public int getMaxBitrate() {
      return mMaxBitrate;
    }
    @Override
    public int getMaxPixels() {
      return mMaxPixels;
    }
    @Override
    public float getLower() {
      return mLower;
    }
    @Override
    public float getUpper() {
      return mUpper;
    }
    @Override
    public float getLimited() {
      return mLimited;
    }
  }
  /* example :exclude Mime video, but we don't have such type
  private enum SpeedRangeInfoVideoExcludeMime implements SpeedRangeInfo {
    VEX_MIME("OMX.MTK.VIDEO.DECODER.VP8",
      DONTCARE, DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 1.5f);

    private final String mName;
    private final int mMaxSampleRate;
    private final int mChannelCount;
    private final int mMaxFrameRate;
    private final int mMaxBitrate;
    private final int mMaxPixels;  // width * height
    private final float mLower;
    private final float mUpper;

    SpeedRangeInfoVideoExcludeMime(String name,
                        final int sampleRate, final int channel, final int frameRate,
                        final int bitrate, final int pixels,
                        final float lower, final float upper) {
      mName = name;
      mMaxSampleRate = sampleRate;
      mChannelCount = channel;
      mMaxFrameRate = frameRate;
      mMaxBitrate = bitrate;
      mMaxPixels = pixels;
      mLower = lower;
      mUpper = upper;
      mLimited = limited;
    }

    @Override
    public String getName() {
      return mName;
    }
    @Override
    public int getMaxSampleRate() {
      return mMaxSampleRate;
    }
    @Override
    public int getChannelCount() {
      return mChannelCount;
    }
    @Override
    public int getMaxFrameRate() {
      return mMaxFrameRate;
    }
    @Override
    public int getMaxBitrate() {
      return mMaxBitrate;
    }
    @Override
    public int getMaxPixels() {
      return mMaxPixels;
    }
    @Override
    public float getLower() {
      return mLower;
    }
    @Override
    public float getUpper() {
      return mUpper;
    }
    @Override
    public boolean findDecoder(String name, String mime,
                                     final int sampleRate, final int channel,
                                     final int frameRate, final int bitrate, final int pixels) {
      return name.equalsIgnoreCase(getName())
               && sampleRate < getMaxSampleRate() && channel < getChannelCount()
               && frameRate < getMaxFrameRate() && bitrate < getMaxBitrate() && pixels < getMaxPixels();
    }
  }*/
  /* example :exclude Name and Mime video, but we don't have such type
  private enum SpeedRangeInfoVideoLazy implements SpeedRangeInfo {
    VEX_XXX(DONTCARE, DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 1.5f);

    private final int mMaxSampleRate;
    private final int mChannelCount;
    private final int mMaxFrameRate;
    private final int mMaxBitrate;
    private final int mMaxPixels;  // width * height
    private final float mLower;
    private final float mUpper;

    SpeedRangeInfoVideoLazy(
                        final int sampleRate, final int channel, final int frameRate,
                        final int bitrate, final int pixels,
                        final float lower, final float upper) {
      mMaxSampleRate = sampleRate;
      mChannelCount = channel;
      mMaxFrameRate = frameRate;
      mMaxBitrate = bitrate;
      mMaxPixels = pixels;
      mLower = lower;
      mUpper = upper;
    }

    @Override
    public int getMaxSampleRate() {
      return mMaxSampleRate;
    }
    @Override
    public int getChannelCount() {
      return mChannelCount;
    }
    @Override
    public int getMaxFrameRate() {
      return mMaxFrameRate;
    }
    @Override
    public int getMaxBitrate() {
      return mMaxBitrate;
    }
    @Override
    public int getMaxPixels() {
      return mMaxPixels;
    }
    @Override
    public float getLower() {
      return mLower;
    }
    @Override
    public float getUpper() {
      return mUpper;
    }
    @Override
    public boolean findDecoder(String name, String mime,
                                     final int sampleRate, final int channel,
                                     final int frameRate, final int bitrate, final int pixels) {
      return sampleRate < getMaxSampleRate() && channel < getChannelCount()
             && frameRate < getMaxFrameRate() && bitrate < getMaxBitrate() && pixels < getMaxPixels();
    }
  }*/
  private enum SpeedRangeInfoVideo5895 implements SpeedRangeInfo {
//    MTK_AVC("OMX.MTK.VIDEO.DECODER.AVC",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    MTK_MPEG4("OMX.MTK.VIDEO.DECODER.MPEG4",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    MTK_H263("OMX.MTK.VIDEO.DECODER.H263",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    MTK_VP8("OMX.MTK.VIDEO.DECODER.VP8",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    MTK_VP9("OMX.MTK.VIDEO.DECODER.VP9",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    MTK_HEVC("OMX.MTK.VIDEO.DECODER.HEVC",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    DOLBY_VISION_DVHE_DTR("OMX.dolby.vision.dvhe.dtr.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    DOLBY_VISION_DVHE_STN("OMX.dolby.vision.dvhe.stn.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    DOLBY_VISION_DVHE_ST("OMX.dolby.vision.dvhe.st.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    DOLBY_VISION_DVAV_SE("OMX.dolby.vision.dvav.se.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    MTK_MPEG2("OMX.MTK.VIDEO.DECODER.MPEG2",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    MTK_WMV("OMX.MTK.VIDEO.DECODER.WMV",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    MTK_MJPEG("OMX.MTK.VIDEO.DECODER.MJPEG",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    MTK_VP6("OMX.MTK.VIDEO.DECODER.VP6",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    GOOGLE_MPEG2("OMX.google.mpeg2.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    GOOGLE_MPEG4("OMX.google.mpeg4.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    GOOGLE_H263("OMX.google.h263.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    GOOGLE_H264("OMX.google.h264.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    GOOGLE_HEVC("OMX.google.hevc.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    GOOGLE_VP8("OMX.google.vp8.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
//    GOOGLE_VP9("OMX.google.vp9.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.0f, 3.0f),
    VP8("OMX.MTK.VIDEO.DECODER.VP8", "video/x-vnd.on2.vp8",
      DONTCARE, DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 1.5f, 1.2f),
    SECURE_MPEG2("OMX.MTK.VIDEO.DECODER.MPEG2.secure", "video/mpeg2",
      DONTCARE, DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 1.5f, 1.2f),
    SECURE_AVC("OMX.MTK.VIDEO.DECODER.AVC.secure", "video/avc",
      DONTCARE, DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 1.5f, 1.2f);
    private final String mName;
    private final String mMime;
    private final int mMaxSampleRate;
    private final int mChannelCount;
    private final int mMaxFrameRate;
    private final int mMaxBitrate;
    private final int mMaxPixels;  // width * height
    private final float mLower;
    private final float mUpper;
    private final float mLimited;

    SpeedRangeInfoVideo5895(String name, String mime,
                        final int sampleRate, final int channel, final int frameRate,
                        final int bitrate, final int pixels,
                        final float lower, final float upper, final float limited) {
      mName = name;
      mMime = mime;
      mMaxSampleRate = sampleRate;
      mChannelCount = channel;
      mMaxFrameRate = frameRate;
      mMaxBitrate = bitrate;
      mMaxPixels = pixels;
      mLower = lower;
      mUpper = upper;
      mLimited = limited;
    }

    @Override
    public String getName() {
      return mName;
    }
    @Override
    public String getMime() {
      return mMime;
    }
    @Override
    public int getMaxSampleRate() {
      return mMaxSampleRate;
    }
    @Override
    public int getChannelCount() {
      return mChannelCount;
    }
    @Override
    public int getMaxFrameRate() {
      return mMaxFrameRate;
    }
    @Override
    public int getMaxBitrate() {
      return mMaxBitrate;
    }
    @Override
    public int getMaxPixels() {
      return mMaxPixels;
    }
    @Override
    public float getLower() {
      return mLower;
    }
    @Override
    public float getUpper() {
      return mUpper;
    }
    @Override
    public float getLimited() {
      return mLimited;
    }
  }

  private enum SpeedRangeInfoAudio implements SpeedRangeInfo {
//    RAW_AAC("OMX.google.raw.decoder", "audio/mp4a-latm",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.50f, 2.0f),
//    RAW_ALL("OMX.google.raw.decoder", DEFAULT_NAME,
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.25f, 2.0f),
//    GOOGLE_MP3("OMX.google.mp3.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 2.0f),
//    GOOGLE_AMRNB("OMX.google.amrnb.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 2.0f),
//    GOOGLE_AMRWB("OMX.google.amrwb.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 2.0f),
//    GOOGLE_AAC("OMX.google.aac.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 2.0f),
//    GOOGLE_G711_ALAW("OMX.google.g711.alaw.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 2.0f),
//    GOOGLE_G711_MLAW("OMX.google.g711.mlaw.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 2.0f),
//    GOOGLE_VORBIS("OMX.google.vorbis.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 2.0f),
//    GOOGLE_OPUS("OMX.google.opus.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 2.0f),
//    GOOGLE_RAW("OMX.google.raw.decoder",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 2.0f),
//    MTK_DSPAC3("OMX.MTK.AUDIO.DECODER.DSPAC3",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 2.0f),
//    MTK_DSPEAC3("OMX.MTK.AUDIO.DECODER.DSPEAC3",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 2.0f),
//    MTK_DSPAAC("OMX.MTK.AUDIO.DECODER.DSPAAC",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 2.0f),
//    MTK_DSPWMA("OMX.MTK.AUDIO.DECODER.DSPWMA",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 2.0f),
//    MTK_DSPWMAPRO("OMX.MTK.AUDIO.DECODER.DSPWMAPRO",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 2.0f),
//    MTK_DSPDTS("OMX.MTK.AUDIO.DECODER.DSPDTS",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 2.0f),
//    MTK_DSPDTSHD("OMX.MTK.AUDIO.DECODER.DSPDTSHD",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 2.0f),
//    MTK_DSPADPCMMS("OMX.MTK.AUDIO.DECODER.DSPADPCMMS",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 2.0f),
//    MTK_DSPADPCMIMA("OMX.MTK.AUDIO.DECODER.DSPADPCMIMA",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 2.0f),
//    MTK_DSPLPCMALAW("OMX.MTK.AUDIO.DECODER.DSPLPCMALAW",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 2.0f),
//    MTK_DSPLPCMMLAW("OMX.MTK.AUDIO.DECODER.DSPLPCMMLAW",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 2.0f),
//    MTK_DSPPCM("OMX.MTK.AUDIO.DECODER.DSPPCM",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 2.0f),
//    MTK_DSPMP1("OMX.MTK.AUDIO.DECODER.DSPMP1",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 2.0f),
//    MTK_DSPMP2("OMX.MTK.AUDIO.DECODER.DSPMP2",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 2.0f),
//    MTK_APE("OMX.MTK.AUDIO.DECODER.APE",
//            DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 2.0f),
    RAW_EAC3_JOC("OMX.google.raw.decoder", "audio/eac3-joc",
      DONTCARE, DONTCARE, DONTCARE, DONTCARE, DONTCARE, 1.0f, 1.0f, 1.0f),
    RAW_AC4_JOC("OMX.google.raw.decoder", "audio/ac4-joc",
      DONTCARE, DONTCARE, DONTCARE, DONTCARE, DONTCARE, 1.0f, 1.0f, 1.0f),
    RAW_AC4("OMX.google.raw.decoder", "audio/ac4",
      DONTCARE, DONTCARE, DONTCARE, DONTCARE, DONTCARE, 1.0f, 1.0f, 1.0f);

    private final String mName;
    private final String mMime;
    private final int mMaxSampleRate;
    private final int mChannelCount;
    private final int mMaxFrameRate;
    private final int mMaxBitrate;
    private final int mMaxPixels;  // width * height
    private final float mLower;
    private final float mUpper;
    private final float mLimited;

    SpeedRangeInfoAudio(String name, String mime,
                        final int sampleRate, final int channel, final int frameRate,
                        final int bitrate, final int pixels,
                        final float lower, final float upper, final float limited) {
      mName = name;
      mMime = mime;
      mMaxSampleRate = sampleRate;
      mChannelCount = channel;
      mMaxFrameRate = frameRate;
      mMaxBitrate = bitrate;
      mMaxPixels = pixels;
      mLower = lower;
      mUpper = upper;
      mLimited = limited;
    }

    @Override
    public String getName() {
      return mName;
    }
    @Override
    public String getMime() {
      return mMime;
    }
    @Override
    public int getMaxSampleRate() {
      return mMaxSampleRate;
    }
    @Override
    public int getChannelCount() {
      return mChannelCount;
    }
    @Override
    public int getMaxFrameRate() {
      return mMaxFrameRate;
    }
    @Override
    public int getMaxBitrate() {
      return mMaxBitrate;
    }
    @Override
    public int getMaxPixels() {
      return mMaxPixels;
    }
    @Override
    public float getLower() {
      return mLower;
    }
    @Override
    public float getUpper() {
      return mUpper;
    }
    @Override
    public float getLimited() {
      return mLimited;
    }
  }

  private enum SpeedRangeInfoAudio5891 implements SpeedRangeInfo {
    RAW_AC3("OMX.google.raw.decoder", "audio/ac3",
      DONTCARE, DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 1.0f, 1.0f),
    RAW_EAC3("OMX.google.raw.decoder", "audio/eac3",
      DONTCARE, DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 1.0f, 1.0f),
    RAW_DTS("OMX.google.raw.decoder", "audio/vnd.dts",
      DONTCARE, DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 1.0f, 1.0f),
    RAW_MPEG_AUDIO11("OMX.MTK.AUDIO.DECODER.DSPMP1", "audio/mpeg-L1",
      DONTCARE, DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 1.0f, 1.0f),
    RAW_MPEG_AUDIO22("OMX.MTK.AUDIO.DECODER.DSPMP2", "audio/mpeg-L2",
      DONTCARE, DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 1.0f, 1.0f),
    RAW_WMA("OMX.MTK.AUDIO.DECODER.DSPWMA", "audio/x-ms-wma",
      DONTCARE, DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 1.0f, 1.0f),
    RAW_EAC3_JOC("OMX.google.raw.decoder", "audio/eac3-joc",
      DONTCARE, DONTCARE, DONTCARE, DONTCARE, DONTCARE, 1.0f, 1.0f, 1.0f),
    RAW_AC4_JOC("OMX.google.raw.decoder", "audio/ac4-joc",
      DONTCARE, DONTCARE, DONTCARE, DONTCARE, DONTCARE, 1.0f, 1.0f, 1.0f),
    RAW_AC4("OMX.google.raw.decoder", "audio/ac4",
      DONTCARE, DONTCARE, DONTCARE, DONTCARE, DONTCARE, 1.0f, 1.0f, 1.0f);

    private final String mName;
    private final String mMime;
    private final int mMaxSampleRate;
    private final int mChannelCount;
    private final int mMaxFrameRate;
    private final int mMaxBitrate;
    private final int mMaxPixels;  // width * height
    private final float mLower;
    private final float mUpper;
    private final float mLimited;

    SpeedRangeInfoAudio5891(String name, String mime,
                        final int sampleRate, final int channel, final int frameRate,
                        final int bitrate, final int pixels,
                        final float lower, final float upper, final float limited) {
      mName = name;
      mMime = mime;
      mMaxSampleRate = sampleRate;
      mChannelCount = channel;
      mMaxFrameRate = frameRate;
      mMaxBitrate = bitrate;
      mMaxPixels = pixels;
      mLower = lower;
      mUpper = upper;
      mLimited = limited;
    }

    @Override
    public String getName() {
      return mName;
    }
    @Override
    public String getMime() {
      return mMime;
    }
    @Override
    public int getMaxSampleRate() {
      return mMaxSampleRate;
    }
    @Override
    public int getChannelCount() {
      return mChannelCount;
    }
    @Override
    public int getMaxFrameRate() {
      return mMaxFrameRate;
    }
    @Override
    public int getMaxBitrate() {
      return mMaxBitrate;
    }
    @Override
    public int getMaxPixels() {
      return mMaxPixels;
    }
    @Override
    public float getLower() {
      return mLower;
    }
    @Override
    public float getUpper() {
      return mUpper;
    }
    @Override
    public float getLimited() {
      return mLimited;
    }
  }
  /* example :exclude Mime audio, but we don't have such type
  private enum SpeedRangeInfoAudioExcludeMime implements SpeedRangeInfo {
    AEX_MIME("OMX.MTK.AUDIO.DECODER.VP8",
      DONTCARE, DONTCARE, DONTCARE, DONTCARE, DONTCARE, 0.5f, 1.5f);

    private final String mName;
    private final int mMaxSampleRate;
    private final int mChannelCount;
    private final int mMaxFrameRate;
    private final int mMaxBitrate;
    private final int mMaxPixels;  // width * height
    private final float mLower;
    private final float mUpper;

    SpeedRangeInfoAudioExcludeMime(String name,
                        final int sampleRate, final int channel, final int frameRate,
                        final int bitrate, final int pixels,
                        final float lower, final float upper) {
      mName = name;
      mMaxSampleRate = sampleRate;
      mChannelCount = channel;
      mMaxFrameRate = frameRate;
      mMaxBitrate = bitrate;
      mMaxPixels = pixels;
      mLower = lower;
      mUpper = upper;
    }

    @Override
    public String getName() {
      return mName;
    }
    @Override
    public int getMaxSampleRate() {
      return mMaxSampleRate;
    }
    @Override
    public int getChannelCount() {
      return mChannelCount;
    }
    @Override
    public int getMaxFrameRate() {
      return mMaxFrameRate;
    }
    @Override
    public int getMaxBitrate() {
      return mMaxBitrate;
    }
    @Override
    public int getMaxPixels() {
      return mMaxPixels;
    }
    @Override
    public float getLower() {
      return mLower;
    }
    @Override
    public float getUpper() {
      return mUpper;
    }
    @Override
    public boolean findDecoder(String name, String mime,
                                     final int sampleRate, final int channel,
                                     final int frameRate, final int bitrate, final int pixels) {
      return name.equalsIgnoreCase(getName())
               && sampleRate < getMaxSampleRate() && channel < getChannelCount()
               && frameRate < getMaxFrameRate() && bitrate < getMaxBitrate() && pixels < getMaxPixels();
    }
  }*/
  // exclude Name and Mime audio,
  private enum SpeedRangeInfoAudioLazy implements SpeedRangeInfo {
    MAX_CHANNEL(DONTCARE, 8, DONTCARE, DONTCARE, DONTCARE, 0.5f, 2.0f, 1.5f);

    private final int mMaxSampleRate;
    private final int mChannelCount;
    private final int mMaxFrameRate;
    private final int mMaxBitrate;
    private final int mMaxPixels;  // width * height
    private final float mLower;
    private final float mUpper;
    private final float mLimited;

    SpeedRangeInfoAudioLazy(
                        final int sampleRate, final int channel, final int frameRate,
                        final int bitrate, final int pixels,
                        final float lower, final float upper, final float limited) {
      mMaxSampleRate = sampleRate;
      mChannelCount = channel;
      mMaxFrameRate = frameRate;
      mMaxBitrate = bitrate;
      mMaxPixels = pixels;
      mLower = lower;
      mUpper = upper;
      mLimited = limited;
    }

    @Override
    public int getMaxSampleRate() {
      return mMaxSampleRate;
    }
    @Override
    public int getChannelCount() {
      return mChannelCount;
    }
    @Override
    public int getMaxFrameRate() {
      return mMaxFrameRate;
    }
    @Override
    public int getMaxBitrate() {
      return mMaxBitrate;
    }
    @Override
    public int getMaxPixels() {
      return mMaxPixels;
    }
    @Override
    public float getLower() {
      return mLower;
    }
    @Override
    public float getUpper() {
      return mUpper;
    }
    @Override
    public float getLimited() {
      return mLimited;
    }
    @Override
    public MatchedCode findDecoder(String name, String mime, final int sampleRate
      , final int channel, final int frameRate, final int bitrate, final int pixels) {
      return
        (sampleRate <= getMaxSampleRate() && channel <= getChannelCount() &&
         frameRate <= getMaxFrameRate() && bitrate <= getMaxBitrate() && pixels <= getMaxPixels())?
          MatchedCode.MATCHED_COMPLETELY: MatchedCode.OVER_SPECIFIED;
    }
  }

  public static SpeedShiftRange.SpeedRange getDefaultRange(final boolean isVideo) {
    Log.i("getDefaultRange(isVideo=" + isVideo + ")");
    return isVideo? getDefaultSpeedRangeVideo(): getDefaultSpeedRangeAudio();
  }

  public static SpeedShiftRange.SpeedRange
  getRange(String name, String mime, final boolean isVideo, final int sampleRate, final int channel
    , final int frameRate, final int bitrate, final int pixels) {
    Log.d(name + ", " + mime + ", isVideo: " + isVideo + ", ch: " + channel
            + ", frameRate: " + frameRate + ", bitrate: " + bitrate + ", pixels: " + pixels);
    SpeedShiftRange.SpeedRange speedRange;
    final int chipsetNumber = getChipsetNumber();
    Log.d("chipsetNumber: " + chipsetNumber);
    if (isVideo) {
      if (MT5895 == chipsetNumber) {
        speedRange = getRangeInternal(SpeedRangeInfoVideo5895.class, null, null
          , name, mime, sampleRate, channel, frameRate, bitrate, pixels);
      } else {
        speedRange = getRangeInternal(SpeedRangeInfoVideo.class, null, null
          , name, mime, sampleRate, channel, frameRate, bitrate, pixels);
      }
      if (speedRange != null) {
        return speedRange;
      }
      return getDefaultSpeedRangeVideo();
    }
    // audio
    if (CHIPSET_TRINITY == chipsetNumber) {
      speedRange = getRangeInternal(SpeedRangeInfoAudio5891.class, null
        , SpeedRangeInfoAudioLazy.class
        , name, mime, sampleRate, channel, frameRate, bitrate, pixels);
    } else {
      speedRange = getRangeInternal(SpeedRangeInfoAudio.class, null
        , SpeedRangeInfoAudioLazy.class
        , name, mime, sampleRate, channel, frameRate, bitrate, pixels);
    }
    if (speedRange != null) {
      return speedRange;
    }
    return getDefaultSpeedRangeAudio();
  }

  private static <T1 extends Enum<T1> & SpeedRangeInfo, T2 extends Enum<T2> & SpeedRangeInfo
                , T3 extends Enum<T3> & SpeedRangeInfo>
  SpeedShiftRange.SpeedRange
  getRangeInternal(Class<T1> complete, Class<T2> excludeMime, Class<T3> lazy
    , String name, String mime, final int sampleRate, final int channel
    , final int frameRate, final int bitrate, final int pixels) {
    SpeedShiftRange.SpeedRange speedRange;

    if (complete != null) {
      speedRange = getRangeInternal(complete, name, mime, sampleRate, channel, frameRate
        , bitrate, pixels);
      if (speedRange != null) {
        return speedRange;
      }
    }

    if (excludeMime != null) {
      speedRange = getRangeInternal(excludeMime, name, mime, sampleRate, channel, frameRate
        , bitrate, pixels);
      if (speedRange != null) {
        return speedRange;
      }
    }

    if (lazy != null) {
      return getRangeInternal(lazy, name, mime, sampleRate, channel, frameRate, bitrate, pixels);
    }
    return null;
  }

  private static <T extends Enum<T> & SpeedRangeInfo> SpeedShiftRange.SpeedRange
  getRangeInternal(Class<T> obj
    , String name, String mime, final int sampleRate, final int channel
    , final int frameRate, final int bitrate, final int pixels) {
    for (T info : obj.getEnumConstants()) {
      final MatchedCode retCode = info.findDecoder(name, mime, sampleRate, channel, frameRate
        , bitrate, pixels);
      if (retCode != MatchedCode.NOT_FOUND) {
        final float lower = info.getLower();
        final float upper = retCode == MatchedCode.MATCHED_COMPLETELY ?
                              info.getUpper(): info.getLimited();
        Log.d("found " + name + ", " + mime + "=> " + retCode + ": " + lower + " - " + upper);
        return new SpeedShiftRange.SpeedRange(lower, upper);
      }
    }
    return null;
  }

  private static SpeedShiftRange.SpeedRange getDefaultSpeedRangeVideo() {
    return new SpeedShiftRange.SpeedRange(0.25f, 2.0f);
  }
  private static SpeedShiftRange.SpeedRange getDefaultSpeedRangeAudio() {
    return new SpeedShiftRange.SpeedRange(0.5f, 2.0f);
  }
}
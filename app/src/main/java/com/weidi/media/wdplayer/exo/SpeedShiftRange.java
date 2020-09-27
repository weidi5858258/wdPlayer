/*
 * Copyright 2018 Sony Corporation
 */
package com.weidi.media.wdplayer.exo;

public class SpeedShiftRange {
  public static class SpeedRange {
    private float mLower;
    private float mUpper;
    SpeedRange(final float lower, final float upper) {
      mLower = lower;
      mUpper = upper;
    }
    SpeedRange(final boolean isVideo) {
      if (isVideo) {
        mLower = DEFAULT_VIDEO_LOWER;
        mUpper = DEFAULT_VIDEO_UPPER;
      } else {
        mLower = DEFAULT_AUDIO_LOWER;
        mUpper = DEFAULT_AUDIO_UPPER;
      }
    }
    public float getLower() { return mLower; }
    public float getUpper() { return mUpper; }
  }
  private static final int TYPE_VIDEO = 0;
  private static final int TYPE_AUDIO = 1;

  private static final float DEFAULT_AUDIO_LOWER = 0.5f;
  private static final float DEFAULT_AUDIO_UPPER = 1.5f;
  private static final float DEFAULT_VIDEO_LOWER = 0.0f;
  private static final float DEFAULT_VIDEO_UPPER = 3.0f;

  // Range State Index
  private static final int IN_RANGE = 0;
  private static final int OUT_RANGE = 1;
  private static final int NO_TRACK = 2;
  // Next State

  private class SpeedRangeState {
    private final String mName;
    private boolean mValid;
    private boolean mEnabled;
    private final SpeedRange mSpeedRange;
    private final SpeedRange mDefaultSpeedRange;
    private SpeedRangeState(String name, final float lower, final float upper) {
      mName = name;
      mDefaultSpeedRange = new SpeedRange(lower, upper);
      mSpeedRange = new SpeedRange(lower, upper);
    }
    private void update(final float lower, final float upper) {
      mSpeedRange.mLower = lower;
      mSpeedRange.mUpper = upper;
    }
    private void activate(final SpeedRange newRange) {
      mValid = true;
      if (newRange == null) {
        Log.w("given SpeedRange is null!");
        return;
      }
      update(newRange.mLower, newRange.mUpper);
    }
    private boolean isInRange(final float speed) {
      if (speed == 0.0f) {
        return false;
      }
      return mSpeedRange.mLower <= speed && speed <= mSpeedRange.mUpper;
    }
    private boolean equals(final SpeedRange newRange) {
      if (newRange == null) {
        Log.w("given SpeedRange is null!");
        return false;
      }
      Log.w("current L:" + mSpeedRange.mLower + " R:" + mSpeedRange.mUpper + ", new L:" + newRange.mLower + " R:" + newRange.mUpper);
      return newRange.mLower == mSpeedRange.mLower && newRange.mUpper == mSpeedRange.mUpper;
    }
    private void reset() {
      mValid = false;
      update(mDefaultSpeedRange.mLower, mDefaultSpeedRange.mUpper);
    }
    private int getStateIndex(final float speed) {
      if (!mEnabled) {
        return NO_TRACK;
      }
      if (isInRange(speed)) {
        return IN_RANGE;
      }
      return OUT_RANGE;
    }
    private boolean isEnabledAndValid() {
      return mEnabled && mValid;
    }
  }
  private SpeedRangeState mAudioSpeedRangeState;
  private SpeedRangeState mVideoSpeedRangeState;

  public SpeedShiftRange() {
    mAudioSpeedRangeState = new SpeedRangeState("Audio", DEFAULT_AUDIO_LOWER, DEFAULT_AUDIO_UPPER);
    mVideoSpeedRangeState = new SpeedRangeState("Video", DEFAULT_VIDEO_LOWER, DEFAULT_VIDEO_UPPER);
  }
  public void reset() {
    Log.d("reset!");
    mAudioSpeedRangeState.reset();
    mVideoSpeedRangeState.reset();
  }
  public void update(final boolean isVideo, final SpeedRange newRange) {
    if (newRange == null) {
      Log.w("given SpeedRange is null!");
      return;
    }
    if (isVideo) {
      Log.d("Update Video: " + newRange.mLower +", " + newRange.mUpper);
      mVideoSpeedRangeState.activate(newRange);
    } else {
      Log.d("Update Audio: " + newRange.mLower +", " + newRange.mUpper);
      mAudioSpeedRangeState.activate(newRange);
    }
  }
  /**
   * A value that can be passed as the second argument to {@link #setSelectedTrack(int, int)} to
   * disable the renderer.
   */
  public static final int TRACK_DISABLED = -1;
  public void setSelectedTrack(final int rendererIndex, final int trackIndex) {
    Log.d("rendererIndex: " + rendererIndex + ", trackIndex: " + trackIndex);
    final boolean enabled = trackIndex != TRACK_DISABLED;
    if (rendererIndex == TYPE_VIDEO) {
      mVideoSpeedRangeState.mEnabled = enabled;
      Log.d("mEnabledVideo: " + enabled);
    } else if (rendererIndex == TYPE_AUDIO) {
      mAudioSpeedRangeState.mEnabled = enabled;
      Log.d("mEnabledAudio=" + enabled);
    }
  }
  public boolean isStateValid() {
    final boolean valid =
      (!mAudioSpeedRangeState.mEnabled || mAudioSpeedRangeState.mValid)
        && (!mVideoSpeedRangeState.mEnabled || mVideoSpeedRangeState.mValid);
    Log.d("audio_en=" + mAudioSpeedRangeState.mEnabled + " audio_vd=" + mAudioSpeedRangeState.mValid
      + " video_en=" + mVideoSpeedRangeState.mEnabled + " video_vd=" + mVideoSpeedRangeState.mValid
      + " -> returns " + valid);
    return valid;
  }
  public int getAudioSpeedRangeStateIndex(final float speed) {
    return mAudioSpeedRangeState.getStateIndex(speed);
  }
  public int getVideoSpeedRangeStateIndex(final float speed) {
    return mVideoSpeedRangeState.getStateIndex(speed);
  }
  public boolean isUpdateNeeded(final boolean isVideo, final SpeedRange speedRange) {
    final SpeedRangeState speedRangeState = isVideo? mVideoSpeedRangeState: mAudioSpeedRangeState;
    if (!speedRangeState.isEnabledAndValid()) {
      Log.d("need to update " + speedRangeState.mName + " due to invalid value");
      return true;
    }

    if (speedRangeState.equals(speedRange)) {
      Log.d("not need to update " + speedRangeState.mName);
      return false;
    }
    Log.d("need to update " + speedRangeState.mName);
    return true;
  }

  public SpeedRange getSpeedRange() {
    SpeedRange speedRange = new SpeedRange(0.0f, 0.0f);
    final RendererConfiguration rendererConfiguration = RendererConfiguration.getInstance();
    if (!rendererConfiguration.isDpcPlusLibAvailable()
            || rendererConfiguration.isHlsUri() || rendererConfiguration.is4k2kDownconvert()) {
      Log.i("Disable SpeedShift");
      return speedRange;
    }

    if (mVideoSpeedRangeState.mEnabled && mAudioSpeedRangeState.mEnabled) {
      // minimum range
      speedRange.mLower = Math.max(mVideoSpeedRangeState.mSpeedRange.mLower, mAudioSpeedRangeState.mSpeedRange.mLower);
      speedRange.mUpper = Math.min(mVideoSpeedRangeState.mSpeedRange.mUpper, mAudioSpeedRangeState.mSpeedRange.mUpper);
      // maximum range
      //speedRange.mLower = Math.min(mVideoSpeedRangeState.mSpeedRange.mLower, mAudioSpeedRangeState.mSpeedRange.mLower);
      //speedRange.mUpper = Math.max(mVideoSpeedRangeState.mSpeedRange.mUpper, mAudioSpeedRangeState.mSpeedRange.mUpper);
    } else if (mVideoSpeedRangeState.mEnabled && mAudioSpeedRangeState.mEnabled == false) {
      speedRange.mLower = mVideoSpeedRangeState.mSpeedRange.mLower;
      speedRange.mUpper = mVideoSpeedRangeState.mSpeedRange.mUpper;
    } else {
      speedRange.mLower = 0.0f;
      speedRange.mUpper = 0.0f;
    }

    Log.i("Enabled V:" + mVideoSpeedRangeState.mEnabled + " A:" + mAudioSpeedRangeState.mEnabled +
            ", Range L:" + speedRange.mLower + " U:" + speedRange.mUpper +
            ", Range (VL:" + mVideoSpeedRangeState.mSpeedRange.mLower + " VU:" + mVideoSpeedRangeState.mSpeedRange.mUpper +
            ", AL:" + mAudioSpeedRangeState.mSpeedRange.mLower + " AU:" + mAudioSpeedRangeState.mSpeedRange.mUpper);
    return speedRange;
  }
}
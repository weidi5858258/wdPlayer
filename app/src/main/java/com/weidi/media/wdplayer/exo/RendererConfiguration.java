/*
 * Copyright 2016 - 2017 Sony Corporation
 */
package com.weidi.media.wdplayer.exo;

public class RendererConfiguration {
    public enum playbackState {
        NOT_PAUSED,
        PAUSED
    }
    public enum tunnelingState {
        UNINIT,
        TUNNEL,
        NO_TUNNEL
    }

  private int mTunnelingAudioSessionId;
  // deletion tunnel mode switching on seeking workaround
  // private playbackState mExpectedState;
  private static RendererConfiguration mConfiguration;
  private boolean mForcedDisableTunneling;      // Content: Audio Channels > 2 and none pass through
  private boolean mContentSupportsTunneling;    // Content: Video not support Tunneling
  private boolean mAudioLessContent;             // Content: Audio Track not exist
  private boolean mIsBluetoothConnect;           // bluetooth audio doesn't support tunnel mode and direct mode;
  private boolean isAudioContentSupportPassthrough;
  private boolean previousAllowPassthrough;
  private boolean aacAudioDisablePassthrough;
  private boolean mSuspendAudioTrack;
  private tunnelingState mVideoTunnelingState;
  private boolean speedConvDisablePassthrough;
  private boolean speedConvertDisableTunneling;
  private boolean isSpeedShiftPlay;
  private boolean mIsDpcLibraryAvailable;
  private boolean mIsHlsUri;
  private boolean mIs4k2kDownconvert;
  private boolean mPcmPassthrough = false;
  private int audioBitrate;

  public RendererConfiguration setPcmPassthrough(final boolean enablePcmPassthrough) {
    Log.i("setPcmPassthrough: " + enablePcmPassthrough);
    mPcmPassthrough = enablePcmPassthrough;
    return mConfiguration;
  }
  public boolean isPcmPassthroughEnabled() {
    Log.i("mPcmPassthrough: " + mPcmPassthrough);
    return mPcmPassthrough;
  }

  public void setDpcPlusLibAvailable(final boolean available) {
    Log.i("setDpcPlusLibAvailable: " + available);
    mIsDpcLibraryAvailable = available;
  }
  public boolean isDpcPlusLibAvailable() {
    Log.i("isDpcPlusLibAvailable: " + mIsDpcLibraryAvailable);
    return mIsDpcLibraryAvailable;
  }

  public static RendererConfiguration getInstance() {
      if (mConfiguration == null) {
          mConfiguration = new RendererConfiguration();
      }
      return mConfiguration;
  }
  public static void releaseInstance() {
      if (mConfiguration != null) {
          mConfiguration = null;
      }
  }
    /**
     * Represents an unset {@link android.media.AudioTrack} session identifier.
     */
  public static final int SESSION_ID_NOT_SET = 0;
  private RendererConfiguration() {
      // deletion tunnel mode switching on seeking workaround
      // mExpectedState = playbackState.NOT_PAUSED;
      mTunnelingAudioSessionId = SESSION_ID_NOT_SET;
      mForcedDisableTunneling = false;
      mContentSupportsTunneling = false;
      mAudioLessContent = false;
      aacAudioDisablePassthrough = false;
      mSuspendAudioTrack = false;
      speedConvDisablePassthrough = false;
      speedConvertDisableTunneling = false;
      isSpeedShiftPlay = false;
      mIsHlsUri = false;
      mIs4k2kDownconvert = false;
      audioBitrate = 0;
  }
  public int getTunnelingAudioSessionId() {
      // Dolby Vision Audioless content supports Tunneling
     if (mForcedDisableTunneling || !mContentSupportsTunneling || mSuspendAudioTrack
          || mIsBluetoothConnect || speedConvertDisableTunneling || mIsHlsUri) {
          return SESSION_ID_NOT_SET;
      }
      return mTunnelingAudioSessionId;
  }
  public void setTunnelingAudioSessionId(int sessionId) {
      Log.d("Tunneling set sessionId=" + sessionId);
      mTunnelingAudioSessionId = sessionId;
  }

  public void setForcedDisableTunneling(boolean forcedDisableTunneling) {
      Log.i("Tunneling (Audio Channels > 2) forcedDisableTunneling=" + forcedDisableTunneling);
      mForcedDisableTunneling = forcedDisableTunneling;
  }

  // deletion tunnel mode switching on seeking workaround
  // public playbackState getExpectedState() {
  //      return mExpectedState;
  //  }
  // public void setExpectedState(playbackState state) {
  //    Log.d("Tunneling set ExpectedState=" + state);
  //    mExpectedState = state;
  //  }
  public void setContentSupportsTunneling(boolean contentSupportsTunneling) {
      Log.i("Tunneling set contentSupportsTunneling=" + contentSupportsTunneling);
      mContentSupportsTunneling = contentSupportsTunneling;
  }

  public void setAudioLessContent(boolean audioLessContent) {
      Log.i("audioLessContent=" + audioLessContent);
      mAudioLessContent = audioLessContent;
  }

  public boolean isAudioLessContent() {
      return mAudioLessContent;
  }

  public void setBluetoothConnectStatus(boolean isConnect) {
      mIsBluetoothConnect = isConnect;
      Log.i("Tunneling set mIsBluetoothConnect=" + mIsBluetoothConnect);
  }

  public void setAudioContentSupportPassthrough(boolean supportPasstthrough){
      isAudioContentSupportPassthrough = supportPasstthrough && !aacAudioDisablePassthrough;
      previousAllowPassthrough = isAudioContentSupportPassthrough && !mIsBluetoothConnect;
      Log.d("setAudioContentSupportPassthrough = " + supportPasstthrough
            + " aacAudioDisablePassthrough=" + aacAudioDisablePassthrough);
  }

  public boolean isPassthroughModeNeedChange(){
      boolean curAllowPassthrough = isAudioContentSupportPassthrough && !mIsBluetoothConnect
              && !aacAudioDisablePassthrough;
      return previousAllowPassthrough != curAllowPassthrough;
  }

  public void setAacAudioDisablePassthrough(boolean aacAudioDisablePassthrough) {
      this.aacAudioDisablePassthrough = aacAudioDisablePassthrough;
  }

  public boolean isAacAudioDisablePassthrough() {
      return aacAudioDisablePassthrough;
  }

  public void setSuspendAudioTrack(boolean suspend) {
      mSuspendAudioTrack = suspend;
      Log.d("suspend=" + suspend);
  }

  public boolean isSuspendAudioTrack() { return mSuspendAudioTrack; }

  public void setSpeedConvDisablePassthrough(boolean speedConvDisablePassthrough) {
      this.speedConvDisablePassthrough = speedConvDisablePassthrough;
  }

  public boolean isSpeedConvDisablePassthrough() {
      return speedConvDisablePassthrough;
  }

  public boolean isAllowPassthrough() {
    return isAudioContentSupportPassthrough && !mIsBluetoothConnect
            && !aacAudioDisablePassthrough;
  }

  public void setVideoTunnelingState(tunnelingState state) { mVideoTunnelingState = state; }

  public tunnelingState getVideoTunnelingState() { return mVideoTunnelingState; }

  public void setSpeedConvertDisableTunneling(boolean speedConvertDisableTunneling) {
      this.speedConvertDisableTunneling = speedConvertDisableTunneling;
  }
  public boolean getSpeedConvertDisableTunneling() {
      return speedConvertDisableTunneling;
  }

  public void setSpeedShiftPlay(boolean speedShift) {
      this.isSpeedShiftPlay = speedShift;
  }
  public boolean isSpeedShiftPlay() {
      return this.isSpeedShiftPlay;
  }

  public void setHlsUri(boolean isHlsUri) {
      Log.i("setHlsUri(China Cast): " + isHlsUri);
      mIsHlsUri = isHlsUri;
  }

  public boolean isHlsUri() {
      Log.i("isHlsUri(China Cast): " + mIsHlsUri);
      return mIsHlsUri;
  }

  public void set4k2kDownconvert(boolean is4k2kDownconvert) {
      Log.i("set4k2kDownconvert: " + is4k2kDownconvert);
      mIs4k2kDownconvert = is4k2kDownconvert;
  }

  public boolean is4k2kDownconvert() {
      Log.i("is4k2kDownconvert: " + mIs4k2kDownconvert);
      return mIs4k2kDownconvert;
  }

  public void setAudioBitrate(int bps) {
      Log.i("Audio Bitrate setting=" + bps);
      audioBitrate = bps;
  }

  public int getAudioBitrate() {
      return audioBitrate;
  }
}

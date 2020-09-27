package com.weidi.media.wdplayer.util;

/***
 Created by weidi on 2019/7/13.
 */
public interface Callback {
    // 生产者 消费者
    int MSG_ON_TRANSACT_VIDEO_PRODUCER = 0x1001;
    int MSG_ON_TRANSACT_VIDEO_CONSUMER = 0x1002;
    int MSG_ON_TRANSACT_AUDIO_PRODUCER = 0x1003;
    int MSG_ON_TRANSACT_AUDIO_CONSUMER = 0x1004;
    int MSG_ON_TRANSACT_INIT_VIDEO_MEDIACODEC = 0x1005;// 没用了

    int MSG_ON_TRANSACT_READY = 0x001;
    int MSG_ON_TRANSACT_CHANGE_WINDOW = 0x002;
    int MSG_ON_TRANSACT_PLAYED = 0x003;
    int MSG_ON_TRANSACT_PAUSED = 0x004;
    int MSG_ON_TRANSACT_FINISHED = 0x005;
    int MSG_ON_TRANSACT_INFO = 0x006;
    int MSG_ON_TRANSACT_ERROR = 0x007;
    int MSG_ON_TRANSACT_PROGRESS_UPDATED = 0x008;

    int ERROR_FFMPEG_INIT = 0x100;
    int ERROR_TIME_OUT = 0x101;
    //int ERROR_DATA_EXCEPTION = 0x102;

    int onTransact(int code, JniObject jniObject);

    void onReady();

    void onChangeWindow(int width, int height);

    void onPlayed();

    void onPaused();

    void onFinished();

    /***
     录音时presentationTimeUs这个参数没有意义,
     只要知道回调一次,时间为一秒.
     * @param presentationTimeUs
     */
    void onProgressUpdated(long presentationTimeUs);

    void onError(int error, String errorInfo);

    void onInfo(String info);

}
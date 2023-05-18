package com.weidi.media.wdplayer.video_player;

import android.content.Context;
import android.os.Handler;
import android.view.Surface;

import com.weidi.media.wdplayer.util.Callback;
import com.weidi.media.wdplayer.util.JniObject;

public interface WdPlayer {

    void setContext(Context context);

    void setHandler(Handler handler);

    // 使用FFMPEG中的mCallback
    void setCallback(Callback callback);

    void setSurface(Surface surface);

    void setDataSource(String path);

    void setVolume(float volume);

    // 单位: 秒
    void seekTo(long second);

    boolean prepareSync();

    boolean prepareAsync();

    void start();

    // 播放
    void play();

    // 暂停
    void pause();

    // 结束
    void release();

    // 包括播放(isPlaying)与暂停(!isPlaying)两种状态
    boolean isRunning();

    boolean isPlaying();

    // 单位: 秒
    long getDuration();

    // 有些操作可以通过这样的方法进行
    String onTransact(int code, JniObject jniObject);

}

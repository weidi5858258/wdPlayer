# wdPlayer

MacBook连接安卓机最好的工具
OpenMTP：https://openmtp.ganeshrvel.com
OpenMTP GitHub页面：https://github.com/ganeshrvel/openmtp

/Users/alexander/mydev/workspace_android/wdPlayer/gradle/wrapper/gradle-wrapper.properties
https://www.kagura.me/dev/20200828131600.html
distributionUrl=https://code.aliyun.com/kar/gradle-bin-zip/raw/master/gradle-6.5-bin.zip
distributionUrl=https\://services.gradle.org/distributions/gradle-6.5-bin.zip

视频的帧率（Frame Rate）   : 指示视频一秒显示的帧数（图像数）.
音频的采样率（Sample Rate）: 表示音频一秒播放的样本（Sample）的个数.
DTS，Decoding Time Stamp，    解码时间戳，告诉解码器packet的解码顺序.
PTS，Presentation Time Stamp，显示时间戳，指示从packet中解码出来的数据的显示顺序.
对于音频来说，DTS和PTS是相同的，也就是其解码的顺序和播放的顺序是相同的，但对于视频来说情况就有些不同了.

I帧 关键帧，包含了一帧的完整数据，解码时只需要本帧的数据，不需要参考其他帧。
P帧 P是向前搜索，该帧的数据不完全的，解码时需要参考其前一帧的数据。
B帧 B是双向搜索，解码这种类型的帧是最复杂，不但需要参考其一帧的数据，还需要其后一帧的数据。

通常来说只有在流中含有B帧的时候，PTS和DTS才会不同。

read_thread(void *arg) ---> audio_thread(void *arg)    ---> audio_play(void *arg)
read_thread(void *arg) ---> video_thread(void *arg)    ---> video_play(void *arg)
read_thread(void *arg) ---> video_thread_mc(void *arg) ---> video_play(void *arg)

audio:
    audio_thread(...)








































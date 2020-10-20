package com.weidi.media.wdplayer;

public interface Constants {

    String PREFERENCES_NAME = "alexander_preferences";
    String PLAYBACK_ADDRESS = "playback_address";
    String PLAYBACK_POSITION = "playback_position";
    String PLAYBACK_ISLIVE = "playback_islive";
    // 是否正常结束,true表示正常结束
    String PLAYBACK_NORMAL_FINISH = "playback_normal_finish";
    String PLAYBACK_MEDIA_TYPE = "playback_media_type";
    // true表示静音
    String PLAYBACK_IS_MUTE = "playback_is_mute";
    // 保存窗口位置
    String PLAYBACK_WINDOW_POSITION = "playback_window_position";
    String PLAYBACK_WINDOW_POSITION_TAG = "@@@@@@@@@@";
    // 显示控制面板(true表示显示,false表示隐藏)
    String PLAYBACK_SHOW_CONTROLLERPANELLAYOUT = "playback_show_controllerpanellayout";
    // 使用"use_exoplayer"和"use_ffmpeg"两个字符串.默认为"use_exoplayer"
    // 当值为use_exoplayer时,表示在FfmpegUseMediaCodecDecode类中,
    // MediaFormat的值由exoplayer去得到的.
    // 另一个的意思是由底层传给java层一些参数然后创建MediaFormat
    String PLAYBACK_USE_EXOPLAYER_OR_FFMPEG = "playback_use_exoplayer_or_ffmpeg";

    String PLAYBACK_USE_PLAYER = "playback_use_player";
    String PLAYER_FFMPEG = "player_ffmpeg";
    String PLAYER_MEDIACODEC = "player_mediacodec";
    String PLAYER_FFMPEG_MEDIACODEC = "player_ffmpeg_mediacodec";
}

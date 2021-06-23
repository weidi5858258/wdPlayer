package com.weidi.media.wdplayer;

public interface Constants {

    String PREFERENCES_NAME = "alexander_preferences";
    String PREFERENCES_NAME_REMOTE = "alexander_preferences_remote";

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
    String MEDIACODEC_TIME_OUT = "mediacodec_time_out";
    String NEED_TWO_PLAYER = "need_two_player";
    String NEED_SHOW_MEDIA_INFO = "need_show_media_info";
    String NEED_SHOW_CACHE_PROGRESS = "need_show_cache_progress";
    String PLAYBACK_USE_PLAYER = "playback_use_player";
    String PLAYER_IJKPLAYER = "player_ijkplayer";
    String PLAYER_FFMPEG_MEDIACODEC = "player_ffmpeg_mediacodec";
    String PLAYER_FFPLAY = "player_ffplay";
    String PLAYER_MEDIACODEC = "player_mediacodec";
    // 针对PLAYER_IJKPLAYER和PLAYER_FFMPEG_MEDIACODEC是否进行硬解
    // 针对音频部分和视频部分,为0时表示使用软解,为1时表示使用硬解
    String HARD_SOLUTION = "hard_solution";
    // 针对音频部分(本地窗口),为0时表示使用软解,为1时表示使用硬解
    String HARD_SOLUTION_AUDIO = "hard_solution_audio";

    int BUTTON_CLICK_FR = 1000;
    int BUTTON_CLICK_FF = 1001;
    int BUTTON_CLICK_PREV = 1002;
    int BUTTON_CLICK_NEXT = 1003;
    int BUTTON_CLICK_PLAY = 1004;
    int BUTTON_CLICK_PAUSE = 1005;
    int BUTTON_CLICK_EXIT = 1006;
    int BUTTON_CLICK_VOLUME_NORMAL = 1007;
    int BUTTON_CLICK_VOLUME_MUTE = 1008;
    int BUTTON_CLICK_REPEAT_OFF = 1009;
    int BUTTON_CLICK_REPEAT_ALL = 1010;
    int BUTTON_CLICK_REPEAT_ONE = 1011;
    int BUTTON_CLICK_SHUFFLE_OFF = 1012;
    int BUTTON_CLICK_SHUFFLE_ON = 1013;
    int BUTTON_CLICK_TEST_START = 1014;
    int BUTTON_CLICK_TEST_STOP = 1015;

    int DO_SOMETHING_EVENT_IS_RUNNING = 2000;
    int DO_SOMETHING_EVENT_GET_MEDIA_DURATION = 2001;
    int DO_SOMETHING_EVENT_GET_REPEAT = 2002;
    int DO_SOMETHING_EVENT_GET_SHUFFLE = 2003;
    int DO_SOMETHING_EVENT_WIDTH_SCREEN = 2004;
    int DO_SOMETHING_EVENT_MIN_SCREEN = 2005;
}

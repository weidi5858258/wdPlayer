package com.weidi.media.wdplayer;

import android.app.Activity;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.weidi.media.wdplayer.business.contents.LiveActivity;
import com.weidi.media.wdplayer.business.contents.LocalAudioActivity;
import com.weidi.media.wdplayer.util.MediaUtils;
import com.weidi.media.wdplayer.video_player.JniPlayerActivity;
import com.weidi.media.wdplayer.video_player.PlayerService;
import com.weidi.media.wdplayer.video_player.PlayerWrapper;
import com.weidi.utils.MyToast;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

import static com.weidi.media.wdplayer.Constants.PLAYBACK_IS_MUTE;
import static com.weidi.media.wdplayer.Constants.PLAYER_IJKPLAYER;
import static com.weidi.media.wdplayer.Constants.PLAYER_MEDIACODEC;
import static com.weidi.media.wdplayer.video_player.FFMPEG.VOLUME_MUTE;
import static com.weidi.media.wdplayer.video_player.FFMPEG.VOLUME_NORMAL;
import static com.weidi.media.wdplayer.video_player.JniPlayerActivity.isRunService;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final String PLAYERSERVICE =
            "com.weidi.media.wdplayer.video_player.PlayerService";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(
                com.weidi.library.R.anim.push_left_in,
                com.weidi.library.R.anim.push_left_out);
        super.onCreate(savedInstanceState);
        // Volume change should always affect media volume_normal
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        setContentView(R.layout.activity_main);

        internalCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");
        internalResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop()");
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy()");
        super.onDestroy();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(
                com.weidi.library.R.anim.push_right_in,
                com.weidi.library.R.anim.push_right_out);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult() requestCode: " + requestCode +
                " resultCode: " + resultCode +
                " data: " + data);
        if (requestCode == 0
                && resultCode == Activity.RESULT_CANCELED
                && null == data) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // onActivityResult() requestCode: 0 resultCode: 0 data: null
                if (Settings.canDrawOverlays(this)) {
                    startService(new Intent(this, PlayerService.class));
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && whatIsDevice == Configuration.UI_MODE_TYPE_NORMAL) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            boolean hasIgnored = powerManager.isIgnoringBatteryOptimizations(getPackageName());
            // 判断当前APP是否有加入电池优化的白名单，如果没有，弹出加入电池优化的白名单的设置对话框。
            if (!hasIgnored) {
                Intent intent =
                        new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////

    private int whatIsDevice = -1;
    private int clickCounts = 0;
    private boolean IS_TV = false;

    private void internalCreate(Bundle savedInstanceState) {
        UiModeManager uiModeManager =
                (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
        whatIsDevice = uiModeManager.getCurrentModeType();
        IS_TV = false;
        switch (whatIsDevice) {
            case Configuration.UI_MODE_TYPE_TELEVISION:
                IS_TV = true;
                break;
            default:
                break;
        }

        Handler uiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                //super.handleMessage(msg);
                if (clickCounts > 5) {
                    clickCounts = 5;
                }
                switch (clickCounts) {
                    case 1:
                        break;
                    case 2:
                        startActivity(new Intent(MainActivity.this, LiveActivity.class));
                        break;
                    case 3:
                        Intent intent = new Intent();
                        intent.putExtra(JniPlayerActivity.COMMAND_NO_FINISH, true);
                        intent.setClass(MainActivity.this, JniPlayerActivity.class);
                        startActivity(intent);
                        break;
                    case 4:
                        if (PlayerWrapper.IS_PHONE) {
                            startActivity(new Intent(MainActivity.this, LocalAudioActivity.class));
                        } else {
                            finish();
                        }
                        break;
                    case 5:
                        finish();
                        break;
                    default:
                        break;
                }
                clickCounts = 0;
            }
        };

        View view = findViewById(R.id.text);
        view.setClickable(true);
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickCounts++;
                MyToast.show(String.valueOf(clickCounts));
                uiHandler.removeMessages(1);
                uiHandler.sendEmptyMessageDelayed(1, 1000);
            }
        });

        if (IS_TV) {
            setTvView();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 申请浮窗权限
            if (!isRunService(this, PLAYERSERVICE)) {
                if (!Settings.canDrawOverlays(this)) {
                    Intent intent = new Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, 0);
                } else {
                    startService(new Intent(this, PlayerService.class));
                }
            }

            test2();
        }
    }

    private void internalResume() {
        boolean isPlaying = true;
        if (IS_TV && isPlaying) {
            findViewById(R.id.controller_panel_layout).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.controller_panel_layout).setVisibility(View.GONE);
        }
    }

    private void setTvView() {
        SeekBar mProgressBar = findViewById(R.id.progress_bar);
        TextView mFileNameTV = findViewById(R.id.file_name_tv);
        TextView mProgressTimeTV = findViewById(R.id.progress_time_tv);
        TextView mSeekTimeTV = findViewById(R.id.seek_time_tv);
        TextView mDurationTimeTV = findViewById(R.id.duration_time_tv);

        ImageButton mFrIB = findViewById(R.id.button_fr);
        ImageButton mFfIB = findViewById(R.id.button_ff);
        ImageButton mPrevIB = findViewById(R.id.button_prev);
        ImageButton mNextIB = findViewById(R.id.button_next);
        ImageButton mPlayIB = findViewById(R.id.button_play);
        ImageButton mPauseIB = findViewById(R.id.button_pause);
        ImageButton mExitIB = findViewById(R.id.button_exit);
        ImageButton mVolumeNormal = findViewById(R.id.volume_normal);
        ImageButton mVolumeMute = findViewById(R.id.volume_mute);
        ImageButton mRepeatOff = findViewById(R.id.button_repeat_off);
        ImageButton mRepeatAll = findViewById(R.id.button_repeat_all);
        ImageButton mRepeatOne = findViewById(R.id.button_repeat_one);
        ImageButton mShuffleOff = findViewById(R.id.button_shuffle_off);
        ImageButton mShuffleOn = findViewById(R.id.button_shuffle_on);

        mFrIB.setOnClickListener(mOnClickListener);
        mFfIB.setOnClickListener(mOnClickListener);
        mPrevIB.setOnClickListener(mOnClickListener);
        mNextIB.setOnClickListener(mOnClickListener);
        mPlayIB.setOnClickListener(mOnClickListener);
        mPauseIB.setOnClickListener(mOnClickListener);
        mExitIB.setOnClickListener(mOnClickListener);
        mVolumeNormal.setOnClickListener(mOnClickListener);
        mVolumeMute.setOnClickListener(mOnClickListener);
        mRepeatOff.setOnClickListener(mOnClickListener);
        mRepeatAll.setOnClickListener(mOnClickListener);
        mRepeatOne.setOnClickListener(mOnClickListener);
        mShuffleOff.setOnClickListener(mOnClickListener);
        mShuffleOn.setOnClickListener(mOnClickListener);

        mFrIB.setOnFocusChangeListener(mOnFocusChangeListener);
        mFfIB.setOnFocusChangeListener(mOnFocusChangeListener);
        mPrevIB.setOnFocusChangeListener(mOnFocusChangeListener);
        mNextIB.setOnFocusChangeListener(mOnFocusChangeListener);
        mPlayIB.setOnFocusChangeListener(mOnFocusChangeListener);
        mPauseIB.setOnFocusChangeListener(mOnFocusChangeListener);
        mExitIB.setOnFocusChangeListener(mOnFocusChangeListener);
        mVolumeNormal.setOnFocusChangeListener(mOnFocusChangeListener);
        mVolumeMute.setOnFocusChangeListener(mOnFocusChangeListener);
        mRepeatOff.setOnFocusChangeListener(mOnFocusChangeListener);
        mRepeatAll.setOnFocusChangeListener(mOnFocusChangeListener);
        mRepeatOne.setOnFocusChangeListener(mOnFocusChangeListener);
        mShuffleOff.setOnFocusChangeListener(mOnFocusChangeListener);
        mShuffleOn.setOnFocusChangeListener(mOnFocusChangeListener);
    }

    private void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_fr:
                break;
            case R.id.button_ff:
                break;
            case R.id.button_prev:
                break;
            case R.id.button_next:
                break;
            case R.id.button_play:
                break;
            case R.id.button_pause:
                break;
            case R.id.button_exit:
                break;
            case R.id.volume_normal:
                break;
            case R.id.volume_mute:
                break;
            case R.id.button_repeat_off:
                break;
            case R.id.button_repeat_all:
                break;
            case R.id.button_repeat_one:
                break;
            case R.id.button_shuffle_off:
                break;
            case R.id.button_shuffle_on:
                break;
            default:
                break;
        }
    }

    private void onFocusChange(View v, boolean hasFocus) {
        switch (v.getId()) {
            case R.id.button_fr:
                break;
            case R.id.button_ff:
                break;
            case R.id.button_prev:
                break;
            case R.id.button_next:
                break;
            case R.id.button_play:
                break;
            case R.id.button_pause:
                break;
            case R.id.button_exit:
                break;
            case R.id.volume_normal:
                break;
            case R.id.volume_mute:
                break;
            case R.id.button_repeat_off:
                break;
            case R.id.button_repeat_all:
                break;
            case R.id.button_repeat_one:
                break;
            case R.id.button_shuffle_off:
                break;
            case R.id.button_shuffle_on:
                break;
            default:
                break;
        }
    }

    private void test2() {
        /***
         {@link Environment#DIRECTORY_MUSIC}
         {@link Environment#DIRECTORY_MOVIES}
         {@link Environment#DIRECTORY_PICTURES}
         {@link Environment#DIRECTORY_PODCASTS}
         {@link Environment#DIRECTORY_RINGTONES}
         {@link Environment#DIRECTORY_ALARMS}
         {@link Environment#DIRECTORY_NOTIFICATIONS}

         访问的还是手机本身的存储卡,不是外置的SD卡
         getFilesDir            :
         /data/user/0/com.weidi.usefragments/files

         getCacheDir            :
         /data/user/0/com.weidi.usefragments/cache

         getExternalCacheDir    :
         /storage/emulated/0/Android/data/com.weidi.usefragments/cache

         getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC)
         DIRECTORY_MUSIC        :
         /storage/emulated/0/Android/data/com.weidi.usefragments/files/Music

         DIRECTORY_MOVIES       :
         /storage/emulated/0/Android/data/com.weidi.usefragments/files/Movies

         DIRECTORY_PICTURES     :
         /storage/emulated/0/Android/data/com.weidi.usefragments/files/Pictures

         DIRECTORY_PODCASTS     :
         /storage/emulated/0/Android/data/com.weidi.usefragments/files/Podcasts

         DIRECTORY_RINGTONES    :
         /storage/emulated/0/Android/data/com.weidi.usefragments/files/Ringtones

         DIRECTORY_ALARMS       :
         /storage/emulated/0/Android/data/com.weidi.usefragments/files/Alarms

         DIRECTORY_NOTIFICATIONS:
         /storage/emulated/0/Android/data/com.weidi.usefragments/files/Notifications

         getExternalMediaDirs();
         /storage/emulated/0/Android/media/com.weidi.media.wdplayer
         */

        File file = null;
        file = new File(getFilesDir().getAbsolutePath());
        file = new File(getCacheDir().getAbsolutePath());
        file = new File(getExternalCacheDir().getAbsolutePath());
        file = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath());
        file = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES).getAbsolutePath());
        file = new File(getExternalFilesDir(Environment.DIRECTORY_ALARMS).getAbsolutePath());
        file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath());
        file = new File(getExternalFilesDir(Environment.DIRECTORY_PODCASTS).getAbsolutePath());
        file = new File(getExternalFilesDir(Environment.DIRECTORY_RINGTONES).getAbsolutePath());
        file = new File(getExternalFilesDir(Environment.DIRECTORY_RINGTONES).getAbsolutePath());
        file = new File(getExternalFilesDir(Environment.DIRECTORY_NOTIFICATIONS).getAbsolutePath());
        file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES).getAbsolutePath());
        Log.i(TAG, "canWrite: " + file.canWrite());// false

        /***
         getExternalFilesDirs(Environment.DIRECTORY_MOVIES)
         Environment.DIRECTORY_MOVIES:
         /storage/emulated/0/Android/data/com.weidi.usefragments/files/Movies
         Environment.DIRECTORY_MOVIES:
         /storage/2430-1702/Android/data/com.weidi.usefragments/files/Movies

         Environment.MEDIA_MOUNTED   :
         /storage/emulated/0/Android/data/com.weidi.usefragments/files/mounted
         Environment.MEDIA_MOUNTED   :
         /storage/2430-1702/Android/data/com.weidi.usefragments/files/mounted

         Environment.MEDIA_SHARED    :
         /storage/emulated/0/Android/data/com.weidi.usefragments/files/shared
         /storage/2430-1702/Android/data/com.weidi.usefragments/files/shared
         */
        File[] files = null;
        files = getExternalFilesDirs(Environment.DIRECTORY_MOVIES);
        for (File f : files) {
            Log.i(TAG, "Environment.DIRECTORY_MOVIES: " + f.getAbsolutePath());
        }
        files = getExternalFilesDirs(Environment.MEDIA_MOUNTED);
        for (File f : files) {
            Log.i(TAG, "Environment.MEDIA_MOUNTED   : " + f.getAbsolutePath());
        }
        files = getExternalFilesDirs(Environment.MEDIA_SHARED);
        for (File f : files) {
            Log.i(TAG, "Environment.MEDIA_SHARED    : " + f.getAbsolutePath());
        }

        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_AUDIO_RAW);
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_AUDIO_MPEG);
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_AUDIO_AAC);
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_AUDIO_AC3);
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_AUDIO_VORBIS);
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_AUDIO_EAC3);
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_AUDIO_OPUS);
        // 不支持
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_AUDIO_QCELP);
        MediaUtils.findAllDecodersByMime("audio/mpeg-L2");
        MediaUtils.findAllDecodersByMime("audio/x-ms-wma");
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MainActivity.this.onClick(v);
        }
    };

    private View.OnFocusChangeListener mOnFocusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            MainActivity.this.onFocusChange(v, hasFocus);
        }
    };

}

package com.weidi.media.wdplayer;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;

import com.weidi.media.wdplayer.business.contents.LiveActivityForMenFavoriteWear;
import com.weidi.media.wdplayer.business.contents.LiveActivityForWear;
import com.weidi.media.wdplayer.business.contents.LocalAudioActivityForWear;
import com.weidi.media.wdplayer.business.contents.LocalVideoActivityForWear;
import com.weidi.media.wdplayer.video_player.JniPlayerActivity;
import com.weidi.media.wdplayer.video_player.PlayerService;
import com.weidi.utils.MyToast;

import java.io.File;

import androidx.annotation.NonNull;

import static com.weidi.media.wdplayer.Constants.HARD_SOLUTION;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_USE_PLAYER;
import static com.weidi.media.wdplayer.Constants.PLAYER_FFMPEG_MEDIACODEC;
import static com.weidi.media.wdplayer.Constants.PLAYER_IJKPLAYER;
import static com.weidi.media.wdplayer.Constants.PREFERENCES_NAME;
import static com.weidi.media.wdplayer.video_player.JniPlayerActivity.CONTENT_PATH;
import static com.weidi.media.wdplayer.video_player.JniPlayerActivity.CONTENT_TYPE;
import static com.weidi.media.wdplayer.video_player.JniPlayerActivity.isRunService;

//import android.support.wearable.activity.WearableActivity;

public class WearMainActivity extends WearableActivity {


    private static final String TAG = "WearMainActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(
                com.weidi.library.R.anim.push_left_in,
                com.weidi.library.R.anim.push_left_out);
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

        setContentView(R.layout.activity_main_wear);

        // Enables Always-on
        setAmbientEnabled();

        internalCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");
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
                && resultCode == Activity.RESULT_OK
                && null != data) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    startService(new Intent(this, PlayerService.class));
                }
            }
        }
    }

    /***
     进入待机的情况，在一定时间内没有接受到其他指令.应用退出不会被执行.
     */
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        Log.i(TAG, "onEnterAmbient()");
    }

    /***
     只要应用运行情况下每一分钟（就是手表分钟发生变化）触发.
     */
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        Log.i(TAG, "onUpdateAmbient()");
    }

    /***
     再次收到指令，并且被唤醒，类似手机被唤醒.第一次进入应用的时候 该方法不被执行.
     */
    public void onExitAmbient() {
        super.onExitAmbient();
        Log.i(TAG, "onExitAmbient()");
    }

    public void onInvalidateAmbientOffload() {
        super.onInvalidateAmbientOffload();
        Log.i(TAG, "onInvalidateAmbientOffload()");
    }

    /////////////////////////////////////////////////////////////////////////

    public void createAlarmTask() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        long anHour = 30 * 1000;
        long triggerAtTime = SystemClock.elapsedRealtime() + anHour;
        Intent intent = new Intent(this, JniPlayerActivity.class);
        intent.putExtra(CONTENT_PATH, "/storage/37C8-3904/myfiles/music/冷漠、云菲菲 - 伤心城市.mp3");
        intent.putExtra(CONTENT_PATH, "/storage/emulated/0/Music/谭咏麟 - 水中花.mp3");
        intent.putExtra(CONTENT_TYPE, "audio/");
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pendingIntent);
    }

    private int clickCounts = 0;

    private void internalCreate(Bundle savedInstanceState) {
        Handler uiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                //super.handleMessage(msg);

                SharedPreferences sp = null;
                /*if (clickCounts > 5) {
                    clickCounts = 5;
                }*/
                switch (clickCounts) {
                    case 1:
                        break;
                    case 2:
                        // 直播类节目
                        startActivity(
                                new Intent(WearMainActivity.this, LiveActivityForWear.class));
                        break;
                    case 3:
                        // 本地音乐
                        startActivity(
                                new Intent(WearMainActivity.this, LocalAudioActivityForWear.class));
                        break;
                    case 4:
                        // 本地视频
                        startActivity(
                                new Intent(WearMainActivity.this, LocalVideoActivityForWear.class));
                        break;
                    case 5:
                        sp = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
                        sp.edit().putString(PLAYBACK_USE_PLAYER, PLAYER_FFMPEG_MEDIACODEC).commit();
                        MyToast.show(PLAYER_FFMPEG_MEDIACODEC);
                        break;
                    case 6:
                        sp = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
                        sp.edit().putString(PLAYBACK_USE_PLAYER, PLAYER_IJKPLAYER).commit();
                        MyToast.show(PLAYER_IJKPLAYER);
                        break;
                    case 7:
                        sp = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
                        int softSolution = sp.getInt(HARD_SOLUTION, 1);
                        if (softSolution == 1) {
                            MyToast.show("使用软解码");
                            sp.edit().putInt(HARD_SOLUTION, 0).commit();
                        } else if (softSolution == 0) {
                            MyToast.show("使用硬解码");
                            sp.edit().putInt(HARD_SOLUTION, 1).commit();
                        }
                        break;
                    case 8:
                        finish();
                        break;
                    case 9:
                        break;
                    case 10:
                        break;
                    case 20:
                        startActivity(
                                new Intent(WearMainActivity.this,
                                        LiveActivityForMenFavoriteWear.class));
                        break;
                    default:
                        break;
                }
                clickCounts = 0;
            }
        };
        /*findViewById(R.id.root_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickCounts++;
                MyToast.show(String.valueOf(clickCounts));
                uiHandler.removeMessages(1);
                uiHandler.sendEmptyMessageDelayed(1, 1000);
            }
        });*/
        findViewById(R.id.text).requestFocus();
        findViewById(R.id.text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickCounts++;
                MyToast.show(String.valueOf(clickCounts));
                uiHandler.removeMessages(1);
                uiHandler.sendEmptyMessageDelayed(1, 1000);
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 申请浮窗权限
            if (!isRunService(this, "com.weidi.media.wdplayer.video_player.PlayerService")) {
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

        /*MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_AUDIO_RAW);
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_AUDIO_MPEG);
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_AUDIO_AAC);
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_AUDIO_AC3);
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_AUDIO_VORBIS);
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_AUDIO_EAC3);
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_AUDIO_OPUS);
        // 不支持
        MediaUtils.findAllDecodersByMime(MediaFormat.MIMETYPE_AUDIO_QCELP);
        MediaUtils.findAllDecodersByMime("audio/mpeg-L2");
        MediaUtils.findAllDecodersByMime("audio/x-ms-wma");*/
    }
}

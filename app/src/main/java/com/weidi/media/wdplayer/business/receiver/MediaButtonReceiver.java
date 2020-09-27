package com.weidi.media.wdplayer.business.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.view.KeyEvent;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.log.MLog;
import com.weidi.media.wdplayer.video_player.PlayerService;

/***
 Created by root on 19-7-2.
 这个广播需要两个地方注册才有效
 1.AndroidManifest.xml
 2.PlayerService(AudioManager)
 */

public class MediaButtonReceiver extends BroadcastReceiver {

    private static final String TAG = "player_alexander";
    /*private static final String TAG =
            MediaButtonReceiver.class.getSimpleName();*/

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        String intentAction = intent.getAction();
        //MLog.d(TAG, "onReceive() intentAction: " + intentAction);
        if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null) {
                return;
            }

            if (event.getAction() != KeyEvent.ACTION_UP) {
                return;
            }

            int keyCode = event.getKeyCode();
            MLog.d(TAG, "onReceive() " +
                    "intentAction: " + intentAction +
                    " keyCode: " + keyCode);
            switch (keyCode) {
                case KeyEvent.KEYCODE_HEADSETHOOK:// 79
                    EventBusUtils.post(
                            PlayerService.class, KeyEvent.KEYCODE_HEADSETHOOK, null);
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:// 85
                    EventBusUtils.post(
                            PlayerService.class, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, null);
                    break;
                case KeyEvent.KEYCODE_MEDIA_STOP:// 86
                    EventBusUtils.post(
                            PlayerService.class, KeyEvent.KEYCODE_MEDIA_STOP, null);
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:// 88
                    // 三击
                    EventBusUtils.post(
                            PlayerService.class, KeyEvent.KEYCODE_MEDIA_PREVIOUS, null);
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:// 87
                    // 双击
                    EventBusUtils.post(
                            PlayerService.class, KeyEvent.KEYCODE_MEDIA_NEXT, null);
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY:// 126
                    // 单击
                    EventBusUtils.post(
                            PlayerService.class, KeyEvent.KEYCODE_MEDIA_PLAY, null);
                    break;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:// 127
                    // 单击
                    EventBusUtils.post(
                            PlayerService.class, KeyEvent.KEYCODE_MEDIA_PAUSE, null);
                    break;
                default:
                    break;
            }
        } else if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intentAction)) {
            // 耳机插事件,没有拔事件
        }
    }

}

package com.weidi.media.wdplayer.video_player;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.weidi.eventbus.Phone;

/***
 Created by root on 20-12-01.
 */

public class TestService extends Service {

    private static final String TAG = "alexander_player";

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind() intent: " + intent);
        // 被PlayerService绑定(PlayerService启动时就绑定)
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind() intent: " + intent);
        // 被PlayerService解绑(实际不绑定,一直绑定)
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate()");
        internalCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand() intent: " + intent);
        internalStartCommand(intent, flags, startId);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        super.onDestroy();
        internalDestroy();
    }

    ////////////////////////////////////////////////////////////////////////

    private void internalCreate() {
        Phone.register(this);
    }

    // 测试时使用
    private void internalStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            // app crash后的操作
            return;
        }

        String action = intent.getAction();
        Log.i(TAG, "internalStartCommand()   action: " + action);
    }

    private void internalDestroy() {
        Phone.unregister(this);
    }

    // 当前Service活着的时候,由其他地方发送事件到这里进行处理
    private Object onEvent(int what, Object[] objArray) {
        Object result = null;
        switch (what) {
            case 100:
                stopSelf();
                break;
            default:
                break;
        }
        return result;
    }

}

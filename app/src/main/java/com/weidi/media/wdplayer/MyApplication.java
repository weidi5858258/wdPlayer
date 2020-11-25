package com.weidi.media.wdplayer;

import android.content.Context;

import com.weidi.application.WeidiApplication;

/***
 Created by root on 18-12-13.
 */

public class MyApplication extends WeidiApplication {

    private static final String TAG =
            MyApplication.class.getSimpleName();

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        xcrash.XCrash.init(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

}

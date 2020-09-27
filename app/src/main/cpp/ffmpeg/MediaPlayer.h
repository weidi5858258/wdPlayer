//
// Created by root on 19-8-8.
//

#ifndef USEFRAGMENTS_MEDIAPLAYER_H
#define USEFRAGMENTS_MEDIAPLAYER_H

// 需要引入native绘制的头文件
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <list>
#include <iostream>
#include <iomanip>

#include <time.h>

#include "Wrapper.h"

#if __ANDROID_API__ >= 21
//__INTRODUCED_IN(21);
#endif

#if __ANDROID_API__ >= __ANDROID_API_N__
#endif

///////////////////////////////////////////

namespace alexander_media {

    void *readData(void *opaque);

    void *handleData(void *opaque);

    void initAV();

    void initAudio();

    void initVideo();

    int openAndFindAVFormatContext();

    int findStreamIndex();

    int findAndOpenAVCodecForAudio();

    int findAndOpenAVCodecForVideo();

    int createSwrContent();

    int createSwsContext();

    void closeAudio();

    void closeVideo();

    int initPlayer();

    void setJniParameters(JNIEnv *env, const char *filePath, jobject surfaceJavaObject);

    int play();

    int pause();

    int stop();

    int release();

    bool isRunning();

    bool isPlaying();

    bool isPausedForUser();

    int seekTo(int64_t timestamp);

    long getDuration();

    void stepAdd(int64_t addStep);

    void stepSubtract(int64_t subtractStep);

    int download(int flag, const char *filePath, const char *fileName);

}

#endif //USEFRAGMENTS_MEDIAPLAYER_H

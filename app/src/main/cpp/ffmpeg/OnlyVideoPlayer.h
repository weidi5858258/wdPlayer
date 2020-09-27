//
// Created by root on 19-8-8.
//

#ifndef USEFRAGMENTS_ONLYVIDEOPLAYER_H
#define USEFRAGMENTS_ONLYVIDEOPLAYER_H

#include "MediaPlayer.h"
#include "ffmpeg.h"
#include "../include/Log.h"

namespace alexander_only_video {

    void *readData(void *opaque);

    void *handleData(void *opaque);

    void initAV();

    void initVideo();

    int openAndFindAVFormatContext();

    int findStreamIndex();

    int findAndOpenAVCodecForVideo();

    int createSwsContext();

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

    int64_t getDuration();

    void stepAdd(int64_t addStep);

    void stepSubtract(int64_t subtractStep);
}


#endif //USEFRAGMENTS_ONLYVIDEOPLAYER_H

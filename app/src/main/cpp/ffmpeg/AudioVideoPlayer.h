//
// Created by root on 19-8-8.
//

#ifndef USEFRAGMENTS_AUDIOVIDEOPLAYER_H
#define USEFRAGMENTS_AUDIOVIDEOPLAYER_H

#include "MediaPlayer.h"

namespace alexander_audio_video {

    void *readData(void *opaque);

    void *handleData(void *opaque);

    int handleVideoOutputBuffer(int roomIndex);

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

}

#endif //USEFRAGMENTS_AUDIOVIDEOPLAYER_H

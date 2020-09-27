//
// Created by root on 19-8-8.
//

#ifndef USEFRAGMENTS_ONLYAUDIOPLAYER_H
#define USEFRAGMENTS_ONLYAUDIOPLAYER_H

#include "MediaPlayer.h"
#include "ffmpeg.h"
#include "../include/Log.h"

namespace alexander_only_audio {

    void *readData(void *opaque);

    void *handleData(void *opaque);

    int handleAudioOutputBuffer(int roomIndex);

    void initAV();

    void initAudio();

    int openAndFindAVFormatContext();

    int findStreamIndex();

    int findAndOpenAVCodecForAudio();

    int createSwrContent();

    void closeAudio();

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


#endif //USEFRAGMENTS_ONLYAUDIOPLAYER_H

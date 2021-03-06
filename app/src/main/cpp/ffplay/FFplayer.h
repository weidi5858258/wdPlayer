//
// Created by alexander on 2020/3/2.
//

#ifndef FFMPEG_STUDY_FFPLAYER_H
#define FFMPEG_STUDY_FFPLAYER_H

// 需要引入native绘制的头文件
#include <android/native_window.h>
#include <android/native_window_jni.h>

//namespace alexander_ffplayer {

void setDataSource(const char *filePath);

void setSurface(JNIEnv *env, jobject surfaceJavaObject);

int initPlayer();

int start();

int play_pause();

int stop();

int release();

bool isRunning();

bool isPlaying();

//bool isPausedForUser();

int seekTo(int64_t timestamp);

long getDuration();

void stepAdd(int64_t addStep);

void stepSubtract(int64_t subtractStep);

void clearQueue();

int decoder_decode_frame_by_mediacodec(int roomIndex,
                                       int offset,
                                       int size,
                                       int serial,
                                       long long int presentationTimeUs,
                                       long long int pts,
                                       long long int dts,
                                       long long int pos,
                                       long long int duration,
                                       const uint8_t *data);

//}


#endif //FFMPEG_STUDY_FFPLAYER_H

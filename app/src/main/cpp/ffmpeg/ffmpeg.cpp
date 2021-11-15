//
// Created by ex-wangliwei on 2016/2/14.
//

#include <libavcodec/jni.h>
#include <string>
#include "ffmpeg.h"
//#include "MediaPlayer.h"
#include "OnlyVideoPlayer.h"
#include "OnlyAudioPlayer.h"
#include "AudioVideoPlayer.h"
#include "AACH264Player.h"
#include "MediaPlayerForMediaCodec.h"
#include "../ffplay/FFplayer.h"

// 这个是自定义的LOG的标识
#define LOG "player_alexander"

// 避免最常见的 10 大 JNI 编程错误的技巧和工具
// https://blog.csdn.net/lubeijing2008xu/article/details/37569809
/***
 全局变量的生命周期:
 普通的全局变量,不需要是static的全局变量
 (好像)
 只要进程不被kill掉,这些变量的值就会一直被保存着

 // jclass jobject jstring jobjectArray jintArray 等需要释放空间


    jsize count = env->GetArrayLength(objects);
    for (jsize i = 0; i < count; i++) {
        jobject element = env->GetObjectArrayElement(objects, i);
        if (env->ExceptionOccurred()) {
            break;
        }

        env->DeleteLocalRef(element);
    }

    jclass integerClass = env->FindClass("java/lang/Integer");
    jobject element0 = env->GetObjectArrayElement(objects, 0);
    int flag = -1;
    if (env->IsInstanceOf(element0, integerClass)) {
        jint flag_ = (jint) element0;
        flag = (int) flag_;
    }
    env->DeleteLocalRef(integerClass);
    得到的flag跟java端传过来的值不一样

    每个方法里至少有两个参数,即:
    JNIEnv *env和jobject ffmpegObject
    其中jobject ffmpegObject代表java层定义setSurface方法的类对象.

    jobject data;
    jclass class_jclass = env->FindClass("java/lang/Class");
    jmethodID forName_jmethodID = env->GetStaticMethodID(
            class_jclass, "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
    jstring surface_jstring = env->NewStringUTF("android.view.Surface");
    jobject class_jobject = env->CallStaticObjectMethod(
            class_jclass, forName_jmethodID, surface_jstring);
    jmethodID getClassLoader_jmethodID = env->GetMethodID(
            class_jclass, "getClassLoader", "()Ljava/lang/ClassLoader;");
    // ClassLoader classLoader = Surface.class.getClassLoader()
    jobject classLoader_jobject = env->CallObjectMethod(
            class_jobject, getClassLoader_jmethodID);
    // 从data中读出Surface对象
    jmethodID readParcelable_jmethodID = env->GetMethodID(
            parcel_jclass, "readParcelable", "(Ljava/lang/ClassLoader;)Landroid/os/Parcelable;");
    jobject surfaceObject = env->CallObjectMethod(
            data, readParcelable_jmethodID, classLoader_jobject);
 */

// 这个值在任何情况下都不要置为"NULL"
static JavaVM *gJavaVm = nullptr;

jclass jniObject_jclass = nullptr;
jfieldID valueObject_jfieldID = nullptr;
jfieldID valueString_jfieldID = nullptr;
jfieldID valueInt_jfieldID = nullptr;
jfieldID valueLong_jfieldID = nullptr;
jfieldID valueByte_jfieldID = nullptr;
jfieldID valueBoolean_jfieldID = nullptr;
jfieldID valueFloat_jfieldID = nullptr;
jfieldID valueDouble_jfieldID = nullptr;
// array
jfieldID valueObjectArray_jfieldID = nullptr;
jfieldID valueStringArray_jfieldID = nullptr;
jfieldID valueIntArray_jfieldID = nullptr;
jfieldID valueLongArray_jfieldID = nullptr;
jfieldID valueByteArray_jfieldID = nullptr;
jfieldID valueBooleanArray_jfieldID = nullptr;
jfieldID valueFloatArray_jfieldID = nullptr;
jfieldID valueDoubleArray_jfieldID = nullptr;
// MediaCodec.BufferInfo
jfieldID presentationTimeUs_jfieldID = nullptr;

// 下面的jobject,jmethodID按照java的反射过程去理解,套路(jni层调用java层方法)跟反射是一样的
// java层FFMPEG对象
jobject ffmpegJavaObject = nullptr;
jmethodID initMediaCodecMethodID = nullptr;
jmethodID feedInputBufferAndDrainOutputBufferMethodID = nullptr;
jmethodID feedInputBufferAndDrainOutputBufferMethodID2 = nullptr;
jmethodID createAudioTrackMethodID = nullptr;
jmethodID writeMethodID = nullptr;
jmethodID sleepMethodID = nullptr;
jmethodID postDelayed_jmethodID = nullptr;

jobject videoProducerObject = nullptr;
jobject videoConsumerObject = nullptr;
jobject audioProducerObject = nullptr;
jobject audioConsumerObject = nullptr;

// java层FFMPEG中定义的Callback对象
jobject callback_jobject = nullptr;
struct Callback {
    jmethodID onTransactMethodID = nullptr;
    jmethodID onReadyMethodID = nullptr;
    jmethodID onChangeWindowMethodID = nullptr;
    jmethodID onPlayedMethodID = nullptr;
    jmethodID onPausedMethodID = nullptr;
    jmethodID onFinishedMethodID = nullptr;
    jmethodID onProgressUpdatedMethodID = nullptr;
    jmethodID onErrorMethodID = nullptr;
    jmethodID onInfoMethodID = nullptr;
} callback;

int use_mode = USE_MODE_MEDIA_FFPLAY;

extern double REMAINING_TIME;
extern double TIME_STEP;
extern bool isWatch;
extern bool isWatchForCloseVideo;
extern bool isWatchForCloseAudio;

// test
int runCount = 0;

/***
 called at the library loaded.
 这个方法只有放在这个文件里才有效,在其他文件不会被回调
 */
jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    LOGD("JNI_OnLoad()\n");
    gJavaVm = vm;
    /*int ret = av_jni_set_java_vm(vm, nullptr);
    if (ret < 0) {
        LOGE("JNI_OnLoad() av_jni_set_java_vm() error ret: %d\n", ret);
    }*/
    return JNI_VERSION_1_6;
}

// 这个方法只有放在这个文件里才有效,在其他文件调用失效
bool getEnv(JNIEnv **env) {
    bool isAttached = false;
    jint jResult = gJavaVm->GetEnv((void **) env, JNI_VERSION_1_6);
    if (jResult != JNI_OK) {
        if (jResult == JNI_EDETACHED) {
            if (gJavaVm->AttachCurrentThread(env, NULL) != JNI_OK) {
                LOGE("AttachCurrentThread Failed.\n");
                *env = nullptr;
                return isAttached;
            }
            isAttached = true;
        } else {
            LOGE("GetEnv Failed.\n");
            *env = nullptr;
            return isAttached;
        }
    }

    return isAttached;
}

bool initMediaCodecImpl(JNIEnv *jniEnv, int type, int mimeType,
                        long long *parameters, int parameters_size,
                        unsigned char *sps_pps, int sps_pps_size) {
    bool initRet = false;
    // 创建装2个对象的Object[]
    jsize length = 2;
    jclass elementClass = jniEnv->FindClass("java/lang/Object");
    jobject initialElement = nullptr;
    jobjectArray objectsData = jniEnv->NewObjectArray(length, elementClass, initialElement);

    // int[]保存各种int值,创建MediaFormat时要用
    jlongArray parasData = nullptr;
    if (parameters != nullptr) {
        parasData = jniEnv->NewLongArray(parameters_size);
        jniEnv->SetLongArrayRegion(
                parasData, 0, parameters_size, reinterpret_cast<const jlong *>(parameters));
        jniEnv->SetObjectArrayElement(objectsData, 0, (jobject) parasData);
    } else {
        jniEnv->SetObjectArrayElement(objectsData, 0, nullptr);
    }

    jbyteArray sps_pps_data = nullptr;
    if (sps_pps != nullptr) {
        sps_pps_data = jniEnv->NewByteArray(sps_pps_size);
        jniEnv->SetByteArrayRegion(
                sps_pps_data, 0, sps_pps_size, reinterpret_cast<const jbyte *>(sps_pps));
        jniEnv->SetObjectArrayElement(objectsData, 1, (jobject) sps_pps_data);
    } else {
        jniEnv->SetObjectArrayElement(objectsData, 1, nullptr);
    }

    jobject jniObject = jniEnv->AllocObject(jniObject_jclass);
    jfieldID valueInt_jfieldID = jniEnv->GetFieldID(
            jniObject_jclass, "valueInt", "I");
    jfieldID valueObjectArray_jfieldID = jniEnv->GetFieldID(
            jniObject_jclass, "valueObjectArray", "[Ljava/lang/Object;");
    // valueInt = mimeType
    jniEnv->SetIntField(jniObject, valueInt_jfieldID, mimeType);
    // valueObjectArray = objectsData
    jniEnv->SetObjectField(jniObject, valueObjectArray_jfieldID, objectsData);

    initRet = jniEnv->CallBooleanMethod(ffmpegJavaObject, initMediaCodecMethodID, type, jniObject);

    jniEnv->DeleteLocalRef(parasData);
    parasData = nullptr;
    if (sps_pps_data != nullptr) {
        jniEnv->DeleteLocalRef(sps_pps_data);
        sps_pps_data = nullptr;
    }
    jniEnv->DeleteLocalRef(objectsData);
    objectsData = nullptr;
    jniEnv->DeleteLocalRef(jniObject);
    jniObject = nullptr;
    jniEnv->DeleteLocalRef(elementClass);
    elementClass = nullptr;

    return initRet;
}

bool initMediaCodec(int type,
                    int mimeType,
                    long long *parameters, int parameters_size,
                    unsigned char *sps_pps, int sps_pps_size) {
    bool initRet = false;
    JNIEnv *jniEnv;
    bool isAttached = getEnv(&jniEnv);
    if (jniEnv != nullptr
        && ffmpegJavaObject != nullptr
        && initMediaCodecMethodID != nullptr) {
        initRet = initMediaCodecImpl(jniEnv, type, mimeType,
                                     parameters, parameters_size,
                                     sps_pps, sps_pps_size);
    }
    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
    return initRet;
}

bool feedInputBufferAndDrainOutputBuffer(int type,
                                         unsigned char *encodedData,
                                         int size,
                                         long long int presentationTimeUs) {
    bool feedAndDrainRet = false;
    JNIEnv *bufferEnv;
    bool audioIsAttached = getEnv(&bufferEnv);
    if (bufferEnv != nullptr
        && ffmpegJavaObject != nullptr
        && feedInputBufferAndDrainOutputBufferMethodID != nullptr) {
        jbyteArray data = bufferEnv->NewByteArray(size);
        bufferEnv->SetByteArrayRegion(
                data, 0, size, reinterpret_cast<const jbyte *>(encodedData));
        feedAndDrainRet = bufferEnv->CallBooleanMethod(ffmpegJavaObject,
                                                       feedInputBufferAndDrainOutputBufferMethodID,
                                                       (jint) type, data, (jint) size,
                                                       (jlong) presentationTimeUs);
        bufferEnv->DeleteLocalRef(data);
    }
    if (audioIsAttached) {
        gJavaVm->DetachCurrentThread();
    }
    return feedAndDrainRet;
}

bool feedInputBufferAndDrainOutputBuffer2(int type,
                                          int serial,
                                          int flags,
                                          unsigned char *encodedData,
                                          int size,
                                          long long int pts,
                                          long long int dts,
                                          long long int pos,
                                          long long int duration) {
    bool feedAndDrainRet = false;
    JNIEnv *bufferEnv;
    bool audioIsAttached = getEnv(&bufferEnv);
    if (bufferEnv != nullptr
        && ffmpegJavaObject != nullptr
        && feedInputBufferAndDrainOutputBufferMethodID2 != nullptr) {
        jbyteArray data = bufferEnv->NewByteArray(size);
        jbyte *elems = bufferEnv->GetByteArrayElements(data, NULL);
        memcpy(elems, encodedData, size);
        bufferEnv->ReleaseByteArrayElements(data, elems, 0);
        feedAndDrainRet = bufferEnv->CallBooleanMethod(ffmpegJavaObject,
                                                       feedInputBufferAndDrainOutputBufferMethodID2,
                                                       (jint) type,
                                                       (jint) serial,
                                                       (jint) flags,
                                                       data,
                                                       (jint) size,
                                                       (jlong) pts,
                                                       (jlong) dts,
                                                       (jlong) pos,
                                                       (jlong) duration);
        bufferEnv->DeleteLocalRef(data);

        /*jbyteArray data = bufferEnv->NewByteArray(size);
        bufferEnv->SetByteArrayRegion(
                data, 0, size, reinterpret_cast<const jbyte *>(encodedData));
        feedAndDrainRet = bufferEnv->CallBooleanMethod(ffmpegJavaObject,
                                                       feedInputBufferAndDrainOutputBufferMethodID2,
                                                       (jint) type,
                                                       (jint) serial,
                                                       (jint) flags,
                                                       data,
                                                       (jint) size,
                                                       (jlong) pts,
                                                       (jlong) dts,
                                                       (jlong) pos,
                                                       (jlong) duration);
        bufferEnv->DeleteLocalRef(data);*/
    }
    if (audioIsAttached) {
        gJavaVm->DetachCurrentThread();
    }
    return feedAndDrainRet;
}

void createAudioTrack(int sampleRateInHz,
                      int channelCount,
                      int audioFormat) {
    JNIEnv *audioEnv;
    bool isAttached = getEnv(&audioEnv);
    if (audioEnv != nullptr
        && ffmpegJavaObject != nullptr
        && createAudioTrackMethodID != nullptr) {
        audioEnv->CallVoidMethod(ffmpegJavaObject, createAudioTrackMethodID,
                                 (jint) sampleRateInHz, (jint) channelCount, (jint) audioFormat);
    }
    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
}

void write(unsigned char *pcmData,
           int offsetInBytes,
           int sizeInBytes) {
    JNIEnv *audioEnv;
    bool audioIsAttached = getEnv(&audioEnv);
    if (audioEnv != nullptr
        && ffmpegJavaObject != nullptr
        && writeMethodID != nullptr) {
        jbyteArray audioData = audioEnv->NewByteArray(sizeInBytes);

        // 第一种方式
        audioEnv->SetByteArrayRegion(
                audioData, 0, sizeInBytes, reinterpret_cast<const jbyte *>(pcmData));

        // 第二种方式
        // 拷贝数组需要对指针操作
        /*jbyte *cache = audioEnv->GetByteArrayElements(audioData, NULL);
        memcpy(cache, pcmData, (size_t) sizeInBytes);
        // 同步到audioData,并释放cache,与GetByteArrayElements对应
        // 如果不调用,audioData里面是空的,播放无声,并且会内存泄漏
        // 不能放到audioEnv->CallVoidMethod(...)的后面调用
        audioEnv->ReleaseByteArrayElements(audioData, cache, 0);*/

        // AudioTrack->write
        audioEnv->CallVoidMethod(ffmpegJavaObject, writeMethodID,
                                 audioData, (jint) offsetInBytes, (jint) sizeInBytes);

        // 释放局部引用,对应NewByteArray
        audioEnv->DeleteLocalRef(audioData);
    }
    if (audioIsAttached) {
        gJavaVm->DetachCurrentThread();
    }
}

void closeJni() {
    JNIEnv *env;
    bool isAttached = getEnv(&env);
    if (ffmpegJavaObject) {
        env->DeleteGlobalRef(ffmpegJavaObject);
        ffmpegJavaObject = nullptr;
    }
    if (callback_jobject) {
        env->DeleteGlobalRef(callback_jobject);
        callback_jobject = nullptr;
    }
    if (videoProducerObject) {
        env->DeleteGlobalRef(videoProducerObject);
        videoProducerObject = nullptr;
    }
    if (videoConsumerObject) {
        env->DeleteGlobalRef(videoConsumerObject);
        videoConsumerObject = nullptr;
    }
    if (audioProducerObject) {
        env->DeleteGlobalRef(audioProducerObject);
        audioProducerObject = nullptr;
    }
    if (audioConsumerObject) {
        env->DeleteGlobalRef(audioConsumerObject);
        audioConsumerObject = nullptr;
    }
    if (jniObject_jclass) {
        env->DeleteGlobalRef(jniObject_jclass);
        jniObject_jclass = nullptr;
    }
    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
}

void sleep(long ms) {
    JNIEnv *env;
    bool isAttached = getEnv(&env);
    if (env != nullptr
        && ffmpegJavaObject != nullptr
        && use_mode == USE_MODE_MEDIA_FFPLAY
        && sleepMethodID != nullptr) {
        env->CallVoidMethod(ffmpegJavaObject, sleepMethodID, (jlong) ms);
    }
    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
}

void postDelayed(int what, long long delayMillis) {
    JNIEnv *env;
    bool isAttached = getEnv(&env);
    if (env != nullptr
        && ffmpegJavaObject != nullptr
        && postDelayed_jmethodID != nullptr) {
        env->CallVoidMethod(ffmpegJavaObject, postDelayed_jmethodID, what, delayMillis);
    }
    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
}

// 回调java端FFMPEG类中的有关方法
int onLoadProgressUpdated(int code, int progress) {
    //LOGI("onLoadProgressUpdated() code: %d progress: %d\n", code, progress);
    JNIEnv *jniEnv;
    bool isAttached = getEnv(&jniEnv);
    if (jniEnv != nullptr
        && callback_jobject != nullptr
        && callback.onTransactMethodID != nullptr) {
        jobject *jniObject = nullptr;
        switch (code) {
            case MSG_ON_TRANSACT_VIDEO_PRODUCER:
                jniObject = &videoProducerObject;
                break;
            case MSG_ON_TRANSACT_VIDEO_CONSUMER:
                jniObject = &videoConsumerObject;
                break;
            case MSG_ON_TRANSACT_AUDIO_PRODUCER:
                jniObject = &audioProducerObject;
                break;
            case MSG_ON_TRANSACT_AUDIO_CONSUMER:
                jniObject = &audioConsumerObject;
                break;
            default:
                break;
        }
        if (jniObject) {
            jniEnv->SetIntField(*jniObject, valueInt_jfieldID, progress);
            jniEnv->CallIntMethod(
                    callback_jobject, callback.onTransactMethodID, code, *jniObject);
        }
    }
    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
    return 0;
}

void onReady() {
    LOGI("onReady()\n");
    JNIEnv *jniEnv;
    bool isAttached = getEnv(&jniEnv);
    if (jniEnv != nullptr
        && callback_jobject != nullptr
        && callback.onReadyMethodID != nullptr) {
        jniEnv->CallVoidMethod(callback_jobject, callback.onReadyMethodID);
    }
    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
}

void onChangeWindow(int width, int height) {
    LOGI("onChangeWindow()\n");
    JNIEnv *jniEnv;
    bool isAttached = getEnv(&jniEnv);
    if (jniEnv != nullptr
        && callback_jobject != nullptr
        && callback.onChangeWindowMethodID != nullptr) {
        jniEnv->CallVoidMethod(callback_jobject, callback.onChangeWindowMethodID,
                               (jint) width, (jint) height);
    }
    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
}

void onPlayed() {
    LOGI("onPlayed()\n");
    JNIEnv *jniEnv;
    bool isAttached = getEnv(&jniEnv);
    if (jniEnv != nullptr
        && callback_jobject != nullptr
        && callback.onPlayedMethodID != nullptr) {
        jniEnv->CallVoidMethod(callback_jobject, callback.onPlayedMethodID);
    }
    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
}

void onPaused() {
    LOGI("onPaused()\n");
    JNIEnv *jniEnv;
    bool isAttached = getEnv(&jniEnv);
    if (jniEnv != nullptr
        && callback_jobject != nullptr
        && callback.onPausedMethodID != nullptr) {
        jniEnv->CallVoidMethod(callback_jobject, callback.onPausedMethodID);
    }
    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
}

void onFinished() {
    LOGF("onFinished() start\n");
    JNIEnv *jniEnv;
    bool isAttached = getEnv(&jniEnv);
    //LOGF("onFinished() isAttached: %d\n", isAttached);
    if (jniEnv == nullptr) {
        LOGI("onFinished() jniEnv = nullptr\n");
    }
    if (callback_jobject == nullptr) {
        LOGI("onFinished() callbackJavaObject = nullptr\n");
    }
    if (callback.onFinishedMethodID == nullptr) {
        LOGI("onFinished() callback.onFinishedMethodID = nullptr\n");
    }
    if (jniEnv != nullptr
        && callback_jobject != nullptr
        && callback.onFinishedMethodID != nullptr) {
        //LOGF("onFinished() callback.onFinishedMethodID\n");
        jniEnv->CallVoidMethod(callback_jobject, callback.onFinishedMethodID);
    }
    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
    LOGF("onFinished() end\n");
    //closeJni();
}

void onProgressUpdated(long seconds) {
    JNIEnv *jniEnv;
    bool isAttached = getEnv(&jniEnv);
    if (jniEnv != nullptr
        && callback_jobject != nullptr
        && callback.onProgressUpdatedMethodID != nullptr) {
        jniEnv->CallVoidMethod(callback_jobject,
                               callback.onProgressUpdatedMethodID,
                               (jlong) seconds);
    }
    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
}

void onError(int error, char *errorInfo) {
    LOGE("jni onError() error: %d errorInfo: %s\n", error, errorInfo);
    JNIEnv *jniEnv;
    bool isAttached = getEnv(&jniEnv);
    if (jniEnv != nullptr
        && callback_jobject != nullptr
        && callback.onErrorMethodID != nullptr) {
        jniEnv->CallVoidMethod(callback_jobject, callback.onErrorMethodID,
                               error, jniEnv->NewStringUTF(errorInfo));
    }
    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
}

void onInfo(char *info) {
    JNIEnv *jniEnv;
    bool isAttached = getEnv(&jniEnv);
    if (jniEnv != nullptr
        && callback_jobject != nullptr
        && callback.onInfoMethodID != nullptr) {
        jniEnv->CallVoidMethod(callback_jobject,
                               callback.onInfoMethodID,
                               jniEnv->NewStringUTF(info));
    }
    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
}

/////////////////////////////////////////////////////////////////////////

/*static jint onTransact_download2(JNIEnv *env, jobject thiz, jint code, jobject jniObject) {
    if (use_mode != USE_MODE_MEDIA) {
        return -1;
    }

    jfieldID fieldID = env->GetFieldID(jniObjectClass, "valueStringArray", "[Ljava/lang/String;");
    jobjectArray objects = static_cast<jobjectArray>(env->GetObjectField(jniObject, fieldID));

    jstring element0 = static_cast<jstring>(env->GetObjectArrayElement(objects, 0));
    jstring element1 = static_cast<jstring>(env->GetObjectArrayElement(objects, 1));
    jstring element2 = static_cast<jstring>(env->GetObjectArrayElement(objects, 2));
    const char *flagStr = env->GetStringUTFChars(element0, 0);
    const char *filePath = env->GetStringUTFChars(element1, 0);
    const char *fileName = env->GetStringUTFChars(element2, 0);
    int flag = -1;
    if (!strcmp(flagStr, "0")) {
        flag = 0;
    } else if (!strcmp(flagStr, "1")) {
        flag = 1;
    } else if (!strcmp(flagStr, "2")) {
        flag = 2;
    } else if (!strcmp(flagStr, "3")) {
        flag = 3;
    } else if (!strcmp(flagStr, "4")) {
        flag = 4;
    } else if (!strcmp(flagStr, "5")) {
        flag = 5;
    }
    LOGI("onTransact() flag: %d path: %s fileName: %s\n", flag, filePath, fileName);

    alexander_media::download(flag, filePath, fileName);

    // release
    env->ReleaseStringUTFChars(element0, flagStr);
    env->ReleaseStringUTFChars(element1, filePath);
    env->ReleaseStringUTFChars(element2, fileName);
    env->DeleteLocalRef(objects);
    env->DeleteLocalRef(element0);
    env->DeleteLocalRef(element1);
    env->DeleteLocalRef(element2);

    return 0;
}*/

static int type;
static int COUNTS = 1;

static jint onTransact_init(JNIEnv *env, jobject ffmpegObject,
                            jint code, jobject jniObject) {
    if (jniObject_jclass != nullptr) {
        env->DeleteGlobalRef(jniObject_jclass);
        jniObject_jclass = nullptr;
    }
    jclass tempJniObjectClass = env->FindClass("com/weidi/media/wdplayer/util/JniObject");
    jniObject_jclass = reinterpret_cast<jclass>(env->NewGlobalRef(tempJniObjectClass));
    env->DeleteLocalRef(tempJniObjectClass);

    valueObject_jfieldID = env->GetFieldID(
            jniObject_jclass, "valueObject", "Ljava/lang/Object;");
    valueString_jfieldID = env->GetFieldID(
            jniObject_jclass, "valueString", "Ljava/lang/String;");
    valueInt_jfieldID = env->GetFieldID(
            jniObject_jclass, "valueInt", "I");
    valueLong_jfieldID = env->GetFieldID(
            jniObject_jclass, "valueLong", "J");
    valueByte_jfieldID = env->GetFieldID(
            jniObject_jclass, "valueByte", "B");
    valueBoolean_jfieldID = env->GetFieldID(
            jniObject_jclass, "valueBoolean", "Z");
    valueFloat_jfieldID = env->GetFieldID(
            jniObject_jclass, "valueFloat", "F");
    valueDouble_jfieldID = env->GetFieldID(
            jniObject_jclass, "valueDouble", "D");

    valueObjectArray_jfieldID = env->GetFieldID(
            jniObject_jclass, "valueObjectArray", "[Ljava/lang/Object;");
    valueStringArray_jfieldID = env->GetFieldID(
            jniObject_jclass, "valueStringArray", "[Ljava/lang/String;");
    valueIntArray_jfieldID = env->GetFieldID(
            jniObject_jclass, "valueIntArray", "[I");
    valueLongArray_jfieldID = env->GetFieldID(
            jniObject_jclass, "valueLongArray", "[J");
    valueByteArray_jfieldID = env->GetFieldID(
            jniObject_jclass, "valueByteArray", "[B");
    valueBooleanArray_jfieldID = env->GetFieldID(
            jniObject_jclass, "valueBooleanArray", "[Z");
    valueFloatArray_jfieldID = env->GetFieldID(
            jniObject_jclass, "valueFloatArray", "[F");
    valueDoubleArray_jfieldID = env->GetFieldID(
            jniObject_jclass, "valueDoubleArray", "[D");

    // presentationTimeUs_jfieldID
    tempJniObjectClass = env->FindClass("android/media/MediaCodec$BufferInfo");
    presentationTimeUs_jfieldID = env->GetFieldID(
            tempJniObjectClass, "presentationTimeUs", "J");
    env->DeleteLocalRef(tempJniObjectClass);

    /////////////////////////////////////////////////////////////////////////////////

    if (ffmpegJavaObject != nullptr) {
        env->DeleteGlobalRef(ffmpegJavaObject);
        ffmpegJavaObject = nullptr;
    }
    // java端FFMPEG类信息
    // java层native方法所对应的类对象
    // 在java层的native方法不是static的,因此需要用到java层的对象
    // ffmpegJavaObject = ffmpegObject;// error 直接赋值是不OK的
    ffmpegJavaObject = reinterpret_cast<jobject>(env->NewGlobalRef(ffmpegObject));
    jclass FFMPEGClass = env->GetObjectClass(ffmpegObject);
    //jclass FFMPEGClass = env->FindClass("com/weidi/usefragments/business/video_player/FFMPEG");
    //CHECK(FFMPEGClass != nullptr);
    // 第三个参数: 括号中是java端方法的参数签名,括号后面是java端方法的返回值签名(V表示void)
    initMediaCodecMethodID = env->GetMethodID(
            FFMPEGClass, "initMediaCodec", "(ILcom/weidi/media/wdplayer/util/JniObject;)Z");
    //FFMPEGClass, "initMediaCodec", "(ILcom/weidi/media/wdplayer/util/JniObject;)B");
    feedInputBufferAndDrainOutputBufferMethodID = env->GetMethodID(
            FFMPEGClass, "feedInputBufferAndDrainOutputBuffer", "(I[BIJ)Z");
    feedInputBufferAndDrainOutputBufferMethodID2 = env->GetMethodID(
            FFMPEGClass, "feedInputBufferAndDrainOutputBuffer2", "(III[BIJJJJ)Z");
    createAudioTrackMethodID = env->GetMethodID(
            FFMPEGClass, "createAudioTrack", "(III)V");
    writeMethodID = env->GetMethodID(
            FFMPEGClass, "write", "([BII)V");
    sleepMethodID = env->GetMethodID(
            FFMPEGClass, "sleep", "(J)V");
    postDelayed_jmethodID = env->GetMethodID(
            FFMPEGClass, "postDelayed", "(IJ)V");

    jobject jni_object;
    jfieldID fieldID;
    //
    if (videoProducerObject != nullptr) {
        env->DeleteGlobalRef(videoProducerObject);
        videoProducerObject = nullptr;
    }
    fieldID = env->GetStaticFieldID(FFMPEGClass, "videoProducer",
                                    "Lcom/weidi/media/wdplayer/util/JniObject;");
    jni_object = env->GetStaticObjectField(FFMPEGClass, fieldID);
    videoProducerObject = reinterpret_cast<jobject>(env->NewGlobalRef(jni_object));
    env->DeleteLocalRef(jni_object);
    //
    if (videoConsumerObject != nullptr) {
        env->DeleteGlobalRef(videoConsumerObject);
        videoConsumerObject = nullptr;
    }
    fieldID = env->GetStaticFieldID(FFMPEGClass, "videoConsumer",
                                    "Lcom/weidi/media/wdplayer/util/JniObject;");
    jni_object = env->GetStaticObjectField(FFMPEGClass, fieldID);
    videoConsumerObject = reinterpret_cast<jobject>(env->NewGlobalRef(jni_object));
    env->DeleteLocalRef(jni_object);
    //
    if (audioProducerObject != nullptr) {
        env->DeleteGlobalRef(audioProducerObject);
        audioProducerObject = nullptr;
    }
    fieldID = env->GetStaticFieldID(FFMPEGClass, "audioProducer",
                                    "Lcom/weidi/media/wdplayer/util/JniObject;");
    jni_object = env->GetStaticObjectField(FFMPEGClass, fieldID);
    audioProducerObject = reinterpret_cast<jobject>(env->NewGlobalRef(jni_object));
    env->DeleteLocalRef(jni_object);
    //
    if (audioConsumerObject != nullptr) {
        env->DeleteGlobalRef(audioConsumerObject);
        audioConsumerObject = nullptr;
    }
    fieldID = env->GetStaticFieldID(FFMPEGClass, "audioConsumer",
                                    "Lcom/weidi/media/wdplayer/util/JniObject;");
    jni_object = env->GetStaticObjectField(FFMPEGClass, fieldID);
    audioConsumerObject = reinterpret_cast<jobject>(env->NewGlobalRef(jni_object));
    env->DeleteLocalRef(jni_object);

    // 得到FFMPEG类中的mCallback属性
    if (callback_jobject != nullptr) {
        env->DeleteGlobalRef(callback_jobject);
        callback_jobject = nullptr;
    }
    jfieldID mCallback_jfieldID = env->GetFieldID(
            FFMPEGClass, "mCallback", "Lcom/weidi/media/wdplayer/util/Callback;");
    jobject temp_callback_jobject = env->GetObjectField(ffmpegJavaObject, mCallback_jfieldID);
    // 调用下面方法需要用到这个对象
    callback_jobject = reinterpret_cast<jobject>(env->NewGlobalRef(temp_callback_jobject));
    env->DeleteLocalRef(temp_callback_jobject);

    jclass callback_jclass = env->FindClass("com/weidi/media/wdplayer/util/Callback");
    // 第三个参数: 括号中是java端方法的参数签名,括号后面是java端方法的返回值签名(V表示void)
    callback.onTransactMethodID = env->GetMethodID(
            callback_jclass, "onTransact", "(ILcom/weidi/media/wdplayer/util/JniObject;)I");
    callback.onReadyMethodID = env->GetMethodID(
            callback_jclass, "onReady", "()V");
    callback.onChangeWindowMethodID = env->GetMethodID(
            callback_jclass, "onChangeWindow", "(II)V");
    callback.onPlayedMethodID = env->GetMethodID(
            callback_jclass, "onPlayed", "()V");
    callback.onPausedMethodID = env->GetMethodID(
            callback_jclass, "onPaused", "()V");
    callback.onFinishedMethodID = env->GetMethodID(
            callback_jclass, "onFinished", "()V");
    callback.onProgressUpdatedMethodID = env->GetMethodID(
            callback_jclass, "onProgressUpdated", "(J)V");
    callback.onErrorMethodID = env->GetMethodID(
            callback_jclass, "onError", "(ILjava/lang/String;)V");
    callback.onInfoMethodID = env->GetMethodID(
            callback_jclass, "onInfo", "(Ljava/lang/String;)V");
    env->DeleteLocalRef(callback_jclass);

    env->DeleteLocalRef(FFMPEGClass);
    FFMPEGClass = nullptr;

    return (jint) 0;
}

static jint onTransact_setMode(JNIEnv *env, jobject thiz,
                               jint code, jobject jniObject) {
    jint mode = env->GetIntField(jniObject, valueInt_jfieldID);

    use_mode = (int) mode;
    LOGI("setMode() use_mode: %d\n", use_mode);
    if (use_mode != USE_MODE_MEDIA
        && use_mode != USE_MODE_ONLY_VIDEO
        && use_mode != USE_MODE_ONLY_AUDIO
        && use_mode != USE_MODE_AUDIO_VIDEO
        && use_mode != USE_MODE_AAC_H264
        && use_mode != USE_MODE_MEDIA_4K
        && use_mode != USE_MODE_MEDIA_MEDIACODEC
        && use_mode != USE_MODE_MEDIA_FFPLAY) {
        use_mode = USE_MODE_MEDIA;
    }

    return (jint) 0;
}

static jint onTransact_setSurface(JNIEnv *env, jobject ffmpegObject,
                                  jint code, jobject jniObject) {

    jstring path = static_cast<jstring>(env->GetObjectField(jniObject, valueString_jfieldID));
    jobject surfaceObject = env->GetObjectField(jniObject, valueObject_jfieldID);

    // 路径
    const char *filePath = env->GetStringUTFChars(path, 0);
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            //alexander_media::setJniParameters(env, filePath, surfaceObject);
            break;
        }
        case USE_MODE_ONLY_VIDEO: {
            alexander_only_video::setJniParameters(env, filePath, surfaceObject);
            break;
        }
        case USE_MODE_ONLY_AUDIO: {
            alexander_only_audio::setJniParameters(env, filePath, NULL);
            break;
        }
        case USE_MODE_AUDIO_VIDEO: {
            alexander_audio_video::setJniParameters(env, filePath, surfaceObject);
            break;
        }
        case USE_MODE_AAC_H264: {
            alexander_aac_h264::setJniParameters(env, filePath, surfaceObject);
            break;
        }
        case USE_MODE_MEDIA_4K: {
            break;
        }
        case USE_MODE_MEDIA_MEDIACODEC: {
            alexander_media_mediacodec::setJniParameters(env, filePath, surfaceObject);
            break;
        }
        case USE_MODE_MEDIA_FFPLAY: {
            setDataSource(filePath);
            setSurface(env, surfaceObject);
            break;
        }
        default:
            break;
    }
    env->ReleaseStringUTFChars(path, filePath);
    env->DeleteLocalRef(surfaceObject);

    LOGI("setSurface()       runCount  : %d\n", (++runCount));

    return (jint) 0;
}

static jint onTransact_initPlayer(JNIEnv *env, jobject thiz,
                                  jint code, jobject jniObject) {
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            //return (jint) alexander_media::initPlayer();
            break;
        }
        case USE_MODE_ONLY_VIDEO: {
            return (jint) alexander_only_video::initPlayer();
        }
        case USE_MODE_ONLY_AUDIO: {
            return (jint) alexander_only_audio::initPlayer();
        }
        case USE_MODE_AUDIO_VIDEO: {
            return (jint) alexander_audio_video::initPlayer();
        }
        case USE_MODE_AAC_H264: {
            return (jint) alexander_aac_h264::initPlayer();
        }
        case USE_MODE_MEDIA_4K: {
        }
        case USE_MODE_MEDIA_MEDIACODEC: {
            return (jint) alexander_media_mediacodec::initPlayer();
        }
        case USE_MODE_MEDIA_FFPLAY: {
            return (jint) initPlayer();
        }
        default:
            break;
    }

    return (jint) - 1;
}

static jint onTransact_readData(JNIEnv *env, jobject thiz,
                                jint code, jobject jniObject) {
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            //alexander_media::readData(NULL);
            break;
        }
        case USE_MODE_ONLY_VIDEO: {
            alexander_only_video::readData(NULL);
            break;
        }
        case USE_MODE_ONLY_AUDIO: {
            alexander_only_audio::readData(NULL);
            break;
        }
        case USE_MODE_AUDIO_VIDEO: {
            if (COUNTS == 1) {
                type = TYPE_AUDIO;
                ++COUNTS;
                LOGI("readData() TYPE_AUDIO\n");
                alexander_audio_video::readData(&type);
            } else {
                type = TYPE_VIDEO;
                --COUNTS;
                LOGI("readData() TYPE_VIDEO\n");
                alexander_audio_video::readData(&type);
            }
            break;
        }
        case USE_MODE_AAC_H264: {
            if (COUNTS == 1) {
                type = TYPE_AUDIO;
                ++COUNTS;
                LOGI("readData() TYPE_AUDIO\n");
                alexander_aac_h264::readData(&type);
            } else {
                type = TYPE_VIDEO;
                --COUNTS;
                LOGI("readData() TYPE_VIDEO\n");
                alexander_aac_h264::readData(&type);
            }
            break;
        }
        case USE_MODE_MEDIA_4K: {
            break;
        }
        case USE_MODE_MEDIA_MEDIACODEC: {
            alexander_media_mediacodec::readData(NULL);
            break;
        }
        case USE_MODE_MEDIA_FFPLAY: {
            start();
            break;
        }
        default:
            break;
    }

    return (jint) 0;
}

static jint onTransact_audioHandleData(JNIEnv *env, jobject thiz,
                                       jint code, jobject jniObject) {
    int type = TYPE_AUDIO;
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            //alexander_media::handleData(&type);
            break;
        }
        case USE_MODE_ONLY_VIDEO: {
            //alexander_only_video::handleData(&type);
            break;
        }
        case USE_MODE_ONLY_AUDIO: {
            alexander_only_audio::handleData(&type);
            break;
        }
        case USE_MODE_AUDIO_VIDEO: {
            alexander_audio_video::handleData(&type);
            break;
        }
        case USE_MODE_AAC_H264: {
            alexander_aac_h264::handleData(&type);
            break;
        }
        case USE_MODE_MEDIA_4K: {
            break;
        }
        case USE_MODE_MEDIA_MEDIACODEC: {
            alexander_media_mediacodec::handleData(&type);
            break;
        }
        default:
            break;
    }

    return (jint) 0;
}

static jint onTransact_videoHandleData(JNIEnv *env, jobject thiz,
                                       jint code, jobject jniObject) {
    int type = TYPE_VIDEO;
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            //alexander_media::handleData(&type);
            break;
        }
        case USE_MODE_ONLY_VIDEO: {
            alexander_only_video::handleData(&type);
            break;
        }
        case USE_MODE_ONLY_AUDIO: {
            //alexander_only_audio::handleData(&type);
            break;
        }
        case USE_MODE_AUDIO_VIDEO: {
            alexander_audio_video::handleData(&type);
            break;
        }
        case USE_MODE_AAC_H264: {
            alexander_aac_h264::handleData(&type);
            break;
        }
        case USE_MODE_MEDIA_4K: {
            break;
        }
        case USE_MODE_MEDIA_MEDIACODEC: {
            alexander_media_mediacodec::handleData(&type);
            break;
        }
        default:
            break;
    }

    return (jint) 0;
}

static jint onTransact_videoHandleRender(JNIEnv *env, jobject thiz,
                                         jint code, jobject jniObject) {
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            break;
        }
        case USE_MODE_ONLY_VIDEO: {
            break;
        }
        case USE_MODE_ONLY_AUDIO: {
            break;
        }
        case USE_MODE_AUDIO_VIDEO: {
            break;
        }
        case USE_MODE_AAC_H264: {
            break;
        }
        case USE_MODE_MEDIA_4K: {
            break;
        }
        default:
            break;
    }
    return (jint) 0;
}

static jint onTransact_handleOutputBuffer(JNIEnv *env, jobject thiz,
                                          jint code, jobject jniObject) {
    jint handleRet = 0;
    jobject intArrayObject = env->GetObjectField(jniObject, valueIntArray_jfieldID);
    jobject longArrayObject = env->GetObjectField(jniObject, valueLongArray_jfieldID);
    // jobject objectArrayObject = env->GetObjectField(jniObject, valueObjectArray_jfieldID);
    jlong presentationTimeUs = env->GetLongField(jniObject, valueLong_jfieldID);
    if (intArrayObject != nullptr
        && longArrayObject != nullptr
        /*&& objectArrayObject != nullptr*/) {
        jint *intArray = reinterpret_cast<jint *>(
                env->GetIntArrayElements(
                        static_cast<jintArray>(intArrayObject), nullptr));
        jlong *longArray = reinterpret_cast<jlong *>(
                env->GetLongArrayElements(
                        static_cast<jlongArray>(longArrayObject), nullptr));
        int roomIndex = intArray[0];
        int offset = intArray[1];
        int size = intArray[2];
        int serial = intArray[3];

        long long int pts = longArray[0];
        long long int dts = longArray[1];
        long long int pos = longArray[2];
        long long int duration = longArray[3];

        // jobjectArray objectArray = reinterpret_cast<jobjectArray>(objectArrayObject);
        // ByteBuffer room
        // jobject room = static_cast<jobject>(env->GetObjectArrayElement(objectArray, 0));
        // MediaCodec.BufferInfo roomInfo
        // jobject roomInfo = static_cast<jobject>(env->GetObjectArrayElement(objectArray, 1));
        // uint8_t *data = (uint8_t *) env->GetDirectBufferAddress(room);

        switch (code) {
            case DO_SOMETHING_CODE_handleAudioOutputBuffer:
                switch (use_mode) {
                    case USE_MODE_MEDIA: {
                        break;
                    }
                    case USE_MODE_ONLY_VIDEO: {
                        break;
                    }
                    case USE_MODE_ONLY_AUDIO: {
                        handleRet = alexander_only_audio::handleAudioOutputBuffer(roomIndex);
                        break;
                    }
                    case USE_MODE_AUDIO_VIDEO: {
                        handleRet = alexander_audio_video::handleAudioOutputBuffer(roomIndex);
                        break;
                    }
                    case USE_MODE_AAC_H264: {
                        break;
                    }
                    case USE_MODE_MEDIA_4K: {
                        break;
                    }
                    case USE_MODE_MEDIA_MEDIACODEC: {
                        handleRet = alexander_media_mediacodec::handleAudioOutputBuffer(roomIndex);
                        break;
                    }
                    case USE_MODE_MEDIA_FFPLAY: {
                        break;
                    }
                    default:
                        break;
                }
                break;
            case DO_SOMETHING_CODE_handleVideoOutputBuffer:
                switch (use_mode) {
                    case USE_MODE_MEDIA: {
                        break;
                    }
                    case USE_MODE_ONLY_VIDEO: {
                        break;
                    }
                    case USE_MODE_ONLY_AUDIO: {
                        break;
                    }
                    case USE_MODE_AUDIO_VIDEO: {
                        handleRet = alexander_audio_video::handleVideoOutputBuffer(roomIndex);
                        break;
                    }
                    case USE_MODE_AAC_H264: {
                        break;
                    }
                    case USE_MODE_MEDIA_4K: {
                        break;
                    }
                    case USE_MODE_MEDIA_MEDIACODEC: {
                        handleRet = alexander_media_mediacodec::handleVideoOutputBuffer(roomIndex);
                        break;
                    }
                    case USE_MODE_MEDIA_FFPLAY: {
                        handleRet = decoder_decode_frame_by_mediacodec(roomIndex,
                                                                       offset,
                                                                       size,
                                                                       serial,
                                                                       presentationTimeUs,
                                                                       pts,
                                                                       dts,
                                                                       pos,
                                                                       duration,
                                                                       nullptr);
                        break;
                    }
                    default:
                        break;
                }
                break;
            default:
                break;
        }

        // release
        // env->DeleteLocalRef(room);
        // env->DeleteLocalRef(roomInfo);
        // env->DeleteLocalRef(objectArrayObject);
        env->DeleteLocalRef(intArrayObject);
        env->DeleteLocalRef(longArrayObject);
    }

    return handleRet;
}

static jint onTransact_play(JNIEnv *env, jobject thiz,
                            jint code, jobject jniObject) {
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            //alexander_media::play();
            break;
        }
        case USE_MODE_ONLY_VIDEO: {
            alexander_only_video::play();
            break;
        }
        case USE_MODE_ONLY_AUDIO: {
            alexander_only_audio::play();
            break;
        }
        case USE_MODE_AUDIO_VIDEO: {
            alexander_audio_video::play();
            break;
        }
        case USE_MODE_AAC_H264: {
            alexander_aac_h264::play();
            break;
        }
        case USE_MODE_MEDIA_4K: {
            break;
        }
        case USE_MODE_MEDIA_MEDIACODEC: {
            alexander_media_mediacodec::play();
            break;
        }
        case USE_MODE_MEDIA_FFPLAY: {
            play_pause();
            break;
        }
        default:
            break;
    }

    return (jint) 0;
}

static jint onTransact_pause(JNIEnv *env, jobject thiz,
                             jint code, jobject jniObject) {
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            //alexander_media::pause();
            break;
        }
        case USE_MODE_ONLY_VIDEO: {
            alexander_only_video::pause();
            break;
        }
        case USE_MODE_ONLY_AUDIO: {
            alexander_only_audio::pause();
            break;
        }
        case USE_MODE_AUDIO_VIDEO: {
            alexander_audio_video::pause();
            break;
        }
        case USE_MODE_AAC_H264: {
            alexander_aac_h264::pause();
            break;
        }
        case USE_MODE_MEDIA_4K: {
            break;
        }
        case USE_MODE_MEDIA_MEDIACODEC: {
            alexander_media_mediacodec::pause();
            break;
        }
        case USE_MODE_MEDIA_FFPLAY: {
            play_pause();
            break;
        }
        default:
            break;
    }

    return (jint) 0;
}

static jint onTransact_stop(JNIEnv *env, jobject thiz,
                            jint code, jobject jniObject) {
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            //alexander_media::stop();
            break;
        }
        case USE_MODE_ONLY_VIDEO: {
            alexander_only_video::stop();
            break;
        }
        case USE_MODE_ONLY_AUDIO: {
            alexander_only_audio::stop();
            break;
        }
        case USE_MODE_AUDIO_VIDEO: {
            alexander_audio_video::stop();
            break;
        }
        case USE_MODE_AAC_H264: {
            alexander_aac_h264::stop();
            break;
        }
        case USE_MODE_MEDIA_4K: {
            break;
        }
        case USE_MODE_MEDIA_MEDIACODEC: {
            alexander_media_mediacodec::stop();
            break;
        }
        case USE_MODE_MEDIA_FFPLAY: {
            stop();
            break;
        }
        default:
            break;
    }

    return (jint) 0;
}

static jint onTransact_release(JNIEnv *env, jobject thiz,
                               jint code, jobject jniObject) {
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            //alexander_media::release();
            break;
        }
        case USE_MODE_ONLY_VIDEO: {
            alexander_only_video::release();
            break;
        }
        case USE_MODE_ONLY_AUDIO: {
            alexander_only_audio::release();
            break;
        }
        case USE_MODE_AUDIO_VIDEO: {
            alexander_audio_video::release();
            break;
        }
        case USE_MODE_AAC_H264: {
            alexander_aac_h264::release();
            break;
        }
        case USE_MODE_MEDIA_4K: {
            break;
        }
        case USE_MODE_MEDIA_MEDIACODEC: {
            alexander_media_mediacodec::release();
            break;
        }
        case USE_MODE_MEDIA_FFPLAY: {
            release();
            break;
        }
        default:
            break;
    }

    return (jint) 0;
}

static jboolean onTransact_isRunning(JNIEnv *env, jobject thiz,
                                     jint code, jobject jniObject) {
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            //return (jboolean) alexander_media::isRunning();
            break;
        }
        case USE_MODE_ONLY_VIDEO: {
            return (jboolean) alexander_only_video::isRunning();
        }
        case USE_MODE_ONLY_AUDIO: {
            return (jboolean) alexander_only_audio::isRunning();
        }
        case USE_MODE_AUDIO_VIDEO: {
            return (jboolean) alexander_audio_video::isRunning();
        }
        case USE_MODE_AAC_H264: {
            return (jboolean) alexander_aac_h264::isRunning();
        }
        case USE_MODE_MEDIA_4K: {
            break;
        }
        case USE_MODE_MEDIA_MEDIACODEC: {
            return (jboolean) alexander_media_mediacodec::isRunning();
        }
        case USE_MODE_MEDIA_FFPLAY: {
            return (jboolean) isRunning();
        }
        default:
            break;
    }

    return (jboolean)
    false;
}

static jboolean onTransact_isPlaying(JNIEnv *env, jobject thiz,
                                     jint code, jobject jniObject) {
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            //return (jboolean) alexander_media::isPlaying();
            break;
        }
        case USE_MODE_ONLY_VIDEO: {
            return (jboolean) alexander_only_video::isPlaying();
        }
        case USE_MODE_ONLY_AUDIO: {
            return (jboolean) alexander_only_audio::isPlaying();
        }
        case USE_MODE_AUDIO_VIDEO: {
            return (jboolean) alexander_audio_video::isPlaying();
        }
        case USE_MODE_AAC_H264: {
            return (jboolean) alexander_aac_h264::isPlaying();
        }
        case USE_MODE_MEDIA_4K: {
            break;
        }
        case USE_MODE_MEDIA_MEDIACODEC: {
            return (jboolean) alexander_media_mediacodec::isPlaying();
        }
        case USE_MODE_MEDIA_FFPLAY: {
            return (jboolean) isPlaying();
        }
        default:
            break;
    }

    return (jboolean)
    false;
}

static jint onTransact_isPausedForUser(JNIEnv *env, jobject thiz,
                                       jint code, jobject jniObject) {
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            //return (jboolean) alexander_media::isPausedForUser();
            break;
        }
        case USE_MODE_ONLY_VIDEO: {
            return (jboolean) alexander_only_video::isPausedForUser();
        }
        case USE_MODE_ONLY_AUDIO: {
            return (jboolean) alexander_only_audio::isPausedForUser();
        }
        case USE_MODE_AUDIO_VIDEO: {
            return (jboolean) alexander_audio_video::isPausedForUser();
        }
        case USE_MODE_AAC_H264: {
            return (jboolean) alexander_aac_h264::isPausedForUser();
        }
        case USE_MODE_MEDIA_4K: {
            break;
        }
        case USE_MODE_MEDIA_MEDIACODEC: {
            return (jboolean) alexander_media_mediacodec::isPausedForUser();
        }
        default:
            break;
    }

    return (jboolean)
    false;
}

static jint onTransact_stepAdd(JNIEnv *env, jobject thiz,
                               jint code, jobject jniObject) {
    jlong addStep = env->GetLongField(jniObject, valueLong_jfieldID);

    switch (use_mode) {
        case USE_MODE_MEDIA: {
            //alexander_media::stepAdd((int64_t) addStep);
            break;
        }
        case USE_MODE_ONLY_VIDEO: {
            alexander_only_video::stepAdd((int64_t) addStep);
            break;
        }
        case USE_MODE_ONLY_AUDIO: {
            alexander_only_audio::stepAdd((int64_t) addStep);
            break;
        }
        case USE_MODE_AUDIO_VIDEO: {
            alexander_audio_video::stepAdd((int64_t) addStep);
            break;
        }
        case USE_MODE_AAC_H264: {
            alexander_aac_h264::stepAdd((int64_t) addStep);
            break;
        }
        case USE_MODE_MEDIA_4K: {
            break;
        }
        case USE_MODE_MEDIA_MEDIACODEC: {
            alexander_media_mediacodec::stepAdd((int64_t) addStep);
            break;
        }
        case USE_MODE_MEDIA_FFPLAY: {
            stepAdd((int64_t) addStep);
            break;
        }
        default:
            break;
    }

    return (jint) - 1;
}

static jint onTransact_stepSubtract(JNIEnv *env, jobject thiz,
                                    jint code, jobject jniObject) {
    jlong subtractStep = env->GetLongField(jniObject, valueLong_jfieldID);

    switch (use_mode) {
        case USE_MODE_MEDIA: {
            //alexander_media::stepSubtract((int64_t) subtractStep);
            break;
        }
        case USE_MODE_ONLY_VIDEO: {
            alexander_only_video::stepSubtract((int64_t) subtractStep);
            break;
        }
        case USE_MODE_ONLY_AUDIO: {
            alexander_only_audio::stepSubtract((int64_t) subtractStep);
            break;
        }
        case USE_MODE_AUDIO_VIDEO: {
            alexander_audio_video::stepSubtract((int64_t) subtractStep);
            break;
        }
        case USE_MODE_AAC_H264: {
            alexander_aac_h264::stepSubtract((int64_t) subtractStep);
            break;
        }
        case USE_MODE_MEDIA_4K: {
            break;
        }
        case USE_MODE_MEDIA_MEDIACODEC: {
            alexander_media_mediacodec::stepSubtract((int64_t) subtractStep);
            break;
        }
        case USE_MODE_MEDIA_FFPLAY: {
            stepSubtract((int64_t) subtractStep);
            break;
        }
        default:
            break;
    }

    return (jint) - 1;
}

static jint onTransact_seekTo(JNIEnv *env, jobject thiz,
                              jint code, jobject jniObject) {
    jlong timestamp = env->GetLongField(jniObject, valueLong_jfieldID);

    switch (use_mode) {
        case USE_MODE_MEDIA: {
            //return (jint) alexander_media::seekTo((int64_t) timestamp);
            break;
        }
        case USE_MODE_ONLY_VIDEO: {
            return (jint) alexander_only_video::seekTo((int64_t) timestamp);
        }
        case USE_MODE_ONLY_AUDIO: {
            return (jint) alexander_only_audio::seekTo((int64_t) timestamp);
        }
        case USE_MODE_AUDIO_VIDEO: {
            return (jint) alexander_audio_video::seekTo((int64_t) timestamp);
        }
        case USE_MODE_AAC_H264: {
            return (jint) alexander_aac_h264::seekTo((int64_t) timestamp);
        }
        case USE_MODE_MEDIA_4K: {
            break;
        }
        case USE_MODE_MEDIA_MEDIACODEC: {
            return (jint) alexander_media_mediacodec::seekTo((int64_t) timestamp);
        }
        case USE_MODE_MEDIA_FFPLAY: {
            return (jint) seekTo((int64_t) timestamp);
        }
        default:
            break;
    }

    return (jint) - 1;
}

static jlong onTransact_getDuration(JNIEnv *env, jobject thiz,
                                    jint code, jobject jniObject) {
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            //return (jlong) alexander_media::getDuration();
            break;
        }
        case USE_MODE_ONLY_VIDEO: {
            return (jlong) alexander_only_video::getDuration();
        }
        case USE_MODE_ONLY_AUDIO: {
            return (jlong) alexander_only_audio::getDuration();
        }
        case USE_MODE_AUDIO_VIDEO: {
            return (jlong) alexander_audio_video::getDuration();
        }
        case USE_MODE_AAC_H264: {
            return (jlong) alexander_aac_h264::getDuration();
        }
        case USE_MODE_MEDIA_4K: {
            break;
        }
        case USE_MODE_MEDIA_MEDIACODEC: {
            return (jlong) alexander_media_mediacodec::getDuration();
        }
        case USE_MODE_MEDIA_FFPLAY: {
            return (jlong) getDuration();
        }
        default:
            break;
    }

    return (jlong) - 1;
}

static jint onTransact_download(JNIEnv *env, jobject thiz,
                                jint code, jobject jniObject) {
    if (use_mode != USE_MODE_MEDIA_MEDIACODEC) {
        return (jint) - 1;
    }

    jint flag = env->GetIntField(jniObject, valueInt_jfieldID);
    jobject object = env->GetObjectField(jniObject, valueStringArray_jfieldID);
    if (object != nullptr) {
        jobjectArray objects = reinterpret_cast<jobjectArray>(object);
        //jsize length = env->GetArrayLength(objects);
        jstring element0 = static_cast<jstring>(env->GetObjectArrayElement(objects, 0));
        jstring element1 = static_cast<jstring>(env->GetObjectArrayElement(objects, 1));
        const char *filePath = env->GetStringUTFChars(element0, 0);
        const char *fileName = env->GetStringUTFChars(element1, 0);

        LOGI("onTransact_download() flag: %d path: %s fileName: %s\n", flag, filePath, fileName);
        alexander_media_mediacodec::download(flag, filePath, fileName);

        // release
        env->ReleaseStringUTFChars(element0, filePath);
        env->ReleaseStringUTFChars(element1, fileName);
        env->DeleteLocalRef(element0);
        env->DeleteLocalRef(element1);
        env->DeleteLocalRef(object);
    }

    return (jint) 0;
}

static jint onTransact_closeJni(JNIEnv *env, jobject thiz,
                                jint code, jobject jniObject) {
    closeJni();
}

/////////////////////////////////////////////////////////////////////////

char *getStrFromDO_SOMETHING_CODE(DO_SOMETHING_CODE code) {
    char info[100];
    memset(info, '\0', sizeof(info));
    switch (code) {
        case DO_SOMETHING_CODE_init:
            // return "DO_SOMETHING_CODE_init";
            strncpy(info, "DO_SOMETHING_CODE_init",
                    strlen("DO_SOMETHING_CODE_init"));
            break;
        case DO_SOMETHING_CODE_setMode:
            strncpy(info, "DO_SOMETHING_CODE_setMode",
                    strlen("DO_SOMETHING_CODE_setMode"));
            break;
        case DO_SOMETHING_CODE_setSurface:
            strncpy(info, "DO_SOMETHING_CODE_setSurface",
                    strlen("DO_SOMETHING_CODE_setSurface"));
            break;
        case DO_SOMETHING_CODE_initPlayer:
            strncpy(info, "DO_SOMETHING_CODE_initPlayer",
                    strlen("DO_SOMETHING_CODE_initPlayer"));
            break;
        case DO_SOMETHING_CODE_readData:
            strncpy(info, "DO_SOMETHING_CODE_readData",
                    strlen("DO_SOMETHING_CODE_readData"));
            break;
        case DO_SOMETHING_CODE_audioHandleData:
            strncpy(info, "DO_SOMETHING_CODE_audioHandleData",
                    strlen("DO_SOMETHING_CODE_audioHandleData"));
            break;
        case DO_SOMETHING_CODE_videoHandleData:
            strncpy(info, "DO_SOMETHING_CODE_videoHandleData",
                    strlen("DO_SOMETHING_CODE_videoHandleData"));
            break;
        case DO_SOMETHING_CODE_play:
            strncpy(info, "DO_SOMETHING_CODE_play",
                    strlen("DO_SOMETHING_CODE_play"));
            break;
        case DO_SOMETHING_CODE_pause:
            strncpy(info, "DO_SOMETHING_CODE_pause",
                    strlen("DO_SOMETHING_CODE_pause"));
            break;
        case DO_SOMETHING_CODE_stop:
            strncpy(info, "DO_SOMETHING_CODE_stop",
                    strlen("DO_SOMETHING_CODE_stop"));
            break;
        case DO_SOMETHING_CODE_release:
            strncpy(info, "DO_SOMETHING_CODE_release",
                    strlen("DO_SOMETHING_CODE_release"));
            break;
        case DO_SOMETHING_CODE_isRunning:
            strncpy(info, "DO_SOMETHING_CODE_isRunning",
                    strlen("DO_SOMETHING_CODE_isRunning"));
            break;
        case DO_SOMETHING_CODE_isPlaying:
            strncpy(info, "DO_SOMETHING_CODE_isPlaying",
                    strlen("DO_SOMETHING_CODE_isPlaying"));
            break;
        case DO_SOMETHING_CODE_isPausedForUser:
            strncpy(info, "DO_SOMETHING_CODE_isPausedForUser",
                    strlen("DO_SOMETHING_CODE_isPausedForUser"));
            break;
        case DO_SOMETHING_CODE_stepAdd:
            strncpy(info, "DO_SOMETHING_CODE_stepAdd",
                    strlen("DO_SOMETHING_CODE_stepAdd"));
            break;
        case DO_SOMETHING_CODE_stepSubtract:
            strncpy(info, "DO_SOMETHING_CODE_stepSubtract",
                    strlen("DO_SOMETHING_CODE_stepSubtract"));
            break;
        case DO_SOMETHING_CODE_seekTo:
            strncpy(info, "DO_SOMETHING_CODE_seekTo",
                    strlen("DO_SOMETHING_CODE_seekTo"));
            break;
        case DO_SOMETHING_CODE_getDuration:
            strncpy(info, "DO_SOMETHING_CODE_getDuration",
                    strlen("DO_SOMETHING_CODE_getDuration"));
            break;
        case DO_SOMETHING_CODE_download:
            strncpy(info, "DO_SOMETHING_CODE_download",
                    strlen("DO_SOMETHING_CODE_download"));
            break;
        case DO_SOMETHING_CODE_closeJni:
            strncpy(info, "DO_SOMETHING_CODE_closeJni",
                    strlen("DO_SOMETHING_CODE_closeJni"));
            break;
        case DO_SOMETHING_CODE_videoHandleRender:
            strncpy(info, "DO_SOMETHING_CODE_videoHandleRender",
                    strlen("DO_SOMETHING_CODE_videoHandleRender"));
            break;
        case DO_SOMETHING_CODE_handleAudioOutputBuffer:
            strncpy(info, "DO_SOMETHING_CODE_handleAudioOutputBuffer",
                    strlen("DO_SOMETHING_CODE_handleAudioOutputBuffer"));
            break;
        case DO_SOMETHING_CODE_handleVideoOutputBuffer:
            strncpy(info, "DO_SOMETHING_CODE_handleVideoOutputBuffer",
                    strlen("DO_SOMETHING_CODE_handleVideoOutputBuffer"));
            break;
        case DO_SOMETHING_CODE_frameByFrameForReady:
            strncpy(info, "DO_SOMETHING_CODE_frameByFrameForReady",
                    strlen("DO_SOMETHING_CODE_frameByFrameForReady"));
            break;
        case DO_SOMETHING_CODE_frameByFrameForFinish:
            strncpy(info, "DO_SOMETHING_CODE_frameByFrameForFinish",
                    strlen("DO_SOMETHING_CODE_frameByFrameForFinish"));
            break;
        case DO_SOMETHING_CODE_frameByFrame:
            strncpy(info, "DO_SOMETHING_CODE_frameByFrame",
                    strlen("DO_SOMETHING_CODE_frameByFrame"));
            break;
        case DO_SOMETHING_CODE_isWatch:
            strncpy(info, "DO_SOMETHING_CODE_isWatch",
                    strlen("DO_SOMETHING_CODE_isWatch"));
            break;
        case DO_SOMETHING_CODE_isWatchForCloseVideo:
            strncpy(info, "DO_SOMETHING_CODE_isWatchForCloseVideo",
                    strlen("DO_SOMETHING_CODE_isWatchForCloseVideo"));
            break;
        case DO_SOMETHING_CODE_isWatchForCloseAudio:
            strncpy(info, "DO_SOMETHING_CODE_isWatchForCloseAudio",
                    strlen("DO_SOMETHING_CODE_isWatchForCloseAudio"));
            break;
        case DO_SOMETHING_CODE_clearQueue:
            strncpy(info, "DO_SOMETHING_CODE_clearQueue",
                    strlen("DO_SOMETHING_CODE_clearQueue"));
            break;
        case DO_SOMETHING_CODE_postDelayed: {
            strncpy(info, "DO_SOMETHING_CODE_postDelayed",
                    strlen("DO_SOMETHING_CODE_postDelayed"));
            break;
        }
        default:
            strncpy(info, "DO_SOMETHING_CODE_nothing",
                    strlen("DO_SOMETHING_CODE_nothing"));
            break;
    }

    return info;
}

/***
 * jobject thiz 代表定义native方法的类的对象(如果native方法不是static的话)
 */
extern "C"
JNIEXPORT jstring

JNICALL
Java_com_weidi_media_wdplayer_video_1player_FFMPEG_onTransact(JNIEnv *env, jobject thiz,
                                                              jint code,
                                                              jobject jniObject) {
    // TODO: implement onTransact()
    if (code != DO_SOMETHING_CODE_handleAudioOutputBuffer
        && code != DO_SOMETHING_CODE_handleVideoOutputBuffer) {
        LOGI("onTransact() %s\n",
             getStrFromDO_SOMETHING_CODE(static_cast<DO_SOMETHING_CODE>(code)));
    }
    const char ret[] = "0";
    switch (code) {
        case DO_SOMETHING_CODE_init:
            return env->NewStringUTF(
                    std::to_string(onTransact_init(env, thiz, code, jniObject)).c_str());

        case DO_SOMETHING_CODE_setMode:
            return env->NewStringUTF(
                    std::to_string(onTransact_setMode(env, thiz, code, jniObject)).c_str());

        case DO_SOMETHING_CODE_setSurface:
            return env->NewStringUTF(
                    std::to_string(onTransact_setSurface(env, thiz, code, jniObject)).c_str());

        case DO_SOMETHING_CODE_initPlayer:
            return env->NewStringUTF(
                    std::to_string(onTransact_initPlayer(env, thiz, code, jniObject)).c_str());

        case DO_SOMETHING_CODE_readData:
            onTransact_readData(env, thiz, code, jniObject);
            return env->NewStringUTF(ret);

        case DO_SOMETHING_CODE_audioHandleData:
            onTransact_audioHandleData(env, thiz, code, jniObject);
            return env->NewStringUTF(ret);

        case DO_SOMETHING_CODE_videoHandleData:
            onTransact_videoHandleData(env, thiz, code, jniObject);

            return env->NewStringUTF(ret);

        case DO_SOMETHING_CODE_play:
            return env->NewStringUTF(
                    std::to_string(onTransact_play(env, thiz, code, jniObject)).c_str());

        case DO_SOMETHING_CODE_pause:
            return env->NewStringUTF(
                    std::to_string(onTransact_pause(env, thiz, code, jniObject)).c_str());

        case DO_SOMETHING_CODE_stop:
            return env->NewStringUTF(
                    std::to_string(onTransact_stop(env, thiz, code, jniObject)).c_str());

        case DO_SOMETHING_CODE_release:
            return env->NewStringUTF(
                    std::to_string(onTransact_release(env, thiz, code, jniObject)).c_str());

        case DO_SOMETHING_CODE_isRunning:
            return (onTransact_isRunning(env, thiz, code, jniObject)
                    ? env->NewStringUTF("true") : env->NewStringUTF("false"));

        case DO_SOMETHING_CODE_isPlaying:
            return (onTransact_isPlaying(env, thiz, code, jniObject)
                    ? env->NewStringUTF("true") : env->NewStringUTF("false"));

        case DO_SOMETHING_CODE_isPausedForUser:
            return (onTransact_isPausedForUser(env, thiz, code, jniObject)
                    ? env->NewStringUTF("true") : env->NewStringUTF("false"));

        case DO_SOMETHING_CODE_stepAdd:
            return env->NewStringUTF(
                    std::to_string(onTransact_stepAdd(env, thiz, code, jniObject)).c_str());

        case DO_SOMETHING_CODE_stepSubtract:
            return env->NewStringUTF(
                    std::to_string(onTransact_stepSubtract(env, thiz, code, jniObject)).c_str());

        case DO_SOMETHING_CODE_seekTo:
            return env->NewStringUTF(
                    std::to_string(onTransact_seekTo(env, thiz, code, jniObject)).c_str());

        case DO_SOMETHING_CODE_getDuration:
            return env->NewStringUTF(
                    std::to_string(onTransact_getDuration(env, thiz, code, jniObject)).c_str());

        case DO_SOMETHING_CODE_download:
            return env->NewStringUTF(
                    std::to_string(onTransact_download(env, thiz, code, jniObject)).c_str());

        case DO_SOMETHING_CODE_closeJni:
            return env->NewStringUTF(ret);

        case DO_SOMETHING_CODE_videoHandleRender:
            onTransact_videoHandleRender(env, thiz, code, jniObject);
            return env->NewStringUTF(ret);

        case DO_SOMETHING_CODE_handleAudioOutputBuffer:
        case DO_SOMETHING_CODE_handleVideoOutputBuffer:
            return env->NewStringUTF(
                    std::to_string(
                            onTransact_handleOutputBuffer(env, thiz, code, jniObject)).c_str());

        case DO_SOMETHING_CODE_frameByFrameForReady:
            return (alexander_media_mediacodec::frameByFrameForReady()
                    ? env->NewStringUTF("true") : env->NewStringUTF("false"));
        case DO_SOMETHING_CODE_frameByFrameForFinish:
            return (alexander_media_mediacodec::frameByFrameForFinish()
                    ? env->NewStringUTF("true") : env->NewStringUTF("false"));
        case DO_SOMETHING_CODE_frameByFrame:
            return (alexander_media_mediacodec::frameByFrame()
                    ? env->NewStringUTF("true") : env->NewStringUTF("false"));
        case DO_SOMETHING_CODE_isWatch: {
            jboolean iswatch = env->GetBooleanField(jniObject, valueBoolean_jfieldID);
            isWatch = iswatch;
            return env->NewStringUTF(ret);
        }
        case DO_SOMETHING_CODE_isWatchForCloseVideo: {
            if (isWatchForCloseVideo) {
                isWatchForCloseVideo = false;
                onInfo("需要视频");
            } else {
                isWatchForCloseVideo = true;
                onInfo("不要视频");
            }
            return env->NewStringUTF(ret);
        }
        case DO_SOMETHING_CODE_isWatchForCloseAudio: {
            if (isWatchForCloseAudio) {
                isWatchForCloseAudio = false;
                onInfo("需要音频");
            } else {
                isWatchForCloseAudio = true;
                onInfo("不要音频");
            }
            return env->NewStringUTF(ret);
        }
        case DO_SOMETHING_CODE_setTimeDifference: {
            jdouble time_difference = env->GetDoubleField(jniObject, valueDouble_jfieldID);
            TIME_STEP = time_difference;
            return env->NewStringUTF(ret);
        }
        case DO_SOMETHING_CODE_setRemainingTime: {
            jdouble remaining_time = env->GetDoubleField(jniObject, valueDouble_jfieldID);
            REMAINING_TIME = remaining_time;
            return env->NewStringUTF(ret);
        }
        case DO_SOMETHING_CODE_clearQueue: {
            clearQueue();
            return env->NewStringUTF(ret);
        }
        case DO_SOMETHING_CODE_postDelayed: {
            onEvent(env->GetIntField(jniObject, valueInt_jfieldID));
            return env->NewStringUTF(ret);
        }

        default:
            break;
    }

    return env->NewStringUTF("-1");
}


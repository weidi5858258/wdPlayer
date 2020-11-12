//
// Created by root on 19-8-8.
//








/***
usleep(1000*40);//等待40毫秒
av_gettime_relative() 单位:微秒

AAC 128kbps
60fps
4320P 7680 x 4320 8K
2160P 3840 x 2160 UHD 4K 超高清
1440P 2560 x 1440 QHD 2K
1080P 1920 x 1080 FHD    全高清
 720P 1280 x 720   HD
 480P  854 x 480
 360P
 240P
 144P

4：3宽高比分辨率：640×480,800×600,960×720,1024×768,1280×960,1400×1050,1440×1080,1600×1200,1856×1392,1920×1440和2048× 1536。
16:10宽高比分辨率：1280×800,1440×900,1680×1050,1920×1200和2560×1600。
16：9宽高比分辨率：1024×576,1152×648,1280×720（HD），1366×768,1600×900,1920×1080（FHD），2560×1440,3880×2160（4K）和7680 x 4320（8K）。

NV21(yuv420sp)和I420(yuv420p)
NV21的排列是YYYYYYYY VUVU  -> YUV420SP
I420的排列是YYYYYYYY UU VV -> YUV420P
ffmpeg中jpeg编码输入要求是YUVJ420P格式.
1.YUV420P to YUVJ420P
2.YUVJ420P to jpeg

在FFMPEG中，图像原始数据包括两种：planar和packed。
planar就是将几个分量分开存，比如YUV420中，data[0]专门存Y，data[1]专门存U，data[2]专门存V。
而packed则是打包存，所有数据都存在data[0]中。

大多数图像处理软件在处理时是需要RGB格式的图像.
默认的视频流是压缩的YUV格式，Android下是YUV420SP.
Android提供的SurfaceView、GLSurfaceView、TextureView等控件只支持RGB格式的渲染.
RGB 转换成 YUV
Y=Y  = (0.257 * R) + (0.504 * G) + (0.098 * B) + 16
V=Cr =  (0.439 * R) - (0.368 * G) - (0.071 * B) + 128
U=Cb =  -( 0.148 * R) - (0.291 * G) + (0.439 * B) + 128
YUV 转换成 RGB
R = 1.164 (Y - 16)  +  1.596 (V  -  128)
G = 1.164 (Y - 16)  -  0.813 (V  -  128)  -  0.391 (U  -  128)
B = 1.164 (Y - 16)  +  2.018 (U  -  128)

R=Y+1.4075*(V-128)
G=Y-0.3455*(U-128) – 0.7169*(V-128)
B=Y+1.779*(U-128)

public native int[] decodeYUV420SP(byte[] buf, int width, int height);
返回的结果是一个ARGB_8888格式的颜色数组.
mBitmap = Bitmap.createBitmap(data, width, height, Config.ARGB_8888);

保存YUV420P格式的数据，用以下代码：
fwrite(pFrameYUV->data[0],(pCodecCtx->width)*(pCodecCtx->height),1,output);
fwrite(pFrameYUV->data[1],(pCodecCtx->width)*(pCodecCtx->height)/4,1,output);
fwrite(pFrameYUV->data[2],(pCodecCtx->width)*(pCodecCtx->height)/4,1,output);
保存RGB24格式的数据，用以下代码：
fwrite(pFrameYUV->data[0],(pCodecCtx->width)*(pCodecCtx->height)*3,1,output);
保存UYVY格式的数据，用以下代码：
fwrite(pFrameYUV->data[0],(pCodecCtx->width)*(pCodecCtx->height),2,output);

 https://www.cnblogs.com/azraelly/archive/2013/01/01/2841269.html
 图文详解YUV420数据格式
 https://blog.csdn.net/cgwang_1580/article/details/79595958
 常用视频像素格式NV12、NV2、I420、YV12、YUYV

 int av_image_get_buffer_size(enum AVPixelFormat pix_fmt, int width, int height, int align);
 函数的作用是通过指定像素格式、图像宽、图像高来计算所需的内存大小
 重点说明一个参数align:此参数是设定内存对齐的对齐数,也就是按多大的字节进行内存对齐.
 比如设置为1,表示按1字节对齐,那么得到的结果就是与实际的内存大小一样.
 再比如设置为4,表示按4字节对齐.也就是内存的起始地址必须是4的整倍数.

 int av_image_alloc(
     uint8_t *pointers[4],
     int linesizes[4],
     int w,
     int h,
     enum AVPixelFormat pix_fmt,
     int align);
 pointers[4]：保存图像通道的地址.
 如果是RGB,则前三个指针分别指向R,G,B的内存地址,第四个指针保留不用.
 linesizes[4]：保存图像每个通道的内存对齐的步长,即一行的对齐内存的宽度,此值大小等于图像宽度.
 w:       要申请内存的图像宽度.
 h:       要申请内存的图像高度.
 pix_fmt: 要申请内存的图像的像素格式.
 align:   用于内存对齐的值.
 返回值：所申请的内存空间的总大小.如果是负值,表示申请失败.

 int av_image_fill_arrays(
     uint8_t *dst_data[4],
     int dst_linesize[4],
     const uint8_t *src,
     enum AVPixelFormat pix_fmt,
     int width,
     int height,
     int align);
 av_image_fill_arrays()函数自身不具备内存申请的功能,
 此函数类似于格式化已经申请的内存,即通过av_image_alloc()函数申请的内存空间.
 dst_data[4]:     [out]对申请的内存格式化为三个通道后,分别保存其地址.
 dst_linesize[4]: [out]格式化的内存的步长(即内存对齐后的宽度).
 *src:            [in]av_image_alloc()函数申请的内存地址.
 pix_fmt:         [in]申请 src内存时的像素格式.
 width:           [in]申请src内存时指定的宽度.
 height:          [in]申请scr内存时指定的高度.
 align:           [in]申请src内存时指定的对齐字节数.

int sws_scale(struct SwsContext *c, const uint8_t *const srcSlice[],
                                          const int srcStride[], int srcSliceY, int srcSliceH,
                                          uint8_t *const dst[], const int dstStride[]);
// c参数指定使用包含图像数据格式转换信息的struct SwsContext对象
// srcSlice参数用于指定的原图像数据缓冲区地址
// srcStride参数用于指定原数据缓冲区一行数据的大小
// srcSliceY参数用于指定从原图像的第几行开始转换
// srcSliceH参数用于指定转换到原图像的第几行.
// dst参数用于指定存放生成图像数据的缓冲区地址
// dstStride参数指定存放生成图像的一行数据大小的缓冲区

typedef struct AVFrame {
    #define AV_NUM_DATA_POINTERS 8
    // 此指针数组是用于存放数据缓冲区地址，因有可能是平面的数据，所有用了多个指针变量存放不同分量的数据缓冲区
    uint8_t *data[AV_NUM_DATA_POINTERS];
    // 存放每个缓冲区的一行数据的字节数
    int linesize[AV_NUM_DATA_POINTERS];
    ...
}AVFrame

yuv422转换成420p:
1. 创建AVFrame对象
    AVFrame  *frm422 = av_frame_alloc();
    AVFrame  *frm420p = av_frame_alloc();
2. 绑定数据缓冲区
    av_image_fill_arrays(frm422->data, frm422->linesize, buf_422, AV_PIX_FMT_YUYV422, w, h, 16);
    av_image_fill_arrays(frm420p->data, frm420p->linesize, buf_420p, AV_PIX_FMT_YUV420P, w, h, 16);
3. 指定原数据格式，分辨率及目标数据格式，分辨率
    struct SwsContext *sws = sws_getContext(w, h, AV_PIX_FMT_YUYV422,
                                            w,h, AV_PIX_FMT_YUV420P,
                                            SWS_BILINEAR,
                                            nullptr, nullptr, nullptr);
4. 转换并调整分辨率
    int ret = sws_scale(sws, frm422->data, frm422->linesize, 0, h, frm420p->data, frm420p->linesize);
5. 回收空间
    av_frame_free(&frm422);
    av_frame_free(&frm420p);
    sws_freeContext(sws);

argb转换成yuv420p:

int MyDataCovert::argb32Toyuv420p(uint8_t *buf_argb, uint8_t *buf_420p, int w, int h)
{
    AVFrame  *frmArgb = av_frame_alloc();
    AVFrame  *frm420p = av_frame_alloc();

    //绑定数据缓冲区
    avpicture_fill((AVPicture *)frmArgb, buf_argb, AV_PIX_FMT_BGRA, w, h);
    avpicture_fill((AVPicture *)frm420p, buf_420p, AV_PIX_FMT_YUV420P, w, h);

    //指定原数据格式，分辨率及目标数据格式，分辨率
    struct SwsContext *sws = sws_getContext(w, h, AV_PIX_FMT_BGRA,
                                            w,h, AV_PIX_FMT_YUV420P,
                                            SWS_BILINEAR,
                                            nullptr, nullptr, nullptr);

    //转换
    int ret = sws_scale(sws, frmArgb->data, frmArgb->linesize, 0, h, frm420p->data, frm420p->linesize);
    av_frame_free(&frmArgb);
    av_frame_free(&frm420p);
    sws_freeContext(sws);
    return  (ret == h) ? 0 : -1;
}

// 在jni层或者其他cpp文件中创建线程是不行的
pthread_t audioReadDataThread, audioHandleDataThread;
// 创建线程
pthread_create(&audioReadDataThread, nullptr, readData, audioWrapper->father);
pthread_create(&audioHandleDataThread, nullptr, handleAudioData, nullptr);
// 等待线程执行完
pthread_join(audioReadDataThread, nullptr);
pthread_join(audioHandleDataThread, nullptr);
// 取消线程
//pthread_cancel(audioReadDataThread);
//pthread_cancel(audioHandleDataThread);

pthread_t videoReadDataThread, videoHandleDataThread;
// 创建线程
pthread_create(&videoReadDataThread, nullptr, readData, videoWrapper->father);
pthread_create(&videoHandleDataThread, nullptr, handleVideoData, nullptr);
// 等待线程执行完
pthread_join(videoReadDataThread, nullptr);
pthread_join(videoHandleDataThread, nullptr);
// 取消线程
//pthread_cancel(videoReadDataThread);
//pthread_cancel(videoHandleDataThread);

适合播放对象:
fps不大于30,kbps为0或者小于4000,最大分辨率为1080P
 */

#include <stdlib.h>
#include <string>
#include "MediaPlayerForMediaCodec.h"

#define LOG "player_alexander_media_mediacodec"

char inFilePath[2048];
AVFormatContext *avFormatContext = nullptr;
struct AudioWrapper *audioWrapper = nullptr;
struct VideoWrapper *videoWrapper = nullptr;
pthread_mutex_t readLockMutex = PTHREAD_MUTEX_INITIALIZER;
pthread_cond_t readLockCondition = PTHREAD_COND_INITIALIZER;
// 是否是本地文件
bool isLocal = false;
bool isLive = false;
// 不在这个文件中使用,在OnlyVideoPlayer.cpp中使用
bool isH264 = false;
bool isReading = false;
bool isReadWaited = false;
bool isVideoHandling = false;
//bool isVideoRendering = false;
bool isInterrupted = false;
bool runOneTime = true;
bool videoDisable = false;
bool audioDisable = false;
double fileLength = 0.0;
// seek时间
int64_t timeStamp = -1;
long long curProgress = 0;
long long preProgress = 0;
// 视频播放时每帧之间的暂停时间,单位为us
int videoSleepTime = 11000;

double TIME_DIFFERENCE = 1.000000;// 0.180000
// 当前音频时间戳
double audioPts = 0.0;
// 当前视频时间戳
double videoPts = 0.0;
// 上一个时间戳
double preAudioPts = 0.0;
double preVideoPts = 0.0;
bool isWatch = false;
bool isWatchForCloseVideo = false;
bool isWatchForCloseAudio = false;

ANativeWindow *pANativeWindow = nullptr;

extern int use_mode;

namespace alexander_media_mediacodec {

    // 绘制时的缓冲区
    static ANativeWindow_Buffer mANativeWindow_Buffer;

    static int runCounts = 0;
    static double averageTimeDiff = 0;
    static double timeDiff[RUN_COUNTS];
    static int averageTimeDiffCount = 0;
    static bool needToGetResultAgain = true;
    static bool needToResetVideoPts = false;
    static bool needToResetVideoPts2 = false;

    // 单位: 秒
    static long long mediaDuration = -1;
    // 读线程超时变量
    static int64_t startReadTime = -1;
    static int64_t endReadTime = -1;

    static int frameRate = 0;
    // Duration: 01:27:51.64, start: 0.000000, bitrate: 1666 kb/s
    static int64_t bitRate = 0;
    // Stream #0:1: Video: rv40 (RV40 / 0x30345652), yuv420p, 1280x720, 1506 kb/s, 23.98 fps, 23.98 tbr, 1k tbn, 1k tbc
    static int64_t bit_rate_video = 0;
    static int64_t bit_rate_audio = 0;

    // true时表示一帧一帧的看画面
    static bool isFrameByFrameMode = false;

    ///////////////////////////////////////////////////////

    // true表示只下载,不播放
    static bool onlyDownloadNotPlayback = false;
    static bool needToDownload = false;
    static bool isInitSuccess = false;
    // 下载时音视频为一个文件
    static char outFilePath[2048];
    static AVFormatContext *avFormatContextOutput = nullptr;
    static int64_t *dts_start_from = nullptr;
    static int64_t *pts_start_from = nullptr;
    // 下载时音视频分开
    static char videoOutFilePath[2048];// 2048
    static char audioOutFilePath[2048];// 2048
    static AVFormatContext *avFormatContextVideoOutput = nullptr;
    static AVFormatContext *avFormatContextAudioOutput = nullptr;
    static AVStream *video_out_stream = nullptr;
    static AVStream *audio_out_stream = nullptr;
    //
    static FILE *videoFile = nullptr;
    static FILE *audioFile = nullptr;

    ///////////////////////////////////////////////////////

    char *getStrAVPixelFormat(AVPixelFormat format);

    void closeDownload();

    void closeOther();

    // 基本形式为strcmp(str1,str2)
    // 若str1=str2，则返回零；
    // 若str1<str2，则返回负数；
    // 若str1>str2，则返回正数。
    int startsWith(const char *str1, char *str2) {
        if (str1 == nullptr || str2 == nullptr) {
            return false;
        }
        int len1 = strlen(str1);
        int len2 = strlen(str2);
        if (len1 < len2 || len1 == 0 || len2 == 0) {
            return false;
        }
        char temp[len2 + 1];
        memset(temp, '\0', sizeof(temp));
        strncpy(temp, str1, len2);
        int ret = strcmp(temp, str2);
        if (!ret) {
            return true;
        } else {
            return false;
        }
    }

    bool endsWith(const char *str1, char *str2) {
        if (str1 == nullptr || str2 == nullptr) {
            return false;
        }
        int len1 = strlen(str1);
        int len2 = strlen(str2);
        if (len1 < len2 || len1 == 0 || len2 == 0) {
            return false;
        }
        char temp[len2 + 1];
        strncpy(temp, str1 + (len1 - len2), len2);
        int ret = strcmp(temp, str2);
        if (!ret) {
            return true;
        } else {
            return false;
        }
    }

    /***
     在Android logcat中打印FFmpeg调试信息
     https://zhuanlan.zhihu.com/p/48384062
     FFmpeg日志输出到adb logcat
     https://blog.csdn.net/matrix_laboratory/article/details/57080891
     */
    static void log_callback(void *ptr, int level, const char *fmt, va_list vl) {
        static int print_prefix = 1;
        static char prev[1024];
        char line[1024];

        av_log_format_line(ptr, level, fmt, vl, line, sizeof(line), &print_prefix);

        strcpy(prev, line);
        // sanitize((uint8_t *)line);

        if (level <= AV_LOG_WARNING) {
            LOGE("%s", line);
        } else {
            LOGI("%s", line);
        }
    }

    static void log_callback_null(void *ptr, int level, const char *fmt, va_list vl) {

    }

    void notifyToRead() {
        pthread_mutex_lock(&readLockMutex);
        pthread_cond_signal(&readLockCondition);
        pthread_mutex_unlock(&readLockMutex);
    }

    void notifyToReadWait() {
        pthread_mutex_lock(&readLockMutex);
        pthread_cond_wait(&readLockCondition, &readLockMutex);
        pthread_mutex_unlock(&readLockMutex);
    }

    // 通知读线程开始读(其中有一个队列空的情况)
    void notifyToRead(Wrapper *wrapper) {
        pthread_mutex_lock(&wrapper->readLockMutex);
        pthread_cond_signal(&wrapper->readLockCondition);
        pthread_mutex_unlock(&wrapper->readLockMutex);
    }

    // 通知读线程开始等待(队列都满的情况)
    void notifyToReadWait(Wrapper *wrapper) {
        pthread_mutex_lock(&wrapper->readLockMutex);
        pthread_cond_wait(&wrapper->readLockCondition, &wrapper->readLockMutex);
        pthread_mutex_unlock(&wrapper->readLockMutex);
    }

    // 通知处理线程开始处理(几种情况需要唤醒:开始播放时,cache缓存时)
    void notifyToHandle(Wrapper *wrapper) {
        pthread_mutex_lock(&wrapper->handleLockMutex);
        pthread_cond_signal(&wrapper->handleLockCondition);
        pthread_mutex_unlock(&wrapper->handleLockMutex);
    }

    // 通知处理线程开始等待
    void notifyToHandleWait(Wrapper *wrapper) {
        pthread_mutex_lock(&wrapper->handleLockMutex);
        pthread_cond_wait(&wrapper->handleLockCondition, &wrapper->handleLockMutex);
        pthread_mutex_unlock(&wrapper->handleLockMutex);
    }

    int read_thread_interrupt_cb(void *opaque) {
        if (isInterrupted) {
            LOGE("read_thread_interrupt_cb() 1 退出\n");
            return 1;
        }
        if (audioWrapper == nullptr
            || audioWrapper->father == nullptr
            || videoWrapper == nullptr
            || videoWrapper->father == nullptr) {
            LOGE("read_thread_interrupt_cb() 2 退出\n");
            return 0;
        }
        // 必须通过传参方式进行判断,不能用全局变量判断
        AudioWrapper *audioWrapper = (AudioWrapper *) opaque;
        endReadTime = av_gettime_relative();
        if (!audioDisable && !audioWrapper->father->isReading) {
            LOGE("read_thread_interrupt_cb() 3 退出\n");
            isInterrupted = true;
            return 1;
        } else if (
                (
                        (!audioDisable
                         && (audioWrapper->father->isPausedForCache ||
                             !audioWrapper->father->isStarted))
                        ||
                        (!videoDisable
                         && (videoWrapper->father->isPausedForCache ||
                             !videoWrapper->father->isStarted))
                )
                && isReading
                && startReadTime > 0
                && (endReadTime - startReadTime) > MAX_RELATIVE_TIME) {
            /*if (audioWrapper->father->list1->size() < audioWrapper->father->list1LimitCounts
                && videoWrapper->father->list1->size() < videoWrapper->father->list1LimitCounts
                && audioWrapper->father->list2->size() < audioWrapper->father->list1LimitCounts
                && videoWrapper->father->list2->size() < videoWrapper->father->list1LimitCounts) {
            }*/
            LOGE("read_thread_interrupt_cb() 读取数据超时\n");
            isInterrupted = true;
            onError(0x101, "读取数据超时");
            return 1;
        } else if ((endReadTime - startReadTime) > MAX_RELATIVE_TIME_OUT) {
            LOGE("read_thread_interrupt_cb() 读取数据超时了\n");
            isInterrupted = true;
            onError(0x101, "读取数据超时了");
            if (!isReading) {
                stop();
            }
            return 1;
        }
        return 0;
    }

    // 已经不需要调用了
    void initAV() {
        av_register_all();
        // 用于从网络接收数据,如果不是网络接收数据,可不用（如本例可不用）
        avcodec_register_all();

        //avdevice_register_all();

        // 注册设备的函数,如用获取摄像头数据或音频等,需要此函数先注册
        // avdevice_register_all();
        // 注册复用器和编解码器,所有的使用ffmpeg,首先必须调用这个函数
        avformat_network_init();

        if (!isLocal) {
            // 打印ffmpeg里面的日志
            // av_log_set_callback(log_callback);
        }

        LOGW("ffmpeg [av_version_info()] version: %s\n", av_version_info());

        /*AVCodec *codecName = av_codec_next(nullptr);
        while (codecName) {
            switch (codecName->type) {
                case AVMEDIA_TYPE_VIDEO: {
                    LOGI("ffmpeg AVMEDIA_TYPE_VIDEO: %s\n", codecName->name);
                    break;
                }
                case AVMEDIA_TYPE_AUDIO: {
                    LOGI("ffmpeg AVMEDIA_TYPE_AUDIO: %s\n", codecName->name);
                    break;
                }
                default: {
                    LOGI("ffmpeg other: %s\n", codecName->name);
                    break;
                }
            }
            codecName = codecName->next;
        }*/

        readLockMutex = PTHREAD_MUTEX_INITIALIZER;
        readLockCondition = PTHREAD_COND_INITIALIZER;
        TIME_DIFFERENCE = 1.000000;
        videoSleepTime = 11000;
        preProgress = 0;
        audioPts = 0.0;
        videoPts = 0.0;
        preAudioPts = 0.0;
        preVideoPts = 0.0;
        runCounts = 0;
        averageTimeDiff = 0.0;
        averageTimeDiffCount = 0;
        memset(timeDiff, '0', sizeof(timeDiff));
        startReadTime = -1;
        endReadTime = -1;
        mediaDuration = -1;
        frameRate = 0;
        bitRate = 0;
        bit_rate_video = 0;
        bit_rate_audio = 0;

        needToResetVideoPts = false;
        needToResetVideoPts2 = false;
        isReading = false;
        isReadWaited = false;
        isVideoHandling = false;
        isInterrupted = false;
        runOneTime = true;
        isFrameByFrameMode = false;
        onlyDownloadNotPlayback = false;
        isInitSuccess = false;
        needToDownload = false;
        videoDisable = false;
        audioDisable = false;
    }

    void initAudio() {
        if (audioWrapper != nullptr && audioWrapper->father != nullptr) {
            av_free(audioWrapper->father);
            audioWrapper->father = nullptr;
        }
        if (audioWrapper != nullptr) {
            av_free(audioWrapper);
            audioWrapper = nullptr;
        }

        // 这里是先有儿子,再有父亲了.其实应该先构造父亲,再把父亲信息传给儿子.
        audioWrapper = (struct AudioWrapper *) av_mallocz(sizeof(struct AudioWrapper));
        memset(audioWrapper, 0, sizeof(struct AudioWrapper));
        audioWrapper->father = (struct Wrapper *) av_mallocz(sizeof(struct Wrapper));
        memset(audioWrapper->father, 0, sizeof(struct Wrapper));

        audioWrapper->father->type = TYPE_AUDIO;
        if (isLocal) {
            audioWrapper->father->list1LimitCounts = MAX_AVPACKET_COUNT_AUDIO_LOCAL;
            audioWrapper->father->list2LimitCounts = MAX_AVPACKET_COUNT_AUDIO_LOCAL;
        } else {
            audioWrapper->father->list1LimitCounts = MAX_AVPACKET_COUNT_AUDIO_HTTP;
            audioWrapper->father->list2LimitCounts = MAX_AVPACKET_COUNT;
        }
        LOGD("initAudio() list1LimitCounts: %d\n", audioWrapper->father->list1LimitCounts);
        LOGD("initAudio() list2LimitCounts: %d\n", audioWrapper->father->list2LimitCounts);
        audioWrapper->father->streamIndex = -1;
        audioWrapper->father->readFramesCount = 0;
        audioWrapper->father->handleFramesCount = 0;
        audioWrapper->father->isStarted = false;
        audioWrapper->father->isReading = true;
        audioWrapper->father->isHandling = true;
        audioWrapper->father->isSleeping = false;
        audioWrapper->father->isPausedForUser = false;
        audioWrapper->father->isPausedForCache = false;
        audioWrapper->father->isPausedForSeek = false;
        audioWrapper->father->needToSeek = false;
        audioWrapper->father->allowDecode = false;
        audioWrapper->father->isHandleList1Full = false;
        audioWrapper->father->list1 = new std::list<AVPacket>();
        audioWrapper->father->list2 = new std::list<AVPacket>();
        audioWrapper->father->useMediaCodec = false;
        audioWrapper->father->avBitStreamFilter = nullptr;
        audioWrapper->father->avbsfContext = nullptr;

        audioWrapper->father->duration = -1;
        audioWrapper->father->timestamp = -1;
        audioWrapper->father->readLockMutex = PTHREAD_MUTEX_INITIALIZER;
        audioWrapper->father->readLockCondition = PTHREAD_COND_INITIALIZER;
        audioWrapper->father->handleLockMutex = PTHREAD_MUTEX_INITIALIZER;
        audioWrapper->father->handleLockCondition = PTHREAD_COND_INITIALIZER;
    }

    void initVideo() {
        if (videoWrapper != nullptr && videoWrapper->father != nullptr) {
            av_free(videoWrapper->father);
            videoWrapper->father = nullptr;
        }
        if (videoWrapper != nullptr) {
            av_free(videoWrapper);
            videoWrapper = nullptr;
        }

        videoWrapper = (struct VideoWrapper *) av_mallocz(sizeof(struct VideoWrapper));
        memset(videoWrapper, 0, sizeof(struct VideoWrapper));
        videoWrapper->father = (struct Wrapper *) av_mallocz(sizeof(struct Wrapper));
        memset(videoWrapper->father, 0, sizeof(struct Wrapper));

        videoWrapper->father->type = TYPE_VIDEO;
        if (isLocal) {
            videoWrapper->father->list1LimitCounts = MAX_AVPACKET_COUNT_VIDEO_LOCAL;
            videoWrapper->father->list2LimitCounts = MAX_AVPACKET_COUNT_VIDEO_LOCAL;
        } else {
            videoWrapper->father->list1LimitCounts = MAX_AVPACKET_COUNT_VIDEO_HTTP;
            videoWrapper->father->list2LimitCounts = MAX_AVPACKET_COUNT;
        }
        LOGW("initVideo() list1LimitCounts: %d\n", videoWrapper->father->list1LimitCounts);
        LOGW("initVideo() list2LimitCounts: %d\n", videoWrapper->father->list2LimitCounts);
        videoWrapper->father->streamIndex = -1;
        videoWrapper->father->readFramesCount = 0;
        videoWrapper->father->handleFramesCount = 0;
        videoWrapper->father->isStarted = false;
        videoWrapper->father->isReading = true;
        videoWrapper->father->isHandling = true;
        videoWrapper->father->isSleeping = false;
        videoWrapper->father->isPausedForUser = false;
        videoWrapper->father->isPausedForCache = false;
        videoWrapper->father->isPausedForSeek = false;
        videoWrapper->father->needToSeek = false;
        videoWrapper->father->allowDecode = false;
        videoWrapper->father->isHandleList1Full = false;
        videoWrapper->father->list1 = new std::list<AVPacket>();
        videoWrapper->father->list2 = new std::list<AVPacket>();
        videoWrapper->father->useMediaCodec = false;
        videoWrapper->father->avBitStreamFilter = nullptr;
        videoWrapper->father->avbsfContext = nullptr;
        videoWrapper->srcWidth = 0;
        videoWrapper->srcHeight = 0;

        videoWrapper->father->duration = -1;
        videoWrapper->father->timestamp = -1;
        videoWrapper->father->readLockMutex = PTHREAD_MUTEX_INITIALIZER;
        videoWrapper->father->readLockCondition = PTHREAD_COND_INITIALIZER;
        videoWrapper->father->handleLockMutex = PTHREAD_MUTEX_INITIALIZER;
        videoWrapper->father->handleLockCondition = PTHREAD_COND_INITIALIZER;
    }

    // *.mp4
    int initDownload() {
        //avformat_alloc_output_context2(&avFormatContextOutput, nullptr, nullptr, outFilePath);
        //AVOutputFormat *out_fmt = avFormatContextOutput->oformat;
        AVOutputFormat *out_fmt = av_guess_format(nullptr, outFilePath, nullptr);
        if (!out_fmt) {
            LOGE("initDownload() out_fmt is nullptr.\n");
            return -1;
        }

        if (avFormatContextOutput != nullptr) {
            //avformat_close_input(&avFormatContextOutput);
            avformat_free_context(avFormatContextOutput);
            avFormatContextOutput = nullptr;
        }
        avFormatContextOutput = avformat_alloc_context();
        avFormatContextOutput->oformat = out_fmt;

        int nb_streams = avFormatContext->nb_streams;
        /*for (int i = 0; i < nb_streams; i++) {
            AVStream *in_stream = avFormatContext->streams[i];
            AVStream *out_stream = avformat_new_stream(avFormatContextOutput, nullptr);
            if (!out_stream) {
                LOGE("initDownload() out_stream is nullptr.\n");
                return -1;
            }
            avcodec_parameters_copy(out_stream->codecpar, in_stream->codecpar);
            out_stream->codecpar->codec_tag = 0;
        }*/

        audio_out_stream = avformat_new_stream(avFormatContextOutput, nullptr);
        video_out_stream = avformat_new_stream(avFormatContextOutput, nullptr);
        if (!audio_out_stream) {
            LOGE("initDownload() audio_out_stream is nullptr.\n");
            return -1;
        }
        if (!video_out_stream) {
            LOGE("initDownload() video_out_stream is nullptr.\n");
            return -1;
        }
        AVCodecParameters *audio_codecpar = audioWrapper->father->avStream->codecpar;
        AVCodecParameters *video_codecpar = videoWrapper->father->avStream->codecpar;
        int ret = avcodec_parameters_copy(audio_out_stream->codecpar, audio_codecpar);
        if (ret < 0) {
            LOGE("initDownload() audio avcodec_parameters_copy occurs error.\n");
            return -1;
        }
        ret = avcodec_parameters_copy(video_out_stream->codecpar, video_codecpar);
        if (ret < 0) {
            LOGE("initDownload() video avcodec_parameters_copy occurs error.\n");
            return -1;
        }
        audio_out_stream->codecpar->codec_tag = 0;
        video_out_stream->codecpar->codec_tag = 0;

        ret = avio_open(&avFormatContextOutput->pb, outFilePath, AVIO_FLAG_WRITE);
        if (ret < 0) {
            LOGE("initDownload() avio_open occurs error.\n");
            return -1;
        }

        // 写头部信息
        ret = avformat_write_header(avFormatContextOutput, nullptr);
        if (ret < 0) {
            LOGE("initDownload() avformat_write_header occurs error.\n");
            return -1;
        }

        // 根据流数量申请空间，并全部初始化为0
        if (dts_start_from != nullptr) {
            free(dts_start_from);
            dts_start_from = nullptr;
        }
        if (pts_start_from != nullptr) {
            free(pts_start_from);
            pts_start_from = nullptr;
        }
        size_t size = sizeof(int64_t) * nb_streams;
        dts_start_from = (int64_t *) malloc(size);
        pts_start_from = (int64_t *) malloc(size);
        if (!dts_start_from) {
            LOGE("initDownload() dts_start_from is nullptr.\n");
            return -1;
        }
        if (!pts_start_from) {
            LOGE("initDownload() pts_start_from is nullptr.\n");
            return -1;
        }
        memset(dts_start_from, 0, size);
        memset(pts_start_from, 0, size);

        return 0;
    }

    // *.h264 *.aac
    int initDownload2() {
        int ret = -1;
        AVOutputFormat *video_out_fmt = av_guess_format(nullptr, videoOutFilePath, nullptr);
        if (!video_out_fmt) {
            LOGE("initDownload() video_out_fmt is nullptr.\n");
            return -1;
        }
        AVOutputFormat *audio_out_fmt = av_guess_format(nullptr, audioOutFilePath, nullptr);
        if (!audio_out_fmt) {
            LOGE("initDownload() audio_out_fmt is nullptr.\n");
            return -1;
        }

        if (avFormatContextVideoOutput != nullptr) {
            //avformat_close_input(&avFormatContextVideoOutput);
            avformat_free_context(avFormatContextVideoOutput);
            avFormatContextVideoOutput = nullptr;
        }
        avFormatContextVideoOutput = avformat_alloc_context();
        avFormatContextVideoOutput->oformat = video_out_fmt;
        video_out_stream = avformat_new_stream(avFormatContextVideoOutput, nullptr);
        if (!video_out_stream) {
            LOGE("initDownload() video_out_stream is nullptr.\n");
            return -1;
        }
        if (avFormatContextAudioOutput != nullptr) {
            //avformat_close_input(&avFormatContextAudioOutput);
            avformat_free_context(avFormatContextAudioOutput);
            avFormatContextAudioOutput = nullptr;
        }
        avFormatContextAudioOutput = avformat_alloc_context();
        avFormatContextAudioOutput->oformat = audio_out_fmt;
        audio_out_stream = avformat_new_stream(avFormatContextAudioOutput, nullptr);
        if (!audio_out_stream) {
            LOGE("initDownload() audio_out_stream is nullptr.\n");
            return -1;
        }

        AVStream *videoAVStream = videoWrapper->father->avStream;
        AVCodecParameters *video_codecpar = videoAVStream->codecpar;
        AVStream *audioAVStream = audioWrapper->father->avStream;
        AVCodecParameters *audio_codecpar = audioAVStream->codecpar;

        ret = avcodec_parameters_copy(video_out_stream->codecpar, video_codecpar);
        //ret = avcodec_copy_context(video_out_stream->codec, videoAVStream->codec);
        if (ret < 0) {
            LOGE("initDownload() video avcodec_parameters_copy occurs error.\n");
            return -1;
        }
        ret = avcodec_parameters_copy(audio_out_stream->codecpar, audio_codecpar);
        if (ret < 0) {
            LOGE("initDownload() audio avcodec_parameters_copy occurs error.\n");
            return -1;
        }

        video_out_stream->codecpar->codec_tag = 0;
        audio_out_stream->codecpar->codec_tag = 0;
        ret = avio_open(&avFormatContextVideoOutput->pb, videoOutFilePath, AVIO_FLAG_WRITE);
        if (ret < 0) {
            LOGE("initDownload() video avio_open occurs error.\n");
            return -1;
        }
        ret = avio_open(&avFormatContextAudioOutput->pb, audioOutFilePath, AVIO_FLAG_WRITE);
        if (ret < 0) {
            LOGE("initDownload() audio avio_open occurs error.\n");
            return -1;
        }

        // 写头部信息
        ret = avformat_write_header(avFormatContextVideoOutput, nullptr);
        if (ret < 0) {
            LOGE("initDownload() video avformat_write_header occurs error.\n");
            return -1;
        }
        ret = avformat_write_header(avFormatContextAudioOutput, nullptr);
        if (ret < 0) {
            LOGE("initDownload() audio avformat_write_header occurs error.\n");
            return -1;
        }

        return 0;
    }

    int initDownload3() {
        audioFile = fopen(audioOutFilePath, "wb");
        videoFile = fopen(videoOutFilePath, "wb");
        if (!audioFile || !videoFile) {
            return -1;
        }

        return 0;
    }

    void initAudioMediaCodec() {
        if (!audioWrapper->father->useMediaCodec) {
            return;
        }

        int audioFormat = 2;
        int parameters_size = 5;
        long long parameters[parameters_size];
        // 采样率
        parameters[0] = audioWrapper->srcSampleRate;
        // 声道数
        parameters[1] = audioWrapper->srcNbChannels;
        //
        parameters[2] = audioFormat;
        // 时长.单位为"秒"
        parameters[3] = mediaDuration;
        parameters[4] = (long long) audioWrapper->father->avCodecContext->bit_rate;

        uint8_t *extradata = audioWrapper->father->avCodecContext->extradata;
        int extradata_size = audioWrapper->father->avCodecContext->extradata_size;

        bool initRet = initMediaCodec(0x0001, audioWrapper->father->avCodecId,
                                      parameters, parameters_size,
                                      extradata, extradata_size);
        if (!initRet) {
            audioWrapper->father->useMediaCodec = false;
        }
        LOGW("initAudioMediaCodec() audio useMediaCodec: %d\n",
             audioWrapper->father->useMediaCodec);
    }

    void initVideoMediaCodec() {
        if (!videoWrapper->father->useMediaCodec) {
            return;
        }

        int parameters_size = 6;
        long long parameters[parameters_size];
        // 创建MediaFormat对象时需要的参数
        // width
        parameters[0] = videoWrapper->srcWidth;
        // height
        parameters[1] = videoWrapper->srcHeight;
        // durationUs
        parameters[2] = mediaDuration;
        // frame-rate
        parameters[3] = frameRate;
        // bitrate
        parameters[4] = (long long) videoWrapper->father->avCodecContext->bit_rate;// bitRate
        // max_input_size
        parameters[5] = videoWrapper->father->outBufferSize;
        //parameters[6] = videoFrames;

        // 传递到java层去处理比较方便
        uint8_t *extradata = videoWrapper->father->avCodecContext->extradata;
        int extradata_size = videoWrapper->father->avCodecContext->extradata_size;

        bool initRet = initMediaCodec(0x0002, videoWrapper->father->avCodecId,
                                      parameters, parameters_size,
                                      extradata, extradata_size);
        if (!initRet) {
            videoWrapper->father->useMediaCodec = false;
        }
        LOGW("initVideoMediaCodec() video useMediaCodec: %d\n",
             videoWrapper->father->useMediaCodec);
    }

    int openAndFindAVFormatContext() {
        LOGI("openAndFindAVFormatContext() start\n");
        if (avFormatContext != nullptr) {
            LOGI("openAndFindAVFormatContext() avFormatContext isn't nullptr\n");
            avformat_free_context(avFormatContext);
            avFormatContext = nullptr;
        }
        avFormatContext = avformat_alloc_context();
        if (avFormatContext == nullptr) {
            LOGE("openAndFindAVFormatContext() avFormatContext is nullptr\n");
            return -1;
        }
        if (!isLocal) {
            avFormatContext->interrupt_callback.callback = read_thread_interrupt_cb;
            avFormatContext->interrupt_callback.opaque = audioWrapper;
            /*AVDictionary *options = nullptr;
            av_dict_set(&options, "stimeout", "10000000", 0);*/
            int64_t startTime = av_gettime_relative();
            startReadTime = startTime;
            /***
             -104(Connection reset by peer)
             -875574520(Server returned 404 Not Found)
             -1094995529(Invalid data found when processing input)
             -1330794744(Protocol not found)
             */
            int ret = avformat_open_input(&avFormatContext,
                                          inFilePath,
                                          nullptr, nullptr);
            if (ret) {
                char buf[1024];
                av_strerror(ret, buf, 1024);
                LOGE("openAndFindAVFormatContext() Couldn't open file, because this: [ %d(%s) ]",
                     ret, buf);
                // 这里就是某些视频初始化失败的地方
                LOGE("openAndFindAVFormatContext() Couldn't open input stream\n");
                return -1;
            }
            int64_t endTime = av_gettime_relative();
            LOGI("openAndFindAVFormatContext() avformat_open_input: %ld\n",
                 (long) ((endTime - startTime) / 1000));

            // av_log_set_callback(log_callback_null);
        } else {
            if (avformat_open_input(&avFormatContext,
                                    inFilePath,
                                    nullptr, nullptr) != 0) {
                LOGE("openAndFindAVFormatContext() Couldn't open input stream\n");
                return -1;
            }
        }
        if (avformat_find_stream_info(avFormatContext, nullptr) != 0) {
            LOGE("openAndFindAVFormatContext() Couldn't find stream information\n");
            return -1;
        }
        LOGI("openAndFindAVFormatContext() end\n");
        return 0;
    }

    int findStreamIndex() {
        if (avFormatContext == nullptr) {
            LOGE("findStreamIndex() avFormatContext is nullptr\n");
            return -1;
        }
        LOGI("findStreamIndex() start\n");
        // stream counts
        int streams = avFormatContext->nb_streams;
        LOGI("findStreamIndex() Stream counts   : %d\n", streams);
        for (int i = 0; i < streams; i++) {
            // 得到AVCodecParameters
            AVCodecParameters *avCodecParameters = avFormatContext->streams[i]->codecpar;
            if (avCodecParameters != nullptr) {
                AVMediaType mediaType = avCodecParameters->codec_type;
                switch (mediaType) {
                    case AVMEDIA_TYPE_VIDEO: {
                        videoWrapper->father->streamIndex = i;
                        videoWrapper->father->avCodecParameters = avCodecParameters;
                        videoWrapper->father->avStream = avFormatContext->streams[i];
                        LOGW("findStreamIndex() videoStreamIndex: %d\n",
                             videoWrapper->father->streamIndex);
                        break;
                    }
                    case AVMEDIA_TYPE_AUDIO: {
                        audioWrapper->father->streamIndex = i;
                        audioWrapper->father->avCodecParameters = avCodecParameters;
                        audioWrapper->father->avStream = avFormatContext->streams[i];
                        LOGD("findStreamIndex() audioStreamIndex: %d\n",
                             audioWrapper->father->streamIndex);
                        break;
                    }
                    case AVMEDIA_TYPE_SUBTITLE: {
                        break;
                    }
                    default:
                        break;
                }
            }
        }

        /*if (audioWrapper->father->streamIndex != -1
            && videoWrapper->father->streamIndex != -1) {
            use_mode = USE_MODE_MEDIA;
            use_mode = USE_MODE_MEDIA_MEDIACODEC;
            videoWrapper->father->avStream = avFormatContext->streams[videoWrapper->father->streamIndex];
            audioWrapper->father->avStream = avFormatContext->streams[audioWrapper->father->streamIndex];
            LOGI("findStreamIndex() USE_MODE_MEDIA_MEDIACODEC\n");
        } else if (audioWrapper->father->streamIndex == -1
                   && videoWrapper->father->streamIndex != -1) {
            use_mode = USE_MODE_ONLY_VIDEO;
            use_mode = USE_MODE_MEDIA_MEDIACODEC;
            videoWrapper->father->avStream = avFormatContext->streams[videoWrapper->father->streamIndex];
            LOGI("findStreamIndex() USE_MODE_ONLY_VIDEO\n");
        } else if (audioWrapper->father->streamIndex != -1
                   && videoWrapper->father->streamIndex == -1) {
            use_mode = USE_MODE_ONLY_AUDIO;
            audioWrapper->father->avStream = avFormatContext->streams[audioWrapper->father->streamIndex];
            LOGI("findStreamIndex() USE_MODE_ONLY_AUDIO\n");
        } else {
            LOGE("findStreamIndex() Didn't find audio or video stream\n");
            return -1;
        }*/

        if (audioWrapper->father->streamIndex == -1
            && videoWrapper->father->streamIndex == -1) {
            LOGE("findStreamIndex() Didn't find audio or video stream\n");
            return -1;
        }

        LOGI("findStreamIndex() end\n");
        return 0;
    }

    int findAndOpenAVCodecForAudio() {
        LOGI("findAndOpenAVCodecForAudio() start\n");
        // audio
        audioWrapper->father->useMediaCodec = false;
        audioWrapper->father->decoderAVCodec = nullptr;
        // 获取音频解码器
        // 先通过AVCodecParameters找到AVCodec
        audioWrapper->father->avCodecId = audioWrapper->father->avCodecParameters->codec_id;
        LOGD("findAndOpenAVCodecForAudio() codecID: %d avcodec_get_name: %s\n",
             audioWrapper->father->avCodecId, avcodec_get_name(audioWrapper->father->avCodecId));

        switch (audioWrapper->father->avCodecId) {
            // 86017 --->
            case AV_CODEC_ID_MP3: {
                LOGD("findAndOpenAVCodecForAudio() AV_CODEC_ID_MP3\n");
                audioWrapper->father->useMediaCodec = true;
                audioWrapper->father->avBitStreamFilter = av_bsf_get_by_name("mp3decomp");
                break;
            }
                // 86018 aac ---> audio/mp4a-latm
            case AV_CODEC_ID_AAC: {
                LOGD("findAndOpenAVCodecForAudio() AV_CODEC_ID_AAC\n");
                audioWrapper->father->useMediaCodec = true;
                audioWrapper->father->avBitStreamFilter = av_bsf_get_by_name("aac_adtstoasc");
                break;
            }

                // 65537 pcm_s16be ---> audio/raw
            case AV_CODEC_ID_PCM_S16BE:
                // 86016 mp2 ---> audio/mpeg-L2
            case AV_CODEC_ID_MP2:
                // 86019 ac3 ---> audio/ac3
            case AV_CODEC_ID_AC3:
                // 86021 vorbis ---> audio/vorbis
            case AV_CODEC_ID_VORBIS:
                // 86024 wmav2 ---> audio/x-ms-wma
            case AV_CODEC_ID_WMAV2:
                // 86028 --->
            case AV_CODEC_ID_FLAC:
                // 86040 --->
            case AV_CODEC_ID_QCELP:
                // 86056 eac3 ---> audio/eac3
            case AV_CODEC_ID_EAC3:
                // 86065 --->
            case AV_CODEC_ID_AAC_LATM:
                // 86076 --->
            case AV_CODEC_ID_OPUS:
            default: {
                audioWrapper->father->useMediaCodec = true;
                audioWrapper->father->avBitStreamFilter = av_bsf_get_by_name("null");
                break;
            }
        }
        if (audioWrapper->father->useMediaCodec) {
            if (audioWrapper->father->avBitStreamFilter == nullptr) {
                LOGE("findAndOpenAVCodecForAudio() audio avBitStreamFilter is nullptr\n");
                audioWrapper->father->useMediaCodec = false;
            } else {
                // 过滤器分配内存
                int ret = av_bsf_alloc(
                        audioWrapper->father->avBitStreamFilter,
                        &audioWrapper->father->avbsfContext);
                if (ret < 0) {
                    LOGE("findAndOpenAVCodecForAudio() audio av_bsf_alloc failure\n");
                    audioWrapper->father->useMediaCodec = false;
                } else {
                    AVStream *audio_avstream = audioWrapper->father->avStream;
                    // 添加解码器属性
                    ret = avcodec_parameters_copy(
                            audioWrapper->father->avbsfContext->par_in,
                            audio_avstream->codecpar);
                    if (ret < 0) {
                        LOGE("findAndOpenAVCodecForAudio() audio avcodec_parameters_copy failure\n");
                        audioWrapper->father->useMediaCodec = false;
                    } else {
                        audioWrapper->father->avbsfContext->time_base_in = audio_avstream->time_base;
                        // 初始化过滤器上下文
                        ret = av_bsf_init(audioWrapper->father->avbsfContext);
                        if (ret < 0) {
                            LOGE("findAndOpenAVCodecForAudio() audio av_bsf_init failure\n");
                            audioWrapper->father->useMediaCodec = false;
                        }
                    }
                }
            }
        }

        audioWrapper->father->decoderAVCodec = avcodec_find_decoder(
                audioWrapper->father->avCodecId);
        if (audioWrapper->father->decoderAVCodec != nullptr) {
            // 获取解码器上下文
            // 再通过AVCodec得到AVCodecContext
            audioWrapper->father->avCodecContext = avcodec_alloc_context3(
                    audioWrapper->father->decoderAVCodec);
            if (audioWrapper->father->avCodecContext != nullptr) {
                // 关联操作
                if (avcodec_parameters_to_context(
                        audioWrapper->father->avCodecContext,
                        audioWrapper->father->avCodecParameters) < 0) {
                    LOGE("findAndOpenAVCodecForAudio() avcodec_parameters_to_context failure\n");
                    return -1;
                } else {
                    // 打开AVCodec
                    if (avcodec_open2(
                            audioWrapper->father->avCodecContext,
                            audioWrapper->father->decoderAVCodec, nullptr) != 0) {
                        LOGE("findAndOpenAVCodecForAudio() avcodec_open2 failure\n");
                        return -1;
                    }
                }
            }
        } else {
            LOGI("findAndOpenAVCodecForAudio() decoderAVCodec is nullptr\n");
            audioWrapper->father->avCodecContext =
                    avFormatContext->streams[audioWrapper->father->streamIndex]->codec;
            if (audioWrapper->father->avCodecContext != nullptr) {
                audioWrapper->father->decoderAVCodec =
                        avcodec_find_decoder(audioWrapper->father->avCodecContext->codec_id);
                if (audioWrapper->father->decoderAVCodec != nullptr) {
                    // 关联操作
                    if (avcodec_parameters_to_context(
                            audioWrapper->father->avCodecContext,
                            audioWrapper->father->avCodecParameters) < 0) {
                        LOGE("findAndOpenAVCodecForAudio() avcodec_parameters_to_context failure\n");
                        return -1;
                    } else {
                        // 打开AVCodec
                        if (avcodec_open2(
                                audioWrapper->father->avCodecContext,
                                audioWrapper->father->decoderAVCodec, nullptr) != 0) {
                            LOGE("findAndOpenAVCodecForAudio() avcodec_open2 failure\n");
                            return -1;
                        }
                    }
                }
            }
        }
        if (audioWrapper->father->avCodecContext == nullptr) {
            LOGE("findAndOpenAVCodecForAudio() avCodecContext is nullptr\n");
            return -1;
        }
        LOGI("findAndOpenAVCodecForAudio() end\n");
        return 0;
    }

    int findAndOpenAVCodecForVideo() {
        LOGI("findAndOpenAVCodecForVideo() start\n");
        /***
        aac_adtstoasc
        chomp
        dump_extra
        dca_core
        eac3_core
        extract_extradata
        filter_units
        h264_metadata
        h264_mp4toannexb
        h264_redundant_pps
        hapqa_extract
        hevc_metadata
        hevc_mp4toannexb
        imxdump
        mjpeg2jpeg
        mjpegadump
        mp3decomp
        mpeg2_metadata
        mpeg4_unpack_bframes
        mov2textsub
        noise
        null
        remove_extra
        text2movsub
        trace_headers
        vp9_raw_reorder
        vp9_superframe
        vp9_superframe_split
         */
        // 第一种方式
        /*void *state = nullptr;
        const AVBitStreamFilter *avBitStreamFilter = nullptr;
        while ((avBitStreamFilter = av_bsf_next(&state))) {
            LOGI("findAndOpenAVCodecForVideo() avBitStreamFilter->name: %s\n",
                 avBitStreamFilter->name);
        }*/
        // 第二种方式
        //avBitStreamFilter = av_bsf_get_by_name("hevc_mp4toannexb");

        // video
        videoWrapper->father->useMediaCodec = false;
        videoWrapper->father->decoderAVCodec = nullptr;
        videoWrapper->father->avCodecId = videoWrapper->father->avCodecParameters->codec_id;
        LOGW("findAndOpenAVCodecForVideo() codecID: %d avcodec_get_name: %s\n",
             videoWrapper->father->avCodecId, avcodec_get_name(videoWrapper->father->avCodecId));
        switch (videoWrapper->father->avCodecId) {
            ////// 2 mpeg2video ---> video/mpeg2(创建MediaCodec时需要的mime)
            case AV_CODEC_ID_MPEG2VIDEO: {
                LOGW("findAndOpenAVCodecForVideo() mpeg2_mediacodec\n");
                videoWrapper->father->useMediaCodec = true;
                videoWrapper->father->avBitStreamFilter =
                        av_bsf_get_by_name("mpeg2_metadata");
                break;
            }
                // 12 mpeg4 ---> video/mp4v-es
            case AV_CODEC_ID_MPEG4: {
                LOGW("findAndOpenAVCodecForVideo() mpeg4_mediacodec\n");
                videoWrapper->father->useMediaCodec = true;
                videoWrapper->father->avBitStreamFilter =
                        av_bsf_get_by_name("mpeg4_unpack_bframes");
                break;
            }
                // 27 h264 ---> video/avc
            case AV_CODEC_ID_H264: {
                LOGW("findAndOpenAVCodecForVideo() h264_mediacodec\n");
                videoWrapper->father->useMediaCodec = true;
                videoWrapper->father->avBitStreamFilter =
                        av_bsf_get_by_name("h264_mp4toannexb");
                break;
            }
                // 173 hevc(h265) ---> video/hevc
            case AV_CODEC_ID_HEVC: {
                LOGW("findAndOpenAVCodecForVideo() hevc_mediacodec\n");
                videoWrapper->father->useMediaCodec = true;
                videoWrapper->father->avBitStreamFilter =
                        av_bsf_get_by_name("hevc_mp4toannexb");
                break;
            }

                //////////////////////////////////////////////
                // 1 mpeg1video ---> video/mpeg2
            case AV_CODEC_ID_MPEG1VIDEO:
                // 7 mjpeg ---> video/mjpeg
            case AV_CODEC_ID_MJPEG:
                // 69 rv40 --->
            case AV_CODEC_ID_RV40:
                // 70 vc1 ---> video/x-ms-wmv
            case AV_CODEC_ID_VC1:
                // 71 wmv3 ---> video/x-ms-wmv
            case AV_CODEC_ID_WMV3:
                // 91 vp6 --->
            case AV_CODEC_ID_VP6:
                // 92 vp6f ---> video/x-vp6
            case AV_CODEC_ID_VP6F:
                //////////////////////////////////////////////

                // 4 h263 ---> video/3gpp(找不到视频测试)
            case AV_CODEC_ID_H263:
                // 139 vp8 ---> video/x-vnd.on2.vp8
            case AV_CODEC_ID_VP8:
                // 167 vp9 ---> video/x-vnd.on2.vp9
            case AV_CODEC_ID_VP9:
            default: {
                videoWrapper->father->useMediaCodec = true;
                videoWrapper->father->avBitStreamFilter =
                        av_bsf_get_by_name("null");
                break;
            }
        }
        if (videoWrapper->father->useMediaCodec) {
            if (videoWrapper->father->avBitStreamFilter == nullptr) {
                LOGE("findAndOpenAVCodecForVideo() video avBitStreamFilter is nullptr\n");
                videoWrapper->father->useMediaCodec = false;
            } else {
                // 过滤器分配内存
                int ret = av_bsf_alloc(
                        videoWrapper->father->avBitStreamFilter,
                        &videoWrapper->father->avbsfContext);
                if (ret < 0) {
                    LOGE("findAndOpenAVCodecForVideo() video av_bsf_alloc failure\n");
                    videoWrapper->father->useMediaCodec = false;
                } else {
                    AVStream *video_avstream = videoWrapper->father->avStream;
                    // 添加解码器属性
                    ret = avcodec_parameters_copy(
                            videoWrapper->father->avbsfContext->par_in,
                            video_avstream->codecpar);
                    if (ret < 0) {
                        LOGE("findAndOpenAVCodecForVideo() video avcodec_parameters_copy failure\n");
                        videoWrapper->father->useMediaCodec = false;
                    } else {
                        videoWrapper->father->avbsfContext->time_base_in = video_avstream->time_base;
                        // 初始化过滤器上下文
                        ret = av_bsf_init(videoWrapper->father->avbsfContext);
                        if (ret < 0) {
                            LOGE("findAndOpenAVCodecForVideo() video av_bsf_init failure\n");
                            videoWrapper->father->useMediaCodec = false;
                        }
                    }
                    /*ret = avcodec_parameters_copy(out->codecpar, avbsfContext->par_out);
                    if (ret < 0) {
                        LOGE("findAndOpenAVCodecForVideo() video avcodec_parameters_copy 2 failure\n");
                        return ret;
                    }
                    out->time_base = avbsfContext->time_base_out;*/
                }
            }
        }
        /*if (!videoWrapper->father->decoderAVCodec) {
            // 有相应的so库时这句就不要执行了
            videoWrapper->father->decoderAVCodec = avcodec_find_decoder(codecID);
        }*/
        // videoWrapper->avCodecParserContext = av_parser_init(codecID);
        videoWrapper->father->decoderAVCodec = avcodec_find_decoder(
                videoWrapper->father->avCodecId);
        if (videoWrapper->father->decoderAVCodec != nullptr) {
            videoWrapper->father->avCodecContext = avcodec_alloc_context3(
                    videoWrapper->father->decoderAVCodec);
            if (videoWrapper->father->avCodecContext != nullptr) {
                // https://www.cnblogs.com/subo_peng/p/7800658.html
                videoWrapper->father->avCodecContext->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;

                if (avcodec_parameters_to_context(
                        videoWrapper->father->avCodecContext,
                        videoWrapper->father->avCodecParameters) < 0) {
                    return -1;
                } else {
                    // avcodec_parameters_to_context(...)成功
                    if (avcodec_open2(
                            videoWrapper->father->avCodecContext,
                            videoWrapper->father->decoderAVCodec, nullptr) != 0) {
                        LOGE("Could not open video codec.\n");
                        return -1;
                    }
                }
            } else {
                LOGE("findAndOpenAVCodecForVideo() video avCodecContext is nullptr\n");
                return -1;
            }
        } else {
            LOGE("findAndOpenAVCodecForVideo() video decoderAVCodec is nullptr\n");
            return -1;
        }

        LOGI("findAndOpenAVCodecForVideo() end\n");
        return 0;
    }

    int createSwrContent() {
        LOGI("createSwrContent() start\n");
        audioWrapper->dstSampleRate = 44100;
        audioWrapper->dstAVSampleFormat = AV_SAMPLE_FMT_S16;
        audioWrapper->dstChannelLayout = AV_CH_LAYOUT_STEREO;

        // src
        audioWrapper->srcSampleRate = audioWrapper->father->avCodecContext->sample_rate;
        audioWrapper->srcNbChannels = audioWrapper->father->avCodecContext->channels;
        audioWrapper->srcAVSampleFormat = audioWrapper->father->avCodecContext->sample_fmt;
        audioWrapper->srcChannelLayout = audioWrapper->father->avCodecContext->channel_layout;
        LOGD("---------------------------------\n");
        LOGD("srcSampleRate        : %d\n", audioWrapper->srcSampleRate);// 48000 44100 32000
        LOGD("srcNbChannels        : %d\n", audioWrapper->srcNbChannels);// 1 2 6
        // 8 fltp
        LOGD("srcAVSampleFormat    : %d %s\n",
             audioWrapper->srcAVSampleFormat,
             av_get_sample_fmt_name(audioWrapper->srcAVSampleFormat));// 8 -1 -1

        LOGD("srcChannelLayout1    : %d\n", audioWrapper->srcChannelLayout);// 3 0 0
        // 有些视频从源视频中得到的channel_layout与使用函数得到的channel_layout结果是一样的
        // 但是还是要使用函数得到的channel_layout为好
        audioWrapper->srcChannelLayout = av_get_default_channel_layout(audioWrapper->srcNbChannels);
        LOGD("srcChannelLayout2    : %d\n", audioWrapper->srcChannelLayout);// 3 63 0

        bit_rate_audio = audioWrapper->father->avCodecContext->bit_rate / 1000;
        LOGD("bit_rate             : %lld\n", (long long) bit_rate_audio);
        LOGD("---------------------------------\n");

        //audioWrapper->srcNbSamples = audioWrapper->father->avCodecContext->frame_size;
        //audioWrapper->dstNbSamples = audioWrapper->srcNbSamples;
        //LOGD("srcNbSamples         : %d\n", audioWrapper->srcNbSamples);// 1024 0
        //LOGD("dstNbSamples         : %d\n", audioWrapper->dstNbSamples);// 1024 0

        // dst
        // Android中跟音频有关的参数: dstSampleRate dstNbChannels 位宽
        // dstSampleRate, dstAVSampleFormat和dstChannelLayout指定
        // 然后通过下面处理后在Java端就能创建AudioTrack对象了
        // 不然像有些5声道,6声道就创建不了,因此没有声音
        audioWrapper->dstSampleRate = audioWrapper->srcSampleRate;
        audioWrapper->dstNbChannels = av_get_channel_layout_nb_channels(
                audioWrapper->dstChannelLayout);

        LOGD("dstSampleRate        : %d\n", audioWrapper->dstSampleRate);// 48000 44100 32000
        LOGD("dstNbChannels        : %d\n", audioWrapper->dstNbChannels);// 2 2
        // 1 s16
        LOGD("dstAVSampleFormat    : %d %s\n",
             audioWrapper->dstAVSampleFormat,
             av_get_sample_fmt_name(audioWrapper->dstAVSampleFormat));// 1 1
        LOGD("dstChannelLayout     : %d\n", audioWrapper->dstChannelLayout);
        LOGD("---------------------------------\n");

        audioWrapper->swrContext = swr_alloc();
        swr_alloc_set_opts(audioWrapper->swrContext,

                           audioWrapper->dstChannelLayout,  // out_ch_layout
                           audioWrapper->dstAVSampleFormat, // out_sample_fmt
                           audioWrapper->dstSampleRate,     // out_sample_rate

                           audioWrapper->srcChannelLayout,  // in_ch_layout
                           audioWrapper->srcAVSampleFormat, // in_sample_fmt
                           audioWrapper->srcSampleRate,     // in_sample_rate

                           0,                       // log_offset
                           nullptr);                  // log_ctx
        if (audioWrapper->swrContext == nullptr) {
            LOGE("%s\n", "createSwrContent() swrContext is nullptr");
            return -1;
        }

        int ret = swr_init(audioWrapper->swrContext);
        if (ret != 0) {
            LOGE("%s\n", "createSwrContent() swrContext swr_init failed");
            return -1;
        } else {
            LOGD("%s\n", "createSwrContent() swrContext swr_init success");
        }

        // 这个对应关系还不知道怎么弄
        int audioFormat = 2;
        switch (audioWrapper->dstAVSampleFormat) {
            case AV_SAMPLE_FMT_NONE: {
                break;
            }
            case AV_SAMPLE_FMT_U8: {
                break;
            }
            case AV_SAMPLE_FMT_S16: {
                audioFormat = 2;
                break;
            }
            case AV_SAMPLE_FMT_S32: {
                break;
            }
            case AV_SAMPLE_FMT_S64: {
                break;
            }
            case AV_SAMPLE_FMT_FLT: {
                break;
            }
            case AV_SAMPLE_FMT_DBL: {
                break;
            }
            case AV_SAMPLE_FMT_U8P: {
                break;
            }
            case AV_SAMPLE_FMT_S16P: {
                break;
            }
            case AV_SAMPLE_FMT_S32P: {
                break;
            }
            case AV_SAMPLE_FMT_S64P: {
                break;
            }
            case AV_SAMPLE_FMT_FLTP: {
                break;
            }
            case AV_SAMPLE_FMT_DBLP: {
                break;
            }
            case AV_SAMPLE_FMT_NB: {
                break;
            }
            default:
                break;
        }

        if (videoWrapper->father->useMediaCodec
            && (audioWrapper->srcSampleRate == 48000 || audioWrapper->srcSampleRate == 44100)
            && (audioWrapper->srcNbChannels == 1 || audioWrapper->srcNbChannels == 2)) {
            initAudioMediaCodec();
        } else {
            audioWrapper->father->useMediaCodec = false;
        }

        LOGD("%s\n", "createSwrContent() createAudioTrack start");
        createAudioTrack(audioWrapper->dstSampleRate,
                         audioWrapper->dstNbChannels,
                         audioFormat);
        LOGD("%s\n", "createSwrContent() createAudioTrack end");

        // avPacket ---> decodedAVFrame ---> dstAVFrame ---> 播放声音
        audioWrapper->decodedAVFrame = av_frame_alloc();
        // 16bit 44100 PCM 数据,16bit是2个字节
        audioWrapper->father->outBuffer1 = (unsigned char *) av_malloc(MAX_AUDIO_FRAME_SIZE);
        audioWrapper->father->outBufferSize = MAX_AUDIO_FRAME_SIZE;

        LOGI("createSwrContent() end\n");
        return 0;
    }

    int createSwsContext() {
        /***
         帧率:fps每秒显示帧数(frames) Frame per Second(fps)/Hz
         码率:bps(比特率,数据率)确定整体视频/音频质量的参数，秒为单位处理的字节数，码率和视频质量成正比
         肉眼想看到连续移动图像至少需要15帧。
         值得一提的是手机视频拍摄中，无论是720P还是1080P基本都是30帧每秒。
         目前受到硬件迭代更新的影响，拍摄帧率有达到60fps/120fps，但基本还是30的倍数。
         电影放映的标准是每秒放映24帧，中国的电视、广告、动画播放每秒25帧，
         这个帧率属于PAL制式，在亚洲和欧洲电视台较为常用，而美国加拿大一般都是NTSC制式每秒29.97帧。

         看起来好像比较正常
         30fps 1480kbps
         30fps 6704kbps

         30fps 12897kbps快
         30fps 14739kbps快
         30fps 11697kbps快
         30fps 1948kbps 快
         29fps 1119kbps 快
         25fps 895kbps  快
         29fps 622kbps  快
         23fps 1212kbps 快
         29fps 38572kbps慢
         */
        LOGI("createSwsContext() start\n");
        // Android支持的目标像素格式
        // AV_PIX_FMT_RGB32
        // AV_PIX_FMT_RGBA
        videoWrapper->dstAVPixelFormat = AV_PIX_FMT_RGBA;

        videoWrapper->srcWidth = videoWrapper->father->avCodecContext->width;
        videoWrapper->srcHeight = videoWrapper->father->avCodecContext->height;
        // AV_PIX_FMT_YUV420P
        videoWrapper->srcAVPixelFormat = videoWrapper->father->avCodecContext->pix_fmt;
        if (videoWrapper->srcAVPixelFormat < 0) {
            LOGE("createSwsContext() videoWrapper->srcAVPixelFormat < 0\n");
            // 无用
            // videoWrapper->srcAVPixelFormat = AV_PIX_FMT_YUV420P;
            return -1;
        }

        int bit_rate_tolerance = videoWrapper->father->avCodecContext->bit_rate_tolerance;
        int bits_per_coded_sample = videoWrapper->father->avCodecContext->bits_per_coded_sample;
        int bits_per_raw_sample = videoWrapper->father->avCodecContext->bits_per_raw_sample;
        int delay = videoWrapper->father->avCodecContext->delay;
        int frame_number = videoWrapper->father->avCodecContext->frame_number;
        int frame_size = videoWrapper->father->avCodecContext->frame_size;
        int level = videoWrapper->father->avCodecContext->level;
        AVStream *stream = avFormatContext->streams[videoWrapper->father->streamIndex];
        if (stream->avg_frame_rate.den != 0) {
            // 帧率
            frameRate = stream->avg_frame_rate.num / stream->avg_frame_rate.den;
        }
        // 码率(视频)/比特率(音频)
        bitRate = avFormatContext->bit_rate / 1000;
        bit_rate_video = videoWrapper->father->avCodecContext->bit_rate / 1000;
        // 帧数
        int64_t videoFrames = stream->nb_frames;

        LOGW("---------------------------------\n");
        LOGW("videoFrames          : %lld\n", (long long) videoFrames);
        LOGW("srcWidth             : %d\n", videoWrapper->srcWidth);
        LOGW("srcHeight            : %d\n", videoWrapper->srcHeight);
        LOGW("frameRate            : %d fps/Hz\n", frameRate);
        LOGW("bitRate              : %lld kb/s\n", (long long) bitRate);// Kbps
        LOGW("bit_rate             : %lld kb/s\n", (long long) bit_rate_video);
        LOGW("bit_rate_tolerance   : %d\n", bit_rate_tolerance);
        LOGW("bits_per_coded_sample: %d\n", bits_per_coded_sample);
        LOGW("bits_per_raw_sample  : %d\n", bits_per_raw_sample);
        LOGW("delay                : %d\n", delay);
        LOGW("level                : %d\n", level);
        LOGW("frame_size           : %d\n", frame_size);
        LOGW("frame_number         : %d\n", frame_number);

        // -1 AV_PIX_FMT_NONE (null)
        LOGW("srcAVPixelFormat     : %d %s %s\n",
             videoWrapper->srcAVPixelFormat,
             getStrAVPixelFormat(videoWrapper->srcAVPixelFormat),
             av_get_pix_fmt_name(videoWrapper->srcAVPixelFormat));
        LOGW("dstAVPixelFormat     : %d %s %s\n",// 在initVideo()中初始化设置
             videoWrapper->dstAVPixelFormat,
             getStrAVPixelFormat(videoWrapper->dstAVPixelFormat),
             av_get_pix_fmt_name(videoWrapper->dstAVPixelFormat));
        videoWrapper->dstWidth = videoWrapper->srcWidth;
        videoWrapper->dstHeight = videoWrapper->srcHeight;
        videoWrapper->srcArea = videoWrapper->srcWidth * videoWrapper->srcHeight;
        videoWrapper->dstArea = videoWrapper->srcArea;

        // decodedAVFrame为解码后的数据
        // avPacket ---> decodedAVFrame ---> rgbAVFrame ---> 渲染画面
        videoWrapper->decodedAVFrame = av_frame_alloc();
        videoWrapper->rgbAVFrame = av_frame_alloc();

        int imageGetBufferSize = av_image_get_buffer_size(
                videoWrapper->dstAVPixelFormat, videoWrapper->srcWidth, videoWrapper->srcHeight, 1);
        videoWrapper->father->outBufferSize = imageGetBufferSize * sizeof(unsigned char);
        videoWrapper->father->outBuffer1 =
                (unsigned char *) av_malloc(videoWrapper->father->outBufferSize);
        int imageFillArrays = av_image_fill_arrays(videoWrapper->rgbAVFrame->data,
                                                   videoWrapper->rgbAVFrame->linesize,
                                                   videoWrapper->father->outBuffer1,
                                                   videoWrapper->dstAVPixelFormat,
                                                   videoWrapper->srcWidth,
                                                   videoWrapper->srcHeight,
                                                   1);
        LOGW("imageGetBufferSize1  : %d\n", imageGetBufferSize);
        LOGW("imageGetBufferSize2  : %d\n", videoWrapper->father->outBufferSize);
        LOGW("imageFillArrays      : %d\n", imageFillArrays);
        if (imageFillArrays < 0) {
            LOGE("imageFillArrays      : %d\n", imageFillArrays);
            return -1;
        }
        // 由于解码出来的帧格式不是RGBA,在渲染之前需要进行格式转换
        // 现在swsContext知道程序员想要得到什么样的像素格式了
        videoWrapper->swsContext = sws_getContext(
                videoWrapper->srcWidth, videoWrapper->srcHeight, videoWrapper->srcAVPixelFormat,
                videoWrapper->srcWidth, videoWrapper->srcHeight, videoWrapper->dstAVPixelFormat,
                // SWS_BICUBIC SWS_BILINEAR 原分辨率与目标分辨率不一致时使用哪种算法来调整.
                SWS_BICUBIC,
                nullptr, nullptr,
                // 指定调整图像缩放的算法,可设为nullptr使用默认算法.
                nullptr);
        if (videoWrapper->swsContext == nullptr) {
            LOGE("%s\n", "videoSwsContext is nullptr.");
            return -1;
        }
        LOGW("---------------------------------\n");

        initVideoMediaCodec();

        LOGI("createSwsContext() end\n");
        return 0;
    }

    int seekToImpl() {
        // seekTo
        LOGI("seekToImpl() sleep start\n");
        if (!videoDisable && !audioDisable) {
            while (!audioWrapper->father->needToSeek
                   || !videoWrapper->father->needToSeek) {
                if (!audioWrapper->father->isHandling || !videoWrapper->father->isHandling) {
                    return 0;
                }
                av_usleep(1000);
            }
        } else if (videoDisable && !audioDisable) {
            while (!audioWrapper->father->needToSeek) {
                if (!audioWrapper->father->isHandling) {
                    return 0;
                }
                av_usleep(1000);
            }
        } else if (!videoDisable && audioDisable) {
            while (!videoWrapper->father->needToSeek) {
                if (!videoWrapper->father->isHandling) {
                    return 0;
                }
                av_usleep(1000);
            }
        }
        LOGI("seekToImpl() sleep end\n");
        //LOGD("seekToImpl() audio list2 size: %d\n", audioWrapper->father->list2->size());
        //LOGD("seekToImpl() video list2 size: %d\n", videoWrapper->father->list2->size());
        if (audioWrapper->father->list2->size() != 0) {
            std::list<AVPacket>::iterator iter;
            for (iter = audioWrapper->father->list2->begin();
                 iter != audioWrapper->father->list2->end();
                 iter++) {
                AVPacket avPacket = *iter;
                av_packet_unref(&avPacket);
            }
            audioWrapper->father->list2->clear();
            onLoadProgressUpdated(MSG_ON_TRANSACT_AUDIO_PRODUCER, 0);
        }
        if (videoWrapper->father->list2->size() != 0) {
            std::list<AVPacket>::iterator iter;
            for (iter = videoWrapper->father->list2->begin();
                 iter != videoWrapper->father->list2->end();
                 iter++) {
                AVPacket avPacket = *iter;
                av_packet_unref(&avPacket);
            }
            videoWrapper->father->list2->clear();
            onLoadProgressUpdated(MSG_ON_TRANSACT_VIDEO_PRODUCER, 0);
        }
        LOGI("seekToImpl() av_seek_frame start\n");
        LOGI("seekToImpl() timestamp: %lld\n", (long long) timeStamp);
        //LOGI("seekToImpl() timestamp: %"PRIu64"\n", timestamp);
        av_seek_frame(avFormatContext, -1,
                      timeStamp * AV_TIME_BASE,
                      AVSEEK_FLAG_BACKWARD | AVSEEK_FLAG_FRAME);
        // 清空解码器的缓存
        if (!audioDisable) {
            avcodec_flush_buffers(audioWrapper->father->avCodecContext);
            audioWrapper->father->isPausedForSeek = false;
            //audioWrapper->father->isStarted = false;
        }
        if (!videoDisable) {
            avcodec_flush_buffers(videoWrapper->father->avCodecContext);
            videoWrapper->father->isPausedForSeek = false;
            //videoWrapper->father->isStarted = false;
        }
        timeStamp = -1;
        preProgress = 0;
        preAudioPts = 0.0;
        preVideoPts = 0.0;
        LOGI("seekToImpl() av_seek_frame end\n");
        LOGI("==================================================================\n");
        return 0;
    }

    // *.mp4
    int downloadImpl(Wrapper *wrapper, AVPacket *srcAVPacket, AVPacket *copyAVPacket) {
        av_packet_ref(copyAVPacket, srcAVPacket);

        AVStream *in_stream, *out_stream;
        in_stream = avFormatContext->streams[copyAVPacket->stream_index];
        out_stream = avFormatContextOutput->streams[copyAVPacket->stream_index];

        // 将截取后的每个流的起始dts,pts保存下来,作为开始时间,用来做后面的时间基转换
        if (dts_start_from[copyAVPacket->stream_index] == 0) {
            dts_start_from[copyAVPacket->stream_index] = copyAVPacket->dts;
        }
        if (pts_start_from[copyAVPacket->stream_index] == 0) {
            pts_start_from[copyAVPacket->stream_index] = copyAVPacket->pts;
        }

        // 时间基转换
        copyAVPacket->pts = av_rescale_q_rnd(
                copyAVPacket->pts - pts_start_from[copyAVPacket->stream_index],
                in_stream->time_base,
                out_stream->time_base,
                (AVRounding) (AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX));
        copyAVPacket->dts = av_rescale_q_rnd(
                copyAVPacket->dts - dts_start_from[copyAVPacket->stream_index],
                in_stream->time_base,
                out_stream->time_base,
                (AVRounding) (AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX));

        if (copyAVPacket->pts < 0) {
            copyAVPacket->pts = 0;
        }
        if (copyAVPacket->dts < 0) {
            copyAVPacket->dts = 0;
        }

        copyAVPacket->duration = (int) av_rescale_q(
                (int64_t) copyAVPacket->duration,
                in_stream->time_base,
                out_stream->time_base);
        copyAVPacket->pos = -1;

        // 一帧视频播放时间必须在解码时间点之后,当出现pkt.pts < pkt.dts时会导致程序异常,
        // 所以我们丢掉有问题的帧,不会有太大影响
        if (copyAVPacket->pts < copyAVPacket->dts) {
            return 0;
        }

        av_interleaved_write_frame(avFormatContextOutput, copyAVPacket);

        /*AVRational time_base;
        if (wrapper->type == TYPE_AUDIO) {
            time_base = audio_out_stream->time_base;
        } else {
            time_base = video_out_stream->time_base;
        }
        copyAVPacket->pts = av_rescale_q_rnd(copyAVPacket->pts,
                                             wrapper->avStream->time_base,
                                             time_base,
                                             (AVRounding) (AV_ROUND_NEAR_INF |
                                                           AV_ROUND_PASS_MINMAX));
        copyAVPacket->dts = av_rescale_q_rnd(copyAVPacket->dts,
                                             wrapper->avStream->time_base,
                                             time_base,
                                             (AVRounding) (AV_ROUND_NEAR_INF |
                                                           AV_ROUND_PASS_MINMAX));
        copyAVPacket->duration = av_rescale_q(copyAVPacket->duration,
                                              wrapper->avStream->time_base,
                                              time_base);
        copyAVPacket->pos = -1;
        //将包写到输出媒体文件
        av_interleaved_write_frame(avFormatContextOutput, copyAVPacket);
        copyAVPacket->stream_index = 0;*/

        av_packet_unref(copyAVPacket);
    }

    // *.h264 *.aac
    int downloadImpl2(Wrapper *wrapper, AVPacket *srcAVPacket, AVPacket *copyAVPacket) {
        av_packet_ref(copyAVPacket, srcAVPacket);

        AVRational time_base;
        if (wrapper->type == TYPE_AUDIO) {
            time_base = audio_out_stream->time_base;
        } else {
            time_base = video_out_stream->time_base;
        }
        copyAVPacket->pts = av_rescale_q_rnd(copyAVPacket->pts,
                                             wrapper->avStream->time_base,
                                             time_base,
                                             (AVRounding) (AV_ROUND_NEAR_INF |
                                                           AV_ROUND_PASS_MINMAX));
        copyAVPacket->dts = av_rescale_q_rnd(copyAVPacket->dts,
                                             wrapper->avStream->time_base,
                                             time_base,
                                             (AVRounding) (AV_ROUND_NEAR_INF |
                                                           AV_ROUND_PASS_MINMAX));
        copyAVPacket->duration = av_rescale_q(copyAVPacket->duration,
                                              wrapper->avStream->time_base,
                                              time_base);
        copyAVPacket->pos = -1;
        copyAVPacket->stream_index = 0;
        //将包写到输出媒体文件
        if (wrapper->type == TYPE_AUDIO) {
            //LOGD("downloadImpl2() audio copyAVPacket->pts: %d\n", (long) copyAVPacket->pts);
            av_interleaved_write_frame(avFormatContextAudioOutput, copyAVPacket);
        } else {
            //LOGW("downloadImpl2() video copyAVPacket->pts: %d\n", (long) copyAVPacket->pts);
            av_interleaved_write_frame(avFormatContextVideoOutput, copyAVPacket);
        }

        av_packet_unref(copyAVPacket);
    }

    int downloadImpl3(Wrapper *wrapper, AVPacket *srcAVPacket, AVPacket *copyAVPacket) {
        av_packet_ref(copyAVPacket, srcAVPacket);

        if (wrapper->type == TYPE_AUDIO) {
            fwrite(copyAVPacket->data, copyAVPacket->size, 1, audioFile);
        } else {
            fwrite(copyAVPacket->data, copyAVPacket->size, 1, videoFile);
        }

        av_packet_unref(copyAVPacket);
    }

    int readDataImpl(Wrapper *wrapper, AVPacket *srcAVPacket, AVPacket *copyAVPacket) {
        wrapper->readFramesCount++;
        // 复制数据
        av_packet_ref(copyAVPacket, srcAVPacket);
        av_packet_unref(srcAVPacket);

        // 保存数据
        pthread_mutex_lock(&wrapper->readLockMutex);
        wrapper->list2->push_back(*copyAVPacket);
        size_t list2Size = wrapper->list2->size();
        pthread_mutex_unlock(&wrapper->readLockMutex);

        if (wrapper->type == TYPE_AUDIO) {
            if (list2Size % 10 == 0) {
                onLoadProgressUpdated(MSG_ON_TRANSACT_AUDIO_PRODUCER, list2Size);
            }
        } else {
            if (list2Size % 10 == 0) {
                onLoadProgressUpdated(MSG_ON_TRANSACT_VIDEO_PRODUCER, list2Size);
            }
        }

        if (!wrapper->isHandleList1Full
            && list2Size == wrapper->list1LimitCounts) {
            /***
             什么时候走这里?
             1.开始播放缓冲好的时候
             2.因为缓冲原因
             3.seek后
             */
            // 下面两个都不行
            // std::move(wrapper->list2->begin(), wrapper->list2->end(), std::back_inserter(wrapper->list1));
            // wrapper->list1->swap((std::list<AVPacket> &) wrapper->list2);
            // 把list2中的内容全部复制给list1
            pthread_mutex_lock(&wrapper->readLockMutex);
            wrapper->list1->clear();
            wrapper->list1->assign(wrapper->list2->begin(), wrapper->list2->end());
            wrapper->list2->clear();
            wrapper->isHandleList1Full = true;
            pthread_mutex_unlock(&wrapper->readLockMutex);
            // 开始播放
            notifyToHandle(wrapper);

            if (wrapper->type == TYPE_AUDIO) {
                onLoadProgressUpdated(MSG_ON_TRANSACT_AUDIO_PRODUCER, 0);
                onLoadProgressUpdated(MSG_ON_TRANSACT_AUDIO_CONSUMER, wrapper->list1->size());
                if (!isLocal) {
                    LOGD("readDataImpl() audio 填满数据了\n");
                }
            } else {
                onLoadProgressUpdated(MSG_ON_TRANSACT_VIDEO_PRODUCER, 0);
                onLoadProgressUpdated(MSG_ON_TRANSACT_VIDEO_CONSUMER, wrapper->list1->size());
                if (!isLocal) {
                    LOGW("readDataImpl() video 填满数据了\n");
                }
            }
        } else if (wrapper->type == TYPE_VIDEO) {
            onLoadProgressUpdated(
                    MSG_ON_TRANSACT_AUDIO_PRODUCER, audioWrapper->father->list2->size());
            onLoadProgressUpdated(
                    MSG_ON_TRANSACT_VIDEO_PRODUCER, videoWrapper->father->list2->size());
        } else if (wrapper->type == TYPE_AUDIO) {
            onLoadProgressUpdated(
                    MSG_ON_TRANSACT_AUDIO_PRODUCER, audioWrapper->father->list2->size());
            onLoadProgressUpdated(
                    MSG_ON_TRANSACT_VIDEO_PRODUCER, videoWrapper->father->list2->size());
        }

        bool isFull = false;
        if (!videoDisable && !audioDisable) {
            // video and audio
            if (videoWrapper->father->list2->size() >=
                videoWrapper->father->list2LimitCounts
                && audioWrapper->father->list2->size() >=
                   audioWrapper->father->list2LimitCounts) {
                isFull = true;
            }
        } else if (!videoDisable && audioDisable) {
            // video
            if (videoWrapper->father->list2->size() >=
                videoWrapper->father->list2LimitCounts) {
                isFull = true;
            }
        } else if (videoDisable && !audioDisable) {
            // audio
            if (audioWrapper->father->list2->size() >=
                audioWrapper->father->list2LimitCounts) {
                isFull = true;
            }
        }
        if (isFull) {
            if (!isLocal) {
                LOGI("readDataImpl() audio list1: %d\n", audioWrapper->father->list1->size());
                LOGI("readDataImpl() audio list2: %d\n", audioWrapper->father->list2->size());
                LOGI("readDataImpl() video list1: %d\n", videoWrapper->father->list1->size());
                LOGI("readDataImpl() video list2: %d\n", videoWrapper->father->list2->size());
                LOGD("readDataImpl() notifyToReadWait start\n");
            }
            isReadWaited = true;
            notifyToReadWait();
            isReadWaited = false;
            if (!isLocal) {
                LOGD("readDataImpl() notifyToReadWait end\n");
            }
        }

        return 0;
    }

    void *readData(void *opaque) {
        if (audioWrapper == nullptr
            || audioWrapper->father == nullptr
            || videoWrapper == nullptr
            || videoWrapper->father == nullptr) {
            LOGF("%s\n", "readData() finish nullptr");
            isReading = false;
            return nullptr;
        } else if (!audioWrapper->father->isReading
                   || !audioWrapper->father->isHandling
                   || !videoWrapper->father->isReading
                   || !videoWrapper->father->isHandling) {
            /*closeAudio();
            closeVideo();
            closeOther();
            onFinished();*/
            LOGF("%s\n", "readData() finish");
            isReading = false;
            return nullptr;
        }

        AVPacket *srcAVPacket = av_packet_alloc();
        AVPacket *copyAVPacket = av_packet_alloc();
        av_init_packet(srcAVPacket);
        srcAVPacket->data = nullptr;
        srcAVPacket->size = 0;
        av_init_packet(copyAVPacket);
        copyAVPacket->data = nullptr;
        copyAVPacket->size = 0;

        // seekTo
        if (timeStamp > 0) {
            LOGI("readData() timeStamp: %ld\n", (long) timeStamp);
            audioWrapper->father->needToSeek = true;
            videoWrapper->father->needToSeek = true;
            audioWrapper->father->isPausedForSeek = true;
            videoWrapper->father->isPausedForSeek = true;
        }

        bool audioHasSentNullPacket = false;
        bool videoHasSentNullPacket = false;
        isReading = true;
        /***
         有几种情况:
         1.list1中先存满n个,然后list2多次存取
         2.list1中先存满n个,然后list2一次性存满
         3.list1中还没满n个文件就读完了
         */
        LOGI("%s\n", "readData() start");
        for (;;) {
            // exit
            if (!audioWrapper->father->isReading
                && !videoWrapper->father->isReading) {
                // for (;;) end
                break;
            }

            // seekTo
            if ((audioWrapper->father->isPausedForSeek || videoWrapper->father->isPausedForSeek)
                && timeStamp >= 0) {
                seekToImpl();
            }

            startReadTime = av_gettime_relative();
            int readFrame = av_read_frame(avFormatContext, srcAVPacket);
            endReadTime = av_gettime_relative();

            if (isInterrupted) {
                stop();
                break;
            }

            // 0 if OK, < 0 on error or end of file
            if (readFrame < 0) {
                if (readFrame != -12 && readFrame != AVERROR_EOF) {
                    if (readFrame != AVERROR_HTTP_FORBIDDEN) {
                        LOGE("readData()   readFrame: %d\n", readFrame);
                    }
                    continue;
                }
                // 有些直播节目会这样
                if (isLive && readFrame == AVERROR_EOF) {
                    // LOGF("readData() readFrame  : %d\n", readFrame);
                    av_usleep(10000);
                    continue;
                }

                // -101
                // AVERROR_HTTP_FORBIDDEN -858797304
                // readData() AVERROR_EOF readFrame: -12 (Cannot allocate memory)
                // readData() AVERROR_EOF readFrame: -1094995529
                // readData() AVERROR_EOF readFrame: -1414092869 超时
                // readData() AVERROR_EOF readFrame: -541478725(AVERROR_EOF) 文件已经读完了
                LOGF("readData()   readFrame: %d\n", readFrame);

                if (!audioDisable && audioWrapper->father->useMediaCodec) {
                    readFrame = av_bsf_send_packet(audioWrapper->father->avbsfContext, nullptr);
                    if (readFrame == 0) {
                        while ((readFrame = av_bsf_receive_packet(
                                audioWrapper->father->avbsfContext, srcAVPacket)) == 0) {
                            readDataImpl(audioWrapper->father, srcAVPacket, copyAVPacket);
                        }
                    }
                    audioHasSentNullPacket = true;
                }
                if (!videoDisable && videoWrapper->father->useMediaCodec) {
                    readFrame = av_bsf_send_packet(videoWrapper->father->avbsfContext, nullptr);
                    if (readFrame == 0) {
                        while ((readFrame = av_bsf_receive_packet(
                                videoWrapper->father->avbsfContext, srcAVPacket)) == 0) {
                            readDataImpl(videoWrapper->father, srcAVPacket, copyAVPacket);
                        }
                    }
                    videoHasSentNullPacket = true;
                }

                LOGF("readData() audio list2: %d\n", audioWrapper->father->list2->size());
                LOGF("readData() video list2: %d\n", videoWrapper->father->list2->size());

                // 读到文件末尾了
                // 说明歌曲长度比较短,达不到"规定值",因此处理数据线程还在等待
                if (!audioDisable) {
                    audioWrapper->father->isReading = false;
                    audioWrapper->father->isHandleList1Full = true;
                    notifyToHandle(audioWrapper->father);
                    onLoadProgressUpdated(
                            MSG_ON_TRANSACT_AUDIO_PRODUCER,
                            audioWrapper->father->list2->size());
                }
                if (!videoDisable) {
                    videoWrapper->father->isReading = false;
                    videoWrapper->father->isHandleList1Full = true;
                    notifyToHandle(videoWrapper->father);
                    onLoadProgressUpdated(
                            MSG_ON_TRANSACT_VIDEO_PRODUCER,
                            videoWrapper->father->list2->size());
                }

                pthread_mutex_lock(&readLockMutex);
                if (needToDownload && isInitSuccess) {
                    LOGI("readData() 文件读完,已经停止下载\n");
                    needToDownload = false;
                    isInitSuccess = false;
                    closeDownload();
                }
                pthread_mutex_unlock(&readLockMutex);

                if (onlyDownloadNotPlayback) {
                    onInfo("文件已读完");
                }

                // 不退出线程
                LOGI("readData() notifyToReadWait start\n");
                notifyToReadWait();
                LOGI("readData() notifyToReadWait end\n");
                if (audioWrapper->father->isPausedForSeek
                    || videoWrapper->father->isPausedForSeek) {
                    LOGF("readData() start seek\n");
                    audioWrapper->father->isReading = true;
                    videoWrapper->father->isReading = true;
                    continue;
                } else {
                    /***
                     why???
                     readFrame: -541478725
                     播放优酷视频时,播放一半的时候就读完了,导致再次播放时因为mPrePath != null;播放不了.
                     因此走到这里时发送一个字符串给上层,然后执行mPrePath = null;这样就能再次播放了.
                     */
                    onInfo("AVERROR_EOF");
                    // for (;;) end
                    break;
                }
            }// 文件已读完

            if (srcAVPacket->data == nullptr) {
                continue;
            }

            // 关键帧的判断
            /*if (srcAVPacket->flags & AV_PKT_FLAG_KEY) {
                LOGI("read a key frame");
            }*/

            Wrapper *wrapper = nullptr;
            if (!audioDisable
                && srcAVPacket->stream_index == audioWrapper->father->streamIndex) {
                wrapper = audioWrapper->father;
            } else if (!videoDisable
                       && srcAVPacket->stream_index == videoWrapper->father->streamIndex) {
                wrapper = videoWrapper->father;
            }

            if (wrapper == nullptr) {
                av_packet_unref(srcAVPacket);
                continue;
            }

            if (isInitSuccess && needToDownload) {
                downloadImpl(wrapper, srcAVPacket, copyAVPacket);
                if (onlyDownloadNotPlayback) {
                    av_packet_unref(srcAVPacket);
                    continue;
                }
            }
            if (wrapper->useMediaCodec) {
                readFrame = av_bsf_send_packet(wrapper->avbsfContext, srcAVPacket);
                if (readFrame < 0) {
                    LOGE("readData() video av_bsf_send_packet failure\n");
                    break;
                }
                while (av_bsf_receive_packet(wrapper->avbsfContext, srcAVPacket) == 0) {
                    readDataImpl(wrapper, srcAVPacket, copyAVPacket);
                }
                continue;
            }
            readDataImpl(wrapper, srcAVPacket, copyAVPacket);
        }// for(;;) end
        LOGF("readData() end\n");

        if (!audioHasSentNullPacket) {
            if (!audioDisable
                && audioWrapper->father->useMediaCodec) {
                int readFrame = av_bsf_send_packet(audioWrapper->father->avbsfContext, nullptr);
                if (readFrame == 0) {
                    while ((readFrame = av_bsf_receive_packet(
                            audioWrapper->father->avbsfContext, srcAVPacket)) == 0) {
                        readDataImpl(audioWrapper->father, srcAVPacket, copyAVPacket);
                    }
                }
            }
        }

        if (!videoHasSentNullPacket) {
            if (!videoDisable
                && videoWrapper->father->useMediaCodec) {
                int readFrame = av_bsf_send_packet(videoWrapper->father->avbsfContext, nullptr);
                if (readFrame == 0) {
                    while ((readFrame = av_bsf_receive_packet(
                            videoWrapper->father->avbsfContext, srcAVPacket)) == 0) {
                        readDataImpl(videoWrapper->father, srcAVPacket, copyAVPacket);
                    }
                }
            }
        }

        pthread_mutex_lock(&readLockMutex);
        if (needToDownload && isInitSuccess) {
            LOGI("readData() 读线程退出,停止下载\n");
            needToDownload = false;
            isInitSuccess = false;
            closeDownload();
        }
        pthread_mutex_unlock(&readLockMutex);

        if (srcAVPacket != nullptr) {
            av_packet_unref(srcAVPacket);
            srcAVPacket = nullptr;
        }

        isReading = false;

        return nullptr;
    }

    /***
     034be7f3de99ec63393b2395204aa2b0921a7e7a 有audio硬解码时的策略
     frameRate: 50 averageTimeDiff: 0.085061  TIME_DIFFERENCE: 0.135061
     音频快于视频
     */
    void hope_to_get_a_good_result() {
        if (averageTimeDiff > 0) {
            LOGI("hope_to_get_a_good_result() averageTimeDiff: %lf frameRate: %d \n",
                 averageTimeDiff, frameRate);
            /*bool isGoodResult = false;
            if ((bitRate > 0 && bit_rate_video > 0 && bitRate >= bit_rate_video)
                || (bitRate > 0 && bit_rate_video == 0)
                || (bitRate == 0 && bit_rate_video == 0)) {
                isGoodResult = true;
            }
            if (isGoodResult) {
            } else {
                TIME_DIFFERENCE = averageTimeDiff + 0.100000;
            }*/

            // region

            needToGetResultAgain = true;
            if (averageTimeDiff > 0.500000) {
                averageTimeDiffCount++;
                needToResetVideoPts = true;
            } else if (averageTimeDiff > 0.400000 && averageTimeDiff < 0.500000) {
                // region 第一次就走进这里的话,算是得到一个比较好的结果了

                /***
                 0.405114 0.418364 0.429602 0.439030 0.449823
                 0.457614 0.461167 0.472319 0.486549 0.494847
                 */
                needToGetResultAgain = false;
                double step = -0.000500;
                if (videoWrapper->father->useMediaCodec) {
                    if (audioWrapper->father->useMediaCodec) {
                        step = -0.105000;
                        step = -0.115000;
                    }

                    if (averageTimeDiff > 0.490000) {
                        TIME_DIFFERENCE = 0.199500 + step;
                    } else if (averageTimeDiff > 0.480000 && averageTimeDiff < 0.490000) {
                        TIME_DIFFERENCE = 0.199000 + step;
                    } else if (averageTimeDiff > 0.470000 && averageTimeDiff < 0.480000) {
                        TIME_DIFFERENCE = 0.198500 + step;
                    } else if (averageTimeDiff > 0.460000 && averageTimeDiff < 0.470000) {
                        TIME_DIFFERENCE = 0.198000 + step;
                    } else if (averageTimeDiff > 0.450000 && averageTimeDiff < 0.460000) {
                        TIME_DIFFERENCE = 0.197500 + step;
                    } else if (averageTimeDiff > 0.440000 && averageTimeDiff < 0.450000) {
                        TIME_DIFFERENCE = 0.197000 + step;
                    } else if (averageTimeDiff > 0.430000 && averageTimeDiff < 0.440000) {
                        TIME_DIFFERENCE = 0.196500 + step;
                    } else if (averageTimeDiff > 0.420000 && averageTimeDiff < 0.430000) {
                        TIME_DIFFERENCE = 0.196000 + step;
                    } else if (averageTimeDiff > 0.410000 && averageTimeDiff < 0.420000) {
                        TIME_DIFFERENCE = 0.195500 + step;
                    } else if (averageTimeDiff > 0.400000 && averageTimeDiff < 0.410000) {
                        TIME_DIFFERENCE = 0.195000 + step;
                    }

                    //TIME_DIFFERENCE = 0.200000;
                } else {
                    TIME_DIFFERENCE = 0.300000;
                }

                // endregion
            } else if (averageTimeDiff > 0.300000 && averageTimeDiff < 0.400000) {
                /***
                 0.339266 0.344956 0.350436 0.365758 0.376415 0.385712 0.397755
                 */
                needToGetResultAgain = false;
                if (videoWrapper->father->useMediaCodec) {
                    TIME_DIFFERENCE = 0.100000;
                    if (audioWrapper->father->useMediaCodec) {
                        //TIME_DIFFERENCE = 0.300000;
                        TIME_DIFFERENCE = 0.050000;
                    }
                } else {
                    TIME_DIFFERENCE = 0.200000;
                }
            } else if (averageTimeDiff > 0.200000 && averageTimeDiff < 0.300000) {
                /***
                 0.204199 0.245688 0.263926 0.271427 0.284538
                 */
                needToGetResultAgain = false;
                if (videoWrapper->father->useMediaCodec) {
                    TIME_DIFFERENCE = 0.050000;
                } else {
                    TIME_DIFFERENCE = 0.100000;
                }
            } else if (averageTimeDiff > 0.100000 && averageTimeDiff < 0.200000) {
                /***
                 0.100523 0.127849 0.168335
                 */
                needToGetResultAgain = false;
                if (videoWrapper->father->useMediaCodec) {
                    //TIME_DIFFERENCE = averageTimeDiff - 0.100000;
                    TIME_DIFFERENCE = averageTimeDiff + 0.200000;
                } else {
                    TIME_DIFFERENCE = averageTimeDiff;
                }
                needToResetVideoPts2 = true;
            } else if (averageTimeDiff > 0.010000 && averageTimeDiff < 0.100000) {
                /***
                 之前frameRate <= 23时,会走这里.
                 现在好像当frameRate = 0或者frameRate >= 50时才可能走到这里.
                 0.014149 0.018936
                 0.022836 0.023516 0.024403 0.026983 0.027595 0.028610 0.029898
                 0.030690 0.031515 0.034621 0.035779 0.036042 0.037615 0.038017 0.039632
                 0.042750 0.043855 0.047141 0.048789
                 0.052697 0.054136 0.055711 0.059648
                 0.062606 0.063012 0.064637 0.065509 0.066374 0.067457
                 0.073902 0.074668 0.079382
                 0.088914
                 0.099370
                 */
                needToGetResultAgain = false;
                TIME_DIFFERENCE = averageTimeDiff + 0.050000;
                if (TIME_DIFFERENCE < 0.100000) {
                    TIME_DIFFERENCE = 0.100000;
                }
                TIME_DIFFERENCE = averageTimeDiff + 0.200000;
                needToResetVideoPts2 = true;
            }

            // endregion

            if (needToGetResultAgain) {
                runCounts = 0;
                averageTimeDiff = 0;
                if (needToResetVideoPts) {
                    TIME_DIFFERENCE = 0.500000 - 0.100000 * averageTimeDiffCount;
                    if (TIME_DIFFERENCE < 0) {
                        TIME_DIFFERENCE = 0.100000;
                    }
                    //TIME_DIFFERENCE = 0.000800;
                }
            } else {
                // 对4K视频特殊处理
                if (frameRate >= 45
                    && videoWrapper->srcWidth >= 3840
                    && videoWrapper->srcHeight >= 2160) {
                    // 增大TIME_DIFFERENCE值让视频加快
                    TIME_DIFFERENCE = averageTimeDiff + 0.200000;
                }
            }
            LOGI("hope_to_get_a_good_result() TIME_DIFFERENCE: %lf\n", TIME_DIFFERENCE);
        }

        char info[200];
        memset(info, '\0', sizeof(info));
        if (videoWrapper->father->useMediaCodec
            && audioWrapper->father->useMediaCodec) {
            sprintf(info,
                    "[%lld] [%lld] [%lld] [%d] [%lf] [%lf]"
                    "\n[%s] [%s] [%d] [%d] %s"
                    "\n[%s] [%s] [%d] [%d] %s",
                    (long long) bitRate,
                    (long long) bit_rate_video,
                    (long long) bit_rate_audio,
                    frameRate,
                    averageTimeDiff,
                    TIME_DIFFERENCE,
                    // video
                    avcodec_get_name(videoWrapper->father->avCodecId),
                    av_get_pix_fmt_name(videoWrapper->srcAVPixelFormat),
                    videoWrapper->srcWidth,
                    videoWrapper->srcHeight,
                    "[V]",
                    // audio
                    avcodec_get_name(audioWrapper->father->avCodecId),
                    av_get_sample_fmt_name(audioWrapper->srcAVSampleFormat),
                    audioWrapper->srcSampleRate,
                    audioWrapper->srcNbChannels,
                    "[A]"
            );
        } else if (videoWrapper->father->useMediaCodec
                   && !audioWrapper->father->useMediaCodec) {
            if (!audioDisable) {
                sprintf(info,
                        "[%lld] [%lld] [%lld] [%d] [%lf] [%lf]"
                        "\n[%s] [%s] [%d] [%d] %s"
                        "\n[%s] [%s] [%d] [%d] %s",
                        (long long) bitRate,
                        (long long) bit_rate_video,
                        (long long) bit_rate_audio,
                        frameRate,
                        averageTimeDiff,
                        TIME_DIFFERENCE,
                        // video
                        avcodec_get_name(videoWrapper->father->avCodecId),
                        av_get_pix_fmt_name(videoWrapper->srcAVPixelFormat),
                        videoWrapper->srcWidth,
                        videoWrapper->srcHeight,
                        "[V]",
                        // audio
                        avcodec_get_name(audioWrapper->father->avCodecId),
                        av_get_sample_fmt_name(audioWrapper->srcAVSampleFormat),
                        audioWrapper->srcSampleRate,
                        audioWrapper->srcNbChannels,
                        "[ ]"
                );
            } else {
                sprintf(info,
                        "[%lld] [%lld] [%lld] [%d] [%lf] [%lf]"
                        "\n[%s] [%s] [%d] [%d] %s",
                        (long long) bitRate,
                        (long long) bit_rate_video,
                        (long long) bit_rate_audio,
                        frameRate,
                        averageTimeDiff,
                        TIME_DIFFERENCE,
                        // video
                        avcodec_get_name(videoWrapper->father->avCodecId),
                        av_get_pix_fmt_name(videoWrapper->srcAVPixelFormat),
                        videoWrapper->srcWidth,
                        videoWrapper->srcHeight,
                        "[V]"
                );
            }
        } else if (!videoWrapper->father->useMediaCodec
                   && audioWrapper->father->useMediaCodec) {
            // 这里不会执行到,因为这种情况不好
        } else if (!videoWrapper->father->useMediaCodec
                   && !audioWrapper->father->useMediaCodec) {
            if (!audioDisable) {
                sprintf(info,
                        "[%lld] [%lld] [%lld] [%d] [%lf] [%lf]"
                        "\n[%s] [%s] [%d] [%d] %s"
                        "\n[%s] [%s] [%d] [%d] %s",
                        (long long) bitRate,
                        (long long) bit_rate_video,
                        (long long) bit_rate_audio,
                        frameRate,
                        averageTimeDiff,
                        TIME_DIFFERENCE,
                        // video
                        avcodec_get_name(videoWrapper->father->avCodecId),
                        av_get_pix_fmt_name(videoWrapper->srcAVPixelFormat),
                        videoWrapper->srcWidth,
                        videoWrapper->srcHeight,
                        "[ ]",
                        // audio
                        avcodec_get_name(audioWrapper->father->avCodecId),
                        av_get_sample_fmt_name(audioWrapper->srcAVSampleFormat),
                        audioWrapper->srcSampleRate,
                        audioWrapper->srcNbChannels,
                        "[ ]"
                );
            } else {
                sprintf(info,
                        "[%lld] [%lld] [%lld] [%d] [%lf] [%lf]"
                        "\n[%s] [%s] [%d] [%d] %s",
                        (long long) bitRate,
                        (long long) bit_rate_video,
                        (long long) bit_rate_audio,
                        frameRate,
                        averageTimeDiff,
                        TIME_DIFFERENCE,
                        // video
                        avcodec_get_name(videoWrapper->father->avCodecId),
                        av_get_pix_fmt_name(videoWrapper->srcAVPixelFormat),
                        videoWrapper->srcWidth,
                        videoWrapper->srcHeight,
                        "[ ]"
                );
            }
        }
        onInfo(info);
    }

    void resetForGetAGoodResult() {
        // 重新计算TIME_DIFFERENCE值
        /*runCounts = 0;
        averageTimeDiffCount = 0;
        averageTimeDiff = 0;
        TIME_DIFFERENCE = 0.5;
        needToResetVideoPts = false;
        hope_to_get_a_good_result();*/

        onError(0x101, "需要重新开始播放");
        stop();
    }

    int handleAudioDataImpl(AVStream *stream, AVFrame *decodedAVFrame) {
        if (runOneTime) {
            audioWrapper->father->isStarted = true;
            if (!videoDisable) {
                while (!videoWrapper->father->isStarted) {
                    if (audioWrapper->father->isPausedForUser
                        || audioWrapper->father->isPausedForCache
                        || audioWrapper->father->isPausedForSeek
                        || !audioWrapper->father->isHandling
                        || !videoWrapper->father->isHandling) {
                        LOGD("handleAudioDataImpl() audioWrapper->father->isStarted return\n");
                        return 0;
                    }
                    av_usleep(1000);
                }
                LOGD("handleAudioDataImpl() 音视频已经准备好,开始播放!!!\n");
            } else {
                LOGD("handleAudioDataImpl() 音频已经准备好,开始播放!!!\n");
            }

            // 回调(通知到java层)
            onPlayed();
            hope_to_get_a_good_result();
            runOneTime = false;
        }

        /***
         audioPts: 0.000000
         audioPos: 93.456848
         audioPts: 0.023220
         audioPos: 93.465261
         audioPts: 0.046440
         audioPos: 94.134739
         */
        audioPts = decodedAVFrame->pts * av_q2d(stream->time_base);
        if (isLive && preAudioPts > audioPts
            /*&& preAudioPts > 0 && audioPts > 0*/) {
            return 0;
        }
        preAudioPts = audioPts;

        // 有时长时才更新时间进度
        if (!isLive) {
            curProgress = (long long) audioPts;// 秒
            if (curProgress > preProgress) {
                if (curProgress <= mediaDuration) {
                    onProgressUpdated(curProgress);
                } else {
                    onProgressUpdated(mediaDuration);
                }
            }
            preProgress = curProgress;
        }

        if (videoWrapper->father->useMediaCodec) {
            /*if (needToResetVideoPts2
                && !needToGetResultAgain) {
                long long prePts = (long long) (preAudioPts * 1000000);
                long long curPts = (long long) (audioPts * 1000000);
                if (prePts != curPts) {
                    double tempTimeDifference = videoPts - audioPts;
                    //LOGE("handleVideoOutputBuffer()    timeDiff: %lf\n", tempTimeDifference);
                    if (tempTimeDifference < 0) {
                        int cycleNumber = 0;
                        while (true) {
                            if (tempTimeDifference > 0.0001) {
                                break;
                            }
                            if (isFrameByFrameMode
                                || videoWrapper->father->isPausedForUser
                                || videoWrapper->father->isPausedForCache
                                || videoWrapper->father->isPausedForSeek
                                || !videoWrapper->father->isHandling
                                || !audioWrapper->father->isHandling) {
                                LOGD("handleAudioOutputBuffer() tempTimeDifference return\n");
                                return 0;
                            }
                            audioPts = preAudioPts - (0.01 * (++cycleNumber));
                            tempTimeDifference = videoPts - audioPts;
                        }
                    }
                    //LOGI("handleVideoOutputBuffer()    timeDiff: %lf\n", tempTimeDifference);
                }
            }*/

            // 为了达到音视频同步,只能牺牲音频了.让音频慢下来.(个别视频会这样)
            while (audioPts - videoPts > 0
                   && !videoWrapper->father->isSleeping) {
                if (audioWrapper->father->isPausedForUser
                    || audioWrapper->father->isPausedForCache
                    || audioWrapper->father->isPausedForSeek
                    || !audioWrapper->father->isHandling
                    || !videoWrapper->father->isHandling) {
                    LOGD("handleAudioDataImpl() TIME_DIFFERENCE return\n");
                    audioWrapper->father->isSleeping = false;
                    return 0;
                }
                audioWrapper->father->isSleeping = true;
                av_usleep(1000);
            }
            audioWrapper->father->isSleeping = false;
        }

        // region 转换音频

        int ret = swr_convert(
                audioWrapper->swrContext,
                // 输出缓冲区
                &audioWrapper->father->outBuffer1,
                // 每通道采样的可用空间量
                MAX_AUDIO_FRAME_SIZE,
                // 输入缓冲区
                (const uint8_t **) decodedAVFrame->data,
                // 一个通道中可用的输入采样数量
                decodedAVFrame->nb_samples);

        if (ret < 0) {
            LOGE("handleAudioDataImpl() swr_convert failure: %d", ret);
            return ret;
        }

        // 获取给定音频参数所需的缓冲区大小
        int out_buffer_size = av_samples_get_buffer_size(
                nullptr,
                // 输出的声道个数
                audioWrapper->dstNbChannels,
                // 一个通道中音频采样数量
                decodedAVFrame->nb_samples,
                // 输出采样格式16bit
                audioWrapper->dstAVSampleFormat,
                // 缓冲区大小对齐(0 = 默认值,1 = 不对齐)
                1);
        // 把数据送到java层进行播放
        write(audioWrapper->father->outBuffer1, 0, out_buffer_size);

        // endregion

        return ret;
    }

    int handleVideoDataImpl(AVStream *stream, AVFrame *decodedAVFrame) {
        if (runOneTime) {
            videoWrapper->father->isStarted = true;
            if (!audioDisable) {
                while (!audioWrapper->father->isStarted) {
                    if (videoWrapper->father->isPausedForUser
                        || videoWrapper->father->isPausedForCache
                        || videoWrapper->father->isPausedForSeek
                        || !videoWrapper->father->isHandling
                        || !audioWrapper->father->isHandling) {
                        LOGI("handleVideoDataImpl() videoWrapper->father->isStarted return\n");
                        break;
                    }
                    av_usleep(1000);
                }
            } else {
                LOGD("handleVideoDataImpl() 视频已经准备好,开始播放!!!\n");
                onPlayed();
                hope_to_get_a_good_result();
                runOneTime = false;
            }
        }

        /***
         以音频为基准,同步视频到音频
         1.视频慢了则加快播放或丢掉部分视频帧
         2.视频快了则延迟播放,继续渲染上一帧
         音频需要正常播放才是好的体验
         */
        videoPts = decodedAVFrame->pts * av_q2d(stream->time_base);
        if (isLive && preVideoPts > videoPts
            /*&& preVideoPts > 0 && videoPts > 0*/) {
            return 0;
        }

        double tempTimeDifference = 0.0;
        // timeDifference = 0.040000
        if (videoPts > 0 && audioPts > 0 && !isFrameByFrameMode) {
            tempTimeDifference = videoPts - audioPts;
            //LOGI("handleVideoDataImpl() preVideoPts: %lf\n", preVideoPts);
            //LOGW("handleVideoDataImpl()    videoPts: %lf\n", videoPts);
            //LOGD("handleVideoDataImpl()    audioPts: %lf\n", audioPts);
            //LOGI("handleVideoDataImpl()    timeDiff: %lf\n", tempTimeDifference);

            // region 我去,找你找的好辛苦啊(折腾了好多天,才想到下面的方法)

            if (needToResetVideoPts && !needToGetResultAgain && isLive) {
                long long prePts = (long long) (preVideoPts * 1000000);
                long long curPts = (long long) (videoPts * 1000000);
                if (prePts != curPts) {
                    //LOGE("handleVideoDataImpl()    timeDiff: %lf\n", tempTimeDifference);
                    if (tempTimeDifference > 0) {
                        int cycleNumber = 0;
                        while (true) {
                            if (isFrameByFrameMode
                                || videoWrapper->father->isPausedForUser
                                || videoWrapper->father->isPausedForCache
                                || videoWrapper->father->isPausedForSeek
                                || !videoWrapper->father->isHandling
                                || !audioWrapper->father->isHandling) {
                                LOGW("handleVideoDataImpl() tempTimeDifference return\n");
                                return 0;
                            }
                            if (tempTimeDifference > 0.14) {
                                videoPts = preVideoPts - (0.01 * (++cycleNumber));
                            } else if (tempTimeDifference < 0.10) {
                                videoPts = preVideoPts + (0.01 * (++cycleNumber));
                            } else {
                                break;
                            }
                            tempTimeDifference = videoPts - audioPts;
                        }
                    }
                    //LOGI("handleVideoDataImpl()    timeDiff: %lf\n", tempTimeDifference);
                }
            }

            // endregion

            if (tempTimeDifference <= 0) {
                // 正常情况下videoTimeDifference比audioTimeDifference大一些
                // 如果发现小了,说明视频播放慢了,应丢弃这些帧
                // break后videoTimeDifference增长的速度会加快
                // videoPts = audioPts + averageTimeDiff;
                preVideoPts = videoPts;
                return 0;
            }

            // region 希望得到一个好结果(关键值:TIME_DIFFERENCE)

            if (runCounts <= RUN_COUNTS) {
                if (runCounts < RUN_COUNTS) {
                    timeDiff[runCounts++] = tempTimeDifference;
                } else /*if (runCounts == RUN_COUNTS)*/ {
                    runCounts++;
                    double totleTimeDiff = 0;
                    for (int i = 0; i < RUN_COUNTS; i++) {
                        if ((!audioDisable
                             && !audioWrapper->father->isHandling)
                            || !videoWrapper->father->isHandling) {
                            LOGW("handleVideoDataImpl() RUN_COUNTS return\n");
                            return 0;
                        }
                        totleTimeDiff += timeDiff[i];
                    }
                    averageTimeDiff = totleTimeDiff / RUN_COUNTS;
                    // 希望得到一个好的TIME_DIFFERENCE值
                    hope_to_get_a_good_result();
                }
            }

            // endregion

            // region 音视频同步操作

            if (tempTimeDifference > 2.000000) {
                videoPts = audioPts + averageTimeDiff;
            }
            // 如果videoTimeDifference比audioTimeDifference大出了一定的范围
            // 那么说明视频播放快了,应等待音频
            while (videoPts - audioPts > TIME_DIFFERENCE
                   && !audioWrapper->father->isSleeping) {
                if (isFrameByFrameMode
                    || videoWrapper->father->isPausedForUser
                    || videoWrapper->father->isPausedForCache
                    || videoWrapper->father->isPausedForSeek
                    || !videoWrapper->father->isHandling
                    || !audioWrapper->father->isHandling) {
                    LOGW("handleVideoDataImpl() TIME_DIFFERENCE return\n");
                    videoWrapper->father->isSleeping = false;
                    return 0;
                }
                videoWrapper->father->isSleeping = true;
                av_usleep(1000);
            }
            videoWrapper->father->isSleeping = false;

            // endregion
        }

        if (!videoWrapper->father->isHandling) {
            LOGW("handleVideoDataImpl() return\n");
            return 0;
        }

        // 单位: 毫秒
        if (audioDisable
            && !isFrameByFrameMode) {
            // 只有视频时
            av_usleep(11000);

            if (!isLive) {
                curProgress = (long long) videoPts;// 秒
                if (curProgress > preProgress) {
                    if (curProgress <= mediaDuration) {
                        onProgressUpdated(curProgress);
                    } else {
                        onProgressUpdated(mediaDuration);
                    }
                }
                preProgress = curProgress;
            }
        }

        preVideoPts = videoPts;

        // region 渲染画面

        // 3.lock锁定下一个即将要绘制的Surface
        ANativeWindow_lock(pANativeWindow, &mANativeWindow_Buffer, nullptr);

        // 把decodedAVFrame的数据经过格式转换后保存到rgbAVFrame中
        sws_scale(videoWrapper->swsContext,
                  (uint8_t const *const *) decodedAVFrame->data,
                  decodedAVFrame->linesize,
                  0,
                  videoWrapper->srcHeight,
                  videoWrapper->rgbAVFrame->data,
                  videoWrapper->rgbAVFrame->linesize);

        // 这段代码非常关键,还看不懂啥意思
        // 把rgbAVFrame里面的数据复制到outBuffer中就能渲染画面了
        uint8_t *src = videoWrapper->rgbAVFrame->data[0];
        // 一行的长度
        int srcStride = videoWrapper->rgbAVFrame->linesize[0];
        uint8_t *dst = (uint8_t *) mANativeWindow_Buffer.bits;
        int dstStride = mANativeWindow_Buffer.stride * 4;
        // 由于window的stride和帧的stride不同,因此需要逐行复制
        for (int h = 0; h < videoWrapper->srcHeight; h++) {
            if (!videoWrapper->father->isHandling) {
                LOGW("handleVideoDataImpl() memcpy return\n");
                ANativeWindow_unlockAndPost(pANativeWindow);
                return 0;
            }
            memcpy(dst + h * dstStride, src + h * srcStride, srcStride);
        }

        ////////////////////////////////////////////////////////

        // ......

        ////////////////////////////////////////////////////////

        // 6.unlock绘制
        ANativeWindow_unlockAndPost(pANativeWindow);

        // endregion
    }

    int handleAudioOutputBuffer(int roomIndex) {
        if (runOneTime) {
            audioWrapper->father->isStarted = true;
            if (!videoDisable) {
                while (!videoWrapper->father->isStarted) {
                    if (audioWrapper->father->isPausedForUser
                        || audioWrapper->father->isPausedForCache
                        || audioWrapper->father->isPausedForSeek
                        || !audioWrapper->father->isHandling
                        || !videoWrapper->father->isHandling
                            ) //
                    {
                        LOGD("handleAudioOutputBuffer() audioWrapper->father->isStarted return\n");
                        return 0;
                    }
                    av_usleep(1000);
                }
                LOGD("handleAudioOutputBuffer() 音视频已经准备好,开始播放!!!\n");
            } else {
                LOGD("handleAudioOutputBuffer() 音频已经准备好,开始播放!!!\n");
            }

            // 回调(通知到java层)
            onPlayed();
            hope_to_get_a_good_result();
            runOneTime = false;
        }

        if (!isLive) {
            curProgress = (long long) audioPts;// 秒
            if (curProgress > preProgress) {
                if (curProgress <= mediaDuration) {
                    onProgressUpdated(curProgress);
                } else {
                    onProgressUpdated(mediaDuration);
                }
            }
            preProgress = curProgress;
        }

        /*if (roomIndex < 0) {
            audioWrapper->father->useMediaCodec = false;
            resetForGetAGoodResult();
            return 0;
        }*/

        /*if (needToResetVideoPts2
            && !needToGetResultAgain) {
            long long prePts = (long long) (preAudioPts * 1000000);
            long long curPts = (long long) (audioPts * 1000000);
            if (prePts != curPts) {
                double tempTimeDifference = videoPts - audioPts;
                //LOGE("handleVideoOutputBuffer()    timeDiff: %lf\n", tempTimeDifference);
                if (tempTimeDifference < 0) {
                    int cycleNumber = 0;
                    while (true) {
                        if (tempTimeDifference > 0.0001) {
                            break;
                        }
                        if (isFrameByFrameMode
                            || videoWrapper->father->isPausedForUser
                            || videoWrapper->father->isPausedForCache
                            || videoWrapper->father->isPausedForSeek
                            || !videoWrapper->father->isHandling
                            || !audioWrapper->father->isHandling) {
                            LOGD("handleAudioOutputBuffer() tempTimeDifference return\n");
                            return 0;
                        }
                        audioPts = preAudioPts - (0.01 * (++cycleNumber));
                        tempTimeDifference = videoPts - audioPts;
                    }
                }
                //LOGI("handleVideoOutputBuffer()    timeDiff: %lf\n", tempTimeDifference);
            }
        }*/

        // region 为了达到音视频同步,只能牺牲点音频了.让音频慢下来.(个别视频会这样)

        while (audioPts - videoPts > 0
               && !videoWrapper->father->isSleeping) {
            if (audioWrapper->father->isPausedForUser
                || audioWrapper->father->isPausedForCache
                || audioWrapper->father->isPausedForSeek
                || !audioWrapper->father->isHandling
                || !videoWrapper->father->isHandling) {
                LOGD("handleAudioOutputBuffer() TIME_DIFFERENCE return\n");
                audioWrapper->father->isSleeping = false;
                return 0;
            }
            audioWrapper->father->isSleeping = true;
            av_usleep(1000);
        }
        audioWrapper->father->isSleeping = false;

        // endregion

        return 0;
    }

    int handleVideoOutputBuffer(int roomIndex) {
        if (isFrameByFrameMode) {
            preVideoPts = videoPts;
            return 0;
        }

        if (runOneTime) {
            videoWrapper->father->isStarted = true;
            if (!audioDisable) {
                while (!audioWrapper->father->isStarted) {
                    if (isFrameByFrameMode
                        || videoWrapper->father->isPausedForUser
                        || videoWrapper->father->isPausedForCache
                        || videoWrapper->father->isPausedForSeek
                        || !videoWrapper->father->isHandling
                        || !audioWrapper->father->isHandling
                            ) //
                    {
                        LOGW("handleVideoOutputBuffer() videoWrapper->father->isStarted return\n");
                        break;
                    }
                    av_usleep(1000);
                }
            } else {
                LOGW("handleVideoOutputBuffer() 视频已经准备好,开始播放!!!\n");
                onPlayed();
                hope_to_get_a_good_result();
                runOneTime = false;
            }
        }

        /*if (roomIndex < 0) {
            videoWrapper->father->useMediaCodec = false;
            audioWrapper->father->useMediaCodec = false;
            resetForGetAGoodResult();
            return 0;
        }*/

        // 没有音频,只有视频
        if (audioDisable) {
            if (videoWrapper->father->useMediaCodec) {
                av_usleep(videoSleepTime);
            } else {
                av_usleep(11000);
            }

            if (!isLive) {
                curProgress = (long long) videoPts;// 秒
                if (curProgress > preProgress) {
                    if (curProgress <= mediaDuration) {
                        onProgressUpdated(curProgress);
                    } else {
                        onProgressUpdated(mediaDuration);
                    }
                }
                preProgress = curProgress;
            }
            return 0;
        }

        if (videoPts > 0 && audioPts > 0) {
            double tempTimeDifference = videoPts - audioPts;
            //LOGI("handleVideoOutputBuffer() preVideoPts: %lf\n", preVideoPts);
            //LOGW("handleVideoOutputBuffer()    videoPts: %lf\n", videoPts);
            //LOGD("handleVideoOutputBuffer()    audioPts: %lf\n", audioPts);
            //LOGI("handleVideoOutputBuffer()    timeDiff: %lf\n", tempTimeDifference);

            // region 我去,找你找的好辛苦啊(折腾了好多天,才想到下面的方法)

            if (needToResetVideoPts && !needToGetResultAgain && isLive) {
                long long prePts = (long long) (preVideoPts * 1000000);
                long long curPts = (long long) (videoPts * 1000000);
                if (prePts != curPts) {
                    //LOGE("handleVideoOutputBuffer()    timeDiff: %lf\n", tempTimeDifference);
                    if (tempTimeDifference > 0) {
                        int cycleNumber = 0;
                        while (true) {
                            if (isFrameByFrameMode
                                || videoWrapper->father->isPausedForUser
                                || videoWrapper->father->isPausedForCache
                                || videoWrapper->father->isPausedForSeek
                                || !videoWrapper->father->isHandling
                                || !audioWrapper->father->isHandling) {
                                LOGW("handleVideoOutputBuffer() tempTimeDifference return\n");
                                return 0;
                            }
                            if (tempTimeDifference > 0.14) {
                                videoPts = preVideoPts - (0.01 * (++cycleNumber));
                            } else if (tempTimeDifference < 0.10) {
                                videoPts = preVideoPts + (0.01 * (++cycleNumber));
                            } else {
                                break;
                            }
                            tempTimeDifference = videoPts - audioPts;
                        }
                    }
                    //LOGI("handleVideoOutputBuffer()    timeDiff: %lf\n", tempTimeDifference);
                }
            }
            /*else if (needToResetVideoPts2
                       && !needToGetResultAgain) {
                long long prePts = (long long) (preVideoPts * 1000000);
                long long curPts = (long long) (videoPts * 1000000);
                if (prePts != curPts) {
                    //LOGE("handleVideoOutputBuffer()    timeDiff: %lf\n", tempTimeDifference);
                    if (tempTimeDifference > 0.000001) {
                        int cycleNumber = 0;
                        while (true) {
                            if (tempTimeDifference < 0.000001) {
                                break;
                            }
                            if (isFrameByFrameMode
                                || videoWrapper->father->isPausedForUser
                                || videoWrapper->father->isPausedForCache
                                || videoWrapper->father->isPausedForSeek
                                || !videoWrapper->father->isHandling
                                || !audioWrapper->father->isHandling) {
                                LOGW("handleVideoOutputBuffer() tempTimeDifference return\n");
                                return 0;
                            }
                            videoPts = preVideoPts - (0.01 * (++cycleNumber));
                            tempTimeDifference = videoPts - audioPts;
                        }
                    }
                    //LOGI("handleVideoOutputBuffer()    timeDiff: %lf\n", tempTimeDifference);
                }
            }*/

            // endregion

            if (tempTimeDifference <= 0) {
                // 正常情况下videoTimeDifference比audioTimeDifference大一些
                // 如果发现小了,说明视频播放慢了,应丢弃这些帧
                // break后videoTimeDifference增长的速度会加快
                // videoPts = audioPts + averageTimeDiff;
                preVideoPts = videoPts;
                return 0;
            }

            // region 希望得到一个好结果(关键值:TIME_DIFFERENCE)

            if (runCounts <= RUN_COUNTS) {
                if (runCounts < RUN_COUNTS) {
                    timeDiff[runCounts++] = tempTimeDifference;
                } else /*if (runCounts == RUN_COUNTS)*/ {
                    runCounts++;
                    double totleTimeDiff = 0;
                    for (int i = 0; i < RUN_COUNTS; i++) {
                        if ((!audioDisable
                             && !audioWrapper->father->isHandling)
                            || !videoWrapper->father->isHandling) {
                            LOGW("handleVideoOutputBuffer() RUN_COUNTS return\n");
                            return 0;
                        }
                        totleTimeDiff += timeDiff[i];
                    }
                    averageTimeDiff = totleTimeDiff / RUN_COUNTS;
                    // 希望得到一个好的TIME_DIFFERENCE值
                    hope_to_get_a_good_result();
                }
            }

            // endregion

            // region 音视频同步操作

            if (tempTimeDifference > 2.000000) {
                videoPts = audioPts + averageTimeDiff;
            }
            while (videoPts - audioPts > TIME_DIFFERENCE
                   && !audioWrapper->father->isSleeping) {
                if (isFrameByFrameMode
                    || videoWrapper->father->isPausedForUser
                    || videoWrapper->father->isPausedForCache
                    || videoWrapper->father->isPausedForSeek
                    || !videoWrapper->father->isHandling
                    || !audioWrapper->father->isHandling) {
                    LOGW("handleVideoOutputBuffer() TIME_DIFFERENCE return\n");
                    videoWrapper->father->isSleeping = false;
                    return 0;
                }
                videoWrapper->father->isSleeping = true;
                av_usleep(1000);
            }
            videoWrapper->father->isSleeping = false;

            // endregion
        }

        return 0;
    }

    int handleDataClose(Wrapper *wrapper) {
        if (audioWrapper == nullptr
            || videoWrapper == nullptr
            || wrapper == nullptr) {
            LOGF("handleDataClose() finish nullptr\n");
            return 0;
        }

        // 让"读线程"退出
        notifyToRead();

        if (wrapper->type == TYPE_AUDIO) {
            audioPts = 0.0;
            LOGD("handleDataClose() for (;;) audio end\n");
            while (isReading) {
                av_usleep(100000);
            }
            LOGF("handleDataClose() audio end\n");

            LOGD("handleDataClose() audio                   list1: %d\n",
                 audioWrapper->father->list1->size());
            LOGD("handleDataClose() audio                   list2: %d\n",
                 audioWrapper->father->list2->size());
            LOGW("handleDataClose() video                   list1: %d\n",
                 videoWrapper->father->list1->size());
            LOGW("handleDataClose() video                   list2: %d\n",
                 videoWrapper->father->list2->size());

            /*int64_t startTime = av_gettime_relative();
            int64_t endTime = -1;*/
            LOGD("handleDataClose() audio isVideoHandling start\n");
            while (isVideoHandling) {
                /*endTime = av_gettime_relative();
                if ((endTime - startTime) >= 15000000) {
                    LOGE("Exception Exit\n");
                    break;
                }*/
                av_usleep(100000);
            }
            LOGD("handleDataClose() audio isVideoHandling end\n");
            if (!audioDisable
                && audioWrapper->father->avCodecContext != nullptr) {
                avcodec_flush_buffers(audioWrapper->father->avCodecContext);
            }
            if (!videoDisable
                && videoWrapper->father->avCodecContext != nullptr) {
                avcodec_flush_buffers(videoWrapper->father->avCodecContext);
            }
            closeAudio();
            closeVideo();
            closeOther();
            // 必须保证每次退出都要执行到
            onFinished();
            LOGF("%s\n", "Safe Exit");
        } else {
            LOGW("handleDataClose() for (;;) video end\n");
            isVideoHandling = false;
            if (audioDisable) {
                stop();
            }
            LOGF("handleDataClose() video end\n");
        }
    }

    void *handleData(void *opaque) {
        if (!opaque) {
            return nullptr;
        }
        Wrapper *wrapper = nullptr;
        int *type = (int *) opaque;
        if (*type == TYPE_AUDIO) {
            wrapper = audioWrapper->father;
        } else {
            wrapper = videoWrapper->father;
        }
        if (!wrapper) {
            LOGE("%s\n", "wrapper is nullptr");
            return nullptr;
        }

        // 线程等待
        if (wrapper->type == TYPE_AUDIO) {
            LOGD("handleData() wait() audio start\n");
        } else {
            LOGW("handleData() wait() video start\n");
        }
        // 有视频无音频或者有音频无视频的情况,没有的一方被挡在这里
        notifyToHandleWait(wrapper);
        if (wrapper->type == TYPE_AUDIO) {
            LOGD("handleData() wait() audio end\n");
        } else {
            LOGW("handleData() wait() video end\n");
        }

        if (!wrapper->isHandling) {
            handleDataClose(wrapper);
            return nullptr;
        }

        if (wrapper->type == TYPE_AUDIO) {
            LOGD("handleData() for (;;) audio start\n");
        } else {
            // LOGW("handleData() ANativeWindow_setBuffersGeometry() start\n");
            // 2.设置缓冲区的属性（宽、高、像素格式）,像素格式要和SurfaceView的像素格式一直
            ANativeWindow_setBuffersGeometry(pANativeWindow,
                                             videoWrapper->srcWidth,
                                             videoWrapper->srcHeight,
                                             WINDOW_FORMAT_RGBA_8888);
            // LOGW("handleData() ANativeWindow_setBuffersGeometry() end\n");
            LOGW("handleData() for (;;) video start\n");
            isVideoHandling = true;
        }

        AVStream *stream = avFormatContext->streams[wrapper->streamIndex];
        AVPacket *srcAVPacket/* = av_packet_alloc()*/;
        AVPacket *copyAVPacket = av_packet_alloc();
        // decodedAVFrame为解码后的数据
        // flags: 0, pts: 118803601, pkt_pos: 376, pkt_duration: 0, pkt_size: 104689
        // flags: 0, pts: 119228401, pkt_pos: 871192, pkt_duration: 3600, pkt_size: 7599
        AVFrame *decodedAVFrame = nullptr;
        // flags: 0, pts: -9223372036854775808, pkt_pos: -1, pkt_duration: 0, pkt_size: -1
        AVFrame *preAudioAVFrame = nullptr;
        AVFrame *preVideoAVFrame = nullptr;
        if (wrapper->type == TYPE_AUDIO) {
            decodedAVFrame = audioWrapper->decodedAVFrame;
            preAudioAVFrame = av_frame_alloc();
        } else {
            decodedAVFrame = videoWrapper->decodedAVFrame;
            preVideoAVFrame = av_frame_alloc();
        }

        bool feedAndDrainRet = false;
        int ret = 0;
        for (;;) {
            if (!wrapper->isHandling) {
                // for (;;) end
                break;
            }

            // region 暂停装置

            if (wrapper->isPausedForUser
                || wrapper->isPausedForCache
                || wrapper->isPausedForSeek) {
                bool isPausedForSeek = wrapper->isPausedForSeek;
                bool isPausedForUser = wrapper->isPausedForUser;
                bool isPausedForCache = wrapper->isPausedForCache;
                if (isPausedForSeek) {
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGD("handleData() wait() Seek  audio start\n");
                    } else {
                        LOGW("handleData() wait() Seek  video start\n");
                    }
                    if (wrapper->list1->size() != 0) {
                        std::list<AVPacket>::iterator iter;
                        for (iter = wrapper->list1->begin();
                             iter != wrapper->list1->end();
                             iter++) {
                            AVPacket avPacket = *iter;
                            av_packet_unref(&avPacket);
                        }
                        wrapper->list1->clear();
                        if (wrapper->type == TYPE_AUDIO) {
                            onLoadProgressUpdated(MSG_ON_TRANSACT_AUDIO_CONSUMER, 0);
                        } else {
                            onLoadProgressUpdated(MSG_ON_TRANSACT_VIDEO_CONSUMER, 0);
                        }
                    }
                    wrapper->isHandleList1Full = false;
                    wrapper->needToSeek = true;
                } else if (isPausedForUser) {
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGD("handleData() wait() User  audio start\n");
                    } else {
                        LOGW("handleData() wait() User  video start\n");
                    }
                } else if (isPausedForCache) {
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGE("handleData() wait() Cache audio start 被动暂停\n");
                    } else {
                        LOGE("handleData() wait() Cache video start 被动暂停\n");
                    }
                }

                notifyToHandleWait(wrapper);
                if (!wrapper->isHandling) {
                    // for (;;) end
                    break;
                }
                if (wrapper->isPausedForUser
                    || wrapper->isPausedForCache
                    || wrapper->isPausedForSeek) {
                    continue;
                }

                if (isPausedForSeek) {
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGD("handleData() wait() Seek  audio end\n");
                    } else {
                        LOGW("handleData() wait() Seek  video end\n");
                    }
                } else if (isPausedForUser) {
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGD("handleData() wait() User  audio end\n");
                    } else {
                        LOGW("handleData() wait() User  video end\n");
                    }
                } else if (isPausedForCache) {
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGE("handleData() wait() Cache audio end   被动暂停\n");
                    } else {
                        LOGE("handleData() wait() Cache video end   被动暂停\n");
                    }
                }
            }

            // endregion

            // region 从队列中取出一个AVPacket(copyAVPacket)进行处理

            wrapper->allowDecode = false;
            if (wrapper->list1->size() > 0) {
                srcAVPacket = &wrapper->list1->front();
                av_packet_ref(copyAVPacket, srcAVPacket);
                av_packet_unref(srcAVPacket);
                wrapper->list1->pop_front();
                wrapper->handleFramesCount++;
                wrapper->allowDecode = true;
            }

            size_t list1Size = wrapper->list1->size();
            if (wrapper->type == TYPE_AUDIO) {
                if (list1Size % 10 == 0) {
                    onLoadProgressUpdated(MSG_ON_TRANSACT_AUDIO_CONSUMER, list1Size);
                }
            } else {
                if (list1Size % 10 == 0) {
                    onLoadProgressUpdated(MSG_ON_TRANSACT_VIDEO_CONSUMER, list1Size);
                }
            }

            if (isReadWaited
                && ((!audioDisable
                     && audioWrapper->father->isReading
                     && audioWrapper->father->list2->size() <
                        audioWrapper->father->list2LimitCounts)
                    ||
                    (!videoDisable
                     && videoWrapper->father->isReading
                     && videoWrapper->father->list2->size() <
                        videoWrapper->father->list2LimitCounts))) {
                if (!isLocal) {
                    LOGI("handleData() notifyToRead\n");
                }
                notifyToRead();
            }

            // endregion

            // region 复制数据

            if (wrapper->isReading) {
                if (wrapper->list1->size() == 0) {
                    wrapper->isHandleList1Full = false;
                    if (wrapper->list2->size() > 0) {
                        pthread_mutex_lock(&wrapper->readLockMutex);
                        wrapper->list1->clear();
                        wrapper->list1->assign(wrapper->list2->begin(), wrapper->list2->end());
                        wrapper->list2->clear();
                        wrapper->isHandleList1Full = true;
                        pthread_mutex_unlock(&wrapper->readLockMutex);

                        // LOGI("===================================================\n");
                        if (wrapper->type == TYPE_AUDIO) {
                            onLoadProgressUpdated(
                                    MSG_ON_TRANSACT_AUDIO_PRODUCER, 0);
                            onLoadProgressUpdated(
                                    MSG_ON_TRANSACT_AUDIO_CONSUMER, wrapper->list1->size());
                            /*if (!isLocal) {
                                LOGD("handleData() audio 接下去要处理的数据有 list1: %d\n",
                                     wrapper->list1->size());
                                LOGD("handleData() audio                   list2: %d\n",
                                     wrapper->list2->size());
                                LOGW("handleData() video 接下去要处理的数据有 list1: %d\n",
                                     videoWrapper->father->list1->size());
                                LOGW("handleData() video                   list2: %d\n",
                                     videoWrapper->father->list2->size());
                            }*/
                        } else {
                            onLoadProgressUpdated(
                                    MSG_ON_TRANSACT_VIDEO_PRODUCER, 0);
                            onLoadProgressUpdated(
                                    MSG_ON_TRANSACT_VIDEO_CONSUMER, wrapper->list1->size());
                            /*if (!isLocal) {
                                LOGW("handleData() video 接下去要处理的数据有 list1: %d\n",
                                     wrapper->list1->size());
                                LOGW("handleData() video                   list2: %d\n",
                                     wrapper->list2->size());
                                LOGD("handleData() audio 接下去要处理的数据有 list1: %d\n",
                                     audioWrapper->father->list1->size());
                                LOGD("handleData() audio                   list2: %d\n",
                                     audioWrapper->father->list2->size());
                            }*/
                        }
                        // LOGI("===================================================\n");
                    }
                    notifyToRead();
                }
            } else {
                if (wrapper->list1->size() > 0) {
                    // 还有数据,先用完再说
                } else {
                    if (wrapper->list2->size() > 0) {
                        // 把剩余的数据全部复制过来
                        pthread_mutex_lock(&wrapper->readLockMutex);
                        wrapper->list1->clear();
                        wrapper->list1->assign(wrapper->list2->begin(), wrapper->list2->end());
                        wrapper->list2->clear();
                        pthread_mutex_unlock(&wrapper->readLockMutex);

                        LOGI("===================================================\n");
                        if (wrapper->type == TYPE_AUDIO) {
                            onLoadProgressUpdated(
                                    MSG_ON_TRANSACT_AUDIO_PRODUCER, 0);
                            onLoadProgressUpdated(
                                    MSG_ON_TRANSACT_AUDIO_CONSUMER, wrapper->list1->size());
                            LOGD("handleData() audio 最后要处理的数据还有 list1: %d\n",
                                 wrapper->list1->size());
                            LOGD("handleData() audio                   list2: %d\n",
                                 wrapper->list2->size());
                            LOGW("handleData() video                   list1: %d\n",
                                 videoWrapper->father->list1->size());
                            LOGW("handleData() video                   list2: %d\n",
                                 videoWrapper->father->list2->size());
                        } else {
                            onLoadProgressUpdated(
                                    MSG_ON_TRANSACT_VIDEO_PRODUCER, 0);
                            onLoadProgressUpdated(
                                    MSG_ON_TRANSACT_VIDEO_CONSUMER, wrapper->list1->size());
                            LOGW("handleData() video 最后要处理的数据还有 list1: %d\n",
                                 wrapper->list1->size());
                            LOGW("handleData() video                   list2: %d\n",
                                 wrapper->list2->size());
                            LOGD("handleData() audio                   list1: %d\n",
                                 audioWrapper->father->list1->size());
                            LOGD("handleData() audio                   list2: %d\n",
                                 audioWrapper->father->list2->size());
                        }
                        LOGI("===================================================\n");
                    } else {
                        wrapper->isHandling = false;
                        if (wrapper->type == TYPE_AUDIO) {
                            LOGD("handleData() audio wrapper->isHandling is false\n");
                        } else {
                            LOGW("handleData() video wrapper->isHandling is false\n");
                        }
                    }
                }
            }

            // endregion

            // region 缓冲处理

            if (wrapper->isReading
                && wrapper->isHandling
                && !wrapper->isHandleList1Full
                && wrapper->list1->size() == 0
                && wrapper->list2->size() == 0) {// 本地视频一般是走不进去的

                LOGE("---------------------------------------------------\n");
                if (wrapper->type == TYPE_AUDIO) {
                    LOGD("handleData() audio                   list1: %d\n",
                         wrapper->list1->size());
                    LOGD("handleData() audio                   list2: %d\n",
                         wrapper->list2->size());
                    LOGW("handleData() video                   list1: %d\n",
                         videoWrapper->father->list1->size());
                    LOGW("handleData() video                   list2: %d\n",
                         videoWrapper->father->list2->size());
                } else {
                    LOGW("handleData() video                   list1: %d\n",
                         wrapper->list1->size());
                    LOGW("handleData() video                   list2: %d\n",
                         wrapper->list2->size());
                    LOGD("handleData() audio                   list1: %d\n",
                         audioWrapper->father->list1->size());
                    LOGD("handleData() audio                   list2: %d\n",
                         audioWrapper->father->list2->size());
                }
                LOGE("---------------------------------------------------\n");

                // 开始暂停
                onPaused();

                // 通知"读"
                notifyToRead();
                if (wrapper->type == TYPE_AUDIO) {
                    // 音频Cache引起的暂停
                    LOGE("handleData() wait() Cache audio start 主动暂停\n");
                    // 让视频同时暂停
                    videoWrapper->father->isPausedForCache = true;
                    audioWrapper->father->isPausedForCache = true;
                    // 音频自身暂停
                    notifyToHandleWait(audioWrapper->father);
                    audioWrapper->father->isPausedForCache = false;
                    videoWrapper->father->isPausedForCache = false;
                    LOGE("handleData() wait() Cache audio end   主动暂停\n");
                    if (wrapper->isPausedForSeek) {
                        if (wrapper->allowDecode) {
                            av_packet_unref(copyAVPacket);
                        }
                        continue;
                    }
                    if (!videoDisable) {
                        // 通知视频结束暂停
                        notifyToHandle(videoWrapper->father);
                    }
                } else {
                    // 视频Cache引起的暂停
                    LOGE("handleData() wait() Cache video start 主动暂停\n");
                    // 让音频同时暂停
                    audioWrapper->father->isPausedForCache = true;
                    videoWrapper->father->isPausedForCache = true;
                    // 视频自身暂停
                    notifyToHandleWait(videoWrapper->father);
                    audioWrapper->father->isPausedForCache = false;
                    videoWrapper->father->isPausedForCache = false;
                    LOGE("handleData() wait() Cache video end   主动暂停\n");
                    if (wrapper->isPausedForSeek) {
                        if (wrapper->allowDecode) {
                            av_packet_unref(copyAVPacket);
                        }
                        continue;
                    }
                    if (!audioDisable) {
                        // 通知音频结束暂停
                        notifyToHandle(audioWrapper->father);
                    }
                }

                if (!wrapper->isHandling) {
                    // for (;;) end
                    break;
                }

                if (!audioWrapper->father->isPausedForUser
                    && !videoWrapper->father->isPausedForUser) {
                    // 开始播放
                    onPlayed();
                }

                LOGI("***************************************************\n");
                if (wrapper->type == TYPE_AUDIO) {
                    LOGD("handleData() audio                   list1: %d\n",
                         wrapper->list1->size());
                    LOGD("handleData() audio                   list2: %d\n",
                         wrapper->list2->size());
                    LOGW("handleData() video                   list1: %d\n",
                         videoWrapper->father->list1->size());
                    LOGW("handleData() video                   list2: %d\n",
                         videoWrapper->father->list2->size());
                } else {
                    LOGW("handleData() video                   list1: %d\n",
                         wrapper->list1->size());
                    LOGW("handleData() video                   list2: %d\n",
                         wrapper->list2->size());
                    LOGD("handleData() audio                   list1: %d\n",
                         audioWrapper->father->list1->size());
                    LOGD("handleData() audio                   list2: %d\n",
                         audioWrapper->father->list2->size());
                }
                LOGI("***************************************************\n");
            }

            // endregion

            if (!wrapper->allowDecode) {
                continue;
            }

            // region 硬解码过程

            if (wrapper->type == TYPE_AUDIO) {
                if (wrapper->useMediaCodec) {
                    audioPts = copyAVPacket->pts * av_q2d(stream->time_base);
                    if (isLive && preAudioPts > audioPts) {
                        continue;
                    }

                    feedAndDrainRet = feedInputBufferAndDrainOutputBuffer(
                            0x0001,
                            copyAVPacket->data,
                            copyAVPacket->size,
                            (long long) copyAVPacket->pts);
                    av_packet_unref(copyAVPacket);
                    preAudioPts = audioPts;

                    if (!feedAndDrainRet && wrapper->isHandling) {
                        LOGE("handleData() audio feedInputBufferAndDrainOutputBuffer failure\n");
                        audioWrapper->father->useMediaCodec = false;
                        resetForGetAGoodResult();
                    }

                    continue;
                }
            } else {
                if (wrapper->useMediaCodec) {
                    videoPts = copyAVPacket->pts * av_q2d(stream->time_base);
                    if (isLive && preVideoPts > videoPts) {
                        continue;
                    }

                    if (isFrameByFrameMode) {
                        long long prePts = (long long) (preVideoPts * 1000000);
                        long long curPts = (long long) (videoPts * 1000000);
                        LOGW("handleData() curPts: %lld prePts: %lld\n", curPts, prePts);
                        if (curPts > prePts) {
                            LOGW("handleData() wait() Step  video start\n");
                            notifyToHandleWait(wrapper);
                            LOGW("handleData() wait() Step  video end\n");
                        }
                    }

                    feedAndDrainRet = feedInputBufferAndDrainOutputBuffer(
                            0x0002,
                            copyAVPacket->data,
                            copyAVPacket->size,
                            (long long) copyAVPacket->pts);
                    av_packet_unref(copyAVPacket);
                    preVideoPts = videoPts;

                    if (!feedAndDrainRet && wrapper->isHandling) {
                        LOGE("handleData() video feedInputBufferAndDrainOutputBuffer failure\n");
                        videoWrapper->father->useMediaCodec = false;
                        audioWrapper->father->useMediaCodec = false;
                        resetForGetAGoodResult();
                    }

                    continue;
                }
            }

            // endregion

            // region 软解码过程

            ret = avcodec_send_packet(wrapper->avCodecContext, copyAVPacket);
            av_packet_unref(copyAVPacket);
            switch (ret) {
                case AVERROR(EAGAIN):
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGE("handleData() audio avcodec_send_packet   ret: %d\n", ret);// -11
                    } else {
                        LOGE("handleData() video avcodec_send_packet   ret: %d\n", ret);
                    }
                    break;
                case AVERROR(EINVAL):
                case AVERROR(ENOMEM):
                case AVERROR_EOF:
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGE("audio 发送数据包到解码器时出错 %d", ret);// -22
                    } else {
                        LOGE("video 发送数据包到解码器时出错 %d", ret);
                    }
                    //wrapper->isHandling = false;
                    break;
                case 0:
                    break;
                default:
                    if (wrapper->type == TYPE_AUDIO) {
                        // audio 发送数据包时出现异常 -50531338
                        LOGE("audio 发送数据包时出现异常 %d", ret);// -1094995529
                    } else {
                        LOGE("video 发送数据包时出现异常 %d", ret);// -1094995529
                    }
                    break;
            }// switch (ret) end

            if (!ret) {
                ret = avcodec_receive_frame(wrapper->avCodecContext, decodedAVFrame);
                switch (ret) {
                    // 输出是不可用的,必须发送新的输入
                    case AVERROR(EAGAIN):
                        if (wrapper->type == TYPE_AUDIO) {
                            LOGE("handleData() audio avcodec_receive_frame ret: %d\n", ret);
                        } else {
                            //LOGE("handleData() video avcodec_receive_frame ret: %d\n", ret);// -11
                        }
                        break;
                    case AVERROR(EINVAL):
                        // codec打不开,或者是一个encoder
                    case AVERROR_EOF:
                        // 已经完全刷新,不会再有输出帧了
                        if (wrapper->type == TYPE_AUDIO) {
                            LOGE("audio 从解码器接收解码帧时出错 %d", ret);
                        } else {
                            LOGE("video 从解码器接收解码帧时出错 %d", ret);
                        }
                        //wrapper->isHandling = false;
                        break;
                    case 0: {
                        // 解码成功,返回一个输出帧
                        if (wrapper->type == TYPE_AUDIO) {
                            //av_frame_unref(preAudioAVFrame);
                            //av_frame_ref(preAudioAVFrame, decodedAVFrame);
                        } else {
                            /*LOGW("handleData() video                   "
                                 "flags: %d, pts: %lld, pkt_pos: %lld, pkt_duration: %lld, pkt_size: %d\n",
                                 decodedAVFrame->flags,
                                 (long long) decodedAVFrame->pts,
                                 (long long) decodedAVFrame->pkt_pos,
                                 (long long) decodedAVFrame->pkt_duration,
                                 decodedAVFrame->pkt_size);*/
                            // 下面两个的作用一样,不能用av_frame_copy(...)这个方法,不起作用
                            av_frame_unref(preVideoAVFrame);
                            av_frame_ref(preVideoAVFrame, decodedAVFrame);
                            //preVideoAVFrame = av_frame_clone(decodedAVFrame);
                        }
                        break;
                    }
                    default:
                        if (wrapper->type == TYPE_AUDIO) {
                            LOGE("audio 接收解码帧时出现异常 %d", ret);
                        } else {
                            LOGE("video 接收解码帧时出现异常 %d", ret);
                        }
                        break;
                }// switch (ret) end

                if (ret != 0) {
                    av_frame_unref(decodedAVFrame);
                    if (wrapper->type == TYPE_AUDIO && preAudioAVFrame->pkt_size > 0) {
                        av_frame_ref(decodedAVFrame, preAudioAVFrame);
                        LOGD("handleData() audio av_frame_ref\n");
                    } else if (wrapper->type == TYPE_VIDEO && preVideoAVFrame->pkt_size > 0) {
                        av_frame_ref(decodedAVFrame, preVideoAVFrame);
                        //http://101.71.255.229:6610/zjhs/2/10106/index.m3u8?virtualDomain=zjhs.live_hls.zte.com
                        //http://101.71.255.229:6610/zjhs/2/10109/index.m3u8?virtualDomain=zjhs.live_hls.zte.com
                        //LOGW("handleData() video av_frame_ref\n");
                    } else {
                        continue;
                    }
                }
            } else {
                continue;
            }

            // endregion

            // region 处理软解码后的数据(播放声音和渲染画面)

            if (wrapper->type == TYPE_AUDIO) {
                handleAudioDataImpl(stream, decodedAVFrame);
            } else {
                handleVideoDataImpl(stream, decodedAVFrame);
            }
            av_frame_unref(decodedAVFrame);

            // endregion
        }//for(;;) end

        if (wrapper->type == TYPE_AUDIO) {
            LOGD("handleData() end 1\n");
        } else {
            LOGW("handleData() end 1\n");
        }
        if (srcAVPacket != nullptr) {
            // av_packet_unref(srcAVPacket);
            // app crash 上面的copyAVPacket调用却没事,why
            // av_packet_free(&srcAVPacket);
            srcAVPacket = nullptr;
        }
        if (wrapper->type == TYPE_AUDIO) {
            LOGD("handleData() end 2\n");
        } else {
            LOGW("handleData() end 2\n");
        }
        if (copyAVPacket != nullptr) {
            av_packet_free(&copyAVPacket);
            copyAVPacket = nullptr;
        }
        if (wrapper->type == TYPE_AUDIO) {
            LOGD("handleData() end 3\n");
        } else {
            LOGW("handleData() end 3\n");
        }
        if (wrapper->type == TYPE_AUDIO) {
            if (preAudioAVFrame != nullptr) {
                av_frame_unref(preAudioAVFrame);
                av_frame_free(&preAudioAVFrame);
                preAudioAVFrame = nullptr;
            }
        } else {
            if (preVideoAVFrame != nullptr) {
                av_frame_unref(preVideoAVFrame);
                av_frame_free(&preVideoAVFrame);
                preVideoAVFrame = nullptr;
            }
        }
        if (wrapper->type == TYPE_AUDIO) {
            LOGD("handleData() end 4\n");
        } else {
            LOGW("handleData() end 4\n");
        }
        handleDataClose(wrapper);

        return nullptr;
    }

    void closeAudio() {
        if (audioWrapper == nullptr
            || audioWrapper->father == nullptr) {
            return;
        }
        LOGD("%s\n", "closeAudio() start");
        if (audioWrapper->father->outBuffer1 != nullptr) {
            av_free(audioWrapper->father->outBuffer1);
            audioWrapper->father->outBuffer1 = nullptr;
        }
        if (audioWrapper->father->outBuffer2 != nullptr) {
            av_free(audioWrapper->father->outBuffer2);
            audioWrapper->father->outBuffer2 = nullptr;
        }
        if (audioWrapper->father->outBuffer3 != nullptr) {
            av_free(audioWrapper->father->outBuffer3);
            audioWrapper->father->outBuffer3 = nullptr;
        }
        /*if (audioWrapper->father->srcData[0] != nullptr) {
            av_freep(&audioWrapper->father->srcData[0]);
            audioWrapper->father->srcData[0] = nullptr;
        }
        if (audioWrapper->father->dstData[0] != nullptr) {
            av_freep(&audioWrapper->father->dstData[0]);
            audioWrapper->father->dstData[0] = nullptr;
        }*/
        if (audioWrapper->swrContext != nullptr) {
            swr_free(&audioWrapper->swrContext);
            audioWrapper->swrContext = nullptr;
        }
        if (audioWrapper->decodedAVFrame != nullptr) {
            av_frame_unref(audioWrapper->decodedAVFrame);
            av_frame_free(&audioWrapper->decodedAVFrame);
            audioWrapper->decodedAVFrame = nullptr;
        }
        if (audioWrapper->father->avCodecContext != nullptr) {
            //没有使用过avCodecContext的话,不能调用avcodec_flush_buffers
            //avcodec_flush_buffers(audioWrapper->father->avCodecContext);
            avcodec_close(audioWrapper->father->avCodecContext);
            av_free(audioWrapper->father->avCodecContext);
            audioWrapper->father->avCodecContext = nullptr;
        }
        pthread_mutex_destroy(&audioWrapper->father->readLockMutex);
        pthread_cond_destroy(&audioWrapper->father->readLockCondition);
        pthread_mutex_destroy(&audioWrapper->father->handleLockMutex);
        pthread_cond_destroy(&audioWrapper->father->handleLockCondition);

        if (audioWrapper->father->list1->size() != 0) {
            LOGD("closeAudio() list1 is not empty, %d\n", audioWrapper->father->list1->size());
            std::list<AVPacket>::iterator iter;
            for (iter = audioWrapper->father->list1->begin();
                 iter != audioWrapper->father->list1->end();
                 iter++) {
                AVPacket avPacket = *iter;
                av_packet_unref(&avPacket);
            }
        }
        if (audioWrapper->father->list2->size() != 0) {
            LOGD("closeAudio() list2 is not empty, %d\n", audioWrapper->father->list2->size());
            std::list<AVPacket>::iterator iter;
            for (iter = audioWrapper->father->list2->begin();
                 iter != audioWrapper->father->list2->end();
                 iter++) {
                AVPacket avPacket = *iter;
                av_packet_unref(&avPacket);
            }
        }
        delete (audioWrapper->father->list1);
        delete (audioWrapper->father->list2);
        audioWrapper->father->list1 = nullptr;
        audioWrapper->father->list2 = nullptr;
        if (audioWrapper->father->avbsfContext != nullptr) {
            av_bsf_free(&audioWrapper->father->avbsfContext);
            audioWrapper->father->avbsfContext = nullptr;
        }

        av_free(audioWrapper->father);
        audioWrapper->father = nullptr;
        av_free(audioWrapper);
        audioWrapper = nullptr;
        LOGD("%s\n", "closeAudio() end");
    }

    void closeVideo() {
        if (pANativeWindow != nullptr) {
            // 7.释放资源
            ANativeWindow_release(pANativeWindow);
            pANativeWindow = nullptr;
        }
        if (videoWrapper == nullptr
            || videoWrapper->father == nullptr) {
            return;
        }
        LOGW("%s\n", "closeVideo() start");
        if (videoWrapper->father->outBuffer1 != nullptr) {
            av_free(videoWrapper->father->outBuffer1);
            videoWrapper->father->outBuffer1 = nullptr;
        }
        if (videoWrapper->father->outBuffer2 != nullptr) {
            av_free(videoWrapper->father->outBuffer2);
            videoWrapper->father->outBuffer2 = nullptr;
        }
        if (videoWrapper->father->outBuffer3 != nullptr) {
            av_free(videoWrapper->father->outBuffer3);
            videoWrapper->father->outBuffer3 = nullptr;
        }
        /*if (videoWrapper->father->srcData[0] != nullptr) {
            av_freep(&videoWrapper->father->srcData[0]);
            videoWrapper->father->srcData[0] = nullptr;
        }
        if (videoWrapper->father->dstData[0] != nullptr) {
            av_freep(&videoWrapper->father->dstData[0]);
            videoWrapper->father->dstData[0] = nullptr;
        }*/
        if (videoWrapper->swsContext != nullptr) {
            sws_freeContext(videoWrapper->swsContext);
            videoWrapper->swsContext = nullptr;
        }
        if (videoWrapper->decodedAVFrame != nullptr) {
            av_frame_unref(videoWrapper->decodedAVFrame);
            av_frame_free(&videoWrapper->decodedAVFrame);
            videoWrapper->decodedAVFrame = nullptr;
        }
        if (videoWrapper->rgbAVFrame != nullptr) {
            av_frame_free(&videoWrapper->rgbAVFrame);
            videoWrapper->rgbAVFrame = nullptr;
        }
        if (videoWrapper->father->avCodecContext != nullptr) {
            //没有使用过avCodecContext的话,不能调用avcodec_flush_buffers
            //avcodec_flush_buffers(videoWrapper->father->avCodecContext);
            avcodec_close(videoWrapper->father->avCodecContext);
            av_free(videoWrapper->father->avCodecContext);
            videoWrapper->father->avCodecContext = nullptr;
        }

        pthread_mutex_destroy(&videoWrapper->father->readLockMutex);
        pthread_cond_destroy(&videoWrapper->father->readLockCondition);
        pthread_mutex_destroy(&videoWrapper->father->handleLockMutex);
        pthread_cond_destroy(&videoWrapper->father->handleLockCondition);

        if (videoWrapper->father->list1->size() != 0) {
            LOGW("closeVideo() list1 is not empty, %d\n", videoWrapper->father->list1->size());
            std::list<AVPacket>::iterator iter;
            for (iter = videoWrapper->father->list1->begin();
                 iter != videoWrapper->father->list1->end();
                 iter++) {
                AVPacket avPacket = *iter;
                av_packet_unref(&avPacket);
            }
        }
        if (videoWrapper->father->list2->size() != 0) {
            LOGW("closeVideo() list2 is not empty, %d\n", videoWrapper->father->list2->size());
            std::list<AVPacket>::iterator iter;
            for (iter = videoWrapper->father->list2->begin();
                 iter != videoWrapper->father->list2->end();
                 iter++) {
                AVPacket avPacket = *iter;
                av_packet_unref(&avPacket);
            }
        }
        delete (videoWrapper->father->list1);
        delete (videoWrapper->father->list2);
        videoWrapper->father->list1 = nullptr;
        videoWrapper->father->list2 = nullptr;
        if (videoWrapper->father->avbsfContext != nullptr) {
            av_bsf_free(&videoWrapper->father->avbsfContext);
            videoWrapper->father->avbsfContext = nullptr;
        }

        av_free(videoWrapper->father);
        videoWrapper->father = nullptr;
        av_free(videoWrapper);
        videoWrapper = nullptr;

        /*if (videoAVFormatContext != nullptr) {
            avformat_close_input(&videoAVFormatContext);
            videoAVFormatContext = nullptr;
        }*/

        /*if (inFile != nullptr) {
            fclose(inFile);
            inFile = nullptr;
        }
        if (outFile != nullptr) {
            fclose(outFile);
            outFile = nullptr;
        }*/
        LOGW("%s\n", "closeVideo() end");
    }

    void closeDownload() {
        LOGI("%s\n", "closeDownload() start");
        if (avFormatContextOutput != nullptr) {
            // 写尾部信息
            av_write_trailer(avFormatContextOutput);
            avio_close(avFormatContextOutput->pb);

            //avformat_close_input(&avFormatContextOutput);
            avformat_free_context(avFormatContextOutput);
            avFormatContextOutput = nullptr;
        }
        if (avFormatContextVideoOutput != nullptr) {
            av_write_trailer(avFormatContextVideoOutput);
            avio_close(avFormatContextVideoOutput->pb);

            //avformat_close_input(&avFormatContextVideoOutput);
            avformat_free_context(avFormatContextVideoOutput);
            avFormatContextVideoOutput = nullptr;
        }
        if (avFormatContextAudioOutput != nullptr) {
            av_write_trailer(avFormatContextAudioOutput);
            avio_close(avFormatContextAudioOutput->pb);

            //avformat_close_input(&avFormatContextAudioOutput);
            avformat_free_context(avFormatContextAudioOutput);
            avFormatContextAudioOutput = nullptr;
        }
        if (audio_out_stream != nullptr) {
        }
        if (video_out_stream != nullptr) {
        }
        if (dts_start_from != nullptr) {
            free(dts_start_from);
            dts_start_from = nullptr;
        }
        if (pts_start_from != nullptr) {
            free(pts_start_from);
            pts_start_from = nullptr;
        }
        if (videoFile) {
            fclose(videoFile);
            videoFile = nullptr;
        }
        if (audioFile) {
            fclose(audioFile);
            audioFile = nullptr;
        }
        LOGI("%s\n", "closeDownload() end");
    }

    void closeOther() {
        if (avFormatContext != nullptr) {
            LOGI("%s\n", "closeOther() start");
            //ffmpeg 4.0版本不能调用
            //avformat_close_input(&avFormatContext);
            avformat_free_context(avFormatContext);
            avFormatContext = nullptr;
            LOGI("%s\n", "closeOther() end");
        }
        closeDownload();
        pthread_mutex_destroy(&readLockMutex);
        pthread_cond_destroy(&readLockCondition);
    }

    int initPlayer() {
        LOGI("%s\n", "initPlayer() start");

        onReady();

        initAV();
        initVideo();
        initAudio();
        if (openAndFindAVFormatContext() < 0) {
            closeVideo();
            closeAudio();
            closeOther();
            if (isInterrupted) {
                onFinished();
            } else {
                onError(0x100, "openAndFindAVFormatContext() < 0 failed");
            }
            return -1;
        }
        if (findStreamIndex() < 0) {
            closeVideo();
            closeAudio();
            closeOther();
            onError(0x100, "findStreamIndex() < 0 failed");
            return -1;
        }

        mediaDuration = (long long) (avFormatContext->duration / AV_TIME_BASE);
        LOGI("initPlayer()   mediaDuration: %lld\n", mediaDuration);
        if (avFormatContext->duration != AV_NOPTS_VALUE) {
            // 得到的是秒数
            mediaDuration = (long long) ((avFormatContext->duration + 5000) / AV_TIME_BASE);
            long long hours, mins, seconds;
            seconds = mediaDuration;
            mins = seconds / 60;
            seconds %= 60;
            hours = mins / 60;
            mins %= 60;
            // 00:54:16
            // 单位: 秒
            LOGI("initPlayer()   media seconds: %lld %02lld:%02lld:%02lld\n",
                 mediaDuration, hours, mins, seconds);
        }
        audioWrapper->father->duration =
        videoWrapper->father->duration = mediaDuration;
        if (mediaDuration <= 0) {
            isLive = true;
        } else {
            isLive = false;
        }

        if (isLocal) {
            FILE *fp = nullptr, *fq = nullptr;
            fp = fopen(inFilePath, "rb");
            fq = fp;
            // 存储文件指针位置
            fpos_t post_head;
            fpos_t post_end;
            // 定位在文件开头
            fseek(fp, 0, SEEK_SET);
            // 获取指针地址
            fgetpos(fq, &post_head);
            // 定位在文件尾部
            fseek(fq, 0, SEEK_END);
            // 获取指针地址
            fgetpos(fq, &post_end);
            // 计算文件大小
            fileLength = post_end - post_head;
            LOGI("initPlayer()      fileLength: %.0lf\n", fileLength);
            fclose(fp);
            fp = nullptr;
            fq = nullptr;
        }

        int videoRet = 0;
        int audioRet = 0;
        if (videoWrapper->father->streamIndex != -1) {
            videoRet = findAndOpenAVCodecForVideo();
            if (videoRet < 0) {
                videoWrapper->father->useMediaCodec = false;
                videoWrapper->father->streamIndex = -1;
            } else {
                videoRet = createSwsContext();
                if (videoRet < 0) {
                    videoWrapper->father->useMediaCodec = false;
                    videoWrapper->father->streamIndex = -1;
                }
            }
        }

        if (audioWrapper->father->streamIndex != -1) {
            audioRet = findAndOpenAVCodecForAudio();
            if (audioRet < 0) {
                audioWrapper->father->useMediaCodec = false;
                audioWrapper->father->streamIndex = -1;
            } else {
                audioRet = createSwrContent();
                if (audioRet < 0) {
                    audioWrapper->father->useMediaCodec = false;
                    audioWrapper->father->streamIndex = -1;
                }
            }
        }

        if (audioWrapper->father->streamIndex != -1
            && isWatchForCloseVideo) {
            videoWrapper->father->useMediaCodec = false;
            videoWrapper->father->streamIndex = -1;
        }

        if (videoWrapper->father->streamIndex != -1
            && isWatchForCloseAudio) {
            audioWrapper->father->useMediaCodec = false;
            audioWrapper->father->streamIndex = -1;
        }

        if (videoWrapper->father->streamIndex == -1) {
            videoDisable = true;
        }
        if (audioWrapper->father->streamIndex == -1) {
            audioDisable = true;
        }

        if (videoDisable && audioDisable) {
            closeVideo();
            closeAudio();
            closeOther();
            onError(0x100, "audio streamIndex and video streamIndex are -1");
            return -1;
        }

        if (!audioWrapper->father->isReading
            || !audioWrapper->father->isHandling
            || !videoWrapper->father->isReading
            || !videoWrapper->father->isHandling) {
            closeAudio();
            closeVideo();
            closeOther();
            onFinished();
            LOGI("%s\n", "initPlayer() finish");
            return -1;
        }

        /*if (frameRate <= 23) {
            TIME_DIFFERENCE = 0.000600;
        } else {
            TIME_DIFFERENCE = 0.500000;
        }*/
        TIME_DIFFERENCE = 0.500000;
        /*if (!strcmp(inFilePath, "http://39.134.157.205/PLTV/88888888/224/3221225562/index.m3u8")
            || !strcmp(inFilePath, "http://39.134.157.205/PLTV/88888888/224/3221225664/index.m3u8")
            || !strcmp(inFilePath,
                       "http://221.179.217.9/otttv.bj.chinamobile.com/PLTV/88888888/224/3221226551/1.m3u8")
            || !strcmp(inFilePath,
                       "http://221.179.217.9/otttv.bj.chinamobile.com/PLTV/88888888/224/3221226552/1.m3u8")
            || !strcmp(inFilePath,
                       "http://221.179.217.9/otttv.bj.chinamobile.com/PLTV/88888888/224/3221226553/1.m3u8")){
            TIME_DIFFERENCE = 0.000600;
        }*/

        if (!audioDisable && videoDisable) {
            if (!isWatch) {
                if (isWatchForCloseVideo) {
                    // 显示播放界面,这样才有机会把isWatchForCloseVideo设置为false
                    onChangeWindow(854, 480);
                } else {
                    onChangeWindow(0, 0);
                }
            } else if (isWatch
                       && videoWrapper->srcWidth != 0
                       && videoWrapper->srcHeight != 0) {
                onChangeWindow(videoWrapper->srcWidth, videoWrapper->srcHeight);
            }
        } else {
            onChangeWindow(videoWrapper->srcWidth, videoWrapper->srcHeight);
            if (audioDisable) {
                if (frameRate != 0) {
                    videoSleepTime = (int) (1000 / frameRate);
                    videoSleepTime *= 1000;
                } else {
                    videoSleepTime = 15000;
                }
                LOGI("initPlayer() videoSleepTime: %d", videoSleepTime);
                if (isWatch) {
                    videoSleepTime = 11000;
                }
                LOGI("initPlayer() videoSleepTime: %d", videoSleepTime);
            }
        }

        /*switch (use_mode) {
            case USE_MODE_MEDIA:
            case USE_MODE_MEDIA_MEDIACODEC: {
                audioWrapper->father->duration =
                videoWrapper->father->duration = mediaDuration;
                onChangeWindow(videoWrapper->srcWidth, videoWrapper->srcHeight);
                break;
            }
            case USE_MODE_ONLY_VIDEO: {
                videoWrapper->father->duration = mediaDuration;
                closeAudio();
                onChangeWindow(videoWrapper->srcWidth, videoWrapper->srcHeight);
                break;
            }
            case USE_MODE_ONLY_AUDIO: {
                audioWrapper->father->duration = mediaDuration;
                closeVideo();
                onChangeWindow(0, 0);
                break;
            }
            default:
                break;
        }*/

        LOGI("%s\n", "initPlayer() end");
        return 0;
    }

    /***
     * 判断str1是否以str2开头
     * 如果是返回1
     * 不是返回0
     * 出错返回-1
     */
    int is_begin_with2(const char *str1, char *str2) {
        if (str1 == nullptr || str2 == nullptr) {
            return -1;
        }
        int len1 = strlen(str1);
        int len2 = strlen(str2);
        if (len1 < len2 || len1 == 0 || len2 == 0) {
            return -1;
        }
        char *p = str2;
        int i = 0;
        while (*p != '\0') {
            if (*p != str1[i]) {
                return 0;
            }
            p++;
            i++;
        }
        return 1;
    }

    /***
     * 判断str1是否以str2结尾
     * 如果是返回1
     * 不是返回0
     * 出错返回-1
     *
     */
    int is_end_with2(const char *str1, char *str2) {
        if (str1 == nullptr || str2 == nullptr) {
            return -1;
        }
        int len1 = strlen(str1);
        int len2 = strlen(str2);
        if (len1 < len2 || len1 == 0 || len2 == 0) {
            return -1;
        }
        while (len2 >= 1) {
            if (str2[len2 - 1] != str1[len1 - 1]) {
                return 0;
            }
            len2--;
            len1--;
        }
        return 1;
    }

    void setJniParameters(JNIEnv *env, const char *filePath, jobject surfaceJavaObject) {
//        const char *src = "/storage/emulated/0/Movies/权力的游戏第三季05.mp4";
//        const char *src = "http://192.168.0.112:8080/tomcat_video/game_of_thrones/game_of_thrones_season_1/01.mp4";
//        av_strlcpy(inVideoFilePath, src, sizeof(inVideoFilePath));

        memset(inFilePath, '\0', sizeof(inFilePath));
        av_strlcpy(inFilePath, filePath, sizeof(inFilePath));
        LOGI("setJniParameters() filePath  : %s", inFilePath);

        isLocal = startsWith(inFilePath, "/storage/");
        isH264 = endsWith(inFilePath, ".h264");
        LOGI("setJniParameters() isLocal   : %d", isLocal);
        LOGI("setJniParameters() isH264    : %d", isH264);
        LOGI("setJniParameters() isWatch   : %d", isWatch);

        if (pANativeWindow != nullptr) {
            ANativeWindow_release(pANativeWindow);
            pANativeWindow = nullptr;
        }
        // 1.获取一个关联Surface的NativeWindow窗体
        pANativeWindow = ANativeWindow_fromSurface(env, surfaceJavaObject);
    }

    int play() {
        if (onlyDownloadNotPlayback) {
            return 0;
        }

        if (audioWrapper != nullptr
            && audioWrapper->father != nullptr
            && !audioDisable) {
            // 缓存时不能设置状态
            audioWrapper->father->isPausedForUser = false;
            if (!audioWrapper->father->isPausedForCache) {
                notifyToHandle(audioWrapper->father);
            }
        }

        if (videoWrapper != nullptr
            && videoWrapper->father != nullptr
            && !videoDisable) {
            videoWrapper->father->isPausedForUser = false;
            if (!videoWrapper->father->isPausedForCache) {
                notifyToHandle(videoWrapper->father);
            }
        }
        return 0;
    }

    int pause() {
        LOGI("pause() start\n");
        if (audioWrapper != nullptr
            && audioWrapper->father != nullptr
            && !audioDisable) {
            audioWrapper->father->isPausedForUser = true;
        }

        if (videoWrapper != nullptr
            && videoWrapper->father != nullptr
            && !videoDisable) {
            videoWrapper->father->isPausedForUser = true;
        }
        LOGI("pause() end\n");
        return 0;
    }

    int stop() {
        LOGI("stop() start\n");
        isInterrupted = true;
        if (audioWrapper != nullptr
            && audioWrapper->father != nullptr) {
            LOGI("stop() audio\n");
            // audio
            // audioWrapper->father->isStarted = false;
            audioWrapper->father->isReading = false;
            audioWrapper->father->isHandling = false;
            audioWrapper->father->isPausedForUser = false;
            audioWrapper->father->isPausedForCache = false;
            audioWrapper->father->isPausedForSeek = false;
            audioWrapper->father->isHandleList1Full = false;
            notifyToRead();
            //notifyToRead(audioWrapper->father);
            notifyToHandle(audioWrapper->father);
        }

        if (videoWrapper != nullptr
            && videoWrapper->father != nullptr) {
            LOGI("stop() video\n");
            // video
            isFrameByFrameMode = false;
            // videoWrapper->father->isStarted = false;
            videoWrapper->father->isReading = false;
            videoWrapper->father->isHandling = false;
            videoWrapper->father->isPausedForUser = false;
            videoWrapper->father->isPausedForCache = false;
            videoWrapper->father->isPausedForSeek = false;
            videoWrapper->father->isHandleList1Full = false;
            notifyToRead();
            //notifyToRead(videoWrapper->father);
            notifyToHandle(videoWrapper->father);
        }
        LOGI("stop() end\n");
        return 0;
    }

    int release() {
        stop();
        return 0;
    }

    // 有没有在运行,即使暂停状态也是运行状态
    bool isRunning() {
        bool audioRunning = false;
        bool videoRunning = false;
        if (audioWrapper != nullptr
            && audioWrapper->father != nullptr) {
            if (!audioDisable) {
                audioRunning = audioWrapper->father->isStarted
                               && audioWrapper->father->isHandling;
            } else {
                audioRunning = true;
            }
        }

        if (videoWrapper != nullptr
            && videoWrapper->father != nullptr) {
            if (!videoDisable) {
                videoRunning = videoWrapper->father->isStarted
                               && videoWrapper->father->isHandling;
            } else {
                videoRunning = true;
            }
        }
        return audioRunning && videoRunning;
    }

    // 有没有在播放,暂停状态不算播放状态
    bool isPlaying() {
        bool audioPlaying = false;
        bool videoPlaying = false;
        if (audioWrapper != nullptr
            && audioWrapper->father != nullptr) {
            if (!audioDisable) {
                audioPlaying = audioWrapper->father->isStarted
                               && audioWrapper->father->isHandling
                               && !audioWrapper->father->isPausedForUser
                               && !audioWrapper->father->isPausedForCache
                               && !audioWrapper->father->isPausedForSeek;
            } else {
                audioPlaying = true;
            }
        }

        if (videoWrapper != nullptr
            && videoWrapper->father != nullptr) {
            if (!videoDisable) {
                videoPlaying = videoWrapper->father->isStarted
                               && videoWrapper->father->isHandling
                               && !videoWrapper->father->isPausedForUser
                               && !videoWrapper->father->isPausedForCache
                               && !videoWrapper->father->isPausedForSeek;
            } else {
                videoPlaying = true;
            }
        }
        return audioPlaying && videoPlaying;
    }

    bool isPausedForUser() {
        bool audioPlaying = false;
        bool videoPlaying = false;
        if (audioWrapper != nullptr
            && audioWrapper->father != nullptr) {
            if (!audioDisable) {
                audioPlaying = audioWrapper->father->isStarted
                               && audioWrapper->father->isHandling
                               && audioWrapper->father->isPausedForUser
                               && videoWrapper->father->isPausedForCache;
            } else {
                audioPlaying = true;
            }
        }

        if (videoWrapper != nullptr
            && videoWrapper->father != nullptr) {
            if (!videoDisable) {
                videoPlaying = videoWrapper->father->isStarted
                               && videoWrapper->father->isHandling
                               && videoWrapper->father->isPausedForUser
                               && videoWrapper->father->isPausedForCache;
            } else {
                videoPlaying = true;
            }
        }
        return audioPlaying && videoPlaying;
    }

    /***
     单位秒.比如seek到100秒,就传100
     */
    int seekTo(int64_t timestamp) {
        LOGI("==================================================================\n");
        LOGI("seekTo() timeStamp: %lld\n", (long long) timestamp);

        if (isFrameByFrameMode) {
            return 0;
        }

        if ((long long) timestamp > 0
            && (audioWrapper == nullptr
                || videoWrapper == nullptr)) {
            timeStamp = timestamp;
            return 0;
        }

        if ((long long) timestamp < 0
            || audioWrapper == nullptr
            || audioWrapper->father == nullptr
            || audioWrapper->father->isPausedForSeek
            || videoWrapper == nullptr
            || videoWrapper->father == nullptr
            || videoWrapper->father->isPausedForSeek
            || isLive
            || ((long long) timestamp) > mediaDuration) {
            return -1;
        }

        LOGD("seekTo() notify to Read and Handle\n");
        timeStamp = timestamp;
        /*if (!isLocal && (long long) timestamp == 0) {
            timeStamp = 5;
        }*/
        if (!audioDisable) {
            audioWrapper->father->isPausedForSeek = true;
            audioWrapper->father->needToSeek = false;
            notifyToHandle(audioWrapper->father);
            //notifyToRead(audioWrapper->father);
        }
        if (!videoDisable) {
            videoWrapper->father->isPausedForSeek = true;
            videoWrapper->father->needToSeek = false;
            notifyToHandle(videoWrapper->father);
            //notifyToRead(videoWrapper->father);
        }
        notifyToRead();

        return 0;
    }

    // 返回值单位是秒
    long getDuration() {
        return mediaDuration;
    }

    void stepAdd(int64_t addStep) {
        /*++videoSleepTime;
        char dest[50];
        sprintf(dest, "videoSleepTime: %d\n", videoSleepTime);
        onInfo(dest);
        LOGF("stepAdd()      videoSleepTime: %d\n", videoSleepTime);*/

        if (!isLive) {
            LOGI("stepAdd() addStep: %ld\n", (long) addStep);
            seekTo((int64_t) curProgress + addStep);
        }
    }

    void stepSubtract(int64_t subtractStep) {
        /*--videoSleepTime;
        char dest[50];
        sprintf(dest, "videoSleepTime: %d\n", videoSleepTime);
        onInfo(dest);
        LOGF("stepSubtract() videoSleepTime: %d\n", videoSleepTime);*/

        if (!isLive) {
            LOGI("stepAdd() subtractStep: %ld\n", (long) subtractStep);
            seekTo((int64_t) curProgress - subtractStep);
        }
    }

    /***
     底层:
     不管音频,只要在java层让它静音就行了.结束后恢复声音.

     */
    bool frameByFrameForReady() {
        isFrameByFrameMode = false;
        if (videoWrapper != nullptr
            && videoWrapper->father != nullptr) {
            if (videoDisable) {
                return isFrameByFrameMode;
            }

            if (videoWrapper->father->isStarted
                && !videoWrapper->father->isPausedForUser
                && !videoWrapper->father->isPausedForCache
                && !videoWrapper->father->isPausedForSeek) {
                if (audioWrapper != nullptr
                    && audioWrapper->father != nullptr) {
                    if (!audioDisable) {
                        if (audioWrapper->father->isStarted
                            && !audioWrapper->father->isPausedForUser
                            && !audioWrapper->father->isPausedForCache
                            && !audioWrapper->father->isPausedForSeek) {
                            isFrameByFrameMode = true;
                        }
                    } else {
                        isFrameByFrameMode = true;
                    }
                } else {
                    isFrameByFrameMode = true;
                }
            }
        }

        return isFrameByFrameMode;
    }

    bool frameByFrameForFinish() {
        isFrameByFrameMode = false;

        return true;
    }

    // 一帧一帧的看(可行,但还需要修改代码)
    // 音频也一帧一帧的走,但是需要静音
    bool frameByFrame() {
        if (isFrameByFrameMode
            && videoWrapper != nullptr
            && videoWrapper->father != nullptr
            && !videoDisable) {
            notifyToHandle(videoWrapper->father);
        }

        return true;
    }

    int download(int flag, const char *filePath, const char *fileName) {
        switch (flag) {
            case 0: // 下载音视频
            case 4: // 只下载,不播放
            case 5: // 只提取音视频,不播放
            {
                /*if (!audioWrapper->father->isReading
                    || !videoWrapper->father->isReading) {
                    return 0;
                }*/

                std::string mediaPath;
                mediaPath.append(filePath);
                mediaPath.append(fileName);
                mediaPath.append(".mp4");// .h264 error
                memset(outFilePath, '\0', sizeof(outFilePath));
                av_strlcpy(outFilePath, mediaPath.c_str(), sizeof(outFilePath));
                LOGI("download() outFilePath: %s\n", outFilePath);

                /*std::string videoPath;
                std::string audioPath;
                videoPath.append(filePath);
                videoPath.append(fileName);
                videoPath.append(".h264");// ppm
                audioPath.append(filePath);
                audioPath.append(fileName);
                audioPath.append(".aac");
                memset(videoOutFilePath, '\0', sizeof(videoOutFilePath));
                av_strlcpy(videoOutFilePath, videoPath.c_str(), sizeof(videoOutFilePath));
                memset(audioOutFilePath, '\0', sizeof(audioOutFilePath));
                av_strlcpy(audioOutFilePath, audioPath.c_str(), sizeof(audioOutFilePath));
                LOGI("download() videoOutFilePath: %s\n", videoOutFilePath);
                LOGI("download() audioOutFilePath: %s\n", audioOutFilePath);*/

                onlyDownloadNotPlayback = false;
                if (initDownload() < 0) {
                    needToDownload = false;
                    isInitSuccess = false;
                } else {
                    if (flag == 4 || flag == 5) {
                        onlyDownloadNotPlayback = true;
                        if (flag == 5) {
                            seekTo(0);
                        }
                        pause();
                    }
                    needToDownload = true;
                    isInitSuccess = true;
                }
                LOGI("download() isInitSuccess: %d\n", isInitSuccess);

                break;
            }
            case 1: // 停止下载
            {
                pthread_mutex_lock(&readLockMutex);
                if (needToDownload && isInitSuccess) {
                    LOGI("download() 停止下载\n");
                    needToDownload = false;
                    isInitSuccess = false;
                    //av_usleep(1000 * 1000 * 3);// 3秒
                    closeDownload();
                }
                pthread_mutex_unlock(&readLockMutex);
                break;
            }
            case 2: // 只下载音频
            {
                break;
            }
            case 3: // 只下载视频
            {
                break;
            }
            default:
                break;
        }

        return 0;
    }

    /***
     char src[40];
     char dest[100];
     memset(dest, '\0', sizeof(dest));
     strcpy(src, "This is runoob.com");
     strcpy(dest, src);

     原型：int strlen ( const char *str )
     功能：返回字符串的实际长度,不含 '\0'.
     strlen之所以不包含'\0',是因为它在计数的途中遇到'\0'结束.
     char buf[100] = "hello";
     printf("%d\n", strlen(buf));// 5
     printf("%d\n", sizeof(buf));// 100
     */

    char *getStrAVPixelFormat(AVPixelFormat format) {
        char info[25] = {0};
        switch (format) {
            case AV_PIX_FMT_NONE:// -1
                strncpy(info, "AV_PIX_FMT_NONE", strlen("AV_PIX_FMT_NONE"));
                break;
            case AV_PIX_FMT_YUV420P:// 0
                strncpy(info, "AV_PIX_FMT_YUV420P", strlen("AV_PIX_FMT_YUV420P"));
                break;
            case AV_PIX_FMT_YUYV422:
                strncpy(info, "AV_PIX_FMT_YUYV422", strlen("AV_PIX_FMT_YUYV422"));
                break;
            case AV_PIX_FMT_RGB24:
                strncpy(info, "AV_PIX_FMT_RGB24", strlen("AV_PIX_FMT_RGB24"));
                break;
            case AV_PIX_FMT_BGR24:
                strncpy(info, "AV_PIX_FMT_BGR24", strlen("AV_PIX_FMT_BGR24"));
                break;
            case AV_PIX_FMT_YUV422P:
                strncpy(info, "AV_PIX_FMT_YUV422P", strlen("AV_PIX_FMT_YUV422P"));
                break;
            case AV_PIX_FMT_YUV444P:
                strncpy(info, "AV_PIX_FMT_YUV444P", strlen("AV_PIX_FMT_YUV444P"));
                break;
            case AV_PIX_FMT_YUV410P:
                strncpy(info, "AV_PIX_FMT_YUV410P", strlen("AV_PIX_FMT_YUV410P"));
                break;
            case AV_PIX_FMT_YUV411P:
                strncpy(info, "AV_PIX_FMT_YUV411P", strlen("AV_PIX_FMT_YUV411P"));
                break;
            case AV_PIX_FMT_GRAY8:
                strncpy(info, "AV_PIX_FMT_GRAY8", strlen("AV_PIX_FMT_GRAY8"));
                break;
            case AV_PIX_FMT_MONOWHITE:
                strncpy(info, "AV_PIX_FMT_MONOWHITE", strlen("AV_PIX_FMT_MONOWHITE"));
                break;
            case AV_PIX_FMT_MONOBLACK:
                strncpy(info, "AV_PIX_FMT_MONOBLACK", strlen("AV_PIX_FMT_MONOBLACK"));
                break;
            case AV_PIX_FMT_PAL8:
                strncpy(info, "AV_PIX_FMT_PAL8", strlen("AV_PIX_FMT_PAL8"));
                break;
            case AV_PIX_FMT_YUVJ420P:
                strncpy(info, "AV_PIX_FMT_YUVJ420P", strlen("AV_PIX_FMT_YUVJ420P"));
                break;
            case AV_PIX_FMT_YUVJ422P:
                strncpy(info, "AV_PIX_FMT_YUVJ422P", strlen("AV_PIX_FMT_YUVJ422P"));
                break;
            case AV_PIX_FMT_YUVJ444P:
                strncpy(info, "AV_PIX_FMT_YUVJ444P", strlen("AV_PIX_FMT_YUVJ444P"));
                break;
            case AV_PIX_FMT_UYVY422:
                strncpy(info, "AV_PIX_FMT_UYVY422", strlen("AV_PIX_FMT_UYVY422"));
                break;
            case AV_PIX_FMT_UYYVYY411:
                strncpy(info, "AV_PIX_FMT_UYYVYY411", strlen("AV_PIX_FMT_UYYVYY411"));
                break;
            case AV_PIX_FMT_BGR8:
                strncpy(info, "AV_PIX_FMT_BGR8", strlen("AV_PIX_FMT_BGR8"));
                break;
            case AV_PIX_FMT_BGR4:
                strncpy(info, "AV_PIX_FMT_BGR4", strlen("AV_PIX_FMT_BGR4"));
                break;
            case AV_PIX_FMT_BGR4_BYTE:
                strncpy(info, "AV_PIX_FMT_BGR4_BYTE", strlen("AV_PIX_FMT_BGR4_BYTE"));
                break;
            case AV_PIX_FMT_RGB8:
                strncpy(info, "AV_PIX_FMT_RGB8", strlen("AV_PIX_FMT_RGB8"));
                break;
            case AV_PIX_FMT_RGB4:
                strncpy(info, "AV_PIX_FMT_RGB4", strlen("AV_PIX_FMT_RGB4"));
                break;
            case AV_PIX_FMT_RGB4_BYTE:
                strncpy(info, "AV_PIX_FMT_RGB4_BYTE", strlen("AV_PIX_FMT_RGB4_BYTE"));
                break;
            case AV_PIX_FMT_NV12:
                strncpy(info, "AV_PIX_FMT_NV12", strlen("AV_PIX_FMT_NV12"));
                break;
            case AV_PIX_FMT_NV21:
                strncpy(info, "AV_PIX_FMT_NV21", strlen("AV_PIX_FMT_NV21"));
                break;
            case AV_PIX_FMT_ARGB:
                strncpy(info, "AV_PIX_FMT_ARGB", strlen("AV_PIX_FMT_ARGB"));
                break;
            case AV_PIX_FMT_RGBA:
                strncpy(info, "AV_PIX_FMT_RGBA", strlen("AV_PIX_FMT_RGBA"));
                break;
            case AV_PIX_FMT_ABGR:
                strncpy(info, "AV_PIX_FMT_ABGR", strlen("AV_PIX_FMT_ABGR"));
                break;
            case AV_PIX_FMT_BGRA:
                strncpy(info, "AV_PIX_FMT_BGRA", strlen("AV_PIX_FMT_BGRA"));
                break;
            case AV_PIX_FMT_GRAY16BE:
                strncpy(info, "AV_PIX_FMT_GRAY16BE", strlen("AV_PIX_FMT_GRAY16BE"));
                break;
            case AV_PIX_FMT_GRAY16LE:
                strncpy(info, "AV_PIX_FMT_GRAY16LE", strlen("AV_PIX_FMT_GRAY16LE"));
                break;
            case AV_PIX_FMT_YUV440P:
                strncpy(info, "AV_PIX_FMT_YUV440P", strlen("AV_PIX_FMT_YUV440P"));
                break;
            case AV_PIX_FMT_YUVJ440P:
                strncpy(info, "AV_PIX_FMT_YUVJ440P", strlen("AV_PIX_FMT_YUVJ440P"));
                break;
            case AV_PIX_FMT_YUVA420P:
                strncpy(info, "AV_PIX_FMT_YUVA420P", strlen("AV_PIX_FMT_YUVA420P"));
                break;
            case AV_PIX_FMT_RGB48BE:
                strncpy(info, "AV_PIX_FMT_RGB48BE", strlen("AV_PIX_FMT_RGB48BE"));
                break;
            case AV_PIX_FMT_RGB48LE:
                strncpy(info, "AV_PIX_FMT_RGB48LE", strlen("AV_PIX_FMT_RGB48LE"));
                break;
            case AV_PIX_FMT_RGB565BE:
                strncpy(info, "AV_PIX_FMT_RGB565BE", strlen("AV_PIX_FMT_RGB565BE"));
                break;
            case AV_PIX_FMT_RGB565LE:
                strncpy(info, "AV_PIX_FMT_RGB565LE", strlen("AV_PIX_FMT_RGB565LE"));
                break;
            case AV_PIX_FMT_RGB555BE:
                strncpy(info, "AV_PIX_FMT_RGB555BE", strlen("AV_PIX_FMT_RGB555BE"));
                break;
            case AV_PIX_FMT_RGB555LE:
                strncpy(info, "AV_PIX_FMT_RGB555LE", strlen("AV_PIX_FMT_RGB555LE"));
                break;
            case AV_PIX_FMT_BGR565BE:
                strncpy(info, "AV_PIX_FMT_BGR565BE", strlen("AV_PIX_FMT_BGR565BE"));
                break;
            case AV_PIX_FMT_BGR565LE:
                strncpy(info, "AV_PIX_FMT_BGR565LE", strlen("AV_PIX_FMT_BGR565LE"));
                break;
            case AV_PIX_FMT_BGR555BE:
                strncpy(info, "AV_PIX_FMT_BGR555BE", strlen("AV_PIX_FMT_BGR555BE"));
                break;
            case AV_PIX_FMT_BGR555LE:
                strncpy(info, "AV_PIX_FMT_BGR555LE", strlen("AV_PIX_FMT_BGR555LE"));
                break;
            default:
                strncpy(info, "AV_PIX_FMT_NONE", strlen("AV_PIX_FMT_NONE"));
                break;
        }

        return info;
    }

}

//
// Created by alexander on 2020/7/5.
//

#ifndef USEFRAGMENTS_WRAPPER_H
#define USEFRAGMENTS_WRAPPER_H

#ifdef __cplusplus
extern "C" {
#endif

//extern "C" {// 不能少
// ffmpeg使用MediaCodec进行硬解码(需要编译出支持硬解码的so库)
#include <libavcodec/jni.h>
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavformat/avio.h>
#include "libswresample/swresample.h"
// libswscale是一个主要用于处理图片像素数据的类库.可以完成图片像素格式的转换,图片的拉伸等工作.
#include <libswscale/swscale.h>
#include <libavfilter/avfilter.h>
#include <libavfilter/buffersink.h>
#include <libavfilter/buffersrc.h>
#include <libavutil/channel_layout.h>
#include <libavutil/mathematics.h>
#include <libavutil/samplefmt.h>
// 这里是做分片时候重采样编码音频用的
#include <libavutil/audio_fifo.h>
#include <libavutil/imgutils.h>
#include <libavutil/avutil.h>
#include <libavutil/avassert.h>
#include <libavutil/avstring.h>
#include <libavutil/frame.h>
#include <libavutil/hwcontext.h>
#include <libavutil/parseutils.h>
#include <libavutil/pixdesc.h>
#include <libavutil/pixfmt.h>
#include <libavutil/fifo.h>
#include <libavutil/log.h>
#include <libavutil/opt.h>
#include <libavutil/mem.h>
#include <libavutil/error.h>
#include <libavutil/time.h>

//#include <libavdevice/avdevice.h>

// 使用libyuv,将YUV转换RGB
//#include <libyuv/basic_types.h>
//#include <libyuv/compare.h>
//#include <libyuv/convert.h>
//#include <libyuv/convert_argb.h>
//#include <libyuv/convert_from.h>
//#include <libyuv/convert_from_argb.h>
//#include <libyuv/cpu_id.h>
//#include <libyuv/mjpeg_decoder.h>
//#include <libyuv/planar_functions.h>
//#include <libyuv/rotate.h>
//#include <libyuv/rotate_argb.h>
//#include <libyuv/row.h>
//#include <libyuv/scale.h>
//#include <libyuv/scale_argb.h>
//#include <libyuv/scale_row.h>
//#include <libyuv/version.h>
//#include <libyuv/video_common.h>

//    #include <jconfig.h>
//    #include <jerror.h>
//    #include <jmorecfg.h>
//    #include <jpeglib.h>
//    #include <turbojpeg.h>

//};// extern "C" end

#ifdef __cplusplus
}
#endif

#include <pthread.h>
#include "ffmpeg.h"
#include "../include/Log.h"

// 1 second of 48khz 32bit audio
#define MAX_AUDIO_FRAME_SIZE 192000

#define TYPE_UNKNOW -1
#define TYPE_AUDIO 1
#define TYPE_VIDEO 2

// 不能无限制读取数据进行保存,这样要出错的(能播放,但不是想要的结果)
#define MAX_AVPACKET_COUNT         10000

#define MAX_AVPACKET_COUNT_AUDIO_HTTP 140 // 3000 1500 380 190
#define MAX_AVPACKET_COUNT_VIDEO_HTTP 50  // 2000 1000 200 100

#define MAX_AVPACKET_COUNT_AUDIO_LOCAL 140// 3000
#define MAX_AVPACKET_COUNT_VIDEO_LOCAL 50// 2000

#define MAX_RELATIVE_TIME     30000000 // 30秒
#define MAX_RELATIVE_TIME_OUT 600000000// 10分

#define RUN_COUNTS 88

struct AVBSFInternal {
    AVPacket *buffer_pkt;
    int eof;
};

// 子类都要用到的部分
struct Wrapper {
    int type = TYPE_UNKNOW;
    AVCodecContext *avCodecContext = nullptr;
    // 有些东西需要通过它去得到(自己千万千万千万不要去释放内存)
    AVCodecParameters *avCodecParameters = nullptr;
    AVStream *avStream = nullptr;
    // 解码器
    AVCodec *decoderAVCodec = nullptr;
    // 编码器(没用到,因为是播放,所以不需要编码)
    AVCodec *encoderAVCodec = nullptr;
    AVCodecID avCodecId;

    /***
     FFmpeg解码获得的AVPacket只包含视频压缩数据，并没有包含相关的解码信息
    （比如：h264的sps pps头信息，AAC的adts头信息），没有这些编码头信息解
    码器（MediaCodec）是不能解码的。在FFmpeg中，这些头信息是保存
    在解码器上下文（AVCodecContext）的extradata中的，所以我们需要为每一种
    格式的视频添加相应的解码头信息，这样解码器（MediaCodec）才能正确解析
    每一个AVPacket里的视频数据。
     */
    // 默认为0,即false
    bool useMediaCodec;
    bool need_to_do_for_av_bsf_packet;
    const AVBitStreamFilter *avBitStreamFilter = nullptr;
    AVBSFContext *avbsfContext = nullptr;

    unsigned char *outBuffer1 = nullptr;
    unsigned char *outBuffer2 = nullptr;
    unsigned char *outBuffer3 = nullptr;
    // 默认为0
    size_t outBufferSize;

    // 默认为0
    int streamIndex;
    // 总共读取了多少个AVPacket
    int readFramesCount;
    // 总共处理了多少个AVPacket
    int handleFramesCount;

    // 不能这样定义
    // std::list<AVPacket*> *list1 = nullptr;
    std::list<AVPacket> *list1 = nullptr;
    std::list<AVPacket> *list2 = nullptr;
    // 队列中最多保存多少个AVPacket
    int list1LimitCounts;
    int list2LimitCounts;
    bool isReadList1Full;
    bool isHandleList1Full;

    bool isStarted;
    bool isReading;
    bool isHandling;
    bool isSleeping;
    // 因为user所以pause
    bool isPausedForUser;
    // 因为cache所以pause
    bool isPausedForCache;
    // 因为seek所以pause
    bool isPausedForSeek;
    // seek的初始化条件有没有完成,true表示完成
    bool needToSeek;
    bool allowDecode;

    // 播放异常处理
    // int64_t startHandleTime = av_gettime_relative();
    // int64_t endHandleTime;
    // 1秒
    // endHandleTime - startHandleTime >= 1000000;

    // 单位: 秒
    int64_t duration;
    // 单位: 秒 seekTo到某个时间点
    int64_t timestamp;
    // 跟线程有关
    pthread_mutex_t readLockMutex;
    pthread_cond_t readLockCondition;
    pthread_mutex_t handleLockMutex;
    pthread_cond_t handleLockCondition;

    // 存储压缩数据(视频对应H.264等码流数据,音频对应AAC/MP3等码流数据)
    // AVPacket *avPacket = nullptr;
    // 视频使用到sws_scale函数时需要定义这些变量,音频也要用到
    // unsigned char *srcData[4] = {nullptr}, dstData[4] = {nullptr};
};

struct AudioWrapper {
    struct Wrapper *father = nullptr;
    SwrContext *swrContext = nullptr;
    // 存储非压缩数据(视频对应RGB/YUV像素数据,音频对应PCM采样数据)
    AVFrame *decodedAVFrame = nullptr;
    // 从音频源或视频源中得到
    // 采样率
    int srcSampleRate;
    int dstSampleRate;// 取值于srcSampleRate
    // 声道数
    int srcNbChannels;
    int dstNbChannels;// 由dstChannelLayout去获到
    int srcNbSamples;// 用不到
    int dstNbSamples;// 用不到
    // 由srcNbChannels能得到srcChannelLayout,也能由srcChannelLayout得到srcNbChannels
    int srcChannelLayout;
    // 双声道输出
    int dstChannelLayout = AV_CH_LAYOUT_STEREO;
    // 从音频源或视频源中得到(采样格式)
    enum AVSampleFormat srcAVSampleFormat = AV_SAMPLE_FMT_NONE;
    // 输出的采样格式16bit PCM
    enum AVSampleFormat dstAVSampleFormat = AV_SAMPLE_FMT_S16;
};

struct VideoWrapper {
    struct Wrapper *father = nullptr;
    SwsContext *swsContext = nullptr;
    // 从视频源中得到
    enum AVPixelFormat srcAVPixelFormat = AV_PIX_FMT_NONE;
    // 从原来的像素格式转换为想要的视频格式(可能应用于不需要播放视频的场景)
    // 播放时dstAVPixelFormat必须跟srcAVPixelFormat的值一样,不然画面有问题
    enum AVPixelFormat dstAVPixelFormat = AV_PIX_FMT_RGBA;
    // 一个视频没有解码之前读出的数据是压缩数据,把压缩数据解码后就是原始数据
    // 解码后的原始数据(像素格式可能不是我们想要的,如果是想要的,那么没必要再调用sws_scale函数了)
    AVFrame *decodedAVFrame = nullptr;
    // 解码后的原始数据(像素格式是我们想要的)
    AVFrame *rgbAVFrame = nullptr;
    // 从视频源中得到的宽高
    int srcWidth, srcHeight;
    size_t srcArea;
    // 想要播放的窗口大小,可以直接使用srcWidth和srcHeight
    int dstWidth = 720, dstHeight = 360;
    size_t dstArea;
    // 使用到sws_scale函数时需要定义这些变量
    int srcLineSize[4] = {0}, dstLineSize[4] = {0};

    AVCodecParserContext *avCodecParserContext = nullptr;
};

#endif //USEFRAGMENTS_WRAPPER_H

/***
 YUV采样方式：YUV4:4:4, YUV4:2:2, YUV4:2:0
 YUV存储方式：planar和packed
 对于所有的YUV420图像，它们的Y值排列是完全相同的，因为只有Y的图像就是灰度图像.
 在YUV420中，一个像素点对应一个Y，一个4X4的小方块对应一个U和V.
 YUV420sp与YUV420p的数据格式它们的UV排列在原理上是完全不同的.
 420p它是先把U存放完后，再存放V，也就是说UV它们是连续的.
 420sp它是UV,UV这样交替存放的.
 P与SP就是存储格式不同,但是所占空间是相等的.
 总结:
    不管是YUV444,YUV422还是YUV420,(width * hight)的图像就有(width * hight)个Y值.
    YUV444,如果有(width * hight)个Y值,那么就有(width * hight)个U值,还有(width * hight)个V值
    YUV422,如果有(width * hight)个Y值,那么就有(width * hight)/2个U值,还有(width * hight)/2个V值
    YUV420,如果有(width * hight)个Y值,那么就有(width * hight)/4个U值,还有(width * hight)/4个V值
    U值与V值的大小是相等的.

















 */
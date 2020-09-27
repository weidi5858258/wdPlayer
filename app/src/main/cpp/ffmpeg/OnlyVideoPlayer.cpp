//
// Created by root on 19-8-8.
//

#include "OnlyVideoPlayer.h"

#define LOG "player_alexander_only_video"

extern AVFormatContext *avFormatContext;
extern struct VideoWrapper *videoWrapper;
extern bool isLocal;
extern bool isH264;
extern bool isReading;
extern double fileLength;
// seek时间
extern int64_t timeStamp;
extern ANativeWindow *pANativeWindow;
extern bool isWatch;

namespace alexander_only_video {

    static char inFilePath[2048];
    static double videoPts = 0.0;
    static double videoPtsPre = 0.0;
    static long long curProgress = 0;
    static long long preProgress = 0;
    // 视频播放时每帧之间的暂停时间,单位为ms
    static int videoSleepTime = 11;
    static long long pktCounts = 0;
    static int64_t curTime = 0, preTime = 0;

    static int seek_flags;
    static float seek_interval = 0.0;

    // 绘制时的缓冲区
    static ANativeWindow_Buffer mANativeWindow_Buffer;

    clock_t startTime = 0, endTime = 0;

    int frameRate = 0;

    char *getStrAVCodec(AVCodecID codecID);

    char *getStrAVPixelFormat(AVPixelFormat format);

    /////////////////////////////////////////////////////

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

    // 已经不需要调用了
    void initAV() {
        av_register_all();
        // 用于从网络接收数据,如果不是网络接收数据,可不用（如本例可不用）
        avcodec_register_all();
        // 注册复用器和编解码器,所有的使用ffmpeg,首先必须调用这个函数
        avformat_network_init();
        // 注册设备的函数,如用获取摄像头数据或音频等,需要此函数先注册
        // avdevice_register_all();
        LOGW("initAV() version: %s\n", av_version_info());
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

        struct Wrapper *wrapper = (struct Wrapper *) av_mallocz(sizeof(struct Wrapper));
        memset(wrapper, 0, sizeof(struct Wrapper));

        wrapper->type = TYPE_VIDEO;
        if (isLocal) {
            wrapper->list1LimitCounts = MAX_AVPACKET_COUNT_VIDEO_LOCAL;
            wrapper->list2LimitCounts = MAX_AVPACKET_COUNT_VIDEO_LOCAL;
        } else {
            wrapper->list1LimitCounts = MAX_AVPACKET_COUNT_VIDEO_HTTP;
            wrapper->list2LimitCounts = MAX_AVPACKET_COUNT;
        }
        LOGW("initVideo() list1LimitCounts: %d\n", wrapper->list1LimitCounts);
        LOGW("initVideo() list2LimitCounts: %d\n", wrapper->list2LimitCounts);
        wrapper->duration = -1;
        wrapper->timestamp = -1;
        wrapper->streamIndex = -1;
        wrapper->isStarted = false;
        wrapper->isReading = true;
        wrapper->isHandling = true;
        wrapper->isPausedForUser = false;
        wrapper->isPausedForCache = false;
        wrapper->isPausedForSeek = false;
        wrapper->needToSeek = false;
        wrapper->isHandleList1Full = false;
        wrapper->list1 = new std::list<AVPacket>();
        wrapper->list2 = new std::list<AVPacket>();
        wrapper->readLockMutex = PTHREAD_MUTEX_INITIALIZER;
        wrapper->readLockCondition = PTHREAD_COND_INITIALIZER;
        wrapper->handleLockMutex = PTHREAD_MUTEX_INITIALIZER;
        wrapper->handleLockCondition = PTHREAD_COND_INITIALIZER;

        if (videoWrapper != NULL) {
            av_free(videoWrapper);
            videoWrapper = NULL;
        }
        videoWrapper = (struct VideoWrapper *) av_mallocz(sizeof(struct VideoWrapper));
        memset(videoWrapper, 0, sizeof(struct VideoWrapper));
        videoWrapper->father = wrapper;
    }

    int openAndFindAVFormatContext() {
        LOGI("openAndFindAVFormatContext() start\n");
        if (avFormatContext != NULL) {
            LOGI("openAndFindAVFormatContext() videoAVFormatContext isn't NULL\n");
            avformat_free_context(avFormatContext);
            avFormatContext = NULL;
        }
        avFormatContext = avformat_alloc_context();
        if (avFormatContext == NULL) {
            LOGE("videoAVFormatContext is NULL.\n");
            return -1;
        }
        if (avformat_open_input(&avFormatContext,
                                inFilePath,
                                NULL, NULL) != 0) {
            LOGE("Couldn't open input stream.\n");
            return -1;
        }
        if (avformat_find_stream_info(avFormatContext, NULL) != 0) {
            LOGE("Couldn't find stream information.\n");
            return -1;
        }
        LOGI("openAndFindAVFormatContext() end\n");

        return 0;
    }

    int findStreamIndex() {
        // stream counts
        int streams = avFormatContext->nb_streams;
        LOGI("Stream counts       : %d\n", streams);
        for (int i = 0; i < streams; i++) {
            // 得到AVCodecParameters
            AVCodecParameters *avCodecParameters = avFormatContext->streams[i]->codecpar;
            if (avCodecParameters != NULL) {
                AVMediaType mediaType = avCodecParameters->codec_type;
                switch (mediaType) {
                    case AVMEDIA_TYPE_AUDIO: {
                        break;
                    }
                    case AVMEDIA_TYPE_VIDEO: {
                        videoWrapper->father->streamIndex = i;
                        videoWrapper->father->avCodecParameters = avCodecParameters;
                        LOGW("videoStreamIndex    : %d\n", videoWrapper->father->streamIndex);
                        break;
                    }
                    default:
                        break;
                }
            }
        }
        if (videoWrapper->father->streamIndex == -1) {
            LOGE("Didn't find video stream.\n");
            return -1;
        }

        return 0;
    }

    int findAndOpenAVCodecForVideo() {
        if (videoWrapper->father->streamIndex != -1) {
            // AV_CODEC_ID_NONE(0) AV_CODEC_ID_MPEG4(12)
            AVCodecID codecID = videoWrapper->father->avCodecParameters->codec_id;
            LOGW("AVCodecID           : %d %s\n", codecID, getStrAVCodec(codecID));
            AVCodec *decoderAVCodec = NULL;
            /*switch (codecID) {
                case AV_CODEC_ID_HEVC: {// 173
                    // 硬解码265
                    decoderAVCodec = avcodec_find_decoder_by_name("hevc_mediacodec");
                    break;
                }
                case AV_CODEC_ID_H264: {// 27
                    // 硬解码264
                    decoderAVCodec = avcodec_find_decoder_by_name("h264_mediacodec");
                    break;
                }
                case AV_CODEC_ID_MPEG4: {// 12
                    // 硬解码mpeg4
                    decoderAVCodec = avcodec_find_decoder_by_name("mpeg4_mediacodec");
                    break;
                }
                default: {
                    // 软解
                    // videoWrapper->father->decoderAVCodec = avcodec_find_decoder(codecID);
                    break;
                }
            }*/

            // 有相应的so库时这句就不要执行了
            decoderAVCodec = avcodec_find_decoder(codecID);
            videoWrapper->father->decoderAVCodec = decoderAVCodec;

            if (decoderAVCodec != NULL) {
                videoWrapper->father->avCodecContext = avcodec_alloc_context3(decoderAVCodec);
                if (videoWrapper->father->avCodecContext != NULL) {
                    if (avcodec_parameters_to_context(
                            videoWrapper->father->avCodecContext,
                            videoWrapper->father->avCodecParameters) < 0) {
                        return -1;
                    } else {
                        if (avcodec_open2(
                                videoWrapper->father->avCodecContext,
                                decoderAVCodec, NULL) != 0) {
                            LOGE("Could not open video codec.\n");
                            return -1;
                        }
                    }
                }
            } else {
                LOGW("findAndOpenAVCodecForVideo() decoderAVCodec is NULL\n");
                //onError();
                return -1;
            }
        }

        return 0;
    }

    int createSwsContext() {
        // Android支持的目标像素格式
        // AV_PIX_FMT_RGB32
        // AV_PIX_FMT_RGBA
        videoWrapper->dstAVPixelFormat = AV_PIX_FMT_RGBA;

        videoWrapper->srcWidth = videoWrapper->father->avCodecContext->width;
        videoWrapper->srcHeight = videoWrapper->father->avCodecContext->height;
        videoWrapper->srcAVPixelFormat = videoWrapper->father->avCodecContext->pix_fmt;
        /*videoWrapper->dstWidth = videoWrapper->srcWidth;
        videoWrapper->dstHeight = videoWrapper->srcHeight;
        videoWrapper->srcArea = videoWrapper->srcWidth * videoWrapper->srcHeight;
        videoWrapper->dstArea = videoWrapper->srcArea;*/
        LOGW("---------------------------------\n");
        // 视频编码器名称
        const char *long_name = videoWrapper->father->decoderAVCodec->long_name;
        LOGW("encoderName         : %s\n", long_name);
        long_name = avFormatContext->iformat->long_name;
        LOGW("descriptiveName     : %s\n", long_name);
        LOGW("srcWidth            : %d\n", videoWrapper->srcWidth);
        LOGW("srcHeight           : %d\n", videoWrapper->srcHeight);
        LOGW("srcAVPixelFormat    : %d  %s\n",
             videoWrapper->srcAVPixelFormat, getStrAVPixelFormat(videoWrapper->srcAVPixelFormat));
        LOGW("dstAVPixelFormat    : %d %s\n",
             videoWrapper->dstAVPixelFormat, getStrAVPixelFormat(videoWrapper->dstAVPixelFormat));
        AVStream *stream = avFormatContext->streams[videoWrapper->father->streamIndex];
        // 帧数
        int64_t videoFrames = stream->nb_frames;
        LOGW("videoFrames         : %d\n", videoFrames);
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
        frameRate = stream->avg_frame_rate.num / stream->avg_frame_rate.den;
        LOGW("frameRate           : %d fps/Hz\n", frameRate);
        int bitRate = avFormatContext->bit_rate / 1000;
        LOGW("bitRate             : %d kbps\n", bitRate);

        // decodedAVFrame为解码后的数据 rgbAVFrame为转换后的数据
        // avPacket ---> decodedAVFrame ---> rgbAVFrame ---> 渲染画面
        videoWrapper->decodedAVFrame = av_frame_alloc();
        videoWrapper->rgbAVFrame = av_frame_alloc();

        int imageGetBufferSize = av_image_get_buffer_size(
                videoWrapper->dstAVPixelFormat,
                videoWrapper->srcWidth,
                videoWrapper->srcHeight,
                1);
        size_t outBufferSize = imageGetBufferSize * sizeof(unsigned char);
        LOGW("imageGetBufferSize1 : %d\n", imageGetBufferSize);
        LOGW("imageGetBufferSize2 : %d\n", outBufferSize);
        videoWrapper->father->outBuffer1 =
                (unsigned char *) av_malloc(outBufferSize);

        int imageFillArrays = av_image_fill_arrays(
                videoWrapper->rgbAVFrame->data,
                videoWrapper->rgbAVFrame->linesize,
                videoWrapper->father->outBuffer1,
                videoWrapper->dstAVPixelFormat,
                videoWrapper->srcWidth,
                videoWrapper->srcHeight,
                1);
        LOGW("imageFillArrays     : %d\n", imageFillArrays);
        if (imageFillArrays < 0) {
            return -1;
        }
        // 由于解码出来的帧格式不是RGBA,在渲染之前需要进行格式转换
        // 现在swsContext知道程序员想要得到什么样的像素格式了
        videoWrapper->swsContext = sws_getContext(
                videoWrapper->srcWidth, videoWrapper->srcHeight, videoWrapper->srcAVPixelFormat,
                videoWrapper->srcWidth, videoWrapper->srcHeight, videoWrapper->dstAVPixelFormat,
                // SWS_BICUBIC SWS_BILINEAR 原分辨率与目标分辨率不一致时使用哪种算法来调整.
                SWS_BICUBIC,
                NULL, NULL,
                // 指定调整图像缩放的算法,可设为NULL使用默认算法.
                NULL);
        if (videoWrapper->swsContext == NULL) {
            LOGE("%s\n", "videoSwsContext is NULL.");
            return -1;
        }

        // 时长
        if (avFormatContext->duration != AV_NOPTS_VALUE) {
            int64_t duration = avFormatContext->duration + 5000;
            // 得到的是秒数
            videoWrapper->father->duration = duration / AV_TIME_BASE;
            int hours, mins, seconds;
            // 得到的是秒数
            seconds = getDuration();
            mins = seconds / 60;
            seconds %= 60;
            hours = mins / 60;
            mins %= 60;
            // 00:54:16
            // 单位: 秒
            LOGW("video duration      : %d\n", (int) videoWrapper->father->duration);
            LOGW("video                 %02d:%02d:%02d\n", hours, mins, seconds);
        }
        LOGW("---------------------------------\n");

        return 0;
    }

    int seekToImpl() {
        while (!videoWrapper->father->needToSeek) {
            videoSleep(1);
        }
        LOGI("seekToImpl() av_seek_frame start\n");
        if (videoWrapper->father->list2->size() != 0) {
            std::list<AVPacket>::iterator iter;
            for (iter = videoWrapper->father->list2->begin();
                 iter != videoWrapper->father->list2->end();
                 iter++) {
                AVPacket avPacket = *iter;
                av_packet_unref(&avPacket);
            }
            videoWrapper->father->list2->clear();
        }

        if (isH264) {
            int64_t seek_rel = seek_interval * 180000.0;
            int64_t seek_target = timeStamp;
            int64_t seek_min = seek_rel > 0 ? seek_target - seek_rel + 2 : INT64_MIN;
            int64_t seek_max = seek_rel < 0 ? seek_target - seek_rel - 2 : INT64_MAX;
            LOGI("seekToImpl() seek_min: %lld\n", (long long) seek_min);
            LOGI("seekToImpl()      pos: %lld\n", (long long) seek_target);
            LOGI("seekToImpl() seek_max: %lld\n", (long long) seek_max);
            avformat_seek_file(avFormatContext, -1, seek_min, seek_target, seek_max, seek_flags);
        } else {
            av_seek_frame(
                    avFormatContext,
                    -1,
                    timeStamp * AV_TIME_BASE,
                    AVSEEK_FLAG_BACKWARD | AVSEEK_FLAG_FRAME);
        }

        // 清空解码器的缓存
        avcodec_flush_buffers(videoWrapper->father->avCodecContext);
        timeStamp = -1;
        preProgress = 0;
        preTime = 0;
        videoWrapper->father->isPausedForSeek = false;
        LOGI("seekToImpl() av_seek_frame end\n");
    }

    int readDataImpl(Wrapper *wrapper, AVPacket *srcAVPacket, AVPacket *copyAVPacket) {
        av_packet_ref(copyAVPacket, srcAVPacket);
        av_packet_unref(srcAVPacket);

        //LOGW("readDataImpl()     size: %d\n",copyAVPacket->size);// 10810(ok)
        //LOGW("readDataImpl() duration: %ld\n",(long)copyAVPacket->duration);// 48000
        // 27382318
        // 27371508 27360605 27349322 27337010 27320776 27307323
        //LOGW("readDataImpl()      pos: %ld\n",(long)copyAVPacket->pos);// 27371508(ok)
        //LOGW("readDataImpl()      dts: %ld\n",(long)copyAVPacket->dts);// 0
        //LOGW("readDataImpl()      pts: %ld\n",(long)copyAVPacket->pts);// 0
        pthread_mutex_lock(&wrapper->readLockMutex);
        pktCounts++;
        // 存数据
        wrapper->list2->push_back(*copyAVPacket);
        size_t list2Size = wrapper->list2->size();
        pthread_mutex_unlock(&wrapper->readLockMutex);

        // 如果发现是因为Cache问题而暂停了,那么发送一个通知
        /*if (wrapper->isPausedForCache
            && list2Size == CACHE_COUNT) {
            notifyToHandle(wrapper);
        }*/

        if (!wrapper->isHandleList1Full
            && list2Size == wrapper->list1LimitCounts) {
            // 把list2中的内容全部复制给list1
            wrapper->list1->clear();
            wrapper->list1->assign(wrapper->list2->begin(), wrapper->list2->end());
            wrapper->list2->clear();
            list2Size = wrapper->list2->size();

            wrapper->isHandleList1Full = true;
            notifyToHandle(wrapper);
            LOGW("readDataImpl() video 已填满数据可以播放了\n");
        }

        if (list2Size >= wrapper->list2LimitCounts) {
            LOGW("readDataImpl() video list1: %d\n", wrapper->list1->size());
            LOGW("readDataImpl() video list2: %d\n", wrapper->list2->size());
            LOGI("readDataImpl() notifyToReadWait start\n");
            notifyToReadWait(wrapper);
            LOGI("readDataImpl() notifyToReadWait end\n");
        }

        return 0;
    }

    void *readData(void *opaque) {
        LOGW("%s\n", "readData() start");
        preProgress = 0;
        isReading = true;
        pktCounts = 0;
        seek_flags &= ~AVSEEK_FLAG_BYTE;
        seek_flags |= AVSEEK_FLAG_BYTE;
        seek_interval = 10;

        AVPacket *srcAVPacket = av_packet_alloc();
        AVPacket *copyAVPacket = av_packet_alloc();

        // seekTo
        if (timeStamp > 0) {
            LOGI("readData() timeStamp: %lld\n", (long long) timeStamp);
            videoWrapper->father->needToSeek = true;
            videoWrapper->father->isPausedForSeek = true;
        }

        int readFrame = 0;
        /***
         有几种情况:
         1.list1中先存满n个,然后list2多次存取
         2.list1中先存满n个,然后list2一次性存满
         3.list1中还没满n个文件就读完了
         */
        for (;;) {
            // exit
            if (!videoWrapper->father->isReading) {
                // for (;;) end
                break;
            }

            // seekTo
            if (videoWrapper->father->isPausedForSeek
                && timeStamp >= 0) {
                seekToImpl();
            }

            // 0 if OK, < 0 on error or end of file
            readFrame = av_read_frame(avFormatContext, srcAVPacket);
            //LOGI("readFrame           : %d\n", readFrame);
            if (readFrame < 0) {
                // 有些视频一直返回-12
                // LOGF("readData() readFrame            : %d\n", readFrame);
                if (readFrame != -12 && readFrame != AVERROR_EOF) {
                    LOGE("readData() readFrame  : %d\n", readFrame);
                    continue;
                }

                LOGF("readData() AVERROR_EOF: %d\n", AVERROR_EOF);
                LOGF("readData() readFrame  : %d\n", readFrame);
                LOGF("readData() 文件已经读完了\n");
                LOGF("readData() video list2: %d\n", videoWrapper->father->list2->size());
                if (isH264) {
                    LOGF("readData() pktCounts  : %lld\n", pktCounts);
                }
                // 10000个kpt大约为111355502(111.35MB)

                // 读到文件末尾了
                videoWrapper->father->isReading = false;
                videoWrapper->father->isHandleList1Full = true;
                // 说明歌曲长度比较短,达不到"规定值",因此处理数据线程还在等待
                notifyToHandle(videoWrapper->father);

                // 不退出线程
                LOGW("readData() notifyToReadWait start\n");
                notifyToReadWait(videoWrapper->father);
                LOGW("readData() notifyToReadWait end\n");
                if (videoWrapper->father->isPausedForSeek) {
                    LOGF("readData() start seek\n");
                    videoWrapper->father->isReading = true;
                    continue;
                } else {
                    // for (;;) end
                    break;
                }
            }// 文件已读完

            if (srcAVPacket->stream_index == videoWrapper->father->streamIndex) {
                if (videoWrapper->father->isReading) {
                    readDataImpl(videoWrapper->father, srcAVPacket, copyAVPacket);
                }
            }
        }// for(;;) end

        if (srcAVPacket != NULL) {
            av_packet_unref(srcAVPacket);
            srcAVPacket = NULL;
        }

        isReading = false;

        LOGW("%s\n", "readData() end");
        return NULL;
    }

    int handleVideoDataImpl(AVStream *stream, AVFrame *decodedAVFrame) {
        if (!videoWrapper->father->isStarted) {
            videoWrapper->father->isStarted = true;
            LOGW("handleVideoData() 视频已经准备好,开始播放!!!\n");
            // 回调(通知到java层)
            onPlayed();
        }

        if (isH264) {
            curProgress = (long long) decodedAVFrame->pkt_pos;
            curTime = av_gettime_relative();
            if (curTime - preTime >= 1000000) {// 1秒大概传送353618个字节
                // 1135286 1488904 1841467 2204207
                // LOGW("handleVideoData() curProgress: %lld\n", curProgress);
                preTime = curTime;
                onProgressUpdated(curProgress);
            }
        } else {
            videoPts = decodedAVFrame->pts * av_q2d(stream->time_base);
            // 显示时间进度
            curProgress = (long long) videoPts;
            if (curProgress > preProgress) {
                preProgress = curProgress;
                onProgressUpdated(curProgress);
            }
        }

        if (videoWrapper->father->isHandling) {
            // 3.lock锁定下一个即将要绘制的Surface
            ANativeWindow_lock(pANativeWindow, &mANativeWindow_Buffer, NULL);
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
                memcpy(dst + h * dstStride, src + h * srcStride, srcStride);
            }

            if (isH264) {
                videoSleep(11);
            } else {
                endTime = clock();
                int temp1 = (videoPts - videoPtsPre) * 1000000;
                if (temp1 > (endTime - startTime)) {
                    int temp2 = temp1 - (endTime - startTime);
                    int temp3 = (1.000000 / frameRate) * 1000000;

                    int sleepTime = 0;
                    if (temp3 > temp2) {
                        sleepTime = (temp3 - temp2) / 1000;
                    } else {
                        sleepTime = (temp2 - temp3) / 1000;
                    }
                    if (sleepTime > 0) {
                        if (sleepTime >= 12 && sleepTime <= 20) {
                            videoSleep(sleepTime);
                            //LOGW("handleVideoDataImpl() sleepTime: %d\n", sleepTime);
                        } else {
                            videoSleep(11);
                        }
                    }
                }
                videoPtsPre = videoPts;
            }

            // 6.unlock绘制
            ANativeWindow_unlockAndPost(pANativeWindow);
        }
    }

    int handleDataClose(Wrapper *wrapper) {
        LOGW("%s\n", "handleData() video end");

        // 让"读线程"退出
        LOGW("%s\n", "handleData() notifyToRead");
        notifyToRead(wrapper);

        while (isReading) {
            av_usleep(1000);
        }

        closeVideo();
        onFinished();
        LOGF("%s\n", "Safe Exit");
    }

    void *handleData(void *opaque) {
        if (opaque == NULL) {
            return NULL;
        }
        Wrapper *wrapper = NULL;
        int *type = (int *) opaque;
        if (*type == TYPE_AUDIO) {

        } else {
            wrapper = videoWrapper->father;
        }
        if (wrapper == NULL) {
            LOGE("%s\n", "wrapper is NULL");
            return NULL;
        }

        LOGW("handleData() video start\n");
        // 线程等待
        LOGW("handleData() wait() video start\n");
        notifyToHandleWait(wrapper);
        LOGW("handleData() wait() video end\n");

        if (!wrapper->isHandling) {
            handleDataClose(wrapper);
            return NULL;
        }

        LOGW("handleData() ANativeWindow_setBuffersGeometry() start\n");
        // 2.设置缓冲区的属性（宽、高、像素格式）,像素格式要和SurfaceView的像素格式一致
        ANativeWindow_setBuffersGeometry(pANativeWindow,
                                         videoWrapper->srcWidth,
                                         videoWrapper->srcHeight,
                                         WINDOW_FORMAT_RGBA_8888);
        LOGW("handleData() ANativeWindow_setBuffersGeometry() end\n");

        AVStream *stream = avFormatContext->streams[wrapper->streamIndex];
        AVPacket *srcAVPacket = av_packet_alloc();
        AVPacket *copyAVPacket = av_packet_alloc();
        // decodedAVFrame为解码后的数据
        AVFrame *decodedAVFrame = videoWrapper->decodedAVFrame;

        /***
         在播放过程中想要退出时,会执行videoWrapper->father->isHandling = false;
         这样的话,如果在解码过程中(avcodec_send_packet和avcodec_receive_frame)
         执行avcodec_send_packet时就退出,感觉不太好,需要让它把avcodec_receive_frame
         也做完再退出,这样比较好.因此定义了这个局部变量.
         */
        int ret = 0;
        bool allowDecode = false;
        for (;;) {
            if (!wrapper->isHandling) {
                // for (;;) end
                break;
            }

            // 暂停装置
            if (wrapper->isPausedForUser
                || wrapper->isPausedForSeek) {
                bool isPausedForSeek = wrapper->isPausedForSeek;
                if (isPausedForSeek) {
                    LOGW("handleData() wait() Seek  video start\n");
                    if (wrapper->list1->size() != 0) {
                        std::list<AVPacket>::iterator iter;
                        for (iter = wrapper->list1->begin();
                             iter != wrapper->list1->end();
                             iter++) {
                            AVPacket avPacket = *iter;
                            av_packet_unref(&avPacket);
                        }
                        wrapper->list1->clear();
                    }
                    wrapper->isHandleList1Full = false;
                    wrapper->needToSeek = true;
                } else {
                    LOGW("handleData() wait() User  video start\n");
                }
                notifyToHandleWait(wrapper);
                if (wrapper->isPausedForUser || wrapper->isPausedForSeek) {
                    continue;
                }
                if (isPausedForSeek) {
                    LOGW("handleData() wait() Seek  video end\n");
                } else {
                    LOGW("handleData() wait() User  video end\n");
                }
            }// 暂停装置 end

            allowDecode = false;
            if (wrapper->list1->size() > 0) {
                srcAVPacket = &wrapper->list1->front();
                // 内容copy
                av_packet_ref(copyAVPacket, srcAVPacket);
                av_packet_unref(srcAVPacket);
                wrapper->list1->pop_front();
                allowDecode = true;
            }

            size_t list2Size = wrapper->list2->size();
            if (wrapper->isReading) {
                if (wrapper->list1->size() == 0) {
                    wrapper->isHandleList1Full = false;
                    if (list2Size > 0) {
                        pthread_mutex_lock(&videoWrapper->father->readLockMutex);
                        wrapper->list1->clear();
                        wrapper->list1->assign(wrapper->list2->begin(), wrapper->list2->end());
                        wrapper->list2->clear();
                        wrapper->isHandleList1Full = true;
                        pthread_mutex_unlock(&videoWrapper->father->readLockMutex);

                        notifyToRead(wrapper);
                    }
                }
            } else {
                if (wrapper->list1->size() > 0) {
                    // 还有数据,先用完再说
                } else {
                    if (list2Size > 0) {
                        // 把剩余的数据全部复制过来
                        pthread_mutex_lock(&videoWrapper->father->readLockMutex);
                        wrapper->list1->clear();
                        wrapper->list1->assign(wrapper->list2->begin(), wrapper->list2->end());
                        wrapper->list2->clear();
                        pthread_mutex_unlock(&videoWrapper->father->readLockMutex);

                        LOGW("handleData() video 最后要处理的数据还有 list1: %d\n", wrapper->list1->size());
                    } else {
                        wrapper->isHandling = false;
                    }
                }
            }

            if (wrapper->isReading
                && wrapper->isHandling
                && !wrapper->isHandleList1Full
                && wrapper->list2->size() == 0) {
                // 开始暂停
                onPaused();

                videoWrapper->father->isPausedForCache = true;
                LOGE("handleData() wait() Cache video start\n");
                notifyToHandleWait(videoWrapper->father);
                if (wrapper->isPausedForSeek) {
                    videoWrapper->father->isPausedForCache = false;
                    continue;
                }
                LOGE("handleData() wait() Cache video end\n");
                videoWrapper->father->isPausedForCache = false;

                // 开始播放
                onPlayed();
            }

            if (!wrapper->isHandling) {
                // for (;;) end
                break;
            }

            if (!allowDecode) {
                continue;
            }

            if (!isH264) {
                startTime = clock();
            }

            // 解码过程
            ret = avcodec_send_packet(wrapper->avCodecContext, copyAVPacket);
            av_packet_unref(copyAVPacket);
            switch (ret) {
                case AVERROR(EAGAIN):
                    LOGE("handleData() video avcodec_send_packet   ret: %d\n", ret);
                    break;
                case AVERROR(EINVAL):
                case AVERROR(ENOMEM):
                case AVERROR_EOF:
                    LOGE("handleData() video avcodec_send_packet 发送数据包到解码器时出错 %d", ret);
                    // wrapper->isHandling = false;
                    break;
                case 0:
                default:
                    break;
            }// switch (ret) end

            if (!wrapper->isHandling) {
                // for (;;) end
                break;
            }

            if (ret != 0) {
                continue;
            }

            ret = avcodec_receive_frame(wrapper->avCodecContext, decodedAVFrame);
            switch (ret) {
                // 输出是不可用的,必须发送新的输入
                case AVERROR(EAGAIN):
                    if (!isH264) {
                        LOGE("handleData() video avcodec_receive_frame ret: %d\n", ret);
                    }
                    break;
                case AVERROR(EINVAL):
                    // codec打不开,或者是一个encoder
                case AVERROR_EOF:
                    // 已经完全刷新,不会再有输出帧了
                    // wrapper->isHandling = false;
                    break;
                case 0: {
                    // 解码成功,返回一个输出帧
                    break;
                }
                default:
                    // 合法的解码错误
                    LOGE("handleData() video avcodec_receive_frame 从解码器接收帧时出错 %d", ret);
                    break;
            }// switch (ret) end

            if (!wrapper->isHandling) {
                // for (;;) end
                break;
            }

            if (ret != 0) {
                continue;
            }

            ///////////////////////////////////////////////////////////////////

            // 渲染画面
            handleVideoDataImpl(stream, decodedAVFrame);

            // 设置结束标志
            if (!wrapper->isReading
                && wrapper->list1->size() == 0
                && wrapper->list2->size() == 0) {
                wrapper->isHandling = false;
            }

            ///////////////////////////////////////////////////////////////////
        }//for(;;) end

        if (srcAVPacket != NULL) {
            av_packet_unref(srcAVPacket);
            // app crash 上面的copyAVPacket调用却没事,why
            // av_packet_free(&srcAVPacket);
            srcAVPacket = NULL;
        }
        if (copyAVPacket != NULL) {
            av_packet_free(&copyAVPacket);
            copyAVPacket = NULL;
        }

        handleDataClose(wrapper);

        return NULL;
    }

    void closeVideo() {
        if (pANativeWindow != NULL) {
            // 7.释放资源
            ANativeWindow_release(pANativeWindow);
            pANativeWindow = NULL;
        }
        // video
        if (videoWrapper == NULL
            || videoWrapper->father == NULL) {
            return;
        }
        LOGW("%s\n", "closeVideo() start");
        if (videoWrapper->father->outBuffer1 != NULL) {
            av_free(videoWrapper->father->outBuffer1);
            videoWrapper->father->outBuffer1 = NULL;
        }
        if (videoWrapper->father->outBuffer2 != NULL) {
            av_free(videoWrapper->father->outBuffer2);
            videoWrapper->father->outBuffer2 = NULL;
        }
        if (videoWrapper->father->outBuffer3 != NULL) {
            av_free(videoWrapper->father->outBuffer3);
            videoWrapper->father->outBuffer3 = NULL;
        }
        if (videoWrapper->swsContext != NULL) {
            sws_freeContext(videoWrapper->swsContext);
            videoWrapper->swsContext = NULL;
        }
        if (videoWrapper->decodedAVFrame != NULL) {
            av_frame_free(&videoWrapper->decodedAVFrame);
            videoWrapper->decodedAVFrame = NULL;
        }
        if (videoWrapper->rgbAVFrame != NULL) {
            av_frame_free(&videoWrapper->rgbAVFrame);
            videoWrapper->rgbAVFrame = NULL;
        }
        if (videoWrapper->father->avCodecContext != NULL) {
            avcodec_close(videoWrapper->father->avCodecContext);
            av_free(videoWrapper->father->avCodecContext);
            videoWrapper->father->avCodecContext = NULL;
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
        videoWrapper->father->list1 = NULL;
        videoWrapper->father->list2 = NULL;

        avformat_free_context(avFormatContext);
        avFormatContext = NULL;
        av_free(videoWrapper->father);
        videoWrapper->father = NULL;
        av_free(videoWrapper);
        videoWrapper = NULL;

        LOGW("%s\n", "closeVideo() end");
    }

    int initPlayer() {
        LOGW("%s\n", "initPlayer() start");

        onReady();
        initAV();
        initVideo();
        if (openAndFindAVFormatContext() < 0) {
            LOGE("openAndFindAVFormatContext() failed\n");
            closeVideo();
            //onError();
            return -1;
        }
        if (findStreamIndex() < 0) {
            LOGE("findStreamIndex() failed\n");
            closeVideo();
            //onError();
            return -1;
        }
        if (findAndOpenAVCodecForVideo() < 0) {
            LOGE("findAndOpenAVCodecForVideo() failed\n");
            closeVideo();
            //onError();
            return -1;
        }
        if (createSwsContext() < 0) {
            LOGE("createSwsContext() failed\n");
            closeVideo();
            //onError();
            return -1;
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
        onChangeWindow(videoWrapper->srcWidth, videoWrapper->srcHeight);

        LOGW("%s\n", "initPlayer() end");
        return 0;
    }

    void setJniParameters(JNIEnv *env, const char *filePath, jobject surfaceJavaObject) {
        memset(inFilePath, '\0', sizeof(inFilePath));
        av_strlcpy(inFilePath, filePath, sizeof(inFilePath));
        LOGI("setJniParameters() filePath  : %s", inFilePath);

        isLocal = false;
        char *result = strstr(inFilePath, "http://");
        if (result == nullptr) {
            result = strstr(inFilePath, "https://");
            if (result == nullptr) {
                result = strstr(inFilePath, "rtmp://");
                if (result == nullptr) {
                    result = strstr(inFilePath, "rtsp://");
                    if (result == nullptr) {
                        isLocal = true;
                    }
                }
            }
        }
        isH264 = true;
        result = strstr(inFilePath, ".h264");
        if (result == nullptr) {
            result = strstr(inFilePath, ".H264");
            if (result == nullptr) {
                isH264 = false;
            }
        }
        LOGI("setJniParameters() isLocal   : %d", isLocal);
        LOGI("setJniParameters() isH264    : %d", isH264);

        if (pANativeWindow != NULL) {
            ANativeWindow_release(pANativeWindow);
            pANativeWindow = NULL;
        }
        // 1.获取一个关联Surface的NativeWindow窗体
        pANativeWindow = ANativeWindow_fromSurface(env, surfaceJavaObject);
    }

    int play() {
        if (videoWrapper != NULL && videoWrapper->father != NULL) {
            videoWrapper->father->isPausedForUser = false;
            notifyToHandle(videoWrapper->father);
        }
        return 0;
    }

    int pause() {
        LOGI("pause() start\n");
        if (videoWrapper != NULL && videoWrapper->father != NULL) {
            videoWrapper->father->isPausedForUser = true;
        }
        LOGI("pause() end\n");
        return 0;
    }

    int stop() {
        if (videoWrapper != NULL
            && videoWrapper->father != NULL) {
            LOGI("stop() start\n");
            videoWrapper->father->isStarted = false;
            videoWrapper->father->isReading = false;
            videoWrapper->father->isHandling = false;
            videoWrapper->father->isHandleList1Full = false;
            videoWrapper->father->isPausedForUser = false;
            videoWrapper->father->isPausedForCache = false;
            videoWrapper->father->isPausedForSeek = false;
            notifyToRead(videoWrapper->father);
            notifyToHandle(videoWrapper->father);
            LOGI("stop() end\n");
        }

        return 0;
    }

    int release() {
        stop();
        return 0;
    }

    // 有没有在运行,即使暂停状态也是运行状态
    bool isRunning() {
        bool videoRunning = false;
        if (videoWrapper != NULL && videoWrapper->father != NULL) {
            videoRunning = videoWrapper->father->isStarted
                           && videoWrapper->father->isHandling;
        }
        return videoRunning;
    }

    // 有没有在播放,暂停状态不算播放状态
    bool isPlaying() {
        bool videoPlaying = false;
        if (videoWrapper != NULL && videoWrapper->father != NULL) {
            videoPlaying = videoWrapper->father->isStarted
                           && videoWrapper->father->isHandling
                           && !videoWrapper->father->isPausedForUser
                           && !videoWrapper->father->isPausedForCache;
        }
        return videoPlaying;
    }

    bool isPausedForUser() {
        bool videoPlaying = false;
        if (videoWrapper != NULL
            && videoWrapper->father != NULL) {
            videoPlaying = videoWrapper->father->isStarted
                           && videoWrapper->father->isHandling
                           && videoWrapper->father->isPausedForUser;
        }
        return videoPlaying;
    }

    int seekTo(int64_t timestamp) {
        LOGI("==================================================================\n");
        LOGI("seekTo() timestamp: %lld\n", (long long) timestamp);
        //LOGI("seekTo() timestamp: %" PRIu64 "\n", timestamp);

        if ((long long) timestamp > 0
            && videoWrapper == NULL) {
            timeStamp = timestamp;
            return 0;
        }

        if (((long long) timestamp) < 0
            || videoWrapper == NULL
            || videoWrapper->father == NULL
            || videoWrapper->father->isPausedForSeek
            || getDuration() < 0
            || ((long long) timestamp) > getDuration()) {
            return -1;
        }

        LOGW("seekTo() signal() to Read and Handle\n");

        timeStamp = timestamp;
        videoWrapper->father->isPausedForSeek = true;
        videoWrapper->father->needToSeek = false;
        notifyToHandle(videoWrapper->father);
        notifyToRead(videoWrapper->father);

        /*double pos, incr;
        pos = 11000000;
        incr = seek_interval ? -seek_interval : -10.0;// 快退
        incr = seek_interval ? seek_interval : 10.0;// 快进
        incr *= 180000.0;
        pos += incr;
        int64_t seek_rel = incr;
        int64_t seek_target = pos;
        int64_t seek_min = seek_rel > 0 ? seek_target - seek_rel + 2 : INT64_MIN;
        int64_t seek_max = seek_rel < 0 ? seek_target - seek_rel - 2 : INT64_MAX;
        LOGW("readData() seek_min: %ld\n", (long) seek_min);
        LOGW("readData()      pos: %ld\n", (long) seek_target);
        LOGW("readData() seek_max: %ld\n", (long) seek_max);
        avformat_seek_file(avFormatContext, -1, seek_min, seek_target, seek_max, seek_flags);*/

        return 0;
    }

    // 返回值单位是秒
    int64_t getDuration() {
        if (isH264) {
            return fileLength;
        } else {
            int64_t videoDuration = -1;
            if (videoWrapper != NULL
                && videoWrapper->father != NULL) {
                videoDuration = videoWrapper->father->duration;
            }
            return videoDuration;
        }
    }

    void stepAdd(int64_t addStep) {
        /*++videoSleepTime;
        char dest[50];
        sprintf(dest, "videoSleepTime: %d\n", videoSleepTime);
        onInfo(dest);
        LOGF("stepAdd()      videoSleepTime: %d\n", videoSleepTime);*/

        if (getDuration() > 0) {
            seekTo((int64_t) curProgress + addStep);
        }
    }

    void stepSubtract(int64_t subtractStep) {
        /*--videoSleepTime;
        char dest[50];
        sprintf(dest, "videoSleepTime: %d\n", videoSleepTime);
        onInfo(dest);
        LOGF("stepSubtract() videoSleepTime: %d\n", videoSleepTime);*/

        if (getDuration() > 0) {
            seekTo((int64_t) curProgress - subtractStep);
        }
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

    char *getStrAVCodec(AVCodecID codecID) {
        char info[50] = {0};
        switch (codecID) {
            case AV_CODEC_ID_NONE:// 0
                strncpy(info, "AV_CODEC_ID_NONE", strlen("AV_CODEC_ID_NONE"));
                break;
            case AV_CODEC_ID_MPEG1VIDEO:
                strncpy(info, "AV_CODEC_ID_MPEG1VIDEO", strlen("AV_CODEC_ID_MPEG1VIDEO"));
                break;
            case AV_CODEC_ID_MPEG2VIDEO:
                strncpy(info, "AV_CODEC_ID_MPEG2VIDEO", strlen("AV_CODEC_ID_MPEG2VIDEO"));
                break;
            case AV_CODEC_ID_H261:
                strncpy(info, "AV_CODEC_ID_H261", strlen("AV_CODEC_ID_H261"));
                break;
            case AV_CODEC_ID_H263:
                strncpy(info, "AV_CODEC_ID_H263", strlen("AV_CODEC_ID_H263"));
                break;
            case AV_CODEC_ID_RV10:
                strncpy(info, "AV_CODEC_ID_RV10", strlen("AV_CODEC_ID_RV10"));
                break;
            case AV_CODEC_ID_RV20:
                strncpy(info, "AV_CODEC_ID_RV20", strlen("AV_CODEC_ID_RV20"));
                break;
            case AV_CODEC_ID_MJPEG:
                strncpy(info, "AV_CODEC_ID_MJPEG", strlen("AV_CODEC_ID_MJPEG"));
                break;
            case AV_CODEC_ID_MJPEGB:
                strncpy(info, "AV_CODEC_ID_MJPEGB", strlen("AV_CODEC_ID_MJPEGB"));
                break;
            case AV_CODEC_ID_LJPEG:
                strncpy(info, "AV_CODEC_ID_LJPEG", strlen("AV_CODEC_ID_LJPEG"));
                break;
            case AV_CODEC_ID_SP5X:
                strncpy(info, "AV_CODEC_ID_SP5X", strlen("AV_CODEC_ID_SP5X"));
                break;
            case AV_CODEC_ID_JPEGLS:
                strncpy(info, "AV_CODEC_ID_JPEGLS", strlen("AV_CODEC_ID_JPEGLS"));
                break;
            case AV_CODEC_ID_MPEG4:
                strncpy(info, "AV_CODEC_ID_MPEG4", strlen("AV_CODEC_ID_MPEG4"));
                break;
            case AV_CODEC_ID_RAWVIDEO:
                strncpy(info, "AV_CODEC_ID_RAWVIDEO", strlen("AV_CODEC_ID_RAWVIDEO"));
                break;
            case AV_CODEC_ID_MSMPEG4V1:
                strncpy(info, "AV_CODEC_ID_MSMPEG4V1", strlen("AV_CODEC_ID_MSMPEG4V1"));
                break;
            case AV_CODEC_ID_MSMPEG4V2:
                strncpy(info, "AV_CODEC_ID_MSMPEG4V2", strlen("AV_CODEC_ID_MSMPEG4V2"));
                break;
            case AV_CODEC_ID_MSMPEG4V3:
                strncpy(info, "AV_CODEC_ID_MSMPEG4V3", strlen("AV_CODEC_ID_MSMPEG4V3"));
                break;
            case AV_CODEC_ID_WMV1:
                strncpy(info, "AV_CODEC_ID_WMV1", strlen("AV_CODEC_ID_WMV1"));
                break;
            case AV_CODEC_ID_WMV2:
                strncpy(info, "AV_CODEC_ID_WMV2", strlen("AV_CODEC_ID_WMV2"));
                break;
            case AV_CODEC_ID_H263P:
                strncpy(info, "AV_CODEC_ID_H263P", strlen("AV_CODEC_ID_H263P"));
                break;
            case AV_CODEC_ID_H263I:
                strncpy(info, "AV_CODEC_ID_H263I", strlen("AV_CODEC_ID_H263I"));
                break;
            case AV_CODEC_ID_FLV1:
                strncpy(info, "AV_CODEC_ID_FLV1", strlen("AV_CODEC_ID_FLV1"));
                break;
            case AV_CODEC_ID_SVQ1:
                strncpy(info, "AV_CODEC_ID_SVQ1", strlen("AV_CODEC_ID_SVQ1"));
                break;
            case AV_CODEC_ID_SVQ3:
                strncpy(info, "AV_CODEC_ID_SVQ3", strlen("AV_CODEC_ID_SVQ3"));
                break;
            case AV_CODEC_ID_DVVIDEO:
                strncpy(info, "AV_CODEC_ID_DVVIDEO", strlen("AV_CODEC_ID_DVVIDEO"));
                break;
            case AV_CODEC_ID_HUFFYUV:
                strncpy(info, "AV_CODEC_ID_HUFFYUV", strlen("AV_CODEC_ID_HUFFYUV"));
                break;
            case AV_CODEC_ID_CYUV:
                strncpy(info, "AV_CODEC_ID_CYUV", strlen("AV_CODEC_ID_CYUV"));
                break;
            case AV_CODEC_ID_H264:
                strncpy(info, "AV_CODEC_ID_H264", strlen("AV_CODEC_ID_H264"));
                break;
            case AV_CODEC_ID_INDEO3:
                strncpy(info, "AV_CODEC_ID_INDEO3", strlen("AV_CODEC_ID_INDEO3"));
                break;
            case AV_CODEC_ID_VP3:
                strncpy(info, "AV_CODEC_ID_VP3", strlen("AV_CODEC_ID_VP3"));
                break;
            case AV_CODEC_ID_THEORA:
                strncpy(info, "AV_CODEC_ID_THEORA", strlen("AV_CODEC_ID_THEORA"));
                break;
            case AV_CODEC_ID_ASV1:
                strncpy(info, "AV_CODEC_ID_ASV1", strlen("AV_CODEC_ID_ASV1"));
                break;
            case AV_CODEC_ID_ASV2:
                strncpy(info, "AV_CODEC_ID_ASV2", strlen("AV_CODEC_ID_ASV2"));
                break;
            case AV_CODEC_ID_FFV1:
                strncpy(info, "AV_CODEC_ID_FFV1", strlen("AV_CODEC_ID_FFV1"));
                break;
            case AV_CODEC_ID_4XM:
                strncpy(info, "AV_CODEC_ID_4XM", strlen("AV_CODEC_ID_4XM"));
                break;
            case AV_CODEC_ID_VCR1:
                strncpy(info, "AV_CODEC_ID_VCR1", strlen("AV_CODEC_ID_VCR1"));
                break;
            case AV_CODEC_ID_CLJR:
                strncpy(info, "AV_CODEC_ID_CLJR", strlen("AV_CODEC_ID_CLJR"));
                break;
            case AV_CODEC_ID_MDEC:
                strncpy(info, "AV_CODEC_ID_MDEC", strlen("AV_CODEC_ID_MDEC"));
                break;
            case AV_CODEC_ID_ROQ:
                strncpy(info, "AV_CODEC_ID_ROQ", strlen("AV_CODEC_ID_ROQ"));
                break;
            case AV_CODEC_ID_INTERPLAY_VIDEO:
                strncpy(info, "AV_CODEC_ID_INTERPLAY_VIDEO", strlen("AV_CODEC_ID_INTERPLAY_VIDEO"));
                break;
            case AV_CODEC_ID_XAN_WC3:
                strncpy(info, "AV_CODEC_ID_XAN_WC3", strlen("AV_CODEC_ID_XAN_WC3"));
                break;
            case AV_CODEC_ID_XAN_WC4:
                strncpy(info, "AV_CODEC_ID_XAN_WC4", strlen("AV_CODEC_ID_XAN_WC4"));
                break;
            case AV_CODEC_ID_RPZA:
                strncpy(info, "AV_CODEC_ID_RPZA", strlen("AV_CODEC_ID_RPZA"));
                break;
            case AV_CODEC_ID_CINEPAK:
                strncpy(info, "AV_CODEC_ID_CINEPAK", strlen("AV_CODEC_ID_CINEPAK"));
                break;
            case AV_CODEC_ID_WS_VQA:
                strncpy(info, "AV_CODEC_ID_WS_VQA", strlen("AV_CODEC_ID_WS_VQA"));
                break;
            case AV_CODEC_ID_MSRLE:
                strncpy(info, "AV_CODEC_ID_MSRLE", strlen("AV_CODEC_ID_MSRLE"));
                break;
            case AV_CODEC_ID_MSVIDEO1:
                strncpy(info, "AV_CODEC_ID_MSVIDEO1", strlen("AV_CODEC_ID_MSVIDEO1"));
                break;
            case AV_CODEC_ID_IDCIN:
                strncpy(info, "AV_CODEC_ID_IDCIN", strlen("AV_CODEC_ID_IDCIN"));
                break;
            case AV_CODEC_ID_8BPS:
                strncpy(info, "AV_CODEC_ID_8BPS", strlen("AV_CODEC_ID_8BPS"));
                break;
            case AV_CODEC_ID_SMC:
                strncpy(info, "AV_CODEC_ID_SMC", strlen("AV_CODEC_ID_SMC"));
                break;
            case AV_CODEC_ID_FLIC:
                strncpy(info, "AV_CODEC_ID_FLIC", strlen("AV_CODEC_ID_FLIC"));
                break;
            case AV_CODEC_ID_TRUEMOTION1:
                strncpy(info, "AV_CODEC_ID_TRUEMOTION1", strlen("AV_CODEC_ID_TRUEMOTION1"));
                break;
            case AV_CODEC_ID_VMDVIDEO:
                strncpy(info, "AV_CODEC_ID_VMDVIDEO", strlen("AV_CODEC_ID_VMDVIDEO"));
                break;
            case AV_CODEC_ID_MSZH:
                strncpy(info, "AV_CODEC_ID_MSZH", strlen("AV_CODEC_ID_MSZH"));
                break;
            case AV_CODEC_ID_ZLIB:
                strncpy(info, "AV_CODEC_ID_ZLIB", strlen("AV_CODEC_ID_ZLIB"));
                break;
            case AV_CODEC_ID_HEVC:
                strncpy(info, "AV_CODEC_ID_HEVC AV_CODEC_ID_H265",
                        strlen("AV_CODEC_ID_HEVC AV_CODEC_ID_H265"));
                break;
            default:
                strncpy(info, "AV_CODEC_ID_NONE", strlen("AV_CODEC_ID_NONE"));
                break;
        }

        return info;
    }

    char *getStrAVPixelFormat(AVPixelFormat format) {
        char info[50] = {0};
        switch (format) {
            case AV_PIX_FMT_NONE:
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

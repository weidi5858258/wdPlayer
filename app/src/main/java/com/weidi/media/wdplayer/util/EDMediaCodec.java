package com.weidi.media.wdplayer.util;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import java.nio.ByteBuffer;

public class EDMediaCodec {

    private static final String TAG =
            "player_alexander";

    private static final int TIME_OUT = 10000;

    public enum TYPE {
        TYPE_VIDEO,
        TYPE_AUDIO,
        //TYPE_SUBTITLE
    }

    public interface Callback {
        boolean isVideoFinished();

        boolean isAudioFinished();

        void handleVideoOutputFormat(MediaFormat mediaFormat);

        void handleAudioOutputFormat(MediaFormat mediaFormat);

        int handleVideoOutputBuffer(int roomIndex, ByteBuffer room,
                                    MediaCodec.BufferInfo roomInfo, int roomSize);

        int handleAudioOutputBuffer(int roomIndex, ByteBuffer room,
                                    MediaCodec.BufferInfo roomInfo, int roomSize);
    }

    /***
     *
     * @param callback               Callback
     * @param type                   TYPE.TYPE_VIDEO or TYPE.TYPE_AUDIO
     * @param mediaCodec             编解码器
     * @param data                   需要编解码的数据
     * @param offset                 一般为0
     * @param size                   编解码数据的大小
     * @param presentationTimeUs     时间戳
     * @param render                 TYPE.TYPE_AUDIO为false,TYPE.TYPE_VIDEO为true(录制屏幕时为false).
     * @param needFeedInputBuffer    一般为true.为false时,data,offset,size和presentationTimeUs随便写
     * @return
     */
    public static boolean feedInputBufferAndDrainOutputBuffer(
            Callback callback,
            TYPE type,
            MediaCodec mediaCodec,
            MediaCodec.CryptoInfo cryptoInfo,
            byte[] data,
            int offset,
            int size,
            long presentationTimeUs,
            int flags,
            boolean render,
            boolean needFeedInputBuffer) {
        if (needFeedInputBuffer) {
            return feedInputBuffer(
                    callback,
                    type,
                    mediaCodec,
                    cryptoInfo,
                    data,
                    offset,
                    size,
                    presentationTimeUs,
                    flags)
                    &&
                    drainOutputBuffer(callback, type, mediaCodec, render);
        }

        // 录制屏幕时,video是没有Input过程的
        return drainOutputBuffer(callback, type, mediaCodec, render);
    }

    /***
     * 填充数据送到底层进行编解码
     * @param codec
     * @param data
     * @param offset
     * @param size
     * @param presentationTimeUs
     * @return
     *
    如果第一次调用feedInputBuffer(...)方法,
    执行dequeueInputBuffer(...)时就抛出java.lang.IllegalStateException异常
    android.media.MediaCodec.native_dequeueInputBuffer(Native Method)
    很可能是创建的MediaCodec有问题,这个又是MediaFormat引起的
     */
    private static boolean feedInputBuffer(
            Callback callback,
            TYPE type,
            MediaCodec codec,
            MediaCodec.CryptoInfo info,
            byte[] data,
            int offset,
            int size,
            long presentationTimeUs,
            int flags) {
        try {
            // 拿到房间号
            int roomIndex = codec.dequeueInputBuffer(TIME_OUT);
            if (roomIndex < 0) {
                return true;
            }

            ByteBuffer room = null;
            // 根据房间号找到房间
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                room = codec.getInputBuffer(roomIndex);
            } else {
                room = codec.getInputBuffers()[roomIndex];
            }
            if (room == null) {
                return false;
            }

            // 入住之前打扫一下房间
            room.clear();
            // 入住
            room.put(data, offset, size);

            if (size <= 0) {
                presentationTimeUs = 0L;
                flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
            }

            // 通知已经"入住"了,可以进行"编解码"的操作了
            if (info == null) {
                codec.queueInputBuffer(
                        roomIndex,
                        offset,
                        size,
                        presentationTimeUs,
                        flags);
            } else {
                codec.queueSecureInputBuffer(
                        roomIndex,
                        offset,
                        info,
                        presentationTimeUs,
                        flags);
            }

            // reset
            roomIndex = -1;
            room = null;
        } catch (MediaCodec.CryptoException
                | IllegalStateException
                | IllegalArgumentException
                | NullPointerException e) {
            e.printStackTrace();
            if (type == TYPE.TYPE_AUDIO) {
                Log.e(TAG, "feedInputBuffer() Audio Input occur exception: " + e);
                callback.handleAudioOutputBuffer(-1, null, null, -1);
            } else {
                Log.e(TAG, "feedInputBuffer() Video Input occur exception: " + e);
                callback.handleVideoOutputBuffer(-1, null, null, -1);
            }
            return false;
        }

        return true;
    }

    /***
     * 拿出数据(在底层已经经过编解码了)进行处理(如视频数据进行渲染,音频数据进行播放)
     * @return
     */
    private static boolean drainOutputBuffer(
            Callback callback,
            TYPE type,
            MediaCodec codec,
            boolean render) {
        // 房间信息
        MediaCodec.BufferInfo roomInfo = new MediaCodec.BufferInfo();
        ByteBuffer room = null;
        for (; ; ) {
            if (type == TYPE.TYPE_AUDIO) {
                if (callback.isAudioFinished()) {
                    callback.handleAudioOutputBuffer(-1, null, null, -1);
                    break;
                }
            } else {
                if (callback.isVideoFinished()) {
                    callback.handleVideoOutputBuffer(-1, null, null, -1);
                    break;
                }
            }

            try {
                // 房间号
                int roomIndex = codec.dequeueOutputBuffer(roomInfo, TIME_OUT);
                /*if (type == TYPE.TYPE_AUDIO) {
                    //Log.d(TAG, "drainOutputBuffer() Audio roomIndex: " + roomIndex);
                } else {
                    //Log.w(TAG, "drainOutputBuffer() Video roomIndex: " + roomIndex);
                }*/
                switch (roomIndex) {
                    case MediaCodec.INFO_TRY_AGAIN_LATER:// -1
                        // 不断的调用,不要打印日志
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:// -2
                        // 像音频,第三个输出日志
                        // 一般一个视频各自调用一次
                        if (type == TYPE.TYPE_AUDIO) {
                            Log.d(TAG, "drainOutputBuffer() " +
                                    "Audio Output MediaCodec.INFO_OUTPUT_FORMAT_CHANGED");
                            callback.handleAudioOutputFormat(codec.getOutputFormat());
                        } else {
                            Log.w(TAG, "drainOutputBuffer() " +
                                    "Video Output MediaCodec.INFO_OUTPUT_FORMAT_CHANGED");
                            callback.handleVideoOutputFormat(codec.getOutputFormat());
                        }
                        break;
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:// -3
                        // 像音频,第二个输出日志.视频好像没有这个输出日志
                        if (type == TYPE.TYPE_AUDIO) {
                            Log.d(TAG, "drainOutputBuffer() " +
                                    "Audio Output MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                        } else {
                            Log.w(TAG, "drainOutputBuffer() " +
                                    "Video Output MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                        }
                        break;
                    default:
                        break;
                }
                if (roomIndex < 0) {
                    break;
                }

                if ((roomInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {// 2
                    if (type == TYPE.TYPE_AUDIO) {
                        Log.d(TAG, "drainOutputBuffer() " +
                                "Audio Output MediaCodec.BUFFER_FLAG_CODEC_CONFIG");
                    } else {
                        Log.w(TAG, "drainOutputBuffer() " +
                                "Video Output MediaCodec.BUFFER_FLAG_CODEC_CONFIG");
                    }
                    //codec.releaseOutputBuffer(roomIndex, render);
                    //continue;
                }
                if ((roomInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {// 4
                    if (type == TYPE.TYPE_AUDIO) {
                        Log.d(TAG, "drainOutputBuffer() " +
                                "Audio Output MediaCodec.BUFFER_FLAG_END_OF_STREAM");
                    } else {
                        Log.w(TAG, "drainOutputBuffer() " +
                                "Video Output MediaCodec.BUFFER_FLAG_END_OF_STREAM");
                    }
                    // 结束
                    return false;
                }

                // 根据房间号找到房间
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    room = codec.getOutputBuffer(roomIndex);
                } else {
                    room = codec.getOutputBuffers()[roomIndex];
                }
                // 房间大小
                int roomSize = roomInfo.size;
                // 不能根据room是否为null来判断是audio还是video(但我的三星Note2手机上是可以的)
                if (room != null) {
                    room.position(roomInfo.offset);
                    room.limit(roomInfo.offset + roomSize);
                    if (type == TYPE.TYPE_AUDIO) {
                        callback.handleAudioOutputBuffer(roomIndex, room, roomInfo, roomSize);
                    } else {
                        callback.handleVideoOutputBuffer(roomIndex, room, roomInfo, roomSize);
                    }
                    room.clear();
                } else {
                    if (type == TYPE.TYPE_AUDIO) {
                        callback.handleAudioOutputBuffer(roomIndex, null, null, -1);
                    } else {
                        callback.handleVideoOutputBuffer(roomIndex, null, null, -1);
                    }
                }

                codec.releaseOutputBuffer(roomIndex, render);
            } catch (MediaCodec.CryptoException
                    | IllegalStateException
                    | IllegalArgumentException
                    | NullPointerException e) {
                e.printStackTrace();
                if (type == TYPE.TYPE_AUDIO) {
                    Log.e(TAG, "drainOutputBuffer() Audio Output occur exception: " + e);
                    callback.handleAudioOutputBuffer(-1, null, null, -1);
                } else {
                    Log.e(TAG, "drainOutputBuffer() Video Output occur exception: " + e);
                    callback.handleVideoOutputBuffer(-1, null, null, -1);
                }
                return false;
            }
        }// for(;;) end

        return true;
    }

}

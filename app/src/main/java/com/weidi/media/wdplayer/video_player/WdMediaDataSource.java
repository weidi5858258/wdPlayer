package com.weidi.media.wdplayer.video_player;

import android.media.MediaDataSource;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.M)
public class WdMediaDataSource extends MediaDataSource {

    @Override
    public int readAt(long position, byte[] buffer, int offset, int size) throws IOException {
        /*Log.i(TAG, "readAt() before" +
                " readBigBufferIndex: " + readBigBufferIndex +
                " position: " + position +
                " writeBigBufferIndex: " + writeBigBufferIndex +
                " fillBigBufferCount: " + fillBigBufferCount +
                " offset: " + offset +
                " size: " + size);*/
        int ret = readAt_(position, buffer, offset, size);
        /*Log.i(TAG, "readAt()  after" +
                " readBigBufferIndex: " + readBigBufferIndex +
                " position: " + position +
                " writeBigBufferIndex: " + writeBigBufferIndex +
                " fillBigBufferCount: " + fillBigBufferCount +
                " offset: " + offset +
                " size: " + size +
                " ret: " + ret
        );*/
        return ret;
    }

    @Override
    public long getSize() throws IOException {
        Log.i(TAG, "LocalMediaDataSource getSize() mFileLength: " + mFileLength);
        return 0;// mFileLength
    }

    @Override
    public void close() throws IOException {
        Log.i(TAG, "LocalMediaDataSource close()");
        if (mInputStream != null) {
            try {
                mInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mInputStream = null;
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////

    private static final String TAG = "player_alexander";

    public interface Callback {
        void onStart();

        void onEnd();

        void onError(String msg);
    }

    // Invalid values.
    protected static final int INVALID_VALUE = -1;
    protected static final long INVALID_POSITION = -1;

    private Object readDataLock = new Object();
    private Object bufferLock = new Object();
    // 52428800(50MB) 104857600(100MB)
    private byte[] bigBuffer = new byte[1024 * 1024 * 100];
    // 5242880
    private byte[] smallBuffer = new byte[1024 * 1024 * 5];
    // bigBufferLength必须是smallBufferLength的整数倍
    private long bigBufferLength = bigBuffer.length;
    private long smallBufferLength = smallBuffer.length;
    private long tempLength1 = 0;
    private long tempLength2 = 0;

    private boolean isEOF = false;
    private boolean isReading = true;
    private int fillBigBufferCount = 0;
    // readAt(...)中的position位置
    private long readBigBufferIndex = 0;
    // 当前数据要写入的开始位置,上一次数据写入的最后位置是writeBigBufferIndex-1
    private long writeBigBufferIndex = 0;
    private long smallBufferSize = 0;

    // 4K 3110400
    // 5242880(5MB)
    // 3145728(3MB)
    // 20971520(20MB) 23494850
    private static final int max_input_size = 1024 * 1024 * 3;

    public boolean hasSetDataSource = false;

    private Callback mCallback;
    private long mFileLength;
    private InputStream mInputStream;

    // 必须要设置
    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public void setDataSource(String localPath) {
        if (TextUtils.isEmpty(localPath)) {
            return;
        }
        File file = new File(localPath);
        mFileLength = file.length();
        try {
            mInputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            try {
                close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void myReset() {
        fillBigBufferCount = 0;
        tempLength1 = 0;
        tempLength2 = 0;

        readBigBufferIndex = 0;
        writeBigBufferIndex = 0;

        smallBufferSize = 0;

        hasSetDataSource = false;
        //Arrays.fill(bigBuffer, (byte) 0);
        //Arrays.fill(smallBuffer, (byte) 0);
    }

    // 在一个线程中不断的运行
    public void readData() {
        Log.i(TAG, "readLocalData() start");
        myReset();

        boolean isStarted = false;
        isEOF = false;
        isReading = true;
        for (; ; ) {
            if (!isReading) {
                break;
            }

            if (smallBufferLength - smallBufferSize > 0) {
                // region 把smallBuffer填充满为止,直到smallBufferLength与smallBufferSize相等

                int readSize = -1;
                try {
                    if (mInputStream != null) {
                        readSize = mInputStream.read(
                                smallBuffer,
                                (int) smallBufferSize,
                                (int) (smallBufferLength - smallBufferSize));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "readLocalData() " + e.getMessage());
                    // 能不能放还需要再看
                    // 异常之后还是可以播放一段时间的
                    if (smallBufferSize > 0) {
                        synchronized (bufferLock) {
                            int destPos =
                                    (int) (writeBigBufferIndex - fillBigBufferCount * bigBufferLength);
                            System.arraycopy(smallBuffer, 0,
                                    bigBuffer, destPos, (int) smallBufferSize);
                            writeBigBufferIndex += smallBufferSize;
                            smallBufferSize = 0;
                        }
                    }
                    isEOF = true;
                    // notify
                    if (!isStarted) {
                        isStarted = true;
                        if (mCallback != null) {
                            mCallback.onStart();
                        }
                    }
                    if (mCallback != null) {
                        mCallback.onError(e.getMessage());
                    }

                    try {
                        close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    break;
                }

                if (readSize < 0) {
                    Log.e(TAG, "readLocalData()" +
                            " readSize: " + readSize +
                            " smallBufferSize: " + smallBufferSize);
                    if (smallBufferSize > 0) {
                        synchronized (bufferLock) {
                            int destPos =
                                    (int) (writeBigBufferIndex - fillBigBufferCount * bigBufferLength);
                            System.arraycopy(smallBuffer, 0,
                                    bigBuffer, destPos, (int) smallBufferSize);
                            writeBigBufferIndex += smallBufferSize;
                            smallBufferSize = 0;
                        }
                    }
                    isEOF = true;
                    // notify
                    if (!isStarted) {
                        isStarted = true;
                        if (mCallback != null) {
                            mCallback.onStart();
                        }
                    }
                    if (mCallback != null) {
                        mCallback.onEnd();
                    }
                    break;
                }
                smallBufferSize += readSize;

                // endregion

                continue;
            }

            long readIndex = readBigBufferIndex + bigBufferLength - max_input_size;
            long writeIndex = writeBigBufferIndex + smallBufferSize;
            if (readIndex < writeIndex) {
                notifyToReadWait();
                continue;
            }

            synchronized (bufferLock) {
                /***
                 destPos:
                 0        5242880
                 10485760 15728640 20971520 26214400 31457280 36700160
                 41943040 47185920 52428800 57671680 62914560 68157440
                 73400320 78643200 83886080 89128960 94371840 99614720
                 */
                long destPos = writeBigBufferIndex - fillBigBufferCount * bigBufferLength;
                /*Log.i(TAG, "readLocalData()" +
                        " readBigBufferIndex: " + readBigBufferIndex +
                        " writeBigBufferIndex: " + writeBigBufferIndex +
                        " fillBigBufferCount: " + fillBigBufferCount +
                        " destPos: " + destPos
                );*/
                System.arraycopy(smallBuffer, 0,
                        bigBuffer, (int) destPos, (int) smallBufferSize);
                writeBigBufferIndex += smallBufferSize;
                smallBufferSize = 0;

                Log.i(TAG, "readLocalData()" +
                        " readBigBufferIndex: " + readBigBufferIndex +
                        " writeBigBufferIndex: " + writeBigBufferIndex +
                        " writeIndex: " + (writeBigBufferIndex - fillBigBufferCount * bigBufferLength) +
                        " fillBigBufferCount: " + fillBigBufferCount +
                        " destPos: " + destPos
                );

                if (writeBigBufferIndex % bigBufferLength == 0) {
                    fillBigBufferCount++;
                    Log.d(TAG, "readLocalData()" +
                            " readBigBufferIndex: " + readBigBufferIndex +
                            " writeBigBufferIndex: " + writeBigBufferIndex +
                            " fillBigBufferCount: " + fillBigBufferCount);
                }

                // notify
                // 可以进行setDataSource(...)操作了
                if (!isStarted) {
                    isStarted = true;
                    if (mCallback != null) {
                        mCallback.onStart();
                    }
                }
            }

        }// for (; ; ) end
        isReading = false;

        try {
            close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.i(TAG, "readLocalData() end");
    }

    // 被readAt(long position, byte[] buffer, int offset, int size)调用
    private int readAt_(long position, byte[] buffer, int offset, int size) {
        /***
         */

        if (position < 0 || buffer == null || offset < 0 || size < 0) {
            Log.e(TAG, "Invalid parameters. position=" + position + " buffer=" +
                    buffer + " offset=" + offset + " size=" + size);
            return INVALID_VALUE;
        }
        /*long contentSize = dtcpIpAccessor.getSize();
        if (contentSize > 0 && position >= contentSize) {
            Log.e(TAG, "Invalid parameters. contentSize=" + contentSize
                    + ", position=" + position);
            return INVALID_VALUE;
        }*/

        if (position > writeBigBufferIndex) {
            Log.e(TAG, "readAt_()" +
                    " position: " + position +
                    " writeBigBufferIndex: " + writeBigBufferIndex);
            return INVALID_VALUE;
        }

        int readSize = 0;
        int availableData = 0;
        int eatBigBufferCount = (int) (position / bigBufferLength);
        tempLength1 = eatBigBufferCount * bigBufferLength;
        tempLength2 = (eatBigBufferCount + 1) * bigBufferLength;
        synchronized (bufferLock) {
            if (position >= tempLength1 && position < tempLength2) {
                if (writeBigBufferIndex < (eatBigBufferCount + 1) * bigBufferLength) {
                    availableData = (int) (writeBigBufferIndex - position);
                    if (availableData >= size) {
                        System.arraycopy(bigBuffer,
                                (int) (position - eatBigBufferCount * bigBufferLength),
                                buffer, offset, size);
                        readSize = size;
                    } else {
                        if (availableData > 0) {
                            System.arraycopy(bigBuffer,
                                    (int) (position - eatBigBufferCount * bigBufferLength),
                                    buffer, offset, availableData);
                            readSize = availableData;
                        } else {
                            Log.i(TAG, "readAt_() 1" +
                                    " position: " + position +
                                    " availableData: " + availableData +
                                    " writeBigBufferIndex: " + writeBigBufferIndex +
                                    " eatBigBufferCount: " + eatBigBufferCount
                            );
                        }
                    }
                } else {
                    availableData = (int) ((eatBigBufferCount + 1) * bigBufferLength - position);
                    if (availableData >= size) {
                        System.arraycopy(bigBuffer,
                                (int) (position - eatBigBufferCount * bigBufferLength),
                                buffer, offset, size);
                        readSize = size;
                    } else {
                        if (availableData > 0) {
                            System.arraycopy(bigBuffer,
                                    (int) (position - eatBigBufferCount * bigBufferLength),
                                    buffer, offset, availableData);
                            readSize = availableData;
                        } else {
                            Log.i(TAG, "readAt_() 2" +
                                    " position: " + position +
                                    " availableData: " + availableData +
                                    " writeBigBufferIndex: " + writeBigBufferIndex +
                                    " eatBigBufferCount: " + eatBigBufferCount
                            );
                        }
                    }
                }

                /*Log.i(TAG, "readAt_()" +
                        " position: " + position +
                        " availableData: " + availableData +
                        " writeBigBufferIndex: " + writeBigBufferIndex +
                        " eatBigBufferCount: " + eatBigBufferCount
                );*/
            } else {
                boolean flag1 = position >= tempLength1;
                boolean flag2 = position < tempLength2;
                Log.i(TAG, "readAt_() 3" +
                        " flag1: " + flag1 +
                        " flag2: " + flag2 +
                        " position: " + position +
                        " tempLength1: " + tempLength1 +
                        " tempLength2: " + tempLength2 +
                        " bigBufferLength: " + bigBufferLength +
                        " eatBigBufferCount: " + eatBigBufferCount +
                        " writeBigBufferIndex: " + writeBigBufferIndex
                );
            }
        }

        if (hasSetDataSource) {
            readBigBufferIndex = position;
            notifyToRead();
        }

        if (readSize <= 0 || size != readSize) {
            Log.i(TAG, "readAt_()" +
                    " isEOF: " + isEOF +
                    " size: " + size +
                    " readSize: " + readSize +
                    " position: " + position +
                    " availableData: " + availableData +
                    " writeBigBufferIndex: " + writeBigBufferIndex +
                    " eatBigBufferCount: " + eatBigBufferCount
            );
            if (isEOF && readSize == 0) {
                return INVALID_VALUE;
            }
        }

        return readSize;
    }

    private void notifyToRead() {
        synchronized (readDataLock) {
            readDataLock.notify();
        }
    }

    private void notifyToReadWait() {
        try {
            synchronized (readDataLock) {
                readDataLock.wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void safeExit() {
        isEOF = true;
        isReading = false;
        notifyToRead();
    }

}

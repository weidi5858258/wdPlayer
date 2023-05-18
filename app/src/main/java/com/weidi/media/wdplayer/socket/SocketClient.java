package com.weidi.media.wdplayer.socket;

import com.weidi.log.MLog;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

/***
 Created by root on 19-7-3.
 */

public class SocketClient {

    private static final String TAG =
            SocketClient.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static volatile SocketClient sSocketClient;

    private Socket mSocket;

    private SocketClient() {

    }

    public static SocketClient getInstance() {
        if (sSocketClient == null) {
            synchronized (SocketClient.class) {
                if (sSocketClient == null) {
                    sSocketClient = new SocketClient();
                }
            }
        }
        return sSocketClient;
    }


    public void connect() {
        if (DEBUG)
            MLog.d(TAG, "connect()");
        try {
            mSocket = new Socket(SocketServer.IP, SocketServer.PORT);
            if (mSocket.isConnected()) {
                if (DEBUG)
                    MLog.d(TAG, "connect() " + mSocket.toString());
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            mSocket = null;
        } catch (SecurityException e) {
            e.printStackTrace();
            mSocket = null;
        } catch (IOException e) {
            e.printStackTrace();
            mSocket = null;
        }
    }

    public void disconnect() {
        if (DEBUG)
            MLog.d(TAG, "disconnect()");
        try {
            if (mSocket != null
                    && mSocket.isConnected()
                    && !mSocket.isClosed()) {
                if (DEBUG)
                    MLog.d(TAG, "disconnect() mSocket.close()");
                mSocket.close();
                mSocket = null;
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Socket getSocket() {
        return mSocket;
    }

}

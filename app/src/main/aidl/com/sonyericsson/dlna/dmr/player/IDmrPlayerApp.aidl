package com.sonyericsson.dlna.dmr.player;

import com.sonyericsson.dlna.dmr.player.IDmrPlayerAppCallback;

/**
 * interface between DLNA APP and DMR Player
 */
interface IDmrPlayerApp {
    /**
     * register callback for DMR Player notifications.
     * @param iid instance ID.
     * @param cb callback for IDmrPlayerCallback interface.
     * @throws RemoteException on remote procedure problems.
     */
    void registerCallback(int iid, IDmrPlayerAppCallback cb);

    /**
     * unregister callback for DMR Player notifications.
     * @param iid instance ID.
     * @param cb callback for IDmrPlayerCallback interface.
     * @throws RemoteException on remote procedure problems.
     */
    void unregisterCallback(int iid, IDmrPlayerAppCallback cb);

    /**
     * setDataSource sets current content URI and metadata.
     * @param iid instance ID.
     * @param uri currentURI.
     * @return err code.
     * @throws RemoteException on remote procedure problems.
     */
    int setDataSource(int iid, String uri);

    /**
     * getDuration gets the duration of the file.
     * @param iid instance ID.
     * @return the duration in milliseconds, if no duration is available
     *         (for example, if streaming live content), -1 is returned.
     *         if this service is wrong state, minus value (-x) may be returned.
     * @throws RemoteException on remote procedure problems.
     */
    int getDuration(int iid);

    /**
     * Stops playback after playback has been stopped or paused.
     * @param iid instance ID.
     * @return err code.
     * @throws RemoteException on remote procedure problems.
     */
    int stop(int iid);

    /**
     * Starts or resumes playback. If playback had previously been paused,
     * playback will continue from where it was paused. If playback had
     * been stopped, or never started before, playback will start at the
     * beginning.
     * @param iid instance ID.
     * @return err code.
     * @throws RemoteException on remote procedure problems.
     */
    int start(int iid);

    /**
     * set playback speed. "1" means the normal speed.
     * "1/2" means the half speed and "2" means the double speed.
     * Negative values indicate reverse playback.
     * @param iid instance ID.
     * @return 0 for success or non zero for false.
     * @throws RemoteException on remote procedure problems.
     */
    int setPlaySpeed(int iid, String speedSpec);

    /**
     * returns the available playback speed spec string.
     * multiple playback speed spec string is separated with "\,".
     * @param iid instance ID.
     * @return playback speed spec string.
     * @throws RemoteException on remote procedure problems.
     */
    String availablePlaySpeed(int iid);

    /**
     * pause the media playback.
     * @param iid instance ID.
     * @return err code.
     * @throws RemoteException on remote procedure problems.
     */
    int pause(int iid);

    /**
     * Seeks to specified time position.
     * @param iid instance ID.
     * @param msec the offset in milliseconds from the start to seek to.
     * @return err code.
     * @throws RemoteException on remote procedure problems.
     */
    int seekTo(int iid, int msec);

    /**
     * getCurrentPosition returns the playback position in milliseconds.
     * @param iid instance ID.
     * @return err code.
     * @throws RemoteException on remote procedure problems.
     */
    int getCurrentPosition(int iid);

    /**
     * setDataSourceMetadata sets current content URI and metadata.
     * @param iid instance ID.
     * @param uri currentURI.
     * @param metadata.
     * @return err code.
     * @throws RemoteException on remote procedure problems.
     */
    int setDataSourceMetadata(int iid, String uri, in Map metadata);

    int onTransact(int iid, int code, inout android.os.Bundle data);
}
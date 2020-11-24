package com.sonyericsson.dlna.dmr.player;

/**
 * notification interface from DMR Player to DLNA APP.
 */
oneway interface IDmrPlayerAppCallback {
    /**
     * notification callback on error while media playback.
     * @param iid instance ID.
     * @param errno error number in integer value.
     * @param msg error message in string.
     */
    void onError(int iid, int errno, String msg);

    /**
     * notification callback on media playback completion event.
     * @param iid instance ID.
     */
    void onCompletion(int iid);

    /**
     * notification callback on media seek completion event.
     * @param iid instance ID.
     */
    void onSeekComplete(int iid);

    /**
     * notification callback on state changed event.
     * @param iid instance ID.
     * @param state media player's updated state.
     */
    void onState(int iid, int state);

    /**
     * notification callback on dmr service unbind.
     */
    void onUnbindDMRServices();
}

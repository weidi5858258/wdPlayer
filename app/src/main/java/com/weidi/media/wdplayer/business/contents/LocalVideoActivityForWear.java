package com.weidi.media.wdplayer.business.contents;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.wearable.activity.WearableActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.weidi.eventbus.Phone;
import com.weidi.log.MLog;
import com.weidi.media.wdplayer.R;
import com.weidi.media.wdplayer.recycler_view.WearableVerticalLayoutManager;
import com.weidi.media.wdplayer.video_player.PlayerService;
import com.weidi.media.wdplayer.video_player.PlayerWrapper;
import com.weidi.utils.MyToast;

import java.util.LinkedHashMap;
import java.util.Map;

import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.widget.WearableRecyclerView;

import static com.weidi.media.wdplayer.Constants.PLAYBACK_ADDRESS;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_USE_EXOPLAYER_OR_FFMPEG;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_USE_PLAYER;
import static com.weidi.media.wdplayer.Constants.PLAYER_FFMPEG_MEDIACODEC;
import static com.weidi.media.wdplayer.Constants.PLAYER_IJKPLAYER;
import static com.weidi.media.wdplayer.Constants.PLAYER_MEDIACODEC;

public class LocalVideoActivityForWear extends WearableActivity {

    private static final String TAG = "ContentsActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(
                com.weidi.library.R.anim.push_left_in,
                com.weidi.library.R.anim.push_left_out);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.contents_layout_wear);

        // Enables Always-on
        setAmbientEnabled();

        internalCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");
        internalResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop()");
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy()");
        internalDestroy();
        super.onDestroy();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(
                com.weidi.library.R.anim.push_right_in,
                com.weidi.library.R.anim.push_right_out);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult() requestCode: " + requestCode +
                " resultCode: " + resultCode +
                " data: " + data);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "onConfigurationChanged()" +
                " newConfig: " + newConfig.toString());

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Phone.call(
                    PlayerService.class.getName(),
                    PlayerService.COMMAND_HANDLE_LANDSCAPE_SCREEN,
                    new Object[]{0});
        } else {
            Phone.call(
                    PlayerService.class.getName(),
                    PlayerService.COMMAND_HANDLE_PORTRAIT_SCREEN,
                    null);
        }
    }

    /////////////////////////////////////////////////////////////////////////

    private EditText mAddressET;
    private WearableRecyclerView mRecyclerView;
    private WearableVerticalLayoutManager mLayoutManager;
    private ContentsAdapterForWear mAdapter;

    private Handler mUiHandler;
    private long contentLength = -1;
    private SharedPreferences mPreferences;
    private int mContentsCount = 0;
    private final LinkedHashMap<String, String> mContentsMap = new LinkedHashMap();
    private static final int ONE_TIME_ADD_COUNT = 20;
    private static final int MSG_ON_CLICK_PLAYBACK_BUTTOM = 1;
    private int mClickCount = 0;

    private void internalCreate(Bundle savedInstanceState) {
        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                LocalVideoActivityForWear.this.uiHandleMessage(msg);
            }
        };

        mRecyclerView = findViewById(R.id.contents_rv);
        mRecyclerView.setClickable(true);
        mRecyclerView.setFocusable(true);
        mRecyclerView.setFocusableInTouchMode(true);
        mRecyclerView.requestFocus();

        findViewById(R.id.address_layout).setVisibility(View.GONE);
        /*mAddressET = findViewById(R.id.address_et);
        findViewById(R.id.playback_btn).setOnClickListener(OnClickListener);
        mPreferences = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        String path = mPreferences.getString(PLAYBACK_ADDRESS, null);
        if (!TextUtils.isEmpty(path) && PlayerWrapper.mLocalVideoContentsMap.containsKey(path)) {
            mAddressET.setText(PlayerWrapper.mLocalVideoContentsMap.get(path));
        }*/

        if (!PlayerWrapper.mLocalVideoContentsMap.isEmpty()) {
            initAdapter();
            mRecyclerView.isEdgeItemsCenteringEnabled();
            mRecyclerView.isCircularScrollingGestureEnabled();
            mRecyclerView.setLayoutManager(mLayoutManager);
            mRecyclerView.setAdapter(mAdapter);
            mRecyclerView.addOnScrollListener(OnScrollListener);
            mLayoutManager.setRecyclerView(mRecyclerView);
            MLog.d(TAG, "initView() PlayerWrapper.mLocalContentsMap.size(): " +
                    PlayerWrapper.mLocalVideoContentsMap.size());

            if (PlayerWrapper.mLocalVideoContentsMap.size() > 500) {
                // 太多的先加载20个
                mContentsMap.clear();
                for (Map.Entry<String, String> tempMap :
                        PlayerWrapper.mLocalVideoContentsMap.entrySet()) {
                    mContentsCount++;
                    mContentsMap.put(tempMap.getKey(), tempMap.getValue());
                    if (mContentsCount == ONE_TIME_ADD_COUNT) {
                        break;
                    }
                }
                mAdapter.addData(mContentsMap);
            } else {
                mAdapter.setData(PlayerWrapper.mLocalVideoContentsMap);
            }
        }
    }

    private void internalResume() {

    }

    private void internalDestroy() {
        mPreferences = null;
        mUiHandler = null;
        mLayoutManager = null;
        mAdapter = null;
        mAddressET = null;
        mRecyclerView = null;
    }

    private void initAdapter() {
        mLayoutManager = new WearableVerticalLayoutManager(getApplicationContext());
        mAdapter = new ContentsAdapterForWear(getApplicationContext());
        mAdapter.setOnItemClickListener(
                new ContentsAdapterForWear.OnItemClickListener() {
                    @Override
                    public void onItemClick(String key, int position, int viewId) {
                        MLog.d(TAG, "onItemClick() videoPlaybackPath: " + key);

                        String videoPlaybackPath = key;
                        String name = PlayerWrapper.mLocalVideoContentsMap.get(key);
                        if (TextUtils.isEmpty(key) || TextUtils.isEmpty(name)) {
                            return;
                        }

                        //mAddressET.setText(name);
                        String type = "video/";
                        /*if (name.endsWith(".mp3")
                                || name.endsWith(".aac")// aac
                                || name.endsWith(".ac3")// ac3
                                || name.endsWith(".flac")
                                || name.endsWith(".wma")// wma7x
                                || name.endsWith(".wav")// pcm_s16le
                                || name.endsWith(".amr")
                                || name.endsWith(".mp2")// vorbis
                                || name.endsWith(".ogg")// vorbis
                                || name.endsWith(".m4a")// mpeg4 aac
                                || name.endsWith(".ape")// pcm_s16be
                                || name.endsWith(".au") // pcm_s16be
                        ) //
                        {
                            type = "audio/";
                        }*/

                        switch (viewId) {
                            case R.id.item_root_layout:
                                Phone.call(
                                        PlayerService.class.getName(),
                                        PlayerService.COMMAND_SHOW_WINDOW,
                                        new Object[]{videoPlaybackPath, type});
                                break;
                            case R.id.item_download_btn:
                                break;
                            default:
                                break;
                        }
                    }
                });
    }

    private void uiHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        switch (msg.what) {
            case MSG_ON_CLICK_PLAYBACK_BUTTOM:
                if (mClickCount > 3) {
                    mClickCount = 3;
                }

                switch (mClickCount) {
                    case 3:
                        mClickCount = 0;
                        finish();
                        return;
                    default:
                        break;
                }

                String videoPlaybackPath = mAddressET.getText().toString().trim();
                if (TextUtils.isEmpty(videoPlaybackPath)) {
                    videoPlaybackPath = mPreferences.getString(PLAYBACK_ADDRESS, null);
                }
                if (TextUtils.isEmpty(videoPlaybackPath)) {
                    mClickCount = 0;
                    return;
                }
                String newPath = videoPlaybackPath.toLowerCase();
                if (!newPath.startsWith("http://")
                        && !newPath.startsWith("https://")
                        && !newPath.startsWith("rtmp://")
                        && !newPath.startsWith("rtsp://")
                        && !newPath.startsWith("/storage/")) {
                    int index = 0;
                    if (PlayerWrapper.mLocalVideoContentsMap.containsValue(videoPlaybackPath)) {
                        for (Map.Entry<String, String> entry :
                                PlayerWrapper.mLocalVideoContentsMap.entrySet()) {
                            index++;
                            if (TextUtils.equals(videoPlaybackPath, entry.getValue())) {
                                videoPlaybackPath = entry.getKey();
                                break;
                            }
                        }
                        MLog.i(TAG, "onClick() index: " + index);

                        switch (mClickCount) {
                            case 1:
                                Phone.call(
                                        PlayerService.class.getName(),
                                        PlayerService.COMMAND_SHOW_WINDOW,
                                        new Object[]{videoPlaybackPath, "video/"});
                                break;
                            case 2:
                                maybeJumpToPosition(String.valueOf(index));
                                break;
                            default:
                                break;
                        }
                    } else {
                        maybeJumpToPosition(videoPlaybackPath);
                    }
                } else {
                    Phone.call(
                            PlayerService.class.getName(),
                            PlayerService.COMMAND_SHOW_WINDOW,
                            new Object[]{videoPlaybackPath, "video/"});
                }
                mClickCount = 0;
                break;
            default:
                break;
        }
    }

    private void maybeJumpToPosition(String jumpToPosition) {
        MLog.i(TAG, "maybeJumpToPosition() jumpToPosition: " + jumpToPosition);
        int position = -1;
        try {
            position = Integer.parseInt(jumpToPosition);
        } catch (NumberFormatException e) {
            numberFormatException(jumpToPosition);
            return;
        }
        MLog.i(TAG, "maybeJumpToPosition()       position: " + position);
        if (position < 0) {
            position = 1;
        } else if (position > PlayerWrapper.mLocalVideoContentsMap.size()) {
            position = PlayerWrapper.mLocalVideoContentsMap.size();
        }

        if (position <= mLayoutManager.getItemCount()) {
            // 跳到position的位置就行了
            if (!mLayoutManager.getVisiblePositions().contains(position - 1)) {
                //mRecyclerView.smoothScrollToPosition(position - 1);
                mLayoutManager.smoothScrollToPosition(position - 1);
            }
        } else {
            // 需要加载更多的数据
            int needToLoadCount = position - mLayoutManager.getItemCount();
            needToLoadCount += ONE_TIME_ADD_COUNT;
            mContentsMap.clear();
            int i = 0;
            int addCount = 0;
            for (Map.Entry<String, String> tempMap :
                    PlayerWrapper.mLocalVideoContentsMap.entrySet()) {
                i++;
                if (i <= mContentsCount) {
                    continue;
                }
                mContentsCount++;
                addCount++;
                mContentsMap.put(tempMap.getKey(), tempMap.getValue());
                if (addCount == needToLoadCount) {
                    break;
                }
            }
            mAdapter.addData(mContentsMap);

            int finalPosition = position;
            mUiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //mRecyclerView.smoothScrollToPosition(finalPosition - 1);
                    mLayoutManager.smoothScrollToPosition(finalPosition - 1);
                }
            }, 500);
        }
    }

    private void numberFormatException(String text) {
        if (text.startsWith("player_fm")) {// player_ffmpeg_mediacodec
            mPreferences.edit().putString(PLAYBACK_USE_PLAYER, PLAYER_FFMPEG_MEDIACODEC).commit();
            mAddressET.setText("");
            MyToast.show(PLAYER_FFMPEG_MEDIACODEC);
        } else if (text.startsWith("player_ijk")) {// player_ijkplayer
            mPreferences.edit().putString(PLAYBACK_USE_PLAYER, PLAYER_IJKPLAYER).commit();
            mAddressET.setText("");
            MyToast.show(PLAYER_IJKPLAYER);
        } else if (text.startsWith("player_mc")) {// player_mediacodec
            mPreferences.edit().putString(PLAYBACK_USE_PLAYER, PLAYER_MEDIACODEC).commit();
            mAddressET.setText("");
            MyToast.show(PLAYER_MEDIACODEC);
        } else if (text.startsWith("use_exo")) {// use_exoplayer
            mPreferences.edit().putString(PLAYBACK_USE_EXOPLAYER_OR_FFMPEG, "use_exoplayer").commit();
            mAddressET.setText("");
            MyToast.show(PLAYER_MEDIACODEC);
        } else if (text.startsWith("use_ff")) {// use_ffmpeg
            mPreferences.edit().putString(PLAYBACK_USE_EXOPLAYER_OR_FFMPEG, "use_ffmpeg").commit();
            mAddressET.setText("");
            MyToast.show(PLAYER_MEDIACODEC);
        }
    }

    private final View.OnClickListener OnClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch (v.getId()) {
                        case R.id.playback_btn:
                            mClickCount++;
                            MyToast.show(String.valueOf(mClickCount));
                            mUiHandler.removeMessages(MSG_ON_CLICK_PLAYBACK_BUTTOM);
                            mUiHandler.sendEmptyMessageDelayed(MSG_ON_CLICK_PLAYBACK_BUTTOM, 1000);
                            break;
                        case R.id.download_tv:
                            break;
                        default:
                            break;
                    }
                }
            };

    private final RecyclerView.OnScrollListener OnScrollListener =
            new RecyclerView.OnScrollListener() {
                //用来标记是否正在向最后一个滑动
                boolean isSlidingToLast = false;

                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    // 当不滚动时
                    switch (newState) {
                        case RecyclerView.SCROLL_STATE_IDLE:
                            int itemCount = mLayoutManager.getItemCount();
                            int visibleItemCount = mLayoutManager.getVisibleItemCount();
                            //int firstVisiblePosition = mLayoutManager.getFirstVisiblePosition();
                            int lastVisiblePosition = mLayoutManager.getLastVisiblePosition();
                            if (isSlidingToLast
                                    && lastVisiblePosition >= itemCount - visibleItemCount
                                    && lastVisiblePosition <= itemCount - 1) {
                                //&& lastVisiblePosition == (mLayoutManager.getItemCount() - 1)) {
                                MLog.d(TAG, "onScrollStateChanged() SCROLL_STATE_IDLE");
                                // 加载更多功能的代码
                                mContentsMap.clear();
                                int i = 0;
                                int addCount = 0;
                                for (Map.Entry<String, String> tempMap :
                                        PlayerWrapper.mLocalVideoContentsMap.entrySet()) {
                                    i++;
                                    if (i <= mContentsCount) {
                                        continue;
                                    }
                                    mContentsCount++;
                                    addCount++;
                                    mContentsMap.put(tempMap.getKey(), tempMap.getValue());
                                    if (addCount == ONE_TIME_ADD_COUNT) {
                                        break;
                                    }
                                }
                                mAdapter.addData(mContentsMap);
                            }
                            break;
                        default:
                            break;
                    }
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    // dx用来判断横向滑动方向,dy用来判断纵向滑动方向
                    // MLog.d(TAG, "onScrolled() dx: " + dx + " dy: " + dy);
                    if (dy > 0) {
                        // 表示手指由下往上滑动(内容往上滚动)
                        isSlidingToLast = true;
                    } else {
                        isSlidingToLast = false;
                    }
                }
            };

}

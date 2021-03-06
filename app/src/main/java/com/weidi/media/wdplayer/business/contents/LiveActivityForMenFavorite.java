package com.weidi.media.wdplayer.business.contents;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.log.MLog;
import com.weidi.media.wdplayer.R;
import com.weidi.media.wdplayer.video_player.PlayerService;
import com.weidi.media.wdplayer.video_player.PlayerWrapper;
import com.weidi.recycler_view.VerticalLayoutManager;
import com.weidi.utils.MyToast;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.weidi.media.wdplayer.Constants.PLAYBACK_ADDRESS;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_USE_EXOPLAYER_OR_FFMPEG;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_USE_PLAYER;
import static com.weidi.media.wdplayer.Constants.PLAYER_FFMPEG_MEDIACODEC;
import static com.weidi.media.wdplayer.Constants.PLAYER_IJKPLAYER;
import static com.weidi.media.wdplayer.Constants.PLAYER_MEDIACODEC;
import static com.weidi.media.wdplayer.Constants.PREFERENCES_NAME;

public class LiveActivityForMenFavorite extends Activity {

    private static final String TAG = "LiveActivityFMF";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(
                com.weidi.library.R.anim.push_left_in,
                com.weidi.library.R.anim.push_left_out);
        super.onCreate(savedInstanceState);
        // Volume change should always affect media volume_normal
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        setContentView(R.layout.contents_layout);

        internalCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart()");
        internalStart();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(TAG, "onRestart()");
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

    /////////////////////////////////////////////////////////////////////////

    private EditText mAddressET;
    private RecyclerView mRecyclerView;

    private Handler mUiHandler;
    private long contentLength = -1;
    private VerticalLayoutManager mLayoutManager;
    private ContentsAdapter mAdapter;
    private SharedPreferences mPreferences;
    private int mContentsCount = 0;
    private final LinkedHashMap<String, String> mMenFavoriteContentsMap = new LinkedHashMap();
    private static final int ONE_TIME_ADD_COUNT = 40;
    private static final int MSG_ON_CLICK_PLAYBACK_BUTTOM = 1;
    private int mClickCount = 0;

    private void internalCreate(Bundle savedInstanceState) {
        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                LiveActivityForMenFavorite.this.uiHandleMessage(msg);
            }
        };

        findViewById(R.id.playback_btn).setOnClickListener(OnClickListener);
        mRecyclerView = findViewById(R.id.contents_rv);
        mAddressET = findViewById(R.id.address_et);

        mPreferences = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        String path = mPreferences.getString(PLAYBACK_ADDRESS, null);
        if (!TextUtils.isEmpty(path) && PlayerWrapper.mMenFavoriteContentsMap.containsKey(path)) {
            mAddressET.setText(PlayerWrapper.mMenFavoriteContentsMap.get(path));
        }

        // item高度固定,进行这样的设置以提高性能
        mRecyclerView.setHasFixedSize(true);
        // 下面三句代码作用:用空间换时间,来提高滚动的流畅性
        mRecyclerView.setItemViewCacheSize(20);
        mRecyclerView.setDrawingCacheEnabled(true);
        mRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        if (!PlayerWrapper.IS_PHONE) {
            findViewById(R.id.address_layout).setVisibility(View.GONE);
            mRecyclerView.setClickable(true);
            mRecyclerView.setFocusable(true);
            mRecyclerView.setFocusableInTouchMode(true);
            mRecyclerView.requestFocus();
        }

        if (!PlayerWrapper.mMenFavoriteContentsMap.isEmpty()) {
            initAdapter();
            mRecyclerView.setLayoutManager(mLayoutManager);
            mLayoutManager.setRecyclerView(mRecyclerView);
            mRecyclerView.setAdapter(mAdapter);
            mRecyclerView.addOnScrollListener(OnScrollListener);
            MLog.d(TAG, "initView() PlayerWrapper.mMenFavoriteContentsMap.size(): " +
                    PlayerWrapper.mMenFavoriteContentsMap.size());

            /*if (PlayerWrapper.mMenFavoriteContentsMap.size() > 100) {
                // 太多的先加载20个
                mMenFavoriteContentsMap.clear();
                for (Map.Entry<String, String> tempMap : PlayerWrapper.mMenFavoriteContentsMap
                .entrySet()) {
                    mContentsCount++;
                    mMenFavoriteContentsMap.put(tempMap.getKey(), tempMap.getValue());
                    if (mContentsCount == ONE_TIME_ADD_COUNT) {
                        break;
                    }
                }
                mAdapter.addData(mMenFavoriteContentsMap);
            } else {
            }*/
            mAdapter.setData(PlayerWrapper.mMenFavoriteContentsMap);
        }
    }

    private void internalStart() {

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
        mLayoutManager = new VerticalLayoutManager(getApplicationContext());
        mAdapter = new ContentsAdapter(getApplicationContext());
        mAdapter.setRecyclerView(mRecyclerView);
        mAdapter.setLayoutManager(mLayoutManager);
        mAdapter.setOnItemClickListener(
                new ContentsAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(String key, int position, int viewId) {
                        MLog.d(TAG, "onItemClick() videoPlaybackPath: " + key);

                        String videoPlaybackPath = key;
                        if (TextUtils.isEmpty(videoPlaybackPath)) {
                            return;
                        }

                        mAddressET.setText(PlayerWrapper.mMenFavoriteContentsMap.get(key));

                        switch (viewId) {
                            case R.id.item_root_layout:
                                EventBusUtils.post(
                                        PlayerService.class,
                                        PlayerService.COMMAND_SHOW_WINDOW,
                                        new Object[]{videoPlaybackPath, "video/"});
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
                    if (PlayerWrapper.mMenFavoriteContentsMap.containsValue(videoPlaybackPath)) {
                        for (Map.Entry<String, String> entry :
                                PlayerWrapper.mMenFavoriteContentsMap.entrySet()) {
                            index++;
                            if (TextUtils.equals(videoPlaybackPath, entry.getValue())) {
                                videoPlaybackPath = entry.getKey();
                                break;
                            }
                        }
                        MLog.i(TAG, "onClick() index: " + index);

                        switch (mClickCount) {
                            case 1:
                                EventBusUtils.post(
                                        PlayerService.class,
                                        PlayerService.COMMAND_SHOW_WINDOW,
                                        new Object[]{videoPlaybackPath, "video/"});
                                break;
                            case 2:
                                maybeJumpToPosition(String.valueOf(index));
                                break;
                            case 3:
                                finish();
                                break;
                            default:
                                break;
                        }
                    } else {
                        maybeJumpToPosition(videoPlaybackPath);
                    }
                } else {
                    EventBusUtils.post(
                            PlayerService.class,
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
        } else if (position > PlayerWrapper.mMenFavoriteContentsMap.size()) {
            position = PlayerWrapper.mMenFavoriteContentsMap.size();
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
            mMenFavoriteContentsMap.clear();
            int i = 0;
            int addCount = 0;
            for (Map.Entry<String, String> tempMap :
                    PlayerWrapper.mMenFavoriteContentsMap.entrySet()) {
                i++;
                if (i <= mContentsCount) {
                    continue;
                }
                mContentsCount++;
                addCount++;
                mMenFavoriteContentsMap.put(tempMap.getKey(), tempMap.getValue());
                if (addCount == needToLoadCount) {
                    break;
                }
            }
            mAdapter.addData(mMenFavoriteContentsMap);

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
                                mMenFavoriteContentsMap.clear();
                                int i = 0;
                                int addCount = 0;
                                for (Map.Entry<String, String> tempMap :
                                        PlayerWrapper.mMenFavoriteContentsMap.entrySet()) {
                                    i++;
                                    if (i <= mContentsCount) {
                                        continue;
                                    }
                                    mContentsCount++;
                                    addCount++;
                                    mMenFavoriteContentsMap.put(tempMap.getKey(),
                                            tempMap.getValue());
                                    if (addCount == ONE_TIME_ADD_COUNT) {
                                        break;
                                    }
                                }
                                mAdapter.addData(mMenFavoriteContentsMap);
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

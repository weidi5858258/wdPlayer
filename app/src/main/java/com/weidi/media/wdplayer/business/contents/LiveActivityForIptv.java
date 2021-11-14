package com.weidi.media.wdplayer.business.contents;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.weidi.eventbus.Phone;
import com.weidi.log.MLog;
import com.weidi.media.wdplayer.R;
import com.weidi.media.wdplayer.util.JniObject;
import com.weidi.media.wdplayer.video_player.FFMPEG;
import com.weidi.media.wdplayer.video_player.PlayerService;
import com.weidi.media.wdplayer.video_player.PlayerWrapper;
import com.weidi.recycler_view.VerticalLayoutManager;
import com.weidi.utils.MyToast;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.weidi.media.wdplayer.Constants.MEDIACODEC_TIME_OUT;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_ADDRESS;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_USE_EXOPLAYER_OR_FFMPEG;
import static com.weidi.media.wdplayer.Constants.PLAYBACK_USE_PLAYER;
import static com.weidi.media.wdplayer.Constants.PLAYER_FFMPEG_MEDIACODEC;
import static com.weidi.media.wdplayer.Constants.PLAYER_FFPLAY;
import static com.weidi.media.wdplayer.Constants.PLAYER_IJKPLAYER;
import static com.weidi.media.wdplayer.Constants.PLAYER_MEDIACODEC;
import static com.weidi.media.wdplayer.Constants.PREFERENCES_NAME;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_setRemainingTime;
import static com.weidi.media.wdplayer.video_player.FFMPEG.DO_SOMETHING_CODE_setTimeDifference;

public class LiveActivityForIptv extends Activity {

    private static final String TAG = "LiveActivity";

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
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    PlayerService.mUseLocalPlayer = true;
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    PlayerService.mUseLocalPlayer = false;
                    break;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    break;
                default:
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "onConfigurationChanged()" +
                " newConfig: " + newConfig.toString());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // 横屏的时候隐藏刘海屏的刘海部分
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
                getWindow().setAttributes(lp);
            } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                // 竖屏的时候展示刘海屏的刘海部分
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                getWindow().setAttributes(lp);
            }
        }

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
    private final LinkedHashMap<String, String> mContentsMap = new LinkedHashMap();
    private static final int ONE_TIME_ADD_COUNT = 40;
    private static final int MSG_ON_CLICK_PLAYBACK_BUTTOM = 1;
    private int mClickCount = 0;

    private void internalCreate(Bundle savedInstanceState) {
        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                LiveActivityForIptv.this.uiHandleMessage(msg);
            }
        };

        findViewById(R.id.playback_btn).setOnClickListener(OnClickListener);
        mRecyclerView = findViewById(R.id.contents_rv);
        mAddressET = findViewById(R.id.address_et);

        mPreferences = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        String path = mPreferences.getString(PLAYBACK_ADDRESS, null);
        if (!TextUtils.isEmpty(path) && PlayerWrapper.mIptvContentsMap.containsKey(path)) {
            mAddressET.setText(PlayerWrapper.mIptvContentsMap.get(path));
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

        if (!PlayerWrapper.mIptvContentsMap.isEmpty()) {
            initAdapter();
            mRecyclerView.setLayoutManager(mLayoutManager);
            mLayoutManager.setRecyclerView(mRecyclerView);
            mRecyclerView.setAdapter(mAdapter);
            mRecyclerView.addOnScrollListener(OnScrollListener);
            MLog.d(TAG, "initView() PlayerWrapper.mIptvContentsMap.size(): " +
                    PlayerWrapper.mIptvContentsMap.size());

            if (PlayerWrapper.mIptvContentsMap.size() > 100) {
                // 太多的先加载20个
                mContentsMap.clear();
                for (Map.Entry<String, String> tempMap :
                        PlayerWrapper.mIptvContentsMap.entrySet()) {
                    mContentsCount++;
                    mContentsMap.put(tempMap.getKey(), tempMap.getValue());
                    if (mContentsCount == ONE_TIME_ADD_COUNT) {
                        break;
                    }
                }
                mAdapter.addData(mContentsMap);
            } else {
                mAdapter.setData(PlayerWrapper.mIptvContentsMap);
            }
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

                        mAddressET.setText(PlayerWrapper.mIptvContentsMap.get(key));

                        switch (viewId) {
                            case R.id.item_root_layout:
                                Phone.call(
                                        PlayerService.class.getName(),
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
                    if (PlayerWrapper.mIptvContentsMap.containsValue(videoPlaybackPath)) {
                        for (Map.Entry<String, String> entry :
                                PlayerWrapper.mIptvContentsMap.entrySet()) {
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
        } else if (position > PlayerWrapper.mIptvContentsMap.size()) {
            position = PlayerWrapper.mIptvContentsMap.size();
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
                    PlayerWrapper.mIptvContentsMap.entrySet()) {
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
        if (text.startsWith("player_ff")) {// player_ffplay
            mPreferences.edit().putString(PLAYBACK_USE_PLAYER, PLAYER_FFPLAY).commit();
            mAddressET.setText("");
            MyToast.show(PLAYER_FFPLAY);
        } else if (text.startsWith("player_fm")) {// player_ffmpeg_mediacodec
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
        } else if (text.startsWith("time_out ")) {// time_out
            String temp[] = text.split(" ");
            if (temp.length >= 2) {
                try {
                    String time_out_str = temp[1];
                    int time_out = Integer.valueOf(time_out_str);
                    mPreferences.edit().putInt(MEDIACODEC_TIME_OUT, time_out).commit();
                    mAddressET.setText("");
                    MyToast.show(MEDIACODEC_TIME_OUT);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        } else if (text.startsWith("time_difference ")) {// time_difference
            String temp[] = text.split(" ");
            if (temp.length >= 2) {
                try {
                    String time_difference_str = temp[1];
                    double time_difference = Double.valueOf(time_difference_str);
                    FFMPEG.getDefault().onTransact(
                            DO_SOMETHING_CODE_setTimeDifference,
                            JniObject.obtain().writeDouble(time_difference));
                    mAddressET.setText("");
                    MyToast.show(time_difference_str);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        } else if (text.startsWith("remaining_time ")) {// remaining_time
            String temp[] = text.split(" ");
            if (temp.length >= 2) {
                try {
                    String remaining_time_str = temp[1];
                    double remaining_time = Double.valueOf(remaining_time_str);
                    FFMPEG.getDefault().onTransact(
                            DO_SOMETHING_CODE_setRemainingTime,
                            JniObject.obtain().writeDouble(remaining_time));
                    mAddressET.setText("");
                    MyToast.show(remaining_time_str);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
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
                                        PlayerWrapper.mIptvContentsMap.entrySet()) {
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

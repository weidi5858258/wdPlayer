package com.weidi.media.wdplayer.business.contents;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.weidi.media.wdplayer.R;
import com.weidi.media.wdplayer.video_player.PlayerWrapper;
import com.weidi.recycler_view.VerticalLayoutManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

/***
 Created by root on 19-4-15.


 // test
 final LinkedHashMap<String, String> mContentsMap = new LinkedHashMap();
 int i = 0;
 for (Map.Entry<String, String> tempMap : PlayerWrapper.mContentsMap.entrySet()) {
 i++;
 if (i < 11) {
 continue;
 }
 testCounts++;
 mContentsMap.put(tempMap.getKey(), tempMap.getValue());
 if (testCounts == 20) {
 break;
 }
 }
 mAdapter.addData(mContentsMap);

 关于RecyclerView你知道的不知道的都在这了（上）
 https://www.cnblogs.com/dasusu/p/9159904.html
 关于RecyclerView你知道的不知道的都在这了（下）
 https://www.cnblogs.com/dasusu/p/9255335.html
 */

public class ContentsAdapter extends RecyclerView.Adapter {

    private static final String TAG = "ContentsAdapter";

    private final LinkedHashMap<String, String> mContentsMap = new LinkedHashMap();
    private final ArrayList<String> mKeys = new ArrayList<String>();
    private Context mContext;
    private LayoutInflater mLayoutInflater;
    private RecyclerView mRecyclerView;
    private VerticalLayoutManager mLayoutManager;

    public ContentsAdapter(Context context) {
        mContentsMap.clear();
        mKeys.clear();
        mContext = context;
        mLayoutInflater = LayoutInflater.from(mContext);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            ViewGroup parent, int viewType) {
        ViewGroup container = null;
        container = (ViewGroup) mLayoutInflater.inflate(
                R.layout.item_content_layout, parent, false);
        return new TitleViewHolder(container);
    }

    @Override
    public void onBindViewHolder(
            RecyclerView.ViewHolder holder, int position) {
        if (mContentsMap.isEmpty() || mKeys.isEmpty()) {
            return;
        }

        TitleViewHolder titleViewHolder = (TitleViewHolder) holder;
        String key = mKeys.get(position);
        String value = mContentsMap.get(key);
        titleViewHolder.title.setText(value);
        titleViewHolder.key = key;
    }

    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        // itemView可见时
        if (PlayerWrapper.IS_PHONE) {
            TitleViewHolder titleViewHolder = (TitleViewHolder) holder;
            if (prePosition != -1 && TextUtils.equals(
                    titleViewHolder.title.getText().toString(),
                    mContentsMap.get(mKeys.get(prePosition)))) {
                titleViewHolder.itemView.setBackground(ContextCompat.getDrawable(
                        mContext, R.drawable.item_selector_focused));
            }
        }
    }

    @Override
    public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
        // itemView不可见时
        if (PlayerWrapper.IS_PHONE) {
            holder.itemView.setBackground(ContextCompat.getDrawable(
                    mContext, R.drawable.item_selector_normal));
        }
    }

    @Override
    public int getItemCount() {
        return mContentsMap.size();
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
    }

    public void setLayoutManager(VerticalLayoutManager layoutManager) {
        mLayoutManager = layoutManager;
    }

    // map的key已经是唯一了
    public void setData(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return;
        }

        synchronized (ContentsAdapter.this) {
            mKeys.clear();
            mContentsMap.clear();
            for (Map.Entry<String, String> tempMap : map.entrySet()) {
                mContentsMap.put(tempMap.getKey(), tempMap.getValue());
                mKeys.add(tempMap.getKey());
            }

            notifyDataSetChanged();
        }
    }

    // 外面的数据做好key的唯一性,且key,value都不为null
    public void addData(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return;
        }

        synchronized (ContentsAdapter.this) {
            boolean dataChange = false;
            for (Map.Entry<String, String> tempMap : map.entrySet()) {
                if (!mKeys.contains(tempMap.getKey())) {
                    dataChange = true;
                    mContentsMap.put(tempMap.getKey(), tempMap.getValue());
                    mKeys.add(tempMap.getKey());
                }
            }

            if (dataChange) {
                notifyDataSetChanged();
            }

            //  java.lang.IllegalArgumentException:
            //  Tmp detached view should be removed from RecyclerView before it can be recycled
            // 此异常的解决方案就是取消Item的动画执行，从代码8中只要设置mItemAnimator =null就可以了，
            // RecyclerView也提供的Api：setItemAnimator()
            // 使用这个有几个Exception
            /*notifyItemRangeChanged(mKeys.size() - ContentsFragment.ONE_TIME_ADD_COUNT,
                    ContentsFragment.ONE_TIME_ADD_COUNT);*/
        }
    }

    public interface OnItemClickListener {
        void onItemClick(String key, int position, int viewId);
    }

    private OnItemClickListener mOnItemClickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    private Button mDownloadBtn;

    public void setProgress(String progress) {
        if (mDownloadBtn != null) {
            mDownloadBtn.setText(progress);
        }
    }

    private int prePosition = -1;
    private int curPosition = -1;

    private class TitleViewHolder extends RecyclerView.ViewHolder {

        private TextView title;
        private Button downloadBtn;
        // 保存了mContentsMap的key
        private String key;

        public TitleViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(onClickListener);
            // 不是移动设备时
            if (!PlayerWrapper.IS_PHONE) {
                // 如果移动设备设置下面值时,第一次点击得到焦点,第二次点击才触发事件
                itemView.setClickable(true);
                itemView.setFocusable(true);
                itemView.setFocusableInTouchMode(true);
                itemView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View view, boolean hasFocus) {
                        Log.i("ContentsAdapter", "view: " + view + " hasFocus: " + hasFocus);

                        if (hasFocus) {
                            focusedView = view;
                            view.setBackground(ContextCompat.getDrawable(view.getContext(),
                                    R.drawable.item_selector_focused));
                        } else {
                            view.setBackground(ContextCompat.getDrawable(view.getContext(),
                                    R.drawable.item_selector_normal));
                        }
                        mUiHandler.removeMessages(0);
                        mUiHandler.sendEmptyMessageDelayed(0, 500);
                    }
                });
            }

            title = itemView.findViewById(R.id.content_title);
            downloadBtn = itemView.findViewById(R.id.item_download_btn);
            downloadBtn.setOnClickListener(onClickListener);
        }

        private View.OnClickListener onClickListener =
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mOnItemClickListener != null) {
                            int position = mKeys.indexOf(key);
                            switch (view.getId()) {
                                case R.id.item_root_layout:
                                    //mRecyclerView.findViewHolderForLayoutPosition(prePosition);
                                    if (PlayerWrapper.IS_PHONE) {
                                        // 这样是不行的
                                        // View v = mRecyclerView.getChildAt(prePosition);

                                        if (mRecyclerView != null && prePosition != -1) {
                                            RecyclerView.ViewHolder holder =
                                                    mRecyclerView.findViewHolderForAdapterPosition(prePosition);
                                            if (holder != null && holder.itemView != null) {
                                                holder.itemView.setBackground(ContextCompat.getDrawable(
                                                        view.getContext(),
                                                        R.drawable.item_selector_normal));
                                            }
                                        }

                                        view.setBackground(ContextCompat.getDrawable(
                                                view.getContext(),
                                                R.drawable.item_selector_focused));
                                        prePosition = position;
                                    }

                                    mOnItemClickListener.onItemClick(
                                            key, position, R.id.item_root_layout);
                                    break;
                                case R.id.item_download_btn:
                                    mOnItemClickListener.onItemClick(
                                            key, position, R.id.item_download_btn);
                                    mDownloadBtn = downloadBtn;
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                };

    }

    private View focusedView;
    private Handler mUiHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            //super.handleMessage(msg);
            if (msg == null) {
                return;
            }

            switch (msg.what) {
                case 0:
                    if (focusedView != null) {
                        focusedView.setBackground(ContextCompat.getDrawable(
                                focusedView.getContext(),
                                R.drawable.item_selector_focused));
                    }
                    break;
                case 1:
                    break;
                default:
                    break;
            }
        }
    };

}

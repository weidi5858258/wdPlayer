package com.weidi.media.wdplayer.recycler_view;

import android.content.Context;
import android.graphics.Rect;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import com.weidi.log.MLog;

import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.widget.WearableLinearLayoutManager;

/***
 以后自定义LayoutManager时,按照这个模板写
 只支持itemView的大小都一样的情况
 然后刚开始就把所有的View都layout出来了

 执行过程:
 1.如果一次性加载完所有Item,那么刚开始时只会调用onLayoutChildren.
 2.如果是加载多次内容的情况.那么刚开始时调用onLayoutChildren,然后手指移动到最后加载更多,
 就会先调用scrollVerticallyBy或者scrollHorizontallyBy,接着再调用onLayoutChildren.

 我的情况是:
 第一次加载20个Item,调用onLayoutChildren.
 然后移动到最后一个Item时,再加载20个Item,然后再调用onLayoutChildren.
 手指移动时会调用scrollVerticallyBy.
 再次加载内容时,等到加载完后,会自动滚动到顶部.
 所以我在onLayoutChildren方法里加了scrollVerticallyBy(1, recycler, state);
 这样就看起来就像是加载完内容停在原来位置,没有滚动到顶部.
 */

public class WearableVerticalLayoutManager extends WearableLinearLayoutManager {

    private static final String TAG = "player_alexander VerticalLayoutManager";

    private RecyclerView mRecyclerView;

    private RecyclerView.Recycler mRecycler;

    private RecyclerView.State mState;

    // RecyclerView.HORIZONTAL = 0 水平
    // RecyclerView.VERTICAL   = 1 竖直
    private int mOrientation;

    private int mItemWidth, mItemHeight;
    // 两个itemView之间的间距
    private int mItemSpace = 16;
    // 所有itemView的总高度.如果存在mItemSpace不为0的话,再加上(getItemCount()-1)个这样的高度
    private int mAllItemsTotalHeight;
    // 这是一个累积的结果,比如不断从下往上滑动,这个过程滑动了多少距离
    private int mScrollVerticallyOffset = 0;
    // RecyclerView的可用高度
    private int mRvUsableHeight = 0;
    // 手指允许滑动的距离.
    // 比方说mItemsTotalHeight总高度1280像素,在屏幕中已经显示了280像素,那么用户还能够滚动1000像素就到达底部了
    private int mAllowScrollVerticallyOffset = 0;
    // 保存每个itemView四个点的坐标
    private SparseArray<Rect> mAllItemsRect = new SparseArray<Rect>();

    // 存放可见View的position,有了这个集合,就能马上知道第一个和最后一个可见View的position
    private ArrayList<Integer> mItemsVisiblePositionList = new ArrayList<Integer>();

    public WearableVerticalLayoutManager(Context context) {
        super(context);
        mItemSpace = 16;
        mOrientation = RecyclerView.VERTICAL;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        // 作用于itemView
        return new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    /***
     摆放子布局
     item数量少时可能只调用两次
     */
    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        // 父类没干活(没有意义的代码不用让它执行)
        // super.onLayoutChildren(recycler, state);

        MLog.i(TAG, "onLayoutChildren() start");
        onLayoutChildrenImpl(recycler, state);
        MLog.i(TAG, "onLayoutChildren() end");
    }

    @Override
    public boolean canScrollVertically() {
        // return super.canScrollHorizontally();
        return mOrientation == RecyclerView.VERTICAL;
    }

    @Override
    public boolean canScrollHorizontally() {
        // return super.canScrollHorizontally();
        return mOrientation == RecyclerView.HORIZONTAL;
    }

    @Override
    public int scrollVerticallyBy(int dy,
                                  RecyclerView.Recycler recycler,
                                  RecyclerView.State state) {
        return scrollVerticallyByImpl(dy, recycler, state);
    }

    @Override
    public int scrollHorizontallyBy(int dx,
                                    RecyclerView.Recycler recycler,
                                    RecyclerView.State state) {
        return scrollHorizontallyByImpl(dx, recycler, state);
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
    }

    public void setItemSpace(int itemSpace) {
        mItemSpace = itemSpace;
    }

    public int getFirstVisiblePosition() {
        if (mItemsVisiblePositionList.isEmpty()) {
            return -1;
        }
        return mItemsVisiblePositionList.get(0);
    }

    public int getLastVisiblePosition() {
        if (mItemsVisiblePositionList.isEmpty()) {
            return -1;
        }
        return mItemsVisiblePositionList.get(mItemsVisiblePositionList.size() - 1);
    }

    public int getVisibleItemCount() {
        if (mItemsVisiblePositionList.isEmpty()) {
            return 0;
        }
        return mItemsVisiblePositionList.size();
    }

    public List<Integer> getVisiblePositions() {
        return mItemsVisiblePositionList;
    }

    // position的位置从0开始(第0个位置就是第1个Item)
    public void smoothScrollToPosition(final int position) {
        if (position < 0
                || position >= getItemCount()
                || mItemsVisiblePositionList.isEmpty()) {
            return;
        }

        MLog.d(TAG, "smoothScrollToPosition()   position: " + position);
        if (mItemsVisiblePositionList.contains(position)) {

        } else {
            if (position < getFirstVisiblePosition()) {
                // 向下滚动
                int dy1 = getFirstVisiblePosition() * (mItemHeight + mItemSpace);
                int dy2 = mScrollVerticallyOffset - dy1;
                int dy = (getFirstVisiblePosition() - position) * (mItemHeight + mItemSpace);
                dy += dy2;
                scrollVerticallyBy(-dy, mRecycler, mState);
            } else if (position > getLastVisiblePosition()) {
                // 向上滚动
                final Rect wantedRect = mAllItemsRect.get(position);
                if (wantedRect == null) {
                    return;
                }

                int dy = wantedRect.bottom - getHeight() - mScrollVerticallyOffset;
                scrollVerticallyBy(dy, mRecycler, mState);
                // 下面的方式比较好理解
                /*scrollVerticallyBy(-mScrollVerticallyOffset, mRecycler, mState);
                int dy = wantedRect.bottom - getHeight();
                scrollVerticallyBy(dy, mRecycler, mState);*/
            }
        }
    }

    /////////////////////////////////////////////////////////////////////

    private void onLayoutChildrenImpl(RecyclerView.Recycler recycler, RecyclerView.State state) {
        mRecycler = recycler;
        mState = state;

        // state.getItemCount() = getItemCount()
        // 第一次被调用时196 0
        // 第二次被调用时196 196
        // 第三次被调用时196 196
        // 第四次被调用时196 6
        MLog.d(TAG, "onLayoutChildrenImpl()" +
                // 总的个数
                " getItemCount: " + getItemCount() +
                // 可见的个数
                " getChildCount: " + getChildCount());
        // RecyclerView的可见宽高
        // 第一次被调用时width: 0    height: 0
        // 第二次被调用时width: 850  height: 0
        // 第三次被调用时width: 1189 height: 582
        // 第四次被调用时width: 1189 height: 582
        MLog.d(TAG, "onLayoutChildrenImpl() width: " + getWidth() + " height: " + getHeight());

        if (getItemCount() == 0) {
            detachAndScrapAttachedViews(recycler);
            MLog.d(TAG, "onLayoutChildrenImpl() detachAndScrapAttachedViews return");
            return;
        }
        if (getChildCount() == 0 && state.isPreLayout()) {
            detachAndScrapAttachedViews(recycler);
            // state.isPreLayout()是支持动画的
            MLog.d(TAG, "onLayoutChildrenImpl() return");
            return;
        }

        /***
         先把所有的View先从RecyclerView中detach掉,然后标记为"Scrap"状态,
         表示这些View处于可被重用状态(非显示中)
         */
        detachAndScrapAttachedViews(recycler);

        Rect visibleRect = null;
        boolean isEmpty = false;
        if (mItemsVisiblePositionList.isEmpty()) {
            isEmpty = true;
            mItemsVisiblePositionList.clear();
            visibleRect = new Rect(0, 0, getWidth(), getHeight());
        }

        int heightOffset = 0;
        int itemCount = getItemCount();
        // 针对每一个itemView进行布局
        for (int i = 0; i < itemCount; i++) {
            // 下面三名代码为固定步骤
            View itemView = recycler.getViewForPosition(i);
            // 因为进行了detach操作,所以现在要重新添加
            addView(itemView);
            // 通知测量itemView
            measureChildWithMargins(itemView, 0, 0);

            // 得到itemView的宽高
            mItemWidth = getDecoratedMeasuredWidth(itemView);
            // 一般在layout中会设置一定的高度
            mItemHeight = getDecoratedMeasuredHeight(itemView);
            /*MLog.d(TAG, "onLayoutChildren() itemWidth: " + itemWidth +
                    " itemHeight: " + itemHeight);*/

            /***
             每一个itemView针对RecyclerView的可用宽高进行设置
             left  : 当前itemView距离RecyclerView可用宽度的左边多少距离
             top   :
             right : 当前itemView距离RecyclerView可用宽度的右边多少距离
             bottom:
             left与right设置不好的话,itemView中某些内容可能显示不了
             */
            /*int left = 100;
            int top = heightOffset;
            int right = itemWidth - 100;
            int bottom = heightOffset + itemHeight;
            // 布局
            // layoutDecorated(itemView, left, top, right, bottom);
            layoutDecoratedWithMargins(itemView, left, top, right, bottom);*/

            Rect itemViewRect = mAllItemsRect.get(i);
            if (itemViewRect == null) {
                itemViewRect = new Rect();
            }
            // 每个item的坐标点
            itemViewRect.set(10, heightOffset, mItemWidth - 10, heightOffset + mItemHeight);
            mAllItemsRect.put(i, itemViewRect);
            layoutDecorated(itemView,
                    itemViewRect.left, itemViewRect.top, itemViewRect.right, itemViewRect.bottom);

            if (isEmpty) {
                // itemView在可见范围内
                if (Rect.intersects(visibleRect, itemViewRect)) {
                /*layoutDecorated(itemView,
                        itemViewRect.left, itemViewRect.top,
                        itemViewRect.right, itemViewRect.bottom);*/
                    mItemsVisiblePositionList.add(i);
                    //MLog.d(TAG, "onLayoutChildrenImpl() i: " + i);
                }
            }

            // 使得最后一个itemView的下面没有mItemSpace大小的间距
            if (itemCount > 1 && i < itemCount - 1) {
                heightOffset += mItemHeight + mItemSpace;
            } else {
                // 只有一条内容时itemView下面不要有间距了,这样不好看
                heightOffset += mItemHeight;
            }

            // 旋转itemView
            // itemView.setRotation(45f);
        }// for (...) end

        mAllItemsTotalHeight = heightOffset;
        mRvUsableHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        mAllowScrollVerticallyOffset = mAllItemsTotalHeight - mRvUsableHeight;
        MLog.d(TAG, "onLayoutChildrenImpl()\n" +
                " mAllItemsTotalHeight: " + mAllItemsTotalHeight +
                " mRvUsableHeight: " + mRvUsableHeight +
                " mAllowScrollVerticallyOffset: " + mAllowScrollVerticallyOffset +
                "\n getPaddingStart: " + getPaddingStart() +
                " getPaddingEnd: " + getPaddingEnd() +
                " getPaddingLeft: " + getPaddingLeft() +
                " getPaddingTop: " + getPaddingTop() +
                " getPaddingRight: " + getPaddingRight() +
                " getPaddingBottom: " + getPaddingBottom());

        scrollVerticallyBy(-1, recycler, state);

        StringBuilder sb = new StringBuilder();
        for (Integer position : mItemsVisiblePositionList) {
            sb.append(position);
            sb.append(" ");
        }
        MLog.d(TAG, "onLayoutChildrenImpl() visiblePosition: " + sb.toString());

        /*itemCount = getItemCount();
        for (int i = 0; i < itemCount; i++) {
            Rect itemViewRect = mAllItemsRect.get(i);
            MLog.d(TAG, "onLayoutChildrenImpl() itemViewRect: " + itemViewRect.toString());
        }*/
    }

    /***
     如果没有边界限制的话,可以进行无限滑动,也就是能够把所有的itemView移出手机屏幕十万八千里
     */
    private int scrollVerticallyByImpl(int dy,
                                       RecyclerView.Recycler recycler,
                                       RecyclerView.State state) {
        // MLog.d(TAG, "scrollVerticallyByImpl() dy: " + dy);
        // super.scrollVerticallyBy(dy, recycler, state);

        if (mScrollVerticallyOffset + dy < 0) {
            // MLog.d(TAG, "scrollVerticallyByImpl() 抵达上边界");
            /***
             dy < 0
             怎么理解?
             刚显示列表内容时,mScrollVerticallyOffset为0,
             手指往下滑动时dy < 0,由于列表已经到达上边界,
             因此当前条件满足,走到这里面.如果offsetChildrenVertical(int)这个方法的参数值不为0,
             那么此时列表内容跟RecyclerView的距离差开了dy,这样显然不行.因此此时应该把dy修正为0,
             这样抵达上边界后列表内容才不会继续往下滚动.
             */
            dy = -mScrollVerticallyOffset;
        } else if (mAllItemsTotalHeight > mRvUsableHeight
                && mScrollVerticallyOffset + dy > mAllowScrollVerticallyOffset) {
            // MLog.d(TAG, "scrollVerticallyByImpl() 抵达下边界");
            /***
             dy > 0
             */
            dy = mAllowScrollVerticallyOffset - mScrollVerticallyOffset;
        } else {
            // 手指从下往上滑动时,dy为正,列表内容不断显示下面部分,手指越往上滑动,列表内容越接近RecyclerView的下边界
            // 手指从上往下滑动时,dy为负,列表内容不断显示上面部分,手指越往下滑动,列表内容越接近RecyclerView的上边界
        }

        mScrollVerticallyOffset += dy;
        /*MLog.d(TAG, "scrollVerticallyByImpl() dy: " + dy +
                " mScrollVerticallyOffset: " + mScrollVerticallyOffset);*/

        // 平移容器内的itemView
        offsetChildrenVertical(-dy);

        // recycle view
        handleRecycle(recycler, state);

        return dy;
    }

    private int scrollHorizontallyByImpl(int dx,
                                         RecyclerView.Recycler recycler,
                                         RecyclerView.State state) {
        // super.scrollHorizontallyBy(dx, recycler, state);
        return 0;
    }

    private void handleRecycle(RecyclerView.Recycler recycler,
                               RecyclerView.State state) {
        detachAndScrapAttachedViews(recycler);

        // 可见范围的一个坐标
        Rect visibleRect = new Rect(
                0,
                mScrollVerticallyOffset,
                getWidth(),
                mScrollVerticallyOffset + getHeight());

        // 将滑出屏幕的view进行回收
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            // 某个坐标与可见范围的坐标如果没有交叉点,那么remove
            if (!Rect.intersects(visibleRect, mAllItemsRect.get(i))) {
                // removeAndRecycleView(getChildAt(i), recycler);
                removeAndRecycleViewAt(i, recycler);
                MLog.d(TAG, "handleRecycle() removeView i: " + i);
            }
        }

        mItemsVisiblePositionList.clear();

        // 在可见区域出现的ItemView重新进行layout
        int itemCount = getItemCount();
        for (int i = 0; i < itemCount; i++) {
            Rect itemViewRect = mAllItemsRect.get(i);
            // 某个坐标与可见范围的坐标如果有交叉点,那么add
            if (Rect.intersects(visibleRect, itemViewRect)) {
                View scrap = recycler.getViewForPosition(i);
                addView(scrap);
                measureChildWithMargins(scrap, 0, 0);

                layoutDecorated(scrap,
                        itemViewRect.left,
                        itemViewRect.top - mScrollVerticallyOffset,
                        itemViewRect.right,
                        itemViewRect.bottom - mScrollVerticallyOffset);

                mItemsVisiblePositionList.add(i);
            }
        }

        /*StringBuilder sb = new StringBuilder();
        for (Integer position : mItemsVisiblePositionList) {
            sb.append(position);
            sb.append(" ");
        }
        MLog.d(TAG, "handleRecycle()        position: " + sb.toString());*/
    }

}

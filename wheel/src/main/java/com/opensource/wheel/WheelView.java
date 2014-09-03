/*
 *  Android Wheel Control.
 *  https://code.google.com/p/android-wheel/
 * 
 *  Copyright 2011 Yuri Kanivets
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.opensource.wheel;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;

import com.opensource.wheel.adapter.WheelViewAdapter;

import java.util.LinkedList;
import java.util.List;

/**
 * Numeric wheel view.
 * 
 * @author Yuri Kanivets
 */
public class WheelView extends View {

	/** Top and bottom shadows colors */
	private int[] SHADOWS_COLORS = new int[] { 0xFF111111,
			0x00AAAAAA, 0x00AAAAAA };

	/** Top and bottom items offset (to hide that) */
	private static final int ITEM_OFFSET_PERCENT = 0;

	/** Left and right padding value */
	private static final int PADDING = 10;

	/** Default count of visible items */
	private static final int DEF_VISIBLE_ITEMS = 5;

	// Wheel Values
	private int mCurrentItem = 0;

	// Count of visible items
	private int mVisibleItems = DEF_VISIBLE_ITEMS;

	// Item height
	private int mItemHeight = 0;

	// Center Line
	private Drawable mCenterDrawable;

	// Wheel drawables
	private int mWheelBackground = R.drawable.bg_wheel;
	private int mWheelForeground = R.drawable.bg_wheel_selected;

	// Shadows drawables
	private GradientDrawable mTopShadow;
	private GradientDrawable mBottomShadow;

	// Draw Shadows
	private boolean mDrawShadows = false;

	// Scrolling
	private WheelScroller mScroller;
	private boolean mIsScrollingPerformed;
	private int mScrollingOffset;

	// Cyclic
	boolean mIsCyclic = false;

	// Items layout
	private LinearLayout mItemsLayout;

	// The number of first item in layout
	private int mFirstItem;

	// View adapter
	private WheelViewAdapter mViewAdapter;

	// Recycle
	private WheelRecycle mRecycle = new WheelRecycle(this);

	// Listeners
	private List<OnWheelChangedListener> mChangingListeners = new LinkedList<OnWheelChangedListener>();
	private List<OnWheelScrollListener> mScrollingListeners = new LinkedList<OnWheelScrollListener>();
	private List<OnWheelClickedListener> mClickingListeners = new LinkedList<OnWheelClickedListener>();

	/**
	 * Constructor
	 */
	public WheelView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initData(context);
	}

	/**
	 * Constructor
	 */
	public WheelView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initData(context);
	}

	/**
	 * Constructor
	 */
	public WheelView(Context context) {
		super(context);
		initData(context);
	}

	/**
	 * Initializes class data
	 * @param context the context
	 */
	private void initData(Context context) {
		mScroller = new WheelScroller(getContext(), scrollingListener);
	}

	// Scrolling listener
	WheelScroller.ScrollingListener scrollingListener = new WheelScroller.ScrollingListener() {
		@Override
		public void onStarted() {
			mIsScrollingPerformed = true;
			notifyScrollingListenersAboutStart();
		}

		@Override
		public void onScroll(int distance) {
			doScroll(distance);

			int height = getHeight();
			if (mScrollingOffset > height) {
				mScrollingOffset = height;
				mScroller.stopScrolling();
			} else if (mScrollingOffset < -height) {
				mScrollingOffset = -height;
				mScroller.stopScrolling();
			}
		}

		@Override
		public void onFinished() {
			if (mIsScrollingPerformed) {
				notifyScrollingListenersAboutEnd();
				mIsScrollingPerformed = false;
			}

			mScrollingOffset = 0;
			invalidate();
		}

		@Override
		public void onJustify() {
			if (Math.abs(mScrollingOffset) > WheelScroller.MIN_DELTA_FOR_SCROLLING) {
				mScroller.scroll(mScrollingOffset, 0);
			}
		}
	};

	/**
	 * Set the the specified scrolling interpolator
	 * @param interpolator the interpolator
	 */
	public void setInterpolator(Interpolator interpolator) {
		mScroller.setInterpolator(interpolator);
	}

	/**
	 * Gets count of visible items
	 * 
	 * @return the count of visible items
	 */
	public int getVisibleItems() {
		return mVisibleItems;
	}

	/**
	 * Sets the desired count of visible items.
	 * Actual amount of visible items depends on wheel layout parameters.
	 * To apply changes and rebuild view call measure().
	 * 
	 * @param count the desired count for visible items
	 */
	public void setVisibleItems(int count) {
		mVisibleItems = count;
	}

	/**
	 * Gets view adapter
	 * @return the view adapter
	 */
	public WheelViewAdapter getViewAdapter() {
		return mViewAdapter;
	}

	// Adapter listener
	private DataSetObserver dataObserver = new DataSetObserver() {
		@Override
		public void onChanged() {
			invalidateWheel(false);
		}

		@Override
		public void onInvalidated() {
			invalidateWheel(true);
		}
	};

	/**
	 * Sets view adapter. Usually new adapters contain different views, so
	 * it needs to rebuild view by calling measure().
	 * 
	 * @param viewAdapter the view adapter
	 */
	public void setViewAdapter(WheelViewAdapter viewAdapter) {
		if (this.mViewAdapter != null) {
			this.mViewAdapter.unregisterDataSetObserver(dataObserver);
		}
		this.mViewAdapter = viewAdapter;
		if (this.mViewAdapter != null) {
			this.mViewAdapter.registerDataSetObserver(dataObserver);
		}

		invalidateWheel(true);
	}

	/**
	 * Adds wheel changing listener
	 * @param listener the listener
	 */
	public void addChangingListener(OnWheelChangedListener listener) {
		mChangingListeners.add(listener);
	}

	/**
	 * Removes wheel changing listener
	 * @param listener the listener
	 */
	public void removeChangingListener(OnWheelChangedListener listener) {
		mChangingListeners.remove(listener);
	}

	/**
	 * Notifies changing listeners
	 * @param oldValue the old wheel value
	 * @param newValue the new wheel value
	 */
	protected void notifyChangingListeners(int oldValue, int newValue) {
		for (OnWheelChangedListener listener : mChangingListeners) {
			listener.onChanged(this, oldValue, newValue);
		}
	}

	/**
	 * Adds wheel scrolling listener
	 * @param listener the listener
	 */
	public void addScrollingListener(OnWheelScrollListener listener) {
		mScrollingListeners.add(listener);
	}

	/**
	 * Removes wheel scrolling listener
	 * @param listener the listener
	 */
	public void removeScrollingListener(OnWheelScrollListener listener) {
		mScrollingListeners.remove(listener);
	}

	/**
	 * Notifies listeners about starting scrolling
	 */
	protected void notifyScrollingListenersAboutStart() {
		for (OnWheelScrollListener listener : mScrollingListeners) {
			listener.onScrollingStarted(this);
		}
	}

	/**
	 * Notifies listeners about ending scrolling
	 */
	protected void notifyScrollingListenersAboutEnd() {
		for (OnWheelScrollListener listener : mScrollingListeners) {
			listener.onScrollingFinished(this);
		}
	}

	/**
	 * Adds wheel clicking listener
	 * @param listener the listener
	 */
	public void addClickingListener(OnWheelClickedListener listener) {
		mClickingListeners.add(listener);
	}

	/**
	 * Removes wheel clicking listener
	 * @param listener the listener
	 */
	public void removeClickingListener(OnWheelClickedListener listener) {
		mClickingListeners.remove(listener);
	}

	/**
	 * Notifies listeners about clicking
	 */
	protected void notifyClickListenersAboutClick(int item) {
		for (OnWheelClickedListener listener : mClickingListeners) {
			listener.onItemClicked(this, item);
		}
	}

	/**
	 * Gets current value
	 * 
	 * @return the current value
	 */
	public int getCurrentItem() {
		return mCurrentItem;
	}

	/**
	 * Sets the current item. Does nothing when index is wrong.
	 * 
	 * @param index the item index
	 * @param animated the animation flag
	 */
	public void setCurrentItem(int index, boolean animated) {
		if (mViewAdapter == null || mViewAdapter.getItemsCount() == 0) {
			return; // throw?
		}

		int itemCount = mViewAdapter.getItemsCount();
		if (index < 0 || index >= itemCount) {
			if (mIsCyclic) {
				while (index < 0) {
					index += itemCount;
				}
				index %= itemCount;
			} else{
				return; // throw?
			}
		}
		if (index != mCurrentItem) {
			if (animated) {
				int itemsToScroll = index - mCurrentItem;
				if (mIsCyclic) {
					int scroll = itemCount + Math.min(index, mCurrentItem) - Math.max(index, mCurrentItem);
					if (scroll < Math.abs(itemsToScroll)) {
						itemsToScroll = itemsToScroll < 0 ? scroll : -scroll;
					}
				}
				scroll(itemsToScroll, 0);
			} else {
				mScrollingOffset = 0;

				int old = mCurrentItem;
				mCurrentItem = index;

				notifyChangingListeners(old, mCurrentItem);

				invalidate();
			}
		}
	}

	/**
	 * Sets the current item w/o animation. Does nothing when index is wrong.
	 * 
	 * @param index the item index
	 */
	public void setCurrentItem(int index) {
		setCurrentItem(index, false);
	}

	/**
	 * Tests if wheel is cyclic. That means before the 1st item there is shown the last one
	 * @return true if wheel is cyclic
	 */
	public boolean isCyclic() {
		return mIsCyclic;
	}

	/**
	 * Set wheel cyclic flag
	 * @param isCyclic the flag to set
	 */
	public void setCyclic(boolean isCyclic) {
		this.mIsCyclic = isCyclic;
		invalidateWheel(false);
	}

	/**
	 * Determine whether shadows are drawn
	 * @return true is shadows are drawn
	 */
	public boolean drawShadows() {
		return mDrawShadows;
	}

	/**
	 * Set whether shadows should be drawn
	 * @param drawShadows flag as true or false
	 */
	public void setDrawShadows(boolean drawShadows) {
		this.mDrawShadows = drawShadows;
	}

	/**
	 * Set the shadow gradient color
	 * @param start
	 * @param middle
	 * @param end
	 */
	public void setShadowColor(int start, int middle, int end) {
		SHADOWS_COLORS = new int[] {start, middle, end};
	}

	/**
	 * Sets the drawable for the wheel background
	 * @param resource
	 */
	public void setWheelBackground(int resource) {
		mWheelBackground = resource;
		setBackgroundResource(mWheelBackground);
	}

	/**
	 * Sets the drawable for the wheel foreground
	 * @param resource
	 */
	public void setWheelForeground(int resource) {
		mWheelForeground = resource;
		mCenterDrawable = getContext().getResources().getDrawable(mWheelForeground);
	}

	/**
	 * Invalidates wheel
	 * @param clearCaches if true then cached views will be clear
	 */
	public void invalidateWheel(boolean clearCaches) {
		if (clearCaches) {
			mRecycle.clearAll();
			if (mItemsLayout != null) {
				mItemsLayout.removeAllViews();
			}
			mScrollingOffset = 0;
		} else if (mItemsLayout != null) {
			// cache all items
			mRecycle.recycleItems(mItemsLayout, mFirstItem, new ItemsRange());
		}

		invalidate();
	}

	/**
	 * Initializes resources
	 */
	private void initResourcesIfNecessary() {
		if (mCenterDrawable == null) {
			mCenterDrawable = getContext().getResources().getDrawable(mWheelForeground);
		}

		if (mTopShadow == null) {
			mTopShadow = new GradientDrawable(Orientation.TOP_BOTTOM, SHADOWS_COLORS);
		}

		if (mBottomShadow == null) {
			mBottomShadow = new GradientDrawable(Orientation.BOTTOM_TOP, SHADOWS_COLORS);
		}

		setBackgroundResource(mWheelBackground);
	}

	/**
	 * Calculates desired height for layout
	 * 
	 * @param layout
	 *            the source layout
	 * @return the desired layout height
	 */
	private int getDesiredHeight(LinearLayout layout) {
		if (layout != null && layout.getChildAt(0) != null) {
			mItemHeight = layout.getChildAt(0).getMeasuredHeight();
		}

		int desired = mItemHeight * mVisibleItems - mItemHeight * ITEM_OFFSET_PERCENT / 50;

		return Math.max(desired, getSuggestedMinimumHeight());
	}

	/**
	 * Returns height of wheel item
	 * @return the item height
	 */
	private int getItemHeight() {
		if (mItemHeight != 0) {
			return mItemHeight;
		}

		if (mItemsLayout != null && mItemsLayout.getChildAt(0) != null) {
			mItemHeight = mItemsLayout.getChildAt(0).getHeight();
			return mItemHeight;
		}

		return getHeight() / mVisibleItems;
	}

	/**
	 * Calculates control width and creates text layouts
	 * @param widthSize the input layout width
	 * @param mode the layout mode
	 * @return the calculated control width
	 */
	private int calculateLayoutWidth(int widthSize, int mode) {
		initResourcesIfNecessary();

		// TODO: make it static
		mItemsLayout.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		mItemsLayout.measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.UNSPECIFIED),
				MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
		int width = mItemsLayout.getMeasuredWidth();

		if (mode == MeasureSpec.EXACTLY) {
			width = widthSize;
		} else {
			width += 2 * PADDING;

			// Check against our minimum width
			width = Math.max(width, getSuggestedMinimumWidth());

			if (mode == MeasureSpec.AT_MOST && widthSize < width) {
				width = widthSize;
			}
		}

		mItemsLayout.measure(MeasureSpec.makeMeasureSpec(width - 2 * PADDING, MeasureSpec.EXACTLY),
				MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

		return width;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);

		buildViewForMeasuring();

		int width = calculateLayoutWidth(widthSize, widthMode);

		int height;
		if (heightMode == MeasureSpec.EXACTLY) {
			height = heightSize;
		} else {
			height = getDesiredHeight(mItemsLayout);

			if (heightMode == MeasureSpec.AT_MOST) {
				height = Math.min(height, heightSize);
			}
		}

		setMeasuredDimension(width, height);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		layout(r - l, b - t);
	}

	/**
	 * Sets layouts width and height
	 * @param width the layout width
	 * @param height the layout height
	 */
	private void layout(int width, int height) {
		int itemsWidth = width - 2 * PADDING;

		mItemsLayout.layout(0, 0, itemsWidth, height);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (mViewAdapter != null && mViewAdapter.getItemsCount() > 0) {
			updateView();

//			drawCenterRect(canvas);
			
			drawItems(canvas);
			drawCenterRect(canvas);
		}

		if (mDrawShadows) drawShadows(canvas);
	}

	/**
	 * Draws shadows on top and bottom of control
	 * @param canvas the canvas for drawing
	 */
	private void drawShadows(Canvas canvas) {
		int height = (int)(1.5 * getItemHeight());
		mTopShadow.setBounds(0, 0, getWidth(), height);
		mTopShadow.draw(canvas);

		mBottomShadow.setBounds(0, getHeight() - height, getWidth(), getHeight());
		mBottomShadow.draw(canvas);
	}

	/**
	 * Draws items
	 * @param canvas the canvas for drawing
	 */
	private void drawItems(Canvas canvas) {
		canvas.save();

		int top = (mCurrentItem - mFirstItem) * getItemHeight() + (getItemHeight() - getHeight()) / 2;
		canvas.translate(PADDING, - top + mScrollingOffset);

		mItemsLayout.draw(canvas);

		canvas.restore();
	}

	/**
	 * Draws rect for current value
	 * @param canvas the canvas for drawing
	 */
	private void drawCenterRect(Canvas canvas) {
		int center = getHeight() / 2;
		int offset = (int) (getItemHeight() / 2 * 1.2);
		mCenterDrawable.setBounds(0, center - offset, getWidth(), center + offset);
		mCenterDrawable.draw(canvas);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!isEnabled() || getViewAdapter() == null) {
			return true;
		}

		switch (event.getAction()) {
			case MotionEvent.ACTION_MOVE:
				if (getParent() != null) {
					getParent().requestDisallowInterceptTouchEvent(true);
				}
				break;

			case MotionEvent.ACTION_UP:
				if (!mIsScrollingPerformed) {
					int distance = (int) event.getY() - getHeight() / 2;
					if (distance > 0) {
						distance += getItemHeight() / 2;
					} else {
						distance -= getItemHeight() / 2;
					}
					int items = distance / getItemHeight();
					if (items != 0 && isValidItemIndex(mCurrentItem + items)) {
						notifyClickListenersAboutClick(mCurrentItem + items);
					}
				}
				break;
		}

		return mScroller.onTouchEvent(event);
	}

	/**
	 * Scrolls the wheel
	 * @param delta the scrolling value
	 */
	private void doScroll(int delta) {
		mScrollingOffset += delta;

		int itemHeight = getItemHeight();
		int count = mScrollingOffset / itemHeight;

		int pos = mCurrentItem - count;
		int itemCount = mViewAdapter.getItemsCount();

		int fixPos = mScrollingOffset % itemHeight;
		if (Math.abs(fixPos) <= itemHeight / 2) {
			fixPos = 0;
		}
		if (mIsCyclic && itemCount > 0) {
			if (fixPos > 0) {
				pos--;
				count++;
			} else if (fixPos < 0) {
				pos++;
				count--;
			}
			// fix position by rotating
			while (pos < 0) {
				pos += itemCount;
			}
			pos %= itemCount;
		} else {
			//
			if (pos < 0) {
				count = mCurrentItem;
				pos = 0;
			} else if (pos >= itemCount) {
				count = mCurrentItem - itemCount + 1;
				pos = itemCount - 1;
			} else if (pos > 0 && fixPos > 0) {
				pos--;
				count++;
			} else if (pos < itemCount - 1 && fixPos < 0) {
				pos++;
				count--;
			}
		}

		int offset = mScrollingOffset;
		if (pos != mCurrentItem) {
			setCurrentItem(pos, false);
		} else {
			invalidate();
		}

		// update offset
		mScrollingOffset = offset - count * itemHeight;
		if (mScrollingOffset > getHeight()) {
			mScrollingOffset = mScrollingOffset % getHeight() + getHeight();
		}
	}

	/**
	 * Scroll the wheel
	 * @param itemsToSkip items to scroll
	 * @param time scrolling duration
	 */
	public void scroll(int itemsToScroll, int time) {
		int distance = itemsToScroll * getItemHeight() - mScrollingOffset;
		mScroller.scroll(distance, time);
	}

	/**
	 * Calculates range for wheel items
	 * @return the items range
	 */
	private ItemsRange getItemsRange() {
		if (getItemHeight() == 0) {
			return null;
		}

		int first = mCurrentItem;
		int count = 1;

		while (count * getItemHeight() < getHeight()) {
			first--;
			count += 2; // top + bottom items
		}

		if (mScrollingOffset != 0) {
			if (mScrollingOffset > 0) {
				first--;
			}
			count++;

			// process empty items above the first or below the second
			int emptyItems = mScrollingOffset / getItemHeight();
			first -= emptyItems;
			count += Math.asin(emptyItems);
		}
		return new ItemsRange(first, count);
	}

	/**
	 * Rebuilds wheel items if necessary. Caches all unused items.
	 * 
	 * @return true if items are rebuilt
	 */
	private boolean rebuildItems() {
		boolean updated = false;
		ItemsRange range = getItemsRange();
		if (mItemsLayout != null) {
			int first = mRecycle.recycleItems(mItemsLayout, mFirstItem, range);
			updated = mFirstItem != first;
			mFirstItem = first;
		} else {
			createItemsLayout();
			updated = true;
		}

		if (!updated) {
			updated = mFirstItem != range.getFirst() || mItemsLayout.getChildCount() != range.getCount();
		}

		if (mFirstItem > range.getFirst() && mFirstItem <= range.getLast()) {
			for (int i = mFirstItem - 1; i >= range.getFirst(); i--) {
				if (!addViewItem(i, true)) {
					break;
				}
				mFirstItem = i;
			}
		} else {
			mFirstItem = range.getFirst();
		}

		int first = mFirstItem;
		for (int i = mItemsLayout.getChildCount(); i < range.getCount(); i++) {
			if (!addViewItem(mFirstItem + i, false) && mItemsLayout.getChildCount() == 0) {
				first++;
			}
		}
		mFirstItem = first;

		return updated;
	}

	/**
	 * Updates view. Rebuilds items and label if necessary, recalculate items sizes.
	 */
	private void updateView() {
		if (rebuildItems()) {
			calculateLayoutWidth(getWidth(), MeasureSpec.EXACTLY);
			layout(getWidth(), getHeight());
		}
	}

	/**
	 * Creates item layouts if necessary
	 */
	private void createItemsLayout() {
		if (mItemsLayout == null) {
			mItemsLayout = new LinearLayout(getContext());
			mItemsLayout.setOrientation(LinearLayout.VERTICAL);
		}
	}

	/**
	 * Builds view for measuring
	 */
	private void buildViewForMeasuring() {
		// clear all items
		if (mItemsLayout != null) {
			mRecycle.recycleItems(mItemsLayout, mFirstItem, new ItemsRange());
		} else {
			createItemsLayout();
		}

		// add views
		int addItems = mVisibleItems / 2;
		for (int i = mCurrentItem + addItems; i >= mCurrentItem - addItems; i--) {
			if (addViewItem(i, true)) {
				mFirstItem = i;
			}
		}
	}

	/**
	 * Adds view for item to items layout
	 * @param index the item index
	 * @param first the flag indicates if view should be first
	 * @return true if corresponding item exists and is added
	 */
	private boolean addViewItem(int index, boolean first) {
		View view = getItemView(index);
		if (view != null) {
			if (first) {
				mItemsLayout.addView(view, 0);
			} else {
				mItemsLayout.addView(view);
			}

			return true;
		}

		return false;
	}

	/**
	 * Checks whether intem index is valid
	 * @param index the item index
	 * @return true if item index is not out of bounds or the wheel is cyclic
	 */
	private boolean isValidItemIndex(int index) {
		return mViewAdapter != null && mViewAdapter.getItemsCount() > 0 &&
				(mIsCyclic || index >= 0 && index < mViewAdapter.getItemsCount());
	}

	/**
	 * Returns view for specified item
	 * @param index the item index
	 * @return item view or empty view if index is out of bounds
	 */
	private View getItemView(int index) {
		if (mViewAdapter == null || mViewAdapter.getItemsCount() == 0) {
			return null;
		}
		int count = mViewAdapter.getItemsCount();
		if (!isValidItemIndex(index)) {
			return mViewAdapter.getEmptyItem(mRecycle.getEmptyItem(), mItemsLayout);
		} else {
			while (index < 0) {
				index = count + index;
			}
		}

		index %= count;
		return mViewAdapter.getItem(index, mRecycle.getItem(), mItemsLayout);
	}

	/**
	 * Stops scrolling
	 */
	public void stopScrolling() {
		mScroller.stopScrolling();
	}
}

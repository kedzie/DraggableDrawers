package com.kedzie.drawer.drag;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewGroupCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RelativeLayout;

import com.kedzie.drawer.R;

/**
 * Created by kedzie on 6/25/13.
 */
public class DragLayout extends RelativeLayout {

    private static final String TAG = "DragLayout";

    /**
     * Listener for monitoring events about drawers.
     */
    public interface DrawerListener {
        /**
         * Called when a drawer's position changes.
         * @param drawerView The child view that was moved
         * @param slideOffset The new offset of this drawer within its range, from 0-1
         */
        public void onDrawerSlide(View drawerView, float slideOffset);

        /**
         * Called when a drawer has settled in a completely open state.
         * The drawer is interactive at this point.
         *
         * @param drawerView Drawer view that is now open
         */
        public void onDrawerOpened(View drawerView);

        /**
         * Called when a drawer has settled in a completely closed state.
         *
         * @param drawerView Drawer view that is now closed
         */
        public void onDrawerClosed(View drawerView);

        /**
         * Called when the drawer motion state changes. The new state will
         * be one of {@link #STATE_IDLE}, {@link #STATE_DRAGGING} or {@link #STATE_SETTLING}.
         *
         * @param newState The new drawer motion state
         */
        public void onDrawerStateChanged(int newState);
    }

    /** Indicates that any drawers are in an idle, settled state. No animation is in progress. */
    public static final int STATE_IDLE = ViewDragHelper.STATE_IDLE;
    /** Indicates that a drawer is currently being dragged by the user. */
    public static final int STATE_DRAGGING = ViewDragHelper.STATE_DRAGGING;
    /** Indicates that a drawer is in the process of settling to a final position. */
    public static final int STATE_SETTLING = ViewDragHelper.STATE_SETTLING;

    private static final int DEFAULT_SCRIM_COLOR = 0x99000000;
    private int mScrimColor = DEFAULT_SCRIM_COLOR;
    private float mScrimOpacity;
    private DrawerListener mListener;
    private boolean mInLayout=false;
    private boolean mFirstLayout=true;
    private int mDrawerState;

    private ViewDragHelper mDragHelper;
    private DragCallback mCallback = new DragCallback();

    public DragLayout(Context context) {
        this(context, null);
    }

    public DragLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Drawer, 0, 0);
        try {
            mScrimColor = a.getColor(R.styleable.DrawerLayout_scrim_color, DEFAULT_SCRIM_COLOR);
        } finally {
            a.recycle();
        }

        final float density = getResources().getDisplayMetrics().density;
        final float minVel = getResources().getInteger(R.integer.min_fling_velocity) * density;

        mDragHelper = ViewDragHelper.create(this, 0.5f, mCallback);
        mDragHelper.setMinVelocity(minVel);
        mCallback.setDragger(mDragHelper);

        // So that we can catch the back button
        setFocusableInTouchMode(true);
        ViewGroupCompat.setMotionEventSplittingEnabled(this, false);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mDragHelper.processTouchEvent(event);
        return true;
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams
                ? new LayoutParams((LayoutParams) p)
                : p instanceof ViewGroup.MarginLayoutParams
                ? new LayoutParams((MarginLayoutParams) p)
                : new LayoutParams(p);
    }

    /**
     * Set a listener to be notified of drawer events.
     *
     * @param listener Listener to notify when drawer events occur
     * @see DrawerListener
     */
    public void setDrawerListener(DrawerListener listener) {
        mListener = listener;
    }

    void setDrawerViewOffset(View drawerView, float slideOffset) {
        final LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
        if (slideOffset == lp.onScreen) {
            return;
        }
        lp.onScreen = slideOffset;
        Log.d(TAG, "Slide offset: " + slideOffset);
    }

    float getDrawerViewOffset(View drawerView) {
        return ((LayoutParams) drawerView.getLayoutParams()).onScreen;
    }

    @Override
    public void requestLayout() {
        if (!mInLayout)
            super.requestLayout();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mInLayout=true;
        super.onLayout(changed, l, t, r, b);
        mInLayout=false;
    }

    @Override
    public void computeScroll() {
        final int childCount = getChildCount();
        float scrimOpacity = 0;
        for (int i = 0; i < childCount; i++) {
            final float onscreen = ((LayoutParams) getChildAt(i).getLayoutParams()).onScreen;
            scrimOpacity = Math.max(scrimOpacity, onscreen);
        }
        mScrimOpacity = scrimOpacity;

        final int baseAlpha = (mScrimColor & 0xff000000) >>> 24;
        final int imag = (int) (baseAlpha * mScrimOpacity);
        final int color = imag << 24 | (mScrimColor & 0xffffff);
        setBackgroundColor(color);
        //mScrimPaint.setColor(color);

        // "|" used on purpose; both need to run.
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    /**
     *
     * @param drawer
     */
    public void openDrawer(DraggedView drawer) {

    }

    /**
     *
     * @param drawer
     */
    public void closeDrawer(DraggedView drawer) {
        switch(drawer.getDrawerType()) {
            case DraggedView.DRAWER_LEFT:

                break;
            case DraggedView.DRAWER_RIGHT:
                break;
            case DraggedView.DRAWER_TOP:
                break;
            case DraggedView.DRAWER_BOTTOM:
                break;
        }
    }

    /**
     *
     * @param drawer
     */
    public void closeAllDrawers(DraggedView drawer) {

    }

    /**
     * Resolve the shared state of all drawers from the component ViewDragHelpers.
     * Should be called whenever a ViewDragHelper's state changes.
     */
    void updateDrawerState(int forGravity, int activeState, View activeDrawer) {
        final int state = mDragHelper.getViewDragState();

        if (activeDrawer != null && activeState == STATE_IDLE) {
            final LayoutParams lp = (LayoutParams) activeDrawer.getLayoutParams();
            if (lp.onScreen == 0) {
                dispatchOnDrawerClosed(activeDrawer);
            } else if (lp.onScreen == 1) {
                dispatchOnDrawerOpened(activeDrawer);
            }
        }

        if (state != mDrawerState) {
            mDrawerState = state;

            if (mListener != null) {
                mListener.onDrawerStateChanged(state);
            }
        }
    }

    void dispatchOnDrawerClosed(View drawerView) {
        final LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
        if (lp.knownOpen) {
            lp.knownOpen = false;
            if (mListener != null) {
                mListener.onDrawerClosed(drawerView);
            }
            sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        }
    }

    void dispatchOnDrawerOpened(View drawerView) {
        final LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
        if (!lp.knownOpen) {
            lp.knownOpen = true;
            if (mListener != null) {
                mListener.onDrawerOpened(drawerView);
            }
            drawerView.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        }
    }

    void dispatchOnDrawerSlide(View drawerView, float slideOffset) {
        if (mListener != null) {
            mListener.onDrawerSlide(drawerView, slideOffset);
        }
    }

    /**
     *
     */
    private class DragCallback extends ViewDragHelper.Callback {

        private ViewDragHelper mDragger;


        public void setDragger(ViewDragHelper helper) {
            mDragger=helper;
        }

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child instanceof DraggedView;
        }

        @Override
        public void onViewDragStateChanged(int state) {

        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            float offset=0;
            final int childWidth = changedView.getWidth();
            final int childHeight = changedView.getHeight();

            final DraggedView dragView = (DraggedView)changedView;

            switch(dragView.getDrawerType()) {
                case DraggedView.DRAWER_LEFT:
                    offset = (float) (childWidth + left) / childWidth;
                    break;
                case DraggedView.DRAWER_RIGHT:
                    offset = (float) (getWidth() - left) / childWidth;
                    break;
                case DraggedView.DRAWER_TOP:
                    offset = (float) (childHeight + top) / childHeight;
                    break;
                case DraggedView.DRAWER_BOTTOM:
                    offset = (float) (getHeight() - top) / childHeight;
                    break;

            }
            setDrawerViewOffset(changedView, offset);
            invalidate();
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            final LayoutParams lp = (LayoutParams) capturedChild.getLayoutParams();
            lp.isPeeking = false;
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            final float offset = getDrawerViewOffset(releasedChild);
            final int childWidth = releasedChild.getWidth();
            final int childHeight = releasedChild.getHeight();
            final DraggedView dragView = (DraggedView)releasedChild;

            int left=releasedChild.getLeft();
            int top=releasedChild.getTop();
            switch(dragView.getDrawerType()) {
                case DraggedView.DRAWER_LEFT:
                    left = xvel > 0 || xvel == 0 && offset > 0.5f ? 0 : dragView.getHandleSize()-childWidth;
                    break;
                case DraggedView.DRAWER_RIGHT:
                    final int width = getWidth();
                    left = xvel < 0 || xvel == 0 && offset < 0.5f ? width - childWidth : width-dragView.getHandleSize();
                    break;
                case DraggedView.DRAWER_TOP:
                    top = yvel > 0 || yvel == 0 && offset > 0.5f ? 0 : dragView.getHandleSize()-childHeight;
                    break;
                default:
                    final int height = getHeight();
                    top = yvel < 0 || yvel == 0 && offset < 0.5f ? height-dragView.getHandleSize() : height - childHeight;
                    break;

            }
            mDragger.settleCapturedViewAt(left,top);
            invalidate();
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            final DraggedView dragView = (DraggedView)child;

            switch(dragView.getDrawerType()) {
                case DraggedView.DRAWER_LEFT:
                case DraggedView.DRAWER_RIGHT:
                    return child.getWidth()-dragView.getHandleSize();
                default:
                    return 0;

            }
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            final DraggedView dragView = (DraggedView)child;
            switch(dragView.getDrawerType()) {
                case DraggedView.DRAWER_BOTTOM:
                case DraggedView.DRAWER_TOP:
                    return child.getHeight()-dragView.getHandleSize();
                default:
                    return 0;
            }
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            final DraggedView dragView = (DraggedView)child;

            switch(dragView.getDrawerType()) {
                case DraggedView.DRAWER_TOP:
                    return Math.max(dragView.getHandleSize()-child.getHeight(), Math.min(top, 0));
                case DraggedView.DRAWER_BOTTOM:
                    final int height = getHeight();
                    return Math.max(height - child.getHeight(), Math.min(top, height-dragView.getHandleSize()));
                default:
                    return child.getTop();

            }
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            final DraggedView dragView = (DraggedView)child;

            switch(dragView.getDrawerType()) {
                case DraggedView.DRAWER_LEFT:
                    return Math.max(dragView.getHandleSize()-child.getWidth(), Math.min(left, 0));
                case DraggedView.DRAWER_RIGHT:
                    final int width = getWidth();
                    return Math.max(width - child.getWidth(), Math.min(left, width-dragView.getHandleSize()));
                default:
                    return child.getLeft();
            }

        }
    }

    public static class LayoutParams extends RelativeLayout.LayoutParams{

        float onScreen;
        boolean isPeeking;
        boolean knownOpen;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(LayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
        }
    }
}

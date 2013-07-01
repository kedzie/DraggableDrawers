package com.kedzie.drawer;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.view.KeyEventCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewGroupCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RelativeLayout;

/**
 * Layout which handles sliding drawers in all directions.
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
         * Called when a drawer starts dragging open.
         *
         * @param drawerView Drawer view that is now opening
         */
        public void onDrawerOpening(View drawerView);

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

    /**
     * Default implementation of {@link DrawerListener}
     */
    public static class SimpleDrawerListener implements DrawerListener {
        @Override public void onDrawerSlide(View drawerView, float slideOffset) {}
        @Override public void onDrawerOpened(View drawerView) {}
        @Override public void onDrawerClosed(View drawerView) {}
        @Override public void onDrawerStateChanged(int newState) {}
        @Override public void onDrawerOpening(View drawerView) {}
    }

    /** Indicates that any drawers are in an idle, settled state. No animation is in progress. */
    public static final int STATE_IDLE = ViewDragHelper.STATE_IDLE;
    /** Indicates that a drawer is currently being dragged by the user. */
    public static final int STATE_DRAGGING = ViewDragHelper.STATE_DRAGGING;
    /** Indicates that a drawer is in the process of settling to a final position. */
    public static final int STATE_SETTLING = ViewDragHelper.STATE_SETTLING;

    /** Current state of drawers */
    private int mDrawerState;

    private static final int DEFAULT_SCRIM_COLOR = 0x96000000;
    private int mScrimColor;
    private float mScrimOpacity;
    private DrawerListener mListener;
    private boolean mInLayout=false;
    private boolean mFirstLayout=true;
    private float mInitialMotionX;
    private float mInitialMotionY;

    private float minFlingVelocity;

    /** Each drawer has its own ViewDragHelper and DragCallback */
    private Map<DraggedDrawer, DrawerHolder> mDrawers = new HashMap<DraggedDrawer, DrawerHolder>();
    private ViewDragHelper mDragger;

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

        minFlingVelocity = getResources().getInteger(R.integer.drawer_min_fling_velocity) * getResources().getDisplayMetrics().density;

        // So that we can catch the back button
        setFocusableInTouchMode(true);
        ViewGroupCompat.setMotionEventSplittingEnabled(this, false);
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

    /**
     * Is there a drawer partially or fully open?
     * @return true if a drawer is visible
     */
    public boolean hasVisibleDrawer() {
        return findVisibleDrawer() != null;
    }

    /**
     * Get current state of drawers.
     * Can be STATE_DRAGGING, STATE_SETTLING, STATE_IDLE
     * @return  drawer state
     */
    public int getDrawerState() {
        return mDrawerState;
    }

    /**
     * Open a drawer with animation
     * @param drawerView the drawer to open
     */
    public void openDrawer(DraggedDrawer drawerView) {
        Log.d(TAG, "Opening drawer");
        final ViewDragHelper mDragHelper = mDrawers.get(drawerView).helper;

        switch(drawerView.getDrawerType()) {
            case DraggedDrawer.DRAWER_LEFT:
                mDragHelper.smoothSlideViewTo(drawerView, 0, drawerView.getTop());
                break;
            case DraggedDrawer.DRAWER_RIGHT:
                mDragHelper.smoothSlideViewTo(drawerView, getWidth() - drawerView.getWidth(),
                        drawerView.getTop());
                break;
            case DraggedDrawer.DRAWER_TOP:
                mDragHelper.smoothSlideViewTo(drawerView, drawerView.getLeft(), 0);
                break;
            case DraggedDrawer.DRAWER_BOTTOM:
                mDragHelper.smoothSlideViewTo(drawerView, drawerView.getLeft(), getHeight() - drawerView.getHeight());
                break;
        }
        invalidate();
    }

    /**
     * Close a drawer with animation
     * @param drawerView the drawer to close
     */
    public void closeDrawer(DraggedDrawer drawerView) {
        Log.d(TAG, "Closing drawer");
        final ViewDragHelper mDragHelper = mDrawers.get(drawerView).helper;

        switch(drawerView.getDrawerType()) {
            case DraggedDrawer.DRAWER_LEFT:
                mDragHelper.smoothSlideViewTo(drawerView, drawerView.getHandleSize()-drawerView.getWidth(), drawerView.getTop());
                break;
            case DraggedDrawer.DRAWER_RIGHT:
                mDragHelper.smoothSlideViewTo(drawerView, getWidth()-drawerView.getHandleSize(), drawerView.getTop());
                break;
            case DraggedDrawer.DRAWER_TOP:
                mDragHelper.smoothSlideViewTo(drawerView, drawerView.getLeft(), drawerView.getHandleSize()-drawerView.getHeight());
                break;
            case DraggedDrawer.DRAWER_BOTTOM:
                mDragHelper.smoothSlideViewTo(drawerView, drawerView.getLeft(), getHeight() - drawerView.getHandleSize());
                break;
        }
        invalidate();
    }

    /**
     * Close all the drawers
     */
    public void closeAllDrawers() {
        closeAllDrawers(null);
    }

    private void closeAllDrawers(DraggedDrawer except) {
        Log.d(TAG, "Closing all drawers " + (except==null ? "" : " except 1"));
        for(DraggedDrawer drawer : mDrawers.keySet()) {
            if(except==null || except!=drawer)
                closeDrawer(drawer);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mFirstLayout=true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mFirstLayout=true;
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

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mInLayout=true;
        super.onLayout(changed, l, t, r, b);
        for(DraggedDrawer drawerView : mDrawers.keySet()) {
            LayoutParams lp = (LayoutParams)drawerView.getLayoutParams();
            if(lp.onScreen==0f) {
                drawerView.setContentVisibility(INVISIBLE);
                switch(drawerView.getDrawerType()) {
                    case DraggedDrawer.DRAWER_LEFT:
                        drawerView.offsetLeftAndRight(drawerView.getHandleSize() - drawerView.getWidth());
                        break;
                    case DraggedDrawer.DRAWER_RIGHT:
                        drawerView.offsetLeftAndRight(drawerView.getWidth()-drawerView.getHandleSize());
                        break;
                    case DraggedDrawer.DRAWER_TOP:
                        drawerView.offsetTopAndBottom(drawerView.getHandleSize()-drawerView.getHeight());
                        break;
                    case DraggedDrawer.DRAWER_BOTTOM:
                        drawerView.offsetTopAndBottom(drawerView.getHeight() - drawerView.getHandleSize());
                        break;
                }
            }
        }
        mInLayout=false;
        mFirstLayout=false;
    }

    @Override
    public void addView(View child) {
        super.addView(child);
        processAddView(child);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        super.addView(child, params);
        processAddView(child);
    }

    private void processAddView(View child) {
        if(child instanceof DraggedDrawer) {
            Log.d(TAG, "Registering Drawer");
            DraggedDrawer dragView = (DraggedDrawer)child;
            DragCallback callback = new DragCallback();
            ViewDragHelper helper = ViewDragHelper.create(this, 0.5f, callback);
            helper.setMinVelocity(minFlingVelocity);
            callback.setDragHelper(helper);
            callback.setDragView(dragView);
            mDrawers.put(dragView, new DrawerHolder(helper, callback));
            if(mDragger==null)
                mDragger=helper;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean interceptForDrag = false;
        boolean interceptForTap = false;

        for(DrawerHolder holder : mDrawers.values()) {
            interceptForDrag |= holder.helper.shouldInterceptTouchEvent(ev);
        }

        final int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();
                mInitialMotionX = x;
                mInitialMotionY = y;
                if (mScrimOpacity > 0 &&
                        !(mDragger.findTopChildUnder((int) x, (int) y) instanceof DraggedDrawer)) {
                    interceptForTap = true;
                }
                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                closeAllDrawers();
        }

        return interceptForDrag || interceptForTap;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        for(DrawerHolder holder : mDrawers.values())
            holder.helper.processTouchEvent(event);

        final int action = event.getAction();

        switch (action & MotionEventCompat.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                final float x = event.getX();
                final float y = event.getY();
                mInitialMotionX = x;
                mInitialMotionY = y;
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                float dx = event.getX()-mInitialMotionX;
                float dy = event.getY()-mInitialMotionY;
                int slop = mDragger.getTouchSlop();
                if(dx*dx+dy*dy < slop*slop)
                    closeAllDrawers();
                break;
            }
        }
        return true;
    }

    @Override
    public void requestLayout() {
        if (!mInLayout)
            super.requestLayout();
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

        // "|" used on purpose; both need to run.
        boolean invalidate=false;
        for(DrawerHolder holder : mDrawers.values())
            invalidate |= holder.helper.continueSettling(true);
        if (invalidate)
            ViewCompat.postInvalidateOnAnimation(this);
    }

    private View findVisibleDrawer() {
        for(DraggedDrawer drawer : mDrawers.keySet())
            if(isDrawerVisible(drawer))
                return drawer;
        return null;
    }

    /**
     * Check if a given drawer view is currently visible on-screen. The drawer
     * may be only peeking onto the screen, fully extended, or anywhere inbetween.
     *
     * @param drawer Drawer view to check
     * @return true if the given drawer is visible on-screen
     */
    public boolean isDrawerVisible(View drawer) {
        if (!(drawer instanceof DraggedDrawer)) {
            throw new IllegalArgumentException("View " + drawer + " is not a drawer");
        }
        return ((LayoutParams) drawer.getLayoutParams()).onScreen > 0;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && hasVisibleDrawer()) {
            KeyEventCompat.startTracking(event);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            final View visibleDrawer = findVisibleDrawer();
            if (visibleDrawer != null) {
                closeAllDrawers();
            }
            return visibleDrawer != null;
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * Resolve the shared state of all drawers from the component ViewDragHelpers.
     * Should be called whenever a ViewDragHelper's state changes to notify listeners.
     */
    void updateDrawerState(int activeState, DraggedDrawer activeDrawer) {
        if(activeState!=activeDrawer.mState) {
            activeDrawer.mState=activeState;
            if(activeDrawer.mListener!=null)
                activeDrawer.mListener.onDrawerStateChanged(activeState);
        }
        int state = -1;

        for(DrawerHolder holder : mDrawers.values()) {
            if(holder.helper.getViewDragState() == STATE_DRAGGING) {
                state = STATE_DRAGGING;
                break;
            }
        }
        if(state==-1) {
            for(DrawerHolder holder : mDrawers.values()) {
                if(holder.helper.getViewDragState() == STATE_SETTLING) {
                    state = STATE_SETTLING;
                    break;
                }
            }
        }
        if(state==-1)
            state = STATE_IDLE;

        if (activeDrawer != null && activeState == STATE_IDLE) {
            final LayoutParams lp = (LayoutParams) activeDrawer.getLayoutParams();
            if (lp.onScreen == 0)
                dispatchOnDrawerClosed(activeDrawer);
            else if (lp.onScreen == 1)
                dispatchOnDrawerOpened(activeDrawer);
        }

        if (state != mDrawerState) {
            mDrawerState = state;
            if (mListener != null)
                mListener.onDrawerStateChanged(state);
        }
    }

    void setDrawerViewOffset(DraggedDrawer drawerView, float slideOffset) {
        final LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
        if (slideOffset == lp.onScreen)
            return;
        lp.onScreen = slideOffset;
        lp.knownOpen = slideOffset==1f;
        dispatchOnDrawerSlide(drawerView, slideOffset);
    }

    float getDrawerViewOffset(View drawerView) {
        return ((LayoutParams) drawerView.getLayoutParams()).onScreen;
    }

    /**
     * Dispatch drawer close event to registered listener
     * @param drawerView    The drawer relevant to the event
     */
    private void dispatchOnDrawerClosed(DraggedDrawer drawerView) {
        if (mListener != null)
            mListener.onDrawerClosed(drawerView);
        if(drawerView.mListener!=null)
            drawerView.mListener.onDrawerClosed();
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    /**
     * Dispatch drawer open event to registered listener
     * @param drawerView    The drawer relevant to the event
     */
    private void dispatchOnDrawerOpened(DraggedDrawer drawerView) {
        if (mListener != null)
            mListener.onDrawerOpened(drawerView);
        if(drawerView.mListener!=null)
            drawerView.mListener.onDrawerOpened();
        drawerView.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    /**
     * Dispatch drawer open event to registered listener
     * @param drawerView    The drawer relevant to the event
     */
    private void dispatchOnDrawerOpening(DraggedDrawer drawerView) {
        if (mListener != null)
            mListener.onDrawerOpening(drawerView);
//        if(drawerView.mListener!=null)
//            drawerView.mListener.onDrawerOpening();
    }

    /**
     * Dispatch drawer slide event to registered listener
     * @param drawerView    The drawer relevant to the event
     */
    private void dispatchOnDrawerSlide(DraggedDrawer drawerView, float slideOffset) {
        if (mListener != null)
            mListener.onDrawerSlide(drawerView, slideOffset);
        if(drawerView.mListener!=null)
            drawerView.mListener.onDrawerSlide(slideOffset);
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if(child instanceof DraggedDrawer) {
            final DraggedDrawer dragView = (DraggedDrawer)child;
            final LayoutParams lp = (LayoutParams)dragView.getLayoutParams();
            if(dragView.getShadowDrawable() != null && lp.onScreen>0f) {
                Drawable shadow = dragView.getShadowDrawable();
                final int shadowWidth = shadow.getIntrinsicWidth();
                final int shadowHeight = shadow.getIntrinsicHeight();

                switch(dragView.getDrawerType()) {
                    case DraggedDrawer.DRAWER_LEFT:
                        final int childRight = child.getRight()-dragView.getHandleSize();
                        shadow.setBounds(childRight, child.getTop(),
                                childRight + shadowWidth, child.getBottom());
                        break;
                    case DraggedDrawer.DRAWER_RIGHT:
                        final int childLeft = child.getLeft()+dragView.getHandleSize();
                        shadow.setBounds(childLeft-shadowWidth, child.getTop(),
                                childLeft, child.getBottom());
                        break;
                    case DraggedDrawer.DRAWER_TOP:
                        final int childBottom = child.getBottom()-dragView.getHandleSize();
                        shadow.setBounds(child.getLeft(), childBottom,
                                child.getRight(), childBottom+shadowHeight);
                        break;
                    case DraggedDrawer.DRAWER_BOTTOM:
                        final int childTop = child.getTop()+dragView.getHandleSize();
                        shadow.setBounds(child.getLeft(), childTop-shadowHeight,
                                child.getRight(), childTop);
                        break;
                }
                shadow.draw(canvas);
            }
        }
        return super.drawChild(canvas, child, drawingTime);
    }

    /**
     * Respond to drag events for a particular drawer
     */
    private class DragCallback extends ViewDragHelper.Callback {

        private ViewDragHelper mHelper;
        private DraggedDrawer mDragView;

        public void setDragHelper(ViewDragHelper helper) {
            mHelper = helper;
        }

        public void setDragView(DraggedDrawer view) {
            mDragView = view;
        }

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child == mDragView;
        }

        @Override
        public void onViewDragStateChanged(int state) {
            updateDrawerState(state, (DraggedDrawer)mHelper.getCapturedView());
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            float offset=0;
            final DraggedDrawer dragView = (DraggedDrawer)changedView;
            final int childWidth = dragView.getWidth()-dragView.getHandleSize();
            final int childHeight = dragView.getHeight()-dragView.getHandleSize();

            switch(dragView.getDrawerType()) {
                case DraggedDrawer.DRAWER_LEFT:
                    offset = (float) (childWidth + left) / childWidth;
                    break;
                case DraggedDrawer.DRAWER_RIGHT:
                    offset = (float) (getWidth() - left - dragView.getHandleSize()) / childWidth;
                    break;
                case DraggedDrawer.DRAWER_TOP:
                    offset = (float) (childHeight + top) / childHeight;
                    break;
                case DraggedDrawer.DRAWER_BOTTOM:
                    offset = (float) (getHeight() - top - dragView.getHandleSize()) / childHeight;
                    break;
            }
            setDrawerViewOffset(dragView, offset);
            dragView.setContentVisibility(offset > 0f ? VISIBLE : INVISIBLE);
            invalidate();
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            Log.v(TAG,"onViewCaptured");
            final DraggedDrawer dragView = (DraggedDrawer)capturedChild;
            final LayoutParams lp = (LayoutParams) capturedChild.getLayoutParams();
            if(lp.onScreen==0f)
                dispatchOnDrawerOpening(dragView);
            closeAllDrawers(dragView);
            if(dragView.getHandle()!=null)
                dragView.getHandle().setPressed(true);
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            final float offset = getDrawerViewOffset(releasedChild);
            final int childWidth = releasedChild.getWidth();
            final int childHeight = releasedChild.getHeight();
            final DraggedDrawer dragView = (DraggedDrawer)releasedChild;
            if(dragView.getHandle()!=null)
                dragView.getHandle().setPressed(false);

            int left=releasedChild.getLeft();
            int top=releasedChild.getTop();
            switch(dragView.getDrawerType()) {
                case DraggedDrawer.DRAWER_LEFT:
                    left = xvel > 0 || xvel == 0 && offset > 0.5f ? 0 : dragView.getHandleSize()-childWidth;
                    break;
                case DraggedDrawer.DRAWER_RIGHT:
                    final int width = getWidth();
                    left = xvel < 0 || xvel == 0 && offset > 0.5f ? width-childWidth : width-dragView.getHandleSize();
                    break;
                case DraggedDrawer.DRAWER_TOP:
                    top = yvel > 0 || yvel == 0 && offset > 0.5f ? 0 : dragView.getHandleSize()-childHeight;
                    break;
                default:
                    final int height = getHeight();
                    top = yvel < 0 || yvel == 0 && offset > 0.5f ? height-childHeight : height-dragView.getHandleSize();
                    break;

            }
            mHelper.settleCapturedViewAt(left, top);
            invalidate();
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            if(!(child instanceof DraggedDrawer)) return 0;
            final DraggedDrawer dragView = (DraggedDrawer)child;
            switch(dragView.getDrawerType()) {
                case DraggedDrawer.DRAWER_LEFT:
                case DraggedDrawer.DRAWER_RIGHT:
                    return child.getWidth()-dragView.getHandleSize();
                default:
                    return 0;

            }
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            if(!(child instanceof DraggedDrawer)) return 0;
            final DraggedDrawer dragView = (DraggedDrawer)child;
            switch(dragView.getDrawerType()) {
                case DraggedDrawer.DRAWER_BOTTOM:
                case DraggedDrawer.DRAWER_TOP:
                    return child.getHeight()-dragView.getHandleSize();
                default:
                    return 0;
            }
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            final DraggedDrawer dragView = (DraggedDrawer)child;

            switch(dragView.getDrawerType()) {
                case DraggedDrawer.DRAWER_TOP:
                    return Math.max(dragView.getHandleSize() - child.getHeight(), Math.min(top, 0));
                case DraggedDrawer.DRAWER_BOTTOM:
                    final int height = getHeight();
                    return Math.max(height - child.getHeight(), Math.min(top, height - dragView.getHandleSize()));
                default:
                    return child.getTop();

            }
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            final DraggedDrawer dragView = (DraggedDrawer)child;

            switch(dragView.getDrawerType()) {
                case DraggedDrawer.DRAWER_LEFT:
                    return Math.max(dragView.getHandleSize() - child.getWidth(), Math.min(left, 0));
                case DraggedDrawer.DRAWER_RIGHT:
                    final int width = getWidth();
                    return Math.max(width - child.getWidth(), Math.min(left, width - dragView.getHandleSize()));
                default:
                    return child.getLeft();
            }

        }
    }

    /**
     * Drawer related LayoutParams
     */
    public static class LayoutParams extends RelativeLayout.LayoutParams{

        public float onScreen;
        public boolean knownOpen;

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

    /**
     * Reference to a drawer and helper functionality
     */
    public static class DrawerHolder {
        public ViewDragHelper helper;
        public DragCallback callback;

        public DrawerHolder() {}

        public DrawerHolder(ViewDragHelper helper, DragCallback callback) {
            this.helper=helper;
            this.callback=callback;
        }
    }
}

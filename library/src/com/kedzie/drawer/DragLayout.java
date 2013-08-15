package com.kedzie.drawer;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
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

import java.util.HashMap;
import java.util.Map;

import static com.kedzie.drawer.DraggedDrawer.*;

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
         * be one of {@link DraggedDrawer#STATE_IDLE}, {@link DraggedDrawer#STATE_DRAGGING} or {@link DraggedDrawer#STATE_SETTLING}.
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

    /**
     * Multiplier for how sensitive the drag detection should be.
     * Larger values are more sensitive. 1.0f is normal.
     */
    public static final float DRAG_SENSITIVITY = .4f;

    private static final int DEFAULT_SCRIM_COLOR = 0x96000000;

    /** Current state of drawers */
    private int mDrawerState;

    private int mScrimColor;
    private float mScrimOpacity;
    private Paint mScrimPaint = new Paint();

    private DrawerListener mListener;
    private boolean mInLayout;
    private boolean mFirstLayout=true;
    private float mInitialMotionX;
    private float mInitialMotionY;
    private float mLargestDx;
    private float mLargestDy;

    private float minFlingVelocity;

    private ViewDragHelper mEdgeDragHelper;

    /** Each drawer has its own ViewDragHelper and DragCallback */
    private Map<DraggedDrawer, DrawerHolder> mDrawers = new HashMap<DraggedDrawer, DrawerHolder>();

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

        minFlingVelocity = getResources().getInteger(R.integer.drawer_min_fling_velocity)
                * getResources().getDisplayMetrics().density;

        mEdgeDragHelper = ViewDragHelper.create(this, DRAG_SENSITIVITY, new EdgeCallback());
        mEdgeDragHelper.setMinVelocity(minFlingVelocity);
        mEdgeDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_ALL);

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
     * Can be {@link DraggedDrawer#STATE_DRAGGING}, {@link DraggedDrawer#STATE_SETTLING},
     * {@link DraggedDrawer#STATE_IDLE}
     * @return  drawer state
     */
    public int getDrawerState() {
        return mDrawerState;
    }

    /**
     * Open a drawer with animation
     * @param drawer the drawer to open
     */
    public void openDrawer(DraggedDrawer drawer) {
        openDrawer(drawer, true);
    }

    /**
     * Open a drawer
     * @param drawer the drawer to open
     * @param animate whether to animate
     */
    public void openDrawer(DraggedDrawer drawer, boolean animate) {
        final LayoutParams lp = getDragLayoutParams(drawer);
        if(lp.onScreen==1f)
            return;

        dispatchOnDrawerOpening(drawer);
        closeAllDrawers(drawer, true);

        drawer.destinationOffset=1f;

        if(mFirstLayout) {
            lp.onScreen=1f;
            lp.knownOpen=true;
            drawer.setLayoutParams(lp);
        } else if(animate) {
            final ViewDragHelper helper = mDrawers.get(drawer).helper;
            switch(drawer.getDrawerType()) {
                case DRAWER_LEFT:
                    helper.smoothSlideViewTo(drawer, 0, drawer.getTop());
                    break;
                case DRAWER_RIGHT:
                    helper.smoothSlideViewTo(drawer, getWidth() - drawer.getWidth(), drawer.getTop());
                    break;
                case DRAWER_TOP:
                    helper.smoothSlideViewTo(drawer, drawer.getLeft(), 0);
                    break;
                case DRAWER_BOTTOM:
                    helper.smoothSlideViewTo(drawer, drawer.getLeft(), getHeight() - drawer.getHeight());
                    break;
            }
        } else {
            moveDrawerToOffset(drawer, 1f);
        }
        invalidate();
    }

    /**
     * Close a drawer with animation
     * @param drawer the drawer to close
     */
    public void closeDrawer(DraggedDrawer drawer) {
        closeDrawer(drawer, true);
    }
    /**
     * Close a drawer with animation
     * @param drawer the drawer to close
     * @param animate whether to animate
     */
    public void closeDrawer(DraggedDrawer drawer, boolean animate) {
        final LayoutParams lp = getDragLayoutParams(drawer);
        if(lp.onScreen==0f)
            return;

        drawer.destinationOffset=0f;

        if(mFirstLayout) {
            lp.onScreen=0f;
            lp.knownOpen=false;
            drawer.setLayoutParams(lp);
        } else if(animate) {
            final ViewDragHelper helper = mDrawers.get(drawer).helper;
            switch(drawer.getDrawerType()) {
                case DRAWER_LEFT:
                    helper.smoothSlideViewTo(drawer, drawer.getHandleSize() - drawer.getWidth(), drawer.getTop());
                    break;
                case DRAWER_RIGHT:
                    helper.smoothSlideViewTo(drawer, getWidth() - drawer.getHandleSize(), drawer.getTop());
                    break;
                case DRAWER_TOP:
                    helper.smoothSlideViewTo(drawer, drawer.getLeft(), drawer.getHandleSize() - drawer.getHeight());
                    break;
                case DRAWER_BOTTOM:
                    helper.smoothSlideViewTo(drawer, drawer.getLeft(), getHeight() - drawer.getHandleSize());
                    break;
            }
        } else {
            moveDrawerToOffset(drawer, 0f);
        }
        invalidate();
    }

    private void moveDrawerToOffset(DraggedDrawer drawer, float slideOffset) {
        final float oldOffset = getDrawerViewOffset(drawer);
        Log.v(TAG, "Move drawer to offset " + oldOffset + "-->" + slideOffset);
        switch(drawer.getDrawerType()) {
            case DRAWER_LEFT:
            case DRAWER_RIGHT:
                final int width = drawer.getWidth()-drawer.getHandleSize();
                final int dx = (int)((slideOffset-oldOffset)*width);
                drawer.offsetLeftAndRight(drawer.getDrawerType()==DRAWER_LEFT ? dx : -dx);
                break;
            case DRAWER_TOP:
            case DRAWER_BOTTOM:
                final int height = drawer.getHeight()-drawer.getHandleSize();
                final int dy = (int) ((slideOffset-oldOffset)*height);
                drawer.offsetTopAndBottom(drawer.getDrawerType() == DRAWER_TOP ? dy : -dy);
                break;
        }
        setDrawerViewOffset(drawer, slideOffset);
        updateDrawerState(STATE_IDLE, drawer);
    }

    /**
     * Close all the drawers
     * @param animate   whether to animate the drawers closing
     */
    public void closeAllDrawers(boolean animate) {
        closeAllDrawers(null, animate);
    }

    private void closeAllDrawers(DraggedDrawer except, boolean animate) {
        for(DraggedDrawer drawer : mDrawers.keySet()) {
            if(isDrawerVisible(drawer) && drawer!=except)
                closeDrawer(drawer, animate);
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
    public void requestLayout() {
        if (!mInLayout && mDrawerState==STATE_IDLE)
            super.requestLayout();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mInLayout=true;
        super.onLayout(changed, l, t, r, b);
        for(DraggedDrawer drawer : mDrawers.keySet()) {
            LayoutParams lp = getDragLayoutParams(drawer);
            final int drawerWidth = drawer.getWidth()-drawer.getHandleSize();
            final int drawerHeight = drawer.getHeight()-drawer.getHandleSize();
            final float offScreen = 1-lp.onScreen;
            switch(drawer.getDrawerType()) {
                case DRAWER_LEFT:
                    drawer.offsetLeftAndRight((int)(-offScreen*drawerWidth));
                    break;
                case DRAWER_RIGHT:
                    drawer.offsetLeftAndRight((int)(offScreen*drawerWidth));
                    break;
                case DRAWER_TOP:
                    drawer.offsetTopAndBottom((int)(-offScreen*drawerHeight));
                    break;
                case DRAWER_BOTTOM:
                    drawer.offsetTopAndBottom((int)(offScreen*drawerHeight));
                    break;
            }
            int contentVisibility = drawer.getContent().getVisibility();
            int newVisibility = lp.onScreen==0 ? GONE : VISIBLE;
            if(newVisibility!=contentVisibility)
                drawer.setContentVisibility(newVisibility);

            if(mFirstLayout && lp.onScreen==0f && drawer.mListener!=null)
                drawer.mListener.onDrawerClosed();
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
            final DraggedDrawer dragView = (DraggedDrawer)child;
            DragCallback callback = new DragCallback();
            ViewDragHelper helper = ViewDragHelper.create(this, DRAG_SENSITIVITY, callback);
            helper.setMinVelocity(minFlingVelocity);
            callback.setDragHelper(helper);
            callback.setDragView(dragView);
            mDrawers.put(dragView, new DrawerHolder(helper, callback));
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean interceptForDrag = false;
        boolean interceptForTap = false;

        for(DrawerHolder holder : mDrawers.values())
            interceptForDrag |= holder.helper.shouldInterceptTouchEvent(ev);
        interceptForDrag |= mEdgeDragHelper.shouldInterceptTouchEvent(ev);

        final int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();
                mInitialMotionX = x;
                mInitialMotionY = y;
                mLargestDx=0;
                mLargestDy=0;
                if (mScrimOpacity > 0 &&!(mEdgeDragHelper.findTopChildUnder((int) x, (int) y) instanceof DraggedDrawer))
                    interceptForTap = true;
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                break;
            }
        }
        if(interceptForDrag)
            Log.v(TAG, "interceptForDrag");
        if(interceptForTap)
            Log.v(TAG, "interceptForTap");
        return interceptForDrag || interceptForTap;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        for(DrawerHolder holder : mDrawers.values())
            holder.helper.processTouchEvent(event);
        mEdgeDragHelper.processTouchEvent(event);

        final int action = event.getAction();
        final float x = event.getX();
        final float y = event.getY();

        switch (action & MotionEventCompat.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                mInitialMotionX = x;
                mInitialMotionY = y;
                mLargestDx=0;
                mLargestDy=0;
                break;
            }
            case MotionEvent.ACTION_MOVE:
                updateTouchDelta(x, y);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                updateTouchDelta(x, y);
                int slop = mEdgeDragHelper.getTouchSlop();
                boolean isTap = mLargestDx * mLargestDx + mLargestDy * mLargestDy < slop * slop;
                if(isTap) {
                    Log.v(TAG, "Tap detected");
                    View under = mEdgeDragHelper.findTopChildUnder((int) x, (int) y);
                    if(under instanceof DraggedDrawer) {
                        DraggedDrawer d = (DraggedDrawer)under;
                        if(d.isHandleHit((int) x, (int) y)) {
                            if(getDragLayoutParams(d).knownOpen)
                                closeDrawer(d);
                            else
                                openDrawer(d);
                        }
                    } else {
                        closeAllDrawers(true);
                    }
                }
                break;
            }
        }
        return true;
    }

    /**
     * Calculate furthest distance from ACTION_DOWN event
     */
    private void updateTouchDelta(float x, float y) {
        float dx = Math.abs(x-mInitialMotionX);
        float dy = Math.abs(y-mInitialMotionY);
        mLargestDx=Math.max(mLargestDx, dx);
        mLargestDy=Math.max(mLargestDy, dy);
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
    public boolean isDrawerVisible(DraggedDrawer drawer) {
        return getDragLayoutParams(drawer).onScreen > 0;
    }

    private LayoutParams getDragLayoutParams(DraggedDrawer drawer) {
        return (LayoutParams) drawer.getLayoutParams();
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
            if (visibleDrawer != null)
                closeAllDrawers(true);
            return visibleDrawer != null;
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * Resolve the shared state of all drawers from the component ViewDragHelpers.
     * Should be called whenever a ViewDragHelper's state changes to notify listeners.
     */
    void updateDrawerState(int activeState, DraggedDrawer activeDrawer) {
        int state = STATE_IDLE;
        boolean hasSettling=false;
        for(DrawerHolder holder : mDrawers.values()) {
            if(holder.helper.getViewDragState() == STATE_DRAGGING) {
                state = STATE_DRAGGING;
                break;
            }
            hasSettling |= (holder.helper.getViewDragState()==STATE_SETTLING);
        }
        if(state!=STATE_DRAGGING && hasSettling)
            state = STATE_SETTLING;

        if (activeDrawer != null && activeState == STATE_IDLE) {
            final LayoutParams lp = getDragLayoutParams(activeDrawer);
            if(lp.onScreen>0.f && lp.onScreen<1.f) {
                Log.w(TAG, "Destination offset is off. Forcing drawer location.");
                moveDrawerToOffset(activeDrawer, activeDrawer.destinationOffset);
            }
            if (lp.onScreen == 0)
                dispatchOnDrawerClosed(activeDrawer);
            else if (lp.onScreen == 1)
                dispatchOnDrawerOpened(activeDrawer);
        }
        if(activeState!=activeDrawer.mState) {
            activeDrawer.setDrawerState(activeState);
            if(activeDrawer.mListener!=null)
                activeDrawer.mListener.onDrawerStateChanged(activeState);
        }
        if (state != mDrawerState) {
            mDrawerState = state;
            if (mListener != null)
                mListener.onDrawerStateChanged(state);
        }
    }

    void setDrawerViewOffset(DraggedDrawer drawer, float slideOffset) {
        final LayoutParams lp = getDragLayoutParams(drawer);
        if (slideOffset == lp.onScreen)
            return;
        lp.onScreen = slideOffset;
        lp.knownOpen = slideOffset==1f;
        drawer.setContentVisibility(slideOffset > 0f ? VISIBLE : GONE);
        dispatchOnDrawerSlide(drawer, slideOffset);
        if(drawer.mListener!=null)
            drawer.mListener.onDrawerSlide(slideOffset);
    }

    float getDrawerViewOffset(View drawerView) {
        return ((LayoutParams) drawerView.getLayoutParams()).onScreen;
    }

    /**
     * Dispatch drawer close event to registered listener
     * @param drawerView    The drawer relevant to the event
     */
    private void dispatchOnDrawerClosed(DraggedDrawer drawerView) {
        if (mListener!=null)
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
        if (mListener!=null)
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
        if (mListener!=null)
            mListener.onDrawerOpening(drawerView);
        if(drawerView.mListener!=null)
            drawerView.mListener.onDrawerOpening();
        drawerView.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    /**
     * Dispatch drawer slide event to registered listener
     * @param drawerView    The drawer relevant to the event
     */
    private void dispatchOnDrawerSlide(DraggedDrawer drawerView, float slideOffset) {
        if (mListener!=null)
            mListener.onDrawerSlide(drawerView, slideOffset);
        if(drawerView.mListener!=null)
            drawerView.mListener.onDrawerSlide(slideOffset);
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if(child instanceof DraggedDrawer) {
            final DraggedDrawer dragView = (DraggedDrawer)child;
            final LayoutParams lp = getDragLayoutParams(dragView);
            if(dragView.getShadowDrawable() != null && lp.onScreen>0f) {
                Drawable shadow = dragView.getShadowDrawable();
                final int shadowWidth = shadow.getIntrinsicWidth();
                final int shadowHeight = shadow.getIntrinsicHeight();
                final int drawerPeekDistance = mEdgeDragHelper.getEdgeSize();
                switch(dragView.getDrawerType()) {
                    case DRAWER_LEFT:{
                        final int childRight = child.getRight()-dragView.getHandleSize();
                        shadow.setAlpha((int) (0xff *
                                Math.max(0, Math.min((float) childRight / drawerPeekDistance, 1.f))));
                        shadow.setBounds(childRight, child.getTop(), childRight + shadowWidth, child.getBottom());
                        break;
                    }
                    case DRAWER_RIGHT:{
                        final int childLeft = child.getLeft()+dragView.getHandleSize();
                        shadow.setAlpha((int) (0xff *
                                Math.max(0, Math.min((float) (getWidth()-childLeft) / drawerPeekDistance, 1.f))));
                        shadow.setBounds(childLeft-shadowWidth, child.getTop(), childLeft, child.getBottom());
                        break;
                    }
                    case DRAWER_TOP: {
                        final int childBottom = child.getBottom()-dragView.getHandleSize();
                        shadow.setAlpha((int) (0xff *
                                Math.max(0, Math.min((float) childBottom / drawerPeekDistance, 1.f))));
                        shadow.setBounds(child.getLeft(), childBottom, child.getRight(), childBottom + shadowHeight);
                        break;
                    }
                    case DRAWER_BOTTOM:{
                        final int childTop = child.getTop()+dragView.getHandleSize();
                        shadow.setAlpha((int) (0xff *
                                Math.max(0, Math.min((float) (getHeight()-childTop) / drawerPeekDistance, 1.f))));
                        shadow.setBounds(child.getLeft(), childTop-shadowHeight, child.getRight(), childTop);
                        break;
                    }
                }
                shadow.draw(canvas);
            }
            return super.drawChild(canvas, child, drawingTime);
        } else {
            boolean result = super.drawChild(canvas, child, drawingTime);
//            if(mScrimOpacity>0) {
//                final int baseAlpha = (mScrimColor & 0xff000000) >>> 24;
//                final int imag = (int) (baseAlpha * mScrimOpacity);
//                final int color = imag << 24 | (mScrimColor & 0xffffff);
//                mScrimPaint.setColor(color);
//                canvas.drawRect(0, 0, getWidth(), getHeight(), mScrimPaint);
//            }
            return result;
        }
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        final SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        if (ss.openDrawerId != SavedState.NIL_DRAWER) {
            final DraggedDrawer toOpen = (DraggedDrawer) findViewById(ss.openDrawerId);
            if (toOpen != null)
                openDrawer(toOpen);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final SavedState ss = new SavedState(super.onSaveInstanceState());
        for(DraggedDrawer drawer : mDrawers.keySet()) {
            if (getDragLayoutParams(drawer).knownOpen) {
                ss.openDrawerId = drawer.getId();
                break;
            }
        }
        return ss;
    }

    /**
     * State persisted across instances.
     * Stores the currently opened drawer.
     */
    protected static class SavedState extends BaseSavedState {
        static final int NIL_DRAWER = -1;

        int openDrawerId = NIL_DRAWER;

        public SavedState(Parcel in) {
            super(in);
            openDrawerId = in.readInt();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(openDrawerId);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    /**
     * Handle edge drags for all drawers.
     * Uses Z-order to resolve conflicts (overlapping drawers)
     */
    private class EdgeCallback extends ViewDragHelper.Callback {

        @Override
        public void onEdgeDragStarted(int edgeFlags, int pointerId) {
            Log.v(TAG, String.format("EdgeHelperDrag started @ %1$d x %2$d", (int)mInitialMotionX, (int)mInitialMotionY));
            int drawerType = 0;
            if((edgeFlags&ViewDragHelper.EDGE_LEFT) == edgeFlags)
                drawerType = DRAWER_LEFT;
            else if((edgeFlags&ViewDragHelper.EDGE_RIGHT) == edgeFlags)
                drawerType = DRAWER_RIGHT;
            else if((edgeFlags&ViewDragHelper.EDGE_TOP) == edgeFlags)
                drawerType = DRAWER_TOP;
            else if((edgeFlags&ViewDragHelper.EDGE_BOTTOM) == edgeFlags)
                drawerType = DRAWER_BOTTOM;

            DraggedDrawer drawer = null;
            for(int i=getChildCount()-1;i>=0; i--) {
                View child = getChildAt(i);
                if(!(child instanceof DraggedDrawer))
                    continue;
                DraggedDrawer childDrawer = (DraggedDrawer)child;
                if(childDrawer.isEdgeDraggable() && childDrawer.getDrawerType()==drawerType) {
                    switch(drawerType) {
                        case DRAWER_LEFT:
                        case DRAWER_RIGHT:
                            if(mInitialMotionY>childDrawer.getTop() && mInitialMotionY<childDrawer.getBottom()) {
                                drawer = childDrawer;
                                break;
                            }
                        case DRAWER_TOP:
                        case DRAWER_BOTTOM:
                            if(mInitialMotionX>childDrawer.getLeft() && mInitialMotionX<childDrawer.getRight()) {
                                drawer = childDrawer;
                                break;
                            }
                    }
                }
            }
            if(drawer!=null) {
                Log.v(TAG, "Edge Capturing : " + drawer);
                mDrawers.get(drawer).helper.captureChildView(drawer, pointerId);
            }
        }

        @Override
        public void onEdgeTouched(int edgeFlags, int pointerId) {
            //peek
        }

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return false;	//Drawer is captured by its own ViewDragHelper. Not this one.
        }
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
                case DRAWER_LEFT:
                    offset = (float) (childWidth + left) / childWidth;
                    break;
                case DRAWER_RIGHT:
                    offset = (float) (getWidth() - left - dragView.getHandleSize()) / childWidth;
                    break;
                case DRAWER_TOP:
                    offset = (float) (childHeight + top) / childHeight;
                    break;
                case DRAWER_BOTTOM:
                    offset = (float) (getHeight() - top - dragView.getHandleSize()) / childHeight;
                    break;
            }
            setDrawerViewOffset(dragView, offset);
            invalidate();
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            final DraggedDrawer dragView = (DraggedDrawer)capturedChild;
            if(getDragLayoutParams(dragView).onScreen==0f)
                dispatchOnDrawerOpening(dragView);
            closeAllDrawers(dragView, true);
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

            int left=dragView.getLeft();
            int top=dragView.getTop();
            switch(dragView.getDrawerType()) {
                case DRAWER_LEFT:
                    left = xvel > 0 || xvel == 0 && offset > .5f ? 0 : dragView.getHandleSize()-childWidth;
                    break;
                case DRAWER_RIGHT:
                    final int width = getWidth();
                    left = xvel < 0 || xvel == 0 && offset > .5f ? width-childWidth : width-dragView.getHandleSize();
                    break;
                case DRAWER_TOP:
                    top = yvel > 0 || yvel == 0 && offset > .5f ? 0 : dragView.getHandleSize()-childHeight;
                    break;
                default:
                    final int height = getHeight();
                    top = yvel < 0 || yvel == 0 && offset > .5f ? height-childHeight : height-dragView.getHandleSize();
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
                case DRAWER_LEFT:
                case DRAWER_RIGHT:
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
                case DRAWER_BOTTOM:
                case DRAWER_TOP:
                    return child.getHeight()-dragView.getHandleSize();
                default:
                    return 0;
            }
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            final DraggedDrawer dragView = (DraggedDrawer)child;
            switch(dragView.getDrawerType()) {
                case DRAWER_TOP:
                    return Math.max(dragView.getHandleSize() - child.getHeight(), Math.min(top, 0));
                case DRAWER_BOTTOM:
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
                case DRAWER_LEFT:
                    return Math.max(dragView.getHandleSize() - child.getWidth(), Math.min(left, 0));
                case DRAWER_RIGHT:
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

        public DrawerHolder(ViewDragHelper helper, DragCallback callback) {
            this.helper=helper;
            this.callback=callback;
        }
    }
}
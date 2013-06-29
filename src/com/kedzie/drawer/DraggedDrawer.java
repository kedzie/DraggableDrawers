package com.kedzie.drawer;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * Draggable drawer with content and visible handle for all orientations.  For
 * dragging capabilities must be placed inside a {@link DragLayout}.
 */
public class DraggedDrawer extends LinearLayout {
    public static final String TAG = "DraggedDrawer";

    /**
     * Listener for monitoring events about drawers.
     */
    public interface DrawerListener {

        /**
         * Called when a drawer's position changes.
         * @param slideOffset The new offset of this drawer within its range, from 0-1
         * with 1 being totally open, 0 meaning totally closed.
         */
        public void onDrawerSlide(float slideOffset);

        /**
         * Called when a drawer has settled in a completely open state.
         * The drawer is interactive at this point.
         */
        public void onDrawerOpened();

        /**
         * Called when a drawer has settled in a completely closed state.
         */
        public void onDrawerClosed();

        /**
         * Called when the drawer motion state changes. The new state will
         * be one of {@link DragLayout#STATE_IDLE}, {@link DragLayout#STATE_DRAGGING} or {@link DragLayout#STATE_SETTLING}.
         *
         * @param newState The new drawer motion state
         */
        public void onDrawerStateChanged(int newState);
    }

    /**
     * Default implementation of {@link DrawerListener}
     */
    public static class SimpleDrawerListener implements DrawerListener {
        @Override public void onDrawerSlide(float slideOffset) {}
        @Override public void onDrawerOpened() {}
        @Override public void onDrawerClosed() {}
        @Override public void onDrawerStateChanged(int newState) {}
    }

    /** LEFT --> RIGHT orientation */
    public static final int DRAWER_LEFT=1;
    /** RIGHT --> LEFT orientation */
    public static final int DRAWER_RIGHT=2;
    /** TOP --> DOWN orientation */
    public static final int DRAWER_TOP=3;
    /** BOTTOM --> UP orientation */
    public static final int DRAWER_BOTTOM=4;

    /**
     * Drawer-specific event listener. For events relating to any drawer,
     * see {@link DragLayout#setDrawerListener(DragLayout.DrawerListener)}
     */
    DrawerListener mListener;

    /** Handle size */
    private int mHandleSize;
    /** Drawer orientation */
    private int mDrawerType;
    /** Drawer handle.  Maybe <code>null</code> */
    private View mHandle;
    /** Drawer content */
    private View mContent;
    /** Drawable used for drop-shadow when drawer is visible */
    private Drawable mShadowDrawable;
    /** Resource id of handle view */
    private int mHandleId;
    /** Resource id of content view */
    private int mContentId;

    public DraggedDrawer(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Drawer, 0, 0);
        try {
            mDrawerType = a.getInt(R.styleable.Drawer_type, 0);
            mHandleId = a.getResourceId(R.styleable.Drawer_handle, 0);
            mContentId = a.getResourceId(R.styleable.Drawer_content, 0);
            mShadowDrawable = a.getDrawable(R.styleable.Drawer_shadow);
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mHandle = findViewById(mHandleId);
        mContent = findViewById(mContentId);
        //keep the original layout params
        LayoutParams handleParams = mHandle!=null ? (LayoutParams) mHandle.getLayoutParams() : null;
        removeAllViews();

        switch(mDrawerType) {
            case DRAWER_LEFT:
                setOrientation(HORIZONTAL);
                addView(mContent, new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
                if(mHandle!=null) addView(mHandle, handleParams);
                break;
            case DRAWER_RIGHT:
                setOrientation(HORIZONTAL);
                if(mHandle!=null) addView(mHandle, handleParams);
                addView(mContent, new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
                break;
            case DRAWER_TOP:
                setOrientation(VERTICAL);
                addView(mContent, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
                if(mHandle!=null) addView(mHandle, handleParams);
                break;
            case DRAWER_BOTTOM:
                setOrientation(VERTICAL);
                if(mHandle!=null) addView(mHandle, handleParams);
                addView(mContent, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
                break;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if(mHandle!=null) {
            mHandleSize = (mDrawerType==DRAWER_LEFT || mDrawerType==DRAWER_RIGHT) ?
                    mHandle.getMeasuredWidth() : mHandle.getMeasuredHeight();
        }
    }

    /**
     * Handle view size. Zero if no handle.
     * @return size of handle (width for horizontal drawers, height for vertical drawers)
     */
    public int getHandleSize() {
        return mHandleSize;
    }

    /**
     * Drawer orientation
     * @return drawer orientation, i.e. DRAWER_LEFT, DRAWER_TOP, etc.
     */
    public int getDrawerType() {
        return mDrawerType;
    }

    /**
     * Get drawable to represent the drawer's shadow
     * @return
     */
    public Drawable getShadowDrawable() {
        return mShadowDrawable;
    }

    /**
     * Set visibility of content view.
     * This will alter the content visibility and make needed offsets to
     * maintain consistent view location.
     * <pre>drawer.getContent().setVisibility(View.GONE);</pre>
     * @param visibility    Desired visibilty. i.e. {@link View#VISIBLE} {@link View#INVISIBLE} or {@link View#GONE}
     */
    public void setContentVisibility(int visibility) {
        if(visibility==View.GONE && mContent.getVisibility()!=View.GONE) { //removing from layout
            Log.d(TAG, "Hiding content");
            if(mDrawerType==DRAWER_LEFT)
                mHandle.offsetLeftAndRight(mContent.getWidth());
            else if(mDrawerType==DRAWER_TOP)
                mHandle.offsetTopAndBottom(mContent.getHeight());
        } else if(visibility!=View.GONE && mContent.getVisibility()==View.GONE) {
            Log.d(TAG, "Showing content");
            if(mDrawerType==DRAWER_LEFT)
                mHandle.offsetLeftAndRight(-mContent.getWidth());
            else if(mDrawerType==DRAWER_TOP)
                mHandle.offsetTopAndBottom(-mContent.getHeight());
        }
        mContent.setVisibility(visibility);
    }

    /**
     * Get the drawer handle
     * @return  drawer handle view or <code>null</code> if no handle
     */
    public View getHandle() {
        return mHandle;
    }

    /**
     * Get the drawer content
     * @return  drawer content view
     */
    public View getContent() {
        return mContent;
    }
}

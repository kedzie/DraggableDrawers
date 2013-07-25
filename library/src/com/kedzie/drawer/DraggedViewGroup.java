package com.kedzie.drawer;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;

/**
 * Draggable drawer
 */
public class DraggedViewGroup extends ViewGroup {
    private static final String TAG = "DraggedViewGroup";

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
         * be one of {@link DraggedViewGroup#STATE_IDLE}, {@link DraggedViewGroup#STATE_DRAGGING} or {@link DraggedViewGroup#STATE_SETTLING}.
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

    /** Indicates that any drawers are in an idle, settled state. No animation is in progress. */
    public static final int STATE_IDLE = ViewDragHelper.STATE_IDLE;
    /** Indicates that a drawer is currently being dragged by the user. */
    public static final int STATE_DRAGGING = ViewDragHelper.STATE_DRAGGING;
    /** Indicates that a drawer is in the process of settling to a final position. */
    public static final int STATE_SETTLING = ViewDragHelper.STATE_SETTLING;

    /**
     * Drawer-specific event listener. For events relating to any drawer,
     * see {@link DraggedViewGroup#setDrawerListener(DrawerListener)}
     */
    DrawerListener mListener;

    /** Handle size */
    private int mHandleSize;

    private int mHandleWidth;
    private int mHandleHeight;

    /** Drawer orientation */
    @ViewDebug.ExportedProperty(category = "layout")
    private int mDrawerType;

    /** Drag from edge enabled? */
    @ViewDebug.ExportedProperty(category = "layout")
    private boolean mEdgeDraggable;

    /** Resource id of handle view */
    @ViewDebug.ExportedProperty(category = "layout")
    private int mHandleId;

    /** Resource id of content view */
    @ViewDebug.ExportedProperty(category = "layout")
    private int mContentId;

    /** Drawer handle.  Maybe <code>null</code> */
    private View mHandle;
    /** Drawer content */
    private View mContent;
    /** Drawable used for drop-shadow when drawer is visible */
    private Drawable mShadowDrawable;

    /** Current state i.e. {@link DraggedDrawer#STATE_DRAGGING} {@link DraggedDrawer#STATE_IDLE} */
    int mState;

    private boolean mInLayout;

    public DraggedViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Drawer, 0, 0);
        try {
            mDrawerType = a.getInt(R.styleable.Drawer_type, DRAWER_LEFT);
            mHandleId = a.getResourceId(R.styleable.Drawer_handleId, 0);
            mContentId = a.getResourceId(R.styleable.Drawer_contentId, 0);
            mShadowDrawable = a.getDrawable(R.styleable.Drawer_shadow);
            mEdgeDraggable = a.getBoolean(R.styleable.Drawer_edgeDraggable, false);
        } finally {
            a.recycle();
        }
    }

    /**
     * @param context
     */
    public DraggedViewGroup(Context context) {
        super(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mHandle = findViewById(mHandleId);
        mContent = findViewById(mContentId);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

        if(mHandle!=null) {
            measureChild(mHandle, widthMeasureSpec, heightMeasureSpec);
            mHandleWidth = mHandle.getMeasuredWidth();
            mHandleHeight = mHandle.getMeasuredHeight();
            mHandleSize = (mDrawerType==DRAWER_LEFT || mDrawerType==DRAWER_RIGHT) ?
                    mHandleWidth : mHandleHeight;
        }

        int dw = getPaddingLeft() + getPaddingRight();
        int dh = getPaddingTop() + getPaddingBottom();

        if(mContent!=null) {
        switch(mDrawerType) {
            case DRAWER_BOTTOM:
            case DRAWER_TOP:
                measureChild(mContent, widthMeasureSpec,
                        MeasureSpec.makeMeasureSpec(heightSpecSize-mHandleHeight, heightSpecMode));
                dw += Math.max(mHandleWidth, mContent.getMeasuredWidth());
                dh += mHandleHeight+mContent.getMeasuredHeight();
                break;
            case DRAWER_LEFT:
            case DRAWER_RIGHT:
                measureChild(mContent, MeasureSpec.makeMeasureSpec(widthSpecSize-mHandleWidth, widthSpecMode),
                        heightMeasureSpec);
                dw += mHandleWidth+mContent.getMeasuredWidth();
                dh += Math.max(mHandleHeight, mContent.getMeasuredHeight());
                break;
        }
        }

        setMeasuredDimension(resolveSize(dw, widthMeasureSpec), resolveSize(dh, heightMeasureSpec));
    }

    @Override
    public void requestLayout() {
        if(!mInLayout)
            super.requestLayout();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mInLayout=true;

        int handleTop=0, handleLeft=0;
        if(mHandle!=null) {
            LayoutParams lp = (LayoutParams)mHandle.getLayoutParams();
            if(mDrawerType==DRAWER_LEFT || mDrawerType==DRAWER_RIGHT) { //horizontal drawers
                switch(lp.gravity) {
                    case Gravity.TOP:
                        handleTop = lp.topMargin;
                        break;
                    case Gravity.BOTTOM:
                        handleTop = mContent.getMeasuredHeight()-mHandleHeight-lp.bottomMargin;
                        break;
                    case Gravity.CENTER_HORIZONTAL:
                    case Gravity.CENTER:
                    default:
                        handleTop = (mContent.getMeasuredHeight()-mHandleHeight)/2;
                        break;
                }
            } else {    //vertical drawers
                switch(lp.gravity) {
                    case Gravity.LEFT:
                        handleLeft = lp.leftMargin;
                        break;
                    case Gravity.RIGHT:
                        handleLeft = mContent.getMeasuredWidth()-mHandleWidth-lp.rightMargin;
                        break;
                    case Gravity.CENTER_VERTICAL:
                    case Gravity.CENTER:
                    default:
                        handleLeft = (mContent.getMeasuredWidth()-mHandleWidth)/2;
                        break;
                }
            }
        }
        if(mContent!=null) {
            switch(mDrawerType) {
                case DRAWER_LEFT:
                    mContent.layout(0, 0, mContent.getMeasuredWidth(), mContent.getMeasuredHeight());
                    if(mHandle!=null)
                        mHandle.layout(mContent.getMeasuredWidth(), handleTop, mContent.getMeasuredWidth()+mHandleWidth, handleTop+mHandleHeight);
                    break;
                case DRAWER_RIGHT:
                    if(mHandle!=null)
                        mHandle.layout(0, handleTop, mHandleWidth, handleTop+mHandleHeight);
                    mContent.layout(mHandleWidth, 0, mHandleWidth+mContent.getMeasuredWidth(), mContent.getMeasuredHeight());
                    break;
                case DRAWER_TOP:
                    mContent.layout(0, 0, mContent.getMeasuredWidth(), mContent.getMeasuredHeight());
                    if(mHandle!=null)
                        mHandle.layout(handleLeft, mContent.getMeasuredHeight(), handleLeft+mHandleWidth,mContent.getMeasuredHeight()+mHandleHeight);
                    break;
                case DRAWER_BOTTOM:
                    if(mHandle!=null)
                        mHandle.layout(handleLeft, 0, handleLeft+mHandleWidth, mHandleHeight);
                    mContent.layout(0, mHandleHeight, mContent.getMeasuredWidth(), mHandleHeight+mContent.getMeasuredHeight());
                    break;
            }
        }
        mInLayout=false;
    }

    /**
     * Subscribe to drawer events
     * @param listener  the listener
     */
    public void setDrawerListener(DrawerListener listener) {
        mListener = listener;
    }

    /**
     * Handle view size
     * @return size of handle (width for horizontal drawers, height for vertical drawers)
     */
    public int getHandleSize() {
        return mHandleSize;
    }

    /**
     * Set visibility of content view.
     * This will alter the content visibility and make needed offsets to
     * maintain consistent view location.
     * <pre>drawer.getContent().setVisibility(View.GONE);</pre>
     * @param visibility    Desired visibilty. i.e. {@link View#VISIBLE} {@link View#INVISIBLE} or {@link View#GONE}
     */
    void setContentVisibility(int visibility) {
        if(visibility==View.GONE && mContent.getVisibility()!=View.GONE) { //adding to layout
            Log.d(TAG, "Showing content");
            if(mDrawerType==DRAWER_LEFT)
                offsetLeftAndRight(-mContent.getWidth());
            else if(mDrawerType==DRAWER_TOP)
                offsetTopAndBottom(-mContent.getHeight());
        } else if(visibility!=View.GONE && mContent.getVisibility()==View.GONE) {
            Log.d(TAG, "Hiding content");
            if(mDrawerType==DRAWER_LEFT)
                offsetLeftAndRight(mContent.getWidth());
            else if(mDrawerType==DRAWER_TOP)
                offsetTopAndBottom(mContent.getHeight());
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
     * Drawer state.
     * {@link DraggedDrawer#STATE_IDLE}, {@link DraggedDrawer#STATE_SETTLING} or {@link DraggedDrawer#STATE_DRAGGING}
     * @return The state of the drawer
     */
    public int getDrawerState() {
        return mState;
    }

    public void setDrawerState(int drawerState) {
        mState=drawerState;
    }

    public boolean isEdgeDraggable() {
        return mEdgeDraggable;
    }

    boolean isHandleHit(int x, int y) {
        Rect handleHit = new Rect();
        mHandle.getHitRect(handleHit);
        Point point = mapPoint(this, new Point(x, y));
        return handleHit.contains(point.x, point.y);
    }

    private static Point mapPoint(View view, Point point) {
        Point mapped = new Point(point.x, point.y);
        Matrix matrix = view.getMatrix();
        if(!matrix.isIdentity()) {
            Matrix inverse = new Matrix();
            matrix.invert(inverse);
            float []n = { point.x, point.y };
            matrix.mapPoints(n);
            mapped.x= (int) n[0];
            mapped.y= (int) n[1];
        }
        mapped.offset(-view.getLeft(), -view.getTop());
        return mapped;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
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
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams && super.checkLayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    /**
     * Margin layout params with gravity for handle position
     */
    public static class LayoutParams extends ViewGroup.MarginLayoutParams {

        private static final int[] LAYOUT_ATTRS = new int[] {
                android.R.attr.layout_gravity
        };

        public int gravity = Gravity.NO_GRAVITY;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            final TypedArray a = c.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
            this.gravity = a.getInt(0, Gravity.NO_GRAVITY);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(int width, int height, int gravity) {
            this(width, height);
            this.gravity = gravity;
        }

        public LayoutParams(LayoutParams source) {
            super(source);
            this.gravity = source.gravity;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
        }
    }
}

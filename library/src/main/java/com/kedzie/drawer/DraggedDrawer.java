package com.kedzie.drawer;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;

/**
 * Draggable drawer with content and visible handle for all orientations.  For
 * dragging capabilities must be placed inside a {@link DragLayout}.
 *
 * @attr R.styleable#Drawer_type
 * @attr R.styleable#Drawer_handleId
 * @attr R.styleable#Drawer_contentId
 * @attr R.styleable#Drawer_edgeDraggable
 * @attr R.styleable#Drawer_shadow
 * @see DragLayout
 */
public class DraggedDrawer extends ViewGroup {
    private static final String TAG = "DraggedDrawer";

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
         * Called when drawer is starting to open.
         */
        public void onDrawerOpening();

        /**
         * Called when a drawer has settled in a completely closed state.
         */
        public void onDrawerClosed();

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
        @Override public void onDrawerSlide(float slideOffset) {}
        @Override public void onDrawerOpened() {}
        @Override public void onDrawerOpening() {}
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
     * see {@link #setDrawerListener(DrawerListener)}
     */
    DrawerListener mListener;

    /** Handle size */
    private int mHandleSize;

    private int mHandleWidth;
    private int mHandleHeight;
    private int mContentWidth;
    private int mContentHeight;
    private boolean mInLayout;

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
    /** Current state i.e. {@link #STATE_DRAGGING} {@link #STATE_IDLE} */
    int mState;
    /** Drawer is Settling to this destination offset */
    float destinationOffset;


    public DraggedDrawer(Context context) {
        super(context);
    }

    public DraggedDrawer(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Drawer, 0, 0);
        try {
            mDrawerType = a.getInt(R.styleable.Drawer_type, DRAWER_LEFT);
            mHandleId = a.getResourceId(R.styleable.Drawer_handleId, -1);
            mContentId = a.getResourceId(R.styleable.Drawer_contentId, -1);
            mShadowDrawable = a.getDrawable(R.styleable.Drawer_shadow);
            mEdgeDraggable = a.getBoolean(R.styleable.Drawer_edgeDraggable, false);
            if(mEdgeDraggable && mHandleId!=0)
                throw  new IllegalStateException("Drawer cannot have handle and be edge draggable");
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mHandle = findViewById(mHandleId);
        mContent = findViewById(mContentId);
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int wSpecMode = MeasureSpec.getMode(wSpec);
        int wSpecSize = MeasureSpec.getSize(wSpec);
        int hSpecMode = MeasureSpec.getMode(hSpec);
        int hSpecSize = MeasureSpec.getSize(hSpec);

        if(mHandle!=null) {
            measureChild(mHandle, wSpec, hSpec);
            mHandleWidth = mHandle.getMeasuredWidth();
            mHandleHeight = mHandle.getMeasuredHeight();
            mHandleSize = (mDrawerType==DRAWER_LEFT || mDrawerType==DRAWER_RIGHT) ? mHandleWidth : mHandleHeight;
        }

        int dw = mHandleWidth;
        int dh = mHandleHeight;

        if(mContent!=null) {
            switch(mDrawerType) {
                case DRAWER_BOTTOM:
                case DRAWER_TOP:
                    if(mContent.getVisibility()!=GONE) {
                        measureChild(mContent, wSpec, MeasureSpec.makeMeasureSpec(hSpecSize-mHandleHeight, hSpecMode));
                        mContentWidth = mContent.getMeasuredWidth();
                        mContentHeight = mContent.getMeasuredHeight();
                    }
                    dw += Math.max(mHandleWidth, mContentWidth);
                    dh += mContentHeight;
                    break;
                case DRAWER_LEFT:
                case DRAWER_RIGHT:
                    if(mContent.getVisibility()!=GONE) {
                        measureChild(mContent, MeasureSpec.makeMeasureSpec(wSpecSize-mHandleWidth, wSpecMode), hSpec);
                        mContentWidth = mContent.getMeasuredWidth();
                        mContentHeight = mContent.getMeasuredHeight();
                    }
                    dw += mContentWidth;
                    dh += Math.max(mHandleHeight, mContentHeight);
                    break;
            }
        }
        setMeasuredDimension(resolveSize(dw, wSpec), resolveSize(dh, hSpec));
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
            final LayoutParams lp = (LayoutParams)mHandle.getLayoutParams();
            if(mDrawerType==DRAWER_LEFT || mDrawerType==DRAWER_RIGHT) { //horizontal drawers
                switch(lp.gravity) {
                    case Gravity.TOP:
                        handleTop = lp.topMargin;
                        break;
                    case Gravity.BOTTOM:
                        handleTop = mContentHeight-mHandleHeight-lp.bottomMargin;
                        break;
                    case Gravity.CENTER_VERTICAL:
                    case Gravity.CENTER:
                    default:
                        handleTop = (mContentHeight-mHandleHeight)/2;
                        break;
                }
            } else {    //vertical drawers
                switch(lp.gravity) {
                    case Gravity.LEFT:
                        handleLeft = lp.leftMargin;
                        break;
                    case Gravity.RIGHT:
                        handleLeft = mContentWidth-mHandleWidth-lp.rightMargin;
                        break;
                    case Gravity.CENTER_HORIZONTAL:
                    case Gravity.CENTER:
                    default:
                        handleLeft = (mContentWidth-mHandleWidth)/2;
                        break;
                }
            }
        }
        switch(mDrawerType) {
            case DRAWER_LEFT:
                if(mContent!=null && mContent.getVisibility()!=GONE)
                    mContent.layout(0, 0, mContentWidth, mContentHeight);
                if(mHandle!=null)
                    mHandle.layout(mContentWidth, handleTop, mContentWidth+mHandleWidth, handleTop+mHandleHeight);
                break;
            case DRAWER_RIGHT:
                if(mHandle!=null)
                    mHandle.layout(0, handleTop, mHandleWidth, handleTop+mHandleHeight);
                if(mContent!=null && mContent.getVisibility()!=GONE)
                    mContent.layout(mHandleWidth, 0, mHandleWidth+mContentWidth, mContentHeight);
                break;
            case DRAWER_TOP:
                if(mContent!=null && mContent.getVisibility()!=GONE)
                    mContent.layout(0, 0, mContentWidth, mContentHeight);
                if(mHandle!=null)
                    mHandle.layout(handleLeft, mContentHeight, handleLeft+mHandleWidth, mContentHeight+mHandleHeight);
                break;
            case DRAWER_BOTTOM:
                if(mHandle!=null)
                    mHandle.layout(handleLeft, 0, handleLeft+mHandleWidth, mHandleHeight);
                if(mContent!=null && mContent.getVisibility()!=GONE)
                    mContent.layout(0, mHandleHeight, mContentWidth, mHandleHeight+mContentHeight);
                break;
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
    int getHandleSize() {
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
        mContent.setVisibility(visibility);
    }

    /**
     * Get the drawer handle
     * @return  drawer handle view or <code>null</code> if no handle
     */
    public View getHandle() {
        return mHandle;
    }

    public void setHandle(View handle) {
        mHandle = handle;
    }

    /**
     * Get the drawer content
     * @return  drawer content view
     */
    public View getContent() {
        return mContent;
    }

    public void setContent(View content) {
        mContent=content;
    }

    /**
     * Drawer orientation
     * @return drawer orientation, i.e. DRAWER_LEFT, DRAWER_TOP, etc.
     */
    public int getDrawerType() {
        return mDrawerType;
    }

    public void setDrawerType(int type) {
        mDrawerType=type;
    }

    /**
     * Get drawable to represent the drawer's shadow
     * @return
     */
    public Drawable getShadowDrawable() {
        return mShadowDrawable;
    }

    public void setShadowDrawable(Drawable shadow) {
        mShadowDrawable=shadow;
    }

    /**
     * Drawer state.
     * {@link #STATE_IDLE}, {@link #STATE_SETTLING} or {@link #STATE_DRAGGING}
     * @return The state of the drawer
     */
    public int getDrawerState() {
        return mState;
    }

    void setDrawerState(int drawerState) {
        mState=drawerState;
    }

    public boolean isEdgeDraggable() {
        return mEdgeDraggable;
    }

    public void setEdgeDraggable(boolean edgeDraggable) {
        mEdgeDraggable=edgeDraggable;
    }

    boolean isHandleHit(int x, int y) {
        if(mHandle==null) return false;
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

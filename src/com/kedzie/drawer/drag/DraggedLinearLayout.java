package com.kedzie.drawer.drag;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.kedzie.drawer.R;

/**
 * Draggable drawer implementation based on a linear layout
 */
public class DraggedLinearLayout extends LinearLayout {
    public static final String TAG = "DraggedLinearLayout";

    public static final int DRAWER_LEFT=1;
    public static final int DRAWER_RIGHT=2;
    public static final int DRAWER_TOP=3;
    public static final int DRAWER_BOTTOM=4;

    /** Handle size */
    private int mHandleSize;
    /** Drawer orientation */
    private int mDrawerType;

    private int mHandleId;
    private int mContentId;

    private View mHandle;
    private View mContent;

    private Drawable mShadowDrawable;


    public DraggedLinearLayout(Context context, AttributeSet attrs) {
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

    /**
     * @param context
     */
    public DraggedLinearLayout(Context context) {
        super(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mHandle = findViewById(mHandleId);
        mContent = findViewById(mContentId);
        //get the gravity from layout params
        LayoutParams handleParams = mHandle!=null ? (LayoutParams) mHandle.getLayoutParams() : null;
        removeAllViews();

        switch(mDrawerType) {
            case DRAWER_LEFT:
                setOrientation(HORIZONTAL);
                addView(mContent, new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
                if(mHandle!=null)
                    addView(mHandle, handleParams);
                break;
            case DRAWER_RIGHT:
                setOrientation(HORIZONTAL);
                if(mHandle!=null)
                    addView(mHandle, handleParams);
                addView(mContent, new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
                break;
            case DRAWER_TOP:
                setOrientation(VERTICAL);
                addView(mContent, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
                if(mHandle!=null)
                    addView(mHandle, handleParams);
                break;
            case DRAWER_BOTTOM:
                setOrientation(VERTICAL);
                if(mHandle!=null)
                    addView(mHandle, handleParams);
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
}

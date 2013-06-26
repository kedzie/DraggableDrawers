package com.kedzie.drawer.drag;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.kedzie.drawer.R;

public class DraggedView extends View {

    private int mHandleSize;
    private int mDrawerType;

    private int mHandleId;
    private int mContentId;
    private int mShadowId;

    public static final int DRAWER_LEFT=1;
    public static final int DRAWER_RIGHT=2;
    public static final int DRAWER_TOP=3;
    public static final int DRAWER_BOTTOM=4;

    private Rect mBounds;
    private Paint handlePaint = new Paint();

    public DraggedView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Drawer, 0, 0);
        try {
            mHandleSize = a.getDimensionPixelSize(R.styleable.Drawer_handle_size, 0);
            mDrawerType = a.getInt(R.styleable.Drawer_type, 0);
            mHandleId = a.getInt(R.styleable.Drawer_handle, 0);
            mContentId = a.getInt(R.styleable.Drawer_content, 0);
            mShadowId = a.getInt(R.styleable.Drawer_shadow, 0);
        } finally {
            a.recycle();
        }
        handlePaint.setColor(Color.BLUE);
        handlePaint.setStrokeWidth(0f);
    }

    /**
     * @param context
     */
    public DraggedView(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        switch(mDrawerType) {
            case DRAWER_LEFT:
                canvas.drawRect(getWidth()-mHandleSize, 0, getWidth(), getHeight(), handlePaint);
                break;
            case DRAWER_TOP:
                canvas.drawRect(0, getHeight()-mHandleSize, getWidth(), getHeight(), handlePaint);
                break;
            case DRAWER_BOTTOM:
                canvas.drawRect(0, 0, getWidth(), mHandleSize, handlePaint);
                break;
        }

    }

    public int getHandleSize() {
        return mHandleSize;
    }

    public int getDrawerType() {
        return mDrawerType;
    }
}

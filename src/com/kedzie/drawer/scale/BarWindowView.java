package com.kedzie.drawer.scale;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.FrameLayout;

import com.kedzie.drawer.R;


public class BarWindowView extends FrameLayout
{
    public static final String TAG = "StatusBarWindowView";
    
    public static final boolean DEBUG = true;

    private TopBarView mTopBarView;
    private Paint mDebugPaint;

    public BarWindowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setMotionEventSplittingEnabled(false);
        setWillNotDraw(!DEBUG);
        mDebugPaint = new Paint();
        mDebugPaint.setColor(0xFFFFFF00);
        mDebugPaint.setStrokeWidth(18.0f);
        mDebugPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onAttachedToWindow () {
        super.onAttachedToWindow();
        mTopBarView = (TopBarView)findViewById(R.id.status_bar);
    }

    @Override
    public void dispatchWindowFocusChanged(boolean hasFocus) {
        this.setFocusableInTouchMode(hasFocus);
        this.requestFocus();
        super.dispatchWindowFocusChanged(hasFocus);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        switch (event.getKeyCode()) {
        case KeyEvent.KEYCODE_BACK:
            if (!down && !mTopBarView.isAllPanelsCollapsed()) {
                mTopBarView.collapseAllPanels(true);
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (DEBUG) {
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mDebugPaint);
        }
    }
}


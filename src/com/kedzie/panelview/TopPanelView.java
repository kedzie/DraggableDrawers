package com.kedzie.panelview;

import com.kedzie.panel.PanelBar;
import com.kedzie.panel.PanelView;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

public class TopPanelView extends PanelView {

    Drawable mHandleBar;
    int mHandleBarHeight;
    View mHandleView;
    
    public TopPanelView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        Resources resources = getContext().getResources();
        mHandleBar = resources.getDrawable(R.drawable.status_bar_close);
        mHandleBarHeight = resources.getDimensionPixelSize(R.dimen.close_handle_height);
        mHandleView = findViewById(R.id.handle);
    }

    @Override
    public void setBar(PanelBar panelBar) {
        super.setBar(panelBar);
    }

    @Override
    public void fling(float vel, boolean always) {
//        GestureRecorder gr = ((PhoneStatusBarView) mBar).mBar.getGestureRecorder();
//        if (gr != null) {
//            gr.tag(
//                "fling " + ((vel > 0) ? "open" : "closed"),
//                "settings,v=" + vel);
//        }
        super.fling(vel, always);
    }

    // We draw the handle ourselves so that it's always glued to the bottom of the window.
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            final int pl = getPaddingLeft();
            final int pr = getPaddingRight();
            mHandleBar.setBounds(pl, 0, getWidth() - pr, (int) mHandleBarHeight);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        final int off = (int) (getHeight() - mHandleBarHeight - getPaddingBottom());
        canvas.translate(0, off);
        mHandleBar.setState(mHandleView.getDrawableState());
        mHandleBar.draw(canvas);
        canvas.translate(0, -off);
    }

}

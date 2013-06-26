package com.kedzie.drawer.scale;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import com.kedzie.drawer.scale.panel.PanelBar;
import com.kedzie.drawer.scale.panel.PanelView;
import com.kedzie.drawer.R;

public class TopBarView extends PanelBar {
    private static final String TAG = "PhoneStatusBarView";

    private int mScrimColor;
    
    private BarWindowView mBarWindow;

    boolean mFullWidthNotifications;
    private PanelView mFadingPanel = null;
    private PanelView mLastFullyOpenedPanel = null;
    private boolean mShouldFade;
    
    private boolean mAllCollapsed = true;

    public TopBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mScrimColor = getContext().getResources().getColor(R.color.scrim);
    }
    
    public void setBarWindowView(BarWindowView view) {
    	mBarWindow = view;
    }
    
    public boolean isAllPanelsCollapsed() {
    	return mAllCollapsed;
    }

    @Override
    public void startOpeningPanel(PanelView panel) {
        super.startOpeningPanel(panel);
        // we only want to start fading if this is the "first" or "last" panel,
        // which is kind of tricky to determine
        mShouldFade = (mFadingPanel == null || mFadingPanel.isFullyExpanded());
        if (DEBUG) {
            Log.v(TAG, "start opening: " + panel + " shouldfade=" + mShouldFade);
        }
        mFadingPanel = panel;
    }

    @Override
    public void onAllPanelsCollapsed() {
        super.onAllPanelsCollapsed();
        mFadingPanel = null;
        mLastFullyOpenedPanel = null;
        mAllCollapsed = true;
    }

    @Override
    public void onPanelFullyOpened(PanelView openPanel) {
        super.onPanelFullyOpened(openPanel);
        mAllCollapsed = false;
        mFadingPanel = openPanel;
        mLastFullyOpenedPanel = openPanel;
        mShouldFade = true; // now you own the fade, mister
    }

    @Override
    public void panelExpansionChanged(PanelView panel, float frac) {
        super.panelExpansionChanged(panel, frac);

        if (DEBUG) {
            Log.v(TAG, "panelExpansionChanged: f=" + frac);
        }

        if (panel == mFadingPanel && mScrimColor != 0) {
            if (mShouldFade) {
                frac = mPanelExpandedFractionSum; // don't judge me
                // let's start this 20% of the way down the screen
                frac = frac * 1.2f - 0.2f;
                if (frac <= 0) {
                    mBarWindow.setBackgroundColor(0);
                } else {
                    // woo, special effects
                    final float k = (float)(1f-0.5f*(1f-Math.cos(3.14159f * Math.pow(1f-frac, 2f))));
                    // attenuate background color alpha by k
                    final int color = (int) ((mScrimColor >>> 24) * k) << 24 | (mScrimColor & 0xFFFFFF);
                    mBarWindow.setBackgroundColor(color);
                }
            }
        }

        // fade out the panel as it gets buried into the status bar to avoid overdrawing the
        // status bar on the last frame of a close animation
        final int H = getHeight();
        final float ph = panel.getExpandedHeight() + panel.getPaddingBottom();
        float alpha = 1f;
        if (ph < 2*H) {
            if (ph < H) alpha = 0f;
            else alpha = (ph - H) / H;
            alpha = alpha * alpha; // get there faster
        }
        if (panel.getAlpha() != alpha) {
            panel.setAlpha(alpha);
        }
    }
}

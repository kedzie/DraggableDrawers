
package com.kedzie.panelview;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;

import com.kedzie.panel.PanelHolder;

public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";

    BarWindowView mBarWindowView;
    TopBarView mTopBarView;
    TopPanelView mTopPanel;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mBarWindowView = (BarWindowView) LayoutInflater.from(this).inflate(R.layout.super_status_bar, null);

        setContentView(mBarWindowView);

        mTopBarView = (TopBarView) mBarWindowView.findViewById(R.id.status_bar);

        PanelHolder holder = (PanelHolder) mBarWindowView.findViewById(R.id.panel_holder);
        mTopBarView.setPanelHolder(holder);
        mTopBarView.setBarWindowView(mBarWindowView);

        mTopPanel = (TopPanelView) mBarWindowView.findViewById(R.id.top_panel);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    		case R.id.action_expand:
    			Log.d(TAG, "Expanding");
    			return true;
    		case R.id.action_collapse:
    			Log.d(TAG, "Collapsing");
    			mTopBarView.collapseAllPanels(true);
    			return true;
    	}
    	return false;
    }
}

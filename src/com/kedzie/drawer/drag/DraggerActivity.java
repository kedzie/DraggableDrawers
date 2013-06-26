
package com.kedzie.drawer.drag;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.widget.ViewDragHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import com.kedzie.drawer.R;

public class DraggerActivity extends Activity {
	private static final String TAG = "DraggerActivity";

    private DragLayout mLayout;
    private DraggedView leftDrawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.drawer_layout);

        mLayout = (DragLayout)findViewById(R.id.drawer_layout);

        leftDrawer = (DraggedView)findViewById(R.id.left);
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
                mLayout.openDrawer(leftDrawer);
    			return true;
    		case R.id.action_collapse:
    			Log.d(TAG, "Collapsing");
                mLayout.closeDrawer(leftDrawer);
    			return true;
    	}
    	return false;
    }

    }
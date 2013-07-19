
package com.kedzie.drawer.sample;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.kedzie.drawer.DragLayout;
import com.kedzie.drawer.DraggedDrawer;

public class DraggerActivity extends Activity {
    private static final String TAG = "DraggerActivity";

    private DragLayout mLayout;
    private DraggedDrawer leftDrawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.drawer_layout);

        mLayout = (DragLayout)findViewById(R.id.drawer_layout);
//        mLayout.setDrawerListener(new DragLayout.DrawerListener() {
//            @Override
//            public void onDrawerSlide(View drawerView, float slideOffset) {
//            }
//
//            @Override
//            public void onDrawerOpened(View drawerView) {
//                Log.d(TAG, "onDrawerOpened: " + drawerView);
//            }
//
//            @Override
//            public void onDrawerOpening(View drawerView) {
//                Log.d(TAG, "onDrawerOpened: " + drawerView);
//            }
//
//            @Override
//            public void onDrawerClosed(View drawerView) {
//                Log.d(TAG, "onDrawerClosed: " + drawerView);
//            }
//
//            @Override
//            public void onDrawerStateChanged(int newState) {
//                Log.d(TAG, "onDrawerStateChanged: " + newState);
//            }
//        });

        leftDrawer = (DraggedDrawer)findViewById(R.id.left);
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
                mLayout.closeAllDrawers();
                return true;
        }
        return false;

    }
}

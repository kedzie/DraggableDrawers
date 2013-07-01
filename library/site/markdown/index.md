#DraggableDrawers

Drag drawers with handles in any direction.

##Usage

Drawers must be enclosed in a DragLayout.  Each drawer is defined with a content view, an optional handle, and choice of direction.  For example:

```xml
<?xml version="1.0" encoding="UTF-8"?>

<com.kedzie.drawer.DragLayout
        xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:kedzie="http://schemas.android.com/apk/res-auto"
        android:id="@+id/drawer_layout"
        kedzie:scrim_color="@color/drawer_scrim"
        android:layout_width="match_parent"
        android:layout_height="match_parent">

    <com.kedzie.drawer.DraggedDrawer
            android:id="@+id/left"
            android:layout_width="400dp"
            android:layout_height="400dp"
            kedzie:handle="@+id/left_handle"
            kedzie:content="@+id/left_content"
            kedzie:type="left"
            kedzie:shadow="@drawable/shadow_grad_left"
            android:layout_marginTop="24dp">

        <RelativeLayout
                android:id="@id/left_content"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:background="#FF0000FF">
            <TextView android:text="LEFT"
                      android:layout_centerInParent="true"
                      android:layout_width="wrap_content"
                      android:layout_height="wrap_content"/>
        </RelativeLayout>

        <ImageView android:id="@id/left_handle"
                   android:src="@drawable/drawer_handle_left_closed"
                   android:layout_gravity="top"
                   android:layout_marginTop="14dp"
                   android:layout_width="wrap_content"
                   android:layout_height="wrap_content"/>

    </com.kedzie.drawer.DraggedDrawer>

    <com.kedzie.drawer.DraggedDrawer
            android:id="@+id/bottom"
            android:layout_width="400dp"
            android:layout_height="250dp"
            kedzie:handle="@+id/bottom_handle"
            kedzie:content="@+id/bottom_content"
            kedzie:type="bottom"
            kedzie:shadow="@drawable/shadow_grad_bottom"
            android:layout_alignParentBottom="true"
            android:layout_alignParentRight="true"
            android:layout_marginRight="18dp">

        <RelativeLayout
                android:id="@id/bottom_content"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:background="#FF0000FF">
            <TextView android:text="BOTTOM"
                      android:layout_centerInParent="true"
                      android:layout_width="wrap_content"
                      android:layout_height="wrap_content"/>
        </RelativeLayout>

        <ImageView android:id="@id/bottom_handle"
                   android:background="@drawable/handle"
                   android:layout_width="100dp"
                   android:layout_height="24dp"
                   android:layout_gravity="center"/>

    </com.kedzie.drawer.DraggedDrawer>

    <com.kedzie.drawer.DraggedDrawer
            android:id="@+id/top"
            android:layout_width="300dp"
            android:layout_height="300dp"
            kedzie:handle="@+id/top_handle"
            kedzie:content="@+id/top_content"
            kedzie:type="top"
            kedzie:shadow="@drawable/shadow_grad_top"
            android:layout_alignParentTop="true"
            android:layout_alignParentRight="true">

        <RelativeLayout
                android:id="@id/top_content"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:background="#FF0000FF">
            <TextView android:text="TOP"
                      android:layout_centerInParent="true"
                      android:layout_width="wrap_content"
                      android:layout_height="wrap_content"/>
        </RelativeLayout>

        <ImageView android:id="@id/top_handle"
                   android:background="@drawable/handle"
                   android:layout_gravity="center"
                   android:layout_width="200dp"
                   android:layout_height="30dp"/>

    </com.kedzie.drawer.DraggedDrawer>

</com.kedzie.drawer.DragLayout>
```

Look at the sample application for a complete example.

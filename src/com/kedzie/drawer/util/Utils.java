package com.kedzie.drawer.util;

import android.widget.Toast;

import static android.view.MotionEvent.*;

public class Utils {

	/**
     * Returns a string that represents the symbolic name of the specified action
     * such as "ACTION_DOWN", "ACTION_POINTER_DOWN(3)" or an equivalent numeric constant
     * such as "35" if unknown.
     *
     * @param action The action.
     * @return The symbolic name of the specified action.
     * @hide
     */
    public static String motionActionToString(int action) {
        switch (action) {
            case ACTION_DOWN:
                return "ACTION_DOWN";
            case ACTION_UP:
                return "ACTION_UP";
            case ACTION_CANCEL:
                return "ACTION_CANCEL";
            case ACTION_OUTSIDE:
                return "ACTION_OUTSIDE";
            case ACTION_MOVE:
                return "ACTION_MOVE";
            case ACTION_HOVER_MOVE:
                return "ACTION_HOVER_MOVE";
            case ACTION_SCROLL:
                return "ACTION_SCROLL";
            case ACTION_HOVER_ENTER:
                return "ACTION_HOVER_ENTER";
            case ACTION_HOVER_EXIT:
                return "ACTION_HOVER_EXIT";
        }
        int index = (action & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT;
        switch (action & ACTION_MASK) {
            case ACTION_POINTER_DOWN:
                return "ACTION_POINTER_DOWN(" + index + ")";
            case ACTION_POINTER_UP:
                return "ACTION_POINTER_UP(" + index + ")";
            default:
                return Integer.toString(action);
        }
    }

}

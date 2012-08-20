package com.seeedstudio.android.ir;

import android.util.Log;

public class Utility {
    // debugging switch
    public static boolean DEBUG = true;

    // log
    public static void logging(String tag, String text) {
        Log.d(tag, "+++>>" + text + "<<+++");
    }

}

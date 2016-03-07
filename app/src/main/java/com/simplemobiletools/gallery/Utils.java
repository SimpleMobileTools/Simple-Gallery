package com.simplemobiletools.gallery;

import android.content.Context;
import android.widget.Toast;

public class Utils {
    public static String getFilename(final String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }

    public static void showToast(Context context, int resId) {
        Toast.makeText(context, context.getResources().getString(resId), Toast.LENGTH_SHORT).show();
    }
}

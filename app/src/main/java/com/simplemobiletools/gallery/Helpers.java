package com.simplemobiletools.gallery;

public class Helpers {
    public static String getFilename(final String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }
}

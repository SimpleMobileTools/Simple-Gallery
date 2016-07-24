package com.simplemobiletools.gallery.models;

import com.simplemobiletools.gallery.Constants;

import java.io.Serializable;

public class Medium implements Serializable, Comparable {
    private static final long serialVersionUID = -6543139465975455L;
    private final String mPath;
    private final boolean mIsVideo;
    private final long mTimestamp;
    private final long mSize;
    public static int mOrder;

    public Medium(String path, boolean isVideo, long timestamp, long size) {
        mPath = path;
        mIsVideo = isVideo;
        mTimestamp = timestamp;
        mSize = size;
    }

    public String getPath() {
        return mPath;
    }

    public boolean getIsVideo() {
        return mIsVideo;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public long getSize() {
        return mSize;
    }

    public boolean isGif() {
        return getPath().endsWith(".gif");
    }

    @Override
    public int compareTo(Object object) {
        final Medium medium = (Medium) object;
        int res;
        if ((mOrder & Constants.SORT_BY_NAME) == Constants.SORT_BY_NAME) {
            res = mPath.compareTo(medium.getPath());
        } else if ((mOrder & Constants.SORT_BY_DATE) == Constants.SORT_BY_DATE) {
            res = (mTimestamp > medium.getTimestamp()) ? 1 : -1;
        } else {
            res = (mSize > medium.getSize()) ? 1 : -1;
        }

        if ((mOrder & Constants.SORT_DESCENDING) == Constants.SORT_DESCENDING) {
            res *= -1;
        }
        return res;
    }

    @Override
    public String toString() {
        return "Medium {" +
                "isVideo=" + getIsVideo() +
                ", timestamp=" + getTimestamp() +
                ", path=" + getPath() + "}";
    }
}

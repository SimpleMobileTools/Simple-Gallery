package com.simplemobiletools.gallery.models;

import com.simplemobiletools.gallery.Constants;

import java.io.Serializable;

public class Medium implements Serializable, Comparable {
    private static final long serialVersionUID = -6543139465975455L;
    private final String mPath;
    private final boolean mIsVideo;
    private final long mTimestamp;
    private final long mSize;
    public static int mSorting;

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

    public boolean isImage() {
        return !isGif() && !getIsVideo();
    }

    @Override
    public int compareTo(Object object) {
        final Medium medium = (Medium) object;
        int res;
        if ((mSorting & Constants.SORT_BY_NAME) != 0) {
            res = mPath.compareTo(medium.getPath());
        } else if ((mSorting & Constants.SORT_BY_DATE) != 0) {
            res = (mTimestamp > medium.getTimestamp()) ? 1 : -1;
        } else {
            res = (mSize > medium.getSize()) ? 1 : -1;
        }

        if ((mSorting & Constants.SORT_DESCENDING) != 0) {
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

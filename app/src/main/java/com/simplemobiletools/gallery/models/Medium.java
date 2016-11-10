package com.simplemobiletools.gallery.models;

import com.simplemobiletools.gallery.Constants;

import java.io.Serializable;

public class Medium implements Serializable, Comparable {
    private static final long serialVersionUID = -6543139465975455L;
    private final String mName;
    private final boolean mIsVideo;
    private final long mTimestamp;
    private final long mSize;
    public static int mSorting;
    private String mPath;

    public Medium(String name, String path, boolean isVideo, long timestamp, long size) {
        mName = name;
        mPath = path;
        mIsVideo = isVideo;
        mTimestamp = timestamp;
        mSize = size;
    }

    public String getName() {
        return mName;
    }

    public void setPath(String path) {
        mPath = path;
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
        return getPath().toLowerCase().endsWith(".gif");
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
                ", size=" + getSize() +
                ", path=" + getPath() + "}";
    }
}

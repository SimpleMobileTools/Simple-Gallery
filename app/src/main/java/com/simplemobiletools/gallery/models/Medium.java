package com.simplemobiletools.gallery.models;

import java.io.Serializable;

public class Medium implements Serializable, Comparable {
    private static final long serialVersionUID = -6543139465975455L;
    private final String mPath;
    private final boolean mIsVideo;
    private final long mTimestamp;

    public Medium(String path, boolean isVideo, long timestamp) {
        mPath = path;
        mIsVideo = isVideo;
        mTimestamp = timestamp;
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

    public boolean isGif() {
        return getPath().endsWith(".gif");
    }

    @Override
    public int compareTo(Object object) {
        final Medium medium = (Medium) object;
        if (mTimestamp < medium.getTimestamp()) {
            return 1;
        } else if (mTimestamp > medium.getTimestamp()) {
            return -1;
        }
        return 0;
    }

    @Override
    public String toString() {
        return "Medium {" +
                "isVideo=" + getIsVideo() +
                ", timestamp=" + getTimestamp() +
                ", path=" + getPath() + "}";
    }
}

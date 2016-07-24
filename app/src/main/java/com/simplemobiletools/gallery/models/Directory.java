package com.simplemobiletools.gallery.models;

import com.simplemobiletools.gallery.Constants;

public class Directory implements Comparable {
    private final String mPath;
    private final String mThumbnail;
    private final String mName;
    private final long mTimestamp;
    private int mMediaCnt;
    private long mSize;
    public static int mSorting;

    public Directory(String path, String thumbnail, String name, int mediaCnt, long timestamp, long size) {
        mPath = path;
        mThumbnail = thumbnail;
        mName = name;
        mMediaCnt = mediaCnt;
        mTimestamp = timestamp;
        mSize = size;
    }

    public String getPath() {
        return mPath;
    }

    public String getThumbnail() {
        return mThumbnail;
    }

    public String getName() {
        return mName;
    }

    public int getMediaCnt() {
        return mMediaCnt;
    }

    public void setMediaCnt(int cnt) {
        mMediaCnt = cnt;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public long getSize() {
        return mSize;
    }

    public void addSize(long bytes) {
        mSize += bytes;
    }

    @Override
    public int compareTo(Object object) {
        final Directory directory = (Directory) object;
        int res;
        if ((mSorting & Constants.SORT_BY_NAME) != 0) {
            res = mPath.compareTo(directory.getPath());
        } else if ((mSorting & Constants.SORT_BY_DATE) != 0) {
            res = (mTimestamp > directory.getTimestamp()) ? 1 : -1;
        } else {
            res = (mSize > directory.getSize()) ? 1 : -1;
        }

        if ((mSorting & Constants.SORT_DESCENDING) != 0) {
            res *= -1;
        }
        return res;
    }

    @Override
    public String toString() {
        return "Directory {" +
                "path=" + getPath() +
                ", thumbnail=" + getThumbnail() +
                ", name=" + getName() +
                ", timestamp=" + getTimestamp() +
                ", mediaCnt=" + getMediaCnt() + "}";
    }
}

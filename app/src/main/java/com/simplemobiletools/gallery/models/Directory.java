package com.simplemobiletools.gallery.models;

public class Directory implements Comparable {
    private final String mPath;
    private final String mThumbnail;
    private final String mName;
    private final long mTimestamp;
    private int mMediaCnt;
    private long mBytes;

    public Directory(String path, String thumbnail, String name, int mediaCnt, long timestamp, long size) {
        mPath = path;
        mThumbnail = thumbnail;
        mName = name;
        mMediaCnt = mediaCnt;
        mTimestamp = timestamp;
        mBytes = size;
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
        return mBytes;
    }

    public void addSize(long bytes) {
        mBytes += bytes;
    }

    @Override
    public int compareTo(Object object) {
        final Directory directory = (Directory) object;
        if (mTimestamp < directory.getTimestamp()) {
            return 1;
        } else if (mTimestamp > directory.getTimestamp()) {
            return -1;
        }
        return 0;
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

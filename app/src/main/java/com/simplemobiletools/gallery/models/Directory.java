package com.simplemobiletools.gallery.models;

public class Directory implements Comparable {
    private final String path;
    private final String thumbnail;
    private final String name;
    private final long timestamp;
    private int mediaCnt;

    public Directory(String path, String thumbnail, String name, int mediaCnt, long timestamp) {
        this.path = path;
        this.thumbnail = thumbnail;
        this.name = name;
        this.mediaCnt = mediaCnt;
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public String getName() {
        return name;
    }

    public int getMediaCnt() {
        return mediaCnt;
    }

    public void setMediaCnt(int cnt) {
        mediaCnt = cnt;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public int compareTo(Object object) {
        final Directory directory = (Directory) object;
        if (this.timestamp < directory.getTimestamp()) {
            return 1;
        } else if (this.timestamp > directory.getTimestamp()) {
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

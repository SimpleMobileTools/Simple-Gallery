package com.simplemobiletools.gallery.models;

import java.io.Serializable;

public class Medium implements Serializable, Comparable {
    private static final long serialVersionUID = -6543139465975455L;
    private final String path;
    private final boolean isVideo;
    private final long timestamp;

    public Medium(String path, boolean isVideo, long timestamp) {
        this.path = path;
        this.isVideo = isVideo;
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public boolean getIsVideo() {
        return isVideo;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isGif() {
        return getPath().endsWith(".gif");
    }

    @Override
    public int compareTo(Object object) {
        final Medium medium = (Medium) object;
        if (this.timestamp < medium.getTimestamp()) {
            return 1;
        } else if (this.timestamp > medium.getTimestamp()) {
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

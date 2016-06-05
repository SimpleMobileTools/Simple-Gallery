package com.simplemobiletools.gallery;

public class Media {
    private final String path;
    private final boolean isVideo;

    public Media(String path, boolean isVideo) {
        this.path = path;
        this.isVideo = isVideo;
    }

    public String getPath() {
        return path;
    }

    public boolean getIsVideo() {
        return isVideo;
    }
}

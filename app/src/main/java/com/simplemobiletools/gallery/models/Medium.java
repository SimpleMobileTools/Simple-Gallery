package com.simplemobiletools.gallery.models;

import java.io.Serializable;

public class Medium implements Serializable {
    private static final long serialVersionUID = -6543139465975455L;
    private final String path;
    private final boolean isVideo;

    public Medium(String path, boolean isVideo) {
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

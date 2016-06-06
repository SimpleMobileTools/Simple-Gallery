package com.simplemobiletools.gallery;

import java.io.Serializable;

public class Media implements Serializable {
    private static final long serialVersionUID = -6543139465975455L;
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

package com.simplemobiletools.gallery.models;

public class Directory {
    private final String path;
    private final String thumbnail;
    private final String name;
    private int mediaCnt;

    public Directory(String path, String thumbnail, String name, int mediaCnt) {
        this.path = path;
        this.thumbnail = thumbnail;
        this.name = name;
        this.mediaCnt = mediaCnt;
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
}

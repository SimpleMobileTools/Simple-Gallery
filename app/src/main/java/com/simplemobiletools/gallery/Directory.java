package com.simplemobiletools.gallery;

public class Directory {
    private final String path;
    private final String thumbnail;
    private final String name;
    private int photoCnt;

    public Directory(String path, String thumbnail, String name, int photoCnt) {
        this.path = path;
        this.thumbnail = thumbnail;
        this.name = name;
        this.photoCnt = photoCnt;
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

    public int getPhotoCnt() {
        return photoCnt;
    }

    public void setPhotoCnt(int cnt) {
        photoCnt = cnt;
    }
}

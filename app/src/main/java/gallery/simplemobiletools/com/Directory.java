package gallery.simplemobiletools.com;

public class Directory {
    private final String thumbnail;
    private final String name;
    private int photoCnt;

    public Directory(String thumbnail, String name, int photoCnt) {
        this.thumbnail = thumbnail;
        this.name = name;
        this.photoCnt = photoCnt;
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

package gallery.simplemobiletools.com;

public class Directory {
    private final String path;
    private final String name;
    private final int photoCnt;

    public Directory(String path, String name, int photoCnt) {
        this.path = path;
        this.name = name;
        this.photoCnt = photoCnt;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public int getPhotoCnt() {
        return photoCnt;
    }
}

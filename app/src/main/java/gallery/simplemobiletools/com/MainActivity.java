package gallery.simplemobiletools.com;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.widget.GridView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {
    private List<Directory> dirs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Map<String, Integer> directories = getImages();
        dirs = new ArrayList<>(directories.size());

        for (Map.Entry<String, Integer> dir : directories.entrySet()) {
            final String path = dir.getKey();
            final String name = path.substring(path.lastIndexOf("/") + 1);
            final String cnt = String.valueOf(dir.getValue());
            dirs.add(new Directory(path, name, cnt));
        }

        final GridView gridView = (GridView) findViewById(R.id.photo_grid);
        DirectoryAdapter adapter = new DirectoryAdapter(this, dirs);
        gridView.setAdapter(adapter);
    }

    private Map<String, Integer> getImages() {
        final Map<String, Integer> dirs = new TreeMap<>();
        final Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        final Cursor cursor = getContentResolver().query(uri, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            final int pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            do {
                final File file = new File(cursor.getString(pathIndex));
                final String path = file.getParent().toLowerCase();
                if (dirs.containsKey(path)) {
                    dirs.put(path, dirs.get(path) + 1);
                } else {
                    dirs.put(path, 1);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        return dirs;
    }
}

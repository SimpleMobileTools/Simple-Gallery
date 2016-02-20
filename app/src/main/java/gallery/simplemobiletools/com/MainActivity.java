package gallery.simplemobiletools.com;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity implements OnItemClickListener {
    private List<Directory> dirs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final GridView gridView = (GridView) findViewById(R.id.photo_grid);

        dirs = new ArrayList<>(getDirectories().values());
        final DirectoryAdapter adapter = new DirectoryAdapter(this, dirs);
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(this);
    }

    private Map<String, Directory> getDirectories() {
        final Map<String, Directory> directories = new TreeMap<>();
        final Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        final String[] columns = {MediaStore.Images.Media.DATA};
        final Cursor cursor = getContentResolver().query(uri, columns, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            final int pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            do {
                final File file = new File(cursor.getString(pathIndex));
                final String fileDir = file.getParent().toLowerCase();

                if (directories.containsKey(fileDir)) {
                    final Directory directory = directories.get(fileDir);
                    final int newImageCnt = directory.getPhotoCnt() + 1;
                    directory.setPhotoCnt(newImageCnt);
                } else {
                    final String thumbnail = file.getAbsolutePath();
                    final String dirName = fileDir.substring(fileDir.lastIndexOf("/") + 1);
                    directories.put(fileDir, new Directory(fileDir, thumbnail, dirName, 1));
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        return directories;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Intent intent = new Intent(this, PhotosActivity.class);
        intent.putExtra(Constants.DIRECTORY, dirs.get(position).getPath());
        startActivity(intent);
    }
}

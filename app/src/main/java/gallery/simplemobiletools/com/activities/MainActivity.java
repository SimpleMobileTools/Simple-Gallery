package gallery.simplemobiletools.com.activities;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import gallery.simplemobiletools.com.Constants;
import gallery.simplemobiletools.com.Directory;
import gallery.simplemobiletools.com.R;
import gallery.simplemobiletools.com.adapters.DirectoryAdapter;

public class MainActivity extends AppCompatActivity implements OnItemClickListener {
    private List<Directory> dirs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final GridView gridView = (GridView) findViewById(R.id.directories_grid);

        dirs = new ArrayList<>(getDirectories().values());
        final DirectoryAdapter adapter = new DirectoryAdapter(this, dirs);
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(this);
    }

    private Map<String, Directory> getDirectories() {
        final Map<String, Directory> directories = new LinkedHashMap<>();
        final Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        final String[] columns = {MediaStore.Images.Media.DATA};
        final String order = MediaStore.Images.Media.DATE_MODIFIED + " DESC";
        final Cursor cursor = getContentResolver().query(uri, columns, null, null, order);

        if (cursor != null && cursor.moveToFirst()) {
            final int pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            do {
                final String path = cursor.getString(pathIndex);
                final File file = new File(path);
                final String fileDir = file.getParent().toLowerCase();

                if (directories.containsKey(fileDir)) {
                    final Directory directory = directories.get(fileDir);
                    final int newImageCnt = directory.getPhotoCnt() + 1;
                    directory.setPhotoCnt(newImageCnt);
                } else {
                    final String dirName = fileDir.substring(fileDir.lastIndexOf("/") + 1);
                    directories.put(fileDir, new Directory(fileDir, path, dirName, 1));
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

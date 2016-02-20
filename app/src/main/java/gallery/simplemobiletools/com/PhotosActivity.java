package gallery.simplemobiletools.com;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import java.util.ArrayList;
import java.util.List;

public class PhotosActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photos);

        final GridView gridView = (GridView) findViewById(R.id.photos_grid);
        final PhotosAdapter adapter = new PhotosAdapter(this, getPhotos());
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(this);
    }

    private List<String> getPhotos() {
        final List<String> photos = new ArrayList<>();
        final String path = getIntent().getStringExtra(Constants.DIRECTORY);
        final Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        final String where = MediaStore.Images.Media.DATA + " like ? ";
        final String[] args = new String[]{path + "%"};
        final String[] columns = {MediaStore.Images.Media.DATA};
        final Cursor cursor = getContentResolver().query(uri, columns, where, args, null);

        if (cursor != null && cursor.moveToFirst()) {
            final int pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            do {
                photos.add(cursor.getString(pathIndex));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return photos;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }
}

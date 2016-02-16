package gallery.simplemobiletools.com;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getImages();
    }

    private void getImages() {
        final ContentResolver contentResolver = getContentResolver();
        final Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        final Cursor cursor = contentResolver.query(uri, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            final int pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            do {
                final String path = cursor.getString(pathIndex);
            } while (cursor.moveToNext());
            cursor.close();
        }
    }
}

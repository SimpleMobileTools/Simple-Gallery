package gallery.simplemobiletools.com;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PhotoActivity extends AppCompatActivity {
    private int pos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        pos = 0;
        final ViewPager pager = (ViewPager) findViewById(R.id.view_pager);
        final MyPagerAdapter adapter = new MyPagerAdapter(this, getPhotos());
        pager.setAdapter(adapter);
        pager.setCurrentItem(pos);
    }

    private List<String> getPhotos() {
        final List<String> photos = new ArrayList<>();
        final String path = getIntent().getStringExtra(Constants.PHOTO);
        final String fileDir = new File(path).getParent().toLowerCase();
        final Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        final String where = MediaStore.Images.Media.DATA + " like ? ";
        final String[] args = new String[]{fileDir + "%"};
        final String[] columns = {MediaStore.Images.Media.DATA};
        final Cursor cursor = getContentResolver().query(uri, columns, where, args, null);

        int i = 0;
        if (cursor != null && cursor.moveToFirst()) {
            final int pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            do {
                final String curPath = cursor.getString(pathIndex);
                photos.add(curPath);

                if (curPath.equals(path))
                    pos = i;

                i++;
            } while (cursor.moveToNext());
            cursor.close();
        }
        return photos;
    }
}

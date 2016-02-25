package com.simplemobiletools.gallery.activities;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import com.simplemobiletools.gallery.Constants;
import com.simplemobiletools.gallery.Helpers;
import com.simplemobiletools.gallery.R;
import com.simplemobiletools.gallery.adapters.PhotosAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PhotosActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private List<String> photos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photos);

        photos = new ArrayList<>();
        final String path = getIntent().getStringExtra(Constants.DIRECTORY);
        final GridView gridView = (GridView) findViewById(R.id.photos_grid);
        final PhotosAdapter adapter = new PhotosAdapter(this, getPhotos(path));
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(this);

        final String dirName = Helpers.getFilename(path);
        setTitle(dirName);
    }

    private List<String> getPhotos(final String path) {
        final Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        final String where = MediaStore.Images.Media.DATA + " like ? ";
        final String[] args = new String[]{path + "%"};
        final String[] columns = {MediaStore.Images.Media.DATA};
        final String order = MediaStore.Images.Media.DATE_MODIFIED + " DESC";
        final Cursor cursor = getContentResolver().query(uri, columns, where, args, order);
        final String pattern = Pattern.quote(path) + "/[^/]*";

        if (cursor != null && cursor.moveToFirst()) {
            final int pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            do {
                final String curPath = cursor.getString(pathIndex);
                if (curPath.matches(pattern)) {
                    photos.add(cursor.getString(pathIndex));
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        return photos;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Intent intent = new Intent(this, ViewPagerActivity.class);
        intent.putExtra(Constants.PHOTO, photos.get(position));
        startActivity(intent);
    }
}

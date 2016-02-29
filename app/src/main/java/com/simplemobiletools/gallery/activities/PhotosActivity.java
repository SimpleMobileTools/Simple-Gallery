package com.simplemobiletools.gallery.activities;

import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import com.simplemobiletools.gallery.Constants;
import com.simplemobiletools.gallery.Helpers;
import com.simplemobiletools.gallery.R;
import com.simplemobiletools.gallery.adapters.PhotosAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PhotosActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, GridView.MultiChoiceModeListener {
    private List<String> photos;
    private int selectedItemsCnt;
    private GridView gridView;
    private String path;
    private PhotosAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photos);

        path = getIntent().getStringExtra(Constants.DIRECTORY);
        photos = getPhotos();
        adapter = new PhotosAdapter(this, photos);
        gridView = (GridView) findViewById(R.id.photos_grid);
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(this);
        gridView.setMultiChoiceModeListener(this);

        final String dirName = Helpers.getFilename(path);
        setTitle(dirName);
    }

    private List<String> getPhotos() {
        List<String> myPhotos = new ArrayList<>();
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
                    myPhotos.add(cursor.getString(pathIndex));
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        return myPhotos;
    }

    private void deleteSelectedItems() {
        final SparseBooleanArray items = gridView.getCheckedItemPositions();
        int cnt = items.size();
        for (int i = 0; i < cnt; i++) {
            final int id = items.keyAt(i);
            final File file = new File(photos.get(id));
            file.delete();
        }

        MediaScannerConnection.scanFile(this, new String[]{path}, null, new MediaScannerConnection.OnScanCompletedListener() {
            @Override
            public void onScanCompleted(final String path, final Uri uri) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        photos = getPhotos();
                        adapter.updateItems(photos);
                    }
                });
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Intent intent = new Intent(this, ViewPagerActivity.class);
        intent.putExtra(Constants.PHOTO, photos.get(position));
        startActivity(intent);
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        if (checked)
            selectedItemsCnt++;
        else
            selectedItemsCnt--;

        mode.setTitle(String.valueOf(selectedItemsCnt));
        mode.invalidate();
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        final MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.cab, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.cab_remove:
                deleteSelectedItems();
                mode.finish();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        selectedItemsCnt = 0;
    }
}

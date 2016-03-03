package com.simplemobiletools.gallery.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.simplemobiletools.gallery.Constants;
import com.simplemobiletools.gallery.Helpers;
import com.simplemobiletools.gallery.R;
import com.simplemobiletools.gallery.adapters.PhotosAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PhotosActivity extends AppCompatActivity
        implements AdapterView.OnItemClickListener, GridView.MultiChoiceModeListener, MediaScannerConnection.OnScanCompletedListener,
        GridView.OnTouchListener {
    private final int STORAGE_PERMISSION = 1;
    private List<String> photos;
    private int selectedItemsCnt;
    private GridView gridView;
    private String path;
    private PhotosAdapter adapter;
    private Snackbar snackbar;
    private boolean isSnackbarShown;
    private List<String> toBeDeleted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photos);
    }

    @Override
    protected void onResume() {
        super.onResume();
        tryloadGallery();
    }

    @Override
    protected void onPause() {
        super.onPause();
        deleteFiles();
    }

    private void tryloadGallery() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            initializeGallery();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeGallery();
            } else {
                Toast.makeText(this, getResources().getString(R.string.no_permissions), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initializeGallery() {
        toBeDeleted = new ArrayList<>();
        path = getIntent().getStringExtra(Constants.DIRECTORY);
        photos = getPhotos();
        if (isDirEmpty())
            return;

        adapter = new PhotosAdapter(this, photos);
        gridView = (GridView) findViewById(R.id.photos_grid);
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(this);
        gridView.setMultiChoiceModeListener(this);
        gridView.setOnTouchListener(this);
        isSnackbarShown = false;

        final String dirName = Helpers.getFilename(path);
        setTitle(dirName);
    }

    private void deleteDirectoryIfEmpty() {
        final File file = new File(path);
        if (file.isDirectory() && file.listFiles().length == 0) {
            file.delete();
        }
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
                if (curPath.matches(pattern) && !toBeDeleted.contains(curPath)) {
                    myPhotos.add(cursor.getString(pathIndex));
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        return myPhotos;
    }

    private boolean isDirEmpty() {
        if (photos.size() <= 0) {
            deleteDirectoryIfEmpty();
            finish();
            return true;
        }
        return false;
    }

    private void prepareForDeleting() {
        Helpers.showToast(this, R.string.deleting);
        final SparseBooleanArray items = gridView.getCheckedItemPositions();
        int cnt = items.size();
        for (int i = 0; i < cnt; i++) {
            final int id = items.keyAt(i);
            toBeDeleted.add(photos.get(id));
        }

        MediaScannerConnection.scanFile(this, new String[]{path}, null, this);
    }

    private void notifyDeletion(int cnt) {
        final CoordinatorLayout coordinator = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        final String msg = getResources().getQuantityString(R.plurals.files_deleted, cnt, cnt);
        snackbar = Snackbar.make(coordinator, msg, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(getResources().getString(R.string.undo), undoDeletion);
        snackbar.setActionTextColor(Color.WHITE);
        snackbar.show();
        isSnackbarShown = true;
    }

    private void deleteFiles() {
        if (snackbar == null)
            return;

        snackbar.dismiss();
        isSnackbarShown = false;

        for (String delPath : toBeDeleted) {
            final File file = new File(delPath);
            if (file.exists())
                file.delete();
        }

        MediaScannerConnection.scanFile(this, new String[]{path}, null, null);
    }

    private View.OnClickListener undoDeletion = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            snackbar.dismiss();
            isSnackbarShown = false;
            toBeDeleted.clear();
            updateGridView();
        }
    };

    private void updateGridView() {
        photos = getPhotos();
        if (!isDirEmpty()) {
            adapter.updateItems(photos);
        }
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

        if (selectedItemsCnt > 0)
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
                prepareForDeleting();
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

    @Override
    public void onScanCompleted(String path, Uri uri) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateGridView();
                notifyDeletion(toBeDeleted.size());
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (isSnackbarShown) {
            deleteFiles();
        }

        return false;
    }
}

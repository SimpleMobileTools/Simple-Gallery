package com.simplemobiletools.gallery.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
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
import com.simplemobiletools.gallery.Directory;
import com.simplemobiletools.gallery.Helpers;
import com.simplemobiletools.gallery.R;
import com.simplemobiletools.gallery.adapters.DirectoryAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements AdapterView.OnItemClickListener, GridView.MultiChoiceModeListener, GridView.OnTouchListener {
    private final int STORAGE_PERMISSION = 1;
    private List<Directory> dirs;
    private GridView gridView;
    private int selectedItemsCnt;
    private Snackbar snackbar;
    private boolean isSnackbarShown;
    private List<String> toBeDeleted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        tryloadGallery();
    }

    @Override
    protected void onPause() {
        super.onPause();
        deleteDirs();
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
        dirs = new ArrayList<>(getDirectories().values());
        final DirectoryAdapter adapter = new DirectoryAdapter(this, dirs);

        gridView = (GridView) findViewById(R.id.directories_grid);
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(this);
        gridView.setMultiChoiceModeListener(this);
        gridView.setOnTouchListener(this);
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
                final String fileDir = file.getParent();

                if (directories.containsKey(fileDir)) {
                    final Directory directory = directories.get(fileDir);
                    final int newImageCnt = directory.getPhotoCnt() + 1;
                    directory.setPhotoCnt(newImageCnt);
                } else if (!toBeDeleted.contains(fileDir)) {
                    final String dirName = Helpers.getFilename(fileDir);
                    directories.put(fileDir, new Directory(fileDir, path, dirName, 1));
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        return directories;
    }

    private void prepareForDeleting() {
        Helpers.showToast(this, R.string.deleting);
        final SparseBooleanArray items = gridView.getCheckedItemPositions();
        int cnt = items.size();
        for (int i = 0; i < cnt; i++) {
            final int id = items.keyAt(i);
            final String path = dirs.get(id).getPath();
            toBeDeleted.add(path);
        }

        notifyDeletion(cnt);
    }

    private void notifyDeletion(int cnt) {
        dirs = new ArrayList<>(getDirectories().values());

        final CoordinatorLayout coordinator = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        final Resources res = getResources();
        final String msg = res.getQuantityString(R.plurals.folders_deleted, cnt, cnt);
        snackbar = Snackbar.make(coordinator, msg, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(res.getString(R.string.undo), undoDeletion);
        snackbar.setActionTextColor(Color.WHITE);
        snackbar.show();
        isSnackbarShown = true;
        updateGridView();
    }

    private void deleteDirs() {
        if (toBeDeleted.isEmpty())
            return;

        if (snackbar != null) {
            snackbar.dismiss();
        }

        isSnackbarShown = false;

        final List<String> updatedFiles = new ArrayList<>();
        for (String delPath : toBeDeleted) {
            final File dir = new File(delPath);
            final File[] files = dir.listFiles();
            for (File f : files) {
                updatedFiles.add(f.getAbsolutePath());
                f.delete();
            }
        }

        final String[] deletedPaths = updatedFiles.toArray(new String[updatedFiles.size()]);
        MediaScannerConnection.scanFile(this, deletedPaths, null, null);
    }

    private View.OnClickListener undoDeletion = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            snackbar.dismiss();
            isSnackbarShown = false;
            toBeDeleted.clear();
            dirs = new ArrayList<>(getDirectories().values());
            updateGridView();
        }
    };

    private void updateGridView() {
        final DirectoryAdapter adapter = (DirectoryAdapter) gridView.getAdapter();
        adapter.updateItems(dirs);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Intent intent = new Intent(this, PhotosActivity.class);
        intent.putExtra(Constants.DIRECTORY, dirs.get(position).getPath());
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
        inflater.inflate(R.menu.directories_menu, menu);
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
    public boolean onTouch(View v, MotionEvent event) {
        if (isSnackbarShown) {
            deleteDirs();
        }

        return false;
    }
}

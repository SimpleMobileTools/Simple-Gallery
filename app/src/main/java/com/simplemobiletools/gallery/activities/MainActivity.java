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
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;

import com.simplemobiletools.gallery.Constants;
import com.simplemobiletools.gallery.R;
import com.simplemobiletools.gallery.Utils;
import com.simplemobiletools.gallery.adapters.DirectoryAdapter;
import com.simplemobiletools.gallery.models.Directory;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity
        implements AdapterView.OnItemClickListener, GridView.MultiChoiceModeListener, GridView.OnTouchListener {
    @BindView(R.id.directories_grid) GridView gridView;

    private static final int STORAGE_PERMISSION = 1;
    private static final int PICK_IMAGE = 2;
    private List<Directory> dirs;
    private int selectedItemsCnt;
    private Snackbar snackbar;
    private boolean isSnackbarShown;
    private List<String> toBeDeleted;
    private ActionMode actionMode;
    private Parcelable state;
    private boolean isImagePickIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        isImagePickIntent = isImagePickIntent(getIntent());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about:
                final Intent intent = new Intent(getApplicationContext(), AboutActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        tryloadGallery();
        if (state != null && gridView != null)
            gridView.onRestoreInstanceState(state);
    }

    @Override
    protected void onPause() {
        super.onPause();
        deleteDirs();
        if (gridView != null)
            state = gridView.onSaveInstanceState();
    }

    private void tryloadGallery() {
        if (Utils.hasStoragePermission(getApplicationContext())) {
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
                Utils.showToast(getApplicationContext(), R.string.no_permissions);
                finish();
            }
        }
    }

    private void initializeGallery() {
        toBeDeleted = new ArrayList<>();
        dirs = new ArrayList<>(getDirectories().values());
        final DirectoryAdapter adapter = new DirectoryAdapter(this, dirs);

        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(this);
        gridView.setMultiChoiceModeListener(this);
        gridView.setOnTouchListener(this);
    }

    private Map<String, Directory> getDirectories() {
        final Map<String, Directory> directories = new LinkedHashMap<>();
        final List<String> invalidFiles = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            if (i == 1) {
                if (isImagePickIntent)
                    continue;

                uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            }
            final String[] columns = {MediaStore.Images.Media.DATA};
            final String order = MediaStore.Images.Media.DATE_MODIFIED + " DESC";
            final Cursor cursor = getContentResolver().query(uri, columns, null, null, order);

            if (cursor != null && cursor.moveToFirst()) {
                final int pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                do {
                    final String path = cursor.getString(pathIndex);
                    final File file = new File(path);
                    final String fileDir = file.getParent();

                    if (!file.exists()) {
                        invalidFiles.add(file.getAbsolutePath());
                        continue;
                    }

                    if (directories.containsKey(fileDir)) {
                        final Directory directory = directories.get(fileDir);
                        final int newImageCnt = directory.getMediaCnt() + 1;
                        directory.setMediaCnt(newImageCnt);
                    } else if (!toBeDeleted.contains(fileDir)) {
                        final String dirName = Utils.getFilename(fileDir);
                        directories.put(fileDir, new Directory(fileDir, path, dirName, 1));
                    }
                } while (cursor.moveToNext());
                cursor.close();
            }
        }

        final String[] invalids = invalidFiles.toArray(new String[invalidFiles.size()]);
        MediaScannerConnection.scanFile(getApplicationContext(), invalids, null, null);

        return directories;
    }

    private void prepareForDeleting() {
        Utils.showToast(this, R.string.deleting);
        final SparseBooleanArray items = gridView.getCheckedItemPositions();
        int cnt = items.size();
        int deletedCnt = 0;
        for (int i = 0; i < cnt; i++) {
            if (items.valueAt(i)) {
                final int id = items.keyAt(i);
                final String path = dirs.get(id).getPath();
                toBeDeleted.add(path);
                deletedCnt++;
            }
        }

        notifyDeletion(deletedCnt);
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
        if (toBeDeleted == null || toBeDeleted.isEmpty())
            return;

        if (snackbar != null) {
            snackbar.dismiss();
        }

        isSnackbarShown = false;

        final List<String> updatedFiles = new ArrayList<>();
        for (String delPath : toBeDeleted) {
            final File dir = new File(delPath);
            if (dir.exists()) {
                final File[] files = dir.listFiles();
                for (File f : files) {
                    updatedFiles.add(f.getAbsolutePath());
                    f.delete();
                }
                updatedFiles.add(dir.getAbsolutePath());
                dir.delete();
            }
        }

        final String[] deletedPaths = updatedFiles.toArray(new String[updatedFiles.size()]);
        MediaScannerConnection.scanFile(this, deletedPaths, null, null);
        toBeDeleted.clear();
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

    private void editDirectory() {
        final SparseBooleanArray items = gridView.getCheckedItemPositions();
        final int cnt = items.size();
        for (int i = 0; i < cnt; i++) {
            if (items.valueAt(i)) {
                final int id = items.keyAt(i);
                final String path = dirs.get(id).getPath();
                renameDir(path);
                break;
            }
        }
    }

    private void renameDir(final String path) {
        final File dir = new File(path);

        final View renameFileView = getLayoutInflater().inflate(R.layout.rename_directory, null);
        final EditText dirNameET = (EditText) renameFileView.findViewById(R.id.directory_name);
        dirNameET.setText(dir.getName());

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.rename_folder));
        builder.setView(renameFileView);

        builder.setPositiveButton("OK", null);
        builder.setNegativeButton("Cancel", null);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String newDirName = dirNameET.getText().toString().trim();

                if (newDirName.isEmpty()) {
                    Utils.showToast(getApplicationContext(), R.string.rename_folder_empty);
                    return;
                }

                final List<String> updatedFiles = new ArrayList<>();
                updatedFiles.add(dir.getAbsolutePath());

                final File newDir = new File(dir.getParent(), newDirName);
                if (dir.renameTo(newDir)) {
                    Utils.showToast(getApplicationContext(), R.string.renaming_folder);
                    alertDialog.dismiss();
                    actionMode.finish();

                    final File[] files = newDir.listFiles();
                    for (File f : files) {
                        updatedFiles.add(f.getAbsolutePath());
                    }

                    updatedFiles.add(newDir.getAbsolutePath());
                    final String[] changedFiles = updatedFiles.toArray(new String[updatedFiles.size()]);
                    MediaScannerConnection
                            .scanFile(getApplicationContext(), changedFiles, null, new MediaScannerConnection.OnScanCompletedListener() {
                                @Override
                                public void onScanCompleted(String path, Uri uri) {
                                    scanCompleted(path);
                                }
                            });
                } else {
                    Utils.showToast(getApplicationContext(), R.string.rename_folder_error);
                }
            }
        });
    }

    private boolean isImagePickIntent(Intent intent) {
        return intent != null && intent.getAction() != null && intent.getAction().equals(Intent.ACTION_PICK) &&
                intent.getData().equals(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            final Intent result = new Intent();
            result.setData(data.getData());
            setResult(RESULT_OK, result);
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Intent intent = new Intent(this, MediaActivity.class);
        intent.putExtra(Constants.DIRECTORY, dirs.get(position).getPath());
        intent.putExtra(Constants.PICK_IMAGE_INTENT, isImagePickIntent);
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        if (checked) {
            selectedItemsCnt++;
        } else {
            selectedItemsCnt--;
        }

        if (selectedItemsCnt > 0) {
            mode.setTitle(String.valueOf(selectedItemsCnt));
        }

        mode.invalidate();
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        final MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.directories_menu, menu);
        actionMode = mode;
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        final MenuItem menuItem = menu.findItem(R.id.cab_edit);
        menuItem.setVisible(selectedItemsCnt == 1);
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.cab_edit:
                editDirectory();
                return true;
            case R.id.cab_delete:
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

    private void scanCompleted(final String path) {
        final File dir = new File(path);
        if (dir.isDirectory()) {
            dirs = new ArrayList<>(getDirectories().values());

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateGridView();
                    gridView.requestLayout();
                    Utils.showToast(getApplicationContext(), R.string.rename_folder_ok);
                }
            });
        }
    }
}

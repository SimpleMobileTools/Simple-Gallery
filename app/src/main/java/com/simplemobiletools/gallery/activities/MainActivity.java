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
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
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
import android.widget.TextView;

import com.simplemobiletools.gallery.Config;
import com.simplemobiletools.gallery.Constants;
import com.simplemobiletools.gallery.R;
import com.simplemobiletools.gallery.Utils;
import com.simplemobiletools.gallery.adapters.DirectoryAdapter;
import com.simplemobiletools.gallery.dialogs.ChangeSorting;
import com.simplemobiletools.gallery.models.Directory;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends SimpleActivity
        implements AdapterView.OnItemClickListener, GridView.MultiChoiceModeListener, GridView.OnTouchListener,
        SwipeRefreshLayout.OnRefreshListener, ChangeSorting.ChangeDialogListener {
    @BindView(R.id.directories_grid) GridView mGridView;
    @BindView(R.id.directories_holder) SwipeRefreshLayout mSwipeRefreshLayout;

    private static final int STORAGE_PERMISSION = 1;
    private static final int PICK_MEDIA = 2;
    private static final int PICK_WALLPAPER = 3;

    private static List<Directory> mDirs;
    private static Snackbar mSnackbar;
    private static List<String> mToBeDeleted;
    private static ActionMode mActionMode;
    private static Parcelable mState;

    private static boolean mIsSnackbarShown;
    private static boolean mIsPickImageIntent;
    private static boolean mIsPickVideoIntent;
    private static boolean mIsGetImageContentIntent;
    private static boolean mIsGetVideoContentIntent;
    private static boolean mIsGetAnyContentIntent;
    private static boolean mIsSetWallpaperIntent;
    private static boolean mIsThirdPartyIntent;
    private static int mSelectedItemsCnt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        final Intent intent = getIntent();
        mIsPickImageIntent = isPickImageIntent(intent);
        mIsPickVideoIntent = isPickVideoIntent(intent);
        mIsGetImageContentIntent = isGetImageContentIntent(intent);
        mIsGetVideoContentIntent = isGetVideoContentIntent(intent);
        mIsGetAnyContentIntent = isGetAnyContentIntent(intent);
        mIsSetWallpaperIntent = isSetWallpaperIntent(intent);
        mIsThirdPartyIntent = mIsPickImageIntent || mIsPickVideoIntent || mIsGetImageContentIntent || mIsGetVideoContentIntent ||
                mIsGetAnyContentIntent || mIsSetWallpaperIntent;

        mToBeDeleted = new ArrayList<>();
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mDirs = new ArrayList<>();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mIsThirdPartyIntent)
            return false;

        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sort:
                showSortingDialog();
                return true;
            case R.id.camera:
                startActivity(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA));
                return true;
            case R.id.settings:
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                return true;
            case R.id.about:
                startActivity(new Intent(getApplicationContext(), AboutActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        tryloadGallery();
        if (mState != null && mGridView != null)
            mGridView.onRestoreInstanceState(mState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        deleteDirs();
        if (mGridView != null)
            mState = mGridView.onSaveInstanceState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Config.newInstance(getApplicationContext()).setIsFirstRun(false);
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
        final List<Directory> newDirs = getDirectories();
        if (newDirs.toString().equals(mDirs.toString())) {
            return;
        }
        mDirs = newDirs;

        final DirectoryAdapter adapter = new DirectoryAdapter(this, mDirs);
        mGridView.setAdapter(adapter);
        mGridView.setOnItemClickListener(this);
        mGridView.setMultiChoiceModeListener(this);
        mGridView.setOnTouchListener(this);
    }

    private List<Directory> getDirectories() {
        final Map<String, Directory> directories = new LinkedHashMap<>();
        final List<String> invalidFiles = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            if ((mIsPickVideoIntent || mIsGetVideoContentIntent) && i == 0)
                continue;

            Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            if (i == 1) {
                if (mIsPickImageIntent || mIsGetImageContentIntent)
                    continue;

                uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            }
            final String[] columns = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_TAKEN};
            final String order = getSortOrder();
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

                    final int dateIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
                    final long timestamp = cursor.getLong(dateIndex);
                    if (directories.containsKey(fileDir)) {
                        final Directory directory = directories.get(fileDir);
                        final int newImageCnt = directory.getMediaCnt() + 1;
                        directory.setMediaCnt(newImageCnt);
                        directory.addSize(file.length());
                    } else if (!mToBeDeleted.contains(fileDir)) {
                        final String dirName = Utils.getFilename(fileDir);
                        directories.put(fileDir, new Directory(fileDir, path, dirName, 1, timestamp, file.length()));
                    }
                } while (cursor.moveToNext());
                cursor.close();
            }
        }

        final List<Directory> dirs = new ArrayList<>(directories.values());
        removeNoMediaDirs(dirs);
        Directory.mSorting = mConfig.getDirectorySorting();
        Collections.sort(dirs);

        final String[] invalids = invalidFiles.toArray(new String[invalidFiles.size()]);
        MediaScannerConnection.scanFile(getApplicationContext(), invalids, null, null);

        return dirs;
    }

    private void removeNoMediaDirs(List<Directory> dirs) {
        final List<Directory> ignoreDirs = new ArrayList<>();
        for (final Directory d : dirs) {
            final File dir = new File(d.getPath());
            if (dir.exists() && dir.isDirectory()) {
                final String[] res = dir.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File file, String filename) {
                        return filename.equals(".nomedia");
                    }
                });

                if (res.length > 0)
                    ignoreDirs.add(d);
            }
        }

        dirs.removeAll(ignoreDirs);
    }

    // sort the files at querying too, just to get the correct thumbnail
    private String getSortOrder() {
        final int sorting = mConfig.getDirectorySorting();
        String sortBy = MediaStore.Images.Media.DATE_TAKEN;
        if ((sorting & Constants.SORT_BY_NAME) != 0) {
            sortBy = MediaStore.Images.Media.DATA;
        }

        if ((sorting & Constants.SORT_DESCENDING) != 0) {
            sortBy += " DESC";
        }
        return sortBy;
    }

    private void showSortingDialog() {
        new ChangeSorting(this, true);
    }

    private void prepareForDeleting() {
        Utils.showToast(this, R.string.deleting);
        final SparseBooleanArray items = mGridView.getCheckedItemPositions();
        final int cnt = items.size();
        int deletedCnt = 0;
        for (int i = 0; i < cnt; i++) {
            if (items.valueAt(i)) {
                final int id = items.keyAt(i);
                final String path = mDirs.get(id).getPath();
                mToBeDeleted.add(path);
                deletedCnt++;
            }
        }

        notifyDeletion(deletedCnt);
    }

    private void notifyDeletion(int cnt) {
        mDirs = getDirectories();

        final CoordinatorLayout coordinator = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        final Resources res = getResources();
        final String msg = res.getQuantityString(R.plurals.folders_deleted, cnt, cnt);
        mSnackbar = Snackbar.make(coordinator, msg, Snackbar.LENGTH_INDEFINITE);
        mSnackbar.setAction(res.getString(R.string.undo), undoDeletion);
        mSnackbar.setActionTextColor(Color.WHITE);
        mSnackbar.show();
        mIsSnackbarShown = true;
        updateGridView();
    }

    private void deleteDirs() {
        if (mToBeDeleted == null || mToBeDeleted.isEmpty())
            return;

        if (mSnackbar != null) {
            mSnackbar.dismiss();
        }

        mIsSnackbarShown = false;

        final List<String> updatedFiles = new ArrayList<>();
        for (String delPath : mToBeDeleted) {
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
        mToBeDeleted.clear();
    }

    private View.OnClickListener undoDeletion = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mSnackbar.dismiss();
            mIsSnackbarShown = false;
            mToBeDeleted.clear();
            mDirs = getDirectories();
            updateGridView();
        }
    };

    private void updateGridView() {
        final DirectoryAdapter adapter = (DirectoryAdapter) mGridView.getAdapter();
        adapter.updateItems(mDirs);
    }

    private void editDirectory() {
        final SparseBooleanArray items = mGridView.getCheckedItemPositions();
        final int cnt = items.size();
        for (int i = 0; i < cnt; i++) {
            if (items.valueAt(i)) {
                final int id = items.keyAt(i);
                final String path = mDirs.get(id).getPath();
                renameDir(path);
                break;
            }
        }
    }

    private void renameDir(final String path) {
        final File dir = new File(path);

        final View renameDirView = getLayoutInflater().inflate(R.layout.rename_directory, null);
        final EditText dirNameET = (EditText) renameDirView.findViewById(R.id.directory_name);
        dirNameET.setText(dir.getName());

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.rename_folder));
        builder.setView(renameDirView);

        final TextView dirPath = (TextView) renameDirView.findViewById(R.id.directory_path);
        dirPath.setText(dir.getParent() + "/");

        builder.setPositiveButton(R.string.ok, null);
        builder.setNegativeButton(R.string.cancel, null);

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
                    mActionMode.finish();

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

    private boolean isPickImageIntent(Intent intent) {
        return isPickIntent(intent) && (hasImageContentData(intent) || isImageType(intent));
    }

    private boolean isPickVideoIntent(Intent intent) {
        return isPickIntent(intent) && (hasVideoContentData(intent) || isVideoType(intent));
    }

    private boolean isPickIntent(Intent intent) {
        return intent != null && intent.getAction() != null && intent.getAction().equals(Intent.ACTION_PICK);
    }

    private boolean isGetContentIntent(Intent intent) {
        return intent != null && intent.getAction() != null && intent.getAction().equals(Intent.ACTION_GET_CONTENT) &&
                intent.getType() != null;
    }

    private boolean isGetImageContentIntent(Intent intent) {
        return isGetContentIntent(intent) &&
                (intent.getType().startsWith("image/") || intent.getType().equals(MediaStore.Images.Media.CONTENT_TYPE));
    }

    private boolean isGetVideoContentIntent(Intent intent) {
        return isGetContentIntent(intent) &&
                (intent.getType().startsWith("video/") || intent.getType().equals(MediaStore.Video.Media.CONTENT_TYPE));
    }

    private boolean isGetAnyContentIntent(Intent intent) {
        return isGetContentIntent(intent) && intent.getType().equals("*/*");
    }

    private boolean isSetWallpaperIntent(Intent intent) {
        return intent != null && intent.getAction() != null && intent.getAction().equals(Intent.ACTION_SET_WALLPAPER);
    }

    private boolean hasImageContentData(Intent intent) {
        final Uri data = intent.getData();
        return data != null && data.equals(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    }

    private boolean hasVideoContentData(Intent intent) {
        final Uri data = intent.getData();
        return data != null && data.equals(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
    }

    private boolean isImageType(Intent intent) {
        final String type = intent.getType();
        return type != null && (type.startsWith("image/") || type.equals(MediaStore.Images.Media.CONTENT_TYPE));
    }

    private boolean isVideoType(Intent intent) {
        final String type = intent.getType();
        return type != null && (type.startsWith("video/") || type.equals(MediaStore.Video.Media.CONTENT_TYPE));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_MEDIA && data != null) {
                final Intent result = new Intent();
                final String path = data.getData().getPath();
                final Uri uri = Uri.fromFile(new File(path));
                if (mIsGetImageContentIntent || mIsGetVideoContentIntent || mIsGetAnyContentIntent) {
                    final String type = Utils.getMimeType(path);
                    result.setDataAndTypeAndNormalize(uri, type);
                    result.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                } else if (mIsPickImageIntent || mIsPickVideoIntent) {
                    result.setData(uri);
                    result.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }

                setResult(RESULT_OK, result);
                finish();
            } else if (requestCode == PICK_WALLPAPER) {
                setResult(RESULT_OK);
                finish();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Intent intent = new Intent(this, MediaActivity.class);
        intent.putExtra(Constants.DIRECTORY, mDirs.get(position).getPath());

        if (mIsSetWallpaperIntent) {
            intent.putExtra(Constants.SET_WALLPAPER_INTENT, true);
            startActivityForResult(intent, PICK_WALLPAPER);
        } else {
            intent.putExtra(Constants.GET_IMAGE_INTENT, mIsPickImageIntent || mIsGetImageContentIntent);
            intent.putExtra(Constants.GET_VIDEO_INTENT, mIsPickVideoIntent || mIsGetVideoContentIntent);
            intent.putExtra(Constants.GET_ANY_INTENT, mIsGetAnyContentIntent);
            startActivityForResult(intent, PICK_MEDIA);
        }
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        if (checked) {
            mSelectedItemsCnt++;
        } else {
            mSelectedItemsCnt--;
        }

        if (mSelectedItemsCnt > 0) {
            mode.setTitle(String.valueOf(mSelectedItemsCnt));
        }

        mode.invalidate();
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        final MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.directories_cab, menu);
        mActionMode = mode;
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        final MenuItem menuItem = menu.findItem(R.id.cab_edit);
        menuItem.setVisible(mSelectedItemsCnt == 1);
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
        mSelectedItemsCnt = 0;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mIsSnackbarShown) {
            deleteDirs();
        }

        return false;
    }

    private void scanCompleted(final String path) {
        final File dir = new File(path);
        if (dir.isDirectory()) {
            mDirs = getDirectories();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateGridView();
                    mGridView.requestLayout();
                    Utils.showToast(getApplicationContext(), R.string.rename_folder_ok);
                }
            });
        }
    }

    @Override
    public void onRefresh() {
        initializeGallery();
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void dialogClosed() {
        initializeGallery();
    }
}

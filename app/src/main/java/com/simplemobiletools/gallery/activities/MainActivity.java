package com.simplemobiletools.gallery.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.provider.DocumentFile;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import com.simplemobiletools.fileproperties.dialogs.PropertiesDialog;
import com.simplemobiletools.gallery.Constants;
import com.simplemobiletools.gallery.R;
import com.simplemobiletools.gallery.Utils;
import com.simplemobiletools.gallery.adapters.DirectoryAdapter;
import com.simplemobiletools.gallery.asynctasks.CopyTask;
import com.simplemobiletools.gallery.asynctasks.GetDirectoriesAsynctask;
import com.simplemobiletools.gallery.dialogs.ChangeSorting;
import com.simplemobiletools.gallery.dialogs.CopyDialog;
import com.simplemobiletools.gallery.dialogs.RenameDirectoryDialog;
import com.simplemobiletools.gallery.models.Directory;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends SimpleActivity
        implements AdapterView.OnItemClickListener, GridView.MultiChoiceModeListener, GridView.OnTouchListener,
        SwipeRefreshLayout.OnRefreshListener, ChangeSorting.ChangeDialogListener, GetDirectoriesAsynctask.GetDirectoriesListener,
        CopyTask.CopyDoneListener {
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
    private static boolean mIsGettingDirs;
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
        mConfig.setIsFirstRun(false);
    }

    private void tryloadGallery() {
        if (Utils.Companion.hasStoragePermission(getApplicationContext())) {
            getDirectories();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getDirectories();
            } else {
                Utils.Companion.showToast(getApplicationContext(), R.string.no_permissions);
                finish();
            }
        }
    }

    private void getDirectories() {
        if (mIsGettingDirs)
            return;

        mIsGettingDirs = true;
        new GetDirectoriesAsynctask(getApplicationContext(), mIsPickVideoIntent || mIsGetVideoContentIntent, mIsPickImageIntent || mIsGetImageContentIntent,
                mToBeDeleted, this).execute();
    }

    private void showSortingDialog() {
        new ChangeSorting(this, true);
    }

    private void prepareForDeleting() {
        Utils.Companion.showToast(this, R.string.deleting);
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

        for (String path : mToBeDeleted) {
            if (Utils.Companion.isShowingWritePermissions(this, new File(path))) {
                return;
            }
        }

        notifyDeletion(deletedCnt);
    }

    private void notifyDeletion(int cnt) {
        getDirectories();

        final CoordinatorLayout coordinator = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        final Resources res = getResources();
        final String msg = res.getQuantityString(R.plurals.folders_deleted, cnt, cnt);
        mSnackbar = Snackbar.make(coordinator, msg, Snackbar.LENGTH_INDEFINITE);
        mSnackbar.setAction(res.getString(R.string.undo), undoDeletion);
        mSnackbar.setActionTextColor(Color.WHITE);
        mSnackbar.show();
        mIsSnackbarShown = true;
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
                    if (f.isFile()) {
                        updatedFiles.add(f.getAbsolutePath());
                        deleteItem(f);
                    }
                }
                updatedFiles.add(dir.getAbsolutePath());
                if (dir.listFiles().length == 0)
                    deleteItem(dir);
            }
        }

        final String[] deletedPaths = updatedFiles.toArray(new String[updatedFiles.size()]);
        MediaScannerConnection.scanFile(getApplicationContext(), deletedPaths, null, null);
        mToBeDeleted.clear();
    }

    private void deleteItem(File file) {
        if (Utils.Companion.needsStupidWritePermissions(this, file.getAbsolutePath())) {
            if (!Utils.Companion.isShowingWritePermissions(this, file)) {
                final DocumentFile document = Utils.Companion.getFileDocument(this, file.getAbsolutePath());
                document.delete();
            }
        } else {
            file.delete();
        }
    }

    private View.OnClickListener undoDeletion = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mSnackbar.dismiss();
            mIsSnackbarShown = false;
            mToBeDeleted.clear();
            getDirectories();
        }
    };

    private void showProperties() {
        final SparseBooleanArray items = mGridView.getCheckedItemPositions();
        if (items.size() == 1) {
            new PropertiesDialog(this, (String) getSelectedPaths().toArray()[0], false);
        } else {
            final List<String> paths = new ArrayList<>(items.size());
            final int cnt = items.size();
            for (int i = 0; i < cnt; i++) {
                if (items.valueAt(i)) {
                    final int id = items.keyAt(i);
                    paths.add(mDirs.get(id).getPath());
                }
            }

            new PropertiesDialog(this, paths, false);
        }
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
        new RenameDirectoryDialog(this, dir, new RenameDirectoryDialog.OnRenameDirListener() {
            @Override
            public void onRenameDirSuccess(@NotNull String[] changedFiles) {
                mActionMode.finish();
                MediaScannerConnection.scanFile(getApplicationContext(), changedFiles, null, new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        scanCompleted(path);
                    }
                });
            }
        });
    }

    private void displayCopyDialog() {
        final List<File> files = new ArrayList<>();
        final SparseBooleanArray items = mGridView.getCheckedItemPositions();
        final int cnt = items.size();
        for (int i = 0; i < cnt; i++) {
            if (items.valueAt(i)) {
                final int id = items.keyAt(i);
                final File dir = new File(mDirs.get(id).getPath());
                files.addAll(Arrays.asList(dir.listFiles()));
            }
        }

        new CopyDialog(this, files, this, new CopyDialog.OnCopyListener() {
            @Override
            public void onSuccess() {

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
                    final String type = Utils.Companion.getMimeType(path);
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
        menu.findItem(R.id.cab_edit).setVisible(mSelectedItemsCnt == 1);

        int hiddenCnt = 0;
        int unhiddenCnt = 0;
        final SparseBooleanArray items = mGridView.getCheckedItemPositions();
        final int cnt = items.size();
        for (int i = 0; i < cnt; i++) {
            if (items.valueAt(i)) {
                final int id = items.keyAt(i);
                if (mConfig.getIsFolderHidden(mDirs.get(id).getPath()))
                    hiddenCnt++;
                else
                    unhiddenCnt++;
            }
        }

        menu.findItem(R.id.cab_hide).setVisible(unhiddenCnt > 0);
        menu.findItem(R.id.cab_unhide).setVisible(hiddenCnt > 0);

        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.cab_properties:
                showProperties();
                return true;
            case R.id.cab_edit:
                editDirectory();
                return true;
            case R.id.cab_delete:
                prepareForDeleting();
                mode.finish();
                return true;
            case R.id.cab_hide:
                hideFolders();
                mode.finish();
                return true;
            case R.id.cab_unhide:
                unhideFolders();
                mode.finish();
                return true;
            case R.id.cab_copy:
                displayCopyDialog();
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

    private void hideFolders() {
        mConfig.addHiddenDirectories(getSelectedPaths());
        getDirectories();
    }

    private void unhideFolders() {
        mConfig.removeHiddenDirectories(getSelectedPaths());
        getDirectories();
    }

    private Set<String> getSelectedPaths() {
        final SparseBooleanArray items = mGridView.getCheckedItemPositions();
        final Set<String> selectedPaths = new HashSet<>();
        final int cnt = items.size();
        for (int i = 0; i < cnt; i++) {
            if (items.valueAt(i)) {
                final int id = items.keyAt(i);
                selectedPaths.add(mDirs.get(id).getPath());
            }
        }
        return selectedPaths;
    }

    private void scanCompleted(final String path) {
        final File dir = new File(path);
        if (dir.isDirectory()) {
            getDirectories();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Utils.Companion.showToast(getApplicationContext(), R.string.rename_folder_ok);
                }
            });
        }
    }

    @Override
    public void onRefresh() {
        getDirectories();
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void sortingDialogClosed() {
        getDirectories();
    }

    @Override
    public void gotDirectories(@NotNull ArrayList<Directory> dirs) {
        mIsGettingDirs = false;
        if (dirs.toString().equals(mDirs.toString())) {
            return;
        }
        mDirs = dirs;

        final DirectoryAdapter adapter = new DirectoryAdapter(this, mDirs);
        mGridView.setAdapter(adapter);
        mGridView.setOnItemClickListener(this);
        mGridView.setMultiChoiceModeListener(this);
        mGridView.setOnTouchListener(this);
        mGridView.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
    }

    @Override
    public void copySucceeded(@NotNull File destinationDir) {
        getDirectories();
        Utils.Companion.showToast(getApplicationContext(), R.string.copying_success);
    }

    @Override
    public void copyFailed() {
        Utils.Companion.showToast(getApplicationContext(), R.string.copying_failed);
    }
}

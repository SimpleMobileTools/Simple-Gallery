package com.simplemobiletools.gallery.activities;

import android.app.WallpaperManager;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.provider.DocumentFile;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.simplemobiletools.fileproperties.dialogs.PropertiesDialog;
import com.simplemobiletools.gallery.Constants;
import com.simplemobiletools.gallery.R;
import com.simplemobiletools.gallery.Utils;
import com.simplemobiletools.gallery.adapters.MediaAdapter;
import com.simplemobiletools.gallery.asynctasks.CopyTask;
import com.simplemobiletools.gallery.dialogs.ChangeSorting;
import com.simplemobiletools.gallery.dialogs.CopyDialog;
import com.simplemobiletools.gallery.models.Medium;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MediaActivity extends SimpleActivity
        implements AdapterView.OnItemClickListener, GridView.MultiChoiceModeListener, GridView.OnTouchListener,
        SwipeRefreshLayout.OnRefreshListener, ChangeSorting.ChangeDialogListener, CopyTask.CopyDoneListener {
    private static final String TAG = MediaActivity.class.getSimpleName();
    @BindView(R.id.media_grid) GridView mGridView;
    @BindView(R.id.media_holder) SwipeRefreshLayout mSwipeRefreshLayout;

    private static List<Medium> mMedia;
    private static String mPath;
    private static Snackbar mSnackbar;
    private static List<String> mToBeDeleted;
    private static Parcelable mState;

    private static boolean mIsSnackbarShown;
    private static boolean mIsGetImageIntent;
    private static boolean mIsGetVideoIntent;
    private static boolean mIsGetAnyIntent;
    private static int mSelectedItemsCnt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);
        ButterKnife.bind(this);
        mIsGetImageIntent = getIntent().getBooleanExtra(Constants.GET_IMAGE_INTENT, false);
        mIsGetVideoIntent = getIntent().getBooleanExtra(Constants.GET_VIDEO_INTENT, false);
        mIsGetAnyIntent = getIntent().getBooleanExtra(Constants.GET_ANY_INTENT, false);
        mToBeDeleted = new ArrayList<>();
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mPath = getIntent().getStringExtra(Constants.DIRECTORY);
        mMedia = new ArrayList<>();
    }

    @Override
    protected void onResume() {
        super.onResume();
        tryloadGallery();
        if (mState != null && mGridView != null) {
            mGridView.onRestoreInstanceState(mState);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        deleteFiles();
        if (mGridView != null && isChangingConfigurations()) {
            mState = mGridView.onSaveInstanceState();
        } else {
            mState = null;
        }
    }

    private void tryloadGallery() {
        if (Utils.Companion.hasStoragePermission(getApplicationContext())) {
            initializeGallery();
        } else {
            finish();
        }
    }

    private void initializeGallery() {
        final List<Medium> newMedia = getMedia();
        if (newMedia.toString().equals(mMedia.toString())) {
            return;
        }

        mMedia = newMedia;
        if (isDirEmpty())
            return;

        final MediaAdapter adapter = new MediaAdapter(this, mMedia);
        mGridView.setAdapter(adapter);
        mGridView.setOnItemClickListener(this);
        mGridView.setMultiChoiceModeListener(this);
        mGridView.setOnTouchListener(this);
        mIsSnackbarShown = false;

        final String dirName = Utils.Companion.getFilename(mPath);
        setTitle(dirName);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_media, menu);

        final boolean isFolderHidden = mConfig.getIsFolderHidden(mPath);
        menu.findItem(R.id.hide_folder).setVisible(!isFolderHidden);
        menu.findItem(R.id.unhide_folder).setVisible(isFolderHidden);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sort:
                showSortingDialog();
                return true;
            case R.id.toggle_filename:
                toggleFilenameVisibility();
                return true;
            case R.id.hide_folder:
                hideDirectory();
                return true;
            case R.id.unhide_folder:
                unhideDirectory();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void toggleFilenameVisibility() {
        mConfig.setDisplayFileNames(!mConfig.getDisplayFileNames());
        ((MediaAdapter)mGridView.getAdapter()).updateDisplayFilenames(mConfig.getDisplayFileNames());
    }

    private void rescanDirectory(File dir) {
        final File[] files = dir.listFiles();
        final String[] paths = new String[files.length];
        final int cnt = dir.listFiles().length;
        for (int i = 0; i < cnt; i++) {
            paths[i] = files[i].getPath();
            if (files[i].isDirectory()) {
                rescanDirectory(files[i]);
            }
        }

        Utils.Companion.scanFiles(getApplicationContext(), paths);
    }

    private void showSortingDialog() {
        new ChangeSorting(this, false);
    }

    private void hideDirectory() {
        mConfig.addHiddenDirectory(mPath);

        if (!mConfig.getShowHiddenFolders())
            finish();
        else
            invalidateOptionsMenu();
    }

    private void unhideDirectory() {
        mConfig.removeHiddenDirectory(mPath);
        invalidateOptionsMenu();
    }

    private void deleteDirectoryIfEmpty() {
        final File file = new File(mPath);
        if (file.isDirectory() && file.listFiles().length == 0) {
            file.delete();
        }
    }

    private List<Medium> getMedia() {
        final List<Medium> media = new ArrayList<>();
        final List<String> invalidFiles = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            if (mIsGetVideoIntent && i == 0)
                continue;

            Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            if (i == 1) {
                if (mIsGetImageIntent)
                    continue;

                uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            }
            final String where = MediaStore.Images.Media.DATA + " like ? ";
            final String[] args = new String[]{mPath + "%"};
            final String[] columns = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_MODIFIED};
            final Cursor cursor = getContentResolver().query(uri, columns, where, args, null);
            final String pattern = Pattern.quote(mPath) + "/[^/]*";

            if (cursor != null && cursor.moveToFirst()) {
                final int pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                do {
                    final String curPath = cursor.getString(pathIndex);
                    if (curPath == null)
                        continue;

                    if (curPath.matches(pattern) && !mToBeDeleted.contains(curPath)) {
                        final File file = new File(curPath);
                        if (file.exists()) {
                            final int dateIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED);
                            final long timestamp = cursor.getLong(dateIndex);
                            media.add(new Medium(file.getName(), curPath, (i == 1), timestamp, file.length()));
                        } else {
                            invalidFiles.add(file.getAbsolutePath());
                        }
                    }
                } while (cursor.moveToNext());
                cursor.close();
            }
        }

        Medium.mSorting = mConfig.getSorting();
        Collections.sort(media);

        final String[] invalids = invalidFiles.toArray(new String[invalidFiles.size()]);
        MediaScannerConnection.scanFile(getApplicationContext(), invalids, null, null);

        return media;
    }

    private boolean isDirEmpty() {
        if (mMedia.size() <= 0) {
            deleteDirectoryIfEmpty();
            finish();
            return true;
        }
        return false;
    }

    private void shareMedia() {
        final List<Medium> selectedMedia = getSelectedMedia();
        if (selectedMedia.size() <= 1) {
            Utils.Companion.shareMedium(selectedMedia.get(0), this);
        } else {
            shareMedia(selectedMedia);
        }
    }

    private void shareMedia(List<Medium> media) {
        final String shareTitle = getResources().getString(R.string.share_via);
        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("image/* video/*");
        final ArrayList<Uri> uris = new ArrayList<>(media.size());
        for (Medium medium : media) {
            final File file = new File(medium.getPath());
            uris.add(Uri.fromFile(file));
        }

        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        startActivity(Intent.createChooser(intent, shareTitle));
    }

    private List<Medium> getSelectedMedia() {
        final List<Medium> media = new ArrayList<>();
        final SparseBooleanArray items = mGridView.getCheckedItemPositions();
        final int cnt = items.size();
        for (int i = 0; i < cnt; i++) {
            if (items.valueAt(i)) {
                final int id = items.keyAt(i);
                media.add(mMedia.get(id));
            }
        }
        return media;
    }

    private void prepareForDeleting() {
        if (Utils.Companion.isShowingWritePermissions(this, new File(mPath)))
            return;

        Utils.Companion.showToast(this, R.string.deleting);
        final SparseBooleanArray items = mGridView.getCheckedItemPositions();
        final int cnt = items.size();
        int deletedCnt = 0;
        for (int i = 0; i < cnt; i++) {
            if (items.valueAt(i)) {
                final int id = items.keyAt(i);
                final String path = mMedia.get(id).getPath();
                mToBeDeleted.add(path);
                deletedCnt++;
            }
        }

        notifyDeletion(deletedCnt);
    }

    private void notifyDeletion(int cnt) {
        mMedia = getMedia();

        if (mMedia.isEmpty()) {
            deleteFiles();
        } else {
            final CoordinatorLayout coordinator = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
            final Resources res = getResources();
            final String msg = res.getQuantityString(R.plurals.files_deleted, cnt, cnt);
            mSnackbar = Snackbar.make(coordinator, msg, Snackbar.LENGTH_INDEFINITE);
            mSnackbar.setAction(res.getString(R.string.undo), undoDeletion);
            mSnackbar.setActionTextColor(Color.WHITE);
            mSnackbar.show();
            mIsSnackbarShown = true;
            updateGridView();
        }
    }

    private void deleteFiles() {
        if (mToBeDeleted == null || mToBeDeleted.isEmpty())
            return;

        if (mSnackbar != null) {
            mSnackbar.dismiss();
        }

        mIsSnackbarShown = false;
        boolean wereFilesDeleted = false;

        for (String delPath : mToBeDeleted) {
            final File file = new File(delPath);
            if (file.exists()) {
                if (Utils.Companion.needsStupidWritePermissions(this, delPath)) {
                    if (Utils.Companion.isShowingWritePermissions(this, file))
                        return;

                    final DocumentFile document = Utils.Companion.getFileDocument(this, delPath);
                    if (document.delete()) {
                        wereFilesDeleted = true;
                    }
                } else {
                    if (file.delete())
                        wereFilesDeleted = true;
                }
            }
        }

        if (wereFilesDeleted) {
            final String[] deletedPaths = mToBeDeleted.toArray(new String[mToBeDeleted.size()]);
            MediaScannerConnection.scanFile(getApplicationContext(), deletedPaths, null, new MediaScannerConnection.OnScanCompletedListener() {
                @Override
                public void onScanCompleted(String path, Uri uri) {
                    if (mMedia != null && mMedia.isEmpty()) {
                        finish();
                    }
                }
            });
            mToBeDeleted.clear();
        }
    }

    private View.OnClickListener undoDeletion = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mSnackbar.dismiss();
            mIsSnackbarShown = false;
            mToBeDeleted.clear();
            mMedia = getMedia();
            updateGridView();
        }
    };

    private void updateGridView() {
        if (!isDirEmpty()) {
            final MediaAdapter adapter = (MediaAdapter) mGridView.getAdapter();
            adapter.updateItems(mMedia);
        }
    }

    private void showProperties() {
        final List<Medium> selectedMedia = getSelectedMedia();
        if (selectedMedia.size() == 1) {
            new PropertiesDialog(this, selectedMedia.get(0).getPath(), false);
        } else {
            final List<String> paths = new ArrayList<>(selectedMedia.size());
            for (Medium medium : selectedMedia) {
                paths.add(medium.getPath());
            }
            new PropertiesDialog(this, paths, false);
        }
    }

    private boolean isSetWallpaperIntent() {
        return getIntent().getBooleanExtra(Constants.SET_WALLPAPER_INTENT, false);
    }

    private void displayCopyDialog() {
        final List<File> files = new ArrayList<>();

        final SparseBooleanArray items = mGridView.getCheckedItemPositions();
        final int cnt = items.size();
        for (int i = 0; i < cnt; i++) {
            if (items.valueAt(i)) {
                final int id = items.keyAt(i);
                files.add(new File(mMedia.get(id).getPath()));
            }
        }

        new CopyDialog(this, files, this, new CopyDialog.OnCopyListener() {
            @Override
            public void onSuccess() {

            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final String curItemPath = mMedia.get(position).getPath();
        if (isSetWallpaperIntent()) {
            Utils.Companion.showToast(this, R.string.setting_wallpaper);

            final int wantedWidth = getWallpaperDesiredMinimumWidth();
            final int wantedHeight = getWallpaperDesiredMinimumHeight();
            final float ratio = (float) wantedWidth / wantedHeight;
            Glide.with(this).
                    load(new File(curItemPath)).
                    asBitmap().
                    override((int) (wantedWidth * ratio), wantedHeight).
                    fitCenter().
                    into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                            try {
                                WallpaperManager.getInstance(getApplicationContext()).setBitmap(bitmap);
                                setResult(RESULT_OK);
                            } catch (IOException e) {
                                Log.e(TAG, "item click " + e.getMessage());
                            }
                            finish();
                        }
                    });
        } else if (mIsGetImageIntent || mIsGetVideoIntent || mIsGetAnyIntent) {
            final Intent result = new Intent();
            result.setData(Uri.parse(curItemPath));
            setResult(RESULT_OK, result);
            finish();
        } else {
            final Intent intent = new Intent(this, ViewPagerActivity.class);
            intent.putExtra(Constants.MEDIUM, curItemPath);
            startActivity(intent);
        }
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        if (checked) {
            mSelectedItemsCnt++;
        } else {
            mSelectedItemsCnt--;
        }

        if (mSelectedItemsCnt > 0)
            mode.setTitle(String.valueOf(mSelectedItemsCnt));

        mode.invalidate();
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        final MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.cab_media, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.cab_properties:
                showProperties();
                return true;
            case R.id.cab_share:
                shareMedia();
                return true;
            case R.id.cab_delete:
                prepareForDeleting();
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
            deleteFiles();
        }

        return false;
    }

    @Override
    public void onRefresh() {
        final File dir = new File(mPath);
        if (dir.isDirectory()) {
            rescanDirectory(dir);
        }
        initializeGallery();
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void sortingDialogClosed() {
        initializeGallery();
    }

    @Override
    public void copySucceeded(@NotNull File destinationDir) {
        Utils.Companion.showToast(getApplicationContext(), R.string.copying_success);
    }

    @Override
    public void copyFailed() {
        Utils.Companion.showToast(getApplicationContext(), R.string.copying_failed);
    }
}

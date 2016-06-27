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
import android.support.v7.app.AppCompatActivity;
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
import com.simplemobiletools.gallery.Constants;
import com.simplemobiletools.gallery.R;
import com.simplemobiletools.gallery.Utils;
import com.simplemobiletools.gallery.adapters.MediaAdapter;
import com.simplemobiletools.gallery.models.Medium;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MediaActivity extends AppCompatActivity
        implements AdapterView.OnItemClickListener, GridView.MultiChoiceModeListener, GridView.OnTouchListener {
    private static final String TAG = MediaActivity.class.getSimpleName();
    @BindView(R.id.media_grid) GridView mGridView;

    private static List<Medium> mMedia;
    private static String mPath;
    private static Snackbar mSnackbar;
    private static List<String> mToBeDeleted;
    private static Parcelable mState;

    private static boolean mIsSnackbarShown;
    private static boolean mIsGetImageIntent;
    private static boolean mIsGetVideoIntent;
    private static int mSelectedItemsCnt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);
        ButterKnife.bind(this);
        mIsGetImageIntent = getIntent().getBooleanExtra(Constants.GET_IMAGE_INTENT, false);
        mIsGetVideoIntent = getIntent().getBooleanExtra(Constants.GET_VIDEO_INTENT, false);
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
        deleteFiles();
        if (mGridView != null)
            mState = mGridView.onSaveInstanceState();
    }

    private void tryloadGallery() {
        if (Utils.hasStoragePermission(getApplicationContext())) {
            initializeGallery();
        } else {
            finish();
        }
    }

    private void initializeGallery() {
        mToBeDeleted = new ArrayList<>();
        mPath = getIntent().getStringExtra(Constants.DIRECTORY);
        mMedia = getMedia();
        if (isDirEmpty())
            return;

        final MediaAdapter adapter = new MediaAdapter(this, mMedia);
        mGridView.setAdapter(adapter);
        mGridView.setOnItemClickListener(this);
        mGridView.setMultiChoiceModeListener(this);
        mGridView.setOnTouchListener(this);
        mIsSnackbarShown = false;

        final String dirName = Utils.getFilename(mPath);
        setTitle(dirName);
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
            final String[] columns = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_TAKEN};
            final Cursor cursor = getContentResolver().query(uri, columns, where, args, null);
            final String pattern = Pattern.quote(mPath) + "/[^/]*";

            if (cursor != null && cursor.moveToFirst()) {
                final int pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                do {
                    final String curPath = cursor.getString(pathIndex);
                    if (curPath.matches(pattern) && !mToBeDeleted.contains(curPath)) {
                        final File file = new File(curPath);
                        if (file.exists()) {
                            final int dateIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
                            final long timestamp = cursor.getLong(dateIndex);
                            media.add(new Medium(curPath, (i == 1), timestamp));
                        } else {
                            invalidFiles.add(file.getAbsolutePath());
                        }
                    }
                } while (cursor.moveToNext());
                cursor.close();
            }
        }

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

    private void prepareForDeleting() {
        Utils.showToast(this, R.string.deleting);
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

        for (String delPath : mToBeDeleted) {
            final File file = new File(delPath);
            if (file.exists())
                file.delete();
        }

        final String[] deletedPaths = mToBeDeleted.toArray(new String[mToBeDeleted.size()]);
        MediaScannerConnection.scanFile(this, deletedPaths, null, new MediaScannerConnection.OnScanCompletedListener() {
            @Override
            public void onScanCompleted(String path, Uri uri) {
                if (mMedia != null && mMedia.isEmpty()) {
                    finish();
                }
            }
        });
        mToBeDeleted.clear();
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

    private boolean isSetWallpaperIntent() {
        return getIntent().getBooleanExtra(Constants.SET_WALLPAPER_INTENT, false);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final String curItemPath = mMedia.get(position).getPath();
        if (isSetWallpaperIntent()) {
            Utils.showToast(this, R.string.setting_wallpaper);

            final int wantedWidth = getWallpaperDesiredMinimumWidth();
            final int wantedHeight = getWallpaperDesiredMinimumHeight();
            final float ratio = (float) wantedWidth / wantedHeight;
            Glide.with(this)
                    .load(new File(curItemPath))
                    .asBitmap()
                    .override((int) (wantedWidth * ratio), wantedHeight)
                    .fitCenter()
                    .into(new SimpleTarget<Bitmap>() {
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
        } else if (mIsGetImageIntent || mIsGetVideoIntent) {
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
        inflater.inflate(R.menu.media_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
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
            deleteFiles();
        }

        return false;
    }
}

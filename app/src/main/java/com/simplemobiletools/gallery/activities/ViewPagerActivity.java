package com.simplemobiletools.gallery.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.provider.DocumentFile;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import com.simplemobiletools.fileproperties.dialogs.PropertiesDialog;
import com.simplemobiletools.gallery.Constants;
import com.simplemobiletools.gallery.MyViewPager;
import com.simplemobiletools.gallery.R;
import com.simplemobiletools.gallery.Utils;
import com.simplemobiletools.gallery.adapters.MyPagerAdapter;
import com.simplemobiletools.gallery.asynctasks.CopyTask;
import com.simplemobiletools.gallery.dialogs.CopyDialog;
import com.simplemobiletools.gallery.dialogs.RenameFileDialog;
import com.simplemobiletools.gallery.fragments.ViewPagerFragment;
import com.simplemobiletools.gallery.models.Medium;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ViewPagerActivity extends SimpleActivity
        implements ViewPager.OnPageChangeListener, View.OnSystemUiVisibilityChangeListener, ViewPager.OnTouchListener,
        ViewPagerFragment.FragmentClickListener, CopyTask.CopyDoneListener {
    @BindView(R.id.undo_delete) View mUndoBtn;
    @BindView(R.id.view_pager) MyViewPager mPager;

    private static final int EDIT_IMAGE = 1;
    private static final int SET_WALLPAPER = 2;
    private static ActionBar mActionbar;
    private static List<Medium> mMedia;
    private static String mPath;
    private static String mDirectory;
    private static String mToBeDeleted;
    private static String mBeingDeleted;

    private static boolean mIsFullScreen;
    private static boolean mIsUndoShown;
    private static int mPos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medium);
        ButterKnife.bind(this);

        if (!Utils.Companion.hasStoragePermission(getApplicationContext())) {
            finish();
            return;
        }

        final Uri uri = getIntent().getData();
        if (uri != null) {
            Cursor cursor = null;
            try {
                final String[] proj = {MediaStore.Images.Media.DATA};
                cursor = getContentResolver().query(uri, proj, null, null, null);
                if (cursor != null) {
                    final int dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    cursor.moveToFirst();
                    mPath = cursor.getString(dataIndex);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } else {
            mPath = getIntent().getStringExtra(Constants.MEDIUM);
        }

        if (mPath == null || mPath.isEmpty()) {
            Utils.Companion.showToast(getApplicationContext(), R.string.unknown_error);
            finish();
            return;
        }

        mPos = 0;
        mIsFullScreen = true;
        mActionbar = getSupportActionBar();
        mToBeDeleted = "";
        mBeingDeleted = "";
        hideSystemUI();

        MediaScannerConnection.scanFile(getApplicationContext(), new String[]{mPath}, null, null);
        addUndoMargin();
        mDirectory = new File(mPath).getParent();
        mMedia = getMedia();
        if (isDirEmpty())
            return;

        final MyPagerAdapter adapter = new MyPagerAdapter(this, getSupportFragmentManager(), mMedia);
        mPager.setAdapter(adapter);
        mPager.setCurrentItem(mPos);
        mPager.addOnPageChangeListener(this);
        mPager.setOnTouchListener(this);

        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(this);
        updateActionbarTitle();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Utils.Companion.hasStoragePermission(getApplicationContext())) {
            finish();
        }
    }

    @OnClick(R.id.undo_delete)
    public void undoDeletion() {
        mIsUndoShown = false;
        mToBeDeleted = "";
        mBeingDeleted = "";
        mUndoBtn.setVisibility(View.GONE);
        reloadViewPager();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.viewpager_menu, menu);
        menu.findItem(R.id.menu_set_as_wallpaper).setVisible(getCurrentMedium().isImage());
        menu.findItem(R.id.menu_edit).setVisible(getCurrentMedium().isImage());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        deleteFile();
        switch (item.getItemId()) {
            case R.id.menu_set_as_wallpaper:
                setAsWallpaper();
                return true;
            case R.id.menu_copy:
                displayCopyDialog();
                return true;
            case R.id.menu_open_with:
                openWith();
                return true;
            case R.id.menu_share:
                shareMedium();
                return true;
            case R.id.menu_delete:
                notifyDeletion();
                return true;
            case R.id.menu_rename:
                editMedium();
                return true;
            case R.id.menu_edit:
                openEditor();
                return true;
            case R.id.menu_properties:
                showProperties();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        final MyPagerAdapter adapter = (MyPagerAdapter) mPager.getAdapter();
        adapter.updateItems(mPos);
    }

    private void displayCopyDialog() {
        final File file = getCurrentFile();
        final List<File> files = new ArrayList<>();
        files.add(file);

        new CopyDialog(this, files, this, new CopyDialog.OnCopyListener() {
            @Override
            public void onSuccess() {

            }
        });
    }

    private void openEditor() {
        final Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setDataAndType(Uri.fromFile(getCurrentFile()), "image/*");
        final Intent chooser = Intent.createChooser(intent, getString(R.string.edit_image_with));

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(chooser, EDIT_IMAGE);
        } else {
            Utils.Companion.showToast(getApplicationContext(), R.string.no_editor_found);
        }
    }

    private void setAsWallpaper() {
        final Intent intent = new Intent(Intent.ACTION_ATTACH_DATA);
        intent.setDataAndType(Uri.fromFile(getCurrentFile()), "image/jpeg");
        final Intent chooser = Intent.createChooser(intent, getString(R.string.set_as_wallpaper_with));

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(chooser, SET_WALLPAPER);
        } else {
            Utils.Companion.showToast(getApplicationContext(), R.string.no_wallpaper_setter_found);
        }
    }

    private void openWith() {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(getCurrentFile()), Utils.Companion.getMimeType(getCurrentMedium()));
        final Intent chooser = Intent.createChooser(intent, getString(R.string.open_with));

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(chooser);
        } else {
            Utils.Companion.showToast(getApplicationContext(), R.string.no_app_found);
        }
    }

    private void showProperties() {
        new PropertiesDialog(this, getCurrentFile().getAbsolutePath(), false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == EDIT_IMAGE) {
            if (resultCode == RESULT_OK && data != null) {
                final MyPagerAdapter adapter = (MyPagerAdapter) mPager.getAdapter();
                adapter.updateItems(mPos);
            }
        } else if (requestCode == SET_WALLPAPER) {
            if (resultCode == RESULT_OK) {
                Utils.Companion.showToast(getApplicationContext(), R.string.wallpaper_set_successfully);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void shareMedium() {
        final Medium medium = getCurrentMedium();
        Utils.Companion.shareMedium(medium, this);
    }

    private void notifyDeletion() {
        if (Utils.Companion.isShowingWritePermissions(this, new File(mPath)))
            return;

        mToBeDeleted = getCurrentFile().getAbsolutePath();
        if (mMedia.size() <= 1) {
            deleteFile();
        } else {
            Utils.Companion.showToast(this, R.string.file_deleted);
            mUndoBtn.setVisibility(View.VISIBLE);
            mIsUndoShown = true;
            reloadViewPager();
        }
    }

    private void deleteFile() {
        if (mToBeDeleted.isEmpty())
            return;

        mIsUndoShown = false;
        mBeingDeleted = "";
        boolean mWasFileDeleted = false;

        final File file = new File(mToBeDeleted);
        if (Utils.Companion.needsStupidWritePermissions(this, mToBeDeleted)) {
            if (!Utils.Companion.isShowingWritePermissions(this, file)) {
                final DocumentFile document = Utils.Companion.getFileDocument(this, mToBeDeleted);
                if (document.canWrite()) {
                    mWasFileDeleted = document.delete();
                }
            }
        } else {
            mWasFileDeleted = file.delete();
        }

        if (mWasFileDeleted) {
            mBeingDeleted = mToBeDeleted;
            final String[] deletedPath = new String[]{mToBeDeleted};
            MediaScannerConnection.scanFile(getApplicationContext(), deletedPath, null, new MediaScannerConnection.OnScanCompletedListener() {
                @Override
                public void onScanCompleted(String path, Uri uri) {
                    scanCompleted();
                }
            });
        }

        mToBeDeleted = "";
        mUndoBtn.setVisibility(View.GONE);
    }

    private boolean isDirEmpty() {
        if (mMedia.size() <= 0) {
            deleteDirectoryIfEmpty();
            finish();
            return true;
        }
        return false;
    }

    private void editMedium() {
        new RenameFileDialog(this, getCurrentFile(), new RenameFileDialog.OnRenameFileListener() {
            @Override
            public void onRenameFileSuccess(@NotNull File newFile) {
                mMedia.get(mPager.getCurrentItem()).setPath(newFile.getAbsolutePath());
                updateActionbarTitle();
            }
        });
    }

    private void reloadViewPager() {
        final MyPagerAdapter adapter = (MyPagerAdapter) mPager.getAdapter();
        final int curPos = mPager.getCurrentItem();
        mMedia = getMedia();
        if (isDirEmpty())
            return;

        mPager.setAdapter(null);
        adapter.updateItems(mMedia);
        mPager.setAdapter(adapter);

        final int newPos = Math.min(curPos, adapter.getCount());
        mPager.setCurrentItem(newPos);
        updateActionbarTitle();
    }

    private void deleteDirectoryIfEmpty() {
        final File file = new File(mDirectory);
        if (file.isDirectory() && file.listFiles().length == 0) {
            file.delete();
        }

        final String[] toBeDeleted = new String[]{mDirectory};
        MediaScannerConnection.scanFile(getApplicationContext(), toBeDeleted, null, null);
    }

    private List<Medium> getMedia() {
        final List<Medium> media = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            if (i == 1) {
                uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            }
            final String where = MediaStore.Images.Media.DATA + " like ? ";
            final String[] args = new String[]{mDirectory + "%"};
            final String[] columns = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_MODIFIED, MediaStore.Images.Media.SIZE};
            final Cursor cursor = getContentResolver().query(uri, columns, where, args, null);
            final String pattern = Pattern.quote(mDirectory) + "/[^/]*";

            if (cursor != null && cursor.moveToFirst()) {
                final int pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                do {
                    final String curPath = cursor.getString(pathIndex);
                    if (curPath == null)
                        continue;

                    if (curPath.matches(pattern) && !curPath.equals(mToBeDeleted) && !curPath.equals(mBeingDeleted)) {
                        final int dateIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED);
                        final long timestamp = cursor.getLong(dateIndex);

                        final int sizeIndex = cursor.getColumnIndex(MediaStore.Images.Media.SIZE);
                        final long size = cursor.getLong(sizeIndex);
                        media.add(new Medium("", curPath, i == 1, timestamp, size));
                    }
                } while (cursor.moveToNext());
                cursor.close();
            }
        }

        Medium.mSorting = mConfig.getSorting();
        Collections.sort(media);
        int j = 0;
        for (Medium medium : media) {
            if (medium.getPath().equals(mPath)) {
                mPos = j;
                break;
            }
            j++;
        }
        return media;
    }

    @Override
    public void fragmentClicked() {
        deleteFile();
        mIsFullScreen = !mIsFullScreen;
        if (mIsFullScreen) {
            hideSystemUI();
        } else {
            showSystemUI();
        }
    }

    private void hideSystemUI() {
        Utils.Companion.hideSystemUI(mActionbar, getWindow());
    }

    private void showSystemUI() {
        Utils.Companion.showSystemUI(mActionbar, getWindow());
    }

    private void updateActionbarTitle() {
        setTitle(Utils.Companion.getFilename(mMedia.get(mPager.getCurrentItem()).getPath()));
    }

    private Medium getCurrentMedium() {
        if (mPos >= mMedia.size())
            mPos = mMedia.size() - 1;
        return mMedia.get(mPos);
    }

    private File getCurrentFile() {
        return new File(getCurrentMedium().getPath());
    }

    private void addUndoMargin() {
        final Resources res = getResources();
        final RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mUndoBtn.getLayoutParams();
        final int topMargin = Utils.Companion.getStatusBarHeight(res) + Utils.Companion.getActionBarHeight(getApplicationContext(), res);
        int rightMargin = params.rightMargin;

        if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_PORTRAIT) {
            rightMargin += Utils.Companion.getNavBarHeight(res);
        }

        params.setMargins(params.leftMargin, topMargin, rightMargin, params.bottomMargin);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        updateActionbarTitle();
        mPos = position;
        supportInvalidateOptionsMenu();
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (state == ViewPager.SCROLL_STATE_DRAGGING) {
            final MyPagerAdapter adapter = (MyPagerAdapter) mPager.getAdapter();
            adapter.itemDragged(mPos);
        }
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
            mIsFullScreen = false;
        }

        final MyPagerAdapter adapter = (MyPagerAdapter) mPager.getAdapter();
        adapter.updateUiVisibility(mIsFullScreen, mPos);
    }

    private void scanCompleted() {
        mBeingDeleted = "";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mMedia != null && mMedia.size() <= 1) {
                    reloadViewPager();
                }
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mIsUndoShown) {
            deleteFile();
        }

        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        deleteFile();
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

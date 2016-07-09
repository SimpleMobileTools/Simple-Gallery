package com.simplemobiletools.gallery.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.simplemobiletools.gallery.Constants;
import com.simplemobiletools.gallery.MyViewPager;
import com.simplemobiletools.gallery.R;
import com.simplemobiletools.gallery.Utils;
import com.simplemobiletools.gallery.adapters.MyPagerAdapter;
import com.simplemobiletools.gallery.fragments.ViewPagerFragment;
import com.simplemobiletools.gallery.models.Medium;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ViewPagerActivity extends AppCompatActivity
        implements ViewPager.OnPageChangeListener, View.OnSystemUiVisibilityChangeListener, ViewPager.OnTouchListener,
        ViewPagerFragment.FragmentClickListener {
    @BindView(R.id.undo_delete) View mUndoBtn;
    @BindView(R.id.view_pager) MyViewPager mPager;

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

        if (!Utils.hasStoragePermission(getApplicationContext())) {
            finish();
            return;
        }

        final Uri uri = getIntent().getData();
        if (uri != null) {
            Cursor cursor = null;
            try {
                final String[] proj = {MediaStore.Images.Media.DATA};
                cursor = getContentResolver().query(uri, proj, null, null, null);
                final int dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                mPath = cursor.getString(dataIndex);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } else {
            mPath = getIntent().getStringExtra(Constants.MEDIUM);
        }

        if (mPath == null || mPath.isEmpty()) {
            Utils.showToast(getApplicationContext(), R.string.unknown_error);
            finish();
            return;
        }

        mPos = 0;
        mIsFullScreen = true;
        mActionbar = getSupportActionBar();
        mToBeDeleted = "";
        mBeingDeleted = "";
        hideSystemUI();

        MediaScannerConnection.scanFile(this, new String[]{mPath}, null, null);
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
        if (!Utils.hasStoragePermission(getApplicationContext())) {
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        deleteFile();
        switch (item.getItemId()) {
            case R.id.menu_share:
                shareMedium();
                return true;
            case R.id.menu_delete:
                notifyDeletion();
                return true;
            case R.id.menu_edit:
                editMedium();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void shareMedium() {
        final String shareTitle = getResources().getString(R.string.share_via);
        final Intent sendIntent = new Intent();
        final Medium medium = getCurrentMedium();
        final File file = getCurrentFile();
        final Uri uri = Uri.fromFile(file);
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
        if (medium.getIsVideo()) {
            sendIntent.setType("video/*");
        } else {
            sendIntent.setType("image/*");
        }
        startActivity(Intent.createChooser(sendIntent, shareTitle));
    }

    private void notifyDeletion() {
        mToBeDeleted = getCurrentFile().getAbsolutePath();

        if (mMedia.size() <= 1) {
            deleteFile();
        } else {
            Utils.showToast(this, R.string.file_deleted);
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

        final File file = new File(mToBeDeleted);
        if (file.delete()) {
            mBeingDeleted = mToBeDeleted;
            final String[] deletedPath = new String[]{mToBeDeleted};
            MediaScannerConnection.scanFile(this, deletedPath, null, new MediaScannerConnection.OnScanCompletedListener() {
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
        final File file = getCurrentFile();
        final String fullName = file.getName();
        final int dotAt = fullName.lastIndexOf(".");
        if (dotAt <= 0)
            return;

        final String name = fullName.substring(0, dotAt);
        final String extension = fullName.substring(dotAt + 1, fullName.length());

        final View renameFileView = getLayoutInflater().inflate(R.layout.rename_file, null);
        final EditText fileNameET = (EditText) renameFileView.findViewById(R.id.file_name);
        fileNameET.setText(name);

        final EditText extensionET = (EditText) renameFileView.findViewById(R.id.extension);
        extensionET.setText(extension);

        final TextView filePath = (TextView) renameFileView.findViewById(R.id.file_path);
        filePath.setText(file.getParent() + "/");

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.rename_file));
        builder.setView(renameFileView);

        builder.setPositiveButton("OK", null);
        builder.setNegativeButton("Cancel", null);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String fileName = fileNameET.getText().toString().trim();
                final String extension = extensionET.getText().toString().trim();

                if (fileName.isEmpty() || extension.isEmpty()) {
                    Utils.showToast(getApplicationContext(), R.string.rename_file_empty);
                    return;
                }

                final File newFile = new File(file.getParent(), fileName + "." + extension);

                if (file.renameTo(newFile)) {
                    final int currItem = mPager.getCurrentItem();
                    mMedia.set(currItem, new Medium(newFile.getAbsolutePath(), mMedia.get(currItem).getIsVideo(), 0));

                    final String[] changedFiles = {file.getAbsolutePath(), newFile.getAbsolutePath()};
                    MediaScannerConnection.scanFile(getApplicationContext(), changedFiles, null, null);
                    updateActionbarTitle();
                    alertDialog.dismiss();
                } else {
                    Utils.showToast(getApplicationContext(), R.string.rename_file_error);
                }
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
            final String[] columns = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_TAKEN};
            final Cursor cursor = getContentResolver().query(uri, columns, where, args, null);
            final String pattern = Pattern.quote(mDirectory) + "/[^/]*";

            if (cursor != null && cursor.moveToFirst()) {
                final int pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                do {
                    final String curPath = cursor.getString(pathIndex);
                    if (curPath.matches(pattern) && !curPath.equals(mToBeDeleted) && !curPath.equals(mBeingDeleted)) {
                        final int dateIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
                        final long timestamp = cursor.getLong(dateIndex);
                        media.add(new Medium(curPath, i == 1, timestamp));
                    }
                } while (cursor.moveToNext());
                cursor.close();
            }
        }

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
        Utils.hideSystemUI(mActionbar, getWindow());
    }

    private void showSystemUI() {
        Utils.showSystemUI(mActionbar, getWindow());
    }

    private void updateActionbarTitle() {
        setTitle(Utils.getFilename(mMedia.get(mPager.getCurrentItem()).getPath()));
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
        final int topMargin = Utils.getStatusBarHeight(res) + Utils.getActionBarHeight(getApplicationContext(), res);
        int rightMargin = params.rightMargin;

        if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_PORTRAIT) {
            rightMargin += Utils.getNavBarHeight(res);
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
}

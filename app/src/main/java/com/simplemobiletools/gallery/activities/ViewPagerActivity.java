package com.simplemobiletools.gallery.activities;

import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import com.simplemobiletools.gallery.Constants;
import com.simplemobiletools.gallery.MyViewPager;
import com.simplemobiletools.gallery.R;
import com.simplemobiletools.gallery.Utils;
import com.simplemobiletools.gallery.adapters.MyPagerAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ViewPagerActivity extends AppCompatActivity
        implements ViewPager.OnPageChangeListener, View.OnSystemUiVisibilityChangeListener, MediaScannerConnection.OnScanCompletedListener,
        ViewPager.OnTouchListener {
    private int pos;
    private boolean isFullScreen;
    private ActionBar actionbar;
    private List<String> photos;
    private MyViewPager pager;
    private String path;
    private String directory;
    private Snackbar snackbar;
    private boolean isSnackbarShown;
    private String toBeDeleted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        pos = 0;
        isFullScreen = true;
        actionbar = getSupportActionBar();
        toBeDeleted = "";
        hideSystemUI();

        path = getIntent().getStringExtra(Constants.PHOTO);
        MediaScannerConnection.scanFile(this, new String[]{path}, null, this);
        directory = new File(path).getParent();
        pager = (MyViewPager) findViewById(R.id.view_pager);
        photos = getPhotos();
        if (isDirEmpty())
            return;

        final MyPagerAdapter adapter = new MyPagerAdapter(getSupportFragmentManager());
        adapter.setPaths(photos);
        pager.setAdapter(adapter);
        pager.setCurrentItem(pos);
        pager.addOnPageChangeListener(this);
        pager.setOnTouchListener(this);

        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(this);
        updateActionbarTitle();
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
                shareImage();
                return true;
            case R.id.menu_remove:
                notifyDeletion();
                return true;
            case R.id.menu_edit:
                editImage();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void shareImage() {
        final String shareTitle = getResources().getString(R.string.share_via);
        final Intent sendIntent = new Intent();
        final File file = getCurrentFile();
        final Uri uri = Uri.fromFile(file);
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sendIntent.setType("image/*");
        startActivity(Intent.createChooser(sendIntent, shareTitle));
    }

    private void notifyDeletion() {
        toBeDeleted = getCurrentFile().getAbsolutePath();

        if (photos.size() <= 1) {
            deleteFile();
        } else {
            final CoordinatorLayout coordinator = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
            final Resources res = getResources();
            final String curFileName = getCurrentFile().getName() + " ";
            snackbar = Snackbar.make(coordinator, curFileName + res.getString(R.string.file_deleted), Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction(res.getString(R.string.undo), undoDeletion);
            snackbar.setActionTextColor(Color.WHITE);
            snackbar.show();
            isSnackbarShown = true;
            reloadViewPager();
        }
    }

    private void deleteFile() {
        if (toBeDeleted.isEmpty())
            return;

        if (snackbar != null)
            snackbar.dismiss();

        isSnackbarShown = false;

        final File file = new File(toBeDeleted);
        if (file.delete()) {
            final String[] deletedPath = new String[]{toBeDeleted};
            MediaScannerConnection.scanFile(this, deletedPath, null, this);
        }
        toBeDeleted = "";
    }

    private View.OnClickListener undoDeletion = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            snackbar.dismiss();
            isSnackbarShown = false;
            toBeDeleted = "";
            reloadViewPager();
        }
    };

    private boolean isDirEmpty() {
        if (photos.size() <= 0) {
            deleteDirectoryIfEmpty();
            finish();
            return true;
        }
        return false;
    }

    private void editImage() {
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
                    photos.set(pager.getCurrentItem(), newFile.getAbsolutePath());

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
        final MyPagerAdapter adapter = (MyPagerAdapter) pager.getAdapter();
        final int curPos = pager.getCurrentItem();
        photos = getPhotos();
        if (isDirEmpty())
            return;

        pager.setAdapter(null);
        adapter.updateItems(photos);
        pager.setAdapter(adapter);

        final int newPos = Math.min(curPos, adapter.getCount());
        pager.setCurrentItem(newPos);
        updateActionbarTitle();
    }

    private void deleteDirectoryIfEmpty() {
        final File file = new File(directory);
        if (file.isDirectory() && file.listFiles().length == 0) {
            file.delete();
        }

        final String[] toBeDeleted = new String[]{directory};
        MediaScannerConnection.scanFile(getApplicationContext(), toBeDeleted, null, null);
    }

    private List<String> getPhotos() {
        final List<String> photos = new ArrayList<>();
        final Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        final String where = MediaStore.Images.Media.DATA + " like ? ";
        final String[] args = new String[]{directory + "%"};
        final String[] columns = {MediaStore.Images.Media.DATA};
        final String order = MediaStore.Images.Media.DATE_MODIFIED + " DESC";
        final Cursor cursor = getContentResolver().query(uri, columns, where, args, order);
        final String pattern = Pattern.quote(directory) + "/[^/]*";

        int i = 0;
        if (cursor != null && cursor.moveToFirst()) {
            final int pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            do {
                final String curPath = cursor.getString(pathIndex);

                if (curPath.matches(pattern) && !curPath.equals(toBeDeleted)) {
                    photos.add(curPath);

                    if (curPath.equals(path))
                        pos = i;

                    i++;
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        return photos;
    }

    public void photoClicked() {
        deleteFile();
        isFullScreen = !isFullScreen;
        if (isFullScreen) {
            hideSystemUI();
        } else {
            showSystemUI();
        }
    }

    private void hideSystemUI() {
        if (actionbar != null)
            actionbar.hide();

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LOW_PROFILE |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    private void showSystemUI() {
        if (actionbar != null)
            actionbar.show();

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    private void updateActionbarTitle() {
        setTitle(Utils.getFilename(photos.get(pager.getCurrentItem())));
    }

    private File getCurrentFile() {
        return new File(photos.get(pager.getCurrentItem()));
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        updateActionbarTitle();
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
            isFullScreen = false;
        }
    }

    @Override
    public void onScanCompleted(String path, Uri uri) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                reloadViewPager();
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (isSnackbarShown) {
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

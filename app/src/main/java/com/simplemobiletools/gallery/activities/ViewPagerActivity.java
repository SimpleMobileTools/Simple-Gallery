package com.simplemobiletools.gallery.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
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

import com.simplemobiletools.gallery.Constants;
import com.simplemobiletools.gallery.Media;
import com.simplemobiletools.gallery.MyViewPager;
import com.simplemobiletools.gallery.R;
import com.simplemobiletools.gallery.Utils;
import com.simplemobiletools.gallery.adapters.MyPagerAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ViewPagerActivity extends AppCompatActivity
        implements ViewPager.OnPageChangeListener, View.OnSystemUiVisibilityChangeListener, MediaScannerConnection.OnScanCompletedListener,
        ViewPager.OnTouchListener {
    @BindView(R.id.undo_delete) View undoBtn;
    @BindView(R.id.view_pager) MyViewPager pager;

    private int pos;
    private boolean isFullScreen;
    private ActionBar actionbar;
    private List<Media> media;
    private String path;
    private String directory;
    private boolean isUndoShown;
    private String toBeDeleted;
    private String beingDeleted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medium);
        ButterKnife.bind(this);

        pos = 0;
        isFullScreen = true;
        actionbar = getSupportActionBar();
        toBeDeleted = "";
        beingDeleted = "";
        hideSystemUI();

        path = getIntent().getStringExtra(Constants.MEDIUM);
        MediaScannerConnection.scanFile(this, new String[]{path}, null, null);
        addUndoMargin();
        directory = new File(path).getParent();
        media = getMedia();
        if (isDirEmpty())
            return;

        final MyPagerAdapter adapter = new MyPagerAdapter(getSupportFragmentManager(), media);
        pager.setAdapter(adapter);
        pager.setCurrentItem(pos);
        pager.addOnPageChangeListener(this);
        pager.setOnTouchListener(this);
        pager.setPageTransformer(true, new DepthPageTransformer());

        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(this);
        updateActionbarTitle();
    }

    @OnClick(R.id.undo_delete)
    public void undoDeletion() {
        isUndoShown = false;
        toBeDeleted = "";
        beingDeleted = "";
        undoBtn.setVisibility(View.GONE);
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
        final File file = getCurrentFile();
        final Uri uri = Uri.fromFile(file);
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sendIntent.setType("image/*");
        startActivity(Intent.createChooser(sendIntent, shareTitle));
    }

    private void notifyDeletion() {
        toBeDeleted = getCurrentFile().getAbsolutePath();

        if (media.size() <= 1) {
            deleteFile();
        } else {
            Utils.showToast(this, R.string.file_deleted);
            undoBtn.setVisibility(View.VISIBLE);
            isUndoShown = true;
            reloadViewPager();
        }
    }

    private void deleteFile() {
        if (toBeDeleted.isEmpty())
            return;

        isUndoShown = false;
        beingDeleted = "";

        final File file = new File(toBeDeleted);
        if (file.delete()) {
            beingDeleted = toBeDeleted;
            final String[] deletedPath = new String[]{toBeDeleted};
            MediaScannerConnection.scanFile(this, deletedPath, null, this);
        }
        toBeDeleted = "";
        undoBtn.setVisibility(View.GONE);
    }

    private boolean isDirEmpty() {
        if (media.size() <= 0) {
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
                    final int currItem = pager.getCurrentItem();
                    media.set(currItem, new Media(newFile.getAbsolutePath(), media.get(currItem).getIsVideo()));

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
        media = getMedia();
        if (isDirEmpty())
            return;

        pager.setAdapter(null);
        adapter.updateItems(media);
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

    private List<Media> getMedia() {
        final List<Media> myMedia = new ArrayList<>();
        int j = 0;
        for (int i = 0; i < 2; i++) {
            Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            if (i == 1) {
                uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            }
            final String where = MediaStore.Images.Media.DATA + " like ? ";
            final String[] args = new String[]{directory + "%"};
            final String[] columns = {MediaStore.Images.Media.DATA};
            final String order = MediaStore.Images.Media.DATE_MODIFIED + " DESC";
            final Cursor cursor = getContentResolver().query(uri, columns, where, args, order);
            final String pattern = Pattern.quote(directory) + "/[^/]*";

            if (cursor != null && cursor.moveToFirst()) {
                final int pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                do {
                    final String curPath = cursor.getString(pathIndex);
                    if (curPath.matches(pattern) && !curPath.equals(toBeDeleted) && !curPath.equals(beingDeleted)) {
                        myMedia.add(new Media(curPath, i == 1));

                        if (curPath.equals(path)) {
                            pos = j;
                        }

                        j++;
                    }
                } while (cursor.moveToNext());
                cursor.close();
            }
        }
        return myMedia;
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
        setTitle(Utils.getFilename(media.get(pager.getCurrentItem()).getPath()));
    }

    private File getCurrentFile() {
        return new File(media.get(pos).getPath());
    }

    private void addUndoMargin() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final Resources resources = getResources();
            int id = resources.getIdentifier("navigation_bar_height", "dimen", "android");
            if (id > 0) {
                final RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) undoBtn.getLayoutParams();
                final int navbarHeight = resources.getDimensionPixelSize(id);
                int rightMargin = params.rightMargin;
                int bottomMargin = params.bottomMargin;

                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    bottomMargin = navbarHeight;
                } else {
                    rightMargin = navbarHeight;
                }

                params.setMargins(params.leftMargin, params.topMargin, rightMargin, bottomMargin);
            }
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        updateActionbarTitle();
        pos = position;
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (state == ViewPager.SCROLL_STATE_DRAGGING) {
            final MyPagerAdapter adapter = (MyPagerAdapter) pager.getAdapter();
            adapter.itemDragged();
        }
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
            isFullScreen = false;
        }
    }

    @Override
    public void onScanCompleted(String path, Uri uri) {
        beingDeleted = "";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (media.size() <= 1)
                    reloadViewPager();
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (isUndoShown) {
            deleteFile();
        }

        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        deleteFile();
    }

    public class DepthPageTransformer implements ViewPager.PageTransformer {
        private static final float MIN_SCALE = 0.75f;

        public void transformPage(View view, float position) {
            int pageWidth = view.getWidth();

            if (position < -1) {
                view.setAlpha(0);
            } else if (position <= 0) {
                view.setAlpha(1);
                view.setTranslationX(0);
                view.setScaleX(1);
                view.setScaleY(1);
            } else if (position <= 1) {
                view.setAlpha(1 - position);
                view.setTranslationX(pageWidth * -position);
                float scaleFactor = MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position));
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);
            } else {
                view.setAlpha(0);
            }
        }
    }
}

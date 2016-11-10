package com.simplemobiletools.gallery.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;

import com.simplemobiletools.gallery.Constants;
import com.simplemobiletools.gallery.R;
import com.simplemobiletools.gallery.Utils;
import com.simplemobiletools.gallery.fragments.PhotoFragment;
import com.simplemobiletools.gallery.fragments.VideoFragment;
import com.simplemobiletools.gallery.fragments.ViewPagerFragment;
import com.simplemobiletools.gallery.models.Medium;

import java.io.File;

public class PhotoVideoActivity extends SimpleActivity implements ViewPagerFragment.FragmentClickListener {
    private static ActionBar mActionbar;
    private static Uri mUri;
    private static ViewPagerFragment mFragment;

    private static boolean mIsFullScreen;

    protected static boolean mIsVideo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_holder);

        mUri = getIntent().getData();
        if (mUri == null)
            return;

        mActionbar = getSupportActionBar();
        mIsFullScreen = true;
        hideSystemUI();

        final Bundle bundle = new Bundle();
        final File file = new File(mUri.toString());
        final Medium medium = new Medium(file.getName(), mUri.toString(), mIsVideo, 0, file.length());
        bundle.putSerializable(Constants.MEDIUM, medium);

        if (savedInstanceState == null) {
            mFragment = (mIsVideo ? new VideoFragment() : new PhotoFragment());
            mFragment.setListener(this);
            mFragment.setArguments(bundle);
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_holder, mFragment).commit();
        }
        hideSystemUI();

        if (mUri.getScheme().equals("content")) {
            String[] proj = { MediaStore.Images.Media.TITLE };
            Cursor cursor = getContentResolver().query(mUri, proj, null, null, null);
            if (cursor != null && cursor.getCount() != 0) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE);
                cursor.moveToFirst();
                setTitle(cursor.getString(columnIndex));
            }
            if (cursor != null) {
                cursor.close();
            }
        } else {
            setTitle(Utils.Companion.getFilename(mUri.toString()));
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mFragment.updateItem();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.photo_video_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_share:
                shareMedium();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void shareMedium() {
        final String shareTitle = getResources().getString(R.string.share_via);
        final Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, mUri);
        sendIntent.setType(mIsVideo ? "video/*" : "image/*");
        startActivity(Intent.createChooser(sendIntent, shareTitle));
    }

    @Override
    public void fragmentClicked() {
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
}

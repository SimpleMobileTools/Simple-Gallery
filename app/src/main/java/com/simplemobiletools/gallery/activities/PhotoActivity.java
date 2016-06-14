package com.simplemobiletools.gallery.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.simplemobiletools.gallery.Constants;
import com.simplemobiletools.gallery.R;
import com.simplemobiletools.gallery.Utils;
import com.simplemobiletools.gallery.fragments.PhotoFragment;
import com.simplemobiletools.gallery.fragments.ViewPagerFragment;
import com.simplemobiletools.gallery.models.Medium;

public class PhotoActivity extends AppCompatActivity implements ViewPagerFragment.FragmentClickListener {
    private ActionBar actionbar;
    private boolean isFullScreen;
    private Uri uri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo_layout);

        uri = getIntent().getData();
        if (uri == null)
            return;

        actionbar = getSupportActionBar();
        isFullScreen = true;
        hideSystemUI();

        final Bundle bundle = new Bundle();
        final Medium medium = new Medium(uri.toString(), false);
        bundle.putSerializable(Constants.MEDIUM, medium);
        final ViewPagerFragment fragment = new PhotoFragment();
        fragment.setListener(this);
        fragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction().replace(R.id.photo_holder, fragment).commit();
        hideSystemUI();
        setTitle(Utils.getFilename(uri.toString()));
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
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sendIntent.setType("image/*");
        startActivity(Intent.createChooser(sendIntent, shareTitle));
    }

    @Override
    public void fragmentClicked() {
        isFullScreen = !isFullScreen;
        if (isFullScreen) {
            hideSystemUI();
        } else {
            showSystemUI();
        }
    }

    private void hideSystemUI() {
        Utils.hideSystemUI(actionbar, getWindow());
    }

    private void showSystemUI() {
        Utils.showSystemUI(actionbar, getWindow());
    }
}

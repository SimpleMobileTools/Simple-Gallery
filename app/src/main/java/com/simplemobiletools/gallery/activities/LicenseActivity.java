package com.simplemobiletools.gallery.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.simplemobiletools.gallery.R;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class LicenseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.license_butterknife_title)
    public void butterKnifeClicked() {
        openUrl(R.string.butterknife_url);
    }

    @OnClick(R.id.license_subsampling_title)
    public void subsamplingClicked() {
        openUrl(R.string.subsampling_url);
    }

    @OnClick(R.id.license_exoplayer_title)
    public void exoplayerClicked() {
        openUrl(R.string.exoplayer_url);
    }

    private void openUrl(int id) {
        final String url = getResources().getString(id);
        final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }
}

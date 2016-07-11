package com.simplemobiletools.gallery.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import com.simplemobiletools.gallery.BuildConfig;
import com.simplemobiletools.gallery.Config;
import com.simplemobiletools.gallery.R;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AboutActivity extends AppCompatActivity {
    @BindView(R.id.about_copyright) TextView mCopyright;
    @BindView(R.id.about_email) TextView mEmailTV;
    @BindView(R.id.about_rate_us) View mRateUs;

    private static Resources mRes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        ButterKnife.bind(this);
        mRes = getResources();

        setupEmail();
        setupCopyright();
        setupRateUs();
    }

    private void setupEmail() {
        final String email = mRes.getString(R.string.email);
        final String appName = mRes.getString(R.string.app_name);
        final String href = "<a href=\"mailto:" + email + "?subject=" + appName + "\">" + email + "</a>";
        mEmailTV.setText(Html.fromHtml(href));
        mEmailTV.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void setupCopyright() {
        final String versionName = BuildConfig.VERSION_NAME;
        final int year = Calendar.getInstance().get(Calendar.YEAR);
        final String copyrightText = String.format(mRes.getString(R.string.copyright), versionName, year);
        mCopyright.setText(copyrightText);
    }

    private void setupRateUs() {
        if (Config.newInstance(getApplicationContext()).getIsFirstRun()) {
            mRateUs.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.about_rate_us)
    public void rateUsClicked() {
        final Uri uri = Uri.parse("market://details?id=" + getPackageName());
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException ignored) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }

    @OnClick(R.id.about_license)
    public void licenseClicked() {
        final Intent intent = new Intent(getApplicationContext(), LicenseActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.about_facebook)
    public void facebookClicked() {
        String link = "https://www.facebook.com/simplemobiletools";
        try {
            getPackageManager().getPackageInfo("com.facebook.katana", 0);
            link = "fb://page/150270895341774";
        } catch (Exception ignored) {
        }
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
    }

    @OnClick(R.id.about_gplus)
    public void googlePlusClicked() {
        final String link = "https://plus.google.com/communities/104880861558693868382";
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
    }
}

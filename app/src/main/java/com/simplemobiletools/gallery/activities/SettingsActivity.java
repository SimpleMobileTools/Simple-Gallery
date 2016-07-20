package com.simplemobiletools.gallery.activities;

import android.os.Bundle;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.widget.SwitchCompat;

import com.simplemobiletools.gallery.Config;
import com.simplemobiletools.gallery.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SettingsActivity extends SimpleActivity {
    @BindView(R.id.settings_dark_theme) SwitchCompat mDarkThemeSwitch;

    private static Config mConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        mConfig = Config.newInstance(getApplicationContext());
        ButterKnife.bind(this);

        setupDarkTheme();
    }

    private void setupDarkTheme() {
        mDarkThemeSwitch.setChecked(mConfig.getIsDarkTheme());
    }

    @OnClick(R.id.settings_dark_theme_holder)
    public void handleDarkTheme() {
        mDarkThemeSwitch.setChecked(!mDarkThemeSwitch.isChecked());
        mConfig.setIsDarkTheme(mDarkThemeSwitch.isChecked());
        restartActivity();
    }

    private void restartActivity() {
        TaskStackBuilder.create(getApplicationContext()).addNextIntentWithParentStack(getIntent()).startActivities();
    }
}

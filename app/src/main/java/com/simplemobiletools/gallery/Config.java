package com.simplemobiletools.gallery;

import android.content.Context;
import android.content.SharedPreferences;

public class Config {
    private SharedPreferences mPrefs;

    public static Config newInstance(Context context) {
        return new Config(context);
    }

    public Config(Context context) {
        mPrefs = context.getSharedPreferences(Constants.PREFS_KEY, Context.MODE_PRIVATE);
    }

    public boolean getIsFirstRun() {
        return mPrefs.getBoolean(Constants.IS_FIRST_RUN, true);
    }

    public void setIsFirstRun(boolean firstRun) {
        mPrefs.edit().putBoolean(Constants.IS_FIRST_RUN, firstRun).apply();
    }

    public boolean getIsDarkTheme() {
        return mPrefs.getBoolean(Constants.IS_DARK_THEME, false);
    }

    public void setIsDarkTheme(boolean isDarkTheme) {
        mPrefs.edit().putBoolean(Constants.IS_DARK_THEME, isDarkTheme).apply();
    }

    public boolean getIsSameSorting() {
        return mPrefs.getBoolean(Constants.IS_SAME_SORTING, true);
    }

    public void setIsSameSorting(boolean isSameSorting) {
        mPrefs.edit().putBoolean(Constants.IS_SAME_SORTING, isSameSorting).apply();
    }

    public int getSorting() {
        if (getIsSameSorting())
            return getDirectorySorting();

        return mPrefs.getInt(Constants.SORT_ORDER, Constants.SORT_BY_DATE | Constants.SORT_DESCENDING);
    }

    public void setSorting(int order) {
        if (getIsSameSorting())
            setDirectorySorting(order);
        else
            mPrefs.edit().putInt(Constants.SORT_ORDER, order).apply();
    }

    public int getDirectorySorting() {
        return mPrefs.getInt(Constants.DIRECTORY_SORT_ORDER, Constants.SORT_BY_NAME);
    }

    public void setDirectorySorting(int order) {
        mPrefs.edit().putInt(Constants.DIRECTORY_SORT_ORDER, order).apply();
    }
}

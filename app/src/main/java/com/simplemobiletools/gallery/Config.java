package com.simplemobiletools.gallery;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class Config {
    private SharedPreferences mPrefs;

    public static Config newInstance(Context context) {
        return new Config(context);
    }

    private Config(Context context) {
        mPrefs = context.getSharedPreferences(Constants.PREFS_KEY, Context.MODE_PRIVATE);
    }

    public boolean getIsFirstRun() {
        return mPrefs.getBoolean(Constants.IS_FIRST_RUN, true);
    }

    public void setIsFirstRun(boolean firstRun) {
        mPrefs.edit().putBoolean(Constants.IS_FIRST_RUN, firstRun).apply();
    }

    public boolean getIsDarkTheme() {
        return mPrefs.getBoolean(Constants.IS_DARK_THEME, true);
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
        return mPrefs.getInt(Constants.DIRECTORY_SORT_ORDER, Constants.SORT_BY_DATE | Constants.SORT_DESCENDING);
    }

    public void setDirectorySorting(int order) {
        mPrefs.edit().putInt(Constants.DIRECTORY_SORT_ORDER, order).apply();
    }

    public boolean getShowHiddenFolders() {
        return mPrefs.getBoolean(Constants.SHOW_HIDDEN_FOLDERS, false);
    }

    public void setShowHiddenFolders(boolean showHiddenFolders) {
        mPrefs.edit().putBoolean(Constants.SHOW_HIDDEN_FOLDERS, showHiddenFolders).apply();
    }

    public void addHiddenDirectory(String path) {
        final Set<String> hiddenFolders = getHiddenFolders();
        hiddenFolders.add(path);
        mPrefs.edit().putStringSet(Constants.HIDDEN_FOLDERS, hiddenFolders).apply();
    }

    public void addHiddenDirectories(Set<String> paths) {
        final Set<String> hiddenFolders = getHiddenFolders();
        hiddenFolders.addAll(paths);
        mPrefs.edit().putStringSet(Constants.HIDDEN_FOLDERS, hiddenFolders).apply();
    }

    public void removeHiddenDirectory(String path) {
        final Set<String> hiddenFolders = getHiddenFolders();
        hiddenFolders.remove(path);
        mPrefs.edit().putStringSet(Constants.HIDDEN_FOLDERS, hiddenFolders).apply();
    }

    public void removeHiddenDirectories(Set<String> paths) {
        final Set<String> hiddenFolders = getHiddenFolders();
        hiddenFolders.removeAll(paths);
        mPrefs.edit().putStringSet(Constants.HIDDEN_FOLDERS, hiddenFolders).apply();
    }

    public Set<String> getHiddenFolders() {
        return mPrefs.getStringSet(Constants.HIDDEN_FOLDERS, new HashSet<String>());
    }

    public boolean getIsFolderHidden(String path) {
        return getHiddenFolders().contains(path);
    }

    public boolean getAutoplayVideos() {
        return mPrefs.getBoolean(Constants.AUTOPLAY_VIDEOS, false);
    }

    public void setAutoplayVideos(boolean autoplay) {
        mPrefs.edit().putBoolean(Constants.AUTOPLAY_VIDEOS, autoplay).apply();
    }

    public String getTreeUri() {
        return mPrefs.getString(Constants.TREE_URI, "");
    }

    public void setTreeUri(String uri) {
        mPrefs.edit().putString(Constants.TREE_URI, uri).apply();
    }

    public boolean getDisplayFileNames() {
        return mPrefs.getBoolean(Constants.DISPLAY_FILE_NAMES, false);
    }

    public void setDisplayFileNames(boolean display) {
        mPrefs.edit().putBoolean(Constants.DISPLAY_FILE_NAMES, display).apply();
    }
}

package gallery.simplemobiletools.com.activities;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import gallery.simplemobiletools.com.Constants;
import gallery.simplemobiletools.com.MyViewPager;
import gallery.simplemobiletools.com.R;
import gallery.simplemobiletools.com.adapters.MyPagerAdapter;

public class ViewPagerActivity extends AppCompatActivity {
    private int pos;
    private boolean isFullScreen;
    private ActionBar actionbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        pos = 0;
        actionbar = getSupportActionBar();
        isFullScreen = true;
        hideSystemUI();

        final MyViewPager pager = (MyViewPager) findViewById(R.id.view_pager);
        final MyPagerAdapter adapter = new MyPagerAdapter(getSupportFragmentManager());
        adapter.setPaths(getPhotos());
        pager.setAdapter(adapter);
        pager.setCurrentItem(pos);

        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    isFullScreen = false;
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_share:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private List<String> getPhotos() {
        final List<String> photos = new ArrayList<>();
        final String path = getIntent().getStringExtra(Constants.PHOTO);
        final String fileDir = new File(path).getParent().toLowerCase();
        final Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        final String where = MediaStore.Images.Media.DATA + " like ? ";
        final String[] args = new String[]{fileDir + "%"};
        final String[] columns = {MediaStore.Images.Media.DATA};
        final String order = MediaStore.Images.Media.DATE_MODIFIED + " DESC";
        final Cursor cursor = getContentResolver().query(uri, columns, where, args, order);
        final String pattern = Pattern.quote(fileDir) + "/[^/]*";

        int i = 0;
        if (cursor != null && cursor.moveToFirst()) {
            final int pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            do {
                final String curPath = cursor.getString(pathIndex);
                if (curPath.toLowerCase().matches(pattern)) {
                    photos.add(curPath);

                    if (curPath.equals(path))
                        pos = i;
                }

                i++;
            } while (cursor.moveToNext());
            cursor.close();
        }
        return photos;
    }

    public void photoClicked() {
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
}

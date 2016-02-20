package gallery.simplemobiletools.com;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class PhotosActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photos);

        final String path = getIntent().getStringExtra(Constants.DIRECTORY);
    }
}

package gallery.simplemobiletools.com;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.util.List;

public class MyPagerAdapter extends PagerAdapter {
    private final Context context;
    private final List<String> paths;
    private final LayoutInflater inflater;

    public MyPagerAdapter(Context context, List<String> paths) {
        this.context = context;
        this.paths = paths;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return paths.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        final View view = inflater.inflate(R.layout.pager_item, container, false);
        final ImageView imageView = (ImageView) view.findViewById(R.id.photo);
        Glide.with(context).load(paths.get(position)).fitCenter().crossFade().into(imageView);
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }
}

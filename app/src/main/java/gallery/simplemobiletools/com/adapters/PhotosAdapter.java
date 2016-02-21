package gallery.simplemobiletools.com.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.util.List;

import gallery.simplemobiletools.com.R;

public class PhotosAdapter extends BaseAdapter {
    private final Context context;
    private final List<String> photos;
    private final LayoutInflater inflater;

    public PhotosAdapter(Context context, List<String> photos) {
        this.context = context;
        this.photos = photos;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder holder;
        if (view == null) {
            view = inflater.inflate(R.layout.photo_item, parent, false);
            holder = new ViewHolder(view);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        String path = photos.get(position);
        Glide.with(context).load(path).centerCrop().crossFade().into(holder.photoThumbnail);

        return view;
    }

    @Override
    public int getCount() {
        return photos.size();
    }

    @Override
    public Object getItem(int position) {
        return photos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    static class ViewHolder {
        ImageView photoThumbnail;

        public ViewHolder(View view) {
            photoThumbnail = (ImageView) view.findViewById(R.id.photo_thumbnail);
        }
    }
}

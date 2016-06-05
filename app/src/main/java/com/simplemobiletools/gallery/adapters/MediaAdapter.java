package com.simplemobiletools.gallery.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.simplemobiletools.gallery.Media;
import com.simplemobiletools.gallery.R;

import java.util.List;

public class MediaAdapter extends BaseAdapter {
    private final Context context;
    private final List<Media> media;
    private final LayoutInflater inflater;

    public MediaAdapter(Context context, List<Media> media) {
        this.context = context;
        this.media = media;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder holder;
        if (view == null) {
            view = inflater.inflate(R.layout.medium_item, parent, false);
            holder = new ViewHolder(view);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        final String path = media.get(position).getPath();
        Glide.with(context).load(path).placeholder(R.color.tmb_background).centerCrop().crossFade().into(holder.photoThumbnail);

        return view;
    }

    @Override
    public int getCount() {
        return media.size();
    }

    @Override
    public Object getItem(int position) {
        return media.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public void updateItems(List<Media> newPhotos) {
        media.clear();
        media.addAll(newPhotos);
        notifyDataSetChanged();
    }

    static class ViewHolder {
        ImageView photoThumbnail;

        public ViewHolder(View view) {
            photoThumbnail = (ImageView) view.findViewById(R.id.medium_thumbnail);
        }
    }
}

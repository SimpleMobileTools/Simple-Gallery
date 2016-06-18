package com.simplemobiletools.gallery.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.simplemobiletools.gallery.R;
import com.simplemobiletools.gallery.models.Medium;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MediaAdapter extends BaseAdapter {
    private final Context context;
    private final List<Medium> media;
    private final LayoutInflater inflater;

    public MediaAdapter(Context context, List<Medium> media) {
        this.context = context;
        this.media = media;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        final Medium medium = media.get(position);
        ViewHolder holder;
        if (view == null) {
            view = inflater.inflate(R.layout.video_item, parent, false);
            holder = new ViewHolder(view);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        if (medium.getIsVideo()) {
            holder.playOutline.setVisibility(View.VISIBLE);
        } else {
            holder.playOutline.setVisibility(View.GONE);
        }

        final String path = medium.getPath();
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

    public void updateItems(List<Medium> newPhotos) {
        media.clear();
        media.addAll(newPhotos);
        notifyDataSetChanged();
    }

    static class ViewHolder {
        @BindView(R.id.medium_thumbnail) ImageView photoThumbnail;
        @BindView(R.id.play_outline) ImageView playOutline;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}

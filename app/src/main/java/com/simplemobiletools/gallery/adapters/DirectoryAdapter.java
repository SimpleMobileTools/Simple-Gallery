package com.simplemobiletools.gallery.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.simplemobiletools.gallery.Directory;
import com.simplemobiletools.gallery.R;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DirectoryAdapter extends BaseAdapter {
    private final Context context;
    private final List<Directory> dirs;
    private final LayoutInflater inflater;

    public DirectoryAdapter(Context context, List<Directory> dirs) {
        this.context = context;
        this.dirs = dirs;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder holder;
        if (view == null) {
            view = inflater.inflate(R.layout.directory_item, parent, false);
            holder = new ViewHolder(view);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        final Directory dir = dirs.get(position);
        holder.dirName.setText(dir.getName());
        holder.photoCnt.setText(String.valueOf(dir.getMediaCnt()));
        Glide.with(context).load(dir.getThumbnail()).placeholder(R.color.tmb_background).centerCrop().crossFade().into(holder.dirThumbnail);

        return view;
    }

    @Override
    public int getCount() {
        return dirs.size();
    }

    @Override
    public Object getItem(int position) {
        return dirs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public void updateItems(List<Directory> newDirs) {
        dirs.clear();
        dirs.addAll(newDirs);
        notifyDataSetChanged();
    }

    static class ViewHolder {
        @BindView(R.id.dir_name) TextView dirName;
        @BindView(R.id.photo_cnt) TextView photoCnt;
        @BindView(R.id.dir_thumbnail) ImageView dirThumbnail;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}

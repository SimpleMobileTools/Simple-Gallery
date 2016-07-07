package com.simplemobiletools.gallery.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.simplemobiletools.gallery.R;
import com.simplemobiletools.gallery.models.Directory;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DirectoryAdapter extends BaseAdapter {
    private final Context mContext;
    private final List<Directory> mDirs;
    private final LayoutInflater mInflater;

    public DirectoryAdapter(Context context, List<Directory> dirs) {
        mContext = context;
        mDirs = dirs;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.directory_item, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final Directory dir = mDirs.get(position);
        viewHolder.dirName.setText(dir.getName());
        viewHolder.photoCnt.setText(String.valueOf(dir.getMediaCnt()));
        final String tmb = dir.getThumbnail();
        if (tmb.endsWith(".gif")) {
            Glide.with(mContext).load(tmb).asGif().diskCacheStrategy(DiskCacheStrategy.NONE).placeholder(R.color.tmb_background)
                    .centerCrop().crossFade().into(viewHolder.dirThumbnail);
        } else {
            Glide.with(mContext).load(tmb).placeholder(R.color.tmb_background).centerCrop().crossFade().into(viewHolder.dirThumbnail);
        }

        return convertView;
    }

    @Override
    public int getCount() {
        return mDirs.size();
    }

    @Override
    public Object getItem(int position) {
        return mDirs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public void updateItems(List<Directory> newDirs) {
        mDirs.clear();
        mDirs.addAll(newDirs);
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

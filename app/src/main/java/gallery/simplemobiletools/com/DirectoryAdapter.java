package gallery.simplemobiletools.com;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class DirectoryAdapter extends BaseAdapter {
    private final List<Directory> dirs;
    private final LayoutInflater inflater;

    public DirectoryAdapter(Context context, List<Directory> dirs) {
        this.dirs = dirs;
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

        Directory dir = dirs.get(position);
        holder.dirName.setText(dir.getName());
        holder.photoCnt.setText(String.valueOf(dir.getPhotoCnt()));

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

    static class ViewHolder {
        TextView dirName;
        TextView photoCnt;

        public ViewHolder(View view) {
            dirName = (TextView) view.findViewById(R.id.dir_name);
            photoCnt = (TextView) view.findViewById(R.id.photo_cnt);
        }
    }
}

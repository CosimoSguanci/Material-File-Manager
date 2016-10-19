package app.android.com.materialfilemanager;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

/**
 * Created by Cosimo Sguanci on 08/09/2016.
 */
public class FileAdapter extends RecyclerView.Adapter<FileAdapter.MyViewHolder> {

    private List<Item> items;
    private Context c;
    private int id;
    private onFileClickedListener listener;

    public FileAdapter(Context c, int id, List<Item> items) {
        this.items = items;
        this.id = id;
        this.c = c;
        this.listener = null;
    }


    public void setOnFileClickedListener(onFileClickedListener listener) {
        this.listener = listener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_row, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Item item = items.get(position);
        holder.textViewName.setText(item.getName());
        holder.textViewSecond.setText(item.getData());
        holder.textViewDate.setText(item.getDate());
        if (item.getImage().equals("directory_icon"))
            holder.icon.setImageResource(R.mipmap.ic_folder_grey600_48dp);
        else if (item.getImage().equals("file_icon")) {
            holder.icon.setImageResource(R.mipmap.ic_file_grey600_48dp);
        } else if (item.getImage().equals("image_icon")) {

            //FIX Image Cropping
            Glide.with(c).load(item.getPath())
                    .thumbnail(0.5f)
                    .crossFade()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(holder.icon.getMaxWidth(), holder.icon.getMaxHeight())
                    //.centerCrop()
                    .into(holder.icon);
        } else if (item.getImage().equals("music_icon")) {
            holder.icon.setImageResource(R.mipmap.ic_headphones_grey600_48dp);
        }


    }


    @Override
    public int getItemCount() {

        return items == null ? 0 : items.size();
    }

    public interface onFileClickedListener {
        void onFileClick(String newPath);

        void onLongFileClick(String path);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName;
        TextView textViewSecond;
        TextView textViewDate;
        ImageView icon;


        public MyViewHolder(View v) {
            super(v);
            textViewName = (TextView) v.findViewById(R.id.text_view_name);
            textViewSecond = (TextView) v.findViewById(R.id.second_text_view);
            textViewDate = (TextView) v.findViewById(R.id.text_view_date);
            icon = (ImageView) v.findViewById(R.id.icon);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onFileClick(items.get(getAdapterPosition()).getPath());

                }
            });

            v.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    listener.onLongFileClick(items.get(getAdapterPosition()).getPath());
                    return false;
                }
            });

        }


    }


}

package d2d.testing.gui.gallery;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;

import d2d.testing.R;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder>{

    private ArrayList<GalleryListData> listdata;
    private GalleryFragment fragment;
    private Boolean someItemSelected;

    // RecyclerView recyclerView;
    public GalleryAdapter(ArrayList<GalleryListData> listdata, GalleryFragment fragment) {
        this.listdata = listdata;
        this.fragment = fragment;
        this.someItemSelected = false;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem= layoutInflater.inflate(R.layout.list_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(listItem);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.textView.setText(listdata.get(position).getPath());
        holder.cardView.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
        if(listdata.get(position).getBitmap()!=null){
            holder.shimmer.stopShimmer();
            holder.shimmer.hideShimmer();
            holder.bitmap = listdata.get(position).getBitmap();
            holder.imageView.setImageBitmap(holder.bitmap);
        }

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(someItemSelected){
                    selectedItem(holder, position, view);
                }
                else fragment.startVideo(position);
            }
        });

        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(someItemSelected) {
                    selectedItem(holder, position, view);
                }
                else fragment.startVideo(position);
            }
        });

        holder.cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                someItemSelected = true;
                return selectedItem(holder, position, v);
            }
        });

        holder.imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                someItemSelected = true;
                return selectedItem(holder, position, v);
            }
        });
    }

    public boolean selectedItem(final ViewHolder holder, final int position, View v){
        if(holder.bitmap!=null) {
            if (!listdata.get(position).isSelected()) {
                holder.cardView.setBackgroundTintList(ColorStateList.valueOf(fragment.getResources().getColor(R.color.colorGray, null)));
                holder.imageView.setImageDrawable(fragment.getResources().getDrawable(R.drawable.my_device_background, null));
                listdata.get(position).setSelected(true);
            } else {
                holder.cardView.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                holder.imageView.setImageBitmap(holder.bitmap);
                listdata.get(position).setSelected(false);
                if(!isSomeSelected()){
                    someItemSelected = false;
                }
            }
        }
        return true;
    }

    private boolean isSomeSelected(){
        for(GalleryListData ld : listdata){
            if(ld.isSelected()) return true;
        }
        return false;
    }

    public void setListData(ArrayList<GalleryListData> listData){
        this.listdata = listData;
        this.someItemSelected = false;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return listdata.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageView;
        private TextView textView;
        private CardView cardView;
        private ShimmerFrameLayout shimmer;
        private Bitmap bitmap;

        private ViewHolder(View itemView) {
            super(itemView);
            this.shimmer = itemView.findViewById(R.id.shimmer_view_bitmap);
            shimmer.startShimmer();
            this.imageView = itemView.findViewById(R.id.imageGallery);
            this.textView = itemView.findViewById(R.id.titlegallery);
            this.cardView = itemView.findViewById(R.id.galleryconstraint);
            bitmap = null;
        }
    }
}




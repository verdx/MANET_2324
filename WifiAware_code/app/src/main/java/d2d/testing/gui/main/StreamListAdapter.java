package d2d.testing.gui.main;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;
import java.util.UUID;

import d2d.testing.R;
import d2d.testing.gui.SaveStream;
import d2d.testing.streaming.StreamingRecord;
import d2d.testing.utils.IOUtils;

public class StreamListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_DATA = 0;
    private static final int VIEW_TYPE_PLACEHOLDER = 1;
    private Context mContext;
    private ArrayList<StreamDetail> mStreams;
    private StreamDetail sd;
    private MainFragment fragment;

    public StreamListAdapter(Context context , ArrayList<StreamDetail> objects, MainFragment fragment) {
        this.mStreams = objects;
        this.mContext = context;
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        RecyclerView.ViewHolder holder;
        if (viewType ==  VIEW_TYPE_DATA) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.stream_detail, parent, false);
            holder = new RealViewHolder(v);
        } else {

            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.stream_detail_placeholder, parent, false);
            holder = new PlaceViewHolder(v);
        }
        return holder;
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
        if(holder instanceof  RealViewHolder){
            sd = mStreams.get(position);

            RealViewHolder realHolder = (RealViewHolder) holder;

            String desc;
            if(sd.getName().equals("defaultName")){
                desc = IOUtils.uuidToBase64(sd.getUuid());
            }
            else desc = sd.getName();

            realHolder.stream_name.setText(desc);

            realHolder.stream_download.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!sd.isDownload()){
                        StreamingRecord.getInstance().changeStreamingDownload(UUID.fromString(sd.getUuid()), true);
                        Toast.makeText(mContext, "Comienza la descarga del stream seleccionado...", Toast.LENGTH_LONG).show();
                        SaveStream saveStream = new SaveStream(mContext.getApplicationContext(), sd.getUuid());
                        saveStream.startDownload();
                    }
                }
            });

            realHolder.stream_download.setBackgroundResource(sd.isDownload()?
                    R.drawable.button_download_pressed :
                    R.drawable.button_download);

            realHolder.stream_layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fragment.openStreamActivity(sd.getUuid());
                }
            });
        }
        else {
            PlaceViewHolder placeHolder = (PlaceViewHolder)holder;
            placeHolder.shimmer.startShimmer();
        }


    }

    @Override
    public int getItemCount() {
        return mStreams == null? 0 : mStreams.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mStreams.get(position) == null? VIEW_TYPE_PLACEHOLDER : VIEW_TYPE_DATA;
    }

    public void setStreamsData(ArrayList<StreamDetail> data){
        mStreams = data;
        notifyDataSetChanged();
    }

    class RealViewHolder extends RecyclerView.ViewHolder{

        TextView stream_name;
        ImageButton stream_download;
        LinearLayout stream_layout;

        public RealViewHolder(@NonNull View itemView) {
            super(itemView);
            stream_name = itemView.findViewById(R.id.stream_name);
            stream_download = itemView.findViewById(R.id.downloadButton);
            stream_layout = itemView.findViewById(R.id.stream_item_list);
        }
    }

    class PlaceViewHolder extends RecyclerView.ViewHolder{

        TextView stream_name;
        ImageButton stream_download;
        LinearLayout stream_layout;
        ShimmerFrameLayout shimmer;

        public PlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            shimmer = itemView.findViewById(R.id.shimmer_view_container);
            stream_name = itemView.findViewById(R.id.stream_name_placeHolder);
            stream_download = itemView.findViewById(R.id.downloadButton_placeHolder);
            stream_layout = itemView.findViewById(R.id.stream_item_list_placeHolder);
        }
    }
}

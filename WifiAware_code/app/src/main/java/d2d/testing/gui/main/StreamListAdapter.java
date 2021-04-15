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

import java.util.ArrayList;

import d2d.testing.R;
import d2d.testing.utils.IOUtils;

public class StreamListAdapter extends RecyclerView.Adapter<StreamListAdapter.ViewHolder> {
    private Context mContext;
    private ArrayList<StreamDetail> mStreams;
    private StreamDetail sd;
    private MainFragment fragment;

    public StreamListAdapter(Context context , ArrayList<StreamDetail> objects, MainFragment fragment) {
        this.mStreams = objects;
        this.mContext = context;
        this.fragment = fragment;
    }

    private void startDownload() {
        Toast.makeText(mContext, "Comienza la descarga del stream seleccionado...", Toast.LENGTH_LONG).show();
    }

    @NonNull
    @Override
    public StreamListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.stream_detail, parent, false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull StreamListAdapter.ViewHolder holder, final int position) {
        sd = mStreams.get(position);

        String desc;
        if(sd.getName().equals("defaultName")){
            desc = IOUtils.uuidToBase64(sd.getUuid());
        }
        else desc = sd.getName();

        holder.stream_nam.setText(desc);

        holder.stream_download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!sd.isDownload()){
                    sd.setDownload(true);
                    notifyDataSetChanged();
                    startDownload();
                }
            }
        });

        holder.stream_download.setBackgroundResource(sd.isDownload()?
                        R.drawable.button_download_pressed :
                        R.drawable.button_download);

        holder.stream_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fragment.openStreamActivity(sd.getUuid());
            }
        });
    }

    @Override
    public int getItemCount() {
        return mStreams.size();
    }

    public void setStreamsData(ArrayList<StreamDetail> data){
        mStreams = data;
    }

    class ViewHolder extends RecyclerView.ViewHolder{

        TextView stream_nam;
        ImageButton stream_download;
        LinearLayout stream_layout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            stream_nam = itemView.findViewById(R.id.stream_name);
            stream_download = itemView.findViewById(R.id.downloadButton);
            stream_layout = itemView.findViewById(R.id.stream_item_list);
        }
    }
}

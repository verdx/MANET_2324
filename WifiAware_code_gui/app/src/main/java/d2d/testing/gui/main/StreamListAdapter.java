package d2d.testing.gui.main;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import d2d.testing.R;

public class StreamListAdapter extends ArrayAdapter<StreamDetail> {
    private Context mContext;

    public StreamListAdapter(Context context , ArrayList<StreamDetail> objects) {
        super(context, -1, objects);
        this.mContext = context;
    }


    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        String uuid = getItem(position).getUuid();

        LayoutInflater inflater = LayoutInflater.from(mContext);
        convertView = inflater.inflate(R.layout.stream_detail,null);

        TextView stream_nam = convertView.findViewById(R.id.stream_name);
        ImageButton stream_download = convertView.findViewById(R.id.downloadButton);

        stream_download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!getItem(position).isDownload()){
                    getItem(position).setDownload(true);
                    notifyDataSetChanged();
                    startDownload();
                }
            }
        });

        stream_nam.setText(uuid);
        stream_download.setBackgroundColor(getItem(position).isDownload()? Color.rgb(9, 148, 185) : Color.GRAY);

        return convertView;
    }

    private void startDownload() {
        Toast.makeText(getContext().getApplicationContext(), "Comienza la descarga del stream seleccionado...", Toast.LENGTH_LONG).show();
    }
}

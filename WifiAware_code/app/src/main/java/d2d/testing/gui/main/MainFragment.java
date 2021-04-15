package d2d.testing.gui.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;

import d2d.testing.R;
import d2d.testing.gui.MainActivity;
import d2d.testing.gui.StreamActivity;
import d2d.testing.gui.ViewStreamActivity;
import d2d.testing.streaming.Streaming;
import d2d.testing.streaming.StreamingRecord;
import d2d.testing.streaming.StreamingRecordObserver;
import d2d.testing.streaming.rtsp.RtspClient;
import d2d.testing.streaming.sessions.SessionBuilder;

public class MainFragment extends Fragment implements StreamingRecordObserver, RtspClient.Callback {

    private TextView myName;
    private TextView myAdd;
    private TextView myStatus;

    private Button record;
    private WifiAwareViewModel mAwareModel;

    private TextView numStreams;

    private ArrayList<StreamDetail> streamList = new ArrayList();
    private StreamListAdapter arrayAdapter;
    private RecyclerView streamsListView;

    private ProgressBar progressBar;

    private Boolean isWifiAwareAvailable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAwareModel = new ViewModelProvider(requireActivity()).get(WifiAwareViewModel.class);
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_main, container, false);

        streamsListView = root.findViewById(R.id.streamListView);
        streamsListView.setLayoutManager(new LinearLayoutManager(getContext()));
        arrayAdapter = new StreamListAdapter(getContext(), streamList, this);
        streamsListView.setAdapter(arrayAdapter);

        progressBar = root.findViewById(R.id.progressBar);

        numStreams = root.findViewById(R.id.streams_available);
        numStreams.setText(getString(R.string.dispositivos_encontrados) + "  (" + streamList.size() + ")");

        StreamingRecord.getInstance().addObserver(this);

        record = root.findViewById(R.id.recordButton);
        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkCameraHardware()) handleCamera();
            }
        });

        @SuppressLint("ResourceType") Animation shake = AnimationUtils.loadAnimation(getContext(), R.drawable.animate);
        record.startAnimation(shake);

        myAdd = root.findViewById(R.id.my_address);
        myName = root.findViewById(R.id.my_name);
        myStatus = root.findViewById(R.id.my_status);

        mAwareModel.isWifiAwareAvailable().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                isWifiAwareAvailable = aBoolean;
                myStatus.setText(getDeviceStatus());
                if(isWifiAwareAvailable && !mAwareModel.sessionCreated()){
                    initWifiAware();
                }
            }
        });

        myName.setText("Model: " +  Settings.Secure.getString(getActivity().getContentResolver(), Settings.Secure.ANDROID_ID));

        String mode = "";

        if(MainActivity.mode != null){
            if(MainActivity.mode.equals(getString(R.string.mode_witness))) mode = getString(R.string.witness);
            if(MainActivity.mode.equals(getString(R.string.mode_humanitarian))) mode = getString(R.string.humanitarian);
        }
        myAdd.setText("Mode:   " + mode);

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        if(streamList.size() != 0) progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        StreamingRecord.getInstance().removeObserver(this);
    }

    public ArrayList<StreamDetail> getStreamList(){
        return this.streamList;
    }

    public void updateList(boolean on_off, String uuid, String name, String ip, int port ){
        if(!ip.equals("0.0.0.0")) {
            StreamDetail detail = new StreamDetail(uuid, name, ip, port);
            if (on_off) {
                if (!streamList.contains(detail))
                    streamList.add(detail);
            } else {
                if (streamList.contains(detail))
                    streamList.remove(detail);
            }
            numStreams.setText(getString(R.string.dispositivos_encontrados) + "  (" + streamList.size() + ")");
            if(streamList.size() != 0) progressBar.setVisibility(View.INVISIBLE);
            else progressBar.setVisibility(View.VISIBLE);
            arrayAdapter.setStreamsData(streamList);
            streamsListView.getAdapter().notifyDataSetChanged();
        }
    }

    public void openStreamActivity(String uuid) {
        openViewStreamActivity(getActivity(), uuid);
    }

    private void initWifiAware(){
        try {
            if(mAwareModel.createSession()){
                if(mAwareModel.publishService("Server")){
                    //Toast.makeText(this.getContext(), "Se creo una sesion de publisher con WifiAware", Toast.LENGTH_SHORT).show();
                }
                else{
                    //Toast.makeText(this.getContext(), "No se pudo crear una sesion de publisher de WifiAware", Toast.LENGTH_LONG).show();
                }
                if(mAwareModel.subscribeToService("Server", this)){
                    //Toast.makeText(this.getContext(), "Se creo una sesion de subscripcion con WifiAware", Toast.LENGTH_SHORT).show();
                }
                else{
                    //Toast.makeText(this.getContext(), "No se pudo crear una sesion de subscripcion de WifiAware", Toast.LENGTH_LONG).show();
                }
            }else {
                //Toast.makeText(this.getContext(), "No se pudo crear la sesion de WifiAware", Toast.LENGTH_LONG).show();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handleCamera(){
        openStreamActivity();
    }

    @SuppressLint("ResourceType")
    public String getDeviceStatus() {
        if (isWifiAwareAvailable) {
            myStatus.setTextColor(Color.parseColor(getString(R.color.colorPrimaryDark)));
            record.setEnabled(true);
            if(streamList.size() == 0) progressBar.setVisibility(View.VISIBLE);
            return "Wifi Aware available";
        }
        else {
            myStatus.setTextColor(Color.parseColor(getString(R.color.colorRed)));
            record.setEnabled(false);
            progressBar.setVisibility(View.INVISIBLE);
            return "Wifi Aware unavailable";
        }
    }

    private boolean checkCameraHardware() {
        if (getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // This device has a camera
            return true;
        } else {
            // No camera on this device
            Toast.makeText(getActivity().getApplicationContext(), "YOUR DEVICE HAS NO CAMERA", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void openStreamActivity() {
        Intent streamActivityIntent = new Intent(getActivity(), StreamActivity.class);
        this.startActivity(streamActivityIntent);
    }

    public void openViewStreamActivity(Context context, String uuid) {
        Intent streamActivityIntent = new Intent(context, ViewStreamActivity.class);
        streamActivityIntent.putExtra("isFromGallery", false);
        streamActivityIntent.putExtra("UUID",uuid);
        this.startActivity(streamActivityIntent);
    }

    @Override
    public void localStreamingAvailable(UUID id, String name, SessionBuilder sessionBuilder) {}

    @Override
    public void localStreamingUnavailable() {}

    @Override
    public void streamingAvailable(final Streaming streaming, boolean bAllowDispatch) {
        final String path = streaming.getUUID().toString();
        requireActivity().runOnUiThread(new Runnable() {
            public void run() {
                updateList(true,
                        path,
                        streaming.getName(),
                        streaming.getReceiveSession().getDestinationAddress().toString(),
                        streaming.getReceiveSession().getDestinationPort());
            }
        });
    }

    @Override
    public void streamingUnavailable(final Streaming streaming) {
        final String path = streaming.getUUID().toString();
        requireActivity().runOnUiThread(new Runnable() {
            public void run() {
                updateList(false,
                        path,
                        streaming.getName(),
                        streaming.getReceiveSession().getDestinationAddress().toString(),
                        streaming.getReceiveSession().getDestinationPort());
            }
        });
    }

    @Override
    public void onRtspUpdate(int message, Exception exception) {
        Toast.makeText(getContext(), "RtspClient error message " + message + (exception != null ? " Ex: " + exception.getMessage() : ""), Toast.LENGTH_SHORT).show();
    }

}

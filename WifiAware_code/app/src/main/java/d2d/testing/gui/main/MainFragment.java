package d2d.testing.gui.main;

import static d2d.testing.R.*;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;

import d2d.testing.gui.MainActivity;
import d2d.testing.gui.StreamActivity;
import d2d.testing.gui.ViewStreamActivity;
import d2d.testing.streaming.Streaming;
import d2d.testing.streaming.StreamingRecord;
import d2d.testing.streaming.StreamingRecordObserver;
import d2d.testing.streaming.rtsp.RtspClient;
import d2d.testing.streaming.sessions.SessionBuilder;

public class MainFragment extends Fragment implements StreamingRecordObserver, RtspClient.Callback {
    private  EditText myName;
    private TextView myStatus;
    private TextView numStreams;
    private ArrayList<StreamDetail> streamList;
    private StreamListAdapter arrayAdapter;
    @Inject
    BasicViewModel mViewModel;
    private Boolean isNetworkAvailable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        streamList = new ArrayList<>();

        INetworkComponent INetworkComponent = DaggerINetworkComponent.builder()
                .fragmentActivity(requireActivity())
                .build();

        INetworkComponent.inject(this);

    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(layout.fragment_main, container, false);

        RecyclerView streamsListView = root.findViewById(id.streamListView);
        streamsListView.setLayoutManager(new LinearLayoutManager(getContext()));
        addDefaultItemList();
        arrayAdapter = new StreamListAdapter(getContext(), streamList, this);
        streamsListView.setAdapter(arrayAdapter);

        numStreams = root.findViewById(id.streams_available);
        numStreams.setText(getString(string.dispositivos_encontrados, 0));

        StreamingRecord.getInstance().addObserver(this);

        Button record = root.findViewById(id.recordButton);
        record.setOnClickListener(v -> {
            if(checkCameraHardware()){
                if(!isNetworkAvailable){
                    Toast.makeText(MainFragment.this.getContext(), string.record_not_available, Toast.LENGTH_SHORT).show();
                }
                else{
                    openStreamActivity();
                }

            }
        });

        @SuppressLint("ResourceType") Animation shake = AnimationUtils.loadAnimation(getContext(), anim.animate_record);
        record.startAnimation(shake);

        TextView myMode = root.findViewById(id.my_mode);
        myName = root.findViewById(id.my_name);
        myStatus = root.findViewById(id.my_status);

        mViewModel.isNetworkAvailable().observe(getViewLifecycleOwner(), aBoolean -> {
            isNetworkAvailable = aBoolean;
            myStatus.setText(getDeviceStatus());
            if(isNetworkAvailable){
                mViewModel.initNetwork();
            }
        });

        String mode = "";

        if(MainActivity.mode != null){
            if(MainActivity.mode.equals(getString(string.mode_witness))){
                myName.setEnabled(false);
                mode = getString(string.witness);
            }
            if(MainActivity.mode.equals(getString(string.mode_humanitarian))){
                mode = getString(string.humanitarian);
            }
        }
        myMode.setText(mode);

        return root;
    }

    private void addDefaultItemList(){
        streamList.clear();
        streamList.add(null);
        streamList.add(null);
        streamList.add(null);
        streamList.add(null);
    }

    private void removeDefaultItemList(){

        streamList.removeIf(Objects::isNull);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        StreamingRecord.getInstance().removeObserver(this);
    }


    public void updateList(boolean on_off, String uuid, String name, String ip, int port, boolean download){
        removeDefaultItemList();
        if(!ip.equals("0.0.0.0")) {
            StreamDetail detail = new StreamDetail(uuid, name, ip, port, download);
            if (on_off) {
                if (!streamList.contains(detail))
                    streamList.add(detail);
            } else {
                streamList.remove(detail);
            }
            numStreams.setText(getString(string.dispositivos_encontrados, streamList.size()));
            if(streamList.size() == 0) addDefaultItemList();
            arrayAdapter.setStreamsData(streamList);
        }
    }

    public void putStreamDownloading(String uuid, boolean isDownload){
        for(StreamDetail value: streamList){
            if (value.getUuid().equals(uuid)) {
                value.setDownload(isDownload);
                arrayAdapter.setStreamsData(streamList);
                return;
            }
        }
    }

    public void openStreamActivity(String uuid) {
        openViewStreamActivity(getActivity(), uuid);
    }

    public String getDeviceStatus() {
        Pair<Boolean, String> status = mViewModel.getDeviceStatus(getContext());
        if(status.first){
            myStatus.setTextColor(getResources().getColor(color.colorPrimaryDark));
            return status.second;
        }

        myStatus.setTextColor(getResources().getColor(color.colorRed));
        return status.second;

    }

    private boolean checkCameraHardware() {
        if (requireActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)){
            // This device has a camera
            return true;
        } else {
            // No camera on this device
            Toast.makeText(requireActivity().getApplicationContext(), "YOUR DEVICE HAS NO CAMERA", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void openStreamActivity() {
        Intent streamActivityIntent = new Intent(getActivity(), StreamActivity.class);
        String author;
        if(myName.getText().toString().equals("")) author = myName.getHint().toString();
        else author = myName.getText().toString();
        streamActivityIntent.putExtra("author", author);
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
                        streaming.getReceiveSession().getDestinationPort(),
                        streaming.isDownloading());
            }
        });
    }

    @Override
    public void streamingDownloadStateChanged(final Streaming streaming, final boolean bIsDownload) {
        final String path = streaming.getUUID().toString();
        requireActivity().runOnUiThread(new Runnable() {
            public void run() {
                putStreamDownloading(path, bIsDownload);
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
                        streaming.getReceiveSession().getDestinationPort(),
                        streaming.isDownloading());
            }
        });
    }

    @Override
    public void onRtspUpdate(int message, Exception exception) {
        Toast.makeText(this.requireActivity().getApplicationContext(), "RtspClient error message " + message + (exception != null ? " Ex: " + exception.getMessage() : ""), Toast.LENGTH_SHORT).show();
    }

}

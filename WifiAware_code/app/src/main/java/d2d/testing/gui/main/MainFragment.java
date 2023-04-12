package d2d.testing.gui.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
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

    private  EditText myName;
    private TextView myStatus;
//    private WifiAwareViewModel mAwareModel;
    private TextView numStreams;
    private ArrayList<StreamDetail> streamList;
    private StreamListAdapter arrayAdapter;

    TextView tvIP, tvPort;
    public static String SERVER_IP = "";
    public static int SERVER_PORT = 8080;
    DefaultViewModel mViewModel;


//    private Boolean isWifiAwareAvailable;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        streamList = new ArrayList<>();



//        mAwareModel = new ViewModelProvider(requireActivity()).get(WifiAwareViewModel.class);
        mViewModel = new ViewModelProvider(requireActivity()).get(DefaultViewModel.class);

        try {
            SERVER_IP = mViewModel.getLocalIpAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }




    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_main, container, false);

        RecyclerView streamsListView = root.findViewById(R.id.streamListView);
        streamsListView.setLayoutManager(new LinearLayoutManager(getContext()));
        addDefaultItemList();
        arrayAdapter = new StreamListAdapter(getContext(), streamList, this);
        streamsListView.setAdapter(arrayAdapter);

        numStreams = root.findViewById(R.id.streams_available);
        numStreams.setText(getString(R.string.dispositivos_encontrados, 0));


        tvIP = root.findViewById(R.id.tvIP);
        tvIP.setText(SERVER_IP);
        tvPort = root.findViewById(R.id.tvPort);
        tvPort.setText(String.valueOf(SERVER_PORT));


        StreamingRecord.getInstance().addObserver(this);

        Button record = root.findViewById(R.id.recordButton);
        record.setOnClickListener(v -> {
            if(checkCameraHardware()){
//                if(!isWifiAwareAvailable)
//                    Toast.makeText(MainFragment.this.getContext(), R.string.record_not_available, Toast.LENGTH_SHORT).show();
//                else
                    handleCamera();
            }
        });

        @SuppressLint("ResourceType") Animation shake = AnimationUtils.loadAnimation(getContext(), R.anim.animate_record);
        record.startAnimation(shake);

        TextView myMode = root.findViewById(R.id.my_mode);
        myName = root.findViewById(R.id.my_name);
        myStatus = root.findViewById(R.id.my_status);

        /*
        mAwareModel.isWifiAwareAvailable().observe(getViewLifecycleOwner(), aBoolean -> {
            isWifiAwareAvailable = aBoolean;
            myStatus.setText(getDeviceStatus());
            if(isWifiAwareAvailable && !mAwareModel.sessionCreated()){
                initWifiAware();
            }
        });
        */
        initDefaultNetwork();

        String mode = "";

        if(MainActivity.mode != null){
            if(MainActivity.mode.equals(getString(R.string.mode_witness))){
                myName.setEnabled(false);
                mode = getString(R.string.witness);
            }
            if(MainActivity.mode.equals(getString(R.string.mode_humanitarian))){
                mode = getString(R.string.humanitarian);
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

        for (Iterator<StreamDetail> iterator = streamList.iterator(); iterator.hasNext(); ) {
            StreamDetail value = iterator.next();
            if (value == null) {
                iterator.remove();
            }
        }

//        streamList.removeIf(Objects::isNull);
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

//    public ArrayList<StreamDetail> getStreamList(){
//        return this.streamList;
//    }

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
            numStreams.setText(getString(R.string.dispositivos_encontrados, streamList.size()));
            //if(streamList.size() != 0) progressBar.setVisibility(View.INVISIBLE);
            //else progressBar.setVisibility(View.VISIBLE);
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
        /*
        for (Iterator<StreamDetail> iterator = streamList.iterator(); iterator.hasNext(); ) {
            StreamDetail value = iterator.next();
            if (value.getUuid().equals(uuid)) {
                value.setDownload(isDownload);
                arrayAdapter.setStreamsData(streamList);
                return;
            }
        }
         */
    }

    public void openStreamActivity(String uuid) {
        openViewStreamActivity(getActivity(), uuid);
    }

    private void initWifiAware(){

//        try {
//            if(mAwareModel.createSession()){
//                if(mAwareModel.publishService("Server")){
//                    //Toast.makeText(this.getContext(), "Se creo una sesion de publisher con WifiAware", Toast.LENGTH_SHORT).show();
//                }else{
//                    //Toast.makeText(this.getContext(), "No se pudo crear una sesion de publisher de WifiAware", Toast.LENGTH_LONG).show();
//                }
//
//                if(mAwareModel.subscribeToService("Server", this)){
//                    //Toast.makeText(this.getContext(), "Se creo una sesion de subscripcion con WifiAware", Toast.LENGTH_SHORT).show();
//                }else{
//                    //Toast.makeText(this.getContext(), "No se pudo crear una sesion de subscripcion de WifiAware", Toast.LENGTH_LONG).show();
//                }
//            }else {
//                //Toast.makeText(this.getContext(), "No se pudo crear la sesion de WifiAware", Toast.LENGTH_LONG).show();
//            }
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }

    private void initDefaultNetwork(){
        if(mViewModel.startServer()){
            Toast.makeText(this.getContext(), "Server Started", Toast.LENGTH_SHORT).show();

        }else {
            Toast.makeText(this.getContext(), "ServerStart Error", Toast.LENGTH_LONG).show();
        }

        if(mViewModel.startClient()){
            Toast.makeText(this.getContext(), "Client Started", Toast.LENGTH_SHORT).show();

        }else {
            Toast.makeText(this.getContext(), "ClientStart Error", Toast.LENGTH_LONG).show();
        }

    }

    private void handleCamera(){
        openStreamActivity();
    }

    @SuppressLint("ResourceType")
    public String getDeviceStatus() {
//        if (/*isWifiAwareAvailable) {
//            myStatus.setTextColor(Color.parseColor(getString(R.color.colorPrimaryDark)));
//            return "Wifi Aware available";
//        }
//        else {
//            myStatus.setTextColor(Color.parseColor(getString(R.color.colorRed)));
//            return "Wifi Aware unavailable";
//        }
        myStatus.setTextColor(Color.parseColor(getString(R.color.colorPrimaryDark)));
        return "Wifi Aware available";
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
        Toast.makeText(this.getActivity().getApplicationContext(), "RtspClient error message " + message + (exception != null ? " Ex: " + exception.getMessage() : ""), Toast.LENGTH_SHORT).show();
    }

}

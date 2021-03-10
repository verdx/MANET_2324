package d2d.testing.gui.main;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.UUID;

import d2d.testing.R;
import d2d.testing.StreamActivity;
import d2d.testing.ViewStreamActivity;
import d2d.testing.gui.FragmentStreams;
import d2d.testing.gui.StreamDetail;
import d2d.testing.streaming.Streaming;
import d2d.testing.streaming.StreamingRecord;
import d2d.testing.streaming.StreamingRecordObserver;
import d2d.testing.streaming.rtsp.RtspClient;
import d2d.testing.streaming.sessions.SessionBuilder;

public class MainFragment extends Fragment implements StreamingRecordObserver, RtspClient.Callback {

    private FragmentStreams streams_fragment;

    private TextView myName;
    private TextView myAdd;
    private TextView myStatus;

    private Button record;
    private WifiAwareViewModel mAwareModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAwareModel = new ViewModelProvider(this).get(WifiAwareViewModel.class);
        initialWork();
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_main, container, false);

        streams_fragment = new FragmentStreams();
        streams_fragment.setMainActivity(this);

        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.st_fragment, streams_fragment).commit();

        record = root.findViewById(R.id.recordButton);
        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkCameraHardware()) handleCamera();
            }
        });

        myAdd = root.findViewById(R.id.my_address);
        myName = root.findViewById(R.id.my_name);
        myStatus = root.findViewById(R.id.my_status);
        updateMyDeviceStatus();

        return root;
    }

    private void initialWork() {
        askPermits();
        checkWifiAwareAvailability();
        initWifiAware();
    }

    private void initWifiAware(){
        try {
            if(mAwareModel.createSession()){
                if(mAwareModel.publishService("Server", this)){
                    Toast.makeText(getContext(), "Se creo una sesion de publisher con WifiAware", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(getContext(), "No se pudo crear una sesion de publisher de WifiAware", Toast.LENGTH_SHORT).show();
                }
                if(mAwareModel.subscribeToService("Server", this)){
                    Toast.makeText(getContext(), "Se creo una sesion de subscripcion con WifiAware", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(getContext(), "No se pudo crear una sesion de subscripcion de WifiAware", Toast.LENGTH_SHORT).show();
                }
            }else {
                Toast.makeText(getContext(), "No se pudo crear la sesion de WifiAware", Toast.LENGTH_SHORT).show();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        StreamingRecord.getInstance().addObserver(this);
    }

    private void checkWifiAwareAvailability(){
        if (!getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)) {
            Snackbar.make(getView().findViewById(android.R.id.content), "No dispones de Wifi Aware, la apliación no funcionará correctamente", Snackbar.LENGTH_LONG).setAction("Action", null).show();
        }
    }

    private void handleCamera(){
        //openCameraActivity();
        openStreamActivity();
    }

    public String getDeviceStatus() {
        if (mAwareModel.isWifiAwareAvailable().getValue()) {
            return "Wifi Aware available";
        }
        else return "Wifi Aware not available";
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

    public ArrayList<StreamDetail> getStreamlist(){
        return streams_fragment.getStreamList();
    }

    private void openStreamActivity() {
        Intent streamActivityIntent = new Intent(getActivity(), StreamActivity.class);
        this.startActivity(streamActivityIntent);
    }

    public void openViewStreamActivity(Context context, String ip) {
        Intent streamActivityIntent = new Intent(context, ViewStreamActivity.class);
        streamActivityIntent.putExtra("IP",ip);
        this.startActivity(streamActivityIntent);
    }

    public void updateMyDeviceStatus () {
        if(myName != null) myName.setText("Model:  " + Build.MODEL);
        if(myAdd != null) myAdd.setText("Network: ..." /*+ mAwareModel.getConnectivityManager().getActiveNetwork().toString()*/);
        if(myStatus != null) myStatus.setText(getDeviceStatus());
    }

    @Override
    public void localStreamingAvailable(UUID id, SessionBuilder sessionBuilder) {}

    @Override
    public void localStreamingUnavailable() {}

    @Override
    public void streamingAvailable(Streaming streaming, boolean bAllowDispatch) {
        final String path = streaming.getUUID().toString();
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                streams_fragment.updateList(true, path, path);
            }
        });
    }

    @Override
    public void streamingUnavailable(Streaming streaming) {
        final String path = streaming.getUUID().toString();
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                streams_fragment.updateList(false, path, path);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAwareModel.closeSessions();
    }

    private void askPermits(){
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO}, 1);
        }
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO}, 2);
        }
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO}, 3);
        }
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO}, 4);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(),"No has dado los permisos necesarios", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRtspUpdate(int message, Exception exception) {
        Toast.makeText(getContext(), "RtspClient error message " + message + (exception != null ? " Ex: " + exception.getMessage() : ""), Toast.LENGTH_SHORT).show();
    }

}

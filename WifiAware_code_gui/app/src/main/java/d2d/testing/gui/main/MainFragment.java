package d2d.testing.gui.main;

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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.UUID;

import d2d.testing.R;
import d2d.testing.StreamActivity;
import d2d.testing.ViewStreamActivity;
import d2d.testing.gui.FragmentStreams;
import d2d.testing.gui.StreamDetail;
import d2d.testing.gui.info.InfoViewModel;
import d2d.testing.streaming.Streaming;
import d2d.testing.streaming.StreamingRecord;
import d2d.testing.streaming.StreamingRecordObserver;
import d2d.testing.streaming.rtsp.RtspClient;
import d2d.testing.streaming.sessions.SessionBuilder;

public class MainFragment extends Fragment implements StreamingRecordObserver {

    private FragmentStreams streams_fragment;

    private TextView myName;
    private TextView myAdd;
    private TextView myStatus;

    private Button record;

    private MainViewModel mainViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mainViewModel =  new ViewModelProvider(this).get(MainViewModel.class);
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

        StreamingRecord.getInstance().addObserver(this);

        return root;
    }

    private void handleCamera(){
        //openCameraActivity();
        openStreamActivity();
    }

    /*
    public String getDeviceStatus() {
        if (mAwareModel.isWifiAwareAvailable().getValue()) {
            return "Wifi Aware available";
        }
        else return "Wifi Aware not available";
    }
     */

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
        if(myStatus != null) myStatus.setText("Meter WAwareModel en MainFragment"/*getDeviceStatus()*/);
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
}

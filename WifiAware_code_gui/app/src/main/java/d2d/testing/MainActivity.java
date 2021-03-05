package d2d.testing;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.io.IOException;
import java.util.UUID;

import d2d.testing.gui.FragmentStreams;
import d2d.testing.net.threads.selectors.RTSPServerSelector;
import d2d.testing.streaming.Streaming;
import d2d.testing.streaming.StreamingRecord;
import d2d.testing.streaming.StreamingRecordObserver;
import d2d.testing.streaming.rtsp.RtspClient;
import d2d.testing.streaming.sessions.SessionBuilder;
import d2d.testing.wifip2p.WifiAwareViewModel;

public class MainActivity extends AppCompatActivity implements StreamingRecordObserver, RtspClient.Callback{

    private WifiAwareViewModel mAwareModel;

    MenuItem wifiItem;

    private FragmentStreams streams_fragment;

    private TextView myName;
    private TextView myAdd;
    private TextView myStatus;

    private Button record;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAwareModel = new ViewModelProvider(this, new ViewModelProvider.AndroidViewModelFactory(getApplication())).get(WifiAwareViewModel.class);
        initialWork();
    }

    private void initialWork() {
        askPermits();
        checkWifiAwareAvailability();

        streams_fragment = new FragmentStreams();
        streams_fragment.setMainActivity(this);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.st_fragment, streams_fragment);
        transaction.commit();

        record = findViewById(R.id.recordButton);
        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkCameraHardware()) handleCamera();
            }
        });

        myAdd = findViewById(R.id.my_address);
        myName = findViewById(R.id.my_name);
        myStatus = findViewById(R.id.my_status);

        initWifiAware();
        updateMyDeviceStatus();
    }

    private void initWifiAware(){ //Se inicia en onRequestPermissionsResult()
        try {
            if(mAwareModel.createSession()){
                if(mAwareModel.publishService("Server", MainActivity.this)){
                    Toast.makeText(MainActivity.this, "Se creo una sesion de publisher con WifiAware", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(MainActivity.this, "No se pudo crear una sesion de publisher de WifiAware", Toast.LENGTH_SHORT).show();
                }
                if(mAwareModel.subscribeToService("Server", this)){
                    Toast.makeText(MainActivity.this, "Se creo una sesion de subscripcion con WifiAware", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(MainActivity.this, "No se pudo crear una sesion de subscripcion de WifiAware", Toast.LENGTH_SHORT).show();
                }
            }else {
                Toast.makeText(MainActivity.this, "No se pudo crear la sesion de WifiAware", Toast.LENGTH_SHORT).show();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        StreamingRecord.getInstance().addObserver(this);
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

    public void updateWifiIcon(boolean wifi) {
        if(wifiItem!= null) {
            if(wifi) {
                wifiItem.setIconTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_green_light, null)));
            } else {
                wifiItem.setIconTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_red_dark, null)));
            }
        }
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
        try {
            RTSPServerSelector.getInstance().stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void askPermits(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO}, 1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO}, 2);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO}, 3);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO}, 4);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(),"No has dado los permisos necesarios", Toast.LENGTH_LONG).show();
        }
    }

    private void checkWifiAwareAvailability(){
        if (!getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)) {
            Snackbar.make(findViewById(android.R.id.content), "No dispones de Wifi Aware, la apliación no funcionará correctamente", Snackbar.LENGTH_LONG).setAction("Action", null).show();
        }
    }

    private boolean checkCameraHardware() {
        if (this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // This device has a camera
            return true;
        } else {
            // No camera on this device
            Toast.makeText(getApplicationContext(), "YOUR DEVICE HAS NO CAMERA", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void openStreamActivity() {
        Intent streamActivityIntent = new Intent(this, StreamActivity.class);
        this.startActivity(streamActivityIntent);
    }

    public void openViewStreamActivity(Context context, String ip) {
        Intent streamActivityIntent = new Intent(context, ViewStreamActivity.class);
        streamActivityIntent.putExtra("IP",ip);
        this.startActivity(streamActivityIntent);
    }

    @Override
    public void localStreamingAvailable(UUID id, SessionBuilder sessionBuilder) {}

    @Override
    public void localStreamingUnavailable() {}

    @Override
    public void streamingAvailable(Streaming streaming, boolean bAllowDispatch) {
        final String path = streaming.getUUID().toString();
        runOnUiThread(new Runnable() {
            public void run() {
                streams_fragment.updateList(true, path, path);
            }
        });
    }

    @Override
    public void streamingUnavailable(Streaming streaming) {
        final String path = streaming.getUUID().toString();
        runOnUiThread(new Runnable() {
            public void run() {
                streams_fragment.updateList(false, path, path);
            }
        });
    }

    @Override
    public void onRtspUpdate(int message, Exception exception) {
        Toast.makeText(getApplicationContext(), "RtspClient error message " + message + (exception != null ? " Ex: " + exception.getMessage() : ""), Toast.LENGTH_SHORT).show();
    }

    public void updateMyDeviceStatus () {
        if(myName != null) myName.setText("Model:  " + Build.MODEL);
        if(myAdd != null) myAdd.setText("Network: ..." /*+ mAwareModel.getConnectivityManager().getActiveNetwork().toString()*/);
        if(myStatus != null) myStatus.setText(getDeviceStatus());
    }
}

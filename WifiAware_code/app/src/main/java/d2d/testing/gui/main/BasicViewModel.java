package d2d.testing.gui.main;

import static android.content.Context.WIFI_SERVICE;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.HandlerThread;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import d2d.testing.streaming.rtsp.RtspClient;

public class BasicViewModel extends AndroidViewModel implements RtspClient.Callback{
    protected final HandlerThread worker;
    protected MutableLiveData<Boolean> mIsNetworkAvailable;

    public BasicViewModel(@NonNull Application app) {
        super(app);

        worker = new HandlerThread("BasicNetwork Worker");
        worker.start();

    }

    public LiveData<Boolean> isNetworkAvailable(){
        return mIsNetworkAvailable;
    }

    @SuppressLint("ResourceType")
    public Pair<Boolean, String> getDeviceStatus() {
        if (mIsNetworkAvailable.getValue()) {
            return new Pair<>(Boolean.TRUE, getNetworkAvailabilityString(true));
        }
        else {
            return new Pair<>(Boolean.FALSE, getNetworkAvailabilityString(false));
        }
    }

    protected String getNetworkAvailabilityString(boolean available){
        return "Network Availability unknown";
    }

    protected void initNetwork(){}

    @Override
    protected void onCleared() {
        worker.quitSafely();
    }

    public String getLocalIpAddress() throws UnknownHostException {
        WifiManager wifiManager = (WifiManager) this.getApplication().getApplicationContext().getSystemService(WIFI_SERVICE);
        assert wifiManager!=null;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();

        return InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()).getHostAddress();
    }

    @Override
    public void onRtspUpdate(int message, Exception exception) {
        Toast.makeText(getApplication().getApplicationContext(), "RtspClient error message " + message + (exception != null ? " Ex: " + exception.getMessage() : ""), Toast.LENGTH_SHORT).show();
    }

}

package d2d.testing.gui.main;

import static android.content.Context.WIFI_SERVICE;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.aware.WifiAwareManager;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import d2d.testing.net.threads.selectors.RTSPServerSelector;

public class DefaultViewModel extends AndroidViewModel{

    private DefaultNetwork mNetwork;
    private static ConnectivityManager mConManager;
    private final HandlerThread worker;

    public DefaultViewModel(@NonNull Application app) {
        super(app);

        worker = new HandlerThread("DefaultNetwork Worker");
        worker.start();

        mConManager = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
        mNetwork = new DefaultNetwork(mConManager);

    }

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


    public boolean startServer() {
        return mNetwork.startServer();
    }

    public boolean startClient(){
        return mNetwork.startClient();
    }

}

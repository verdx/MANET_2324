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

public class DefaultViewModel extends AndroidViewModel{

    private DefaultNetwork mNetwork;
    private static ConnectivityManager mConManager;
    private final HandlerThread worker;
    protected MutableLiveData<Boolean> mIsNetworkAvailable;


    public DefaultViewModel(@NonNull Application app) {
        super(app);
        mIsNetworkAvailable = new MutableLiveData<>(Boolean.TRUE);

        worker = new HandlerThread("DefaultNetwork Worker");
        worker.start();

        mConManager = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
        mNetwork = new DefaultNetwork(app, mConManager);

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
        if(available){
            return MainFragment.SERVER_IP + ":" + MainFragment.SERVER_PORT;
        }
        return "Default Network unavailable";
    }

    protected void initNetwork(){

        if(startServer()){
            Toast.makeText(getApplication().getApplicationContext(), "Server Started", Toast.LENGTH_SHORT).show();

        }else {
            Toast.makeText(getApplication().getApplicationContext(), "ServerStart Error", Toast.LENGTH_LONG).show();
        }

        if(startClient()){
            Toast.makeText(getApplication().getApplicationContext(), "Client Started", Toast.LENGTH_SHORT).show();

        }else {
            Toast.makeText(getApplication().getApplicationContext(), "ClientStart Error", Toast.LENGTH_LONG).show();
        }

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

package d2d.testing.gui.main;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import d2d.testing.R;
import d2d.testing.streaming.network.DefaultNetwork;

public class DefaultViewModel extends BasicViewModel {

    public static String SERVER_IP = "";
    public static int SERVER_PORT = 8080;
    private DefaultNetwork mNetwork;

    public DefaultViewModel(@NonNull Application app) {
        super(app);
        mNetwork = new DefaultNetwork(app);
        SERVER_IP = super.getLocalIpAddress();
        mIsNetworkAvailable = new MutableLiveData<>(Boolean.TRUE);
        ConnectivityManager connectivityManager = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                // Internet está disponible
                mIsNetworkAvailable.postValue(true);
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                // No hay conexión a internet
                mIsNetworkAvailable.postValue(false);
            }
        };

        if (connectivityManager != null) {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        }

    }

    @Override
    public String getNetworkAvailabilityString(Context c, boolean available){
        if(available){
            return SERVER_IP + ":" + SERVER_PORT;
        }
        return c.getString(R.string.defaultnet_unavailable_str);
    }

    @Override
    protected void initNetwork(){

        if(mNetwork.startServer()){
            Toast.makeText(getApplication().getApplicationContext(), "Server Started", Toast.LENGTH_SHORT).show();
        }else {
            //Toast.makeText(getApplication().getApplicationContext(), "ServerStart Error", Toast.LENGTH_LONG).show();
        }

        if(mNetwork.startClient()){
            Toast.makeText(getApplication().getApplicationContext(), "Client Started", Toast.LENGTH_SHORT).show();
        }else {
            Toast.makeText(getApplication().getApplicationContext(), "ClientStart Error", Toast.LENGTH_LONG).show();
        }

    }
}

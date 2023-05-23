package d2d.testing.gui.main;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import d2d.testing.R;

public class DefaultViewModel extends BasicViewModel{

    public static String SERVER_IP = "";
    public static int SERVER_PORT = 8080;
    private DefaultNetwork mNetwork;
    private ConnectivityManager mConManager;

    public DefaultViewModel(@NonNull Application app) {
        super(app);
        mConManager = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
        mNetwork = new DefaultNetwork(app, mConManager);
        SERVER_IP = super.getLocalIpAddress();
        mIsNetworkAvailable = new MutableLiveData<>(Boolean.TRUE);
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

    public boolean startServer() {
        return mNetwork.startServer();
    }

    public boolean startClient(){
        return mNetwork.startClient();
    }

}

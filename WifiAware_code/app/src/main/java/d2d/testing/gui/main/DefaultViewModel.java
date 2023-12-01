package d2d.testing.gui.main;

import android.app.Application;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import d2d.testing.R;
import d2d.testing.streaming.network.DefaultNetwork;

public class DefaultViewModel extends BasicViewModel{

    public static String SERVER_IP = "";
    public static int SERVER_PORT = 8080;
    private DefaultNetwork mNetwork;

    public DefaultViewModel(@NonNull Application app) {
        super(app);
        mNetwork = new DefaultNetwork(app);
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

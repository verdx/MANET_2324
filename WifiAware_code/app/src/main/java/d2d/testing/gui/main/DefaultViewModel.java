package d2d.testing.gui.main;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

public class DefaultViewModel extends BasicViewModel{

    private DefaultNetwork mNetwork;
    private static ConnectivityManager mConManager;

    public DefaultViewModel(@NonNull Application app) {
        super(app);

        mConManager = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
        mNetwork = new DefaultNetwork(app, mConManager);

        mIsNetworkAvailable = new MutableLiveData<>(Boolean.TRUE);
    }

    @Override
    public String getNetworkAvailabilityString(boolean available){
        if(available){
            return MainFragment.SERVER_IP + ":" + MainFragment.SERVER_PORT;
        }
        return "Default Network unavailable";
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

package d2d.testing.gui.main;

import android.app.Application;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.TransportInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Pair;

import androidx.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import d2d.testing.R;
import d2d.testing.streaming.rtsp.RtspClient;

public class DefaultNetwork implements INetworkManager{

    private Handler workerHandle;
    private final HandlerThread worker;

    private final Map<String, RtspClient> mClients; //IP, cliente
    private RTSPServerModel mServerModel;
    private static ConnectivityManager mConManager;
    private DestinationIPReader mDestinationReader;

    private SharedPreferences mSharedPrefs;

    private ScheduledExecutorService mScheduler;


    public DefaultNetwork(Application app, ConnectivityManager conManager){

        mServerModel = null;
        this.mConManager = conManager;

        mClients = new HashMap<>();

        worker = new HandlerThread("DefaultNetwork Worker");
        worker.start();
        workerHandle = new Handler(worker.getLooper());

        InputStream inputStream = app.getResources().openRawResource(R.raw.destinations);
        mDestinationReader = new DestinationIPReader(inputStream);

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(app.getApplicationContext());
    }

    private synchronized void checkDestinationsConnectivity() {

        for(DestinationInfo info: mDestinationReader.mDestinationList){
            if(!info.isConnected){
                RtspClient client = mClients.get(info.ip);
                if(client!=null){
                    client.start(); //Retry connection
                    info.isConnected = client.isConnected();
                }
                else{
                    connectToDestination(info);
                }
            }
        }
    }

    private void connectToDestination(DestinationInfo dest) {
        RtspClient client = new RtspClient(DefaultNetwork.this);
        client.setServerAddress(dest.ip, dest.port);
        client.connectionCreated();
        client.start();

        mClients.put(dest.ip, client);
    }

    private boolean startLocalServer(){
        synchronized (DefaultNetwork.this){
            try {
                mServerModel = new RTSPServerModel(mConManager);
                mServerModel.startServer();

                //Pone al server RTSP a escuchar en localhost:1234 para peticiones de descarga de libVLC
                if(!mServerModel.addNewConnection("127.0.0.1", 1234)){
                    throw new IOException();
                }
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    public boolean startServer() {
        if(startLocalServer()){
            return initServer();
        }
        return false;
    }



    public boolean startClient() {
        int delayms = 3000;
        workerHandle.postDelayed(new Runnable() {
            public void run() {
                checkDestinationsConnectivity();
                workerHandle.postDelayed(this, delayms);
            }
        }, delayms);

        return true;
    }

    private boolean initServer(){
        if(!mServerModel.isServerEnabled()) return false;
        return mServerModel.addNewConnection();
    }

    @Override
    public InetAddress getInetAddress(NetworkCapabilities networkCapabilities) {
        TransportInfo ti = networkCapabilities.getTransportInfo();
        InetAddress inetAddress = (InetAddress) ti;
        return inetAddress;
    }

    @Override
    public int getPort(NetworkCapabilities networkCapabilities) {
        return 8080;
    }

    class DestinationInfo{
        String ip;
        int port;
        boolean isConnected;

        public DestinationInfo(String ip, int port, boolean isConnected){
            this.ip = ip;
            this.port = port;
            this.isConnected = isConnected;
        }
    }

    public class DestinationIPReader{

        List<DestinationInfo> mDestinationList;

        public DestinationIPReader(InputStream inputStream){
            mDestinationList = new ArrayList<>();
            getDestinationIps(inputStream);
        }

        public DestinationIPReader(){
            mDestinationList = new ArrayList<>();
            getDestinationIps();
        }

        private void getDestinationIps(InputStream inputStream){
            try {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    String[] res = line.split(":");

                    mDestinationList.add(new DestinationInfo(res[0], Integer.valueOf(res[1]), false));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void getDestinationIps(){
            Set<String> ipAddresses = mSharedPrefs.getStringSet("PREF_IP_ADDRESSES", new HashSet<String>());
            mDestinationList.clear();

            for(String ipaddr: ipAddresses){
                mDestinationList.add(new DestinationInfo(ipaddr, 8080,false));
            }
        }
    }

}

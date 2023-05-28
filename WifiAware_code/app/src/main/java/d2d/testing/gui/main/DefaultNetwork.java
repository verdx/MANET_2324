package d2d.testing.gui.main;

import android.app.Application;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.TransportInfo;
import android.net.wifi.aware.DiscoverySession;
import android.net.wifi.aware.DiscoverySessionCallback;
import android.net.wifi.aware.PeerHandle;
import android.net.wifi.aware.PublishConfig;
import android.net.wifi.aware.PublishDiscoverySession;
import android.net.wifi.aware.SubscribeConfig;
import android.net.wifi.aware.SubscribeDiscoverySession;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import d2d.testing.R;
import d2d.testing.net.threads.selectors.ChangeRequest;
import d2d.testing.net.threads.selectors.RTSPServerSelector;
import d2d.testing.streaming.rtsp.RtspClient;
import d2d.testing.streaming.rtsp.RtspClientWFA;

public class DefaultNetwork implements INetworkManager{

    private Handler workerHandle;
    private final HandlerThread worker;
    private final Map<String, RtspClient> mClients; //IP, cliente
    private RTSPServerController mServerController;
    private static ConnectivityManager mConManager;
    private DestinationIPReader mDestinationReader;


    public DefaultNetwork(Application app, ConnectivityManager conManager){

        mServerController = null;
        this.mConManager = conManager;

        this.mClients = new HashMap<>();

        worker = new HandlerThread("DefaultNetwork Worker");
        worker.start();
        workerHandle = new Handler(worker.getLooper());

        InputStream inputStream = app.getResources().openRawResource(R.raw.destinations);
        mDestinationReader = new DestinationIPReader(inputStream);

    }


    private boolean startLocalServer(){
        synchronized (DefaultNetwork.this){
            try {
                mServerController = new RTSPServerController(mConManager);
                mServerController.startServer();

                //Pone al server RTSP a escuchar en localhost:1234 para peticiones de descarga de libVLC
                if(!mServerController.addNewConnection("127.0.0.1", 1234)){
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



//    RtspClient client;
    public boolean startClient() {

        int tam = mDestinationReader.mDestinationList.size();
        if(tam>0){
            ExecutorService executor = Executors.newFixedThreadPool(tam);
            List<Future<Boolean>> results = new ArrayList<>();

            for(Pair<String, Integer> destination: mDestinationReader.mDestinationList){
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        RtspClient client = new RtspClient(DefaultNetwork.this);
                        client.setServerAddress(destination.first, destination.second);
                        client.connectionCreated(mConManager);
                        client.start();

                        mClients.put(destination.first, client);
                    }
                });
            }
            executor.shutdown();
        }else{
            return false;
        }

        return true;
    }

    private boolean initServer(){
        if(!mServerController.isServerEnabled()) return false;
        return mServerController.addNewConnection();
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

    public class DestinationIPReader{
        List<Pair<String, Integer>> mDestinationList;   //Par ip, puerto

        public DestinationIPReader(InputStream inputStream){
            mDestinationList = new ArrayList<>();
            getDestinationIps(inputStream);
        }

        private void getDestinationIps(InputStream inputStream){
            try {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String line;
                int port;
                while ((line = bufferedReader.readLine()) != null) {
                    String[] res = line.split(":");

                    mDestinationList.add(new Pair<>(res[0], Integer.valueOf(res[1])));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

package d2d.testing.gui.main;

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

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

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

    public DefaultNetwork(ConnectivityManager conManager){

        mServerController = null;
        this.mConManager = conManager;

        this.mClients = new HashMap<>();

        worker = new HandlerThread("DefaultNetwork Worker");
        worker.start();
        workerHandle = new Handler(worker.getLooper());
    }


    private boolean startLocalServer(){
        synchronized (DefaultNetwork.this){
            try {
                mServerController = new RTSPServerController(/*this, */mConManager);
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
        // X is 103
        // M is 23
        String peerIP = "192.168.1.23";
        int peerPORT = 8080;

        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                RtspClient client = new RtspClient(DefaultNetwork.this);
                client.setServerAddress(peerIP, peerPORT);
                client.connectionCreated(mConManager);
                client.start();

                mClients.put(peerIP, client);

            }
        };

        Thread myThread = new Thread(myRunnable);
        myThread.start();

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

}

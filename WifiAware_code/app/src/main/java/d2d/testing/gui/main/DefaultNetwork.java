package d2d.testing.gui.main;

import android.app.Application;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.TransportInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import d2d.testing.R;
import d2d.testing.streaming.rtsp.RtspClient;

public class DefaultNetwork implements INetworkManager{

    private Handler workerHandle;
    private final HandlerThread worker;
//    private final Map<String, RtspClient> mClients; //IP, cliente
    private RTSPServerModel mServerModel;
    private static ConnectivityManager mConManager;
    private DestinationIPReader mDestinationReader;


    public DefaultNetwork(Application app, ConnectivityManager conManager){

        mServerModel = null;
        this.mConManager = conManager;

//        this.mClients = new HashMap<>();

        worker = new HandlerThread("DefaultNetwork Worker");
        worker.start();
        workerHandle = new Handler(worker.getLooper());

        InputStream inputStream = app.getResources().openRawResource(R.raw.destinations);
        mDestinationReader = new DestinationIPReader(inputStream);

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

        int tam = mDestinationReader.mDestinationList.size();
        if(tam>0){
            ExecutorService executor = Executors.newFixedThreadPool(tam);

            for(Pair<String, Integer> destination: mDestinationReader.mDestinationList){
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        RtspClient client = new RtspClient(DefaultNetwork.this);
                        client.setServerAddress(destination.first, destination.second);
                        client.connectionCreated(mConManager);
                        client.start();

//                        mClients.put(destination.first, client);
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

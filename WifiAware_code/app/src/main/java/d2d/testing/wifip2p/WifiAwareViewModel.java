package d2d.testing.wifip2p;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.aware.AttachCallback;
import android.net.wifi.aware.DiscoverySessionCallback;
import android.net.wifi.aware.PeerHandle;
import android.net.wifi.aware.PublishConfig;
import android.net.wifi.aware.PublishDiscoverySession;
import android.net.wifi.aware.SubscribeConfig;
import android.net.wifi.aware.SubscribeDiscoverySession;
import android.net.wifi.aware.WifiAwareManager;
import android.net.wifi.aware.WifiAwareSession;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import d2d.testing.MainActivity;
import d2d.testing.net.threads.selectors.RTSPServerSelector;
import d2d.testing.streaming.rtsp.RtspClient;

public class WifiAwareViewModel extends AndroidViewModel {

    private static ConnectivityManager connectivityManager;
    private WifiAwareManager manager;
    private WifiAwareSession session;
    private PublishDiscoverySession publishSession;
    private SubscribeDiscoverySession subscribeSession;
    private MutableLiveData<Boolean> available;
    private HandlerThread worker;
    private Handler workerHandle;

    private Map<PeerHandle, RtspClient> mClients;


    public WifiAwareViewModel(@NonNull Application app) {
        super(app);
        available = new MutableLiveData<Boolean>(Boolean.FALSE);
        mClients = new HashMap<>();
        session = null;
        publishSession = null;
        subscribeSession = null;

        if(!app.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)){
            manager = null;
            worker = null;
            return;
        }
        worker = new HandlerThread("Worker");
        worker.start();
        workerHandle = new Handler(worker.getLooper());

        connectivityManager = (ConnectivityManager) app.getSystemService(app.CONNECTIVITY_SERVICE);
        manager = (WifiAwareManager)app.getSystemService(app.WIFI_AWARE_SERVICE);
        IntentFilter filter = new IntentFilter(WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED);
        BroadcastReceiver myReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                checkWifiAwareAvailability();
            }
        };
        app.registerReceiver(myReceiver, filter);
        checkWifiAwareAvailability();
    }

    @Override
    protected void onCleared() {
        closeSessions();
    }

    public boolean isWifiAwareSupported(){
        return manager != null;
    }

    private void checkWifiAwareAvailability(){
        closeSessions();
        if(manager.isAvailable()){
            available.postValue(Boolean.TRUE);
        }
        else{
            available.postValue(Boolean.FALSE);
        }
    }

    public LiveData<Boolean> isWifiAwareAvailable(){
        return available;
    }

    public boolean publishSessionCreated(){
        return publishSession != null;
    }

    public boolean subscribeSessionCreated(){
        return subscribeSession != null;
    }

    public static ConnectivityManager getConnectivityManager() {
        return connectivityManager;
    }

    public boolean createSession() throws InterruptedException {
        if(manager == null) return false;
        if(session != null) return true;
        synchronized (this){
            manager.attach(new AttachCallback(){
                @Override
                public void onAttached(WifiAwareSession session) {
                    synchronized (WifiAwareViewModel.this){
                        WifiAwareViewModel.this.session = session;
                        WifiAwareViewModel.this.notify();
                    }
                }

                @Override
                public void onAttachFailed() {
                    synchronized (WifiAwareViewModel.this){
                        session = null;
                        WifiAwareViewModel.this.notify();
                    }
                }
            }, workerHandle);
            this.wait();
            return session != null;
        }
    }

    public boolean publishService(String serviceName, final MainActivity activity) throws InterruptedException {
        if(session == null) return false;
        synchronized (WifiAwareViewModel.this){
            PublishConfig config = new PublishConfig.Builder().setServiceName(serviceName).build();

            session.publish(config, new DiscoverySessionCallback(){
                @Override
                public void onPublishStarted(@NonNull PublishDiscoverySession session) {
                    synchronized (WifiAwareViewModel.this){
                        publishSession = session;
                        try {
                            RTSPServerSelector.initiateInstance(activity, connectivityManager).start();
                            RTSPServerSelector.getInstance().addNewConnection("127.0.0.1", 1234);
                        } catch (IOException e) {
                            session.close();
                            publishSession = null;
                        }
                        WifiAwareViewModel.this.notify();
                    }
                }

                @Override
                public void onSessionConfigFailed() {
                    synchronized (WifiAwareViewModel.this){
                        publishSession = null;
                        WifiAwareViewModel.this.notify();
                    }
                }

                @Override
                public void onMessageReceived(PeerHandle peerHandle, byte[] message) {
                    try {
                        RTSPServerSelector.getInstance().addNewConnection(publishSession, peerHandle);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }, workerHandle);

            this.wait();
            return publishSession != null;
        }
    }

    public boolean subscribeToService(String serviceName, final MainActivity activity) throws InterruptedException {
        if(session == null) return false;
        synchronized (WifiAwareViewModel.this){
            SubscribeConfig config = new SubscribeConfig.Builder().setServiceName(serviceName).build();
            session.subscribe(config, new DiscoverySessionCallback(){

                private PeerHandle lastPeerHandle;
                @Override
                public void onSubscribeStarted(@NonNull SubscribeDiscoverySession session) {
                    synchronized (WifiAwareViewModel.this){
                        subscribeSession = session;
                        WifiAwareViewModel.this.notify();
                    }
                }

                @Override
                public void onSessionConfigFailed() {
                    synchronized (WifiAwareViewModel.this){
                        subscribeSession = null;
                        WifiAwareViewModel.this.notify();
                    }
                }

                @Override
                public void onServiceDiscovered(PeerHandle peerHandle, byte[] serviceSpecificInfo, List<byte[]> matchFilter) {
                    lastPeerHandle = peerHandle;
                    subscribeSession.sendMessage(peerHandle, 0, (new String("connect")).getBytes());
                }

                @Override
                public void onMessageSendSucceeded(int messageId) {
                    RtspClient rtspClient = new RtspClient(WifiAwareViewModel.this);
                    rtspClient.setCallback(activity);
                    synchronized (mClients){
                        mClients.put(lastPeerHandle, rtspClient);
                    }
                    rtspClient.connectionCreated(connectivityManager, subscribeSession, lastPeerHandle);
                }


            }, workerHandle);

            this.wait();
            return subscribeSession != null;
        }
    }

    public void removeClient(PeerHandle handle){
        synchronized (mClients){
            mClients.remove(handle);
        }
    }

    public void closeSessions(){
        synchronized (mClients){
            for(RtspClient client : mClients.values()){
                client.release();
            }
            mClients.clear();
        }
        try {
            if(RTSPServerSelector.itsInitialized()) RTSPServerSelector.getInstance().stop();
        } catch (IOException e) {}
        if(publishSession != null){
            publishSession.close();
            publishSession = null;
        }
        if(subscribeSession != null){
            subscribeSession.close();
            subscribeSession = null;
        }
        if(session != null){
            session.close();
            session = null;
        }
    }

}

package d2d.testing.gui.main;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import d2d.testing.net.threads.selectors.RTSPServerSelector;
import d2d.testing.streaming.rtsp.RtspClient;

public class WifiAwareViewModel extends AndroidViewModel {

    private static final int DELAY_BETWEEN_CONNECTIONS = 500;

    private static ConnectivityManager mConManager;
    private final WifiAwareManager mWifiAwareManager;
    private WifiAwareSession mWifiAwareSession;
    private PublishDiscoverySession mPublishSession;
    private SubscribeDiscoverySession mSubscribeSession;
    private final MutableLiveData<Boolean> mIsWifiAwareAvailable;
    private final HandlerThread worker;
    private Handler workerHandle;

    private final Map<PeerHandle, RtspClient> mClients;
    private RTSPServerSelector mServer;

    public WifiAwareViewModel(@NonNull Application app) {
        super(app);
        mIsWifiAwareAvailable = new MutableLiveData<>(Boolean.FALSE);
        mClients = new HashMap<>();
        mWifiAwareSession = null;
        mPublishSession = null;
        mSubscribeSession = null;
        mServer = null;

        if(!app.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)){
            mWifiAwareManager = null;
            worker = null;
            return;
        }
        //Thread para ejecutar los callbacks
        worker = new HandlerThread("WifiAware Worker");
        worker.start();
        //Manejador de thread: Interactuar con thread
        // getLooper(): Looper es un thread especial que tiene un buble interno que escucha eventos de otros
        // threads. Da acceso al Handler al looper para manejarlo
        workerHandle = new Handler(worker.getLooper());
        //workerhandle tiene un bucle y esta esperando a ejecutar

        mConManager = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);

        /*  Source: https://developer.android.com/guide/topics/connectivity/wifi-aware#initial_setup
        *   Since the availability of WiFi Aware can change at any time, we need a BroadcastReceiver
        *   to receive ACTION_WIFI_AWARE_STATE_CHANGED, which is sent whenever availability changes.
        *
        *   When your app receives the broadcast intent, it should discard all existing sessions
        *   (assume that Wi-Fi Aware service was disrupted), then check the current state of
        *   availability and adjust its behavior accordingly.
         */
        mWifiAwareManager = (WifiAwareManager)app.getSystemService(Context.WIFI_AWARE_SERVICE);
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
        worker.quitSafely();
    }

    public boolean isWifiAwareSupported(){
        return mWifiAwareManager != null;
    }

    private void checkWifiAwareAvailability(){
        closeSessions();
        if(mWifiAwareManager.isAvailable()){
            mIsWifiAwareAvailable.postValue(Boolean.TRUE);
        }
        else{
            mIsWifiAwareAvailable.postValue(Boolean.FALSE);
        }
    }

    public boolean sessionCreated(){
        return mWifiAwareSession != null;
    }

    public LiveData<Boolean> isWifiAwareAvailable(){
        return mIsWifiAwareAvailable;
    }

    public boolean publishSessionCreated(){
        return mPublishSession != null;
    }

    public boolean subscribeSessionCreated(){
        return mSubscribeSession != null;
    }

    public static ConnectivityManager getConnectivityManager() {
        return mConManager;
    }


    public boolean createSession() throws InterruptedException {
        if(mWifiAwareManager == null) return false;
        if(mWifiAwareSession != null) return true;
        synchronized (this){
            //En función de cómo termina el thread de worker, hace callback de onAttached o onAttachFailed
            mWifiAwareManager.attach(new AttachCallback(){
                @Override
                public void onAttached(WifiAwareSession session) {
                    synchronized (WifiAwareViewModel.this){
                        WifiAwareViewModel.this.mWifiAwareSession = session;
                        WifiAwareViewModel.this.notify();
                    }
                }

                @Override
                public void onAttachFailed() {
                    synchronized (WifiAwareViewModel.this){
                        mWifiAwareSession = null;
                        WifiAwareViewModel.this.notify();
                    }
                }
            }, workerHandle);
            //Espera notify del workerthread --> workerHandle
            this.wait();
            return mWifiAwareSession != null;
        }
    }

    //https://developer.android.com/guide/topics/connectivity/wifi-aware#publish_a_service
    public boolean publishService(String serviceName) throws InterruptedException {
        if(mWifiAwareSession == null) return false;
        //Síncrona para creación de servicio
        synchronized (WifiAwareViewModel.this){
            PublishConfig config = new PublishConfig.Builder().setServiceName(serviceName).build();
            //Thread que llama publishservice espera a thread de WFA le avise del resultado de la
            // llamada a publishservice
            mWifiAwareSession.publish(config, new DiscoverySessionCallback(){

                //TODO: Pregunta!
                private int mLastMessageID = 0;
                private boolean mCreatingConnection = false;
                private final Queue<PeerHandle> mPendingConnections = new LinkedList<>();
                //------------------

                @Override
                public void onPublishStarted(@NonNull PublishDiscoverySession session) {
                    synchronized (WifiAwareViewModel.this){
                        mPublishSession = session;
                        try {
                            mServer = new RTSPServerSelector(mConManager);
                            mServer.start();
                            //Server socket para escuchar peticiones
                            if(!mServer.addNewConnection("127.0.0.1", 1234)){
                                throw new IOException();
                            }
                        } catch (IOException e) {
                            session.close();
                            mPublishSession = null;
                        }
                        //
                        WifiAwareViewModel.this.notify();
                    }
                }

                @Override
                public void onSessionConfigFailed() {
                    synchronized (WifiAwareViewModel.this){
                        mPublishSession = null;
                        WifiAwareViewModel.this.notify();
                    }
                }

                @Override
                public void onMessageReceived(PeerHandle peerHandle, byte[] message) {
                    if(mCreatingConnection){
                        mPendingConnections.add(peerHandle);
                    }
                    else{
                        startConnection(peerHandle);
                    }
                }

                @Override
                public void onMessageSendSucceeded(int messageId) {
                    processNextConnection();
                }

                @Override
                public void onMessageSendFailed(int messageId) {
                    processNextConnection();
                }

                private void startConnection(PeerHandle peerHandle){
                    //Para el server, puede establecer conexión con el primer PeerHandle que recibe en
                    // onMessageReceived, mientras que el subscriber usa el segundo, el PeerHandle que
                    // recibe en el mensaje devuelto por server
                    if(mServer.addNewConnection(mPublishSession, peerHandle)){
                        int messageId = mLastMessageID++;
                        mPublishSession.sendMessage(peerHandle, messageId, ("connect").getBytes());
                        mCreatingConnection = true;
                    }
                }

                private void processNextConnection(){
                    try {
                        Thread.sleep(DELAY_BETWEEN_CONNECTIONS);
                    } catch (InterruptedException ignored) {}
                    if(mPendingConnections.isEmpty()){
                        mCreatingConnection = false;
                    }
                    else{
                        PeerHandle nextPeerHandle = mPendingConnections.remove();
                        startConnection(nextPeerHandle);
                    }
                }


            }, workerHandle);

            // Libera mutex y espera a que se cumpla una condición interna del monitor, lo cual se
            // consigue mediante la llamada WifiAwareViewModel.this.notify();

            //Java mete un mutex interno a cada Clase, manejado por un "MONITOR" (objeto con mutex)
            //Cada objeto tiene un MONITOR, que se encarga de bloquearlo/desbloquearlo

            /*
                pthread_cond_t  myConVar = PTHREAD_COND_INITIALIZER;
                pthread_cond_wait(&myConVar , &mymutex);
                pthread_cond_signal(&myConVar);
             */
            this.wait();
            return mPublishSession != null;
        }
    }

    //https://developer.android.com/guide/topics/connectivity/wifi-aware#subscribe_to_a_service
    public boolean subscribeToService(String serviceName, final MainFragment activity) throws InterruptedException {
        if(mWifiAwareSession == null) return false;
        //TODO: PREGUNTA SYNCHRONIZED?
        synchronized (WifiAwareViewModel.this){
            SubscribeConfig config = new SubscribeConfig.Builder().setServiceName(serviceName).build();
            mWifiAwareSession.subscribe(config, new DiscoverySessionCallback(){

                private int mLastMessageID = 0;
                private boolean mCreatingConnection = false;
                private final Queue<PeerHandle> mPendingConnections = new LinkedList<>();

                @Override
                public void onSubscribeStarted(@NonNull SubscribeDiscoverySession session) {
                    synchronized (WifiAwareViewModel.this){
                        mSubscribeSession = session;
                        WifiAwareViewModel.this.notify();
                    }
                }

                @Override
                public void onSessionConfigFailed() {
                    synchronized (WifiAwareViewModel.this){
                        mSubscribeSession = null;
                        WifiAwareViewModel.this.notify();
                    }
                }

                @Override
                public void onServiceDiscovered(PeerHandle peerHandle, byte[] serviceSpecificInfo, List<byte[]> matchFilter) {
                    if(mCreatingConnection){
                        mPendingConnections.add(peerHandle);
                    }
                    else{
                        startConnection(peerHandle);
                    }
                }

                @Override
                public void onMessageSendFailed(int messageId) {
                    processNextConnection();
                }

                @Override
                public void onMessageReceived(PeerHandle peerHandle, byte[] message) {
                    RtspClient rtspClient = new RtspClient();
                    rtspClient.setCallback(activity); //TODO: Cambiar callback a un LiveData Object, puede haber excepciones
                    mClients.put(peerHandle, rtspClient);
                    rtspClient.connectionCreated(mConManager, mSubscribeSession, peerHandle);

                    processNextConnection();
                }

                private void startConnection(PeerHandle peerHandle){
                    mCreatingConnection = true;
                    int nextMessageId = mLastMessageID++;
                    mSubscribeSession.sendMessage(peerHandle, nextMessageId, ("connect").getBytes());
                }

                private void processNextConnection(){
                    try {
                        Thread.sleep(DELAY_BETWEEN_CONNECTIONS);
                    } catch (InterruptedException ignored) {}
                    if(mPendingConnections.isEmpty()){
                        mCreatingConnection = false;
                    }
                    else{
                        PeerHandle nextMessagePeerHandle = mPendingConnections.remove();
                        startConnection(nextMessagePeerHandle);
                    }
                }

            }, workerHandle);

            this.wait();
            return mSubscribeSession != null;
        }
    }

    //Puede llamarlo el main thread al cambiar la disponibilidad de wifiaware u otro thread
    public synchronized void closeSessions(){
        if(mPublishSession != null){
            mPublishSession.close();
            mPublishSession = null;
        }
        if(mSubscribeSession != null){
            mSubscribeSession.close();
            mSubscribeSession = null;
        }
        if(mWifiAwareSession != null){
            mWifiAwareSession.close();
            mWifiAwareSession = null;
        }
        for(RtspClient client : mClients.values()){
            client.release();
        }
        mClients.clear();
        if(mServer != null){
            mServer.stop();
            mServer = null;
        }
    }
}

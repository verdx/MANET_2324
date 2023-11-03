package d2d.testing.gui.main;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.wifi.aware.AttachCallback;
import android.net.wifi.aware.DiscoverySession;
import android.net.wifi.aware.DiscoverySessionCallback;
import android.net.wifi.aware.PeerHandle;
import android.net.wifi.aware.PublishConfig;
import android.net.wifi.aware.PublishDiscoverySession;
import android.net.wifi.aware.SubscribeConfig;
import android.net.wifi.aware.SubscribeDiscoverySession;
import android.net.wifi.aware.WifiAwareManager;
import android.net.wifi.aware.WifiAwareNetworkInfo;
import android.net.wifi.aware.WifiAwareNetworkSpecifier;
import android.net.wifi.aware.WifiAwareSession;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import d2d.testing.streaming.network.INetworkManager;
import d2d.testing.streaming.threads.selectors.ChangeRequest;

public class WifiAwareNetwork extends INetworkManager {
    private static final int DELAY_BETWEEN_CONNECTIONS = 500;
    private WifiAwareManager mWifiAwareManager;
    private PublishDiscoverySession mPublishSession;
    private SubscribeDiscoverySession mSubscribeSession;
    private WifiAwareSession mWifiAwareSession;
    private Handler workerHandle;
    private final HandlerThread worker;
    private static ConnectivityManager mConManager;
    private RTSPServerWFAModel mServerController;

    public WifiAwareNetwork(ConnectivityManager conManager, WifiAwareManager wifiAwareManager){

        mWifiAwareSession = null;
        mPublishSession = null;
        mSubscribeSession = null;
        mServerController = null;

        this.mWifiAwareManager = wifiAwareManager;
        this.mConManager = conManager;

        worker = new HandlerThread("WifiAware Worker");
        worker.start();
        workerHandle = new Handler(worker.getLooper());

    }

    public boolean createSession() throws InterruptedException {
        if(mWifiAwareManager == null) return false;
        if(mWifiAwareSession != null) return true;
        synchronized (this){
            mWifiAwareManager.attach(new AttachCallback(){
                //When WFA is successfully attached
                @Override
                public void onAttached(WifiAwareSession session) {
                    synchronized (WifiAwareNetwork.this){
                        WifiAwareNetwork.this.mWifiAwareSession = session;
                        WifiAwareNetwork.this.notify();
                    }
                }

                @Override
                public void onAttachFailed() {
                    synchronized (WifiAwareNetwork.this){
                        mWifiAwareSession = null;
                        WifiAwareNetwork.this.notify();
                    }
                }
            }, workerHandle);
            //Espera notify del workerthread --> workerHandle
            this.wait();
            return mWifiAwareSession != null;
        }
    }

    /*
        Make a service discoverable
     */
    public boolean publishService(String serviceName) throws InterruptedException {
        if(mWifiAwareSession == null) return false;
        synchronized (WifiAwareNetwork.this){
            PublishConfig config = new PublishConfig.Builder()
                    .setServiceName(serviceName)
                    .build();
            DiscoverySessionCallback discoverySessionCallback = new DiscoverySessionCallback(){
                private int mLastMessageID = 0;
                private boolean mCreatingConnection = false;
                private final Queue<PeerHandle> mPendingConnections = new LinkedList<>();

                @Override
                public void onPublishStarted(@NonNull PublishDiscoverySession session) {
                    synchronized (WifiAwareNetwork.this){
                        mPublishSession = session;
                        try {
                            mServerController = new RTSPServerWFAModel(mConManager);
                            mServerController.startServer();

                            //Pone al server RTSP a escuchar en localhost:1234 para peticiones de descarga de libVLC
                            if(!mServerController.addNewConnection("127.0.0.1", 1234)){
                                throw new IOException();
                            }
                        } catch (IOException e) {
                            session.close();
                            mPublishSession = null;
                        }
                        WifiAwareNetwork.this.notify();
                    }
                }

                @Override
                public void onSessionConfigFailed() {
                    synchronized (WifiAwareNetwork.this){
                        mPublishSession = null;
                        WifiAwareNetwork.this.notify();
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
                    //El server puede establecer conexión con el primer PeerHandle que recibe en
                    // onMessageReceived, mientras que el subscriber usa el segundo, el PeerHandle que
                    // recibe en el mensaje devuelto por server
                    if(addNewConnection(mPublishSession, peerHandle)){
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
            };
            mWifiAwareSession.publish(config, discoverySessionCallback, workerHandle);

            this.wait();    //Espera a recibir un notify
            return mPublishSession != null;
        }
    }

    /*
        Subscribe to a service
     */
    public boolean subscribeToService(String serviceName, final WifiAwareViewModel viewModel) throws InterruptedException {
        if(mWifiAwareSession == null) return false;

        synchronized (WifiAwareNetwork.this){
            SubscribeConfig config = new SubscribeConfig.Builder()
                    .setServiceName(serviceName)
                    .build();
            mWifiAwareSession.subscribe(config, new DiscoverySessionCallback(){
                private int mLastMessageID = 0;
                private boolean mCreatingConnection = false;
                private final Queue<PeerHandle> mPendingConnections = new LinkedList<>();

                @Override
                public void onSubscribeStarted(@NonNull SubscribeDiscoverySession session) {
                    synchronized (WifiAwareNetwork.this){
                        mSubscribeSession = session;
                        WifiAwareNetwork.this.notify();
                    }
                }

                @Override
                public void onSessionConfigFailed() {
                    synchronized (WifiAwareNetwork.this){
                        mSubscribeSession = null;
                        WifiAwareNetwork.this.notify();
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

                    RtspClientWFA rtspClientWFA = new RtspClientWFA(WifiAwareNetwork.this);

                    rtspClientWFA.setCallback(viewModel); //TODO: Cambiar callback a un LiveData Object, puede haber excepciones
                    mServerController.addClient(peerHandle, rtspClientWFA);
                    rtspClientWFA.connectionCreated(mConManager, createNetworkRequest(mSubscribeSession, peerHandle, -1));

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


    public boolean sessionCreated(){
        return mWifiAwareSession != null;
    }

    public boolean publishSessionCreated(){
        return mPublishSession != null;
    }

    public boolean subscribeSessionCreated(){
        return mSubscribeSession != null;
    }

    public ConnectivityManager getConnectivityManager() {
        return mConManager;
    }


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

        if(mServerController!=null){
            mServerController.releaseClients();
            mServerController = null;
        }

        if(mServerController!=null && mServerController.getServer() != null){
            mServerController.stopServer();
            mServerController = null;
        }
    }

    public NetworkRequest createNetworkRequest(final DiscoverySession subscribeSession, final PeerHandle handle, int serverport){
        NetworkSpecifier ns = serverport>-1
                ?new WifiAwareNetworkSpecifier.Builder(subscribeSession, handle)
                        .setPskPassphrase("wifiawaretest")
                        .setPort(serverport)
                        .build()
                :new WifiAwareNetworkSpecifier.Builder(subscribeSession, handle)
                        .setPskPassphrase("wifiawaretest")
                        .build();

        NetworkRequest nr = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
                .setNetworkSpecifier(ns)
                .build();
        return nr;
    }

    public synchronized boolean addNewConnection(DiscoverySession discoverySession, PeerHandle handle){

        if(!mServerController.isServerEnabled()) return false;
        if(mServerController.has(handle))return true;

        try {
            //Crea un ServerSocketChannel específico para gestionar comunicación entre A y B
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.socket().bind(new InetSocketAddress(0));
            //The port the socket is listening
            int serverPort = serverSocketChannel.socket().getLocalPort();

            NetworkRequest networkRequest = createNetworkRequest(discoverySession, handle, serverPort);

            mServerController.addChangeRequest(new ChangeRequest(serverSocketChannel,
                    ChangeRequest.REGISTER,
                    SelectionKey.OP_ACCEPT));

            RTSPServerWFAModel.Connection conn = new RTSPServerWFAModel.Connection(serverSocketChannel,
                    handle,
                    new WifiAwareNetworkCallback(mServerController, handle, mConManager));

            mConManager.requestNetwork(networkRequest, conn.mNetCallback);

            mServerController.addNewConnection(handle,serverSocketChannel,conn);

        } catch (IOException e) {
            return false;
        }
        return true;
    }

    @Override
    public InetAddress getInetAddress(NetworkCapabilities networkCapabilities) {
        WifiAwareNetworkInfo peerAwareInfo = (WifiAwareNetworkInfo) networkCapabilities.getTransportInfo();
        InetAddress peerIpv6 = peerAwareInfo.getPeerIpv6Addr();
        return peerIpv6;
    }

    @Override
    public int getPort(NetworkCapabilities networkCapabilities) {
        WifiAwareNetworkInfo peerAwareInfo = (WifiAwareNetworkInfo) networkCapabilities.getTransportInfo();
        int peerPort = peerAwareInfo.getPort();
        return peerPort;
    }


    private static class WifiAwareNetworkCallback extends ConnectivityManager.NetworkCallback{
        private final PeerHandle mConnectionHandle;
        private final RTSPServerWFAModel mController;
        private final ConnectivityManager mConManager;

        public WifiAwareNetworkCallback(RTSPServerWFAModel server, PeerHandle connectionHandle, ConnectivityManager conManager){
            this.mConnectionHandle = connectionHandle;
            this.mController = server;
            mConManager = conManager;
        }

        @Override
        public void onAvailable(@NonNull Network network) {
            mController.addNetToConnection(network, mConnectionHandle);
        }

        @Override
        public void onUnavailable() {
            mController.removeConnection(mConnectionHandle);
        }

        @Override
        public void onLost(@NonNull Network network) {
            mController.removeConnection(mConnectionHandle);
        }
    }





}

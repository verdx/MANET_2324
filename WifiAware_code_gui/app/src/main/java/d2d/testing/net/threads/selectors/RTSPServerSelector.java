package d2d.testing.net.threads.selectors;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.wifi.aware.DiscoverySession;
import android.net.wifi.aware.PeerHandle;
import android.net.wifi.aware.WifiAwareNetworkSpecifier;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import d2d.testing.MainActivity;
import d2d.testing.net.threads.workers.RTSPServerWorker;

/**
 * Implementacion del AbstractSelector. Se encarga de crear un ServerSocketChannel y asociarlo al Selector para recibir eventos de tipo Accept (initiateInstance).
 * Cuando los clientes intenten conectar con el ServerSocketChannel se creara, en la funcion accept() de AbstractSelector, un SocketChannel con el cliente
 * y se añadira al Selector para recibir eventos de tipo Read.
 * Cuando se comuniquen por el canal se guardaran los datos en mReadBuffer y se pasaran al thread del RTSPServerWorker, que es creado por esta clase.
 */
public class RTSPServerSelector extends AbstractSelector {
    static private RTSPServerSelector INSTANCE = null;

    private Map<PeerHandle, Connection> mConnectionsMap;
    private Map<ServerSocketChannel, Connection> mServerChannelsMap;

    private RTSPServerSelector(ConnectivityManager connManager) throws IOException {
        super(connManager);
        mConnectionsMap = new HashMap<>();
        mServerChannelsMap = new HashMap<>();

        mWorker = new RTSPServerWorker(null, null, null, this);
        mWorker.start();
    }

    private RTSPServerSelector(MainActivity mainActivity, ConnectivityManager connManager) throws IOException {
        super(connManager);
        mConnectionsMap = new HashMap<>();
        mServerChannelsMap = new HashMap<>();
        mWorker = new RTSPServerWorker(null, null, mainActivity, this);
        mWorker.start();
    }

    public static RTSPServerSelector getInstance() throws IOException {
        if(INSTANCE == null) {
            INSTANCE = new RTSPServerSelector(null);
        }

        return INSTANCE;
    }

    public static RTSPServerSelector initiateInstance(MainActivity mainActivity, ConnectivityManager connManager) throws IOException {
        if(INSTANCE == null) {
            INSTANCE = new RTSPServerSelector(mainActivity,connManager);
        }

        return INSTANCE;
    }

    public static boolean itsInitialized(){
        return INSTANCE != null;
    }


    public synchronized boolean addNewConnection(DiscoverySession discoverySession, PeerHandle handle){
        if(mConnectionsMap.get(handle) != null){
            return true;
        }
        if(!mEnabled) return false;

        ServerSocketChannel serverSocketChannel = null;
        int mServerPort;
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.socket().bind(new InetSocketAddress(0));
            mServerPort = serverSocketChannel.socket().getLocalPort();
            NetworkSpecifier networkSpecifier = new WifiAwareNetworkSpecifier.Builder(discoverySession, handle)  //Objeto de especificador de red utilizado para solicitar una red compatible con Wi-Fi. Las aplicaciones deben usar el WifiAwareNetworkSpecifier.Builderclass para crear una instancia.
                    .setPskPassphrase("wifiawaretest")
                    .setPort(mServerPort)
                    .build();
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
                    .setNetworkSpecifier(networkSpecifier)
                    .build();
            this.addChangeRequest(new ChangeRequest(serverSocketChannel, ChangeRequest.REGISTER, SelectionKey.OP_ACCEPT));
            Connection conn = new Connection(serverSocketChannel, handle);
            mConnectionsMap.put(handle, conn);
            mServerChannelsMap.put(serverSocketChannel, conn);
            mConManager.requestNetwork(networkRequest, new WifiAwareNetworkCallback(handle));
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public synchronized boolean addNewConnection(String serverIP, int serverPort){
        if(!mEnabled) return false;
        ServerSocketChannel serverSocketChannel = null;
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.socket().bind(new InetSocketAddress(serverIP ,serverPort));
            this.addChangeRequest(new ChangeRequest(serverSocketChannel, ChangeRequest.REGISTER, SelectionKey.OP_ACCEPT));
            mConnections.add(serverSocketChannel);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public synchronized void removeConnection(PeerHandle handle) {
        Connection conn =  mConnectionsMap.get(handle);
        ServerSocketChannel serverChan = null;
        if(conn == null){
            return;
        }
        serverChan = conn.mServerSocketChannel;
        conn.closeConnection(this);
        mConnectionsMap.remove(handle);
        mServerChannelsMap.remove(serverChan);
    }

    protected void accept(@NonNull SelectionKey key) throws IOException {
        synchronized (this){
            ServerSocketChannel serverChan = (ServerSocketChannel) key.channel();
            Connection conn = mServerChannelsMap.get(serverChan);
            if(conn == null){
                super.accept(key);
                return;
            }
            SocketChannel socketChannel = null;
            try {
                socketChannel = serverChan.accept();
                socketChannel.configureBlocking(false);// Accept the connection and make it non-blocking
                socketChannel.register(mSelector, SelectionKey.OP_READ);
                conn.mComChannels.add(socketChannel);
            } catch (IOException e) {
                mConnectionsMap.remove(conn.handle);
                mServerChannelsMap.remove(serverChan);
                conn.closeConnection(this);
            }
        }
    }


    @Override
    protected void initiateConnection() { //No se crea un canal de escucha para el servidor, como en UDPServerSelector, si no que por cada subscriber se crea un canal de escucha en addNewConnection
        mStatusTCP = STATUS_LISTENING;
    }

    @Override
    protected void onClientDisconnected(SelectableChannel channel) {
        ((RTSPServerWorker) mWorker).onClientDisconnected(channel);
    }

    @Override
    public void send(byte[] data) {
        for (SelectableChannel socket : mConnections) {
            this.send(socket,data);
        }
    }


    public synchronized Network getChannelNetwork(SelectableChannel chan){
        for(Map.Entry<ServerSocketChannel, Connection> entry : mServerChannelsMap.entrySet()){
            if(entry.getKey().equals(chan)){
                return entry.getValue().net;
            }
            for(SocketChannel sockChan : entry.getValue().mComChannels){
                if(sockChan.equals(chan)){
                    return entry.getValue().net;
                }
            }
        }
        return null;
    }

    public void setAllowLiveStreaming(boolean allow) {
        ((RTSPServerWorker) mWorker).setAllowLiveStreaming(allow);
    }

    public class Connection{

        public Connection(ServerSocketChannel serverChan, PeerHandle handle){
            mServerSocketChannel = serverChan;
            this.handle = handle;
            mComChannels = new ArrayList<>();
        }
        public ServerSocketChannel mServerSocketChannel;
        public PeerHandle handle;
        public Network net;
        public List<SocketChannel> mComChannels;

        public void closeConnection(RTSPServerSelector serverSelector){
            serverSelector.addChangeRequest(new ChangeRequest(mServerSocketChannel, ChangeRequest.REMOVE, 0));
            for(SocketChannel chan : this.mComChannels){
                serverSelector.addChangeRequest(new ChangeRequest(chan, ChangeRequest.REMOVE, 0));
                serverSelector.onClientDisconnected(chan);
            }
            this.mServerSocketChannel = null;
            this.handle = null;
            this.net = null;
            this.mComChannels.clear();
        }
    }

    private class WifiAwareNetworkCallback extends ConnectivityManager.NetworkCallback{

        PeerHandle mConnectionHandle;

        public WifiAwareNetworkCallback(PeerHandle connectionHandle){
            this.mConnectionHandle = connectionHandle;
        }

        @Override
        public void onAvailable(@NonNull Network network) {
            synchronized (RTSPServerSelector.this){
                Connection conn = mConnectionsMap.get(mConnectionHandle);
                if(conn == null){
                    return;
                }
                conn.net = network;
            }
        }


        @Override
        public void onLost(@NonNull Network network) {
            mConManager.unregisterNetworkCallback(this);
            removeConnection(mConnectionHandle);
        }
    }

}
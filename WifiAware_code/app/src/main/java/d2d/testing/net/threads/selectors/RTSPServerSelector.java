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

import d2d.testing.net.threads.workers.RTSPServerWorker;

/**
 * Implementacion del AbstractSelector. Se encarga de crear un ServerSocketChannel y asociarlo al Selector para recibir eventos de tipo Accept (initiateInstance).
 * Cuando los clientes intenten conectar con el ServerSocketChannel se creara, en la funcion accept() de AbstractSelector, un SocketChannel con el cliente
 * y se añadira al Selector para recibir eventos de tipo Read.
 * Cuando se comuniquen por el canal se guardaran los datos en mReadBuffer y se pasaran al thread del RTSPServerWorker, que es creado por esta clase.
 */
public class RTSPServerSelector extends AbstractSelector {

    private final Map<PeerHandle, Connection> mConnectionsMap;
    private final Map<ServerSocketChannel, Connection> mServerChannelsMap;


    public RTSPServerSelector(ConnectivityManager connManager) throws IOException {
        super(connManager);
        mConnectionsMap = new HashMap<>();
        mServerChannelsMap = new HashMap<>();

        mWorker = new RTSPServerWorker(null, null, this);
        mWorker.start();
    }


    public synchronized boolean addNewConnection(DiscoverySession discoverySession, PeerHandle handle){
        if(!mEnabled.get()) return false;
        if(mConnectionsMap.get(handle) != null){
            return true;
        }

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
            Connection conn = new Connection(serverSocketChannel, handle, new WifiAwareNetworkCallback(this, handle, mConManager));
            mConnectionsMap.put(handle, conn);
            mServerChannelsMap.put(serverSocketChannel, conn);
            //mNetRequestMan.requestNetwork(networkRequest, conn.mNetCallback);
            mConManager.requestNetwork(networkRequest, conn.mNetCallback);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public synchronized boolean addNewConnection(String serverIP, int serverPort){
        if(!mEnabled.get()) return false;
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
        conn.closeConnection(this, mConManager);
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
                socketChannel.register(mSelector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                conn.mComChannels.add(socketChannel);
            } catch (IOException e) {
                mConnectionsMap.remove(conn.handle);
                mServerChannelsMap.remove(serverChan);
                conn.closeConnection(this, mConManager);
            }
        }
    }

    private synchronized void addNetToConnection(Network net, PeerHandle conHandle){
        Connection conn = mConnectionsMap.get(conHandle);
        if(conn != null){
            conn.net = net;
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

    @Override
    protected synchronized void onServerRelease() {
        for(Connection conn : mConnectionsMap.values()){
            conn.closeConnection(this, mConManager);
        }
        mConnectionsMap.clear();
        mServerChannelsMap.clear();
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

    private static class Connection{

        public Connection(ServerSocketChannel serverChan, PeerHandle handle, ConnectivityManager.NetworkCallback netCallback){
            mServerSocketChannel = serverChan;
            mNetCallback = netCallback;
            this.handle = handle;
            mComChannels = new ArrayList<>();
        }
        public ServerSocketChannel mServerSocketChannel;
        public PeerHandle handle;
        public Network net;
        public List<SocketChannel> mComChannels;
        public ConnectivityManager.NetworkCallback mNetCallback;

        public void closeConnection(RTSPServerSelector serverSelector, ConnectivityManager conManager){
            serverSelector.addChangeRequest(new ChangeRequest(mServerSocketChannel, ChangeRequest.REMOVE, 0));
            for(SocketChannel chan : this.mComChannels){
                serverSelector.addChangeRequest(new ChangeRequest(chan, ChangeRequest.REMOVE_AND_NOTIFY, 0));
            }
            this.mServerSocketChannel = null;
            this.handle = null;
            this.net = null;
            conManager.unregisterNetworkCallback(mNetCallback);
            this.mComChannels.clear();
        }
    }

    private static class WifiAwareNetworkCallback extends ConnectivityManager.NetworkCallback{

        private final PeerHandle mConnectionHandle;
        private final RTSPServerSelector mServer;
        private final ConnectivityManager mConManager;

        public WifiAwareNetworkCallback(RTSPServerSelector server, PeerHandle connectionHandle, ConnectivityManager conManager){
            this.mConnectionHandle = connectionHandle;
            this.mServer = server;
            mConManager = conManager;
        }

        @Override
        public void onAvailable(@NonNull Network network) {
            mServer.addNetToConnection(network, mConnectionHandle);
        }

        @Override
        public void onUnavailable() {
            mServer.removeConnection(mConnectionHandle);
        }

        @Override
        public void onLost(@NonNull Network network) {
            mServer.removeConnection(mConnectionHandle);
        }
    }

}
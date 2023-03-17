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

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;

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
import java.util.concurrent.atomic.AtomicBoolean;

import d2d.testing.gui.main.ConnectionProvider;
import d2d.testing.gui.main.INetwork;
import d2d.testing.gui.main.NetworkModule;
import d2d.testing.gui.main.WifiAwareNetwork;
import d2d.testing.net.threads.workers.RTSPServerWorker;

/**
 * Implementacion del AbstractSelector. Se encarga de crear un ServerSocketChannel y asociarlo al
 * Selector para recibir eventos de tipo Accept (initiateInstance).
 * Cuando los clientes intenten conectar con el ServerSocketChannel se creara, en la funcion accept()
 * de AbstractSelector, un SocketChannel con el cliente y se añadira al Selector para recibir eventos de tipo Read.
 * Cuando se comuniquen por el canal se guardaran los datos en mReadBuffer y se pasaran al thread
 * del RTSPServerWorker, que es creado por esta clase.
 */
public class RTSPServerSelector<T> extends AbstractSelector {

    private final Map<T, Connection> mConnectionsMap;
    private final Map<ServerSocketChannel, Connection> mServerChannelsMap;

    public RTSPServerSelector(ConnectivityManager connManager) throws IOException {
        super(connManager);
        mConnectionsMap = new HashMap<>();
        mServerChannelsMap = new HashMap<>();

        mWorker = new RTSPServerWorker(null, null, this);
        mWorker.start();
    }

    public Map<T, Connection> getmConnectionsMap() {
        return mConnectionsMap;
    }

    public AtomicBoolean getEnabled() {
        return mEnabled;
    }

    public synchronized void addNewConnection(T handle, ServerSocketChannel serverSocketChannel, Connection conn){
        mConnectionsMap.put(handle, conn);
        mServerChannelsMap.put(serverSocketChannel, conn);
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

    public synchronized void removeConnection(T handle) {
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

    public synchronized void addNetToConnection(Network net, T conHandle){
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
            for(Object sockChan : entry.getValue().mComChannels){
                if(sockChan.equals(chan)){
                    return entry.getValue().net;
                }
            }
        }
        return null;
    }

    public static class Connection<T>{

        public Connection(ServerSocketChannel serverChan, T handle, ConnectivityManager.NetworkCallback netCallback){
            mServerSocketChannel = serverChan;
            mNetCallback = netCallback;
            this.handle = handle;
            mComChannels = new ArrayList<>();
        }
        public ServerSocketChannel mServerSocketChannel;
        public T handle;
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


}
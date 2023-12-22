package d2d.testing.gui.main;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.wifi.aware.PeerHandle;

import net.verdx.libstreaming.rtsp.RTSPServerModel;
import net.verdx.libstreaming.rtsp.RtspClient;
import net.verdx.libstreaming.threads.selectors.RTSPServerSelector;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class RTSPServerWFAModel extends RTSPServerModel {
    protected ConnectivityManager mConManager;

    /*
        * Being final only means that the reference cannot be reasigned after initialization.
        * It doesn't mean that the object itself is immutable
            * It's final as a HashMap, but as a Map instance, its content can be modified
     */
    private final Map<PeerHandle, Connection> mConnectionsMap;
    private final Map<ServerSocketChannel, Connection> mServerChannelsMap;
    private final Map<PeerHandle, RtspClient> mClients;


    public RTSPServerWFAModel(ConnectivityManager connManager) throws IOException {
        super(connManager);
        mConnectionsMap = new HashMap<>();
        mServerChannelsMap = new HashMap<>();
        mClients = new HashMap<>();

        mConManager = connManager;

    }

    public void addNewConnection(PeerHandle handle, ServerSocketChannel serverSocketChannel, Connection conn) {
        mConnectionsMap.put(handle, conn);
        mServerChannelsMap.put(serverSocketChannel, conn);
    }

    public boolean has(PeerHandle handle){
        return mConnectionsMap.containsKey(handle);
    }

    public void addNetToConnection(Network network, PeerHandle mConnectionHandle) {
        Connection conn = mConnectionsMap.get(mConnectionHandle);
        if(conn != null){
            conn.net = network;
        }
    }

    public void removeConnection(PeerHandle handle) {
        Connection conn =  mConnectionsMap.get(handle);
        ServerSocketChannel serverChan = null;
        if(conn == null){
            return;
        }
        serverChan = conn.mServerSocketChannel;
        conn.closeConnection(mServer, mConManager);
        mConnectionsMap.remove(handle);
        mServerChannelsMap.remove(serverChan);
    }

    @Override
    public boolean accept(ServerSocketChannel serverChan, Selector selector) {
        synchronized (this){
            Connection conn = mServerChannelsMap.get(serverChan);
            if(conn == null){
                return false;
            }
            SocketChannel socketChannel = null;
            try {
                socketChannel = serverChan.accept();
                socketChannel.configureBlocking(false);// Accept the connection and make it non-blocking
                socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                conn.mComChannels.add(socketChannel);

            } catch (IOException e) {
                mConnectionsMap.remove(conn.handle);
                mServerChannelsMap.remove(serverChan);
                conn.closeConnection(mServer, mConManager);
            }
        }
        return true;
    }

    @Override
    public void handleAcceptException(ServerSocketChannel serverChan) {
        Connection conn = mServerChannelsMap.get(serverChan);
        mConnectionsMap.remove(conn.handle);
        mServerChannelsMap.remove(serverChan);
    }

    @Override
    public void onServerRelease() {
        for(Connection conn : mConnectionsMap.values()){
            conn.closeConnection(mServer, mConManager);
        }
        mConnectionsMap.clear();
        mServerChannelsMap.clear();
    }

    @Override
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

    public void addClient(PeerHandle peerHandle, RtspClientWFA rtspClientWFA) {
        mClients.put(peerHandle, rtspClientWFA);
    }

    public void releaseClients() {
        for(RtspClient client : mClients.values()){
            client.release();
        }
        mClients.clear();
    }

    public static class Connection extends RTSPServerModel.Connection{
        public Connection(ServerSocketChannel serverChan, PeerHandle handle, ConnectivityManager.NetworkCallback netCallback){
            super(serverChan);
            mNetCallback = netCallback;
            this.handle = handle;
        }
        PeerHandle handle;
        Network net;
        ConnectivityManager.NetworkCallback mNetCallback;

        public void closeConnection(RTSPServerSelector serverSelector, ConnectivityManager conManager){
            super.closeConnection(serverSelector);
            this.handle = null;
            this.net = null;
            conManager.unregisterNetworkCallback(mNetCallback);
        }
    }
}

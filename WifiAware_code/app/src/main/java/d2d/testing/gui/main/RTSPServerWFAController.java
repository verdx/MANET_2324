package d2d.testing.gui.main;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.wifi.aware.PeerHandle;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import d2d.testing.net.threads.selectors.ChangeRequest;
import d2d.testing.net.threads.selectors.RTSPServerSelector;

public class RTSPServerWFAController extends RTSPServerController{
    protected ConnectivityManager mConManager;
    private final Map<PeerHandle, Connection> mConnectionsMap;
    private final Map<ServerSocketChannel, Connection> mServerChannelsMap;

    RTSPServerSelector mServer;

    public RTSPServerWFAController(ConnectivityManager connManager) throws IOException {
        mConnectionsMap = new HashMap<>();
        mServerChannelsMap = new HashMap<>();
        mConManager = connManager;

        mServer = new RTSPServerSelector(this, connManager);
    }

    public RTSPServerSelector getServer(){
        return mServer;
    }


    public synchronized boolean addNewConnection(String serverIP, int serverPort){
        return mServer.addNewConnection(serverIP,serverPort);
    }

    public void addNewConnection(PeerHandle handle, ServerSocketChannel serverSocketChannel, Connection conn) {
        mConnectionsMap.put(handle, conn);
        mServerChannelsMap.put(serverSocketChannel, conn);
    }

    public void startServer(){
        mServer.start();
    }

    public void stopServer(){
        mServer.stop();
    }

    public boolean isServerEnabled(){
        return mServer.getEnabled().get();
    }

    public boolean has(PeerHandle handle){
        return mConnectionsMap.containsKey(handle);
    }


    public void addChangeRequest(ChangeRequest changeRequest) {
        mServer.addChangeRequest(changeRequest);
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

    public static class Connection{
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
}

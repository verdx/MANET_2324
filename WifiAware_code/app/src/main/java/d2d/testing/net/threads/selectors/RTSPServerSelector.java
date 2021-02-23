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
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import d2d.testing.MainActivity;
import d2d.testing.utils.Logger;
import d2d.testing.net.threads.workers.RTSPServerWorker;

/**
 * Implementacion del AbstractSelector. Se encarga de crear un ServerSocketChannel y asociarlo al Selector para recibir eventos de tipo Accept (initiateInstance).
 * Cuando los clientes intenten conectar con el ServerSocketChannel se creara, en la funcion accept() de AbstractSelector, un SocketChannel con el cliente
 * y se añadira al Selector para recibir eventos de tipo Read.
 * Cuando se comuniquen por el canal se guardaran los datos en mReadBuffer y se pasaran al thread del RTSPServerWorker, que es creado por esta clase.
 */
public class RTSPServerSelector extends AbstractSelector {
    private ServerSocketChannel mServerSocketChannel;

    static private RTSPServerSelector INSTANCE = null;

    private Map<PeerHandle, Connection> mConnectionsMap;
    private Map<ServerSocketChannel, Connection> mServerChannelsMap;
    public Network mLastNet;

    private RTSPServerSelector(ConnectivityManager connManager) throws IOException {
        super(null, connManager);
        mConnectionsMap = new HashMap<>();
        mServerChannelsMap = new HashMap<>();

        mWorker = new RTSPServerWorker(null, null, null);
        mWorker.start();
    }

    private RTSPServerSelector(MainActivity mainActivity, ConnectivityManager connManager) throws IOException {
        super(mainActivity, connManager);
        mConnectionsMap = new HashMap<>();
        mServerChannelsMap = new HashMap<>();
        mWorker = new RTSPServerWorker(null, null, mainActivity);
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


    public void run(){
        mSelectorLock.lock();
        while(mEnabled){
            try {
                mSelectorLock.unlock();
                mSelector.select();
                mSelectorLock.lock();
                this.processChangeRequests();
                Iterator<SelectionKey> itKeys = mSelector.selectedKeys().iterator();
                while (itKeys.hasNext()) {
                    SelectionKey myKey = itKeys.next();
                    itKeys.remove();

                    if (!myKey.isValid()) {
                        continue;
                    }

                    if (myKey.isAcceptable()) {
                        this.accept(myKey);
                    } else if (myKey.isConnectable()) {
                        this.finishConnection(myKey);
                    } else if (myKey.isReadable()) {
                        this.read(myKey);
                    } else if (myKey.isWritable()) {
                        this.write(myKey);
                    }
                }
            } catch (IOException e) {
                mEnabled = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mSelectorLock.unlock();
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
            mSelectorLock.lock();
            mSelector.wakeup();
            try{
                SelectionKey serverKey = serverSocketChannel.register(mSelector, SelectionKey.OP_ACCEPT);
            }catch (IOException ex){
                mSelectorLock.unlock();
                serverSocketChannel.close();
                throw ex;
            }
            mSelectorLock.unlock();
            Connection conn = new Connection(serverSocketChannel, handle);
            mConnectionsMap.put(handle, conn);
            mServerChannelsMap.put(serverSocketChannel, conn);
            mConManager.requestNetwork(networkRequest, new WifiAwareNetworkCallback(handle));
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public synchronized boolean addLoopbackConnection(){
        if(!mEnabled) return false;
        ServerSocketChannel serverSocketChannel = null;
        int mServerPort;
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            mServerPort = 1234;
            serverSocketChannel.socket().bind(new InetSocketAddress("127.0.0.1" ,mServerPort));
            mSelectorLock.lock();
            mSelector.wakeup();
            try{
                SelectionKey serverKey = serverSocketChannel.register(mSelector, SelectionKey.OP_ACCEPT);
            }catch (IOException ex){
                mSelectorLock.unlock();
                serverSocketChannel.close();
                throw ex;
            }
            mSelectorLock.unlock();
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
        mSelectorLock.lock();
        mSelector.wakeup();
        conn.closeConnection(mSelector);
        mSelectorLock.unlock();
        mConnectionsMap.remove(handle);
        mServerChannelsMap.remove(serverChan);
    }

    protected synchronized void accept(@NonNull SelectionKey key){
        ServerSocketChannel serverChan = (ServerSocketChannel) key.channel();
        try {
            if(((InetSocketAddress)serverChan.getLocalAddress()).getAddress().getHostAddress().equals("127.0.0.1")){ //QUE FUNCION DESEMPEÑA ESTE IF: Averiguar si lo estamos enviando desde looopback??
                super.accept(key);
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Connection conn = mServerChannelsMap.get(serverChan);
        if(conn == null){
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
            conn.closeConnection(mSelector);
        }
    }

    @Override
    protected void initiateConnection() {
        try {
            mServerSocketChannel = (ServerSocketChannel) ServerSocketChannel.open().configureBlocking(false);
            mServerSocketChannel.socket().bind(new InetSocketAddress(mPortTCP));
            // Create a new non-blocking server socket channel and start listening
            mStatusTCP = STATUS_LISTENING;
            addChangeRequest(new ChangeRequest(mServerSocketChannel, ChangeRequest.REGISTER, SelectionKey.OP_ACCEPT));
            Logger.d("RTSPServerSelector: initiateConnection(): server listening TCP connections on port " + mPortTCP);
        } catch (IOException e) {
            Logger.d("RTSPServerSelector: initiateConnection(): failed to listen at port " + mPortTCP);
            e.printStackTrace();
        }
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

        public void closeConnection(Selector selector){
            try {
                SelectionKey selKey = this.mServerSocketChannel.keyFor(selector);
                if(selKey != null) selKey.cancel();
                this.mServerSocketChannel.close();
                for(SocketChannel chan : this.mComChannels){
                    selKey = chan.keyFor(selector);
                    if(selKey != null) selKey.cancel();
                    chan.close();
                }
            } catch (IOException e) {}
            finally {
                this.mServerSocketChannel = null;
                this.handle = null;
                this.net = null;
                this.mComChannels.clear();

            }
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
                mLastNet = network;
            }
        }


        @Override
        public void onLost(@NonNull Network network) {
            mConManager.unregisterNetworkCallback(this);
            removeConnection(mConnectionHandle);
        }
    }

}
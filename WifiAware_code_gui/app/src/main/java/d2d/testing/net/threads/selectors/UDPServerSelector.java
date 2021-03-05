package d2d.testing.net.threads.selectors;

import android.net.ConnectivityManager;
import android.net.Network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

import d2d.testing.net.threads.workers.EchoWorker;
import d2d.testing.utils.Logger;

public class UDPServerSelector extends AbstractSelector {
    private DatagramChannel mDatagramChannel;
    private int mPortUDP;
    private InetAddress mLocalAddress;
    private Network mSocketNet;

    public UDPServerSelector(InetAddress localAddress, int port, Network net, ConnectivityManager conManager) throws IOException {
        super(conManager);
        if(conManager != null && net == null) throw new IllegalArgumentException("Network object cannot be null");
        mSocketNet = net;
        mPortUDP = port;
        mLocalAddress = localAddress;
        mWorker = new EchoWorker();
        mWorker.start();
    }

    public UDPServerSelector(InetAddress localAddress, int port) throws IOException {
        this(localAddress, port, null, null);
    }


    @Override
    protected void onClientDisconnected(SelectableChannel socketChannel) {} //Los canales se cierran en el run del AbstractSelector tras hacer stop, no hace falta cerrarlos aqui

    @Override
    protected void initiateConnection() {
        try {
            if(mConManager != null && !mConManager.bindProcessToNetwork(mSocketNet)) throw new IOException("Error bind to net");
            mDatagramChannel = (DatagramChannel) DatagramChannel.open().configureBlocking(false);
            mDatagramChannel.socket().bind(new InetSocketAddress(mLocalAddress, mPortUDP));
            mStatusUDP = STATUS_LISTENING;
            this.addChangeRequest(new ChangeRequest(mDatagramChannel, ChangeRequest.REGISTER, SelectionKey.OP_READ));
            if(mConManager != null) mConManager.bindProcessToNetwork(null);
            Logger.d("UDPServerSelector: initiateConnection as server listening UDP on port " + mLocalAddress.getHostAddress() + ":" + mPortUDP);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SelectableChannel addConnectionUDP(InetAddress address, int port) throws IOException {

        DatagramChannel datagramChannel =  (DatagramChannel) DatagramChannel.open().configureBlocking(false);
        datagramChannel.connect(new InetSocketAddress(address.getHostAddress(), port));
        addChangeRequest(new ChangeRequest(datagramChannel, ChangeRequest.REGISTER, SelectionKey.OP_READ));
        mConnections.add(datagramChannel);

        Logger.d("UDPServerSelector: initiateConnection UDP client 'connected' to " + address.getHostAddress() + ":" + port);

        return datagramChannel;
    }

    @Override
    public void send(byte[] data) {
        Logger.d("UDPServerSelector: sending " + data.length + "bytes to " + mConnections.size());
        for (SelectableChannel socket : mConnections) {
            this.send(socket,data);
        }
    }
}
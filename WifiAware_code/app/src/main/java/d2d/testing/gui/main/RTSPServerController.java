package d2d.testing.gui.main;

import android.net.Network;

import java.nio.channels.SelectableChannel;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

public class RTSPServerController {
    public boolean accept(ServerSocketChannel serverChan, Selector selector) {return false;}
    public void handleAcceptException(ServerSocketChannel serverChan) {}
    public void onServerRelease() {}
    public synchronized Network getChannelNetwork(SelectableChannel chan){return null;}

}

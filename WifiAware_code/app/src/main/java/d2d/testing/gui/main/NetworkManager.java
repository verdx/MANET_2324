package d2d.testing.gui.main;

import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.wifi.aware.DiscoverySession;
import android.net.wifi.aware.PeerHandle;
import android.net.wifi.aware.WifiAwareNetworkSpecifier;

import java.net.InetAddress;

public class NetworkManager {
    public InetAddress getInetAddress(NetworkCapabilities networkCapabilities){
        return null;
    }

    public int getPort(NetworkCapabilities networkCapabilities){
        return -1;
    }

}

package d2d.testing.gui.main;

import android.net.NetworkCapabilities;

import java.net.InetAddress;

public interface INetworkManager {
    public InetAddress getInetAddress(NetworkCapabilities networkCapabilities);
    public int getPort(NetworkCapabilities networkCapabilities);

}

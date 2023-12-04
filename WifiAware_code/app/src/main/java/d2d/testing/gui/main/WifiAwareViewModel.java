package d2d.testing.gui.main;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.aware.WifiAwareManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import d2d.testing.R;

public class WifiAwareViewModel extends BasicViewModel {

    private WifiAwareNetwork mWifiAwareNetwork;
    private final WifiAwareManager mWifiAwareManager;

    public WifiAwareViewModel(@NonNull Application app) {
        super(app);

        mIsNetworkAvailable = new MutableLiveData<>(Boolean.FALSE);

        if(!app.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)){
            mWifiAwareManager = null;
            return;
        }

        ConnectivityManager mConManager = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
        /*  Source: https://developer.android.com/guide/topics/connectivity/wifi-aware#initial_setup
        *   Since the availability of WiFi Aware can change at any time, we need a BroadcastReceiver
        *   to receive ACTION_WIFI_AWARE_STATE_CHANGED, which is sent whenever availability changes.
        *
        *   When your app receives the broadcast intent, it should discard all existing sessions
        *   (assume that Wi-Fi Aware service was disrupted), then check the current state of
        *   availability and adjust its behavior accordingly.
         */
        mWifiAwareManager = (WifiAwareManager)app.getSystemService(Context.WIFI_AWARE_SERVICE);
        IntentFilter filter = new IntentFilter(WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED);
        BroadcastReceiver myReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                checkWifiAwareAvailability();
            }
        };
        app.registerReceiver(myReceiver, filter);

        mWifiAwareNetwork = new WifiAwareNetwork(mConManager, mWifiAwareManager);
        checkWifiAwareAvailability();
    }

    @Override
    protected String getNetworkAvailabilityString(Context c, boolean available){
        if(available){
            return c.getString(R.string.wfa_available_str);
        }
        return c.getString(R.string.wfa_unavailable_str);
    }

    @Override
    protected void initNetwork(){
        if(!sessionCreated()){
            try {
                if(createSession()){
                    publishService("Server");
                    subscribeToService("Server");
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onCleared() {
        closeSessions();
        super.onCleared();
    }

    private void checkWifiAwareAvailability(){
        closeSessions();

        if(mWifiAwareManager.isAvailable()){
            mIsNetworkAvailable.postValue(Boolean.TRUE);
        }
        else{
            mIsNetworkAvailable.postValue(Boolean.FALSE);
        }
    }

    public boolean sessionCreated(){
        return mWifiAwareNetwork.sessionCreated();
    }

    public boolean createSession() throws InterruptedException {
        return mWifiAwareNetwork.createSession();
    }

    //Puede llamarlo el main thread al cambiar la disponibilidad de wifiaware u otro thread
    public synchronized void closeSessions(){
        mWifiAwareNetwork.closeSessions();
    }

    //https://developer.android.com/guide/topics/connectivity/wifi-aware#publish_a_service
    public void publishService(String serviceName) throws InterruptedException {
        mWifiAwareNetwork.publishService(serviceName);
    }

    //https://developer.android.com/guide/topics/connectivity/wifi-aware#subscribe_to_a_service
    public void subscribeToService(String serviceName) throws InterruptedException {
        mWifiAwareNetwork.subscribeToService(serviceName, this);
    }

}

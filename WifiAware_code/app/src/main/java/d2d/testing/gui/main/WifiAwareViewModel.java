package d2d.testing.gui.main;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.aware.WifiAwareManager;
import android.os.HandlerThread;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import d2d.testing.R;
import d2d.testing.streaming.rtsp.RtspClient;

public class WifiAwareViewModel extends BasicViewModel {

    private WifiAwareNetwork mWifiAwareNetwork;
    private static ConnectivityManager mConManager;
    private final WifiAwareManager mWifiAwareManager;

    public WifiAwareViewModel(@NonNull Application app) {
        super(app);

        mIsNetworkAvailable = new MutableLiveData<>(Boolean.FALSE);

        if(!app.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)){
            mWifiAwareManager = null;
            return;
        }

        mConManager = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);

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
    protected void initNetwork(){
        if(!sessionCreated()){
            try {
                if(createSession()){
                    if(publishService("Server")){
                        //Toast.makeText(this.getContext(), "Se creo una sesion de publisher con WifiAware", Toast.LENGTH_SHORT).show();
                    }else{
                        //Toast.makeText(this.getContext(), "No se pudo crear una sesion de publisher de WifiAware", Toast.LENGTH_LONG).show();
                    }

                    if(subscribeToService("Server")){
                        //Toast.makeText(this.getContext(), "Se creo una sesion de subscripcion con WifiAware", Toast.LENGTH_SHORT).show();
                    }else{
                        //Toast.makeText(this.getContext(), "No se pudo crear una sesion de subscripcion de WifiAware", Toast.LENGTH_LONG).show();
                    }
                }else {
                    //Toast.makeText(this.getContext(), "No se pudo crear la sesion de WifiAware", Toast.LENGTH_LONG).show();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected String getNetworkAvailabilityString(Context c, boolean available){
        if(available){
            return c.getString(R.string.wfa_available_str);
        }
        return c.getString(R.string.wfa_unavailable_str);
    }

    @Override
    protected void onCleared() {
        closeSessions();
        worker.quitSafely();
    }

    public boolean isWifiAwareSupported(){
        return mWifiAwareManager != null;
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

    public boolean publishSessionCreated(){
        return mWifiAwareNetwork.publishSessionCreated();
    }

    public boolean subscribeSessionCreated(){
        return mWifiAwareNetwork.subscribeSessionCreated();
    }

    public static ConnectivityManager getConnectivityManager() {
        return WifiAwareNetwork.getConnectivityManager();
    }


    public boolean createSession() throws InterruptedException {
        return mWifiAwareNetwork.createSession();
    }

    //https://developer.android.com/guide/topics/connectivity/wifi-aware#publish_a_service
    public boolean publishService(String serviceName) throws InterruptedException {
        return mWifiAwareNetwork.publishService(serviceName);
    }

    //https://developer.android.com/guide/topics/connectivity/wifi-aware#subscribe_to_a_service
    public boolean subscribeToService(String serviceName) throws InterruptedException {
        return mWifiAwareNetwork.subscribeToService(serviceName, this);
    }

    //Puede llamarlo el main thread al cambiar la disponibilidad de wifiaware u otro thread
    public synchronized void closeSessions(){
        mWifiAwareNetwork.closeSessions();
    }


}

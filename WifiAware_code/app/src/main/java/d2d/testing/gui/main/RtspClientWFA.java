/*
 * Copyright (C) 2011-2015 GUIGUI Simon, fyhertz@gmail.com
 *
 * This file is part of libstreaming (https://github.com/fyhertz/libstreaming)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package d2d.testing.gui.main;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;

import d2d.testing.streaming.StreamingRecord;
import d2d.testing.streaming.network.INetworkManager;
import d2d.testing.streaming.rtsp.RtspClient;

/**
 * RFC 2326.
 * A basic and asynchronous RTSP client.
 * The original purpose of this class was to implement a small RTSP client compatible with Wowza.
 * It implements Digest Access Authentication according to RFC 2069. 
 */
public class RtspClientWFA extends RtspClient {

	public final static String TAG = "RtspClientWFA";
	protected WFANetworkCallback mNetworkCallback;
	protected NetworkRequest mNetworkRequest;
	protected ConnectivityManager mConnManager;
	protected NetworkCapabilities mCurrentNetCapabitities;
	protected Network mCurrentNet;

	public RtspClientWFA(INetworkManager netMana) {
		super(netMana);
	}

	public void connectionCreated(final ConnectivityManager manager, final NetworkRequest networkRequest){
			mHandler.post(new Runnable() {

			@Override
			public void run() {
				if(mState != STATE_STOPPED) return;
				mCurrentNetCapabitities = null;
				mCurrentNet = null;
				mConnManager = manager;

				//++++++++++++++++
				mNetworkRequest = networkRequest;
				//++++++++++++++++

				mState = STATE_STARTED;

				//++++++++++++++++
				mNetworkCallback = new WFANetworkCallback();
				//++++++++++++++++

				mTotalNetworkRequests = 0;
				//Si no obtienes el network antes del timeout, se produce el código de error 0x2

				//++++++++++++++++
				manager.requestNetwork(mNetworkRequest, mNetworkCallback, 5000);
				//++++++++++++++++

				Log.e(TAG, "connectionCreated Called ");
			}
		});
	}


	public void start(){
		mHandler.post(new Runnable () {
			@Override
			public void run() {
				if(mState == STATE_STARTED && mConnManager.bindProcessToNetwork(mCurrentNet)) {

					InetAddress peerIpv6 = mNetworkManager.getInetAddress(mCurrentNetCapabitities);
					int peerPort = mNetworkManager.getPort(mCurrentNetCapabitities);

					Log.d(TAG,"Connecting to RTSP server...");
					try {
						mSocket = mCurrentNet.getSocketFactory().createSocket(peerIpv6, peerPort);
						mBufferedReader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
						mOutputStream = new BufferedOutputStream(mSocket.getOutputStream());
						// If the user calls some methods to configure the client, it won't modify
						// its behavior until the stream is restarted

						mParameters = mTmpParameters.clone();
						mParameters.host = peerIpv6.getHostAddress();
						mParameters.port = peerPort;
						
						mConnManager.bindProcessToNetwork(null);
						if (mParameters.transport == TRANSPORT_UDP) {
							mHandler.post(mConnectionMonitor);
						}
						StreamingRecord.getInstance().addObserver(RtspClientWFA.this);
					} catch (IOException e) {
						postError(ERROR_CONNECTION_FAILED, e);

						// Start mete un ejecutable a la cola de un hilo. No es recursivo llamar start dentro de otro.
						//Para la versión sin WFA, en catch llama otra vez a start()
						onFailedStart();

					}
				}
			}
		});
	}

	private class WFANetworkCallback extends ConnectivityManager.NetworkCallback{

		public final static String TAG = "AwareNetworkCallback";

		@Override
		public void onAvailable(@NonNull Network network) {
			mCurrentNet = network;
			Log.e(TAG, "Network available");
		}

		@Override
		public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
			if(mCurrentNetCapabitities == null){
				Log.e(TAG, "onCapabilitiesChanged: capabiity 1");
				mCurrentNetCapabitities = networkCapabilities;
				mCurrentNet = network;
				start();
				Log.e(TAG, "start Called");
			}
			else {
				Log.e(TAG, "onCapabilitiesChanged: Red distinta o nueva capability");
				mCurrentNetCapabitities = networkCapabilities;

			}
		}

		@Override
		public void onLost(@NonNull Network network) {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					postError(ERROR_NETWORK_LOST, null);
					//No hace start, sino que limpia y cierra
					restartClient();
				}
			});
			Log.e(TAG, "Network lost");
		}

		@Override
		public void onUnavailable() {
			if(mTotalNetworkRequests++ > MAX_NETWORK_REQUESTS){
				Log.e(TAG, "Network Unavailable, connection failed");
				postError(ERROR_CONNECTION_FAILED, null);
				mNetworkCallback = null;
				restartClient();
			}
			else{
				Log.e(TAG, "Network Unavailable, requesting again");
				mConnManager.requestNetwork(mNetworkRequest, mNetworkCallback, 5000);
			}
		}
	}

	@Override
	protected void onFailedStart() {
		mConnManager.unregisterNetworkCallback(mNetworkCallback);
		mConnManager.requestNetwork(mNetworkRequest, mNetworkCallback, 5000);
	}


	@Override
	protected void clearClient(){
		super.clearClient();
		mCurrentNetCapabitities = null;
		if(mNetworkCallback != null) mConnManager.unregisterNetworkCallback(mNetworkCallback);
	}

}

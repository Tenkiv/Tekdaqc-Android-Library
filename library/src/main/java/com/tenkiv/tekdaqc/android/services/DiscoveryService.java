/**
 * Copyright 2013 Tenkiv, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tenkiv.tekdaqc.android.services;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.os.Process;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.tenkiv.tekdaqc.ATekdaqc;
import com.tenkiv.tekdaqc.android.application.TekCast;
import com.tenkiv.tekdaqc.locator.Locator;
import com.tenkiv.tekdaqc.locator.Locator.OnTekdaqcDiscovered;
import com.tenkiv.tekdaqc.locator.LocatorParams;

/**
 * Android {@link Service} which searches for Tekdaqcs and notifies the application when it finds them.
 *
 * @author Ian Thomas (toxicbakery@gmail.com)
 * @author Jared Woolston (jwoolston@tenkiv.com)
 * @since v1.0.0.0
 */
public class DiscoveryService extends Service implements OnTekdaqcDiscovered {

	private static final String TAG = "TelnetService"; // Logcat tag

	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;
    private LocalBroadcastManager mLocalBroadcastMgr;


	@Override
	public void onCreate() {
        super.onCreate();
		HandlerThread thread = new HandlerThread("TekDAQC Discovery Service", Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();

		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper, this);
        mLocalBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());

        Locator.setDebug(true);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
        // Extract the service instruction
        final String action = intent.getAction();

        // Build the message parameters
        Bundle extras = intent.getExtras();
        if (extras == null)
            extras = new Bundle();
        extras.putString(TekCast.EXTRA_SERVICE_ACTION, action);

        // Run each command in the background thread.
        final Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.setData(extras);
        mServiceHandler.sendMessage(msg);
		return START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		Log.v(TAG, "DiscoveryService is shutting down.");
	}

    @Override
    public void onDiscovery(ATekdaqc board) {
        final Intent intent = new Intent(TekCast.ACTION_FOUND_BOARD);
        intent.putExtra(TekCast.EXTRA_BOARD_SERIAL, board.getSerialNumber());
        mLocalBroadcastMgr.sendBroadcast(intent);
    }
    
    /**
	 * Actions which can be processed by the {@link DiscoveryService}.
	 * 
	 * @author Ian Thomas (toxicbakery@gmail.com)
	 * @author Jared Woolston (jwoolston@tenkiv.com)
	 * @since v1.0.0.0
	 */
	public static enum ServiceAction {
		/**
		 * Locate all Tekdaqc boards on a local network with the optionally provided {@link LocatorParams}.
		 */
		SEARCH
		
		/**
		 * Force shutdown of the {@link DiscoveryService}.
		 */
		, STOP;
	}

	/**
	 * Worker thread for handling incoming {@link DiscoveryService} {@link ServiceAction} requests.
     *
     * @author Ian Thomas (toxicbakery@gmail.com)
     * @author Jared Woolston (jwoolston@tenkiv.com)
     * @since v1.0.0.0
	 */
	private static final class ServiceHandler extends Handler {

		private DiscoveryService mService;

		public ServiceHandler(Looper looper, DiscoveryService service) {
			super(looper);
			mService = service;
		}

		@Override
		public void handleMessage(Message msg) {
			final Bundle data = msg.getData();
			final ServiceAction action = ServiceAction.valueOf(data.getString(TekCast.EXTRA_SERVICE_ACTION));

			switch (action) {
			case SEARCH:
                // Search for Tekdaqcs
                LocatorParams params = (LocatorParams) data.getSerializable(TekCast.EXTRA_LOCATOR_PARAMS);
                if (params == null) {
                    params = LocatorParams.getDefaultInstance();
                }
                Locator.searchForTekDAQCS(mService, params);
                break;
			case STOP:
                // Stop this service
				mService.stopSelf();
				break;
			}
		}
	}
}
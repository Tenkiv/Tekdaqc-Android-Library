package com.tenkiv.tekdaqc;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.os.Process;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class DiscoveryService extends Service implements Locator.OnATekDAQCDiscovered {

	private static final String TAG = "TelnetService";

	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;
    private LocalBroadcastManager mLocalBroadcastMgr;

    /**
	 * Processable actions by the {@link DiscoveryService}.
	 * 
	 * @author <a href=mailto:toxicbakery@gmail.com>Ian Thomas</a>
	 * 
	 */
	public static enum ServiceAction {
		/**
		 * Locate all TekDAQC boards on a local network with the optionally provided {@link LocatorParams}.
		 */
		SEARCH

		/**
		 * Force shutdown of the {@link TelnetService}.
		 */
		, STOP;
	}

	@Override
	public void onCreate() {
		HandlerThread thread = new HandlerThread("TekDAQC Discovery Service", Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();

		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper, this);
        mLocalBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());

        Locator.setDebug(true);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final String action = intent.getAction();
		Bundle extras = intent.getExtras();

		if (extras == null)
			extras = new Bundle();
		
		extras.putString(TekCast.EXTRA_SERVICE_ACTION, action);

		// Run each command in a separate thread.
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
		Log.v(TAG, "TelnetService is shutting down.");
	}

    @Override
    public void onDiscovery(ATekDAQC board) {
        final Intent intent = new Intent(TekCast.ACTION_FOUND_BOARD);
        intent.putExtra(TekCast.EXTRA_TEK_BOARD, board);
        mLocalBroadcastMgr.sendBroadcast(intent);
    }

	/**
	 * Worker thread for handling incoming {@link DiscoveryService} {@link ServiceAction} requests.
	 */
	private static final class ServiceHandler extends Handler {

		private DiscoveryService mService;

		public ServiceHandler(Looper looper, DiscoveryService service) {
			super(looper);
			mService = service;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg) {
			final Bundle data = msg.getData();
			final ServiceAction action = ServiceAction.valueOf(data.getString(TekCast.EXTRA_SERVICE_ACTION));

			switch (action) {
			case SEARCH:
                LocatorParams params = (LocatorParams) data.getSerializable(TekCast.EXTRA_LOCATOR_PARAMS);
                if (params == null) {
                    params = LocatorParams.getDefaultInstance();
                }
                Locator.searchForTekDAQCS(mService, params);
                break;
			case STOP:
				mService.stopSelf();
				return;
			}
		}
	}
}

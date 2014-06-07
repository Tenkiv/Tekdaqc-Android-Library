package com.tenkiv.tekdaqc;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import com.tenkiv.tekdaqc.Locator.OnATekDAQCDiscovered;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class CommunicationService extends Service {

	protected static final String TAG = CommunicationService.class.getSimpleName();

	protected static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(1);

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final LocatorParams locatorParams = (LocatorParams) intent.getSerializableExtra(TekCast.EXTRA_LOCATOR_PARAMS);

		try {
			EXECUTOR.submit(new Worker(this, locatorParams, startId));
		} catch (RejectedExecutionException e) {
			Log.w(TAG, "Locator request rejected; scan in progress.");
		}

		return Service.START_NOT_STICKY;
	}

	protected static class Worker implements Runnable, OnATekDAQCDiscovered {

		protected final CommunicationService mService;
		protected final LocatorParams mParams;
		protected final int mStartId;

		protected Worker(CommunicationService service, LocatorParams params, int startId) {
			mService = service;
			mParams = params;
			mStartId = startId;
		}

		@Override
		public void run() {
			// Search for boards on the network. Discovered boards will immediately callback to onDiscovery.
			if (!Locator.searchForTekDAQCS(this, mParams == null ? LocatorParams.getDefaultInstance() : mParams))
				Log.e(TAG, "Failed to start location request.");

			mService.stopSelf(mStartId);
		}

		@Override
		public void onDiscovery(ATekDAQC board) {
			final Intent boardsIntent = new Intent(TekCast.ACTION_FOUND_BOARD);
			boardsIntent.putExtra(TekCast.EXTRA_TEK_BOARD, board);
			mService.sendBroadcast(boardsIntent);
		}

	}

}

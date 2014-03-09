package com.tenkiv.tekdaqc;

import java.io.IOException;
import java.util.ArrayList;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import com.tenkiv.tekdaqc.Locator.OnATekDAQCDiscovered;
import com.tenkiv.tekdaqc.command.Command;
import com.tenkiv.tekdaqc.command.Parameter;
import com.tenkiv.tekdaqc.peripherals.analog.AAnalogInput;

public class DiscoveryService extends Service {

	private static final String TAG = "TelnetService";

	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;

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
		 * Send an {@link ArrayList} of {@link Parameter}s to the device.
		 */
		, COMMAND

		/**
		 * Force shutdown of the {@link TelnetService}.
		 */
		, STOP;
	}

	@Override
	public void onCreate() {
		HandlerThread thread = new HandlerThread("TekDAQC TelnetService", Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();

		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper, this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final String action = intent.getAction();
		Bundle extras = intent.getExtras();

		if (extras == null)
			extras = new Bundle();
		
		extras.putString(TekCast.EXTRA_SERVICE_ACTION, action);

		// Run each command in a separate thread. When all threads complete, the service will shutdown.
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
			case COMMAND:
				final ATekDAQC<? extends AAnalogInput> board = (ATekDAQC<? extends AAnalogInput>) data
						.getSerializable(TekCast.EXTRA_TEK_BOARD);
				if (board == null) {
					Log.e(TAG, "There was an error retrieving the selected TekDAQC.");
					return;
				}

				// Process the provided command and params
				try {
					final Command command = (Command) data.getSerializable(TekCast.EXTRA_SERVICE_COMMAND);
					final ArrayList<Parameter> params = (ArrayList<Parameter>) data
							.getSerializable(TekCast.EXTRA_SERVICE_PARAMS);

					if (command == null) {
						Log.e(TAG,
								"Ignoring " + ServiceAction.COMMAND + " request missing "
										+ Command.class.getSimpleName());
						return;
					}

					try {
						board.executeCommand(command, params);
					} catch (IOException e) {
						Log.e(TAG, "Error executing command:");
						e.printStackTrace();
					}

					return;
				} catch (ClassCastException e) {
					Log.e(TAG, "Failed to extract command or parameters from the message bundle.");
					return;
				}
			case STOP:
				mService.stopSelf();
				return;
			}
		}

	}

}

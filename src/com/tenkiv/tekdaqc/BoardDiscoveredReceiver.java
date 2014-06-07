package com.tenkiv.tekdaqc;

import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.tenkiv.tekdaqc.peripherals.analog.AAnalogInput;

/**
 * {@link BroadcastReceiver} for capturing search results with convenient callback {@link #onSearchResult(HashMap)}.
 * Register this receiver with the action {@link TekCast#ACTION_FOUND_BOARD}.
 * 
 * @author <a href=mailto:toxicbakery@gmail.com>Ian Thomas</a>
 * 
 */
public abstract class BoardDiscoveredReceiver extends BroadcastReceiver {

	private static final String TAG = "Search Complete Receiver";

	/**
	 * Callback for handling search finish events returning all discovered boards.
	 * 
	 * @param boards
	 */
	protected abstract void onSearchResult(ATekDAQC board);

	@SuppressWarnings("unchecked")
	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			onSearchResult((ATekDAQC) intent.getSerializableExtra(TekCast.EXTRA_TEK_BOARD));
		} catch (ClassCastException e) {
			Log.e(TAG, "Failed to extract map of boards from data bundle.");
			e.printStackTrace();
			return;
		}

	}
}

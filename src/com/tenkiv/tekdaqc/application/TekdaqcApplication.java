package com.tenkiv.tekdaqc.application;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.tenkiv.tekdaqc.ATekdaqc;
import com.tenkiv.tekdaqc.services.DiscoveryService;
import com.tenkiv.tekdaqc.locator.LocatorParams;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom application class which handles aspects of the library in an Android specific manner.
 * This primarily consists of device discovery.
 *  
 * @author Jared Woolston (jwoolston@tenkiv.com)
 * @since v1.0.0.0
 */
public class TekdaqcApplication extends Application {

	/**
	 * Logcat Tag
	 */
    private static final String TAG = "TekdaqcApplication";

    /**
     * List of known Tekdaqcs
     */
    private List<ATekdaqc> mBoards;
    
    /**
     * Broadcast receiver notified when a board is found by the locator
     */
    private DeviceDiscoveryReceiver mDiscoveryReceiver;
    
    /**
     * Local broadcast manager for sending events
     */
    private LocalBroadcastManager mLocalBroadcastMgr; 

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");
        mBoards = new ArrayList<ATekdaqc>();
        mDiscoveryReceiver = new DeviceDiscoveryReceiver(this);
        mLocalBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());
        mLocalBroadcastMgr.registerReceiver(mDiscoveryReceiver, new IntentFilter(TekCast.ACTION_FOUND_BOARD));
    }

    /**
     * Clears the known boards list and causes a new device locator packet to be sent. This
     * effectively means that only un-connected boards will known to this list. Boards which
     * were previously discovered and have an active connection will need to be tracked by 
     * the user application.
     */
    public final void refreshDeviceList() {
        mBoards.clear();
        final LocatorParams.Builder builder = new LocatorParams.Builder();
        final Intent intent = new Intent(getApplicationContext(), DiscoveryService.class);
        intent.setAction(DiscoveryService.ServiceAction.SEARCH.toString());
        intent.putExtra(TekCast.EXTRA_LOCATOR_PARAMS, builder.build());
        getApplicationContext().startService(intent);
    }

    /**
     * Broadcast receiver which will be called when the locator has discovered a board(s).
     * 
     * @author Jared Woolston (jwoolston@tenkiv.com)
     * @since v1.0.0.0
     */
    private static final class DeviceDiscoveryReceiver extends BroadcastReceiver {

    	/**
    	 * Reference to the owning application.
    	 */
        private final TekdaqcApplication mApplication;

        /**
         * Constructor.
         * 
         * @param app {@link TekdaqcApplication} The calling application.
         */
        DeviceDiscoveryReceiver(TekdaqcApplication app) {
            mApplication = app;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final ATekdaqc board = ATekdaqc.getTekdaqcForSerial(intent.getStringExtra(TekCast.EXTRA_BOARD_SERIAL));
            if (board != null) {
                mApplication.mBoards.add(board);
            }
        }
    }
}

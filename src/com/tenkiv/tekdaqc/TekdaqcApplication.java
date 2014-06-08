package com.tenkiv.tekdaqc;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.List;

/**
 * Created by ideal on 6/7/14.
 */
public class TekdaqcApplication extends Application {

    private static final String TAG = "TekdaqcApplication";

    private List<ATekDAQC> mBoards;
    private DeviceDiscoveryReceiver mDiscoveryReceiver;
    private LocalBroadcastManager mLocalBroadcastMgr;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");
        mDiscoveryReceiver = new DeviceDiscoveryReceiver(this);
        mLocalBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());
        mLocalBroadcastMgr.registerReceiver(mDiscoveryReceiver, new IntentFilter(TekCast.ACTION_FOUND_BOARD));
    }

    public final void refreshDeviceList() {
        mBoards.clear();
        final LocatorParams.Builder builder = new LocatorParams.Builder();

        final Intent intent = new Intent(getApplicationContext(), CommunicationService.class);
        intent.setAction(DiscoveryService.ServiceAction.SEARCH.toString());
        intent.putExtra(TekCast.EXTRA_LOCATOR_PARAMS, builder.build());
        getApplicationContext().startService(intent);
    }

    private static final class DeviceDiscoveryReceiver extends BroadcastReceiver {

        private final TekdaqcApplication mApplication;

        DeviceDiscoveryReceiver(TekdaqcApplication app) {
            mApplication = app;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final ATekDAQC board = (ATekDAQC) intent.getSerializableExtra(TekCast.EXTRA_TEK_BOARD);
            if (board != null) {
                mApplication.mBoards.add(board);
            }
        }
    }
}

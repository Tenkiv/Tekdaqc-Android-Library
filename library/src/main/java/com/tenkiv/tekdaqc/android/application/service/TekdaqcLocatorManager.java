package com.tenkiv.tekdaqc.android.application.service;

import android.app.ActivityManager;
import android.content.*;
import android.os.*;
import com.tenkiv.tekdaqc.ATekdaqc;
import com.tenkiv.tekdaqc.android.application.client.Tekdaqc;
import com.tenkiv.tekdaqc.android.application.util.IServiceListener;
import com.tenkiv.tekdaqc.android.application.util.TekCast;
import com.tenkiv.tekdaqc.locator.Locator;
import com.tenkiv.tekdaqc.locator.LocatorResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class TekdaqcLocatorManager implements ServiceConnection, IServiceListener{

    private static boolean hasReceivedData = false;

    private LocatorService.LocatorServiceBinder mServiceBinder;

    private static final int LOCATOR_CHECK_DELAY = 3500;

    private static List<Locator.OnTekdaqcDiscovered> mUserListeners;

    private static TekdaqcCommunicationManager mTekdaqcComManager;

    private static boolean mIsComManagerCreated = false;

    private Context mContext;

    private static Timer mLocatorWatchdog;

    private TimerTask mKeepAlive = new TimerTask() {
        @Override
        public void run() {
            if(!hasReceivedData){
                hasReceivedData = true;
                mServiceBinder.getService().startLocator();
            }else{
                hasReceivedData = false;
            }
        }
    };


    public TekdaqcLocatorManager(Context context, Locator.OnTekdaqcDiscovered listener){
        mUserListeners = new ArrayList<>();
        mUserListeners.add(listener);
        mContext = context;

        startLocatorService();

        TekdaqcCommunicationManager.startCommunicationService(context,this);

    }

    public TekdaqcLocatorManager(Context context){
        mUserListeners = new ArrayList<>();
        mContext = context;

        startLocatorService();

    }

    public void stopLocatorManager(){
        mContext.unbindService(this);
    }

    private void startLocatorService(){
        boolean isRunning = isLocatorServiceRunning(mContext);
        Intent locatorIntent = new Intent(mContext,LocatorService.class);

        mContext.bindService(locatorIntent, this, Context.BIND_AUTO_CREATE);

    }

    public static boolean isLocatorServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (LocatorService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void addLocatorListener(Locator.OnTekdaqcDiscovered listener){
        if(!mUserListeners.contains(listener)) {
            mUserListeners.add(listener);
        }
    }

    public void removeLocatorListener(Locator.OnTekdaqcDiscovered listener){
        if(mUserListeners.contains(listener)) {
            mUserListeners.remove(listener);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mServiceBinder = (LocatorService.LocatorServiceBinder) service;

        mLocatorWatchdog = new Timer();
        mLocatorWatchdog.scheduleAtFixedRate(mKeepAlive, LOCATOR_CHECK_DELAY, LOCATOR_CHECK_DELAY);

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mLocatorWatchdog.cancel();
    }

    @Override
    public void onManagerServiceCreated(TekdaqcCommunicationManager communicationManager) {
        mTekdaqcComManager = communicationManager;
        mIsComManagerCreated = true;
    }

    public static class LocatorReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {

            hasReceivedData = true;

            if(mUserListeners!=null && mIsComManagerCreated) {

                Bundle bundle = intent.getExtras();

                ATekdaqc tekdaqc = new Tekdaqc((LocatorResponse)intent.getExtras().get(TekCast.BROADCAST_TEKDAQC_RESPONSE), mTekdaqcComManager);

                boolean locatedByForeignApp = !(ATekdaqc.getActiveTekdaqcMap().containsKey(tekdaqc.getSerialNumber()));

                cullListeners();

                switch (bundle.getInt(TekCast.BROADCAST_CALL_TYPE)) {

                    case TekCast.LOCATOR_FIRST:
                        for(Locator.OnTekdaqcDiscovered listener: mUserListeners) {
                            listener.onTekdaqcFirstLocated(tekdaqc);
                        }
                        break;

                    case TekCast.LOCATOR_LOST:
                        for(Locator.OnTekdaqcDiscovered listener: mUserListeners) {
                            listener.onTekdaqcNoLongerLocated(tekdaqc);
                        }
                        break;

                    case TekCast.LOCATOR_RESPONSE:
                        if(locatedByForeignApp){
                            ATekdaqc.putTekdaqcInMap(tekdaqc);
                            for(Locator.OnTekdaqcDiscovered listener: mUserListeners) {
                                listener.onTekdaqcFirstLocated(tekdaqc);
                            }
                        }else {
                            for(Locator.OnTekdaqcDiscovered listener: mUserListeners) {
                                listener.onTekdaqcResponse(tekdaqc);
                            }
                        }
                        break;
                }
            }
        }

        private void cullListeners(){
            for(Locator.OnTekdaqcDiscovered listener: mUserListeners) {
                if(listener == null){
                    mUserListeners.remove(listener);
                }
            }
        }
    }
}

package com.tenkiv.tekdaqc.android.application.service;

import android.app.ActivityManager;
import android.app.Service;
import android.content.*;
import android.os.*;
import android.util.Log;
import com.tenkiv.tekdaqc.ATekdaqc;
import com.tenkiv.tekdaqc.android.application.util.TekdaqcHandlerCall;
import com.tenkiv.tekdaqc.locator.Locator;
import com.tenkiv.tekdaqc.locator.LocatorParams;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Ellis Berry (ejberry@tenkiv.com)
 */
public class TekdaqcLocatorManager implements ServiceConnection{

    private static final String BROADCAST_URI = "com.tenkiv.tekdaqc.LOCATOR";
    private static final String BROADCAST_TEKDAQC = "LOCATED_TEKDAQC";
    private static final String BROADCAST_CALL_TYPE = "LOCATED_CALL_TYPE";
    private static final int LOCATOR_RESPONSE = 0;
    private static final int LOCATOR_FIRST = 1;
    private static final int LOCATOR_LOST = 2;

    private static boolean hasReceivedData = false;
    private LocatorService.LocatorServiceBinder mServiceBinder;

    private static final long DEFAULT_DELAY = 0;
    private static final long DEFAULT_PERIOD = 3000;

    private static final int LOCATOR_CHECK_DELAY = 3500;

    private static List<Locator.OnTekdaqcDiscovered> mUserListeners;


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

    }

    public static class LocatorReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {

            Bundle bundle = intent.getExtras();

            ATekdaqc tekdaqc = (ATekdaqc)intent.getExtras().get(BROADCAST_TEKDAQC);

            boolean locatedByForeignApp = !(ATekdaqc.getActiveTekdaqcMap().containsKey(tekdaqc.getSerialNumber()));

            hasReceivedData = true;

            if(mUserListeners!=null) {

                cullListeners();

                switch (bundle.getInt(BROADCAST_CALL_TYPE)) {

                    case LOCATOR_FIRST:
                        for(Locator.OnTekdaqcDiscovered listener: mUserListeners) {
                            listener.onTekdaqcFirstLocated(tekdaqc);
                        }
                        break;

                    case LOCATOR_LOST:
                        for(Locator.OnTekdaqcDiscovered listener: mUserListeners) {
                            listener.onTekdaqcNoLongerLocated(tekdaqc);
                        }
                        break;

                    case LOCATOR_RESPONSE:
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


    public static class LocatorService extends Service implements Locator.OnTekdaqcDiscovered{

        private final IBinder mLocatorBinder = new LocatorServiceBinder();
        private Locator mLocator;
        private boolean isLocatorRunning;

        public LocatorService() {
            super();
        }

        private void haltLocator(){
            mLocator.cancelLocator();
        }

        public void startLocator(){
            try {
                if (!isLocatorRunning) {
                    isLocatorRunning = true;
                    mLocator = new Locator(null, this);
                    mLocator.searchForTekDAQCS(
                            DEFAULT_DELAY,
                            DEFAULT_PERIOD);
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }


        @Override
        public void onDestroy() {
            super.onDestroy();
            if(isLocatorRunning){
                haltLocator();
                mLocatorWatchdog.cancel();
            }
        }


        @Override
        public boolean onUnbind(Intent intent) {
            return super.onUnbind(intent);
        }


        @Override
        public IBinder onBind(Intent intent) {
            return mLocatorBinder;
        }

        @Override
        public void onTekdaqcResponse(ATekdaqc board) {
            Intent broadcast = new Intent(BROADCAST_URI);
            broadcast.putExtra(BROADCAST_TEKDAQC, board);
            broadcast.putExtra(BROADCAST_CALL_TYPE,LOCATOR_RESPONSE);
            sendBroadcast(broadcast);
        }

        @Override
        public void onTekdaqcFirstLocated(ATekdaqc board) {
            Intent broadcast = new Intent(BROADCAST_URI);
            broadcast.putExtra(BROADCAST_TEKDAQC, board);
            broadcast.putExtra(BROADCAST_CALL_TYPE,LOCATOR_FIRST);
            sendBroadcast(broadcast);

        }

        @Override
        public void onTekdaqcNoLongerLocated(ATekdaqc board) {
            Intent broadcast = new Intent(BROADCAST_URI);
            broadcast.putExtra(BROADCAST_TEKDAQC, board);
            broadcast.putExtra(BROADCAST_CALL_TYPE,LOCATOR_LOST);
            sendBroadcast(broadcast);

        }


        public class LocatorServiceBinder extends Binder {

            public LocatorService getService(){
                return LocatorService.this;
            }
        }


    }


}

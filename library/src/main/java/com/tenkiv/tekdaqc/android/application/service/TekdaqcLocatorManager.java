package com.tenkiv.tekdaqc.android.application.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import com.tenkiv.tekdaqc.ATekdaqc;
import com.tenkiv.tekdaqc.android.application.util.TekdaqcHandlerCall;
import com.tenkiv.tekdaqc.locator.Locator;
import com.tenkiv.tekdaqc.locator.LocatorParams;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Ellis Berry (ejberry@tenkiv.com)
 */
public class TekdaqcLocatorManager implements ServiceConnection, Locator.OnTekdaqcDiscovered{

    private static final String MILLISECONDS_DELAY = "milliseconds_delay";
    private static final String MILLISECONDS_PERIOD = "milliseconds_period";

    private static final long DEFAULT_DELAY = 0;
    private static final long DEFAULT_PERIOD = 10000;

    private static Locator.OnTekdaqcDiscovered mUserListener;

    private static Locator.OnTekdaqcDiscovered mThreadListener;

    private static LocatorParams mParams;

    private LocatorService.LocatorServiceBinder mServiceBinder;

    private Handler mHandler;

    private Context mContext;


    public TekdaqcLocatorManager(Context context, Locator.OnTekdaqcDiscovered listener, LocatorParams params){
        mUserListener = listener;
        mThreadListener = this;
        mContext = context;
        mParams = params;

        mHandler = new Handler(context.getMainLooper());

    }

    public synchronized void startLocatorService(long delay, long period){
        Intent locatorIntent = new Intent(mContext,LocatorService.class);

        locatorIntent.putExtra(MILLISECONDS_DELAY,delay);
        locatorIntent.putExtra(MILLISECONDS_PERIOD,period);

        mContext.bindService(locatorIntent, this, Context.BIND_ALLOW_OOM_MANAGEMENT);
        mContext.startService(locatorIntent);

    }

    public synchronized void stopLocatorService(){
        mServiceBinder.getService().haltLocator();
        mServiceBinder.getService().stopSelf();

    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mServiceBinder = (LocatorService.LocatorServiceBinder) service;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    @Override
    public void onTekdaqcResponse(ATekdaqc board) {
        mHandler.post(new TekdaqcLocatedHandlerRunnable(board, mUserListener, TekdaqcHandlerCall.REPOSNE));
    }

    @Override
    public void onTekdaqcFirstLocated(ATekdaqc board) {
        mHandler.post(new TekdaqcLocatedHandlerRunnable(board, mUserListener, TekdaqcHandlerCall.ADDED));
    }

    @Override
    public void onTekdaqcNoLongerLocated(ATekdaqc board) {
        mHandler.post(new TekdaqcLocatedHandlerRunnable(board, mUserListener, TekdaqcHandlerCall.REMOVED));
    }



    public static class LocatorService extends Service{

        private final IBinder mLocatorBinder = new LocatorServiceBinder();
        private Locator mLocator;
        public LocatorService() {
            super();
        }

        public void haltLocator(){
            mLocator.cancelLocator();
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            try {
                if (intent != null) {
                    mLocator = new Locator(mParams, mThreadListener);
                    mLocator.searchForTekDAQCS(
                            intent.getLongExtra(MILLISECONDS_DELAY, DEFAULT_DELAY),
                            intent.getLongExtra(MILLISECONDS_PERIOD,DEFAULT_PERIOD));
                }
            }catch(Exception e){
                e.printStackTrace();
            }

            return super.onStartCommand(intent, flags, startId);
        }

        @Override
        public IBinder onBind(Intent intent) {
            return mLocatorBinder;
        }




        public class LocatorServiceBinder extends Binder {

            public LocatorService getService(){
                return LocatorService.this;
            }
        }


    }


}

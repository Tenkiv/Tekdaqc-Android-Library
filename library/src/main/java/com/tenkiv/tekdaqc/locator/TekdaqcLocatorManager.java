package com.tenkiv.tekdaqc.locator;

import android.app.ActivityManager;
import android.app.Service;
import android.content.*;
import android.os.*;
import com.tenkiv.tekdaqc.android.application.client.Tekdaqc;
import com.tenkiv.tekdaqc.android.application.client.TekdaqcCommunicationManager;
import com.tenkiv.tekdaqc.android.application.service.CommunicationService;
import com.tenkiv.tekdaqc.android.application.service.LocatorService;
import com.tenkiv.tekdaqc.android.application.util.IServiceListener;
import com.tenkiv.tekdaqc.android.application.util.TekCast;
import com.tenkiv.tekdaqc.hardware.ATekdaqc;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Class which handles the client side communication to the {@link LocatorService} as well as initiates the {@link CommunicationService} to ensure that the
 * {@link Service} is running when a connection attempt is made.}
 *
 * @author Tenkiv (software@tenkiv.com)
 * @since v2.0.0.0
 */
public class TekdaqcLocatorManager implements ServiceConnection, IServiceListener{

    /**
     * Flag for if the {@link TekdaqcLocatorManager} has received a broadcast from the {@link LocatorService}.
     */
    private static boolean hasReceivedData = false;

    /**
     * A simple {@link Binder} for basic communication between the app and the {@link LocatorService}.
     */
    private LocatorService.LocatorServiceBinder mServiceBinder;

    /**
     * Default period of the watchdog to check to see if the {@link LocatorService} is still functioning properly.
     */
    private static final int LOCATOR_CHECK_DELAY = 3500;

    /**
     * The {@link List} of all currently registered {@link OnTekdaqcDiscovered} listeners.
     */
    private static List<OnTekdaqcDiscovered> mUserListeners;

    /**
     * The {@link TekdaqcCommunicationManager} which needs to be started in order to facilitate communication with the remote {@link Service}.
     */
    private static TekdaqcCommunicationManager mTekdaqcComManager;

    /**
     * Flag for if the {@link TekdaqcCommunicationManager} has been started.
     */
    private static boolean mIsComManagerCreated = false;

    /**
     * The context.
     */
    private Context mContext;

    /**
     * A {@link Timer} run at regular intervals to check to see if the {@link LocatorService} is still functioing properly.
     */
    private static Timer mLocatorWatchdog;

    /**
     * The watchdog {@link TimerTask} run by {@link TekdaqcLocatorManager#mLocatorWatchdog}.
     */
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


    public TekdaqcLocatorManager(Context context, OnTekdaqcDiscovered listener){
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

        TekdaqcCommunicationManager.startCommunicationService(context,this);

    }

    /**
     * Unbinds from the {@link LocatorService}.
     */
    public void stopLocatorManager(){
        mContext.unbindService(this);
    }

    /**
     * Starts and binds to the {@link LocatorService}.
     */
    private void startLocatorService(){
        boolean isRunning = isLocatorServiceRunning(mContext);
        Intent locatorIntent = new Intent(mContext,LocatorService.class);

        mContext.bindService(locatorIntent, this, Context.BIND_AUTO_CREATE);

    }

    /**
     * Utility method to check if the {@link LocatorService} is running.
     *
     * @param context The application context.
     * @return The status of if the {@link LocatorService}  is running.
     */
    public static boolean isLocatorServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (LocatorService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method to add a {@link OnTekdaqcDiscovered} interface to relieve callbacks.
     * @param listener Listener to be added.
     */
    public void addLocatorListener(OnTekdaqcDiscovered listener){
        if(!mUserListeners.contains(listener)) {
            mUserListeners.add(listener);
        }
    }

    /**
     * Method to remove a {@link OnTekdaqcDiscovered} from receiving.
     * @param listener Listener to be removed.
     */
    public void removeLocatorListener(OnTekdaqcDiscovered listener){
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

    /**
     * A class which receives broadcasts from the {@link LocatorService} and then notifies all added {@link OnTekdaqcDiscovered} listeners in {@link TekdaqcLocatorManager}.
     */
    public static class LocatorReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {

            hasReceivedData = true;

            if(mUserListeners!=null && mIsComManagerCreated) {

                Bundle bundle = intent.getExtras();

                ATekdaqc tekdaqc = new Tekdaqc((LocatorResponse)intent.getExtras().get(TekCast.BROADCAST_TEKDAQC_RESPONSE), mTekdaqcComManager);

                boolean locatedByForeignApp = !(Locator.getActiveTekdaqcMap().containsKey(tekdaqc.getSerialNumber()));

                cullListeners();

                switch (bundle.getInt(TekCast.BROADCAST_CALL_TYPE)) {

                    case TekCast.LOCATOR_FIRST:
                        Locator.addTekdaqctoMap(tekdaqc);
                        for(OnTekdaqcDiscovered listener: mUserListeners) {
                            listener.onTekdaqcFirstLocated(tekdaqc);
                        }
                        break;

                    case TekCast.LOCATOR_LOST:
                        Locator.removeTekdaqcForSerial(tekdaqc.getSerialNumber());
                        for(OnTekdaqcDiscovered listener: mUserListeners) {
                            listener.onTekdaqcNoLongerLocated(tekdaqc);
                        }
                        break;

                    case TekCast.LOCATOR_RESPONSE:
                        if(locatedByForeignApp){
                            for(OnTekdaqcDiscovered listener: mUserListeners) {
                                listener.onTekdaqcFirstLocated(tekdaqc);
                            }
                        }else {
                            for(OnTekdaqcDiscovered listener: mUserListeners) {
                                listener.onTekdaqcResponse(tekdaqc);
                            }
                        }
                        break;
                }
            }
        }

        private void cullListeners(){
            for(OnTekdaqcDiscovered listener: mUserListeners) {
                if(listener == null){
                    mUserListeners.remove(listener);
                }
            }
        }
    }
}

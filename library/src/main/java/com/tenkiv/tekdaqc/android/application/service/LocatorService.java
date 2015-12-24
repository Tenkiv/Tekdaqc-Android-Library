package com.tenkiv.tekdaqc.android.application.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import com.tenkiv.tekdaqc.ATekdaqc;
import com.tenkiv.tekdaqc.android.application.util.TekCast;
import com.tenkiv.tekdaqc.locator.Locator;

/**
 * Class which handles the UDP broadcasts for Tekdaqc location. It then broacasts the discovered Tekdaqcs so they can be received by the {@link TekdaqcLocatorManager}.
 *
 * @author Ellis Berry (ejberry@tenkiv.com)
 * @since v2.0.0.0
 */
public  class LocatorService extends Service implements Locator.OnTekdaqcDiscovered{

    /**
     * Binder for simple IPC.
     */
    private final IBinder mLocatorBinder = new LocatorServiceBinder();

    /**
     * The basic tekdaqc {@link Locator}.
     */
    private Locator mLocator;

    /**
     * Flag for if the locator is running.
     */
    private boolean isLocatorRunning;

    /**
     * Empty constructor for remote {@link Service} instantiation.
     */
    public LocatorService() {
        super();
    }

    /**
     * The {@link Service} side method for halting the {@link Locator}.
     */
    private void haltLocator(){
        mLocator.cancelLocator();
    }

    /**
     * The {@link Service} side method for starting the {@link Locator}.
     */
    public void startLocator(){
        try {
            if (!isLocatorRunning) {
                isLocatorRunning = true;
                mLocator = new Locator(null, this);
                mLocator.searchForTekDAQCS(
                        TekCast.DEFAULT_DELAY,
                        TekCast.DEFAULT_PERIOD);
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
        Intent broadcast = new Intent(TekCast.BROADCAST_URI);
        broadcast.putExtra(TekCast.BROADCAST_TEKDAQC_RESPONSE, board.getLocatorResponse());
        broadcast.putExtra(TekCast.BROADCAST_CALL_TYPE,TekCast.LOCATOR_RESPONSE);
        sendBroadcast(broadcast);
    }

    @Override
    public void onTekdaqcFirstLocated(ATekdaqc board) {
        Intent broadcast = new Intent(TekCast.BROADCAST_URI);
        broadcast.putExtra(TekCast.BROADCAST_TEKDAQC_RESPONSE, board.getLocatorResponse());
        broadcast.putExtra(TekCast.BROADCAST_CALL_TYPE,TekCast.LOCATOR_FIRST);
        sendBroadcast(broadcast);

    }

    @Override
    public void onTekdaqcNoLongerLocated(ATekdaqc board) {
        Intent broadcast = new Intent(TekCast.BROADCAST_URI);
        broadcast.putExtra(TekCast.BROADCAST_TEKDAQC_RESPONSE, board.getLocatorResponse());
        broadcast.putExtra(TekCast.BROADCAST_CALL_TYPE,TekCast.LOCATOR_LOST);
        sendBroadcast(broadcast);

    }

    /**
     * Inner class which acts as a simple binder for the {@link LocatorService}.
     */
    public class LocatorServiceBinder extends Binder {

        public LocatorService getService(){
            return LocatorService.this;
        }
    }


}

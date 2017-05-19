package com.tenkiv.tekdaqc.android.application.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import com.tenkiv.tekdaqc.locator.TekdaqcLocatorManager;
import com.tenkiv.tekdaqc.hardware.ATekdaqc;
import com.tenkiv.tekdaqc.locator.Locator;
import com.tenkiv.tekdaqc.locator.OnTekdaqcDiscovered;
import static com.tenkiv.tekdaqc.android.application.util.UtilKt.*;

/**
 * Class which handles the UDP broadcasts for Tekdaqc location. It then broadcasts the discovered Tekdaqcs so they can be received by the {@link TekdaqcLocatorManager}.
 *
 * @author Tenkiv (software@tenkiv.com)
 * @since v2.0.0.0
 */
public  class LocatorService extends Service implements OnTekdaqcDiscovered {

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
                mLocator = Locator.Companion.getInstance();
                mLocator.searchForTekdaqcs();
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
        broadcastLocatorResponse(board,LOCATOR_RESPONSE);
    }

    @Override
    public void onTekdaqcFirstLocated(ATekdaqc board) {
        broadcastLocatorResponse(board,LOCATOR_FIRST);

    }

    @Override
    public void onTekdaqcNoLongerLocated(ATekdaqc board) {
        broadcastLocatorResponse(board,LOCATOR_LOST);

    }

    private void broadcastLocatorResponse(ATekdaqc tekdaqc, int callType){
        if(tekdaqc == null){return;}
        Intent broadcast = new Intent(BROADCAST_URI);
        broadcast.putExtra(BROADCAST_TEKDAQC_RESPONSE, tekdaqc.getLocatorResponse());
        broadcast.putExtra(BROADCAST_CALL_TYPE,callType);
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

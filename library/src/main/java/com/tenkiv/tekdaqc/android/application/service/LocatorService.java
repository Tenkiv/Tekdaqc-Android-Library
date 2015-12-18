package com.tenkiv.tekdaqc.android.application.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import com.tenkiv.tekdaqc.ATekdaqc;
import com.tenkiv.tekdaqc.android.application.util.TekCast;
import com.tenkiv.tekdaqc.locator.Locator;


public  class LocatorService extends Service implements Locator.OnTekdaqcDiscovered{

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


    public class LocatorServiceBinder extends Binder {

        public LocatorService getService(){
            return LocatorService.this;
        }
    }


}

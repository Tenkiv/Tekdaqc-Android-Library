package com.tenkiv.tekdaqc.android.application.service;

import com.tenkiv.tekdaqc.ATekdaqc;
import com.tenkiv.tekdaqc.android.application.util.TekdaqcHandlerCall;
import com.tenkiv.tekdaqc.locator.Locator;

/**
 * Created by Ellis Berry (ejberry@tenkiv.com)
 */
public class TekdaqcLocatedHandlerRunnable implements Runnable{

    ATekdaqc mTekdaqc;

    Locator.OnTekdaqcDiscovered mListener;

    TekdaqcHandlerCall mCallType;

    protected TekdaqcLocatedHandlerRunnable(ATekdaqc tekdaqc, Locator.OnTekdaqcDiscovered listener, TekdaqcHandlerCall callType){
        mTekdaqc = tekdaqc;
        mListener = listener;
        mCallType = callType;
    }

    @Override
    public void run() {

        switch(mCallType){
            case REPOSNE:
                mListener.onTekdaqcResponse(mTekdaqc);
                break;
            case ADDED:
                mListener.onTekdaqcFirstLocated(mTekdaqc);
                break;
            case REMOVED:
                mListener.onTekdaqcNoLongerLocated(mTekdaqc);
                break;
        }
    }
}

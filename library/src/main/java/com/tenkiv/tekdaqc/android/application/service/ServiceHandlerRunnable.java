package com.tenkiv.tekdaqc.android.application.service;

import com.tenkiv.tekdaqc.android.application.util.IServiceListener;

/**
 * Created by ejberry on 4/29/15.
 */
public class ServiceHandlerRunnable implements Runnable {

    IServiceListener mListener;

    TekdaqcCommunicationManager mManager;

    protected ServiceHandlerRunnable(IServiceListener listener, TekdaqcCommunicationManager manager){
        mListener = listener;
        mManager = manager;
    }

    @Override
    public void run() {
        mListener.onManagerServiceCreated(mManager);
    }
}

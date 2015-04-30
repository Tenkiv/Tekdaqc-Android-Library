package com.tenkiv.tekdaqc.android.application.service;

import android.os.Handler;
import com.tenkiv.tekdaqc.ATekdaqc;
import com.tenkiv.tekdaqc.android.application.util.ICommunicationListener;
import com.tenkiv.tekdaqc.android.application.util.TekdaqcHandlerCall;
import com.tenkiv.tekdaqc.communication.ascii.ASCIICommunicationSession;

import java.io.IOException;

/**
 * Created by ejberry on 4/18/15.
 */
public class TekdaqcConnectionThread extends Thread{

    private ASCIICommunicationSession mSession;
    private Handler mHandler;
    private ICommunicationListener mUserListener;

    protected TekdaqcConnectionThread(ASCIICommunicationSession session, Handler handler, ICommunicationListener userListener){
        mSession = session;
        mHandler = handler;
        mUserListener = userListener;


    }

    @Override
    public void run() {
        super.run();

        try {
            mSession.connect(ATekdaqc.CONNECTION_METHOD.ETHERNET);
            mHandler.post(new TekdaqcDataHandlerRunnable(mSession.getTekdaqc(), mSession.getTekdaqc(), mUserListener, TekdaqcHandlerCall.CONNECTED));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

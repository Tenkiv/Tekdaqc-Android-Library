package com.tenkiv.tekdaqc.android.application.util;

import android.os.Messenger;
import android.util.Log;
import com.tenkiv.tekdaqc.ATekdaqc;
import com.tenkiv.tekdaqc.communication.message.IMessageListener;
import com.tenkiv.tekdaqc.communication.message.MessageBroadcaster;
import com.tenkiv.tekdaqc.revd.Tekdaqc_RevD;

import java.io.IOException;

/**
 * Created by ejberry on 12/7/15.
 */
public class ServiceConnectionThread extends Thread {

    /*private Messenger mMessenger;*/
    private ATekdaqc mTekdaqc;
    /*private IMessageListener mMessageListener;*/

    public ServiceConnectionThread(/*Messenger messenger, */ATekdaqc tekdaqc/*, IMessageListener listener*/){
        /*mMessageListener = listener;*/
        /*mMessenger = messenger;*/
        mTekdaqc = tekdaqc;
    }

    @Override
    public void run() {
        super.run();

      /*  MessageBroadcaster broadcaster = MessageBroadcaster.getInstance();
        broadcaster.registerMessageListener(mTekdaqc, mMessageListener);*/

        try {
            Log.d("ServConThread","PreCon");
            mTekdaqc.connect(ATekdaqc.CONNECTION_METHOD.ETHERNET);
            Log.d("ServConThread","PostCon");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

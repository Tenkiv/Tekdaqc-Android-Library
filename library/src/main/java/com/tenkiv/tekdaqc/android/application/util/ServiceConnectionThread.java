package com.tenkiv.tekdaqc.android.application.util;

import android.os.Messenger;
import android.util.Log;
import com.tenkiv.tekdaqc.ATekdaqc;
import com.tenkiv.tekdaqc.android.application.service.CommunicationService;
import com.tenkiv.tekdaqc.communication.message.IMessageListener;
import com.tenkiv.tekdaqc.communication.message.MessageBroadcaster;
import com.tenkiv.tekdaqc.revd.Tekdaqc_RevD;

import java.io.IOException;

/**
 * Class to encapsulate the creation of the therad to establish a telnet connection with a tekdaqc.
 *
 * @author Tenkiv (software@tenkiv.com)
 * @since v2.0.0.0
 */
public class ServiceConnectionThread extends Thread {

    /**
     * Tekdaqc to bind to.
     */
    private ATekdaqc mTekdaqc;

    /**
     * Constructor which determines which {@link ATekdaqc} to connect to.
     * @param tekdaqc The {@link ATekdaqc} to connect to.
     */
    public ServiceConnectionThread(ATekdaqc tekdaqc){
        mTekdaqc = tekdaqc;
    }

    @Override
    public void run() {
        try {
            mTekdaqc.connect(ATekdaqc.CONNECTION_METHOD.ETHERNET);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

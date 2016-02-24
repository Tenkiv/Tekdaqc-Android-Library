package com.tenkiv.tekdaqc.android.application.util;



import com.tenkiv.tekdaqc.hardware.ATekdaqc;

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

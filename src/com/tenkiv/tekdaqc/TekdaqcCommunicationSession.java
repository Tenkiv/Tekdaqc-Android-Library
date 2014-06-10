package com.tenkiv.tekdaqc;

import android.util.Log;
import com.tenkiv.tekdaqc.telnet.client.EthernetTelnetConnection;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Created by ideal on 6/9/14.
 */
public final class TekdaqcCommunicationSession {

    private static final String TAG = "TekdaqcCommunicationSession";
    private final ATekDAQC mTekdaqc;

    public TekdaqcCommunicationSession(ATekDAQC tekdaqc) {
        mTekdaqc = tekdaqc;
    }

    public void connect() throws IOException {
        Log.d(TAG, "Connecting to host IP: " + mTekdaqc.getHostIP() + " on port: " + EthernetTelnetConnection.TEKDAQC_TELNET_PORT);
        mTekdaqc.connect(ATekDAQC.CONNECTION_METHOD.ETHERNET);
    }

    public void disconnect() throws IOException {
        mTekdaqc.disconnect();
    }

    public BufferedReader getBufferedReader() {
       return mTekdaqc.getReader();
    }
}

package com.tenkiv.tekdaqc;

import android.util.Log;
import com.tenkiv.tekdaqc.parsing.ASCIIParser;
import com.tenkiv.tekdaqc.telnet.client.EthernetTelnetConnection;
import com.tenkiv.tekdaqc.utility.Hexdump;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Created by ideal on 6/9/14.
 */
public final class TekdaqcCommunicationSession {

    private static final String TAG = "TekdaqcCommunicationSession";
    private final ATekDAQC mTekdaqc;

    private StringBuilder mMessageBuilder;

    public TekdaqcCommunicationSession(ATekDAQC tekdaqc) {
        mTekdaqc = tekdaqc;
        mMessageBuilder = new StringBuilder();
    }

    public void connect() throws IOException {
        Log.d(TAG, "Connecting to host IP: " + mTekdaqc.getHostIP() + " on port: " + EthernetTelnetConnection.TEKDAQC_TELNET_PORT);
        mTekdaqc.connect(ATekDAQC.CONNECTION_METHOD.ETHERNET);
    }

    public void disconnect() throws IOException {
        mTekdaqc.disconnect();
    }

    public BufferedReader getBufferedReader() {
       return mTekdaqc.getInputReader();
    }

    /**
     * Appends the provided message data to the sessions message builder. When a complete message is detected, it will be
     * output and the builder reset. This method is not thread safe and must only be called from a single feeder.
     *
     * @param data {@link String} The message fragment to append to the message builder.
     * @return {@link String} containing the full message, or null if the message has not been completed.
     */
    public String appendMessageData(String data) {
        final String[] parts = ASCIIParser.splitOnRecordSeparator(data);
        final String before = parts[0];
        final String after = (parts.length > 1) ? parts[1] : null;
        Log.d("APPEND", "Appending message data: " + Hexdump.hexdump(data.getBytes()));
        mMessageBuilder.append(before); // Add to the end of the message
        if (after != null) {
            // We have completed a message
            final String retval = mMessageBuilder.toString(); // Retrieve the completed message
            mMessageBuilder = new StringBuilder(after);
            return retval;
        } else {
            // We have not completed a message
            return null;
        }
    }
}

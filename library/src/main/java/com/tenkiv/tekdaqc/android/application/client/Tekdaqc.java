package com.tenkiv.tekdaqc.android.application.client;

import com.tenkiv.tekdaqc.communication.command.queue.ICommandManager;
import com.tenkiv.tekdaqc.communication.message.MessageBroadcaster;
import com.tenkiv.tekdaqc.hardware.Tekdaqc_RevD;
import com.tenkiv.tekdaqc.locator.LocatorResponse;

import java.io.IOException;

/**
 * Class which wraps the regular {@link Tekdaqc_RevD} class to override the base {@link ICommandManager}. This allows for the smallest amount of code to be
 * rewritten as well as providing a similar interface for library users across Android and Java.
 *
 * @author Tenkiv (software@tenkiv.com)
 * @since v2.0.0.0
 */
public class Tekdaqc extends Tekdaqc_RevD {

    /**
     * The manager to forward connection calls.
     */
    private TekdaqcCommunicationManager mManager;

    /**
     * Boolean for connection status.
     */
    private boolean isConnected = true;

    /**
     * Constructor which overrides the default {@link ICommandManager}
     *
     * @param response Response required to create any tekdaqc object.
     * @param manager The {@link TekdaqcCommunicationManager} which handles connection.
     */
    public Tekdaqc(LocatorResponse response, TekdaqcCommunicationManager manager){
        super(response);

        mManager = manager;

        mCommandQueue = new ClientCommandManager(mResponse.getSerial(),mManager);

    }

    /**
     * Empty constructor used for serialization.
     */
    public Tekdaqc(){
        super();
    }

    @Override
    public void connect(CONNECTION_METHOD method) throws IOException {
        isConnected = true;
        mManager.connectToTekdaqc(this);
    }

    @Override
    public void disconnect() throws IOException {
        isConnected = false;
        mManager.disconnectFromTekdaqc(this);
    }

    @Override
    public void disconnectCleanly() {
        mManager.disconnectFromTekdaqc(this);
    }

    public void ceaseCommunication(){
        mManager.stopCommunicationManager();
    }

    @Override
    public boolean isConnected() {
        return isConnected;
    }

    protected static MessageBroadcaster getMessageBroadcaster(){
        return messageBroadcaster;
    }
}

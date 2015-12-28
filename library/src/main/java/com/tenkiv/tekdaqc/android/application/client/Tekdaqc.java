package com.tenkiv.tekdaqc.android.application.client;

import com.tenkiv.tekdaqc.android.application.service.TekdaqcCommunicationManager;
import com.tenkiv.tekdaqc.communication.command.queue.ICommandManager;
import com.tenkiv.tekdaqc.locator.LocatorResponse;
import com.tenkiv.tekdaqc.revd.Tekdaqc_RevD;

import java.io.IOException;

/**
 * Class which wraps the regular {@link Tekdaqc_RevD} class to override the base {@link ICommandManager}. This allows for the smallest ammount of code to be
 * rewritten as well as providing a similar interface for library users across Android and Java.
 *
 * @author Ellis Berry (ejberry@tenkiv.com)
 * @since v2.0.0.0
 */
public class Tekdaqc extends Tekdaqc_RevD {

    /**
     * The manager to forward connection calls.
     */
    private TekdaqcCommunicationManager mManager;

    /**
     * Constructor which overrides the default {@link ICommandManager}
     * @param response Response required to create any tekdaqc object.
     * @param manager The {@link TekdaqcCommunicationManager} which handles connection.
     */
    public Tekdaqc(LocatorResponse response, TekdaqcCommunicationManager manager){
        super(response);

        mManager = manager;

        mCommandQueue = new ClientCommandManager(mResponse.getSerial(),mManager);

    }

    @Override
    public void connect(CONNECTION_METHOD method) throws IOException {
        mManager.connectToTekdaqc(this);
    }

    @Override
    public void disconnect() throws IOException {
        mManager.disconnectFromTekdaqc(this);
    }
}

package com.tenkiv.tekdaqc.android.application.client;

import android.app.Service;
import com.tenkiv.tekdaqc.android.application.service.CommunicationService;
import com.tenkiv.tekdaqc.android.application.service.TekdaqcCommunicationManager;
import com.tenkiv.tekdaqc.communication.command.queue.ICommandManager;
import com.tenkiv.tekdaqc.communication.command.queue.IQueueObject;
import com.tenkiv.tekdaqc.communication.command.queue.Task;

/**
 * Implementation of {@link ICommandManager} which reroutes all attempts to execute commands and tasks to the {@link TekdaqcCommunicationManager}.
 * This ensures that all network operations and communication occurs in a remote {@link Service} while maintaining a similar use pattern and reducing
 * code redundancies between the Android and Java libraries.
 *
 * @author Ellis Berry (ejberry@tenkiv.com)
 * @since v2.0.0.0
 */
public class ClientCommandManager implements ICommandManager {

    /**
     * The client side manager of the {@link CommunicationService} used in command execution.
     */
    private TekdaqcCommunicationManager mServiceManager;

    /**
     * Serial number of tekdaqc we are communicating to.
     */
    private String mSerial;

    /**
     * Constructor which specifies which {@link Tekdaqc} this {@link ClientCommandManager} represents as well as the {@link TekdaqcCommunicationManager} to be used.
     *
     * @param serial The {@link String} of the serial number of the {@link Tekdaqc} to be used.
     * @param manager The {@link TekdaqcCommunicationManager} to route commands through.
     */
    protected ClientCommandManager(String serial,TekdaqcCommunicationManager manager){
        mServiceManager = manager;
        mSerial = serial;
    }

    @Override
    public void queueCommand(IQueueObject command) {
        mServiceManager.executeCommand(mSerial,command);

    }

    @Override
    public void queueTask(Task task) {
         mServiceManager.executeTask(mSerial,task);
    }


    @Override
    public void tryCommand() {}
}

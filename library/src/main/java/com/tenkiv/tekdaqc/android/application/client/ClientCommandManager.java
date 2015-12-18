package com.tenkiv.tekdaqc.android.application.client;

import android.os.Messenger;
import com.tenkiv.tekdaqc.android.application.service.TekdaqcCommunicationManager;
import com.tenkiv.tekdaqc.communication.command.queue.ICommandManager;
import com.tenkiv.tekdaqc.communication.command.queue.IQueueObject;
import com.tenkiv.tekdaqc.communication.command.queue.Task;

/**
 * Created by ejberry on 11/30/15.
 */
public class ClientCommandManager implements ICommandManager {

    private TekdaqcCommunicationManager mServiceManager;

    private String mSerial;

    protected ClientCommandManager(String serial,TekdaqcCommunicationManager manager){
        mServiceManager = manager;
        mSerial = serial;
    }

    @Override
    public void queueCommand(IQueueObject command) {
        try {
            mServiceManager.executeCommand(mSerial,command);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void queueTask(Task task) {
         mServiceManager.executeTask(mSerial,task);
    }


    @Override
    public void tryCommand() {

    }
}

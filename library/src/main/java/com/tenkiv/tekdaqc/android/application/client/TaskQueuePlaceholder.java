package com.tenkiv.tekdaqc.android.application.client;

import com.tenkiv.tekdaqc.communication.command.queue.IQueueObject;
import com.tenkiv.tekdaqc.communication.command.queue.QueueCallback;
import com.tenkiv.tekdaqc.communication.command.queue.QueueUtil;
import com.tenkiv.tekdaqc.communication.command.queue.values.ABaseQueueVal;
import com.tenkiv.tekdaqc.revd.CommandBuilder;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Created by ejberry on 12/7/15.
 */
public class TaskQueuePlaceholder extends QueueCallback {

    private double mUID;

    public TaskQueuePlaceholder(double uid){
        mUID = uid;
    }

    @Override
    public void readExternal(ObjectInput input) throws IOException, ClassNotFoundException {
        mUID = input.readDouble();
        Object object = input.readObject();
        int ints = input.readInt();
        double doubles = input.available();


    }

    @Override
    public void writeExternal(ObjectOutput output) throws IOException {
        output.writeDouble(mUID);
    }
}

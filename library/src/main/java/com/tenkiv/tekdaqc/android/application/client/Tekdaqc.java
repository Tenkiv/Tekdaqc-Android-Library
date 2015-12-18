package com.tenkiv.tekdaqc.android.application.client;

import com.tenkiv.tekdaqc.ATekdaqc;
import com.tenkiv.tekdaqc.android.application.service.TekdaqcCommunicationManager;
import com.tenkiv.tekdaqc.communication.message.IAnalogChannelListener;
import com.tenkiv.tekdaqc.communication.message.IDigitalChannelListener;
import com.tenkiv.tekdaqc.communication.message.IMessageListener;
import com.tenkiv.tekdaqc.locator.LocatorResponse;
import com.tenkiv.tekdaqc.peripherals.analog.AAnalogInput;
import com.tenkiv.tekdaqc.peripherals.digital.DigitalInput;
import com.tenkiv.tekdaqc.revd.Tekdaqc_RevD;

import java.io.IOException;

/**
 * Created by ejberry on 11/30/15.
 */
public class Tekdaqc extends Tekdaqc_RevD {

    private TekdaqcCommunicationManager mManager;

    public Tekdaqc(LocatorResponse response, TekdaqcCommunicationManager manager){
        super(response);

        mManager = manager;

        mCommandQueue = new ClientCommandManager(mResponse.getSerial(),mManager);

    }

    @Override
    public void connect(CONNECTION_METHOD method) throws IOException {
        mManager.connectToTekdaqc(this);
    }

}

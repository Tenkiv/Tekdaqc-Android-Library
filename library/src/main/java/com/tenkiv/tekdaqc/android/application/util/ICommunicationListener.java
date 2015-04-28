package com.tenkiv.tekdaqc.android.application.util;

import com.tenkiv.tekdaqc.ATekdaqc;
import com.tenkiv.tekdaqc.android.application.service.TekdaqcCommunicationManager;
import com.tenkiv.tekdaqc.communication.message.IMessageListener;

/**
 * Created by Ellis Berry (ejberry@tenkiv.com)
 */
public interface ICommunicationListener extends IMessageListener {

    public void onManagerServiceCreated(TekdaqcCommunicationManager communicationManager);

    public void onTekdaqcConnected(String serial, ATekdaqc tekdaqc);

    public void onTekdaqcDisconnected(String serial, ATekdaqc tekdaqc);
}

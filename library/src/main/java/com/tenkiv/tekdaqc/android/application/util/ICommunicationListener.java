package com.tenkiv.tekdaqc.android.application.util;

import com.tenkiv.tekdaqc.ATekdaqc;
import com.tenkiv.tekdaqc.android.application.service.TekdaqcCommunicationManager;
import com.tenkiv.tekdaqc.communication.message.IMessageListener;

/**
 * Created by Ellis Berry (ejberry@tenkiv.com)
 */
public interface ICommunicationListener extends IMessageListener {

    public void onTekdaqcConnected(ATekdaqc tekdaqc);

    public void onTekdaqcDisconnected(ATekdaqc tekdaqc);
}

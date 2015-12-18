package com.tenkiv.tekdaqc.android.application.util;

import com.tenkiv.tekdaqc.android.application.service.TekdaqcCommunicationManager;

/**
 * Created by ejberry on 4/29/15.
 */
public interface IServiceListener {

    void onManagerServiceCreated(TekdaqcCommunicationManager communicationManager);

}

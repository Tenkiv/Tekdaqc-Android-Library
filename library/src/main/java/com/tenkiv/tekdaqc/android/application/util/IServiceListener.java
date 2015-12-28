package com.tenkiv.tekdaqc.android.application.util;

import com.tenkiv.tekdaqc.android.application.service.CommunicationService;
import com.tenkiv.tekdaqc.android.application.service.TekdaqcCommunicationManager;

/**
 * Simple interface to provide callbacks upon {@link CommunicationService} creation.
 *
 * @author Tenkiv (software@tenkiv.com)
 * @since v2.0.0.0
 */
public interface IServiceListener {

    /**
     * Notification that the {@link CommunicationService} has been created and bound to.
     *
     * @param communicationManager The manager.
     */
    void onManagerServiceCreated(TekdaqcCommunicationManager communicationManager);

}

package com.tenkiv.tekdaqc.android.application.service.remote_parceling;

import com.tenkiv.tekdaqc.android.application.service.remote_parceling.AndroidRemoteParser;
import com.tenkiv.tekdaqc.communication.ascii.executors.ASCIIParsingExecutor;
import com.tenkiv.tekdaqc.hardware.Tekdaqc_RevD;
import com.tenkiv.tekdaqc.locator.LocatorResponse;

/**
 * Tekdaqc class which overrides the base parsing executor to wrap messages into {@link android.os.Parcelable}s which
 * can be used in inter-process communication more efficiently then {@link java.io.Externalizable}.
 *
 * @author Tenkiv (software@tenkiv.com)
 * @since v1.0.0.0
 */
public class AndroidRemoteTekdaqc extends Tekdaqc_RevD{

    public AndroidRemoteTekdaqc(LocatorResponse response){
        super(response);
    }

    public AndroidRemoteTekdaqc(){
        super();
    }

    @Override
    protected ASCIIParsingExecutor getParingExecutor() {
        return new AndroidRemoteParser(1);
    }
}

package com.tenkiv.tekdaqc.android.application.service.remote_parceling;

import android.os.Parcelable;
import com.tenkiv.tekdaqc.communication.ascii.message.parsing.ASCIIDigitalInputDataMessage;
import com.tenkiv.tekdaqc.communication.data_points.DataPoint;

/**
 * Wrapper to allow messages to be turned into {@link Parcelable}.
 *
 * @author Tenkiv (software@tenkiv.com)
 * @since v1.0.0.0
 */
public class ParcelableDigitalInputMessage extends ASCIIDigitalInputDataMessage {

    public ParcelableDigitalInputMessage(String message){
        setData(message);
    }

    @Override
    public DataPoint toDataPoints() {
        return new ParcelableDigitalInputData(mNumber, mName, Long.parseLong(mTimestamps.replaceAll("\\s", "")), mReadings);
    }


}

package com.tenkiv.tekdaqc.android.application.service.remote_parceling;

import android.os.Parcel;
import android.os.Parcelable;
import com.tenkiv.tekdaqc.communication.ascii.message.parsing.ASCIIStatusMessage;

/**
 * Wrapper to allow status messages to be turned into {@link Parcelable}.
 *
 * @author Tenkiv (software@tenkiv.com)
 * @since v1.0.0.0
 */
public class ParcelableStatusMessage extends ASCIIStatusMessage implements Parcelable {

    public ParcelableStatusMessage(String message){
        setData(message);
    }

    public ParcelableStatusMessage(Parcel in) {
        mMessageString = in.readString();
        mTimestamp = in.readLong();
    }

    public static final Creator<ParcelableStatusMessage> CREATOR = new Creator<ParcelableStatusMessage>() {
        @Override
        public ParcelableStatusMessage createFromParcel(Parcel in) {
            return new ParcelableStatusMessage(in);
        }

        @Override
        public ParcelableStatusMessage[] newArray(int size) {
            return new ParcelableStatusMessage[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mMessageString);
        dest.writeLong(mTimestamp);
    }
}

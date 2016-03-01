package com.tenkiv.tekdaqc.android.application.service.remote_parceling;

import android.os.Parcel;
import android.os.Parcelable;
import com.tenkiv.tekdaqc.communication.ascii.message.parsing.ASCIIDebugMessage;

/**
 * Wrapper to allow messages to be turned into {@link Parcelable}.
 *
 * @author Tenkiv (software@tenkiv.com)
 * @since v1.0.0.0
 */
public class ParcelableDebugMessage extends ASCIIDebugMessage implements Parcelable {

    public ParcelableDebugMessage(String message){
        setData(message);
    }

    public ParcelableDebugMessage(Parcel in) {
        mMessageString = in.readString();
        mTimestamp = in.readLong();
    }

    public static final Creator<ParcelableDebugMessage> CREATOR = new Creator<ParcelableDebugMessage>() {
        @Override
        public ParcelableDebugMessage createFromParcel(Parcel in) {
            return new ParcelableDebugMessage(in);
        }

        @Override
        public ParcelableDebugMessage[] newArray(int size) {
            return new ParcelableDebugMessage[size];
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

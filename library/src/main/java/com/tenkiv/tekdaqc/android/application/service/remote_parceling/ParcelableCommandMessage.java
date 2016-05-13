package com.tenkiv.tekdaqc.android.application.service.remote_parceling;

import android.os.Parcel;
import android.os.Parcelable;
import com.tenkiv.tekdaqc.communication.ascii.message.parsing.ASCIICommandMessage;

/**
 * Wrapper to allow Command messages to be turned into {@link Parcelable}.
 *
 * @author Tenkiv (software@tenkiv.com)
 * @since v2.0.0.0
 */
public class ParcelableCommandMessage extends ASCIICommandMessage implements Parcelable {

    public ParcelableCommandMessage(String message){
        setData(message);
    }

    public ParcelableCommandMessage(Parcel in) {
        mMessageString = in.readString();
        mTimestamp = in.readLong();
    }

    public static final Creator<ParcelableCommandMessage> CREATOR = new Creator<ParcelableCommandMessage>() {
        @Override
        public ParcelableCommandMessage createFromParcel(Parcel in) {
            return new ParcelableCommandMessage(in);
        }

        @Override
        public ParcelableCommandMessage[] newArray(int size) {
            return new ParcelableCommandMessage[size];
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

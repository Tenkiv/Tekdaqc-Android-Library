package com.tenkiv.tekdaqc.android.application.service.remote_parceling;

import android.os.Parcel;
import android.os.Parcelable;
import com.tenkiv.tekdaqc.communication.ascii.message.parsing.ASCIIErrorMessage;

/**
 * Wrapper to allow error messages to be turned into {@link Parcelable}.
 *
 * @author Tenkiv (software@tenkiv.com)
 * @since v1.0.0.0
 */
public class ParcelableErrorMessage extends ASCIIErrorMessage implements Parcelable {

    public ParcelableErrorMessage(String message){
        setData(message);
    }

    public ParcelableErrorMessage(Parcel in) {
        mMessageString = in.readString();
        mTimestamp = in.readLong();
    }

    public static final Creator<ParcelableErrorMessage> CREATOR = new Creator<ParcelableErrorMessage>() {
        @Override
        public ParcelableErrorMessage createFromParcel(Parcel in) {
            return new ParcelableErrorMessage(in);
        }

        @Override
        public ParcelableErrorMessage[] newArray(int size) {
            return new ParcelableErrorMessage[size];
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

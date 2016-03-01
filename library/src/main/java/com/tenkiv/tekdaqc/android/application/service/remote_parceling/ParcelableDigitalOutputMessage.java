package com.tenkiv.tekdaqc.android.application.service.remote_parceling;

import android.os.Parcel;
import android.os.Parcelable;
import com.tenkiv.tekdaqc.communication.ascii.message.parsing.ASCIIDigitalOutputDataMessage;

/**
 * Wrapper to allow messages to be turned into {@link Parcelable}.
 *
 * @author Tenkiv (software@tenkiv.com)
 * @since v1.0.0.0
 */
public class ParcelableDigitalOutputMessage extends ASCIIDigitalOutputDataMessage implements Parcelable {

    public ParcelableDigitalOutputMessage(String message){
        setData(message);
    }

    public ParcelableDigitalOutputMessage(Parcel in) {
        in.readBooleanArray(mDigitalOutputArray);
        mTimestamp = in.readLong();
    }

    public static final Creator<ParcelableDigitalOutputMessage> CREATOR = new Creator<ParcelableDigitalOutputMessage>() {
        @Override
        public ParcelableDigitalOutputMessage createFromParcel(Parcel in) {
            return new ParcelableDigitalOutputMessage(in);
        }

        @Override
        public ParcelableDigitalOutputMessage[] newArray(int size) {
            return new ParcelableDigitalOutputMessage[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBooleanArray(mDigitalOutputArray);
        dest.writeLong(mTimestamp);
    }
}

package com.tenkiv.tekdaqc.android.application.service.remote_parceling;

import android.os.Parcel;
import android.os.Parcelable;
import com.tenkiv.tekdaqc.communication.data_points.DigitalInputData;
import com.tenkiv.tekdaqc.utility.DigitalState;

/**
 * Created by ejberry on 2/15/16.
 */
public class ParcelableDigitalInputData extends DigitalInputData implements Parcelable {

    public ParcelableDigitalInputData(final int channel, final String name, final long timestamp, final DigitalState state) {
        super(channel, name, timestamp, state);
    }

    protected ParcelableDigitalInputData(Parcel in) {
        mPhysicalChannel = in.readInt();
        mName = in.readString();
        mTimeStamp = in.readLong();
        mState = DigitalState.getValueFromOrdinal((byte)in.readInt());
    }

    public static final Creator<ParcelableDigitalInputData> CREATOR = new Creator<ParcelableDigitalInputData>() {
        @Override
        public ParcelableDigitalInputData createFromParcel(Parcel in) {
            return new ParcelableDigitalInputData(in);
        }

        @Override
        public ParcelableDigitalInputData[] newArray(int size) {
            return new ParcelableDigitalInputData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mPhysicalChannel);
        dest.writeString(mName);
        dest.writeLong(mTimeStamp);
        dest.writeInt(mState.ordinal());
    }
}

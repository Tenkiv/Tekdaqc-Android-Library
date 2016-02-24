package com.tenkiv.tekdaqc.android.application.service.remote_parceling;

import android.os.Parcel;
import android.os.Parcelable;
import com.tenkiv.tekdaqc.communication.data_points.AnalogInputData;

/**
 * Created by ejberry on 2/15/16.
 */
public class ParcelableAnalogInputData extends AnalogInputData implements Parcelable{

    public ParcelableAnalogInputData(final int channel, final String name, final long timestamp, final int data) {
        super(channel, name, timestamp, data);
    }


    protected ParcelableAnalogInputData(Parcel in) {
        mPhysicalChannel = in.readInt();
        mName = in.readString();
        mTimeStamp = in.readLong();
        mData = in.readInt();
    }

    public static final Creator<ParcelableAnalogInputData> CREATOR = new Creator<ParcelableAnalogInputData>() {
        @Override
        public ParcelableAnalogInputData createFromParcel(Parcel in) {
            return new ParcelableAnalogInputData(in);
        }

        @Override
        public ParcelableAnalogInputData[] newArray(int size) {
            return new ParcelableAnalogInputData[size];
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
        dest.writeInt(mData);
    }
}

package com.tenkiv.tekdaqc.android.content;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import android.content.ContentValues;

import com.tenkiv.tekdaqc.ATekdaqc.ANALOG_SCALE;
import com.tenkiv.tekdaqc.communication.data_points.AnalogInputData;

public class AnalogDataPoint implements Externalizable {

	private static final long serialVersionUID = 1L;
	
	private AnalogInputData mData;
	private String mSerial;
	private ANALOG_SCALE mScale;
	
	public AnalogDataPoint(AnalogInputData data, String serial, ANALOG_SCALE scale) {
		mData = data;
	}
	
	public ContentValues toContentValues() {
		final ContentValues values = new ContentValues();
		values.put(TekdaqcDataProviderContract.COLUMN_SERIAL, mSerial);
		values.put(TekdaqcDataProviderContract.COLUMN_TIMESTAMP, mData.getTimestamp().longValue());
		values.put(TekdaqcDataProviderContract.COLUMN_TIME, System.currentTimeMillis());
		values.put(TekdaqcDataProviderContract.COLUMN_ANALOG_COUNTS, mData.getData());
		values.put(TekdaqcDataProviderContract.COLUMN_GAIN, mData.getGain().gain);
		values.put(TekdaqcDataProviderContract.COLUMN_RATE, mData.getSampleRate().rate);
		values.put(TekdaqcDataProviderContract.COLUMN_BUFFER, mData.getBufferStatus().name());
		values.put(TekdaqcDataProviderContract.COLUMN_SCALE, mScale.scale);
		return values;
	}

	@Override
	public void readExternal(ObjectInput input) throws IOException, ClassNotFoundException {
		mData = (AnalogInputData) input.readObject();
		mSerial = (String) input.readObject();
		mScale = (ANALOG_SCALE) input.readObject();
	}

	@Override
	public void writeExternal(ObjectOutput output) throws IOException {
		output.writeObject(mData);
		output.writeObject(mSerial);
		output.writeObject(mScale);
	}
}

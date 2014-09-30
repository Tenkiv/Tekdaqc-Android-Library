/**
 * Copyright 2013 Tenkiv, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tenkiv.tekdaqc.android.content;

import android.content.ContentValues;

import com.tenkiv.tekdaqc.ATekdaqc.ANALOG_SCALE;
import com.tenkiv.tekdaqc.peripherals.analog.AAnalogInput.Gain;
import com.tenkiv.tekdaqc.peripherals.analog.AAnalogInput.Rate;
import com.tenkiv.tekdaqc.peripherals.analog.ADS1256_AnalogInput.BUFFER_STATE;

/**
 * POJO Class for a single calibration data point.
 *
 * @author Jared Woolston (jwoolston@tenkiv.com)
 * @since v1.0.0.0
 */
public class CalibrationPoint {

    private long mID;
    private String mSerial;
    private long mTime;
    private double mTemperature;
    private Gain mGain;
    private Rate mRate;
    private BUFFER_STATE mBuffer;
    private ANALOG_SCALE mScale;
    private int mSelfGainCal;
    private int mSystemGainCal;
    private int mGainCalDifference;
    
    public CalibrationPoint(String serial, long time, double temp, Gain gain, Rate rate, BUFFER_STATE buffer, ANALOG_SCALE scale, int selfGain, int systemGain, int gainDiff) {
    	this(-1, serial, time, temp, gain, rate, buffer, scale, selfGain, systemGain, gainDiff);
    }
    
    public CalibrationPoint(long id, String serial, long time, double temp, Gain gain, Rate rate, BUFFER_STATE buffer, ANALOG_SCALE scale,  int selfGain, int systemGain, int gainDiff) {
    	mID = id;
    	mSerial = serial;
    	mTime = time;
    	mTemperature = temp;
    	mGain = gain;
    	mRate = rate;
    	mBuffer = buffer;
    	mScale = scale;
    	mSelfGainCal = selfGain;
    	mSystemGainCal = systemGain;
    	mGainCalDifference = gainDiff;
    }
    
    public ContentValues toContentValues() {
    	final ContentValues values = new ContentValues();
		values.put(TekdaqcDataProviderContract.COLUMN_SERIAL, mSerial);
		values.put(TekdaqcDataProviderContract.COLUMN_TIME, System.currentTimeMillis());
		values.put(TekdaqcDataProviderContract.COLUMN_GAIN, Integer.valueOf(mGain.gain));
		values.put(TekdaqcDataProviderContract.COLUMN_RATE, Float.valueOf(mRate.rate));
		values.put(TekdaqcDataProviderContract.COLUMN_BUFFER, mBuffer.name());
		values.put(TekdaqcDataProviderContract.COLUMN_SCALE, mScale.scale);
		// We are writing the goal here to be as exact as possible
		values.put(TekdaqcDataProviderContract.COLUMN_TEMPERATURE, mTemperature);
		values.put(TekdaqcDataProviderContract.COLUMN_SELF_GAIN_CAL, mSelfGainCal);
		values.put(TekdaqcDataProviderContract.COLUMN_SYSTEM_GAIN_CAL, mSystemGainCal);
		values.put(TekdaqcDataProviderContract.COLUMN_GAIN_CAL_DIFF, mGainCalDifference);
		return values;
    }

    public long getID() {
        return mID;
    }

    public void setID(long id) {
        mID = id;
    }

    public String getSerial() {
        return mSerial;
    }

    public void setSerial(String serial) {
        mSerial = serial;
    }

    public long getTime() {
        return mTime;
    }

    public void setTime(long time) {
        mTime = time;
    }

    public Gain getGain() {
        return mGain;
    }

    public void setGain(Gain gain) {
        mGain = gain;
    }

    public Rate getRate() {
        return mRate;
    }

    public void setRate(Rate rate) {
        mRate = rate;
    }

    public BUFFER_STATE getBuffer() {
        return mBuffer;
    }
    
    public void setBuffer(BUFFER_STATE buffer) {
        mBuffer = buffer;
    }
    
    public ANALOG_SCALE getAnalogScale() {
    	return mScale;
    }
    
    public void setAnalogScale(ANALOG_SCALE scale) {
    	mScale = scale;
    }

    public int getSelfGainCal() {
        return mSelfGainCal;
    }

    public void setSelfGainCal(int cal) {
        mSelfGainCal = cal;
    }

    public int getSystemGainCal() {
        return mSystemGainCal;
    }

    public void setSystemGainCal(int cal) {
        mSystemGainCal = cal;
    }

    public int getGainCalDifference() {
        return mGainCalDifference;
    }

    public void setGainCalDifference(int difference) {
        mGainCalDifference = difference;
    }
}

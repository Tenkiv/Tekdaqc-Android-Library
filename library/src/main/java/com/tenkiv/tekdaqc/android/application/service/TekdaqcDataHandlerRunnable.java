package com.tenkiv.tekdaqc.android.application.service;

import com.tenkiv.tekdaqc.ATekdaqc;
import com.tenkiv.tekdaqc.android.application.util.ICommunicationListener;
import com.tenkiv.tekdaqc.android.application.util.TekdaqcHandlerCall;
import com.tenkiv.tekdaqc.communication.data_points.AnalogInputData;
import com.tenkiv.tekdaqc.communication.data_points.DigitalInputData;
import com.tenkiv.tekdaqc.communication.data_points.DigitalOutputData;
import com.tenkiv.tekdaqc.communication.message.ABoardMessage;

import java.util.List;

/**
 * Created by Ellis Berry (ejberry@tenkiv.com)
 */
public class TekdaqcDataHandlerRunnable implements Runnable{

    private ICommunicationListener mListener;

    private TekdaqcHandlerCall mCallType;

    private String mSerial;

    private Object mData;

    protected TekdaqcDataHandlerRunnable(String serial, Object data, ICommunicationListener listener, TekdaqcHandlerCall callType){

        mSerial = serial;
        mData = data;
        mListener = listener;
        mCallType = callType;

    }

    @Override
    public void run() {

        switch(mCallType){

            case ANALOG_S:
                mListener.onAnalogInputDataReceived(mSerial,(AnalogInputData)mData);
                break;

            case ANALOG_L:
                mListener.onAnalogInputDataReceived(mSerial,(List<AnalogInputData>)mData);
                break;

            case COMMAND:
                mListener.onCommandDataMessageReceived(mSerial,(ABoardMessage)mData);
                break;

            case DEBUG:
                mListener.onDebugMessageReceived(mSerial,(ABoardMessage)mData);
                break;

            case ERROR:
                mListener.onErrorMessageReceived(mSerial,(ABoardMessage)mData);
                break;

            case STATUS:
                mListener.onStatusMessageReceived(mSerial,(ABoardMessage)mData);
                break;

            case DIGITAL_I:
                mListener.onDigitalInputDataReceived(mSerial,(DigitalInputData)mData);
                break;

            case DIGITAL_O:
                mListener.onDigitalOutputDataReceived(mSerial,(DigitalOutputData)mData);
                break;
            case CONNECTED:
                mListener.onTekdaqcConnected(mSerial,(TekdaqcCommunicationManager) mData);
                break;
            case DISCONNECTED:
                mListener.onTekdaqcDisconnected(mSerial,(ATekdaqc) mData);
                break;
        }
    }
}

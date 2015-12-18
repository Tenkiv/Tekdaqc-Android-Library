package com.tenkiv.tekdaqc.android.application.service;

import com.tenkiv.tekdaqc.ATekdaqc;
import com.tenkiv.tekdaqc.android.application.util.ICommunicationListener;
import com.tenkiv.tekdaqc.android.application.util.TekdaqcHandlerCall;
import com.tenkiv.tekdaqc.communication.data_points.AnalogInputData;
import com.tenkiv.tekdaqc.communication.data_points.DigitalInputData;
import com.tenkiv.tekdaqc.communication.message.ABoardMessage;


public class TekdaqcDataHandlerRunnable implements Runnable{

    private ICommunicationListener mListener;

    private TekdaqcHandlerCall mCallType;

    private ATekdaqc mTekdaqc;

    private Object mData;

    protected TekdaqcDataHandlerRunnable(ATekdaqc tekdaqc, Object data, ICommunicationListener listener, TekdaqcHandlerCall callType){

        mTekdaqc = tekdaqc;
        mData = data;
        mListener = listener;
        mCallType = callType;

    }

    @Override
    public void run() {

        switch(mCallType){

            case ANALOG:
                mListener.onAnalogInputDataReceived(mTekdaqc,(AnalogInputData)mData);
                break;

            case COMMAND:
                mListener.onCommandDataMessageReceived(mTekdaqc,(ABoardMessage)mData);
                break;

            case DEBUG:
                mListener.onDebugMessageReceived(mTekdaqc,(ABoardMessage)mData);
                break;

            case ERROR:
                mListener.onErrorMessageReceived(mTekdaqc,(ABoardMessage)mData);
                break;

            case STATUS:
                mListener.onStatusMessageReceived(mTekdaqc,(ABoardMessage)mData);
                break;

            case DIGITAL_I:
                mListener.onDigitalInputDataReceived(mTekdaqc,(DigitalInputData)mData);
                break;

            case DIGITAL_O:
                mListener.onDigitalOutputDataReceived(mTekdaqc,(boolean[])mData);
                break;

            case CONNECTED:
                mListener.onTekdaqcConnected(mTekdaqc);
                break;

            case DISCONNECTED:
                mListener.onTekdaqcDisconnected(mTekdaqc);
                break;

        }
    }
}

package com.tenkiv.tekdaqc.android.application.service;

import com.tenkiv.tekdaqc.android.application.util.TekdaqcHandlerCall;
import com.tenkiv.tekdaqc.communication.tasks.ITaskComplete;

/**
 * Created by Ellis Berry (ejberry@tenkiv.com)
 */
public class TekdaqcTaskRunnable implements Runnable{

    ITaskComplete mListener;

    TekdaqcHandlerCall mCallType;

    public TekdaqcTaskRunnable(ITaskComplete listener, TekdaqcHandlerCall type){

        mListener = listener;
        mCallType = type;

    }

    @Override
    public void run() {
        switch (mCallType){

            case TASK_SUCCESS:
                mListener.onTaskSuccess();
                break;

            case TASK_FAILED:
                mListener.onTaskFailed();
                break;
        }
    }
}

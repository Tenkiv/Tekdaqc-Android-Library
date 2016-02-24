package com.tenkiv.tekdaqc.locator;

import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import com.tenkiv.tekdaqc.android.application.client.TekdaqcCommunicationManager;
import com.tenkiv.tekdaqc.android.application.util.TekCast;
import com.tenkiv.tekdaqc.communication.ascii.message.parsing.ASCIIDigitalOutputDataMessage;
import com.tenkiv.tekdaqc.communication.command.queue.QueueCallback;
import com.tenkiv.tekdaqc.communication.data_points.AnalogInputData;
import com.tenkiv.tekdaqc.communication.data_points.DigitalInputData;
import com.tenkiv.tekdaqc.communication.message.ABoardMessage;
import com.tenkiv.tekdaqc.communication.message.MessageBroadcaster;
import com.tenkiv.tekdaqc.hardware.ATekdaqc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link Handler} class for the {@link TekdaqcCommunicationManager} which manages its {@link Messenger} callbacks.
 */
public class ClientMessageHandler extends Handler {

    private MessageBroadcaster mBroadcaster;

    /**
     * A {@link Map} of all {@link ATekdaqc}s and their serial numbers.
     */
    private ConcurrentHashMap<String,ATekdaqc> mTekdaqcMap = new ConcurrentHashMap<String,ATekdaqc>();

    /**
     * A {@link Map} of all {@link QueueCallback}s and their unique identifiers so that Task callbacks can be conducted across processes.
     */
    private ConcurrentHashMap<Double,QueueCallback> mTaskMap = new ConcurrentHashMap<>();

    public ClientMessageHandler(MessageBroadcaster broadcaster){
        mBroadcaster = broadcaster;
    }

    public void addTaskToMap(final Double uid, final QueueCallback callback){
        if(!mTaskMap.containsKey(uid)){
            mTaskMap.put(uid,callback);
        }
    }

    public void removeTaskFromMap(final Double uid){
        mTaskMap.remove(uid);
    }

    public void addTekdaqcToMap(final ATekdaqc tekdaqc){
        if(!mTekdaqcMap.containsKey(tekdaqc.getSerialNumber())) {
            mTekdaqcMap.put(tekdaqc.getSerialNumber(), tekdaqc);
        }
    }

    public void removeTekdaqcFromMap(final String serial){
        mTekdaqcMap.remove(serial);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what){

            case TekCast.TEKDAQC_ANALOG_INPUT_MESSAGE:
                mBroadcaster.broadcastAnalogInputDataPoint(
                        mTekdaqcMap.get(msg.getData().getString(TekCast.DATA_MESSSAGE_TEKDAQC)),
                        (AnalogInputData)msg.getData().getParcelable(TekCast.DATA_MESSSAGE));
                break;

            case TekCast.TEKDAQC_COMMAND_MESSAGE:
                mBroadcaster.broadcastMessage(
                        mTekdaqcMap.get(msg.getData().getString(TekCast.DATA_MESSSAGE_TEKDAQC)),
                        (ABoardMessage)msg.getData().getParcelable(TekCast.DATA_MESSSAGE));
                break;

            case TekCast.TEKDAQC_DEBUG_MESSAGE:
                mBroadcaster.broadcastMessage(
                        mTekdaqcMap.get(msg.getData().getString(TekCast.DATA_MESSSAGE_TEKDAQC)),
                        (ABoardMessage)msg.getData().getParcelable(TekCast.DATA_MESSSAGE));
                break;

            case TekCast.TEKDAQC_DIGITAL_INPUT_MESSAGE:
                mBroadcaster.broadcastDigitalInputDataPoint(
                        mTekdaqcMap.get(msg.getData().getString(TekCast.DATA_MESSSAGE_TEKDAQC)),
                        (DigitalInputData)msg.getData().getParcelable(TekCast.DATA_MESSSAGE));
                break;

            case TekCast.TEKDAQC_DIGITAL_OUTPUT_MESSAGE:
                mBroadcaster.broadcastMessage(
                        mTekdaqcMap.get(msg.getData().getString(TekCast.DATA_MESSSAGE_TEKDAQC)),
                        new ASCIIDigitalOutputDataMessage(msg.getData().getBooleanArray(TekCast.DATA_MESSSAGE)));
                break;

            case TekCast.TEKDAQC_ERROR_MESSAGE:
                mBroadcaster.broadcastMessage(
                        mTekdaqcMap.get(msg.getData().getString(TekCast.DATA_MESSSAGE_TEKDAQC)),
                        (ABoardMessage)msg.getData().getParcelable(TekCast.DATA_MESSSAGE));
                break;

            case TekCast.TEKDAQC_STATUS_MESSAGE:
                mBroadcaster.broadcastMessage(
                        mTekdaqcMap.get(msg.getData().getString(TekCast.DATA_MESSSAGE_TEKDAQC)),
                        (ABoardMessage)msg.getData().getParcelable(TekCast.DATA_MESSSAGE));
                break;

            case TekCast.TEKDAQC_TASK_COMPLETE:
                final double uidComplete = msg.getData().getDouble(TekCast.DATA_MESSSAGE_UID);
                mTaskMap.get(uidComplete).success(
                        mTekdaqcMap.get(msg.getData().getString(TekCast.DATA_MESSSAGE_TEKDAQC)));
                removeTaskFromMap(uidComplete);
                break;

            case TekCast.TEKDAQC_TASK_FAILURE:
                final double uidFailure = msg.getData().getDouble(TekCast.DATA_MESSSAGE_UID);
                mTaskMap.get(uidFailure).failure(
                        mTekdaqcMap.get(msg.getData().getString(TekCast.DATA_MESSSAGE_TEKDAQC)));
                removeTaskFromMap(uidFailure);
                break;


        }
    }
}
package com.tenkiv.tekdaqc.locator;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import com.tenkiv.tekdaqc.android.application.client.TekdaqcCommunicationManager;
import com.tenkiv.tekdaqc.communication.ascii.message.parsing.ASCIIDigitalOutputDataMessage;
import com.tenkiv.tekdaqc.communication.command.queue.QueueCallback;
import com.tenkiv.tekdaqc.communication.data_points.AnalogInputCountData;
import com.tenkiv.tekdaqc.communication.data_points.DigitalInputData;
import com.tenkiv.tekdaqc.communication.message.ABoardMessage;
import com.tenkiv.tekdaqc.communication.message.MessageBroadcaster;
import com.tenkiv.tekdaqc.hardware.ATekdaqc;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static com.tenkiv.tekdaqc.android.application.util.UtilKt.*;

/**
 * {@link Handler} class for the {@link TekdaqcCommunicationManager} which manages its {@link Messenger} callbacks.
 */
public class ClientMessageHandler extends Handler {

    private MessageBroadcaster mBroadcaster;

    private Context mContext;

    /**
     * A {@link Map} of all {@link ATekdaqc}s and their serial numbers.
     */
    private ConcurrentHashMap<String,ATekdaqc> mTekdaqcMap = new ConcurrentHashMap<String,ATekdaqc>();

    /**
     * A {@link Map} of all {@link QueueCallback}s and their unique identifiers so that Task callbacks can be conducted across processes.
     */
    private ConcurrentHashMap<Double,QueueCallback> mTaskMap = new ConcurrentHashMap<>();

    public ClientMessageHandler(Context context, MessageBroadcaster broadcaster){
        mContext = context;
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


    /**
     * Adds ATekdaqc to {@link ClientMessageHandler} list.
     * @param tekdaqc ATekdaqc to be added.
     */
    public void addTekdaqcToMap(final ATekdaqc tekdaqc){
        if(!mTekdaqcMap.containsKey(tekdaqc.getSerialNumber())) {
            mTekdaqcMap.put(tekdaqc.getSerialNumber(), tekdaqc);
        }
    }

    /**
     * Removes a {@link ATekdaqc} from the created list.
     *
     * @param serial Serial number of the {@link ATekdaqc} being removed.
     */
    public void removeTekdaqcFromMap(final String serial){
        mTekdaqcMap.remove(serial);
    }

    @Override
    public void handleMessage(Message msg) {

        try{
            msg.getData().setClassLoader(mContext.getClassLoader());

            switch (msg.what) {

                case TEKDAQC_ANALOG_INPUT_MESSAGE:
                    mBroadcaster.broadcastAnalogInputDataPoint(
                        mTekdaqcMap.get(msg.getData().getString(DATA_MESSSAGE_TEKDAQC)),
                        (AnalogInputCountData) msg.getData().getParcelable(DATA_MESSSAGE));
                    break;

                case TEKDAQC_COMMAND_MESSAGE:
                    mBroadcaster.broadcastMessage(
                        mTekdaqcMap.get(msg.getData().getString(DATA_MESSSAGE_TEKDAQC)),
                        (ABoardMessage) msg.getData().getParcelable(DATA_MESSSAGE));
                    break;

                case TEKDAQC_DEBUG_MESSAGE:
                    mBroadcaster.broadcastMessage(
                        mTekdaqcMap.get(msg.getData().getString(DATA_MESSSAGE_TEKDAQC)),
                        (ABoardMessage) msg.getData().getParcelable(DATA_MESSSAGE));
                    break;

                case TEKDAQC_DIGITAL_INPUT_MESSAGE:
                    mBroadcaster.broadcastDigitalInputDataPoint(
                        mTekdaqcMap.get(msg.getData().getString(DATA_MESSSAGE_TEKDAQC)),
                        (DigitalInputData) msg.getData().getParcelable(DATA_MESSSAGE));
                    break;

                case TEKDAQC_DIGITAL_OUTPUT_MESSAGE:
                    mBroadcaster.broadcastMessage(
                        mTekdaqcMap.get(msg.getData().getString(DATA_MESSSAGE_TEKDAQC)),
                        new ASCIIDigitalOutputDataMessage(msg.getData().getBooleanArray(DATA_MESSSAGE)));
                    break;

                case TEKDAQC_ERROR_MESSAGE:
                    mBroadcaster.broadcastMessage(
                        mTekdaqcMap.get(msg.getData().getString(DATA_MESSSAGE_TEKDAQC)),
                        (ABoardMessage) msg.getData().getParcelable(DATA_MESSSAGE));
                    break;

                case TEKDAQC_STATUS_MESSAGE:
                    mBroadcaster.broadcastMessage(
                        mTekdaqcMap.get(msg.getData().getString(DATA_MESSSAGE_TEKDAQC)),
                        (ABoardMessage) msg.getData().getParcelable(DATA_MESSSAGE));
                    break;

                case TEKDAQC_TASK_COMPLETE:
                    final double uidComplete = msg.getData().getDouble(DATA_MESSSAGE_UID);
                    mTaskMap.get(uidComplete).success(
                        mTekdaqcMap.get(msg.getData().getString(DATA_MESSSAGE_TEKDAQC)));
                    removeTaskFromMap(uidComplete);
                    break;

                case TEKDAQC_TASK_FAILURE:
                    final double uidFailure = msg.getData().getDouble(DATA_MESSSAGE_UID);
                    mTaskMap.get(uidFailure).failure(
                        mTekdaqcMap.get(msg.getData().getString(DATA_MESSSAGE_TEKDAQC)));
                    removeTaskFromMap(uidFailure);
                    break;
            }

        }catch(NullPointerException ex){
            Log.e("Tekdaqc Handler Error","Null Pointer Exception caught during message handling. This is likely due to disconnection");
        }
    }
}
package com.tenkiv.tekdaqc.android.application.service;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.*;
import com.tenkiv.tekdaqc.ATekdaqc;
import com.tenkiv.tekdaqc.android.application.client.Tekdaqc;
import com.tenkiv.tekdaqc.android.application.util.IServiceListener;
import com.tenkiv.tekdaqc.android.application.util.TekCast;
import com.tenkiv.tekdaqc.communication.ascii.message.parsing.ASCIIDigitalOutputDataMessage;
import com.tenkiv.tekdaqc.communication.command.queue.IQueueObject;
import com.tenkiv.tekdaqc.communication.command.queue.QueueCallback;
import com.tenkiv.tekdaqc.communication.command.queue.Task;
import com.tenkiv.tekdaqc.communication.data_points.AnalogInputData;
import com.tenkiv.tekdaqc.communication.data_points.DigitalInputData;
import com.tenkiv.tekdaqc.communication.message.ABoardMessage;
import com.tenkiv.tekdaqc.communication.message.IMessageListener;
import com.tenkiv.tekdaqc.communication.message.MessageBroadcaster;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class which manages the connection between commands and data sent through {@link Tekdaqc} objects and {@link CommunicationService}.
 *
 * @author Tenkiv (software@tenkiv.com)
 * @since v2.0.0.0
 */
public class TekdaqcCommunicationManager implements ServiceConnection, IMessageListener{

    /**
     * The Context.
     */
    private Context mContext;

    /**
     * Callback for notification that service has been connected to.
     */
    private IServiceListener mServiceListener;

    /**
     * This.
     */
    private static TekdaqcCommunicationManager mComManager;

    /**
     * This process's reference to the {@link CommunicationService}'s {@link Messenger}.
     */
    private Messenger mService;

    /**
     * The {@link Messenger} for this client.
     */
    private Messenger mMessenger = new Messenger(new ClientHandler());

    /**
     * A {@link Map} of all {@link ATekdaqc}s and their serial numbers.
     */
    private ConcurrentHashMap<String,ATekdaqc> mTekdaqcMap;


    /**
     * A {@link Map} of all {@link QueueCallback}s and their unique identifiers so that Task callbacks can be conducted across processes.
     */
    private ConcurrentHashMap<Double,QueueCallback> mTaskMap;

    /**
     * Variable used to assign UIDs.
     */
    private static volatile double mUIDAssign = 0;


    /**
     * Singleton constructor to ensure that there exists only one per process.
     *
     * @param context The context.
     * @param listener The listener callback for completion.
     */
    private TekdaqcCommunicationManager(Context context,IServiceListener listener){

        mContext = context;

        mServiceListener = listener;

        mComManager = this;

        mTekdaqcMap = new ConcurrentHashMap<>();

        mTaskMap = new ConcurrentHashMap<>();

        Intent comService = new Intent(context, CommunicationService.class);
        context.bindService(comService, this, Context.BIND_AUTO_CREATE);

    }


    /**
     * Method used for creating this singleton class.
     *
     * @param context The context.
     * @param listener The listener callback for completion.
     */
    public static void startCommunicationService(Context context, IServiceListener listener){
        if(mComManager == null) {
            mComManager = new TekdaqcCommunicationManager(context, listener);

        }else{
            listener.onManagerServiceCreated(mComManager);
        }
    }

    /**
     * Gets the {@link TekdaqcCommunicationManager}.
     *
     * @return The {@link TekdaqcCommunicationManager}, returns null if not created yet.
     */
    public static TekdaqcCommunicationManager getTekdaqcCommunicationsManager(){
        return mComManager;
    }


    /**
     * Unbinds the manager from the service.
     */
    public void stopCommunicationManager() {
        mContext.unbindService(this);

    }


    /**
     * Static method to determine if the {@link CommunicationService} is running.
     *
     * @param context The context.
     * @return The current status of the {@link CommunicationService}.
     */
    public static boolean isComServiceRunning(Context context) {

        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (CommunicationService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    /**
     * Client side call to attempt to execute a command on a {@link ATekdaqc} on the {@link CommunicationService}.
     *
     * @param serial The {@link String} of the serial number.
     * @param command The {@link IQueueObject}.
     */
    public void executeCommand(String serial, IQueueObject command) {

        if(command == null){
            throw new NullPointerException();
        }

        Bundle dataBundle = new Bundle();
        dataBundle.putString(TekCast.SERVICE_SERIAL_KEY, serial);
        dataBundle.putSerializable(TekCast.SERVICE_COMMAND_KEY,command);
        Message msg = Message.obtain(null,TekCast.SERVICE_MSG_COMMAND);
        msg.setData(dataBundle);
        msg.replyTo = mMessenger;

        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    /**
     * Client side call to attempt to execute a {@link Task} on a {@link ATekdaqc} on the {@link CommunicationService}.
     *
     * @param serial The {@link String} of the serial number.
     * @param task The {@link Task}.
     */
    public void executeTask(String serial, Task task){

        if(task == null){
            throw new NullPointerException();
        }

        List<IQueueObject> commandList = task.getCommandList();

        for(IQueueObject object:commandList){
            if(object instanceof QueueCallback){
                mTaskMap.put(mUIDAssign, (QueueCallback) object);
                ((QueueCallback) object).setUID(mUIDAssign);
                mUIDAssign++;
            }
        }

        Bundle dataBundle = new Bundle();
        dataBundle.putString(TekCast.SERVICE_SERIAL_KEY, serial);
        dataBundle.putSerializable(TekCast.SERVICE_TASK_KEY,(Serializable) commandList);
        Message msg = Message.obtain(null,TekCast.SERVICE_MSG_TASK);
        msg.setData(dataBundle);
        msg.replyTo = mMessenger;

        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    /**
     * Call to have the {@link CommunicationService} attempt to connect to the specified {@link ATekdaqc}.
     *
     * @param tekdaqc The {@link ATekdaqc} to connect to.
     */
    public void connectToTekdaqc(ATekdaqc tekdaqc){

        if(tekdaqc == null){
            throw new NullPointerException();
        }

        mTekdaqcMap.put(tekdaqc.getSerialNumber(),tekdaqc);

        Bundle dataBundle = new Bundle();
        dataBundle.putSerializable(TekCast.SERVICE_TEKDAQC_CONNECT,tekdaqc.getLocatorResponse());
        Message msg = Message.obtain(null,TekCast.SERVICE_MSG_CONNECT);
        msg.setData(dataBundle);
        msg.replyTo = mMessenger;

        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to attempt to safely disconnect from the selected {@link ATekdaqc}.
     *
     * @param tekdaqc The {@link ATekdaqc} to disconnect from.
     */
    public void disconnectFromTekdaqc(ATekdaqc tekdaqc){

        if(tekdaqc == null){
            throw new NullPointerException();
        }

        Bundle dataBundle = new Bundle();
        dataBundle.putSerializable(TekCast.SERVICE_TEKDAQC_DISCONNECT,tekdaqc);
        Message msg = Message.obtain(null,TekCast.SERVICE_MSG_DISCONNECT);
        msg.setData(dataBundle);
        msg.replyTo = mMessenger;

        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }



    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = new Messenger(service);

        mServiceListener.onManagerServiceCreated(this);
    }


    @Override
    public void onServiceDisconnected(ComponentName name) {}


    @Override
    public void onErrorMessageReceived(ATekdaqc tekdaqc, ABoardMessage message) {
        MessageBroadcaster.getInstance().broadcastMessage(tekdaqc,message);
    }


    @Override
    public void onStatusMessageReceived(ATekdaqc tekdaqc, ABoardMessage message) {
        MessageBroadcaster.getInstance().broadcastMessage(tekdaqc,message);
    }


    @Override
    public void onDebugMessageReceived(ATekdaqc tekdaqc, ABoardMessage message) {
        MessageBroadcaster.getInstance().broadcastMessage(tekdaqc,message);

    }


    @Override
    public void onCommandDataMessageReceived(ATekdaqc tekdaqc, ABoardMessage message) {
        MessageBroadcaster.getInstance().broadcastMessage(tekdaqc,message);
    }


    @Override
    public void onAnalogInputDataReceived(ATekdaqc tekdaqc, AnalogInputData data) {
        MessageBroadcaster.getInstance().broadcastAnalogInputDataPoint(tekdaqc,data);
    }



    @Override
    public void onDigitalInputDataReceived(ATekdaqc tekdaqc, DigitalInputData data) {
        MessageBroadcaster.getInstance().broadcastDigitalInputDataPoint(tekdaqc,data);
    }


    @Override
    public void onDigitalOutputDataReceived(ATekdaqc tekdaqc, boolean[] data) {
        MessageBroadcaster.getInstance().broadcastMessage(tekdaqc,new ASCIIDigitalOutputDataMessage(data));
    }


    private class ClientHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){

                case TekCast.TEKDAQC_ANALOG_INPUT_MESSAGE:
                    onAnalogInputDataReceived(
                            mTekdaqcMap.get(msg.getData().getString(TekCast.DATA_MESSSAGE_TEKDAQC)),
                            (AnalogInputData)msg.getData().getSerializable(TekCast.DATA_MESSSAGE));
                    break;

                case TekCast.TEKDAQC_COMMAND_MESSAGE:
                    onCommandDataMessageReceived(
                            mTekdaqcMap.get(msg.getData().getString(TekCast.DATA_MESSSAGE_TEKDAQC)),
                            (ABoardMessage)msg.getData().getSerializable(TekCast.DATA_MESSSAGE));
                    break;

                case TekCast.TEKDAQC_DEBUG_MESSAGE:
                    onDebugMessageReceived(
                            mTekdaqcMap.get(msg.getData().getString(TekCast.DATA_MESSSAGE_TEKDAQC)),
                            (ABoardMessage)msg.getData().getSerializable(TekCast.DATA_MESSSAGE));
                    break;

                case TekCast.TEKDAQC_DIGITAL_INPUT_MESSAGE:
                    onDigitalInputDataReceived(
                            mTekdaqcMap.get(msg.getData().getString(TekCast.DATA_MESSSAGE_TEKDAQC)),
                            (DigitalInputData)msg.getData().getSerializable(TekCast.DATA_MESSSAGE));
                    break;

                case TekCast.TEKDAQC_DIGITAL_OUTPUT_MESSAGE:
                    onDigitalOutputDataReceived(
                            mTekdaqcMap.get(msg.getData().getString(TekCast.DATA_MESSSAGE_TEKDAQC)),
                            msg.getData().getBooleanArray(TekCast.DATA_MESSSAGE));
                    break;

                case TekCast.TEKDAQC_ERROR_MESSAGE:
                    onErrorMessageReceived(
                            mTekdaqcMap.get(msg.getData().getString(TekCast.DATA_MESSSAGE_TEKDAQC)),
                            (ABoardMessage)msg.getData().getSerializable(TekCast.DATA_MESSSAGE));
                    break;

                case TekCast.TEKDAQC_STATUS_MESSAGE:
                    onStatusMessageReceived(
                            mTekdaqcMap.get(msg.getData().getString(TekCast.DATA_MESSSAGE_TEKDAQC)),
                            (ABoardMessage)msg.getData().getSerializable(TekCast.DATA_MESSSAGE));
                    break;

                case TekCast.TEKDAQC_TASK_COMPLETE:
                    mTaskMap.get(
                            msg.getData().getDouble(TekCast.DATA_MESSSAGE_UID)).success(
                            mTekdaqcMap.get(msg.getData().getString(TekCast.DATA_MESSSAGE_TEKDAQC)));
                    break;

                case TekCast.TEKDAQC_TASK_FAILURE:
                    mTaskMap.get(
                            msg.getData().getDouble(TekCast.DATA_MESSSAGE_UID)).failure(
                            mTekdaqcMap.get(msg.getData().getString(TekCast.DATA_MESSSAGE_TEKDAQC)));
                    break;


            }
        }
    }
}

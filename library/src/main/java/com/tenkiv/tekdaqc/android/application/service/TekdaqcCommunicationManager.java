package com.tenkiv.tekdaqc.android.application.service;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.*;
import android.util.Log;
import com.tenkiv.tekdaqc.ATekdaqc;
import com.tenkiv.tekdaqc.android.application.client.TaskQueuePlaceholder;
import com.tenkiv.tekdaqc.android.application.util.ICommunicationListener;
import com.tenkiv.tekdaqc.android.application.util.IServiceListener;
import com.tenkiv.tekdaqc.android.application.util.TekCast;
import com.tenkiv.tekdaqc.communication.ascii.message.parsing.ASCIIDigitalOutputDataMessage;
import com.tenkiv.tekdaqc.communication.command.queue.IQueueObject;
import com.tenkiv.tekdaqc.communication.command.queue.QueueCallback;
import com.tenkiv.tekdaqc.communication.command.queue.Task;
import com.tenkiv.tekdaqc.communication.command.queue.values.ABaseQueueVal;
import com.tenkiv.tekdaqc.communication.data_points.AnalogInputData;
import com.tenkiv.tekdaqc.communication.data_points.DigitalInputData;
import com.tenkiv.tekdaqc.communication.message.ABoardMessage;
import com.tenkiv.tekdaqc.communication.message.IMessageListener;
import com.tenkiv.tekdaqc.communication.message.MessageBroadcaster;
import com.tenkiv.tekdaqc.communication.tasks.ITaskComplete;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


public class TekdaqcCommunicationManager implements ServiceConnection, IMessageListener{

    private Context mContext;

    private IServiceListener mServiceListener;

    private static TekdaqcCommunicationManager mComManager;

    private Messenger mService;

    private Messenger mMessenger = new Messenger(new ClientHandler());

    private ConcurrentHashMap<String,ATekdaqc> mTekdaqcMap;

    private ConcurrentHashMap<String,ArrayList<IMessageListener>> mListenerMap;

    private ConcurrentHashMap<Double,QueueCallback> mTaskMap;

    private static volatile double mUIDAssign = 0;


    private TekdaqcCommunicationManager(Context context,IServiceListener listener){

        mContext = context;

        mServiceListener = listener;

        mComManager = this;

        mTekdaqcMap = new ConcurrentHashMap<>();

        mListenerMap = new ConcurrentHashMap<>();

        mTaskMap = new ConcurrentHashMap<>();

        Intent comService = new Intent(context, CommunicationService.class);
        context.bindService(comService, this, Context.BIND_AUTO_CREATE);

    }


    public static void startCommunicationService(Context context, IServiceListener listener){
        if(mComManager == null) {
            mComManager = new TekdaqcCommunicationManager(context, listener);

        }else{
            listener.onManagerServiceCreated(mComManager);
        }
    }

    public static TekdaqcCommunicationManager getTekdaqcCommunicationsManager(){
        return mComManager;
    }


    public void stopCommunicationManager() throws IOException {
        mContext.unbindService(this);

    }


    public static boolean isComServiceRunning(Context context) {

        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (CommunicationService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    public void executeCommand(String serial, IQueueObject command) throws InterruptedException {

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


    public void disconnectFromTekdaqc(ATekdaqc tekdaqc, ICommunicationListener listener){

        if(tekdaqc == null){
            throw new NullPointerException();
        }

        final MessageBroadcaster broadcaster = MessageBroadcaster.getInstance();
        broadcaster.unRegisterListener(tekdaqc, this);

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


    public void setCommunicationListener(String serial, IMessageListener listener){

        if(mListenerMap.containsKey(serial)){
            if(!mListenerMap.get(serial).contains(listener)) {
                mListenerMap.get(serial).add(listener);
            }

        }else{
            ArrayList<IMessageListener> listenerArrayList = new ArrayList<>();
            listenerArrayList.add(listener);
            mListenerMap.put(serial,listenerArrayList);
        }
    }

    public void removeCommunicationListener(String serial, ICommunicationListener listener){
        if(mListenerMap.get(serial).contains(listener)) {
            mListenerMap.get(serial).remove(listener);
        }
    }


    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {

        mService = new Messenger(service);

        Message msg = Message.obtain(null,TekCast.SERVICE_MSG_REGISTER);
        msg.replyTo = mMessenger;

        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
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
                   /* onTaskSuccess(mTekdaqcMap.get(msg.getData().getString(TekCast.DATA_MESSSAGE_TEKDAQC)));*/
                    break;

                case TekCast.TEKDAQC_TASK_FAILURE:
                    mTaskMap.get(
                            msg.getData().getDouble(TekCast.DATA_MESSSAGE_UID)).failure(
                            mTekdaqcMap.get(msg.getData().getString(TekCast.DATA_MESSSAGE_TEKDAQC)));
                    /*onTaskFailed(mTekdaqcMap.get(msg.getData().getString(TekCast.DATA_MESSSAGE_TEKDAQC)));*/
                    break;


            }
        }
    }
}

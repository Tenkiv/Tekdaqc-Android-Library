package com.tenkiv.tekdaqc.android.application.service;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.*;
import com.tenkiv.tekdaqc.ATekdaqc;
import com.tenkiv.tekdaqc.Task;
import com.tenkiv.tekdaqc.android.application.util.ICommunicationListener;
import com.tenkiv.tekdaqc.android.application.util.IServiceListener;
import com.tenkiv.tekdaqc.android.application.util.TekCast;
import com.tenkiv.tekdaqc.communication.command.ABoardCommand;
import com.tenkiv.tekdaqc.communication.data_points.AnalogInputData;
import com.tenkiv.tekdaqc.communication.data_points.DigitalInputData;
import com.tenkiv.tekdaqc.communication.message.ABoardMessage;
import com.tenkiv.tekdaqc.communication.message.IMessageListener;
import com.tenkiv.tekdaqc.communication.message.MessageBroadcaster;
import com.tenkiv.tekdaqc.communication.tasks.ITaskComplete;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


public class TekdaqcCommunicationManager implements ServiceConnection, IMessageListener, ITaskComplete{

    private Context mContext;

    private IServiceListener mServiceListener;

    private static TekdaqcCommunicationManager mComManager;

    private Messenger mService;

    private Messenger mMessenger = new Messenger(new ClientHandler());

    private ConcurrentHashMap<String,ArrayList<ICommunicationListener>> mListenerMap;

    private ConcurrentHashMap<String,ITaskComplete> mTaskMap;


    public TekdaqcCommunicationManager(Context context,IServiceListener listener){

        mContext = context;

        mServiceListener = listener;

        mComManager = this;

        mTaskMap = new ConcurrentHashMap<>();

        mListenerMap = new ConcurrentHashMap<>();

            Intent comService = new Intent(context, CommunicationService.class);
            context.bindService(comService, this, Context.BIND_AUTO_CREATE);

    }


    public static void startCommunicationService(Context context, IServiceListener listener){
        if(mComManager == null) {
            mComManager = new TekdaqcCommunicationManager(context, listener);

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


    public void executeBoardCommand(String serial, ABoardCommand command) throws InterruptedException {

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


    public void executeTaskCommand(String serial, Task task, ITaskComplete callback){

        if(task == null){
            throw new NullPointerException();
        }

        mTaskMap.put(serial,callback);

        Bundle dataBundle = new Bundle();
        dataBundle.putString(TekCast.SERVICE_SERIAL_KEY, serial);
        dataBundle.putSerializable(TekCast.SERVICE_TASK_KEY,task);
        Message msg = Message.obtain(null,TekCast.SERVICE_MSG_TASK);
        msg.setData(dataBundle);
        msg.replyTo = mMessenger;

        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    public void connectToTekdaqc(ATekdaqc tekdaqc, ICommunicationListener listener){

        if(tekdaqc == null){
            throw new NullPointerException();
        }

        setCommunicationListener(tekdaqc.getSerialNumber(), listener);

        Bundle dataBundle = new Bundle();
        dataBundle.putSerializable(TekCast.SERVICE_TEKDAQC_CONNECT,tekdaqc);
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

        mListenerMap.remove(tekdaqc.getSerialNumber());

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


    public void setCommunicationListener(String serial, ICommunicationListener listener){

        if(mListenerMap.containsKey(serial)){
            if(!mListenerMap.get(serial).contains(listener)) {
                mListenerMap.get(serial).add(listener);
            }

        }else{
            ArrayList<ICommunicationListener> listenerArrayList = new ArrayList<>();
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
    }


    @Override
    public void onServiceDisconnected(ComponentName name) {}


    @Override
    public void onErrorMessageReceived(ATekdaqc tekdaqc, ABoardMessage message) {
        for (ICommunicationListener listener : mListenerMap.get(tekdaqc.getSerialNumber())) {
            listener.onErrorMessageReceived(tekdaqc,message);
        }
    }


    @Override
    public void onStatusMessageReceived(ATekdaqc tekdaqc, ABoardMessage message) {
        for (ICommunicationListener listener : mListenerMap.get(tekdaqc.getSerialNumber())) {
            listener.onStatusMessageReceived(tekdaqc, message);
        }
    }


    @Override
    public void onDebugMessageReceived(ATekdaqc tekdaqc, ABoardMessage message) {
        for (ICommunicationListener listener : mListenerMap.get(tekdaqc.getSerialNumber())) {
            listener.onDebugMessageReceived(tekdaqc, message);
        }

    }


    @Override
    public void onCommandDataMessageReceived(ATekdaqc tekdaqc, ABoardMessage message) {
        for (ICommunicationListener listener : mListenerMap.get(tekdaqc.getSerialNumber())) {
            listener.onCommandDataMessageReceived(tekdaqc, message);
        }
    }


    @Override
    public void onAnalogInputDataReceived(ATekdaqc tekdaqc, AnalogInputData data) {
        for (ICommunicationListener listener : mListenerMap.get(tekdaqc.getSerialNumber())) {
            listener.onAnalogInputDataReceived(tekdaqc, data);
        }
    }


    @Override
    public void onAnalogInputDataReceived(ATekdaqc tekdaqc, List<AnalogInputData> data) {
        for (ICommunicationListener listener : mListenerMap.get(tekdaqc.getSerialNumber())) {
            listener.onAnalogInputDataReceived(tekdaqc, data);
        }
    }


    @Override
    public void onDigitalInputDataReceived(ATekdaqc tekdaqc, DigitalInputData data) {
        for (ICommunicationListener listener : mListenerMap.get(tekdaqc.getSerialNumber())) {
            listener.onDigitalInputDataReceived(tekdaqc, data);
        }

    }


    @Override
    public void onDigitalOutputDataReceived(ATekdaqc tekdaqc, ABoardMessage message) {
        for (ICommunicationListener listener : mListenerMap.get(tekdaqc.getSerialNumber())) {
            listener.onDigitalOutputDataReceived(tekdaqc, message);
        }
    }


    @Override
    public void onTaskSuccess(ATekdaqc tekdaqc) {
        mTaskMap.get(tekdaqc.getSerialNumber()).onTaskSuccess(tekdaqc);
        mTaskMap.remove(tekdaqc.getSerialNumber());
    }


    @Override
    public void onTaskFailed(ATekdaqc tekdaqc) {
        mTaskMap.get(tekdaqc.getSerialNumber()).onTaskFailed(tekdaqc);
        mTaskMap.remove(tekdaqc.getSerialNumber());
    }


    private class ClientHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){

                case TekCast.TEKDAQC_ANALOG_INPUT_MESSAGE:
                    onAnalogInputDataReceived(
                            (ATekdaqc)msg.getData().getSerializable(TekCast.DATA_MESSSAGE_TEKDAQC),
                            (AnalogInputData)msg.getData().getSerializable(TekCast.DATA_MESSSAGE));
                    break;

                case TekCast.TEKDAQC_COMMAND_MESSAGE:
                    onCommandDataMessageReceived(
                            (ATekdaqc)msg.getData().getSerializable(TekCast.DATA_MESSSAGE_TEKDAQC),
                            (ABoardMessage)msg.getData().getSerializable(TekCast.DATA_MESSSAGE));
                    break;

                case TekCast.TEKDAQC_DEBUG_MESSAGE:
                    onDebugMessageReceived(
                            (ATekdaqc)msg.getData().getSerializable(TekCast.DATA_MESSSAGE_TEKDAQC),
                            (ABoardMessage)msg.getData().getSerializable(TekCast.DATA_MESSSAGE));
                    break;

                case TekCast.TEKDAQC_DIGITAL_INPUT_MESSAGE:
                    onDigitalInputDataReceived(
                            (ATekdaqc)msg.getData().getSerializable(TekCast.DATA_MESSSAGE_TEKDAQC),
                            (DigitalInputData)msg.getData().getSerializable(TekCast.DATA_MESSSAGE));
                    break;

                case TekCast.TEKDAQC_DIGITAL_OUTPUT_MESSAGE:
                    onDigitalOutputDataReceived(
                            (ATekdaqc)msg.getData().getSerializable(TekCast.DATA_MESSSAGE_TEKDAQC),
                            (ABoardMessage)msg.getData().getSerializable(TekCast.DATA_MESSSAGE));
                    break;

                case TekCast.TEKDAQC_ERROR_MESSAGE:
                    onErrorMessageReceived(
                            (ATekdaqc)msg.getData().getSerializable(TekCast.DATA_MESSSAGE_TEKDAQC),
                            (ABoardMessage)msg.getData().getSerializable(TekCast.DATA_MESSSAGE));
                    break;

                case TekCast.TEKDAQC_STATUS_MESSAGE:
                    onStatusMessageReceived(
                            (ATekdaqc)msg.getData().getSerializable(TekCast.DATA_MESSSAGE_TEKDAQC),
                            (ABoardMessage)msg.getData().getSerializable(TekCast.DATA_MESSSAGE));
                    break;

                case TekCast.TEKDAQC_TASK_COMPLETE:
                    onTaskSuccess((ATekdaqc)msg.getData().getSerializable(TekCast.DATA_MESSSAGE_TEKDAQC));
                    break;

                case TekCast.TEKDAQC_TASK_FAILURE:
                    onTaskFailed((ATekdaqc)msg.getData().getSerializable(TekCast.DATA_MESSSAGE_TEKDAQC));
                    break;

                case TekCast.TEKDAQC_CONNECTED:
                    ATekdaqc tekdaqcConnected = (ATekdaqc) msg.getData().getSerializable(TekCast.SERVICE_TEKDAQC_CONNECT);
                    for(ICommunicationListener listener:mListenerMap.get(tekdaqcConnected.getSerialNumber())){
                        listener.onTekdaqcConnected(tekdaqcConnected);
                    }
                    break;

                case TekCast.TEKDAQC_DISCONNECTED:
                    ATekdaqc tekdaqcDisconnected = (ATekdaqc) msg.getData().getSerializable(TekCast.SERVICE_TEKDAQC_DISCONNECT);
                    for(ICommunicationListener listener:mListenerMap.get(tekdaqcDisconnected.getSerialNumber())){
                        listener.onTekdaqcDisconnected(tekdaqcDisconnected);
                    }
                    break;
            }
        }
    }
}

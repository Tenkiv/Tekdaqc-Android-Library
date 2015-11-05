package com.tenkiv.tekdaqc.android.application.service;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.util.Log;
import com.tenkiv.tekdaqc.ATekdaqc;
import com.tenkiv.tekdaqc.Task;
import com.tenkiv.tekdaqc.android.application.util.TekCast;
import com.tenkiv.tekdaqc.communication.ascii.ASCIICommunicationSession;
import com.tenkiv.tekdaqc.communication.command.ABoardCommand;
import com.tenkiv.tekdaqc.communication.data_points.AnalogInputData;
import com.tenkiv.tekdaqc.communication.data_points.DigitalInputData;
import com.tenkiv.tekdaqc.communication.message.ABoardMessage;
import com.tenkiv.tekdaqc.communication.message.IMessageListener;
import com.tenkiv.tekdaqc.communication.message.MessageBroadcaster;
import com.tenkiv.tekdaqc.communication.tasks.ITask;
import com.tenkiv.tekdaqc.communication.tasks.ITaskComplete;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;



public class CommunicationService extends Service implements IMessageListener, ITaskComplete{

    private ServiceHandler mServiceHandler = new ServiceHandler();

    private Messenger mComMessenger = new Messenger(mServiceHandler);

    private ConcurrentHashMap<String,Messenger> mMessengerMap = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String,ASCIICommunicationSession> mSessionMap = new ConcurrentHashMap<>();

    List<Messenger> mMessengerList = new ArrayList<Messenger>();

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);

    }

    public CommunicationService() {
        super();
    }

    public void executeCommand(String serial,ABoardCommand command){
        mSessionMap.get(serial).executeCommand(command);
    }

    public void executeTask(final String serial, ITask task){
        task.setSession(mSessionMap.get(serial));
        task.execute(this);

    }

    public void haltComService(String serial) throws IOException {
        mSessionMap.get(serial).disconnect();
    }

    public void connectToTekdaqc(ATekdaqc tekdaqc, Messenger messenger){
        new ServConThread(tekdaqc,messenger,this).start();

    }

    public void disconnectFromTekdaqc(ATekdaqc tekdaqc, Messenger messenger){
        mSessionMap.get(tekdaqc.getSerialNumber()).executeCommand(tekdaqc.halt());
        mSessionMap.get(tekdaqc.getSerialNumber()).executeCommand(tekdaqc.disconnectCleanly());
        mSessionMap.remove(tekdaqc.getSerialNumber());

        Message message = Message.obtain();
        Bundle bundle = new Bundle();
        bundle.putSerializable(TekCast.SERVICE_TEKDAQC_DISCONNECT, tekdaqc);
        message.setData(bundle);
        message.what = TekCast.TEKDAQC_DISCONNECTED;
        try {
            messenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void sendMessageToRegisteredListeners(Message message){
        for (Messenger messenger : mMessengerList) {
            if (messenger != null) {
                try {
                    messenger.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }else{
                mMessengerList.remove(messenger);
            }
        }
    };

    @Override
    public void onTaskSuccess(ATekdaqc tekdaqc) {
        if (mMessengerList.size() > 0) {
            Bundle dataBundle = new Bundle();
            dataBundle.putSerializable(TekCast.DATA_MESSSAGE_TEKDAQC, tekdaqc);

            Message dataMessage = Message.obtain();
            dataMessage.what = TekCast.TEKDAQC_TASK_COMPLETE;
            dataMessage.setData(dataBundle);

            sendMessageToRegisteredListeners(dataMessage);
        }
    }

    @Override
    public void onTaskFailed(ATekdaqc tekdaqc) {
        if (mMessengerList.size() > 0) {
            Bundle dataBundle = new Bundle();
            dataBundle.putSerializable(TekCast.DATA_MESSSAGE_TEKDAQC, tekdaqc);

            Message dataMessage = Message.obtain();
            dataMessage.what = TekCast.TEKDAQC_TASK_FAILURE;
            dataMessage.setData(dataBundle);

            sendMessageToRegisteredListeners(dataMessage);
        }
    }

    private class ServConThread extends Thread{

        ATekdaqc mTekdaqc;
        Messenger mMessenger;
        IMessageListener mListener;

        public ServConThread(ATekdaqc tekdaqc, Messenger messenger, IMessageListener listener){
            mTekdaqc = tekdaqc;
            mMessenger = messenger;
            mListener = listener;
        }
        @Override
        public void run() {
            super.run();
            MessageBroadcaster broadcaster = MessageBroadcaster.getInstance();
            broadcaster.registerListener(mTekdaqc, mListener);

            ASCIICommunicationSession session = new ASCIICommunicationSession(mTekdaqc);
            mSessionMap.put(mTekdaqc.getSerialNumber(),session);
            try {
                session.connect(ATekdaqc.CONNECTION_METHOD.ETHERNET);
                mMessengerMap.put(mTekdaqc.getSerialNumber(),mMessenger);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Message message = Message.obtain();
            Bundle bundle = new Bundle();
            bundle.putSerializable(TekCast.SERVICE_TEKDAQC_CONNECT, mTekdaqc);
            message.setData(bundle);
            message.what = TekCast.TEKDAQC_CONNECTED;
            try {
                mMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mComMessenger.getBinder();
    }

    private class ServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){

                case TekCast.SERVICE_MSG_REGISTER:
                    mMessengerList.add(msg.replyTo);
                    break;

                case TekCast.SERVICE_MSG_UNREGISTER:
                    mMessengerList.remove(msg.replyTo);
                    break;

                case TekCast.SERVICE_MSG_COMMAND:
                    ABoardCommand command = (ABoardCommand) msg.getData().getSerializable(TekCast.SERVICE_COMMAND_KEY);
                    String commandSerial = msg.getData().getString(TekCast.SERVICE_SERIAL_KEY);
                    executeCommand(commandSerial,command);
                    break;

                case TekCast.SERVICE_MSG_TASK:
                    Task task = (Task) msg.getData().getSerializable(TekCast.SERVICE_TASK_KEY);
                    String taskSerial = msg.getData().getString(TekCast.SERVICE_SERIAL_KEY);
                    executeTask(taskSerial, task);
                    break;

                case TekCast.SERVICE_MSG_CONNECT:
                    connectToTekdaqc(((ATekdaqc)msg.getData().getSerializable(TekCast.SERVICE_TEKDAQC_CONNECT)),msg.replyTo);
                    break;

                case TekCast.SERVICE_MSG_DISCONNECT:
                    disconnectFromTekdaqc((ATekdaqc) msg.getData().getSerializable(TekCast.SERVICE_TEKDAQC_DISCONNECT),msg.replyTo);
                    break;

            }

        }
    }

    @Override
    public void onErrorMessageReceived(ATekdaqc tekdaqc, ABoardMessage message) {
        if (mMessengerList.size() > 0) {
            Bundle dataBundle = new Bundle();
            dataBundle.putSerializable(TekCast.DATA_MESSSAGE_TEKDAQC, tekdaqc);
            dataBundle.putSerializable(TekCast.DATA_MESSSAGE, message);

            Message dataMessage = Message.obtain();
            dataMessage.what = TekCast.TEKDAQC_ERROR_MESSAGE;
            dataMessage.setData(dataBundle);

            sendMessageToRegisteredListeners(dataMessage);
        }
    }

    @Override
    public void onStatusMessageReceived(ATekdaqc tekdaqc, ABoardMessage message) {
        if (mMessengerList.size() > 0) {
            Bundle dataBundle = new Bundle();
            dataBundle.putSerializable(TekCast.DATA_MESSSAGE_TEKDAQC, tekdaqc);
            dataBundle.putSerializable(TekCast.DATA_MESSSAGE, message);

            Message dataMessage = Message.obtain();
            dataMessage.what = TekCast.TEKDAQC_STATUS_MESSAGE;
            dataMessage.setData(dataBundle);

            sendMessageToRegisteredListeners(dataMessage);
        }
    }

    @Override
    public void onDebugMessageReceived(ATekdaqc tekdaqc, ABoardMessage message) {
        if (mMessengerList.size() > 0) {
            Bundle dataBundle = new Bundle();
            dataBundle.putSerializable(TekCast.DATA_MESSSAGE_TEKDAQC, tekdaqc);
            dataBundle.putSerializable(TekCast.DATA_MESSSAGE, message);

            Message dataMessage = Message.obtain();
            dataMessage.what = TekCast.TEKDAQC_DEBUG_MESSAGE;
            dataMessage.setData(dataBundle);

            sendMessageToRegisteredListeners(dataMessage);
        }
    }

    @Override
    public void onCommandDataMessageReceived(ATekdaqc tekdaqc, ABoardMessage message) {
        if (mMessengerList.size() > 0) {
            Bundle dataBundle = new Bundle();
            dataBundle.putSerializable(TekCast.DATA_MESSSAGE_TEKDAQC, tekdaqc);
            dataBundle.putSerializable(TekCast.DATA_MESSSAGE, message);

            Message dataMessage = Message.obtain();
            dataMessage.what = TekCast.TEKDAQC_COMMAND_MESSAGE;
            dataMessage.setData(dataBundle);

            sendMessageToRegisteredListeners(dataMessage);
        }
    }

    @Override
    public void onAnalogInputDataReceived(ATekdaqc tekdaqc, AnalogInputData data) {
        if (mMessengerList.size() > 0) {
            Bundle dataBundle = new Bundle();
            dataBundle.putSerializable(TekCast.DATA_MESSSAGE_TEKDAQC, tekdaqc);
            dataBundle.putSerializable(TekCast.DATA_MESSSAGE, data);

            Message dataMessage = Message.obtain();
            dataMessage.what = TekCast.TEKDAQC_ANALOG_INPUT_MESSAGE;
            dataMessage.setData(dataBundle);

            sendMessageToRegisteredListeners(dataMessage);
        }
    }

    @Override
    public void onAnalogInputDataReceived(ATekdaqc tekdaqc, List<AnalogInputData> data) {
        if (mMessengerList.size() > 0) {
            for(AnalogInputData listData: data) {
                Bundle dataBundle = new Bundle();
                dataBundle.putSerializable(TekCast.DATA_MESSSAGE_TEKDAQC, tekdaqc);
                dataBundle.putSerializable(TekCast.DATA_MESSSAGE, listData);

                Message dataMessage = Message.obtain();
                dataMessage.what = TekCast.TEKDAQC_ANALOG_INPUT_MESSAGE;
                dataMessage.setData(dataBundle);

                sendMessageToRegisteredListeners(dataMessage);
            }
        }
    }

    @Override
    public void onDigitalInputDataReceived(ATekdaqc tekdaqc, DigitalInputData data) {
        if (mMessengerList.size() > 0) {
            Bundle dataBundle = new Bundle();
            dataBundle.putSerializable(TekCast.DATA_MESSSAGE_TEKDAQC, tekdaqc);
            dataBundle.putSerializable(TekCast.DATA_MESSSAGE, data);

            Message dataMessage = Message.obtain();
            dataMessage.what = TekCast.TEKDAQC_DIGITAL_INPUT_MESSAGE;
            dataMessage.setData(dataBundle);

            sendMessageToRegisteredListeners(dataMessage);
        }
    }

    @Override
    public void onDigitalOutputDataReceived(ATekdaqc tekdaqc, ABoardMessage message) {
        if (mMessengerList.size() > 0) {
            Bundle dataBundle = new Bundle();
            dataBundle.putSerializable(TekCast.DATA_MESSSAGE_TEKDAQC, tekdaqc);
            dataBundle.putSerializable(TekCast.DATA_MESSSAGE, message);

            Message dataMessage = Message.obtain();
            dataMessage.what = TekCast.TEKDAQC_DIGITAL_OUTPUT_MESSAGE;
            dataMessage.setData(dataBundle);

            sendMessageToRegisteredListeners(dataMessage);
        }
    }


}


package com.tenkiv.tekdaqc.android.application.service;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import com.tenkiv.tekdaqc.ATekdaqc;
import com.tenkiv.tekdaqc.android.application.util.ServiceConnectionThread;
import com.tenkiv.tekdaqc.android.application.util.TekCast;
import com.tenkiv.tekdaqc.communication.command.queue.IQueueObject;
import com.tenkiv.tekdaqc.communication.command.queue.QueueCallback;
import com.tenkiv.tekdaqc.communication.data_points.AnalogInputData;
import com.tenkiv.tekdaqc.communication.data_points.DigitalInputData;
import com.tenkiv.tekdaqc.communication.message.ABoardMessage;
import com.tenkiv.tekdaqc.communication.message.IMessageListener;
import com.tenkiv.tekdaqc.communication.tasks.ITaskComplete;
import com.tenkiv.tekdaqc.locator.LocatorResponse;
import com.tenkiv.tekdaqc.revd.Tekdaqc_RevD;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Class which handles all telnet connections and network operations for communicating with the tekdaqc. This allows for multiple different apps
 * to all share telnet connections and shrink resource use.
 *
 * @author Ellis Berry (ejberry@tenkiv.com)
 * @since v2.0.0.0
 */
public class CommunicationService extends Service implements IMessageListener{

    /**
     * The {@link Handler} for all {@link Messenger callbacks}.
     */
    private ServiceHandler mServiceHandler = new ServiceHandler();

    /**
     * The {@link Messenger} for this {@link Service} which all apps will communicate through.
     */
    private Messenger mComMessenger = new Messenger(mServiceHandler);

    private ConcurrentHashMap<String,Messenger> mMessengerMap = new ConcurrentHashMap<>();

    /**
     * The {@link Map} of all connected {@link ATekdaqc}s.
     */
    private ConcurrentHashMap<String,ATekdaqc> mTekdaqcMap = new ConcurrentHashMap<>();

    /**
     * The {@link List} of all connected {@link Messenger}s.
     */
    private List<Messenger> mMessengerList = new ArrayList<Messenger>();

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);

    }

    /**
     * Empty constructor required for remote {@link Service}.
     */
    public CommunicationService() {
        super();
    }

    /**
     * The {@link Service} side method to queue a {@link IQueueObject} for execution on a {@link ATekdaqc}.
     *
     * @param serial The {@link String} of the {@link ATekdaqc}'s serial number.
     * @param command The {@link IQueueObject} to be queued.
     */
    public void executeCommand(String serial,IQueueObject command){
        mTekdaqcMap.get(serial).queueCommand(command);
    }

    /**
     * The {@link Service} side method to queue a {@link List} of {@link IQueueObject} for execution and provide callbacks to client side {@link ITaskComplete} interfaces.
     *
     * @param serial The {@link String} of the {@link ATekdaqc}'s serial number.
     * @param task The {@link List} of {@link IQueueObject}s to be queued.
     */
    public void executeTask(final String serial, List<IQueueObject> task){

        for(final IQueueObject object: task){
            if(object instanceof QueueCallback){
                ((QueueCallback) object).addCallback(new ITaskComplete() {
                    @Override
                    public void onTaskSuccess(ATekdaqc aTekdaqc) {
                        if (mMessengerList.size() > 0) {
                            Bundle dataBundle = new Bundle();
                            dataBundle.putString(TekCast.DATA_MESSSAGE_TEKDAQC, serial);
                            dataBundle.putDouble(TekCast.DATA_MESSSAGE_UID, ((QueueCallback) object).getUID());

                            Message dataMessage = Message.obtain();
                            dataMessage.what = TekCast.TEKDAQC_TASK_COMPLETE;
                            dataMessage.setData(dataBundle);

                            sendMessageToRegisteredListeners(dataMessage);
                        }
                    }

                    @Override
                    public void onTaskFailed(ATekdaqc aTekdaqc) {
                        if (mMessengerList.size() > 0) {
                            Bundle dataBundle = new Bundle();
                            dataBundle.putString(TekCast.DATA_MESSSAGE_TEKDAQC, serial);
                            dataBundle.putDouble(TekCast.DATA_MESSSAGE_UID, ((QueueCallback) object).getUID());

                            Message dataMessage = Message.obtain();
                            dataMessage.what = TekCast.TEKDAQC_TASK_FAILURE;
                            dataMessage.setData(dataBundle);

                            sendMessageToRegisteredListeners(dataMessage);
                        }
                    }
                });
            }
            executeCommand(serial,object);
        }
    }

    /**
     * Method to generate the correct {@link ATekdaqc} based upon the {@link LocatorResponse}.
     *
     * @param response The {@link LocatorResponse} of the desired Tekdaqc.
     * @return A {@link ATekdaqc} of the appropriate type.
     */
    public ATekdaqc generateTekdaqc(LocatorResponse response){
        ATekdaqc tekdaqc = null;
        switch (response.getType()){
            case 'D':
            case 'E':
                tekdaqc = new Tekdaqc_RevD(response);
                break;
            default:
                tekdaqc = new Tekdaqc_RevD(response);
                break;
        }

        return tekdaqc;
    }

    /**
     * Method to halt communication with a {@link ATekdaqc}.
     *
     * @param serial The {@link String} of the Tekdaqc's serial number.
     * @throws IOException
     */
    public void haltComService(String serial) throws IOException {
        mTekdaqcMap.get(serial).disconnect();
    }

    /**
     * Method to connect to a tekdaqc given a {@link LocatorResponse}. This also serves to add a reference to the client side {@link Messenger} if it doesnt exist already.
     * @param response The {@link LocatorResponse} of the tekdaqc to be connected to.
     * @param messenger The {@link Messenger} associated with the connection.
     */
    public void connectToTekdaqc(LocatorResponse response, Messenger messenger){
        ATekdaqc tekdaqc = generateTekdaqc(response);
        tekdaqc.registerListener(this);
        mTekdaqcMap.put(tekdaqc.getSerialNumber(),tekdaqc);
        new ServiceConnectionThread(tekdaqc).start();
        mMessengerMap.put(tekdaqc.getSerialNumber(),messenger);

    }

    /**
     * Method to disconnect from a {@link ATekdaqc}.
     *
     * @param tekdaqc The {@link ATekdaqc} to disconnect from.
     * @param messenger
     */
    public void disconnectFromTekdaqc(ATekdaqc tekdaqc, Messenger messenger){
        tekdaqc.unregisterListener(this);
        mTekdaqcMap.get(tekdaqc.getSerialNumber()).halt();
        mTekdaqcMap.get(tekdaqc.getSerialNumber()).disconnectCleanly();
        mTekdaqcMap.remove(tekdaqc.getSerialNumber());

    }

    /**
     * Method to iterate through all registered listeners and send the {@link Message}.
     *
     * @param message The {@link Message} to be sent.
     */
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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mComMessenger.getBinder();
    }

    /**
     * Inner class which acts as the {@link Handler} for the {@link CommunicationService}.
     */
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
                    IQueueObject command = (IQueueObject) msg.getData().getSerializable(TekCast.SERVICE_COMMAND_KEY);
                    String commandSerial = msg.getData().getString(TekCast.SERVICE_SERIAL_KEY);
                    executeCommand(commandSerial,command);
                    break;

                case TekCast.SERVICE_MSG_TASK:
                        List<IQueueObject> task = (List<IQueueObject>) msg.getData().getSerializable(TekCast.SERVICE_TASK_KEY);
                        String taskSerial = msg.getData().getString(TekCast.SERVICE_SERIAL_KEY);
                        executeTask(taskSerial, task);

                    break;

                case TekCast.SERVICE_MSG_CONNECT:
                    connectToTekdaqc(((LocatorResponse) msg.getData().getSerializable(TekCast.SERVICE_TEKDAQC_CONNECT)),msg.replyTo);
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
            dataBundle.putString(TekCast.DATA_MESSSAGE_TEKDAQC, tekdaqc.getSerialNumber());
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
            dataBundle.putString(TekCast.DATA_MESSSAGE_TEKDAQC, tekdaqc.getSerialNumber());
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
            dataBundle.putString(TekCast.DATA_MESSSAGE_TEKDAQC, tekdaqc.getSerialNumber());
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
            dataBundle.putString(TekCast.DATA_MESSSAGE_TEKDAQC, tekdaqc.getSerialNumber());
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
            dataBundle.putString(TekCast.DATA_MESSSAGE_TEKDAQC, tekdaqc.getSerialNumber());
            dataBundle.putSerializable(TekCast.DATA_MESSSAGE, data);

            Message dataMessage = Message.obtain();
            dataMessage.what = TekCast.TEKDAQC_ANALOG_INPUT_MESSAGE;
            dataMessage.setData(dataBundle);

            sendMessageToRegisteredListeners(dataMessage);
        }
    }


    @Override
    public void onDigitalInputDataReceived(ATekdaqc tekdaqc, DigitalInputData data) {
        if (mMessengerList.size() > 0) {
            Bundle dataBundle = new Bundle();
            dataBundle.putString(TekCast.DATA_MESSSAGE_TEKDAQC, tekdaqc.getSerialNumber());
            dataBundle.putSerializable(TekCast.DATA_MESSSAGE, data);

            Message dataMessage = Message.obtain();
            dataMessage.what = TekCast.TEKDAQC_DIGITAL_INPUT_MESSAGE;
            dataMessage.setData(dataBundle);

            sendMessageToRegisteredListeners(dataMessage);
        }
    }

    @Override
    public void onDigitalOutputDataReceived(ATekdaqc tekdaqc, boolean[] data) {
        if (mMessengerList.size() > 0) {
            Bundle dataBundle = new Bundle();
            dataBundle.putString(TekCast.DATA_MESSSAGE_TEKDAQC, tekdaqc.getSerialNumber());
            dataBundle.putBooleanArray(TekCast.DATA_MESSSAGE, data);

            Message dataMessage = Message.obtain();
            dataMessage.what = TekCast.TEKDAQC_DIGITAL_OUTPUT_MESSAGE;
            dataMessage.setData(dataBundle);

            sendMessageToRegisteredListeners(dataMessage);
        }
    }


}


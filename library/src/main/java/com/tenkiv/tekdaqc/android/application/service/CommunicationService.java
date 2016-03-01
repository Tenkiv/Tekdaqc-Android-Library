package com.tenkiv.tekdaqc.android.application.service;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.util.Log;
import com.tenkiv.tekdaqc.android.application.service.remote_parceling.*;
import com.tenkiv.tekdaqc.android.application.util.ServiceConnectionThread;
import com.tenkiv.tekdaqc.android.application.util.TekCast;
import com.tenkiv.tekdaqc.communication.command.queue.IQueueObject;
import com.tenkiv.tekdaqc.communication.command.queue.QueueCallback;
import com.tenkiv.tekdaqc.communication.data_points.AnalogInputData;
import com.tenkiv.tekdaqc.communication.data_points.DigitalInputData;
import com.tenkiv.tekdaqc.communication.message.ABoardMessage;
import com.tenkiv.tekdaqc.communication.message.IMessageListener;
import com.tenkiv.tekdaqc.communication.tasks.ITaskComplete;
import com.tenkiv.tekdaqc.hardware.ATekdaqc;
import com.tenkiv.tekdaqc.hardware.Tekdaqc_RevD;
import com.tenkiv.tekdaqc.locator.Locator;
import com.tenkiv.tekdaqc.locator.LocatorResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Class which handles all telnet connections and network operations for communicating with the tekdaqc. This allows for multiple different apps
 * to all share telnet connections and shrink resource use.
 *
 * @author Tenkiv (software@tenkiv.com)
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

    /**
     * A {@link Map} used to store serial numbers and the {@link List} of {@link Messenger}s associated with them.
     */
    private ConcurrentHashMap<String,List<Messenger>> mMessengerMap = new ConcurrentHashMap<>();

    /**
     * The {@link Map} of all connected {@link ATekdaqc}s.
     */
    private ConcurrentHashMap<String,ATekdaqc> mTekdaqcMap = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String,ServiceConnectionThread> mConnectionMap = new ConcurrentHashMap<>();

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
    public void executeTask(final String serial, List<IQueueObject> task, final Messenger callbackBinder){

        for(final IQueueObject object: task){
            if(object instanceof QueueCallback){
                ((QueueCallback) object).addCallback(new ITaskComplete() {
                    @Override
                    public void onTaskSuccess(ATekdaqc aTekdaqc) {
                        if (mMessengerMap.get(serial).size() > 0) {
                            Bundle dataBundle = new Bundle();
                            dataBundle.putString(TekCast.DATA_MESSSAGE_TEKDAQC, serial);
                            dataBundle.putDouble(TekCast.DATA_MESSSAGE_UID, ((QueueCallback) object).getUID());

                            Message dataMessage = Message.obtain();
                            dataMessage.what = TekCast.TEKDAQC_TASK_COMPLETE;
                            dataMessage.setData(dataBundle);

                            try {
                                callbackBinder.send(dataMessage);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onTaskFailed(ATekdaqc aTekdaqc) {
                        if (mMessengerMap.get(serial).size() > 0) {
                            Bundle dataBundle = new Bundle();
                            dataBundle.putString(TekCast.DATA_MESSSAGE_TEKDAQC, serial);
                            dataBundle.putDouble(TekCast.DATA_MESSSAGE_UID, ((QueueCallback) object).getUID());

                            Message dataMessage = Message.obtain();
                            dataMessage.what = TekCast.TEKDAQC_TASK_FAILURE;
                            dataMessage.setData(dataBundle);

                            try {
                                callbackBinder.send(dataMessage);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
            executeCommand(serial,object);
        }
    }

    /**
     * Method to generate the correct {@link ATekdaqc} based upon the {@link LocatorResponse}. At the moment there is only one
     * Tekdaqc revision, however this will not always be the case.
     *
     * @param response The {@link LocatorResponse} of the desired Tekdaqc.
     * @return A {@link ATekdaqc} of the appropriate type.
     */
    public ATekdaqc generateTekdaqc(LocatorResponse response){
        ATekdaqc tekdaqc = null;
        switch (response.getType()){
            case 'D':
            case 'E':
                tekdaqc = new AndroidRemoteTekdaqc(response);
                break;
            default:
                tekdaqc = new AndroidRemoteTekdaqc(response);
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
     * Method for adding to the {@link List} of {@link Messenger}s which need data from Tekdaqcs they try to connect to.
     *
     * @param serial The {@link String} of the serial number.
     * @param messenger The {@link Messenger} used for callbacks.
     */
    public void addMessengerToCallbackMap(String serial, Messenger messenger){
        if(mMessengerMap.get(serial) == null){
            mMessengerMap.put(serial, new ArrayList<Messenger>());
        }

        if(!mMessengerMap.get(serial).contains(messenger)){
            mMessengerMap.get(serial).add(messenger);
        }
    }

    /**
     * Method to connect to a tekdaqc given a {@link LocatorResponse}. This also serves to add a reference to the client side {@link Messenger} if it doesnt exist already.
     * @param response The {@link LocatorResponse} of the tekdaqc to be connected to.
     * @param messenger The {@link Messenger} associated with the connection.
     */
    public void connectToTekdaqc(LocatorResponse response, Messenger messenger){
        addMessengerToCallbackMap(response.getSerial(),messenger);

        if(!mTekdaqcMap.containsKey(response.getSerial())) {
            ATekdaqc tekdaqc = generateTekdaqc(response);
            tekdaqc.registerListener(this);
            mTekdaqcMap.put(tekdaqc.getSerialNumber(), tekdaqc);
            mConnectionMap.put(tekdaqc.getSerialNumber(),new ServiceConnectionThread(tekdaqc));

            mConnectionMap.get(tekdaqc.getSerialNumber()).start();
        }
    }

    /**
     * Method to disconnect from a {@link ATekdaqc}.
     *
     * @param tekdaqc The {@link ATekdaqc} to disconnect from.
     * @param messenger {@link Messenger} to remove from notification.
     */
    public void disconnectFromTekdaqc(ATekdaqc tekdaqc, Messenger messenger){

        mMessengerMap.get(tekdaqc.getSerialNumber()).remove(messenger);

        if(mMessengerMap.get(tekdaqc.getSerialNumber()).size() == 0) {
            try {

                mMessengerMap.remove(tekdaqc.getSerialNumber());

                tekdaqc.unregisterListener(this);
                mTekdaqcMap.get(tekdaqc.getSerialNumber()).halt();
                mTekdaqcMap.get(tekdaqc.getSerialNumber()).disconnect();

                mTekdaqcMap.remove(tekdaqc.getSerialNumber());

                mConnectionMap.get(tekdaqc.getSerialNumber()).join();
                mConnectionMap.remove(tekdaqc.getSerialNumber());

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Method to iterate through all registered listeners and send the {@link Message}.
     *
     * @param message The {@link Message} to be sent.
     */
    public void sendMessageToRegisteredListeners(Message message, String serial){

        List<Messenger> responseList = mMessengerMap.get(serial);

        for (Messenger messenger : responseList) {
            if (messenger != null) {
                try {
                    messenger.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }else{
                mMessengerMap.get(serial).remove(messenger);
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

                case TekCast.SERVICE_MSG_COMMAND:
                    IQueueObject command = (IQueueObject) msg.getData().getSerializable(TekCast.SERVICE_COMMAND_KEY);
                    String commandSerial = msg.getData().getString(TekCast.SERVICE_SERIAL_KEY);
                    executeCommand(commandSerial,command);
                    break;

                case TekCast.SERVICE_MSG_TASK:
                        List<IQueueObject> task = (List<IQueueObject>) msg.getData().getSerializable(TekCast.SERVICE_TASK_KEY);
                        String taskSerial = msg.getData().getString(TekCast.SERVICE_SERIAL_KEY);
                        executeTask(taskSerial, task, msg.replyTo);

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

        if (mMessengerMap.get(tekdaqc.getSerialNumber()).size() > 0) {
            Bundle dataBundle = new Bundle();
            dataBundle.putString(TekCast.DATA_MESSSAGE_TEKDAQC, tekdaqc.getSerialNumber());
            dataBundle.putParcelable(TekCast.DATA_MESSSAGE, (ParcelableErrorMessage) message);

            Message dataMessage = Message.obtain();
            dataMessage.what = TekCast.TEKDAQC_ERROR_MESSAGE;
            dataMessage.setData(dataBundle);

            sendMessageToRegisteredListeners(dataMessage, tekdaqc.getSerialNumber());
        }
    }

    @Override
    public void onStatusMessageReceived(ATekdaqc tekdaqc, ABoardMessage message) {

        if (mMessengerMap.get(tekdaqc.getSerialNumber()).size() > 0) {
            Bundle dataBundle = new Bundle();
            dataBundle.putString(TekCast.DATA_MESSSAGE_TEKDAQC, tekdaqc.getSerialNumber());
            dataBundle.putParcelable(TekCast.DATA_MESSSAGE, (ParcelableStatusMessage) message);

            Message dataMessage = Message.obtain();
            dataMessage.what = TekCast.TEKDAQC_STATUS_MESSAGE;
            dataMessage.setData(dataBundle);

            sendMessageToRegisteredListeners(dataMessage, tekdaqc.getSerialNumber());
        }
    }

    @Override
    public void onDebugMessageReceived(ATekdaqc tekdaqc, ABoardMessage message) {

        if (mMessengerMap.get(tekdaqc.getSerialNumber()).size() > 0) {
            Bundle dataBundle = new Bundle();
            dataBundle.putString(TekCast.DATA_MESSSAGE_TEKDAQC, tekdaqc.getSerialNumber());
            dataBundle.putParcelable(TekCast.DATA_MESSSAGE, (ParcelableDebugMessage) message);

            Message dataMessage = Message.obtain();
            dataMessage.what = TekCast.TEKDAQC_DEBUG_MESSAGE;
            dataMessage.setData(dataBundle);

            sendMessageToRegisteredListeners(dataMessage, tekdaqc.getSerialNumber());
        }
    }

    @Override
    public void onCommandDataMessageReceived(ATekdaqc tekdaqc, ABoardMessage message) {

        if (mMessengerMap.get(tekdaqc.getSerialNumber()).size() > 0) {
            Bundle dataBundle = new Bundle();
            dataBundle.putString(TekCast.DATA_MESSSAGE_TEKDAQC, tekdaqc.getSerialNumber());
            dataBundle.putParcelable(TekCast.DATA_MESSSAGE, (ParcelableCommandMessage) message);

            Message dataMessage = Message.obtain();
            dataMessage.what = TekCast.TEKDAQC_COMMAND_MESSAGE;
            dataMessage.setData(dataBundle);

            sendMessageToRegisteredListeners(dataMessage, tekdaqc.getSerialNumber());
        }
    }

    @Override
    public void onAnalogInputDataReceived(ATekdaqc tekdaqc, AnalogInputData data) {

        if (mMessengerMap.get(tekdaqc.getSerialNumber()).size() > 0) {
            Bundle dataBundle = new Bundle();
            dataBundle.putString(TekCast.DATA_MESSSAGE_TEKDAQC, tekdaqc.getSerialNumber());
            dataBundle.putParcelable(TekCast.DATA_MESSSAGE, (ParcelableAnalogInputData) data);

            Message dataMessage = Message.obtain();
            dataMessage.what = TekCast.TEKDAQC_ANALOG_INPUT_MESSAGE;
            dataMessage.setData(dataBundle);

            sendMessageToRegisteredListeners(dataMessage, tekdaqc.getSerialNumber());
        }
    }


    @Override
    public void onDigitalInputDataReceived(ATekdaqc tekdaqc, DigitalInputData data) {

        if (mMessengerMap.get(tekdaqc.getSerialNumber()).size() > 0) {
            Bundle dataBundle = new Bundle();
            dataBundle.putString(TekCast.DATA_MESSSAGE_TEKDAQC, tekdaqc.getSerialNumber());
            dataBundle.putParcelable(TekCast.DATA_MESSSAGE, (ParcelableDigitalInputData) data);

            Message dataMessage = Message.obtain();
            dataMessage.what = TekCast.TEKDAQC_DIGITAL_INPUT_MESSAGE;
            dataMessage.setData(dataBundle);

            sendMessageToRegisteredListeners(dataMessage, tekdaqc.getSerialNumber());
        }
    }

    @Override
    public void onDigitalOutputDataReceived(ATekdaqc tekdaqc, boolean[] data) {

        if (mMessengerMap.get(tekdaqc.getSerialNumber()).size() > 0) {
            Bundle dataBundle = new Bundle();
            dataBundle.putString(TekCast.DATA_MESSSAGE_TEKDAQC, tekdaqc.getSerialNumber());
            dataBundle.putBooleanArray(TekCast.DATA_MESSSAGE, data);

            Message dataMessage = Message.obtain();
            dataMessage.what = TekCast.TEKDAQC_DIGITAL_OUTPUT_MESSAGE;
            dataMessage.setData(dataBundle);

            sendMessageToRegisteredListeners(dataMessage, tekdaqc.getSerialNumber());
        }
    }
}


package com.tenkiv.tekdaqc.android.application.service;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import com.tenkiv.tekdaqc.ATekdaqc;
import com.tenkiv.tekdaqc.android.application.util.ICommunicationListener;
import com.tenkiv.tekdaqc.android.application.util.IServiceListener;
import com.tenkiv.tekdaqc.android.application.util.TekdaqcHandlerCall;
import com.tenkiv.tekdaqc.communication.ascii.ASCIICommunicationSession;
import com.tenkiv.tekdaqc.communication.command.ABoardCommand;
import com.tenkiv.tekdaqc.communication.data_points.AnalogInputData;
import com.tenkiv.tekdaqc.communication.data_points.DigitalInputData;
import com.tenkiv.tekdaqc.communication.data_points.DigitalOutputData;
import com.tenkiv.tekdaqc.communication.message.ABoardMessage;
import com.tenkiv.tekdaqc.communication.message.IMessageListener;
import com.tenkiv.tekdaqc.communication.message.MessageBroadcaster;
import com.tenkiv.tekdaqc.communication.tasks.ITask;
import com.tenkiv.tekdaqc.communication.tasks.ITaskComplete;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Ellis Berry (ejberry@tenkiv.com)
 */
public class TekdaqcCommunicationManager implements ServiceConnection, IMessageListener, ITaskComplete{

    private Context mContext;

    private ComService.ComServiceBinder mServiceBinder;

    private IServiceListener mServiceListener;

    private static ITaskComplete mTaskCallBack;

    private static TekdaqcCommunicationManager mComManager;

    private static Handler mHandler;

    private static ConcurrentHashMap<String,ArrayList<ICommunicationListener>> mListenerMap;
    private static ConcurrentHashMap<String,ArrayList<ITaskComplete>> mTaskListenerMap;

    private static final String TEKDAQC_BOARD_EXTRA = "TEKDAQC_BOARD_EXTRA";

    private TekdaqcCommunicationManager(Context context,IServiceListener listener){

        mContext = context;

        mHandler = new Handler(mContext.getMainLooper());

        mServiceListener = listener;

        mComManager = this;

        mListenerMap = new ConcurrentHashMap<>();

        mTaskListenerMap = new ConcurrentHashMap<>();

        if(!isComServiceRunning(mContext)) {
            Intent comService = new Intent(context, ComService.class);
            context.bindService(comService, this, Context.BIND_ALLOW_OOM_MANAGEMENT);
            context.startService(comService);

        }else{
            try {
                throw new Exception("Servcie Already Started!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void startCommunicationService(Context context, IServiceListener listener){

        TekdaqcCommunicationManager tekdaqcCommunicationManager = new TekdaqcCommunicationManager(context,listener);
        Intent comService = new Intent(context,ComService.class);
        context.bindService(comService, tekdaqcCommunicationManager, Context.BIND_ALLOW_OOM_MANAGEMENT);
        context.startService(comService);
    }


    //TODO UFM
    /*public void stopCommunication() throws IOException {

        mServiceBinder.getService().executeCommand(mTekdaqc.disconnectCleanly());
        mServiceBinder.getService().haltComService();
        mServiceBinder.getService().stopSelf();

        mHandler.post(new TekdaqcDataHandlerRunnable(mTekdaqc.getSerialNumber(), mTekdaqc, mUserListener, TekdaqcHandlerCall.DISCONNECTED));
    }*/


    public static boolean isComServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ComService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    public void executeBoardCommand(String serial, ABoardCommand command) throws InterruptedException {
        mServiceBinder.getService().executeCommand(serial,command);
    }


    public void executeTaskCommand(String serial, ITask task, ITaskComplete callback){
        mTaskCallBack = callback;
        mServiceBinder.getService().executeTask(serial,task);
    }


    public void connectToTekdaqc(ATekdaqc tekdaqc, ICommunicationListener listener){

        final MessageBroadcaster broadcaster = MessageBroadcaster.getInstance();
        broadcaster.registerListener(tekdaqc, this);

        setCommunicationListener(tekdaqc.getSerialNumber(), listener);

        mServiceBinder.getService().connectToTekdaqc(tekdaqc,listener);
    }


    public void disconnectFromTekdaqc(ATekdaqc tekdaqc, ICommunicationListener listener){
        final MessageBroadcaster broadcaster = MessageBroadcaster.getInstance();
        broadcaster.unRegisterListener(tekdaqc, this);

        mListenerMap.remove(tekdaqc.getSerialNumber());

        try {
            mServiceBinder.getService().haltComService(tekdaqc.getSerialNumber());
        } catch (IOException e) {
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
        mServiceBinder = (ComService.ComServiceBinder) service;
        mHandler.post(new ServiceHandlerRunnable(mServiceListener,mComManager));
    }


    @Override
    public void onServiceDisconnected(ComponentName name) {
    }


    @Override
    public void onErrorMessageReceived(ATekdaqc tekdaqc, ABoardMessage message) {
        for(ICommunicationListener listener: mListenerMap.get(tekdaqc.getSerialNumber())) {
            mHandler.post(new TekdaqcDataHandlerRunnable(tekdaqc, message, listener, TekdaqcHandlerCall.ERROR));
        }
    }


    @Override
    public void onStatusMessageReceived(ATekdaqc tekdaqc, ABoardMessage message) {
        for(ICommunicationListener listener: mListenerMap.get(tekdaqc.getSerialNumber())) {
            mHandler.post(new TekdaqcDataHandlerRunnable(tekdaqc, message, listener, TekdaqcHandlerCall.STATUS));
        }
    }


    @Override
    public void onDebugMessageReceived(ATekdaqc tekdaqc, ABoardMessage message) {
        for(ICommunicationListener listener: mListenerMap.get(tekdaqc.getSerialNumber())) {
            mHandler.post(new TekdaqcDataHandlerRunnable(tekdaqc, message, listener, TekdaqcHandlerCall.DEBUG));
        }
    }


    @Override
    public void onCommandDataMessageReceived(ATekdaqc tekdaqc, ABoardMessage message) {
        for(ICommunicationListener listener: mListenerMap.get(tekdaqc.getSerialNumber())) {
            mHandler.post(new TekdaqcDataHandlerRunnable(tekdaqc, message, listener, TekdaqcHandlerCall.COMMAND));
        }
    }


    @Override
    public void onAnalogInputDataReceived(ATekdaqc tekdaqc, AnalogInputData data) {
        for(ICommunicationListener listener: mListenerMap.get(tekdaqc.getSerialNumber())) {
            mHandler.post(new TekdaqcDataHandlerRunnable(tekdaqc, data, listener, TekdaqcHandlerCall.ANALOG_S));
        }
    }


    @Override
    public void onAnalogInputDataReceived(ATekdaqc tekdaqc, List<AnalogInputData> data) {
        for(ICommunicationListener listener: mListenerMap.get(tekdaqc.getSerialNumber())) {
            mHandler.post(new TekdaqcDataHandlerRunnable(tekdaqc, data, listener, TekdaqcHandlerCall.ANALOG_L));
        }
    }


    @Override
    public void onDigitalInputDataReceived(ATekdaqc tekdaqc, DigitalInputData data) {
        for(ICommunicationListener listener: mListenerMap.get(tekdaqc.getSerialNumber())) {
            mHandler.post(new TekdaqcDataHandlerRunnable(tekdaqc, data, listener, TekdaqcHandlerCall.DIGITAL_I));
        }
    }


    @Override
    public void onDigitalOutputDataReceived(ATekdaqc tekdaqc, ABoardMessage message) {
        for(ICommunicationListener listener: mListenerMap.get(tekdaqc.getSerialNumber())) {
            mHandler.post(new TekdaqcDataHandlerRunnable(tekdaqc, message, listener, TekdaqcHandlerCall.DIGITAL_O));
        }
    }


    @Override
    public void onTaskSuccess() {
        mHandler.post(new TekdaqcTaskRunnable(mTaskCallBack,TekdaqcHandlerCall.TASK_SUCCESS));
    }


    @Override
    public void onTaskFailed() {
        mHandler.post(new TekdaqcTaskRunnable(mTaskCallBack,TekdaqcHandlerCall.TASK_FAILED));
    }


    public static class ComService extends Service {

        private final IBinder mLocatorBinder = new ComServiceBinder();

        private ConcurrentHashMap<String,ASCIICommunicationSession> mSessionMap;


        public ComService() {
            super();
        }

        public void executeCommand(String serial,ABoardCommand command){
            mSessionMap.get(serial).executeCommand(command);
        }

        public void executeTask(String serial, ITask task){
            task.setSession(mSessionMap.get(serial));
            task.execute(mComManager);
        }

        public void haltComService(String serial) throws IOException {
            mSessionMap.get(serial).disconnect();
        }

        public void connectToTekdaqc(ATekdaqc tekdaqc, ICommunicationListener listener){

            ASCIICommunicationSession session = new ASCIICommunicationSession(tekdaqc);
            mSessionMap.put(tekdaqc.getSerialNumber(),session);
            TekdaqcConnectionThread thread = new TekdaqcConnectionThread(session,mHandler,listener);
            thread.setName("sessionThread");
            thread.start();

        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            if(mSessionMap == null) {
                mSessionMap = new ConcurrentHashMap<>();
            }

            return Service.START_NOT_STICKY;
        }

        @Override
        public IBinder onBind(Intent intent) {
            return mLocatorBinder;
        }

        public class ComServiceBinder extends Binder {

            public ComService getService(){
                return ComService.this;
            }
        }


    }
}

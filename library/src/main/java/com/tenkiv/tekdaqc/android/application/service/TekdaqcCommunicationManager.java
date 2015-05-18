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
import android.util.Log;
import com.tenkiv.tekdaqc.ATekdaqc;
import com.tenkiv.tekdaqc.android.application.util.ICommunicationListener;
import com.tenkiv.tekdaqc.android.application.util.IServiceListener;
import com.tenkiv.tekdaqc.android.application.util.TekdaqcHandlerCall;
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

    private static boolean mIsMainThreadCallback = true;

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

    public static void setIsMainThreadCallback(boolean isTrue){
        mIsMainThreadCallback = isTrue;
    }

    public static void startCommunicationService(Context context, IServiceListener listener){
        if(mComManager == null) {
            mComManager = new TekdaqcCommunicationManager(context, listener);
            Intent comService = new Intent(context, ComService.class);
            context.bindService(comService, mComManager, Context.BIND_ALLOW_OOM_MANAGEMENT);
            context.startService(comService);
        }/*else{
            mHandler.post(new ServiceHandlerRunnable(listener,mComManager));
        }*/
    }

    public static TekdaqcCommunicationManager getTekdaqcCommunicationsManager(){
        return mComManager;
    }


    //TODO UFM
    public void stopCommunicationManager() throws IOException {
        mContext.unbindService(this);

    }


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

        //TODO IMPLEMENT TASKCALLBACK BY SERIAL
        /*if(mTaskListenerMap.containsKey(serial)){
            if(!mTaskListenerMap.get(serial).contains(callback)) {
                mTaskListenerMap.get(serial).add(callback);
            }

        }else{
            ArrayList<ITaskComplete> listenerArrayList = new ArrayList<>();
            listenerArrayList.add(callback);
            mTaskListenerMap.put(serial,listenerArrayList);
        }*/

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

        if(mIsMainThreadCallback) {
            mHandler.post(new ServiceHandlerRunnable(mServiceListener, mComManager));
        }else{
            mServiceListener.onManagerServiceCreated(mComManager);
        }
    }


    @Override
    public void onServiceDisconnected(ComponentName name) {
    }


    @Override
    public void onErrorMessageReceived(ATekdaqc tekdaqc, ABoardMessage message) {
        if(mIsMainThreadCallback) {
            for (ICommunicationListener listener : mListenerMap.get(tekdaqc.getSerialNumber())) {
                mHandler.post(new TekdaqcDataHandlerRunnable(tekdaqc, message, listener, TekdaqcHandlerCall.ERROR));
            }
        }else{
            for (ICommunicationListener listener : mListenerMap.get(tekdaqc.getSerialNumber())) {
                listener.onErrorMessageReceived(tekdaqc,message);
            }
        }
    }


    @Override
    public void onStatusMessageReceived(ATekdaqc tekdaqc, ABoardMessage message) {
        if(mIsMainThreadCallback) {
            for(ICommunicationListener listener: mListenerMap.get(tekdaqc.getSerialNumber())) {
                mHandler.post(new TekdaqcDataHandlerRunnable(tekdaqc, message, listener, TekdaqcHandlerCall.STATUS));
            }
        }else{
            for (ICommunicationListener listener : mListenerMap.get(tekdaqc.getSerialNumber())) {
                listener.onStatusMessageReceived(tekdaqc, message);
            }
        }
    }


    @Override
    public void onDebugMessageReceived(ATekdaqc tekdaqc, ABoardMessage message) {
        if(mIsMainThreadCallback) {
            for(ICommunicationListener listener: mListenerMap.get(tekdaqc.getSerialNumber())) {
                mHandler.post(new TekdaqcDataHandlerRunnable(tekdaqc, message, listener, TekdaqcHandlerCall.DEBUG));
            }
        }else{
            for (ICommunicationListener listener : mListenerMap.get(tekdaqc.getSerialNumber())) {
                listener.onDebugMessageReceived(tekdaqc, message);
            }
        }
    }


    @Override
    public void onCommandDataMessageReceived(ATekdaqc tekdaqc, ABoardMessage message) {
        if(mIsMainThreadCallback) {
            for(ICommunicationListener listener: mListenerMap.get(tekdaqc.getSerialNumber())) {
                mHandler.post(new TekdaqcDataHandlerRunnable(tekdaqc, message, listener, TekdaqcHandlerCall.COMMAND));
            }
        }else{
            for (ICommunicationListener listener : mListenerMap.get(tekdaqc.getSerialNumber())) {
                listener.onCommandDataMessageReceived(tekdaqc, message);
            }
        }
    }


    @Override
    public void onAnalogInputDataReceived(ATekdaqc tekdaqc, AnalogInputData data) {
        if(mIsMainThreadCallback) {
            for(ICommunicationListener listener: mListenerMap.get(tekdaqc.getSerialNumber())) {
                mHandler.post(new TekdaqcDataHandlerRunnable(tekdaqc, data, listener, TekdaqcHandlerCall.ANALOG_S));
            }
        }else{
            for (ICommunicationListener listener : mListenerMap.get(tekdaqc.getSerialNumber())) {
                listener.onAnalogInputDataReceived(tekdaqc, data);
            }
        }
    }


    @Override
    public void onAnalogInputDataReceived(ATekdaqc tekdaqc, List<AnalogInputData> data) {
        if(mIsMainThreadCallback) {
            for(ICommunicationListener listener: mListenerMap.get(tekdaqc.getSerialNumber())) {
                mHandler.post(new TekdaqcDataHandlerRunnable(tekdaqc, data, listener, TekdaqcHandlerCall.ANALOG_L));
            }
        }else{
            for (ICommunicationListener listener : mListenerMap.get(tekdaqc.getSerialNumber())) {
                listener.onAnalogInputDataReceived(tekdaqc, data);
            }
        }
    }


    @Override
    public void onDigitalInputDataReceived(ATekdaqc tekdaqc, DigitalInputData data) {
        if(mIsMainThreadCallback) {
            for(ICommunicationListener listener: mListenerMap.get(tekdaqc.getSerialNumber())) {
                mHandler.post(new TekdaqcDataHandlerRunnable(tekdaqc, data, listener, TekdaqcHandlerCall.DIGITAL_I));
            }
        }else{
            for (ICommunicationListener listener : mListenerMap.get(tekdaqc.getSerialNumber())) {
                listener.onDigitalInputDataReceived(tekdaqc, data);
            }
        }
    }


    @Override
    public void onDigitalOutputDataReceived(ATekdaqc tekdaqc, ABoardMessage message) {
        if(mIsMainThreadCallback) {
            for(ICommunicationListener listener: mListenerMap.get(tekdaqc.getSerialNumber())) {
                mHandler.post(new TekdaqcDataHandlerRunnable(tekdaqc, message, listener, TekdaqcHandlerCall.DIGITAL_O));
            }
        }else{
            for (ICommunicationListener listener : mListenerMap.get(tekdaqc.getSerialNumber())) {
                listener.onDigitalOutputDataReceived(tekdaqc, message);
            }
        }
    }


    @Override
    public void onTaskSuccess() {
        if(mIsMainThreadCallback) {
            mHandler.post(new TekdaqcTaskRunnable(mTaskCallBack, TekdaqcHandlerCall.TASK_SUCCESS));
        }else{
            mTaskCallBack.onTaskSuccess();
        }
    }


    @Override
    public void onTaskFailed() {
        if(mIsMainThreadCallback) {
            mHandler.post(new TekdaqcTaskRunnable(mTaskCallBack, TekdaqcHandlerCall.TASK_FAILED));
        }else{
            mTaskCallBack.onTaskFailed();
        }
    }


    public static class ComService extends Service {

        private final IBinder mComBinder = new ComServiceBinder();

        private ConcurrentHashMap<String,ASCIICommunicationSession> mSessionMap;

        @Override
        public void onDestroy() {
            super.onDestroy();
            Log.d("TCM","onDestroy");
        }

        @Override
        public boolean onUnbind(Intent intent) {
            return super.onUnbind(intent);

        }

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
            return mComBinder;
        }

        public class ComServiceBinder extends Binder {

            public ComService getService(){
                return ComService.this;
            }
        }


    }
}

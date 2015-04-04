package com.tenkiv.tekdaqc.android.application.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import com.tenkiv.tekdaqc.ATekdaqc;
import com.tenkiv.tekdaqc.android.application.util.ICommunicationListener;
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
import java.util.List;

/**
 * Created by Ellis Berry (ejberry@tenkiv.com)
 */
public class TekdaqcCommunicationManager implements ServiceConnection, IMessageListener, ITaskComplete{

    private Context mContext;

    private ComService.ComServiceBinder mServiceBinder;

    private static ICommunicationListener mUserListener;

    private static ITaskComplete mTaskCallBack;

    private static TekdaqcCommunicationManager mComManager;

    private static Handler mHandler;

    private ATekdaqc mTekdaqc;

    private static final String TEKDAQC_BOARD_EXTRA = "TEKDAQC_BOARD_EXTRA";


    private TekdaqcCommunicationManager(Context context, ICommunicationListener listener, ATekdaqc tekdaqc){

        mUserListener = listener;

        mContext = context;

        mComManager = this;

        mTekdaqc = tekdaqc;

        final MessageBroadcaster broadcaster = MessageBroadcaster.getInstance();
        broadcaster.registerListener(mTekdaqc.getSerialNumber(), this);

        mHandler = new Handler(mContext.getMainLooper());

    }

    private TekdaqcCommunicationManager(Context context, ATekdaqc tekdaqc){

        mContext = context;

        mComManager = this;

        mTekdaqc = tekdaqc;

        final MessageBroadcaster broadcaster = MessageBroadcaster.getInstance();
        broadcaster.registerListener(mTekdaqc.getSerialNumber(), this);

        mHandler = new Handler(mContext.getMainLooper());

    }

    public static void startCommunication(Context context, ICommunicationListener listener, ATekdaqc tekdaqc) throws InterruptedException {

        TekdaqcCommunicationManager tekdaqcCommunicationManager = new TekdaqcCommunicationManager(context,listener,tekdaqc);

        Intent comService = new Intent(context,ComService.class);

        comService.putExtra(TEKDAQC_BOARD_EXTRA, tekdaqc);

        context.bindService(comService, tekdaqcCommunicationManager, Context.BIND_ALLOW_OOM_MANAGEMENT);
        context.startService(comService);


    }

    public  void setCommunicationListener(ICommunicationListener listener){
        mUserListener = listener;
    }

    public void stopCommunication() throws IOException {

        mServiceBinder.getService().executeCommand(mTekdaqc.disconnectCleanly());
        mServiceBinder.getService().haltComService();
        mServiceBinder.getService().stopSelf();

        mHandler.post(new TekdaqcDataHandlerRunnable(mTekdaqc.getSerialNumber(), mTekdaqc, mUserListener, TekdaqcHandlerCall.DISCONNECTED));

    }

    public void executeBoardCommand(ABoardCommand command) throws InterruptedException {
        mServiceBinder.getService().executeCommand(command);
    }

    public void executeTaskCommand(ITask task, ITaskComplete callback){
        mTaskCallBack = callback;
        mServiceBinder.getService().executeTask(task);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mServiceBinder = (ComService.ComServiceBinder) service;

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }

    @Override
    public void onErrorMessageReceived(String serial, ABoardMessage message) {
        mHandler.post(new TekdaqcDataHandlerRunnable(serial, message, mUserListener, TekdaqcHandlerCall.ERROR));
    }

    @Override
    public void onStatusMessageReceived(String serial, ABoardMessage message) {
        mHandler.post(new TekdaqcDataHandlerRunnable(serial,message,mUserListener,TekdaqcHandlerCall.STATUS));
    }

    @Override
    public void onDebugMessageReceived(String serial, ABoardMessage message) {
        mHandler.post(new TekdaqcDataHandlerRunnable(serial, message, mUserListener, TekdaqcHandlerCall.DEBUG));
    }

    @Override
    public void onCommandDataMessageReceived(String serial, ABoardMessage message) {
        mHandler.post(new TekdaqcDataHandlerRunnable(serial,message,mUserListener,TekdaqcHandlerCall.COMMAND));
    }

    @Override
    public void onAnalogInputDataReceived(String serial, AnalogInputData data) {
        mHandler.post(new TekdaqcDataHandlerRunnable(serial,data,mUserListener,TekdaqcHandlerCall.ANALOG_S));
    }

    @Override
    public void onAnalogInputDataReceived(String serial, List<AnalogInputData> data) {
        mHandler.post(new TekdaqcDataHandlerRunnable(serial,data,mUserListener,TekdaqcHandlerCall.ANALOG_L));
    }

    @Override
    public void onDigitalInputDataReceived(String serial, DigitalInputData data) {
        mHandler.post(new TekdaqcDataHandlerRunnable(serial,data,mUserListener,TekdaqcHandlerCall.DIGITAL_I));
    }

    @Override
    public void onDigitalOutputDataReceived(String serial, DigitalOutputData data) {
        mHandler.post(new TekdaqcDataHandlerRunnable(serial,data,mUserListener,TekdaqcHandlerCall.DIGITAL_O));
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
        private ASCIICommunicationSession mSession;
        private ATekdaqc mTekdaqc;

        public ComService() {
            super();
        }

        public void executeCommand(ABoardCommand command){
            mSession.executeCommand(command);
        }

        public void executeTask(ITask task){
            task.setSession(mSession);
            task.execute(mComManager);
        }

        public void haltComService() throws IOException {
            mSession.disconnect();
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {

            if(intent!=null){
                Bundle extras = intent.getExtras();
                mTekdaqc = (ATekdaqc) extras.getSerializable(TEKDAQC_BOARD_EXTRA);
                mSession = new ASCIICommunicationSession(mTekdaqc);

                Thread sessionThread = new Thread(){

                    @Override
                    public void run() {
                        super.run();
                        try {
                            mSession.connect(ATekdaqc.CONNECTION_METHOD.ETHERNET);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        mHandler.post(new TekdaqcDataHandlerRunnable(null, mComManager, mUserListener, TekdaqcHandlerCall.CONNECTED));
                    }
                };
                sessionThread.setName("sessionThread");
                sessionThread.start();



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

package com.tenkiv.tekdaqc;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.os.Process;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CommunicationService extends Service {

    protected static final String TAG = CommunicationService.class.getSimpleName();

    private Map<String, TekdaqcCommunicationSession> mCommSessions;
    private PollingReadExecutor mPollingExecutor;

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private LocalBroadcastManager mLocalBroadcastMgr;

    @Override
    public void onCreate() {
        super.onCreate();

        HandlerThread thread = new HandlerThread("TekDAQC Discovery Service", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper, this);
        mLocalBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());

        mCommSessions = new ConcurrentHashMap<String, TekdaqcCommunicationSession>();
        mPollingExecutor = new PollingReadExecutor(mCommSessions);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String action = intent.getAction();
        Bundle extras = intent.getExtras();

        if (extras == null)
            extras = new Bundle();

        extras.putString(TekCast.EXTRA_SERVICE_ACTION, action);

        // Run each command in a separate thread.
        final Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.setData(extras);
        mServiceHandler.sendMessage(msg);
        return Service.START_NOT_STICKY;
    }

    private void notifyOfBoardConnection(String serial) {
        final Intent intent = new Intent(TekCast.ACTION_BOARD_CONNECTED);
        intent.putExtra(TekCast.EXTRA_BOARD_SERIAL, serial);
        mLocalBroadcastMgr.sendBroadcast(intent);
    }

    private void notifyOfBoardDisconnection(String serial) {
        final Intent intent = new Intent(TekCast.ACTION_BOARD_DISCONNECTED);
        intent.putExtra(TekCast.EXTRA_BOARD_SERIAL, serial);
        mLocalBroadcastMgr.sendBroadcast(intent);
    }

    /**
     * Processable actions by the {@link CommunicationService}.
     *
     * @author <a href=mailto:toxicbakery@gmail.com>Ian Thomas</a>
     */
    public static enum ServiceAction {
        /**
         * Locate all TekDAQC boards on a local network with the optionally provided {@link LocatorParams}.
         */
        CONNECT, DISCONNECT, COMMAND

        /**
         * Force shutdown of the {@link TelnetService}.
         */
        , STOP;
    }

    /**
     * Worker thread for handling incoming {@link DiscoveryService} {@link ServiceAction} requests.
     */
    private static final class ServiceHandler extends Handler {

        private CommunicationService mService;

        public ServiceHandler(Looper looper, CommunicationService service) {
            super(looper);
            mService = service;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void handleMessage(Message msg) {
            final Bundle data = msg.getData();
            final ServiceAction action = ServiceAction.valueOf(data.getString(TekCast.EXTRA_SERVICE_ACTION));
            final ATekDAQC tekdaqc = (ATekDAQC) data.getSerializable(TekCast.EXTRA_TEK_BOARD);
            switch (action) {
                case CONNECT:
                    Log.d(TAG, "Processing CONNECT message for Tekdaqc: " + tekdaqc);
                    if (tekdaqc != null) {
                        final TekdaqcCommunicationSession session = new TekdaqcCommunicationSession(tekdaqc);
                        try {
                            session.connect();
                            mService.mCommSessions.put(tekdaqc.getSerialNumber(), session);
                            mService.notifyOfBoardConnection(tekdaqc.getSerialNumber());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        throw new IllegalArgumentException("Missing required board extra.");
                    }
                    break;
                case DISCONNECT:
                    Log.d(TAG, "Processing DISCONNECT message for Tekdaqc: " + tekdaqc);
                    if (tekdaqc != null) {
                        try {
                            final TekdaqcCommunicationSession session = mService.mCommSessions.get(tekdaqc.getSerialNumber());
                            if (session != null) {
                                session.disconnect();
                                mService.notifyOfBoardDisconnection(tekdaqc.getSerialNumber());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        throw new IllegalArgumentException("Missing required board extra.");
                    }
                    break;
                case COMMAND:
                    break;
                case STOP:
                    break;
            }
        }
    }
}
